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

package org.mirage.Objects;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.Mirage_gfbs;
import org.mirage.Objects.blocks.BlockRegistration;
import org.mirage.Objects.items.ItemRegistration;

@Mod.EventBusSubscriber(modid = Mirage_gfbs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeModeTabRegistration {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mirage_gfbs.MODID);
    
    public static final RegistryObject<CreativeModeTab> GFBS_TAB = CREATIVE_MODE_TABS.register("mirage_gfbs_tab", () -> CreativeModeTab.builder()
            .title(Component.literal("G.F.B.S."))
            .icon(() -> new ItemStack(BlockRegistration.DARK_MATTER_REACTOR_BLOCK.get(),1))
            .displayItems((parameters, output) -> {
            })
            .build());

    @SubscribeEvent
    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == GFBS_TAB.get()) {
            event.accept(ItemRegistration.DARK_MATTER_REACTOR_ITEM.get());
            event.accept(ItemRegistration.FLUORESCENT_TUBE_ITEM.get());
            event.accept(ItemRegistration.RED_ALARM_LAMP_ITEM.get());
            event.accept(ItemRegistration.WHITE_CUBE_LAMP_ITEM.get());
        }
    }
}
