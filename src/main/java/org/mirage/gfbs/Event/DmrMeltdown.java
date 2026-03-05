/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC

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

package org.mirage.gfbs.Event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.mirage.gfbs.Command.*;
import org.mirage.gfbs.ModSoundEvents;
import org.mirage.gfbs.Phenomenon.network.HexCrackerNetwork;
import org.mirage.gfbs.Tools.CountdownPopup.CountdownEndHooks;
import org.mirage.gfbs.Tools.Task;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import org.mirage.gfbs.api.BroadSystemAPI;
import org.mirage.gfbs.api.CountdownAPI;
import org.mirage.gfbs.auralis.api.AuralisApi;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.mirage.gfbs.auralis.api.AuralisServerApi;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mirage.gfbs.CommandExecutor.executeCommandAsync;
import static org.mirage.gfbs.MirageGFBS.server;

public class DmrMeltdown {
    private static ServerLevel _serverLevel;

    private static boolean meltdownFlashLoopActive = false;

    public static void execute(MirageGFBsEventCommand.CommandContext context, boolean isNewMusic, boolean haveMusic) {
        _serverLevel = context.getSource().getLevel();
        Task.spawn(()->{
            execute_s(context, isNewMusic, haveMusic);
        });
    }

    private static boolean isRedcode = false;
    private static boolean isP1Evec = false;

    public static void execute_s(MirageGFBsEventCommand.CommandContext context, boolean isNewMusic, boolean haveMusic){
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> allPlayers = source.getServer().getPlayerList().getPlayers();

        FluorescentTubeCommandRegistry.turnOnAllTubes(_serverLevel);
        FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.NONE);

        executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_q_b_nb voice @a ~ ~ ~ 1 1 1");

        Task.sleep(40403);

        implosion(allPlayers, true);

        meltdownFlashLoopActive = false;

