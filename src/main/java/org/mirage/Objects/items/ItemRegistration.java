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

package org.mirage.Objects.items;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.Encapsulation.MirageObject.MirageObjectEntity;
import org.mirage.Objects.ModEntities;
import org.mirage.Objects.blocks.BlockRegistration;
import org.mirage.Objects.items.MirageObjectPlacer.MirageObjectPlacerItem;

import static org.mirage.Mirage_gfbs.ITEMS;

public class ItemRegistration {
    // 工具
    private static RegistryObject<Item> MIRAGE_OBJECT_PLACER_ITEM;

    public static RegistryObject<Item> getMirageObjectPlacerItem() {
        if (MIRAGE_OBJECT_PLACER_ITEM == null) {
            MIRAGE_OBJECT_PLACER_ITEM = ITEMS.register("mirage_object_placer_item",
                    () -> {
                        RegistryObject<EntityType<MirageObjectEntity>> miragaObject = ModEntities.MIRAGE_OBJECT;
                        if (miragaObject == null || !miragaObject.isPresent()) {
                            return new Item(new Item.Properties().stacksTo(1)) {
                            };
                        }
                        EntityType<?> entityType = miragaObject.get();
                        return new MirageObjectPlacerItem((EntityType<? extends MirageObjectEntity>) entityType, new Item.Properties().stacksTo(1));
                    });
        }
        return MIRAGE_OBJECT_PLACER_ITEM;
    }

    // 杂类
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

    // 门
    public static final RegistryObject<Item> GATE_ITEM =
            ITEMS.register("gate",
                    () -> new BlockItem(BlockRegistration.GATE.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHECK_POINT_GATE_ITEM =
            ITEMS.register("check_point_gate",
                    () -> new BlockItem(BlockRegistration.CHECK_POINT_GATE.get(), new Item.Properties()));

    // 建筑方块

    public static final RegistryObject<Item> QS_WALL_ITE =
            ITEMS.register("qs_wall",
                    () -> new BlockItem(BlockRegistration.QS_WALL.get(), new Item.Properties()));

    // 贴图方块
    public static final RegistryObject<Item> QS_TRADEMARK_PICTURE_ITEM =
            ITEMS.register("qs_trademark_picture",
                    () -> new BlockItem(BlockRegistration.QS_TRADEMARK_PICTURE_BLOCK.get(), new Item.Properties()));

    public static void init(){
        getMirageObjectPlacerItem();
    }
}
