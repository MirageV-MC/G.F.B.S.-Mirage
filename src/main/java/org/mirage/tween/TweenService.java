package org.mirage.tween;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class TweenService {
    private final List<Tween> tweens = new ArrayList<>();

    public Tween create(DoubleSupplier getter, DoubleConsumer setter, TweenInfo info, double targetValue) {
        Tween t = new Tween(info, getter, setter, targetValue);
        tweens.add(t);
        return t;
    }

    public void tick(double dtSeconds) {
        if (tweens.isEmpty()) return;

        Iterator<Tween> it = tweens.iterator();
        while (it.hasNext()) {
            Tween t = it.next();
            boolean alive = t.tick(dtSeconds);
            if (!alive) it.remove();
        }
    }

    public void cancelAll() {
        for (Tween t : tweens) t.cancel();
        tweens.clear();
    }

    public int size() {
        return tweens.size();
    }
}
