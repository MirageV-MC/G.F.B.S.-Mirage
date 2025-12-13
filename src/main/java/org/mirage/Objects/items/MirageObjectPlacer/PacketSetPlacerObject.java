package org.mirage.Objects.items.MirageObjectPlacer;

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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PacketSetPlacerObject(InteractionHand hand, ResourceLocation objectId) {

    public static PacketSetPlacerObject decode(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        ResourceLocation id = buf.readResourceLocation();
        return new PacketSetPlacerObject(hand, id);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeResourceLocation(objectId);
    }

    public static void handle(PacketSetPlacerObject msg, Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        var player = context.getSender();
        if (player == null) return;

        context.enqueueWork(() -> {
            var stack = player.getItemInHand(msg.hand());
            var obj = org.mirage.Encapsulation.MirageObject.MirageObjectRegistry.get(msg.objectId());
            if (obj != null) {
                MirageObjectPlacerItem.setMirageObject(stack, obj);
            }
        });
        context.setPacketHandled(true);
    }
}