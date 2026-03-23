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

package org.mirage.gfbs.objects.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.gfbs.objects.blocks.BlockRegistration;
import org.mirage.gfbs.objects.items.ColoredDoor.ColoredDoorGeoItem;
import net.minecraft.resources.ResourceLocation;

import static org.mirage.gfbs.MirageGFBS.ITEMS;

public class ItemRegistration {
    // 工具
    private static RegistryObject<Item> MIRAGE_OBJECT_PLACER_ITEM;

    // 杂类
    public static final RegistryObject<Item> DARK_MATTER_REACTOR_ITEM =
            ITEMS.register("darkmatterreactor",
                    () -> new BlockItem(BlockRegistration.DARK_MATTER_REACTOR_BLOCK.get(), new Item.Properties()));

    // 灯
    public static final RegistryObject<Item> FLUORESCENT_TUBE_ITEM =
            ITEMS.register("fluorescent_tube",
                    () -> new BlockItem(BlockRegistration.FLUORESCENT_TUBE.get(), new Item.Properties()));

    public static final RegistryObject<Item> RED_ALARM_LAMP_ITEM =
            ITEMS.register("red_alarm_lamp",
                    () -> new BlockItem(BlockRegistration.RED_ALARM_LAMP.get(), new Item.Properties()));

    public static final RegistryObject<Item> WHITE_CUBE_LAMP_ITEM =
            ITEMS.register("white_cube_lamp",
                    () -> new BlockItem(BlockRegistration.WHITE_CUBE_LAMP.get(), new Item.Properties()));

    public static final RegistryObject<Item> ARC_LAMP_ITEM =
            ITEMS.register("arclamp",
                    () -> new BlockItem(BlockRegistration.ARC_LAMP.get(), new Item.Properties()));

    // 门
    public static final RegistryObject<Item> GATE_ITEM =
            ITEMS.register("gate",
                    () -> new BlockItem(BlockRegistration.GATE.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHECK_POINT_GATE_ITEM =
            ITEMS.register("check_point_gate",
                    () -> new BlockItem(BlockRegistration.CHECK_POINT_GATE.get(), new Item.Properties()));

    public static final RegistryObject<Item> TARTARUS_GATE_ITEM =
            ITEMS.register("tartarus_gate",
                    () -> new BlockItem(BlockRegistration.TARTARUS_GATE.get(), new Item.Properties()));

    // 特效类
//
//    public static final RegistryObject<Item> RWL_ITEM =
//            ITEMS.register("rwl",
//                    () -> new BlockItem(BlockRegistration.RWL.get(), new Item.Properties()));

    // 贴图方块
    public static final RegistryObject<Item> QS_TRADEMARK_PICTURE_ITEM =
            ITEMS.register("qs_trademark_picture",
                    () -> new BlockItem(BlockRegistration.QS_TRADEMARK_PICTURE_BLOCK.get(), new Item.Properties()));

    // 黑洞方块
    public static final RegistryObject<Item> BLACK_HOLE_ITEM =
            ITEMS.register("black_hole",
                    () -> new BlockItem(BlockRegistration.BLACK_HOLE.get(), new Item.Properties()) {
                        @Override
                        public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                                                    @javax.annotation.Nullable net.minecraft.world.level.Level level,
                                                    java.util.List<net.minecraft.network.chat.Component> tooltip,
                                                    net.minecraft.world.item.TooltipFlag flag) {
                            tooltip.add(net.minecraft.network.chat.Component.translatable("item.mirage_gfbs.black_hole.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
                            super.appendHoverText(stack, level, tooltip, flag);
                        }
                    });

    // Colored Doors
    public static final RegistryObject<Item> BLUE_DOOR_ITEM = ITEMS.register("blue_door", () -> new ColoredDoorGeoItem(BlockRegistration.BLUE_DOOR.get(), new Item.Properties(), new ResourceLocation("mirage_gfbs", "textures/block/doors/door_blue.png")));
    public static final RegistryObject<Item> RED_DOOR_ITEM = ITEMS.register("red_door", () -> new ColoredDoorGeoItem(BlockRegistration.RED_DOOR.get(), new Item.Properties(), new ResourceLocation("mirage_gfbs", "textures/block/doors/door_red.png")));
    public static final RegistryObject<Item> BLACK_DOOR_ITEM = ITEMS.register("black_door", () -> new ColoredDoorGeoItem(BlockRegistration.BLACK_DOOR.get(), new Item.Properties(), new ResourceLocation("mirage_gfbs", "textures/block/doors/door_black.png")));
    public static final RegistryObject<Item> ORANGE_DOOR_ITEM = ITEMS.register("orange_door", () -> new ColoredDoorGeoItem(BlockRegistration.ORANGE_DOOR.get(), new Item.Properties(), new ResourceLocation("mirage_gfbs", "textures/block/doors/door_orange.png")));

    public static void init(){}
}
