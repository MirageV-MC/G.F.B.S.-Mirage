package org.mirage.gfbs.Event.ccio.dmr;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.phys.Vec3;
import org.mirage.gfbs.Command.FluorescentTubeCommandRegistry;
import org.mirage.gfbs.Command.NotificationCommand;
import org.mirage.gfbs.CommandExecutor;
import org.mirage.gfbs.Event.DmrMeltdown;
import org.mirage.gfbs.Tools.Task;
import org.mirage.gfbs.api.BroadSystemAPI;
import org.mirage.gfbs.api.CountdownAPI;
import org.mirage.gfbs.auralis.api.AuralisServerApi;
import org.mirage.gfbs.ccio.api.CCIoApiRegistry;
import org.mirage.gfbs.ccio.event.CCIoEventManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.mirage.gfbs.Event.DmrMeltdown.MUSIC_ID;

public final class DmrShutdownApis {
    private DmrShutdownApis() {}

    private static final String EVENT_TEMPERATURE_REQUEST = "dmr_temperature_request";
    private static final long TEMPERATURE_REQUEST_TIMEOUT_MS = 5000;
    private static final ConcurrentHashMap<Long, CompletableFuture<Double>> pendingTemperatureRequests = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong requestIdCounter = new java.util.concurrent.atomic.AtomicLong(0);

    public static boolean isNewMusic = true;
    public static boolean haveMusic = true;

    public static void register() {
        CCIoApiRegistry.register("dmr.shutdownRequest", DmrShutdownApis::shutdownRequest);
        CCIoApiRegistry.register("dmr.getCrackedDigits", DmrShutdownApis::getCrackedDigits);
        CCIoApiRegistry.register("dmr.getCrackedDisplay", DmrShutdownApis::getCrackedDisplay);
        CCIoApiRegistry.register("dmr.hasActiveCode", DmrShutdownApis::hasActiveCode);
        CCIoApiRegistry.register("dmr.submitTemperature", DmrShutdownApis::submitTemperature);
        
        CCIoEventManager.getInstance().registerEvent(EVENT_TEMPERATURE_REQUEST);
    }

    private static Object submitTemperature(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        if (args == null || args.length < 2) {
            throw new LuaException("dmr.submitTemperature expects: (requestId, temperature)");
        }
        
        long requestId;
        if (args[0] instanceof Number num) {
            requestId = num.longValue();
        } else {
            throw new LuaException("requestId must be a number");
        }
        
        double temperature;
        if (args[1] instanceof Number tempNum) {
            temperature = tempNum.doubleValue();
        } else {
            throw new LuaException("temperature must be a number");
        }
        
        CompletableFuture<Double> future = pendingTemperatureRequests.remove(requestId);
        if (future == null) {
            return Map.of(
                "success", false,
                "message", "No pending request found for requestId: " + requestId
            );
        }
        
        future.complete(temperature);
        return Map.of("success", true, "message", "Temperature submitted successfully");
    }

