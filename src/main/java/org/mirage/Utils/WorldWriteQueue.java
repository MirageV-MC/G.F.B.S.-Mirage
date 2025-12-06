package org.mirage.Utils;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber
public class WorldWriteQueue {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    public static void enqueue(Runnable task) {
        if (task == null) return;
        TASKS.add(task);
    }

    public static void flush() {
        Runnable task;
        while ((task = TASKS.poll()) != null) {
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.error("Exception while executing world write task", t);
            }
        }
    }
}
