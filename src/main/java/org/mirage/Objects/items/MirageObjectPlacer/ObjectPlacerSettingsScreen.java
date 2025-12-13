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

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.mirage.Encapsulation.MirageObject.MirageObject;
import org.mirage.Encapsulation.MirageObject.MirageObjectRegistry;

public class ObjectPlacerSettingsScreen extends Screen {

    private final InteractionHand hand;

    public ObjectPlacerSettingsScreen(InteractionHand hand) {
        super(Component.literal("选择要放置的 MirageObject"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        int y = 40;

        for (MirageObject obj : MirageObjectRegistry.values()) {

            Component label = Component.literal(obj.getId().toString());

            this.addRenderableWidget(
                    Button.builder(label, b -> {
                        select(obj);
                    }).bounds(20, y, 250, 20).build()
            );

            y += 25;
        }
    }

    private void select(MirageObject obj) {

        ModNetwork.CHANNEL.sendToServer(
                new PacketSetPlacerObject(hand, obj.getId())
        );

        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}