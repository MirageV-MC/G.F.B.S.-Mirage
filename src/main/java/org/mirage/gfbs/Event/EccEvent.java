package org.mirage.gfbs.Event;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.mirage.gfbs.Client.ExposureController;
import org.mirage.gfbs.Command.*;
import org.mirage.gfbs.Event.ccio.dmr.DmrMeltdownEvents;
import org.mirage.gfbs.Phenomenon.network.Network.ClientEventHandler;
import org.mirage.gfbs.Phenomenon.network.Network.NetworkHandler;
import org.mirage.gfbs.Tools.CountdownPopup.CountdownEndHooks;
import org.mirage.gfbs.Tools.Task;
import org.mirage.gfbs.api.BroadSystemAPI;
import org.mirage.gfbs.api.CountdownAPI;
import org.mirage.gfbs.auralis.api.AuralisServerApi;

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mirage.gfbs.CommandExecutor.executeCommandAsync;
import static org.mirage.gfbs.MirageGFBS.server;

public class EccEvent {
    private static ServerLevel _serverLevel;

    public static String MUSIC_ID = "ecc_music";
    private static boolean meltdownFlashLoopActive = false;

    public static void execute(MirageGFBsEventCommand.CommandContext context) {
        _serverLevel = context.getSource().getLevel();

        Task.spawn(() -> {
            execute_s(context);
        });
    }

    public static void execute_s(MirageGFBsEventCommand.CommandContext context) {
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> allPlayers = source.getServer().getPlayerList().getPlayers();

        FluorescentTubeCommandRegistry.turnOnAllTubes(_serverLevel);
        FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.NONE);

        meltdownFlashLoopActive = false;

        // 反应堆损坏
        sfx("mirage_gfbs:misc.ecc.reactor.move", 2, allPlayers);
        for (ServerPlayer player : allPlayers) {
            CameraShakeCommand.triggerCameraShake(player, 12, 0.07f, 30000, 1000, 14000);
        }

