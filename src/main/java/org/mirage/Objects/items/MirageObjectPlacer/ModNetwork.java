package org.mirage.Objects.items.MirageObjectPlacer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import static org.mirage.Mirage_gfbs.MODID;

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

public class ModNetwork {

    public static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, "mirage_object_placer_network"))
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .networkProtocolVersion(() -> VERSION)
            .simpleChannel();

    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(id++,
                PacketOpenObjectPlacerMenu.class,
                PacketOpenObjectPlacerMenu::encode,
                PacketOpenObjectPlacerMenu::decode,
                PacketOpenObjectPlacerMenu::handle);

        CHANNEL.registerMessage(id++,
                PacketSetPlacerObject.class,
                PacketSetPlacerObject::encode,
                PacketSetPlacerObject::decode,
                PacketSetPlacerObject::handle);
    }
}