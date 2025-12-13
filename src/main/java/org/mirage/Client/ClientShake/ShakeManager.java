/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mirage.Client.ClientShake;

public class ShakeManager {
    public static void startShake(float speed, float maxAmplitude, int duration, int riseTime, int fallTime) {
        ClientShakeHandler.resetShake();

        ClientShakeHandler.speed = speed;
        ClientShakeHandler.maxAmplitude = maxAmplitude;
        ClientShakeHandler.duration = duration;
        ClientShakeHandler.riseTime = riseTime;
        ClientShakeHandler.fallTime = fallTime;
        ClientShakeHandler.startTime = System.currentTimeMillis();
    }
}