        Task.delay(()->{
            broadcast("mirage_gfbs:misc.ecc.nias.a_f_p_r_s_c_l");
            notification_nias("所有设施人员注意, 反应堆结构完整性已达到临界状态, 必须立即采取行动以防止出现热失控情况.", allPlayers, 200);
        }, 6560, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            broadcast("mirage_gfbs:misc.ecc.nias.f_c_o_u_c_c_b");
            notification_nias("该设施目前按照第 3 号协议运行: 控制失效.", allPlayers, 200);

            rupture2(allPlayers, true);
        }, 23560, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            rupture(allPlayers);
        }, 41560, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            rupture(allPlayers);
            Task.sleep(11000);

            broadcast("mirage_gfbs:misc.ecc.nias.w_c_m_r_h_l_a_r");
            notification_nias("警告,与 主反应堆监控集群 的连接已断开, 正在尝试获取错误日志.", allPlayers, 200);

            FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.HIGH);
        }, 55560, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            rupture(allPlayers);

            Task.sleep(12200);

            broadcast("mirage_gfbs:misc.ecc.nias.u_r_m_r_c_l");
            notification_nias("无法获取反应堆主监控集群错误日志, 激活次级反应堆监测数据源.", allPlayers, 200);
        }, 80800, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            broadcast("mirage_gfbs:misc.ecc.nias.a_t_a_y_t_e");
            notification_nias("设施人员注意,该反应堆已出现热失控现象, 激活代码3,你们有5分钟撤离此设施, 将当前反应堆数据传输至 外部反应堆应急小组.", allPlayers, 300);
        }, 111000, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            broadcast("mirage_gfbs:misc.ecc.nias.c_r_d_t_e_r_t");
            notification_nias("当前的反应堆数据已传送至外部反应堆应急小组.", allPlayers, 200);
        }, 130800, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            rupture(allPlayers);
        }, 142000, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            broadcast("mirage_gfbs:misc.ecc.nias_e.a_f_p_e_f_s_w_e_s");
            notification_nias_e("设施人员注意,我们从设施自动化系统接收到紧急信号, 我们已发出指令,要求启动反应堆紧急停机程序.", allPlayers, 200);

            for (ServerPlayer player : allPlayers){
                CountdownAPI.popup(player, "NICR 紧急停机过期在 T- ", 5, 0, 0);
            }
        }, 150000, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            // 紧急停机程序窗口
            p1(context, allPlayers);
        }, 165500, TimeUnit.MILLISECONDS);
    }

    public static void p1(MirageGFBsEventCommand.CommandContext context, Collection<ServerPlayer> allPlayers){
        broadcast("mirage_gfbs:misc.ecc.nias.a_f_f_r_h_i_t_m");
        notification_nias("所有设施人员注意,有以下请求: 外部设施应急小组 启动反应堆停机系统的指令已得到执行,如果反应堆温度超过5000k 关机程序将失效,停机窗口过期在 3 分钟.", allPlayers, 300);

        for (ServerPlayer player : allPlayers){
            CountdownAPI.startCountdown(player);
        }

        startFlashLoop(_serverLevel);
        FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 60, 2.0D);

        FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.LOW);

        AuralisServerApi.playSound(
                MUSIC_ID,
                ResourceLocation.parse("mirage_gfbs:misc.ecc.music.shutdown"),
                1,
                1,
                1,
                true,
                new Vec3(0,0,0),
                false,
                10,
                10,
                10,
                allPlayers
        );

        Task.spawn(()->{
            AtomicBoolean isA = new AtomicBoolean(false);

            CountdownEndHooks.register((player)->{
                Task.spawn(()->{
                    if (isA.get()) return;
                    isA.set(true);

                    CountdownEndHooks.unregisterAll();

                    p2(context, allPlayers);
                });
            });
        });
    }

    public static void p2(MirageGFBsEventCommand.CommandContext context, Collection<ServerPlayer> allPlayers) {
        _serverLevel = context.getSource().getLevel();

        AuralisServerApi.playSound(
                MUSIC_ID,
                ResourceLocation.parse("mirage_gfbs:misc.ecc.music.bms"),
                1,
                1,
                1,
                true,
                new Vec3(0, 0, 0),
                false,
                10,
                10,
                10,
                allPlayers
        );

        Task.spawn(() -> {
            broadcast("mirage_gfbs:misc.ecc.nias.r_s_e_f_w_m");
            notification_nias("反应堆紧急停机窗口已关闭,设施即将进入全面封锁状态, 防爆掩体将在3分钟后关闭.", allPlayers, 200);

            server.execute(() -> MirageGFBsGateApiCommand.exec(_serverLevel, true, "gate"));

            Task.sleep(8549);

            broadcast("mirage_gfbs:misc.ecc.nias.f_c_c_e_b_s_o_f");
            notification_nias("该设施目前按照第 5 号协议运行, 灾难发生时必须立即进行疏散, 已要求进行外部备份, 一条紧急信号已自动发送至其他相关设施.", allPlayers, 200);
        });

        // 反应堆结构破坏
        Task.delay(() -> {
            Task.delay(() -> {
                rupture2(allPlayers, false);
            }, 500, TimeUnit.MILLISECONDS);
            AuralisServerApi.playSound(
                    "explosion3",
                    ResourceLocation.parse("mirage_gfbs:misc.ecc.reactor.explosion1"),
                    0.95f,
                    1,
                    1,
                    true,
                    new Vec3(0, 0, 0),
                    false,
                    10,
                    10,
                    10,
                    allPlayers
            );
            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 5, 0.7f, 10000, 1000, 1000);
            }
            Task.sleep(6493);

            sfx("mirage_gfbs:misc.ecc.reactor.explosion3", 1, allPlayers);
            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 20, 0.7f, 5000, 10, 4000);
            }
            meltdownFlashLoopActive = false;

            Task.sleep(1000);
            FluorescentTubeCommandRegistry.turnOffAllTubes(_serverLevel);

            Task.sleep(6000);

            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 0.1f, 0.01f, 999999, 1000, 0);
            }
        }, 42800, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            AuralisServerApi.playSound(
                    "ecc_playing_alarm",
                    ResourceLocation.parse("mirage_gfbs:misc.ecc.reactor.alarm"),
                    0.4f,
                    1,
                    1,
                    true,
                    new Vec3(0, 0, 0),
                    true,
                    10,
                    10,
                    10,
                    allPlayers
            );
        }, 69000, TimeUnit.MILLISECONDS);

        // P2燃料电池弹射方案
        Task.delay(() -> {
            broadcast("mirage_gfbs:misc.ecc.nias_e.a_r_c_e_f_o_a_m_l");
            notification_nias_e("全体设施人员注意,停机系统已经失效,反应堆控制系统不再响应我们的指令,仍然还有一种方法可以拯救设施:进入反应堆舱室,并几乎同时关闭主燃烧流量阀,如果操作失败,融毁将会更具有破坏性,如果成功,你们将被铭记为英雄,这项操作在接下来的两分钟内仍然可以执行,祝好运.", allPlayers, 500);
        }, 70500, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            rupture2(allPlayers, true);
        }, 110000, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            //避难所关闭
            broadcast("mirage_gfbs:misc.ecc.nias.b_s_c_p_c_t_s");
            notification_nias("避难所现已关闭, 自动化语音系统将关闭以在3秒内节省电力.", allPlayers, 200);

            AuralisServerApi.stopSound("ecc_playing_alarm", allPlayers);

            server.execute(() -> {
                MirageGFBsGateApiCommand.exec(_serverLevel, false, "gate");
                MirageGFBsGateApiCommand.exec(_serverLevel, false, "check_point_gate");
                MirageGFBsGateApiCommand.exec(_serverLevel, false, "check_point_gate_x6");
            });
        }, 222500, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            //反应堆彻底融毁
            sfx("mirage_gfbs:misc.ecc.reactor.explosion2", 1.2f, allPlayers);
            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 20, 0.7f, 5000, 10, 4000);
            }

            Task.sleep(7000);

            fl(allPlayers);
        }, 264800, TimeUnit.MILLISECONDS);
    }

    public static void fl(Collection<ServerPlayer> allPlayers){
        NetworkHandler.sendToAll("mirage_ecc_boom_h_event_client_a1");

        Task.sleep(7000);

        for (ServerPlayer player : allPlayers) {
            CameraShakeCommand.stopCameraShake(player);
        }

        NetworkHandler.sendToAll("mirage_ecc_boom_h_event_client_a2");
        server.execute(()->{
            MirageGFBsGateApiCommand.exec(_serverLevel, true, "check_point_gate");
            MirageGFBsGateApiCommand.exec(_serverLevel, true, "check_point_gate_x6");
        });
        FluorescentTubeCommandRegistry.turnOnAllTubes(_serverLevel);
        FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.NONE);
    }

    public static void clientExec(){
        ClientEventHandler.registerEvent("mirage_ecc_boom_h_event_client_a1", (ct)->{
            ExposureController.COLOR_LERP_TIME_SEC = 0f;
            ExposureController.setExposure(0f, Color.BLACK);
            Task.delay(() -> {
                ExposureController.COLOR_LERP_TIME_SEC = 5f;
                ExposureController.setExposure(1f, Color.BLACK);
            }, 50, TimeUnit.MILLISECONDS);
        });
        ClientEventHandler.registerEvent("mirage_ecc_boom_h_event_client_a2", (ct)->{
            ExposureController.COLOR_LERP_TIME_SEC = 3f;
            ExposureController.setExposure(0f, Color.BLACK);
        });
    }

    //// tools

    // id参数例如: mirage_gfbs:faas.dmr_o
    private static void broadcast(String id) {BroadSystemAPI.startBroadcast(id, 1.5f, 1f);}

    private static void sfx(String id, float volume, Collection<ServerPlayer> allPlayers){
        AuralisServerApi.playSound(
                "ecc_playing_sfx",
                ResourceLocation.parse(id),
                volume,
                1,
                1,
                true,
                new Vec3(0,0,0),
                false,
                10,
                10,
                10,
                allPlayers
        );
    }

    private static void rupture(Collection<ServerPlayer> allPlayers){
        sfx("mirage_gfbs:misc.ecc.reactor.rupture", 1, allPlayers);
        for (ServerPlayer player : allPlayers) {
            CameraShakeCommand.triggerCameraShake(player, 15, 0.2f, 1700, 50, 1500);
        }
        FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 60, 2.0D);
    }
    private static void rupture2(Collection<ServerPlayer> allPlayers, boolean AutoShake){
        sfx("mirage_gfbs:misc.ecc.reactor.rupture2", 1, allPlayers);
        if(AutoShake){
            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 12, 0.05f, 7000, 400, 3000);
            }
        }
    }

    private static void notification_nias(String msg, Collection<ServerPlayer> allPlayers, int time){
        NotificationCommand.sendNotificationToPlayers(allPlayers, "N.I.A.S.",
                msg, time);
    }

    private static void notification_nias_e(String msg, Collection<ServerPlayer> allPlayers, int time){
        NotificationCommand.sendNotificationToPlayers(allPlayers, "Administrative.Staff.",
                msg, time);
    }

    private static void startFlashLoop(ServerLevel serverLevel) {
        if (meltdownFlashLoopActive) {
            return;
        }

        meltdownFlashLoopActive = true;

        Thread thread = new Thread(() -> {
            while (meltdownFlashLoopActive) {
                int delayMs = ThreadLocalRandom.current().nextInt(20_000, 30_001);

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (!meltdownFlashLoopActive) {
                    break;
                }

                serverLevel.getServer().execute(() -> {
                    if (!meltdownFlashLoopActive) return;

                    FluorescentTubeCommandRegistry.flashAllTubes(serverLevel, 60, 2.0D);
                });
            }
        }, "dmr-meltdown-flash-loop");

        thread.setDaemon(true);
        thread.start();
    }
}