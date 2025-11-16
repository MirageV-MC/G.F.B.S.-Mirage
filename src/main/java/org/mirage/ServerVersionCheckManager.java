package org.mirage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import org.mirage.Phenomenon.network.versioncheck.VersionRequestPacket;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerVersionCheckManager {

    // 超时时间：5 秒
    private static final int TIMEOUT_TICKS = 20 * 5;

    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    public static void startCheck(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PENDING.put(uuid, TIMEOUT_TICKS);

        org.mirage.Phenomenon.network.versioncheck.NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new VersionRequestPacket()
        );

    }

    public static void onVersionResponse(ServerPlayer player, String clientVersion) {
        UUID uuid = player.getUUID();

        if (!PENDING.containsKey(uuid)) {
            return;
        }

        PENDING.remove(uuid);

        String serverVersion = ModList.get()
                .getModContainerById(Mirage_gfbs.MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");

        if (!serverVersion.equals(clientVersion)) {
            Component reason = Component.literal(
                    "[G.F.B.S.-VersionLock] 本模组版本不一致\n" +
                            "服务器版本: " + serverVersion + "\n" +
                            "客户端版本: " + clientVersion + "\n" +
                            "请使用与服务器相同版本的模组再尝试连接."
            );
            player.connection.disconnect(reason);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, Integer>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID uuid = entry.getKey();
            int ticksLeft = entry.getValue() - 1;

            if (ticksLeft <= 0) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    player.connection.disconnect(Component.literal(
                            """
                                    [G.F.B.S.-VersionLock]
                                    未在规定时间内完成版本校验, 连接被拒绝.
                                    请确认客户端安装并启用了本模组并更新到对应版本."""
                    ));
                }
                it.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }
    }
}