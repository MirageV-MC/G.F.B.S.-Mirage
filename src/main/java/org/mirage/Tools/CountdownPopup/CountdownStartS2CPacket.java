package org.mirage.Tools.CountdownPopup;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
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

public final class CountdownStartS2CPacket {

    private final long endGameTime;

    public CountdownStartS2CPacket(long endGameTime) {
        this.endGameTime = endGameTime;
    }

    public long getEndGameTime() {
        return endGameTime;
    }

    public static void encode(CountdownStartS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarLong(msg.endGameTime);
    }

    public static CountdownStartS2CPacket decode(FriendlyByteBuf buf) {
        long end = buf.readVarLong();
        return new CountdownStartS2CPacket(end);
    }

    public static void handle(CountdownStartS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> CountdownPopupClient.get().startCountdown(msg.endGameTime)
        ));
        ctx.get().setPacketHandled(true);
    }
}
