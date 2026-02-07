package org.mirage.gfbs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PrivilegeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path WHITELIST_FILE = MirageGFBS.CONFIG_DIR.resolve("privilege_whitelist.json");

    private static final int DEFAULT_OP_LEVEL = 4;

    private static boolean autoOpEnabled = true;

    // 硬编码默认白名单
    private static final Map<String, String> DEFAULT_UUID_WHITELIST = Map.of(
    );

    // 硬编码离线玩家白名单
    private static final Set<String> DEFAULT_OFFLINE_WHITELIST = Set.of(
    );

    private static final Map<UUID, String> uuidToNameMap = new ConcurrentHashMap<>();
    private static final Map<String, UUID> nameToUuidMap = new ConcurrentHashMap<>();

    static {
        loadWhitelist();
        initializeHardcodedOfflinePlayers();
    }

    /**
     * 设置是否自动给予OP权限
     */
    public static void setAutoOpEnabled(boolean enabled) {
        autoOpEnabled = enabled;
        MirageGFBS.LOGGER.info("特权玩家自动OP权限已{}", enabled ? "启用" : "禁用");
    }

    /**
     * 获取当前自动OP权限状态
     */
    public static boolean isAutoOpEnabled() {
        return autoOpEnabled;
    }

    /**
     * 设置特权玩家的OP等级
     */
    public static void setOpLevel(int level) {
        if (level < 0 || level > 4) {
            MirageGFBS.LOGGER.warn("无效的OP等级: {}，必须介于0-4之间", level);
            return;
        }

        if (MirageGFBS.server != null) {
            PlayerList playerList = MirageGFBS.server.getPlayerList();
            for (ServerPlayer player : playerList.getPlayers()) {
                if (hasPrivilege(player)) {
                    playerList.op(player.getGameProfile());
                    MirageGFBS.LOGGER.debug("已更新玩家 {} 的OP等级为 {}", player.getGameProfile().getName(), level);
                }
            }
        }
    }

    /**
     * 给予玩家OP权限
     */
    private static void grantOpPermission(ServerPlayer player) {
        if (!autoOpEnabled || player == null || MirageGFBS.server == null) {
            return;
        }

        try {
            PlayerList playerList = MirageGFBS.server.getPlayerList();

            if (!playerList.isOp(player.getGameProfile())) {
                playerList.op(player.getGameProfile());
                MirageGFBS.LOGGER.info("已自动给予特权玩家 {} OP权限",
                        player.getGameProfile().getName());

                player.sendSystemMessage(Component.literal("§a您已被授予特权玩家权限！"));
            }
        } catch (Exception e) {
            MirageGFBS.LOGGER.error("给予玩家 {} OP权限失败: {}",
                    player.getGameProfile().getName(), e.getMessage());
        }
    }

    /**
     * 移除玩家的OP权限
     */
    private static void revokeOpPermission(ServerPlayer player) {
        if (player == null || MirageGFBS.server == null) {
            return;
        }

        try {
            PlayerList playerList = MirageGFBS.server.getPlayerList();

            if (playerList.isOp(player.getGameProfile())) {
                playerList.deop(player.getGameProfile());
                MirageGFBS.LOGGER.info("已移除玩家 {} 的OP权限", player.getGameProfile().getName());

                player.sendSystemMessage(Component.literal("§c您的特权玩家权限已被移除！"));
            }
        } catch (Exception e) {
            MirageGFBS.LOGGER.error("移除玩家 {} OP权限失败: {}",
                    player.getGameProfile().getName(), e.getMessage());
        }
    }

    private static void initializeHardcodedOfflinePlayers() {
        for (String offlinePlayer : DEFAULT_OFFLINE_WHITELIST) {
            if (!nameToUuidMap.containsKey(offlinePlayer)) {
                nameToUuidMap.put(offlinePlayer, generateVirtualUuid(offlinePlayer));
            }
        }

        MirageGFBS.LOGGER.info("已初始化硬编码离线白名单: {} 个玩家", DEFAULT_OFFLINE_WHITELIST.size());
    }

    private static UUID generateVirtualUuid(String username) {
        long mostSigBits = (long) username.hashCode() << 32;
        long leastSigBits = (long) System.currentTimeMillis() ^ username.hashCode();
        return new UUID(mostSigBits, leastSigBits);
    }

    public static boolean hasPrivilege(ServerPlayer player) {
        if (player == null) return false;

        UUID playerUUID = player.getUUID();
        String playerName = player.getGameProfile().getName();

        if (uuidToNameMap.containsKey(playerUUID)) {
            return true;
        }

        if (nameToUuidMap.containsKey(playerName)) {
            return true;
        }

        if (DEFAULT_UUID_WHITELIST.containsValue(playerUUID.toString())) {
            return true;
        }

        if (DEFAULT_UUID_WHITELIST.containsKey(playerName) ||
                DEFAULT_OFFLINE_WHITELIST.contains(playerName)) {
            return true;
        }

        return false;
    }

    public static boolean hasPrivilege(net.minecraft.commands.CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer) {
            return hasPrivilege((ServerPlayer) source.getEntity());
        }
        return source.hasPermission(4); // 给OP权限作为备选方案
    }

    public static boolean addToWhitelist(ServerPlayer player) {
        if (player == null) return false;

        UUID uuid = player.getUUID();
        String username = player.getGameProfile().getName();

        uuidToNameMap.put(uuid, username);
        nameToUuidMap.put(username, uuid);

        // 自动给予OP权限
        grantOpPermission(player);

        return saveWhitelist();
    }

    @Deprecated
    public static boolean addToWhitelist(String username) {
        if (username == null || username.isEmpty()) return false;

        nameToUuidMap.put(username, null);

        // 如果玩家在线，立即给予OP权限
        if (MirageGFBS.server != null) {
            ServerPlayer player = MirageGFBS.server.getPlayerList().getPlayerByName(username);
            if (player != null) {
                grantOpPermission(player);
            }
        }

        return saveWhitelist();
    }

    public static boolean removeFromWhitelist(ServerPlayer player) {
        if (player == null) return false;

        UUID uuid = player.getUUID();
        String username = player.getGameProfile().getName();

        uuidToNameMap.remove(uuid);
        nameToUuidMap.remove(username);

        // 移除OP权限
        revokeOpPermission(player);

        return saveWhitelist();
    }

    public static boolean removeFromWhitelist(String username) {
        if (username == null || username.isEmpty()) return false;

        UUID uuid = nameToUuidMap.get(username);
        if (uuid != null) {
            uuidToNameMap.remove(uuid);
        }
        nameToUuidMap.remove(username);

        if (MirageGFBS.server != null) {
            ServerPlayer player = MirageGFBS.server.getPlayerList().getPlayerByName(username);
            if (player != null) {
                revokeOpPermission(player);
            }
        }

        return saveWhitelist();
    }

    public static List<String> getPrivilegedPlayers() {
        List<String> result = new ArrayList<>();

        DEFAULT_UUID_WHITELIST.forEach((name, uuid) -> {
            result.add(name + " (默认-UUID)");
        });

        DEFAULT_OFFLINE_WHITELIST.forEach(name -> {
            result.add(name + " (默认-离线)");
        });

        uuidToNameMap.forEach((uuid, name) -> {
            result.add(name + " (动态-UUID)");
        });

        nameToUuidMap.forEach((name, uuid) -> {
            if (uuid == null && !DEFAULT_OFFLINE_WHITELIST.contains(name)) {
                result.add(name + " (动态-离线)");
            }
        });

        return result;
    }

    public static Set<String> getHardcodedOfflinePlayers() {
        return Collections.unmodifiableSet(DEFAULT_OFFLINE_WHITELIST);
    }

    public static boolean isHardcodedOfflinePlayer(String username) {
        return DEFAULT_OFFLINE_WHITELIST.contains(username);
    }

    public static void onPlayerLogin(ServerPlayer player) {
        if (player == null) return;

        UUID uuid = player.getUUID();
        String username = player.getGameProfile().getName();

        if (nameToUuidMap.containsKey(username) && nameToUuidMap.get(username) == null) {
            nameToUuidMap.put(username, uuid);
            uuidToNameMap.put(uuid, username);
            saveWhitelist();

            MirageGFBS.LOGGER.info("玩家 {} 的UUID信息已补全: {}", username, uuid);
        }

        if (DEFAULT_OFFLINE_WHITELIST.contains(username)) {
            MirageGFBS.LOGGER.info("硬编码离线玩家 {} 已上线，UUID: {}", username, uuid);
        }

        if (DEFAULT_UUID_WHITELIST.containsKey(username) &&
                !uuid.toString().equals(DEFAULT_UUID_WHITELIST.get(username))) {
            MirageGFBS.LOGGER.warn("玩家 {} 的用户名在默认名单中，但UUID不匹配", username);
        }

        // 玩家上线时检查并给予OP权限
        if (hasPrivilege(player)) {
            grantOpPermission(player);
        }
    }

    public static void onPlayerLogout(ServerPlayer player) {
    }

    private static void loadWhitelist() {
        try {
            if (!Files.exists(WHITELIST_FILE)) {
                Files.createDirectories(WHITELIST_FILE.getParent());
                saveWhitelist();
                MirageGFBS.LOGGER.info("创建新的特权白名单文件");
                return;
            }

            String json = Files.readString(WHITELIST_FILE);

            if (json.trim().startsWith("[")) {
                MirageGFBS.LOGGER.error("特权白名单文件格式错误（应为对象但实际为数组），将重置文件");
                backupCorruptedFile();
                saveWhitelist();
                return;
            }

            if (json.trim().isEmpty() || json.trim().equals("{}")) {
                MirageGFBS.LOGGER.warn("特权白名单文件为空，使用默认配置");
                saveWhitelist();
                return;
            }

            WhitelistData data = GSON.fromJson(json, WhitelistData.class);

            if (data != null) {
                if (data.uuidMappings != null) {
                    data.uuidMappings.forEach((uuidStr, name) -> {
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            uuidToNameMap.put(uuid, name);
                            nameToUuidMap.put(name, uuid);
                        } catch (IllegalArgumentException e) {
                            MirageGFBS.LOGGER.error("无效的UUID格式: {}", uuidStr);
                        }
                    });
                }

                if (data.offlineNames != null) {
                    data.offlineNames.forEach(name -> {
                        if (!DEFAULT_OFFLINE_WHITELIST.contains(name) && !nameToUuidMap.containsKey(name)) {
                            nameToUuidMap.put(name, null);
                        }
                    });
                }
            }

            MirageGFBS.LOGGER.info("已加载特权白名单: {} 个UUID映射, {} 个动态离线用户名",
                    uuidToNameMap.size(),
                    nameToUuidMap.values().stream().filter(Objects::isNull).count());
        } catch (IOException e) {
            MirageGFBS.LOGGER.error("加载特权白名单失败: {}", e.getMessage());
            try {
                saveWhitelist();
            } catch (Exception ex) {
                MirageGFBS.LOGGER.error("无法创建默认白名单文件: {}", ex.getMessage());
            }
        } catch (Exception e) {
            MirageGFBS.LOGGER.error("解析特权白名单文件时发生未知错误: {}", e.getMessage());
            backupCorruptedFile();
            saveWhitelist();
        }
    }

    private static void backupCorruptedFile() {
        try {
            if (Files.exists(WHITELIST_FILE)) {
                Path backupFile = WHITELIST_FILE.getParent()
                        .resolve("privilege_whitelist_corrupted_" + System.currentTimeMillis() + ".json");
                Files.move(WHITELIST_FILE, backupFile);
                MirageGFBS.LOGGER.info("已备份损坏的白名单件: {}", backupFile.getFileName());
            }
        } catch (IOException e) {
            MirageGFBS.LOGGER.error("备份损坏的白名单文件失败: {}", e.getMessage());
        }
    }

    private static boolean saveWhitelist() {
        try {
            WhitelistData data = new WhitelistData();

            Map<String, String> uuidMappings = new HashMap<>();
            uuidToNameMap.forEach((uuid, name) -> {
                uuidMappings.put(uuid.toString(), name);
            });
            data.uuidMappings = uuidMappings;

            List<String> offlineNames = new ArrayList<>();
            nameToUuidMap.forEach((name, uuid) -> {
                if (uuid == null && !DEFAULT_OFFLINE_WHITELIST.contains(name)) {
                    offlineNames.add(name);
                }
            });
            data.offlineNames = offlineNames;

            Files.writeString(WHITELIST_FILE, GSON.toJson(data));
            return true;
        } catch (IOException e) {
            MirageGFBS.LOGGER.error("保存特权白名单失败: {}", e.getMessage());
            return false;
        }
    }

    private static class WhitelistData {
        public Map<String, String> uuidMappings;
        public List<String> offlineNames;
    }
}