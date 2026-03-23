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

package org.mirage.gfbs.objects;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.gfbs.MirageGFBS;
import org.mirage.gfbs.objects.items.BuildItemRegistration;
import org.mirage.gfbs.objects.items.ItemRegistration;
import org.mirage.gfbs.advanced.broadsystem.BroadSystemRegistry;
import org.mirage.gfbs.ccio.CCIoRegistry;

@Mod.EventBusSubscriber(modid = MirageGFBS.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeModeTabRegistration {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MirageGFBS.MODID);
    
    public static final RegistryObject<CreativeModeTab> GFBS_TAB = CREATIVE_MODE_TABS.register("mirage_gfbs_tab", () -> CreativeModeTab.builder()
            .title(Component.literal("GFBS"))
            .icon(() -> new ItemStack(CCIoRegistry.CC_IO_BRIDGE_ITEM.get(),1))
            .displayItems((parameters, output) -> {
            })
            .build());

    public static final RegistryObject<CreativeModeTab> GFBS_BUILD_BLOCK_TAB = CREATIVE_MODE_TABS.register("mirage_gfbs_build_block_tab", () -> CreativeModeTab.builder()
            .title(Component.literal("GFBS QS建筑方块"))
            .icon(() -> new ItemStack(BuildItemRegistration.QS_WALL_ITEM.get(),1))
            .displayItems((parameters, output) -> {
            })
            .build());

    @SubscribeEvent
    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == GFBS_TAB.get()) {
            event.accept(ItemRegistration.FLUORESCENT_TUBE_ITEM.get());
            event.accept(ItemRegistration.RED_ALARM_LAMP_ITEM.get());
            event.accept(ItemRegistration.WHITE_CUBE_LAMP_ITEM.get());
            event.accept(ItemRegistration.ARC_LAMP_ITEM.get());

            event.accept(ItemRegistration.GATE_ITEM.get());
            event.accept(ItemRegistration.CHECK_POINT_GATE_ITEM.get());
            event.accept(ItemRegistration.TARTARUS_GATE_ITEM.get());
            
            event.accept(ItemRegistration.BLUE_DOOR_ITEM.get());
            event.accept(ItemRegistration.RED_DOOR_ITEM.get());
            event.accept(ItemRegistration.BLACK_DOOR_ITEM.get());
            event.accept(ItemRegistration.ORANGE_DOOR_ITEM.get());

            //event.accept(ItemRegistration.RWL_ITEM.get());

            event.accept(ItemRegistration.QS_TRADEMARK_PICTURE_ITEM.get());

            event.accept(BroadSystemRegistry.SPEAKER_BLOCK_ITEM.get());

            event.accept(CCIoRegistry.CC_IO_BRIDGE_ITEM.get());

            event.accept(ItemRegistration.BLACK_HOLE_ITEM.get());
        }

        if (event.getTab() == GFBS_BUILD_BLOCK_TAB.get()) {
            event.accept(BuildItemRegistration.QS_WALL_ITEM.get());
            event.accept(BuildItemRegistration.BRICKWALL_BLUE_ITEM.get());
            event.accept(BuildItemRegistration.BRICKWALL_GRAY_ITEM.get());
            event.accept(BuildItemRegistration.BRICKWALL_OLIVEBROWN_ITEM.get());
            event.accept(BuildItemRegistration.BRICKWALL_P_RED_ITEM.get());
            event.accept(BuildItemRegistration.BRICKWALL_Q_BLUE_ITEM.get());
            event.accept(BuildItemRegistration.BRICKWALL_Q_OLIVEBROWN_ITEM.get());
            event.accept(BuildItemRegistration.BRICKWALL_S_GRAY_ITEM.get());
            event.accept(BuildItemRegistration.BRICKWALL_SP_ED_ITEM.get());
            event.accept(BuildItemRegistration.BRICKWALL_WHITE_ITEM.get());
            event.accept(BuildItemRegistration.FLOOR_BLACK_ITEM.get());
            event.accept(BuildItemRegistration.FLOOR_WHITE_ITEM.get());
            event.accept(BuildItemRegistration.FLOOR_OLIVEBROWN_ITEM.get());
        }
    }
}