    private static CompletableFuture<Double> requestTemperatureFromLua() {
        long requestId = requestIdCounter.getAndIncrement();
        CompletableFuture<Double> future = new CompletableFuture<>();
        pendingTemperatureRequests.put(requestId, future);
        
        CCIoEventManager.getInstance().triggerEvent(EVENT_TEMPERATURE_REQUEST, requestId);
        
        return future.completeOnTimeout(null, TEMPERATURE_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .whenComplete((result, ex) -> pendingTemperatureRequests.remove(requestId));
    }

    private static Object shutdownRequest(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        if (args == null || args.length < 1) {
            throw new LuaException("dmr.shutdownRequest expects: (code)");
        }

        String inputCode;
        Object codeObj = args[0];
        if (codeObj instanceof String s) {
            inputCode = s;
        } else if (codeObj instanceof Number n) {
            inputCode = String.format("%06d", n.intValue());
        } else {
            throw new LuaException("dmr.shutdownRequest expects code as string or number");
        }

        if (inputCode.length() != 6 || !inputCode.matches("\\d{6}")) {
            return Map.of(
                "success", false,
                "message", "Invalid code format, expected 6-digit number"
            );
        }

        if (!DmrShutdownCodeManager.hasActiveCode()) {
            return Map.of(
                "success", false,
                "message", "No active shutdown window"
            );
        }

        boolean matched = DmrShutdownCodeManager.verifyCode(inputCode);

        if (matched) {
            onShutdownSuccess(level, bridgePos, computer, isNewMusic, haveMusic);
            return Map.of(
                "success", true,
                "message", "Shutdown code accepted! Reactor shutdown initiated."
            );
        } else {
            return Map.of(
                "success", false,
                "message", "Incorrect shutdown code"
            );
        }
    }

    private static void onShutdownSuccess(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, boolean isNewMusic, boolean haveMusic) {
        DmrMeltdown.cancelP1();
        
        Task.spawn(()->{
            Collection<ServerPlayer> allPlayers = level.getServer().getPlayerList().getPlayers();

            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "紧急关机请求已接受.", 200);

            AuralisServerApi.stopSound(MUSIC_ID, allPlayers);
            CommandExecutor.executeCommandAsync("playsound mirage_gfbs:music.shutdowning voice @a ~ ~ ~ 1 1 1");

            Task.delay(()->{
                CompletableFuture<Double> tempFuture = requestTemperatureFromLua();
                try {
                    Double temperature = tempFuture.get();
                    if (temperature != null) {
                        if (temperature < 3000) {
                            // 关机成功
                            shutdown_success(allPlayers, level, isNewMusic, haveMusic);
                        } else {
                            // 关机失败
                            shutdown_failure(allPlayers, level, isNewMusic, haveMusic);
                        }
                    } else {
                        NotificationCommand.sendNotificationToPlayers(allPlayers, "SERVER",
                                "无法获取反应堆温度数据(超时)。", 200);
                        shutdown_failure(allPlayers, level, isNewMusic, haveMusic);
                    }
                } catch (Exception e) {
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "SERVER",
                            "获取温度时发生错误: " + e.getMessage(), 200);
                    shutdown_failure(allPlayers, level, isNewMusic, haveMusic);
                }
            }, 59845, TimeUnit.MILLISECONDS);

            for (ServerPlayer player : allPlayers) {
                CountdownAPI.stop(player);
            }
        });
    }

    private static void shutdown_failure(Collection<ServerPlayer> allPlayers, ServerLevel level, boolean isNewMusic, boolean haveMusic){
        DmrMeltdownEvents.trigger(DmrMeltdownEvents.SHUTDOWN_FAILURE);
        NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                "反应堆紧急停机失败, 请继续疏散该设施内的人员.", 200);

        broadcast("mirage_gfbs:faas_s.f_s_138565");
        Task.sleep(2863);
        broadcast("mirage_gfbs:faas_s.f_s_175682");
        Task.sleep(4265);

        DmrMeltdown.p2(allPlayers, level, isNewMusic, haveMusic);
    }

    private static void shutdown_success(Collection<ServerPlayer> allPlayers, ServerLevel _serverLevel, boolean isNewMusic, boolean haveMusic){
        DmrMeltdownEvents.trigger(DmrMeltdownEvents.SHUTDOWN_SUCCESS);

        Task.delay(()->{
            FluorescentTubeCommandRegistry.turnOnAllTubes(_serverLevel);
            FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.NONE);
            DmrMeltdownEvents.trigger(DmrMeltdownEvents.FACILITY_RESTORE);
        }, 60, TimeUnit.SECONDS);

        NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                "紧急停机系统运行成功, 危机得以化解, 暗物质反应堆正下降至存放舱内以便立即进行维护工作.", 200);
        broadcast("mirage_gfbs:faas.dmr_e_s_s");

        AuralisServerApi.playSound(
                MUSIC_ID,
                ResourceLocation.parse("mirage_gfbs:music.pi_ok2_m"),
                1.0f,
                1.0f,
                1.0f,
                true,
                new Vec3(0, 0, 0),
                false,
                50,
                10.0f,
                10.0f,
                allPlayers
        );

        Task.delay(()->{
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "紧急灭火系统已启用减压程序, 正在排放主水箱及备用水箱.", 200);
            broadcast("mirage_gfbs:faas.efss_start");
        }, 9400,TimeUnit.MILLISECONDS);
    }

    // id参数例如: mirage_gfbs:faas.dmr_o
    private static void broadcast(String id){
        BroadSystemAPI.startBroadcast(id, 1.5f, 1f);
    }

    private static Object getCrackedDigits(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int[] digits = DmrShutdownCodeManager.getCrackedDigits();
        Map<String, Object> result = new HashMap<>();
        result.put("digits", new Object[]{digits[0], digits[1], digits[2], digits[3], digits[4], digits[5]});
        result.put("crackedCount", DmrShutdownCodeManager.getCrackedDigitCount());
        result.put("fullyCracked", DmrShutdownCodeManager.isFullyCracked());
        return result;
    }

    private static Object getCrackedDisplay(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        return Map.of(
            "display", DmrShutdownCodeManager.getCrackedDisplay(),
            "crackedCount", DmrShutdownCodeManager.getCrackedDigitCount(),
            "fullyCracked", DmrShutdownCodeManager.isFullyCracked()
        );
    }

    private static Object hasActiveCode(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        return DmrShutdownCodeManager.hasActiveCode();
    }
}
