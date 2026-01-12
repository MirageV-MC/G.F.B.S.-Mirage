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

package org.mirage.gfbs.Phenomenon.network.packets.BlackHole;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.gfbs.Phenomenon.BlackHole.BlackHoleManager;

import java.util.function.Supplier;

public class BlackHoleCreatePacket {
    private final String name;
    private final Vec3 position;
    private final double radius;
    private final double lensing;

    public BlackHoleCreatePacket(String name, Vec3 position, double radius, double lensing) {
        this.name = name;
        this.position = position;
        this.radius = radius;
        this.lensing = lensing;
    }

    public BlackHoleCreatePacket(FriendlyByteBuf buf) {
        this.name = buf.readUtf();
        this.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.radius = buf.readDouble();
        this.lensing = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeDouble(radius);
        buf.writeDouble(lensing);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            BlackHoleManager.createBlackHole(name, radius, lensing, position);
        });
        return true;
    }
}