        startFlashLoop(_serverLevel);
        FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.LOW);

        Task.sleep(2037);

        Task.delay(()->{
            if (haveMusic) {
                if (isNewMusic){
                    Task.sleep(300);
                    AuralisServerApi.playSound(
                            "sound_id",
                            ResourceLocation.parse("mirage_gfbs:music.new_p1_m"),
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
                }else {
                    executeCommandAsync("playsound mirage_gfbs:music.p1_m voice @a ~ ~ ~ 1 1 1");
                }
            }

            Task.delay(()->{
                broadcast("mirage_gfbs:faas.r_c_s_a_t_r");
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
                broadcast("mirage_gfbs:faas_s.f_s_228127");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "暗物质反应堆压力监测系统故障, 压力未知.", 200);

                Task.sleep(12000);

                Task.spawn(()->{
                    isRedcode = false;
                    while (!isRedcode){
                        executeCommandAsync("playsound mirage_gfbs:alarm.portal_a voice @a ~ ~ ~ 99999 1 1");
                        Task.sleep(2803);
                    }
                });
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "警告,所有设施的工作人员,请立即撤离,你们有 11 分钟的时间到达安全的最小距离点.", 200);
                broadcast("mirage_gfbs:faas.w_a_f_s_d");
            }, 42403, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                broadcast("mirage_gfbs:human.emergency.c_r_p_e");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "Facility.Supervisor.",
                        "所有设施人员注意，设施自动管理系统已发布红色警报。封锁代码已被指定代码\"Bravo-Niner\"覆盖,请立即前往塔塔鲁斯进行撤离. 这不是演习,我重复,这不是演习.", 400);
            }, 67403, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                broadcast("mirage_gfbs:faas_s.f_s_663257");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "暗物质反应堆不稳定, 总电源即将中断.", 200);
                executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b2 voice @a ~ ~ ~ 1 1 1");

                for (ServerPlayer player : allPlayers) {
                    CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 124800, 990, 11290);
                }

                Task.sleep(16000); //old:18000

                isRedcode = true;

                broadcast("mirage_gfbs:faas.dmr_i_c_f");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "注意, 暗物质反应堆完整性监测系统故障预处理程序已启动. 红色代码宣布紧急情况, 请立即撤离.", 200);

                Task.sleep(9000);

                executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b voice @a ~ ~ ~ 1 1 1");

                for (ServerPlayer player : allPlayers) {
                    CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 14800, 990, 11290);
                }
            }, 112403, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                broadcast("mirage_gfbs:human.dmr.s_t_b_e_r_a");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "Facility.Supervisor.",
                        "所有反应堆控制室工作人员注意, 你们接到指示, 在撤离设施之前必须尝试关闭反应堆, 如果选择逃离, 你们将被立即处决. 这是你们唯一警告.", 300);
            }, 144541, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                implosion(allPlayers, true);

                Task.sleep(2500);

                executeCommandAsync("playsound mirage_gfbs:alarm.dmr_r_i_a voice @a ~ ~ ~ 1 1 1");
            }, 137816, TimeUnit.MILLISECONDS); //old:137516

            Task.delay(()->{
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "正在计算融毁时间...", 200);
                broadcast("mirage_gfbs:faas_s.f_s_286753");
                executeCommandAsync("playsound mirage_gfbs:surroundings.pgr_2 voice @a ~ ~ ~ 1.2 1 1");

                for (ServerPlayer player : allPlayers) {
                    CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 6800, 490, 3290);
                }
            }, 165412, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                broadcast("mirage_gfbs:faas.dmr_w_s_i_t_m");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "危险, DMR融毁在倒计时-10分钟, 关机窗口结束时间为倒计时-5分钟.", 200);

                for (ServerPlayer player : allPlayers){
                    CountdownAPI.popup(player, "DMR 关机窗口过期在 T- ", 2, 7, 0);
                }
            }, 175008, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                broadcast("mirage_gfbs:faas.dmr_s_e_s_i_d");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "紧急关机窗口已开启, 温度必须低于3000k.", 300);

                Task.spawn(()->{
                    for (ServerPlayer player : allPlayers){
                        CountdownAPI.startCountdown(player);
                    }
                });

                Task.delay(()->{
                    broadcast("mirage_gfbs:faas_s.f_s_502887");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "注意, 反应堆操作小组已发出求救信号.", 300);

                    Task.delay(()->{
                        explosion(3, true);
                    }, 6473, TimeUnit.MILLISECONDS);
                }, 14614, TimeUnit.MILLISECONDS);

                Task.delay(()->{
                    FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 40, 3.0D);

                    broadcast("mirage_gfbs:faas_s.f_s_955935");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "通信网络出现故障, 正在尝试与东海岸通信基站重新建立连接.", 300);
                }, 24178, TimeUnit.MILLISECONDS);
            }, 190810, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                broadcast("mirage_gfbs:human.work.f_s_d_r_a_c");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "Helen.Kate.",
                        "操作员们, F.A.A.S.工程师们正在尝试修复服务器残骸, 以破解关机代码. 留意你们这边的情况.", 300);

                Task.sleep(10000);

                HexCrackerNetwork.triggerOnAll(server);

                Task.sleep(7500);

                explosion(1, true);

                Task.delay(()->{
                    executeCommandAsync("playsound mirage_gfbs:alarm.a2.warning_a voice @a ~ ~ ~ 1 1 1");

                    Task.sleep(7054);

                    broadcast("mirage_gfbs:faas.f2.d_r_l_e_m_s_l_r_e");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "危险, 辐射水平已超过进入反应堆舱室的最高安全限值.", 200);

                    Task.sleep(7065);

                    explosion(3, true);
                    broadcast("mirage_gfbs:faas.f2.w_f_i_c_p_e_o_f_i");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "警告, 设施完整性受损, 请立即进入避难所或撤离设施.", 200);

                    FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.HIGH);

                    Task.sleep(9270);

                    broadcast("mirage_gfbs:faas.f2.a_f_s_p_p_e_i");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "所有设施科学人员, 请立即疏散.", 200);

                    Task.spawn(()->{
                        isP1Evec = false;
                        while (!isP1Evec){
                            executeCommandAsync("playsound mirage_gfbs:alarm.portal_a voice @a ~ ~ ~ 99999 1 1");
                            Task.sleep(2803);
                        }
                    });

                    Task.sleep(13000);

                    explosion(3, true);
                    executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b3 voice @a ~ ~ ~ 1 1 1");
                    broadcast("mirage_gfbs:faas.f2.a_f_m_p_p_r_s_i");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "所有设施维护人员注意, 请立即报道当前状况.", 200);
                }, 2000, TimeUnit.MILLISECONDS);

            }, 226134, TimeUnit.MILLISECONDS);

            Task.delay(()->{
                explosion(1, true);
                broadcast("mirage_gfbs:faas_s.f_s_476694");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "设施自动化管理系统错误.", 200);
            }, 276000, TimeUnit.MILLISECONDS);

            Task.delay(()->{
            }, 323144, TimeUnit.MILLISECONDS);

            Task.delay(()->{
            }, 317144, TimeUnit.MILLISECONDS);

            Task.spawn(()->{
                AtomicBoolean isA = new AtomicBoolean(false);

                CountdownEndHooks.register((player)->{
                    Task.spawn(()->{
                        if (isA.get()) return;
                        isA.set(true);

                        CountdownEndHooks.unregisterAll();

                        broadcast("mirage_gfbs:faas_s.f_s_749446");
                        isP1Evec = true;

                        Task.sleep(6100);

                        implosion(allPlayers, false);

                        CountdownEndHooks.resetAll();

                        broadcast("mirage_gfbs:faas_s.f_s_476694");
                        NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                                "设施自动化管理系统错误.", 200);

                        Task.sleep(4428);
                    
                        FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.LOW);

                        executeCommandAsync("playsound mirage_gfbs:alarm.dmr_r_i_a voice @a ~ ~ ~ 1 1 1");

                        broadcast("mirage_gfbs:faas.faas_a_p");
                        NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                                "Severe syste-e-e-m damage-ge-ge-ge shutti-ti-ti-ti-ti-ting do-wn", 200);
                        HexCrackerNetwork.stopOnAll(server);

                        Task.sleep(7151);

                        executeCommandAsync("playsound mirage_gfbs:alarm.a2.warning_a voice @a ~ ~ ~ 1 1 1");

                        Task.sleep(6554);

                        broadcast("mirage_gfbs:faas.dmr_w_s_i_f_m");
                        NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                                "危险, DMR预计将在倒计时-5分钟后爆炸, 反应堆关机选项现已失效.", 200);

                        Task.sleep(13000);

                        p2(allPlayers, _serverLevel, isNewMusic, haveMusic);
                    });
                });
            });

        }, 5000, TimeUnit.MILLISECONDS);

        Task.delay(()->{
        }, 27500, TimeUnit.MILLISECONDS);
    }

    public static void p2(Collection<ServerPlayer> allPlayers, ServerLevel level, boolean isNewMusic, boolean haveMusic) {
        _serverLevel = level;

        broadcast("mirage_gfbs:faas.f_b_c_r_t");
        NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                "全体设施人员注意, 已发布黑色紧急指令, 请立即撤离至塔塔鲁斯上层区域.", 200);

        if (haveMusic){
            if (isNewMusic){
                Task.sleep(300);
                AuralisServerApi.playSound(
                        "sound_id",
                        ResourceLocation.parse("mirage_gfbs:music.new_p2_m"),
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
            }else {
                executeCommandAsync("playsound mirage_gfbs:music.p2_m voice @a ~ ~ ~ 1 1 1");
            }
        }

        Task.spawn(()->{
            executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b voice @a ~ ~ ~ 1 1 1");

            Task.sleep(20000);

            executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b2 voice @a ~ ~ ~ 1 1 1");

            Task.sleep(50000);
            executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b3 voice @a ~ ~ ~ 1 1 1");
        });

        Task.delay(()->{
           Task.sleep(3500);
           broadcast("mirage_gfbs:faas.f2.a_dmr_o_t_p_e_f_i");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "暗物质反应堆操作小组注意, 请立即撤离该设施.", 200);
           explosion(2, true);
        }, 7577, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            broadcast("mirage_gfbs:faas.f_m_c_s_o");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "控制系统出现错误, 反应堆控制系统对暗物质反应堆无响应, 处于主控制节点失效状态.", 200);

            Task.delay(()->{
                broadcast("mirage_gfbs:human.dmr.p2");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "Facilities.Supervisor.",
                        "所有反应堆操作小组人员注意, 这是我们阻止DMR彻底破坏的最后机会了, 爬到上层结构, 在1到3秒的时间内依次将所有燃料电池弹出, 以引发燃烧性熄火故障并关闭暗物质反应堆, 你还有1分钟的时间, 祝你好运.", 600);

                Task.delay(()->{
                    server.execute(()->MirageGFBsGateApiCommand.exec(_serverLevel, true, "gate"));

                    broadcast("mirage_gfbs:faas_s.f_s_535533");
                    NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                            "所有设施人员注意, 请立即前往最近的避[数据删除]难所.", 200);

                    executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b2 voice @a ~ ~ ~ 1 1 1");

                    for (ServerPlayer player : allPlayers) {
                        CameraShakeCommand.triggerCameraShake(player, 15, 0.1f, 4800, 490, 3290);
                    }
                    FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 75, 3.0D);

                    Task.sleep(4997);

                    explosion(3, true);
                }, 85, TimeUnit.SECONDS);
            }, 9673 , TimeUnit.MILLISECONDS);
        }, 20523, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            broadcast("mirage_gfbs:faas.dmr_o");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "暗物质压力清除系统不起作用, 压力持续上升, 上层结构完整性可能进一步遭受损坏.", 200);
        }, 54436, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            broadcast("mirage_gfbs:faas.m_s_f");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "监控系统故障, 无法预测反应堆爆炸, 代码Omni紧急状态现已发行, 封锁措施将在倒计时-2分钟后实施.", 200);
        }, 66180, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            executeCommandAsync("playsound mirage_gfbs:surroundings.chj_sh_s voice @a ~ ~ ~ 1 1 1");

            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 14800, 290, 11290);
            }
            FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 75, 3.0D);
        }, 76180, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            broadcast("mirage_gfbs:faas.f_e_p_o_n");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "所有紧急工作装置全部依赖于紧急发电机, 现已降低整体耗电功率.", 200);

            explosion(1, false);

            executeCommandAsync("playsound mirage_gfbs:hybrid.meltdown_a_b voice @a ~ ~ ~ 1 1 1");
            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 14800, 290, 11290);
            }

            FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.HIGH);

            Task.sleep(17060);
            executeCommandAsync("playsound mirage_gfbs:boom.boom_b voice @a ~ ~ ~ 1 1 1");
            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 14800, 290, 11290);
            }

        }, 82972, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            executeCommandAsync("playsound mirage_gfbs:surroundings.ele_dmr_qbp voice @a ~ ~ ~ 1 1 1");
            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 16, 0.05f, 14800, 990, 11290);
            }

            Task.sleep(1000);

            broadcast("mirage_gfbs:faas_s.f_s_435429");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "注意, 在 R.0.洞穴 检测到高剂量辐射.", 200);

            Task.sleep(5172);
            broadcast("mirage_gfbs:faas_s.f_s_326982");
        }, 100000, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            broadcast("mirage_gfbs:faas.f_l_b_a");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "注意, 封锁措施现已启动, 防爆门将在一分钟后关闭.", 200);

            Task.delay(()->{
                explosion(1, true);

                broadcast("mirage_gfbs:faas_s.f_s_476694");
                NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                        "设施自动化管理系统错误.", 200);
            }, 7705, TimeUnit.MILLISECONDS);

            Task.sleep(30000);

            broadcast("mirage_gfbs:faas_s.f_s_148446");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "注意, 防爆门将在30秒后关闭.", 200);

            Task.sleep(5983);

            explosion(3, true);

            Task.sleep(2400);
            executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_up_nb_b3 voice @a ~ ~ ~ 1 1 1");
        }, 142786, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_bom_q_zl voice @a ~ ~ ~ 1 1 1");
            broadcast("mirage_gfbs:faas_s.f_s_194506");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "Deputy.Reactor.Supervisor.",
                    "所有设施人员注意,我们发现反应堆腔室内泄露出大量辐射,我们正在减少损失并立即关闭塔塔鲁斯大门,所以那些还在设施里的人,请立即前往最近的防爆避难所.", 200);

            server.execute(()->{
                MirageGFBsGateApiCommand.exec(_serverLevel, false, "gate");
                MirageGFBsGateApiCommand.exec(_serverLevel, false, "check_point_gate");
            });
        }, 189059, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 30, 0.3f, 43600, 290, 10290);
            }
            executeCommandAsync("playsound mirage_gfbs:boom.dmr_b voice @a ~ ~ ~ 2 1 1");

            meltdownFlashLoopActive = false;

            FluorescentTubeCommandRegistry.turnOffAllTubes(_serverLevel);

            broadcast("mirage_gfbs:faas_s.f_s_785144");
            NotificationCommand.sendNotificationToPlayers(allPlayers, "F.A.A.S.",
                    "强引力源出现在核心腔室.", 200);

            Task.delay(()->{
                executeCommandAsync("playsound mirage_gfbs:surroundings.dmr_bh voice @a ~ ~ ~ 1 1 1");

                broadcast("mirage_gfbs:hybrid.faas_np");
            }, 1899, TimeUnit.MILLISECONDS);

            Task.spawn(()->{
                Task.sleep(42047);
                DmrexAfter.exec(allPlayers, _serverLevel);
            });

        }, 198788, TimeUnit.MILLISECONDS);
    }

    // id参数例如: mirage_gfbs:faas.dmr_o
    private static void broadcast(String id){
        BroadSystemAPI.startBroadcast(id, 1.5f, 1f);
    }

    private static void implosion(Collection<ServerPlayer> players, boolean haveAlarm){
        if (haveAlarm){
            executeCommandAsync("playsound mirage_gfbs:boom.boom_8_what_b voice @a ~ ~ ~ 1 1 1");
        }

        Task.sleep(2150);

        if (!haveAlarm){
            executeCommandAsync("playsound mirage_gfbs:boom.boom_7_what_b voice @a ~ ~ ~ 1 1 1");
        }

        Task.spawn(()->{
            for (ServerPlayer player : players) {
                CameraShakeCommand.triggerCameraShake(player, 15, 0.1f, 1800, 290, 1290);
            }

            Task.delay(()->{
                for (ServerPlayer player : players) {
                    CameraShakeCommand.triggerCameraShake(player, 15, 0.1f, 4800, 490, 3290);
                }
                FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 75, 6D);
            },500, TimeUnit.MILLISECONDS);
        });
    }

    private static void explosion(int b, boolean autoShake) {
        if (b == 1){
            executeCommandAsync("playsound mirage_gfbs:surroundings.s2.dmr_m_p2_b1 voice @a ~ ~ ~ 1 1 1");
        }
        if (b == 2){
            executeCommandAsync("playsound mirage_gfbs:surroundings.s2.dmr_m_p2_b2 voice @a ~ ~ ~ 1 1 1");
        }
        if (b == 3){
            executeCommandAsync("playsound mirage_gfbs:surroundings.s2.dmr_m_p2_b3 voice @a ~ ~ ~ 1 1 1");
        }
        if (autoShake){
            for (ServerPlayer player : _serverLevel.players()) {
                // ShakeQsClient.sendShake(player, 26f, 0.09f);
                CameraShakeCommand.triggerCameraShake(player, 26, 0.09f, 4800, 10, 4290);
            }
        }
        FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 75, 3.0D);
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