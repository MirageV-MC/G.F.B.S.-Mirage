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

package org.mirage.gfbs.ClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.Mirage_gfbs;

/**
 * Adds a "G.F.B.S." button into the Options screen.
 */
@Mod.EventBusSubscriber(modid = Mirage_gfbs.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientConfigMenuHook {

    private ClientConfigMenuHook() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof OptionsScreen optionsScreen)) return;

        // Avoid duplicates (some screens can re-init)
        for (var w : event.getListenersList()) {
            if (w instanceof Button b) {
                if ("G.F.B.S.".equals(b.getMessage().getString())) return;
            }
        }

        int x = 10;
        int y = optionsScreen.height - 28;
        int w = 120;
        int h = 20;

        Button btn = Button.builder(Component.literal("G.F.B.S."), b -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.setScreen(new GFBSClientConfigScreen(optionsScreen, mc.options));
                })
                .bounds(x, y, w, h)
                .build();

        event.addListener(btn);
    }
}
