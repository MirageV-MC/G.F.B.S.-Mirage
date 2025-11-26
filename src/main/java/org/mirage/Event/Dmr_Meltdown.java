/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package org.mirage.Event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import org.mirage.Command.FluorescentTubeCommandRegistry;
import org.mirage.Command.MirageGFBsEventCommand;
import org.mirage.Command.NotificationCommand;
import org.mirage.Command.CameraShakeCommand;
import org.mirage.Phenomenon.network.HexCrackerNetwork;
import org.mirage.Tools.Task;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;

import java.sql.Time;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.Collection;

import static org.mirage.CommandExecutor.executeCommandAsync;
import static org.mirage.Mirage_gfbs.server;

public class Dmr_Meltdown {
    private static ServerLevel _serverLevel;

    private static boolean meltdownFlashLoopActive = false;

    public static void execute(MirageGFBsEventCommand.CommandContext context, boolean isNewMusic) {
        _serverLevel = context.getSource().getLevel();
        Task.spawn(()->{
            execute_s(context, isNewMusic);
        });
    }

    private static boolean isRedcode = false;

    public static void execute_s(MirageGFBsEventCommand.CommandContext context, boolean isNewMusic){
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> allPlayers = source.getServer().getPlayerList().getPlayers();

        FluorescentTubeCommandRegistry.turnOnAllTubes(_serverLevel);

        executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_q_b_nb voice @a ~ ~ ~ 1 1 1");

        Task.sleep(40403);

        NeiBao(allPlayers);

        meltdownFlashLoopActive = false;

        startFlashLoop(_serverLevel);

        Task.sleep(2037);

        Task.delay(()->{
            if (isNewMusic){
                executeCommandAsync("playsound mirage_gfbs:music.new_p1_m voice @a ~ ~ ~ 1 1 1");
            }else {
                executeCommandAsync("playsound mirage_gfbs:music.p1_m voice @a ~ ~ ~ 1 1 1");
            }

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:faas.r_c_s_a_t_r voice @a ~ ~ ~ 0.9 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "反应堆核心结构完整性监测系统出现故障, 正在尝试重启.", 200);

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:alarm.dmr_r_i_a voice @a ~ ~ ~ 1 1 1");
                },6183, TimeUnit.MILLISECONDS);

