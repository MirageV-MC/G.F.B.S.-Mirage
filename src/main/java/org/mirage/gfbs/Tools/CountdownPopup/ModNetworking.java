package org.mirage.gfbs.Tools.CountdownPopup;

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

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mirage.gfbs.MirageGFBS;

public final class ModNetworking {
    private ModNetworking() {}

    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    public static void init() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MirageGFBS.MODID, "gfbs_cp_net"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );

        int id = 0;
        CHANNEL.messageBuilder(CountdownPopupS2CPacket.class, id++)
                .encoder(CountdownPopupS2CPacket::encode)
                .decoder(CountdownPopupS2CPacket::decode)
                .consumerMainThread(CountdownPopupS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(CountdownStartS2CPacket.class, id++)
                .encoder(CountdownStartS2CPacket::encode)
                .decoder(CountdownStartS2CPacket::decode)
                .consumerMainThread(CountdownStartS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(CountdownStopS2CPacket.class, id++)
                .encoder(CountdownStopS2CPacket::encode)
                .decoder(CountdownStopS2CPacket::decode)
                .consumerMainThread(CountdownStopS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(CountdownEndedC2SPacket.class, id++)
                .encoder(CountdownEndedC2SPacket::encode)
                .decoder(CountdownEndedC2SPacket::decode)
                .consumerMainThread(CountdownEndedC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(CountdownEndS2CPacket.class, id++)
                .encoder(CountdownEndS2CPacket::encode)
                .decoder(CountdownEndS2CPacket::decode)
                .consumerMainThread(CountdownEndS2CPacket::handle)
                .add();
    }
}