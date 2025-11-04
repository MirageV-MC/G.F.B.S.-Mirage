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

import org.mirage.Command.CameraShakeCommand;
import org.mirage.Command.FogCommand;
import org.mirage.Command.MirageGFBsEventCommand;
import org.mirage.Command.NotificationCommand;
import org.mirage.CommandExecutor;
import org.mirage.Tools.Task;

import java.util.concurrent.TimeUnit;

public class Main90Alpha {

    public static void execute(MirageGFBsEventCommand.CommandContext context){
        CommandExecutor.executeCommand("playsound mirage_gfbs:war.main90_alpha voice @a ~ ~ ~ 1 1 1");

        Task.delay(()->{
            NotificationCommand.sendNotificationToPlayers(context.getSource().getLevel().players(), "C.A.S.S.I.E.", "Alpha弹头引爆程序已启用, 地下设施将在倒计时-90秒后摧毁.", 250);
        }, 39836, TimeUnit.MILLISECONDS);

        Task.delay(()->{
            context.getSource().getLevel().players().forEach(p -> CameraShakeCommand.triggerCameraShake(p, 10f, 0.9f, 5500, 100, 4900));

            // 设置橙色浓雾（RGB: 1.0, 0.5, 0.0）
            FogCommand.setFogSettings(FogCommand.getFogSettings().getBoolean("active"), 1.0f, 0.5f, 0.0f, FogCommand.getFogSettings().getFloat("start"), FogCommand.getFogSettings().getFloat("end"), 1000);
            // 设置浓雾范围（近距离浓雾效果）
            FogCommand.setFogSettings(FogCommand.getFogSettings().getBoolean("active"), FogCommand.getFogSettings().getFloat("red"), FogCommand.getFogSettings().getFloat("green"), FogCommand.getFogSettings().getFloat("blue"), 0.0f, 20.0f, 1000);
            // 启用雾效果
            FogCommand.setFogSettings(true, FogCommand.getFogSettings().getFloat("red"), FogCommand.getFogSettings().getFloat("green"), FogCommand.getFogSettings().getFloat("blue"), FogCommand.getFogSettings().getFloat("start"), FogCommand.getFogSettings().getFloat("end"), 1000);
            // 同步到所有客户端
            FogCommand.setFogSettings(FogCommand.getFogSettings().getBoolean("active"), FogCommand.getFogSettings().getFloat("red"), FogCommand.getFogSettings().getFloat("green"), FogCommand.getFogSettings().getFloat("blue"), FogCommand.getFogSettings().getFloat("start"), FogCommand.getFogSettings().getFloat("end"), 1000);

            startSmoothFogFadeOut();

        }, 141000, TimeUnit.MILLISECONDS);
    }

    private static void startSmoothFogFadeOut() {
        final int fadeSteps = 120; // 增加步数使过渡更平滑
        final long totalFadeTime = 10000; // 延长总时间
        final long stepInterval = totalFadeTime / fadeSteps;

        // 初始橙色值 (RGB: 1.0, 0.5, 0.0)
        final float startRed = 1.0f;
        final float startGreen = 0.5f;
        final float startBlue = 0.0f;

        // 目标颜色 - 改为深红色过渡，更符合爆炸余晖效果
        final float targetRed = 0.3f;
        final float targetGreen = 0.1f;
        final float targetBlue = 0.0f;

        for (int i = 0; i <= fadeSteps; i++) {
            final float progress = easeOutCubic((float) i / fadeSteps); // 使用缓动函数

            int finalI = i;
            Task.delay(()->{
                float currentRed = lerp(startRed, targetRed, progress);
                float currentGreen = lerp(startGreen, targetGreen, progress);
                float currentBlue = lerp(startBlue, targetBlue, progress);

                float startRange = 0.0f;
                float endRange = lerp(20.0f, 200.0f, easeInOutCubic(progress));

                CommandExecutor.executeCommand("miragefog color " + currentRed + " " + currentGreen + " " + currentBlue);
                CommandExecutor.executeCommand("miragefog range " + startRange + " " + endRange);

                // 减少同步频率，只在关键步骤同步
                if (finalI % 5 == 0 || finalI == fadeSteps) {
                    FogCommand.setFogSettings(FogCommand.getFogSettings().getBoolean("active"), FogCommand.getFogSettings().getFloat("red"), FogCommand.getFogSettings().getFloat("green"), FogCommand.getFogSettings().getFloat("blue"), FogCommand.getFogSettings().getFloat("start"), FogCommand.getFogSettings().getFloat("end"), 1000);
                }

                if (finalI == fadeSteps) {
                    Task.delay(()->{
                        for (int j = 0; j <= 10; j++) {
                            float fadeProgress = (float) j / 10;
                            int finalJ = j;
                            Task.delay(()->{
                                float fade = 1.0f - fadeProgress;
                                CommandExecutor.executeCommand("miragefog color " +
                                        targetRed * fade + " " +
                                        targetGreen * fade + " " +
                                        targetBlue * fade);
                                CommandExecutor.executeCommand("miragefog range " +
                                        startRange + " " + (200.0f + 50.0f * fadeProgress));
                                if (finalJ % 2 == 0) {
                                    FogCommand.setFogSettings(FogCommand.getFogSettings().getBoolean("active"), FogCommand.getFogSettings().getFloat("red"), FogCommand.getFogSettings().getFloat("green"), FogCommand.getFogSettings().getFloat("blue"), FogCommand.getFogSettings().getFloat("start"), FogCommand.getFogSettings().getFloat("end"), 1000);
                                }

                                if (finalJ == 10) {
                                    Task.delay(()->{
                                        FogCommand.setFogSettings(false, FogCommand.getFogSettings().getFloat("red"), FogCommand.getFogSettings().getFloat("green"), FogCommand.getFogSettings().getFloat("blue"), FogCommand.getFogSettings().getFloat("start"), FogCommand.getFogSettings().getFloat("end"), 1000);
                                        FogCommand.setFogSettings(FogCommand.getFogSettings().getBoolean("active"), FogCommand.getFogSettings().getFloat("red"), FogCommand.getFogSettings().getFloat("green"), FogCommand.getFogSettings().getFloat("blue"), FogCommand.getFogSettings().getFloat("start"), FogCommand.getFogSettings().getFloat("end"), 1000);
                                    }, 200, TimeUnit.MILLISECONDS);
                                }
                            }, j * 150, TimeUnit.MILLISECONDS);
                        }
                    }, 500, TimeUnit.MILLISECONDS);
                }
            }, (long)(i * stepInterval * getRandomVariation(i)), TimeUnit.MILLISECONDS);
        }
    }

    // 线性插值函数
    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private static float easeOutCubic(float x) {
        return (float)(1 - Math.pow(1 - x, 3));
    }

    private static float easeInOutCubic(float x) {
        return (float)(x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2);
    }

    private static float getRandomVariation(int step) {
        return 0.95f + (float)Math.random() * 0.1f;
    }
}