                Task.sleep(13000);
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "严重错误: 重启失败,系统完整性状态未知.", 200);
            }, 15000, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                Task.spawn(()->{
                    isRedcode = false;
                    while (!isRedcode){
                        executeCommandAsync("playsound mirage_gfbs:alarm.portal_a voice @a ~ ~ ~ 99999 1 1");
                        Task.sleep(2803);
                    }
                });
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "警告,所有设施的工作人员,请立即撤离,你们有 11 分钟的时间到达安全的最小距离点.", 200);
                executeCommandAsync("playsound mirage_gfbs:faas.w_a_f_s_d voice @a ~ ~ ~ 0.9 1 1");
            }, 42403, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:human.emergency.c_r_p_e voice @a ~ ~ ~ 1 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "Deputy.Reactor.Supervisor.",
                        "所有设施人员注意，设施自动管理系统已发布红色警报。封锁代码已被指定代码\"Bravo-Niner\"覆盖,请立即前往塔塔鲁斯进行撤离. 这不是演习,我重复,这不是演习.", 400);
            }, 55403, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_663257 voice @a ~ ~ ~ 1 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "暗物质反应堆不稳定, 总电源即将中断.", 200);
                executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b2 voice @a ~ ~ ~ 1 1 1");

                for (ServerPlayer player : allPlayers) {
                    CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 124800, 990, 11290);
                }

                Task.sleep(16000);

                isRedcode = true;

                executeCommandAsync("playsound mirage_gfbs:faas.dmr_i_c_f voice @a ~ ~ ~ 1 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "注意, 暗物质反应堆完整性监测系统故障预处理程序已启动. 红色代码宣布紧急情况, 请立即撤离.", 200);

                Task.sleep(6000);

                executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b voice @a ~ ~ ~ 1 1 1");

                for (ServerPlayer player : allPlayers) {
                    CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 14800, 990, 11290);
                }
            }, 75403, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:human.dmr.s_t_b_e_r_a voice @a ~ ~ ~ 1 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "Safety.Supervisor.",
                        "所有设施人员注意, 在DMR关闭之前一个人不准跑, 否则直接枪毙, 这是你唯一的警告.", 300);
            }, 144541, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                NeiBao(allPlayers);

                Task.sleep(3000);

                executeCommandAsync("playsound mirage_gfbs:alarm.dmr_r_i_a voice @a ~ ~ ~ 1 1 1");
            }, 137616, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "正在计算融毁时间...", 200);
                executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_286753 voice @a ~ ~ ~ 1.2 1 1");
                executeCommandAsync("playsound mirage_gfbs:surroundings.pgr_2 voice @a ~ ~ ~ 1.2 1 1");

                for (ServerPlayer player : allPlayers) {
                    CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 4800, 490, 3290);
                }
            }, 165412, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:faas.dmr_w_s_i_t_m voice @a ~ ~ ~ 0.9 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "危险, DMR融毁在倒计时-10分钟, 关机窗口结束时间为倒计时-5分钟.", 200);
            }, 175008, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:faas.dmr_s_e_s_i_d voice @a ~ ~ ~ 0.9 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "紧急关机窗口已开启, 温度必须低于3000k.", 300);

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_502887 voice @a ~ ~ ~ 1.2 1 1");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "注意, 反应堆操作小组已发出求救信号.", 300);
                }, 14614, TimeUnit.MILLISECONDS);

                Task.delay(()->{
                    FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 40, 3.0D);

                    executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_955935 voice @a ~ ~ ~ 1.2 1 1");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "通信网络出现故障, 正在尝试与东海岸通信基站重新建立连接.", 300);
                }, 24178, TimeUnit.MILLISECONDS);
            }, 190810, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:human.work.f_s_d_r_a_c record @a ~ ~ ~ 10000000 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "Helen.Kate.",
                        "操作员们, F.A.A.S.工程师们正在尝试修复服务器残骸, 以破解关机代码. 留意你们这边的情况.", 300);

                Task.sleep(10000);

                HexCrackerNetwork.triggerOnAll(server);
            }, 226134, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                for (ServerPlayer player : allPlayers) {
                    CameraShakeCommand.triggerCameraShake(player, 15, 0.1f, 2800, 290, 1290);
                }
                executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_749446 voice @a ~ ~ ~ 1 1 1");
            }, 274000, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_476694 voice @a ~ ~ ~ 1.5 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "设施自动化管理系统错误.", 200);
            }, 330993, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                NeiBao(allPlayers);
                executeCommandAsync("playsound mirage_gfbs:faas.faas_a_p voice @a ~ ~ ~ 0.9 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "Severe syste-e-e-m damage-ge-ge-ge shutti-ti-ti-ti-ti-ting do-wn", 200);
                HexCrackerNetwork.stopOnAll(server);
            }, 335993, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:faas.dmr_w_s_i_f_m voice @a ~ ~ ~ 1 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "危险, DMR预计将在倒计时-5分钟后爆炸, 反应堆关机选项现已失效.", 200);
            }, 347000, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:faas.f_b_c_r_t voice @a ~ ~ ~ 1 1 1");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "全体设施人员注意, 已发布黑色紧急指令, 请立即撤离至塔塔鲁斯上层区域.", 200);

                if (isNewMusic){
                    executeCommandAsync("playsound mirage_gfbs:music.new_p2_m voice @a ~ ~ ~ 1 1 1");
                }else {
                    executeCommandAsync("playsound mirage_gfbs:music.p2_m voice @a ~ ~ ~ 1 1 1");
                }

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:faas.f_m_c_s_o voice @a ~ ~ ~ 1 1 1");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "控制系统出现错误, 反应堆控制系统对暗物质反应堆无响应, 处于主控制节点失效状态.", 200);

                    Task.delay(()->{
                        executeCommandAsync("playsound mirage_gfbs:human.dmr.p2 voice @a ~ ~ ~ 1 1 1");
                        NotificationCommand.sendNotificationToPlayers(allPlayers, "Facilities.Supervisor.",
                                "所有反应堆操作小组人员注意, 这是我们阻止DMR彻底破坏的最后机会了, 爬到上层结构, 在1到3秒的时间内依次将所有燃料电池弹出, 以引发燃烧性熄火故障并关闭暗物质反应堆, 你还有1分钟的时间, 祝你好运.", 600);
                    }, 9673 , TimeUnit.MILLISECONDS);
                }, 20523, TimeUnit.MILLISECONDS);

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:faas.dmr_o voice @a ~ ~ ~ 1 1 1");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "暗物质压力清除系统不起作用, 压力持续上升, 上层结构完整性可能进一步遭受损坏.", 200);
                }, 54436, TimeUnit.MILLISECONDS);

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:faas.m_s_f voice @a ~ ~ ~ 1 1 1");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "监控系统故障, 无法预测反应堆爆炸, 代码Omni紧急状态现已发行, 封锁措施将在倒计时-2分钟后实施.", 200);
                }, 66180, TimeUnit.MILLISECONDS);

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:faas.f_e_p_o_n voice @a ~ ~ ~ 1 1 1");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "所有紧急工作装置全部依赖于紧急发电机, 现已降低整体耗电功率.", 200);
                }, 82972, TimeUnit.MILLISECONDS);

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:surroundings.ele_dmr_qbp voice @a ~ ~ ~ 1 1 1");
                    for (ServerPlayer player : allPlayers) {
                        CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 14800, 990, 11290);
                    }

                    Task.sleep(1000);

                    executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_435429 voice @a ~ ~ ~ 1 1 1");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "注意, 在 R.0.洞穴 检测到高剂量辐射.", 200);

                    Task.sleep(5172);
                    executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_326982 voice @a ~ ~ ~ 1 1 1");
                }, 100000, TimeUnit.MILLISECONDS);

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:faas.f_l_b_a voice @a ~ ~ ~ 1 1 1");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "注意, 封锁措施现已启动, 防爆门将在一分钟后关闭.", 200);
                }, 142786, TimeUnit.MILLISECONDS);

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_bom_q_zl voice @a ~ ~ ~ 1 1 1");
                    executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_194506 voice @a ~ ~ ~ 1 1 1");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "Deputy.Reactor.Supervisor.",
                            "所有设施人员注意,我们发现反应堆腔室内泄露出大量辐射,我们正在减少损失并立即关闭塔塔鲁斯大门,所以那些还在设施里的人,请立即前往最近的防爆避难所.", 200);
                }, 189059, TimeUnit.MILLISECONDS);

                Task.delay(()->{
                    for (ServerPlayer player : allPlayers) {
                        CameraShakeCommand.triggerCameraShake(player, 30, 0.1f, 43600, 290, 10290);
                    }
                    executeCommandAsync("playsound mirage_gfbs:boom.dmr_b voice @a ~ ~ ~ 2 1 1");

                    meltdownFlashLoopActive = false;

                    FluorescentTubeCommandRegistry.turnOffAllTubes(_serverLevel);

                    Task.delay(()->{
                        executeCommandAsync("playsound mirage_gfbs:faas_s.f_s_785144 voice @a ~ ~ ~ 1 1 1");
                        NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                                "强引力源出现在核心腔室.", 200);
                    }, 1000, TimeUnit.MILLISECONDS);

                }, 196454, TimeUnit.MILLISECONDS);

            }, 360000, TimeUnit.MILLISECONDS);

        }, 5000, TimeUnit.MILLISECONDS);

        Task.delay(()->{
        }, 27500, TimeUnit.MILLISECONDS);
    }

    private static void NeiBao(Collection<ServerPlayer> players){
        executeCommandAsync("playsound mirage_gfbs:boom.boom_8_what_b voice @a ~ ~ ~ 1 1 1");

        Task.sleep(2150);

        Task.spawn(()->{
            for (ServerPlayer player : players) {
                CameraShakeCommand.triggerCameraShake(player, 15, 0.1f, 1800, 290, 1290);
            }

            Task.delay(()->{
                for (ServerPlayer player : players) {
                    CameraShakeCommand.triggerCameraShake(player, 15, 0.1f, 4800, 490, 3290);
                }
                FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 75, 3.0D);
            },500, TimeUnit.MILLISECONDS);
        });
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