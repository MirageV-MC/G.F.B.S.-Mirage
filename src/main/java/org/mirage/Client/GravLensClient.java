/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524

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

package org.mirage.Client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.Mirage_gfbs;

import javax.swing.*;

import static org.mirage.Mirage_gfbs.MODID;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MODID)
public class GravLensClient {
    private static boolean oculusPresent;

    static {
        try {
            Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            oculusPresent = true;
        } catch (ClassNotFoundException e) {
            oculusPresent = false;
        }
    }

    private static PostChain gravLensChain;

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        try {
            gravLensChain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getMainRenderTarget(),
                    new ResourceLocation(MODID, "shaders/post/grav_lens.json")
            );
        } catch (Exception e) {
            System.out.println("[GravLens] Failed to load shader: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render event) {
        if (!shouldRunGravLens()) return;
        if (gravLensChain == null) return;
        gravLensChain.process(event.getPartialTick());
    }

    public static boolean shouldRunGravLens() {
        if (!oculusPresent) return true;
        return false;
    }
}
