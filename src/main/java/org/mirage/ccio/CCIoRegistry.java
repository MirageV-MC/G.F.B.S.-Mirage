package org.mirage.ccio;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
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

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.ccio.block.CCIoBridgeBlock;
import org.mirage.ccio.blockentity.CCIoBridgeBlockEntity;
import org.mirage.ccio.item.CCIoBridgeBlockItem;

import static org.mirage.Mirage_gfbs.BLOCKS;
import static org.mirage.Mirage_gfbs.ITEMS;
import static org.mirage.Objects.ModBlockEntities.BLOCK_ENTITIES;

public final class CCIoRegistry {
    public static final String MOD_ID = "mirage_gfbs";

    public static final RegistryObject<Block> CC_IO_BRIDGE_BLOCK = BLOCKS.register(
            "cc_io_bridge",
            () -> new CCIoBridgeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
            )
    );

    public static final RegistryObject<Item> CC_IO_BRIDGE_ITEM = ITEMS.register(
            "cc_io_bridge",
            () -> new CCIoBridgeBlockItem(CC_IO_BRIDGE_BLOCK.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<CCIoBridgeBlockEntity>> CC_IO_BRIDGE_BE = BLOCK_ENTITIES.register(
            "cc_io_bridge",
            () -> BlockEntityType.Builder.of(CCIoBridgeBlockEntity::new, CC_IO_BRIDGE_BLOCK.get()).build(null)
    );

    public static void init(){};
}
