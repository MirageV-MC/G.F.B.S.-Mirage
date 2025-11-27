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

package org.mirage.Objects.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.Objects.blocks.BlockRegistration;

import static org.mirage.Mirage_gfbs.ITEMS;

public class ItemRegistration {
    public static final RegistryObject<Item> DARK_MATTER_REACTOR_ITEM =
            ITEMS.register("darkmatterreactor",
                    () -> new BlockItem(BlockRegistration.DARK_MATTER_REACTOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> FLUORESCENT_TUBE_ITEM =
            ITEMS.register("fluorescent_tube",
                    () -> new BlockItem(BlockRegistration.FLUORESCENT_TUBE.get(), new Item.Properties()));

    public static final RegistryObject<Item> RED_ALARM_LAMP_ITEM =
            ITEMS.register("red_alarm_lamp",
                    () -> new BlockItem(BlockRegistration.RED_ALARM_LAMP.get(), new Item.Properties()));

    public static final RegistryObject<Item> WHITE_CUBE_LAMP_ITEM =
            ITEMS.register("white_cube_lamp",
                    () -> new BlockItem(BlockRegistration.WHITE_CUBE_LAMP.get(), new Item.Properties()));

    public static void init(){}
}
