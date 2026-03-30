package org.mirage.gfbs.Client.ClientShake;

import java.util.concurrent.atomic.AtomicBoolean;

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

public final class ShakeAnimationThread {
    private static final long FRAME_TIME_NS = 8_333_333L;
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static Thread animationThread;

    private ShakeAnimationThread() {}

    public static void start() {
        if (running.compareAndSet(false, true)) {
            animationThread = new Thread(() -> {
                while (running.get()) {
                    try {
                        long startNs = System.nanoTime();

                        ClientShakeHandler.updateAllShakes();

                        long elapsed = System.nanoTime() - startNs;
                        long sleepNs = FRAME_TIME_NS - elapsed;
                        if (sleepNs > 0) {
                            Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "ShakeController-Animation");
            animationThread.setDaemon(true);
            animationThread.start();
        }
    }

    public static void stop() {
        running.set(false);
        if (animationThread != null) {
            animationThread.interrupt();
            animationThread = null;
        }
    }

    public static boolean isRunning() {
        return running.get();
    }
}
