package org.mirage.gfbs.Client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.Random;

@Mod.EventBusSubscriber(modid = "mirage_gfbs", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SinglePlayerLANHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean hasCheckedForLAN = false;
    private static int ticksSinceLogin = 0;
    private static final int CHECK_DELAY_TICKS = 60;
    private static final Random RANDOM = new Random();
    private static final int MIN_PORT = 25565;
    private static final int MAX_PORT = 65535;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        
        if (mc.level == null || mc.player == null) {
            hasCheckedForLAN = false;
            ticksSinceLogin = 0;
            return;
        }

        if (hasCheckedForLAN) return;

        ticksSinceLogin++;
        
        if (ticksSinceLogin >= CHECK_DELAY_TICKS) {
            checkAndOpenLAN(mc);
            hasCheckedForLAN = true;
        }
    }

    private static void checkAndOpenLAN(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        
        if (server == null) {
            LOGGER.info("[G.F.B.S.] 不是单人游戏服务器，跳过LAN检测");
            return;
        }
        
        if (server.isPublished()) {
            LOGGER.info("[G.F.B.S.] 已开启局域网模式，跳过");
            return;
        }
        
        LOGGER.info("[G.F.B.S.] 检测到单人游戏，正在强制打开局域网模式...");
        LOGGER.info("[G.F.B.S.] Server class: {}", server.getClass().getName());
        
        try {
            int randomPort = generateRandomPort();
            boolean success = openToLan(mc, server, randomPort);
            
            if (success) {
                mc.player.displayClientMessage(
                    Component.literal("[G.F.B.S.] 因模组内容可能不适配于单人游戏，已强制启用局域网 (LAN) 模式在端口" + randomPort + "。"), 
                    false
                );
                LOGGER.info("[G.F.B.S.] 成功打开局域网模式，端口：{}", randomPort);
            } else {
                mc.player.displayClientMessage(
                    Component.literal("[G.F.B.S.] 打开局域网模式返回失败，请手动打开。"), 
                    false
                );
                LOGGER.warn("[G.F.B.S.] openToLan returned false");
            }
        } catch (Exception e) {
            LOGGER.error("[G.F.B.S.] 打开局域网模式失败", e);
            mc.player.displayClientMessage(
                Component.literal("[G.F.B.S.] 尝试打开局域网模式失败，请手动打开。"), 
                false
            );
        }
    }

    private static boolean openToLan(Minecraft mc, MinecraftServer server, int port) throws Exception {
        try {
            Method[] serverMethods = server.getClass().getDeclaredMethods();
            
            LOGGER.debug("[G.F.B.S.] Searching for publishServer method...");
            for (Method method : serverMethods) {
                String name = method.getName();
                Class<?>[] params = method.getParameterTypes();
                
                if (name.equals("publishServer") || name.equals("m_129907_") || name.equals("func_71225_e")) {
                    LOGGER.info("[G.F.B.S.] Found publishServer method: {}, params: {}", name, params.length);
                    method.setAccessible(true);
                    try {
                        Object result = method.invoke(server, 
                            GameType.SURVIVAL, 
                            false, 
                            port
                        );
                        if (result instanceof Boolean) {
                            return (Boolean) result;
                        }
                        return server.isPublished();
                    } catch (Exception e) {
                        LOGGER.error("Failed to invoke publishServer: " + name, e);
                    }
                }
            }
            
            for (Method method : serverMethods) {
                String name = method.getName();
                Class<?>[] params = method.getParameterTypes();
                
                if (name.toLowerCase().contains("publish")) {
                    LOGGER.info("[G.F.B.S.] Found publish-like method: {}, params: {}", name, params.length);
                    method.setAccessible(true);
                    try {
                        Object[] args = new Object[params.length];
                        for (int i = 0; i < params.length; i++) {
                            if (params[i] == GameType.class) {
                                args[i] = GameType.SURVIVAL;
                            } else if (params[i] == boolean.class || params[i] == Boolean.class) {
                                args[i] = false;
                            } else if (params[i] == int.class || params[i] == Integer.class) {
                                args[i] = port;
                            } else if (params[i] == String.class) {
                                args[i] = "";
                            } else {
                                args[i] = null;
                            }
                        }
                        Object result = method.invoke(server, args);
                        if (result instanceof Boolean) {
                            return (Boolean) result;
                        }
                        return server.isPublished();
                    } catch (Exception e) {
                        LOGGER.debug("Method {} invocation failed", method.getName(), e);
                    }
                }
            }
            
            Method[] mcMethods = mc.getClass().getDeclaredMethods();
            for (Method method : mcMethods) {
                String name = method.getName();
                if (name.equals("m_91330_") || name.equals("openToLan") || name.equals("func_71397_h")) {
                    LOGGER.info("[G.F.B.S.] Found Minecraft.openToLan method: {}", name);
                    method.setAccessible(true);
                    try {
                        Object result = method.invoke(mc);
                        if (result instanceof Boolean) {
                            return (Boolean) result;
                        }
                        return server.isPublished();
                    } catch (Exception e) {
                        LOGGER.error("Failed to invoke Minecraft.openToLan: " + name, e);
                    }
                }
            }
            
            LOGGER.info("[G.F.B.S.] Dumping all server methods for debugging:");
            for (Method method : serverMethods) {
                if (method.getReturnType() == boolean.class || method.getName().toLowerCase().contains("publish") || method.getName().toLowerCase().contains("lan")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("  ").append(method.getName()).append("(");
                    Class<?>[] params = method.getParameterTypes();
                    for (int i = 0; i < params.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(params[i].getSimpleName());
                    }
                    sb.append(") -> ").append(method.getReturnType().getSimpleName());
                    LOGGER.info(sb.toString());
                }
            }
            
            LOGGER.error("[G.F.B.S.] Could not find any suitable method to open LAN");
            return false;
        } catch (Exception e) {
            LOGGER.error("[G.F.B.S.] Error in openToLan", e);
            return false;
        }
    }
    
    private static int generateRandomPort() {
        for (int attempt = 0; attempt < 10; attempt++) {
            int randomPort = MIN_PORT + RANDOM.nextInt(MAX_PORT - MIN_PORT + 1);
            if (isPortAvailable(randomPort)) {
                LOGGER.debug("[G.F.B.S.] 生成随机端口：{} (尝试 {} 次)", randomPort, attempt + 1);
                return randomPort;
            }
            LOGGER.debug("[G.F.B.S.] 端口 {} 已被占用，重新生成...", randomPort);
        }
        
        int fallbackPort = MIN_PORT + RANDOM.nextInt(MAX_PORT - MIN_PORT + 1);
        LOGGER.info("[G.F.B.S.] 使用备用随机端口：{}", fallbackPort);
        return fallbackPort;
    }
    
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new java.net.InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
