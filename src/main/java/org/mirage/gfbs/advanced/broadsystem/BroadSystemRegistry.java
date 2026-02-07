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

package org.mirage.gfbs.advanced.broadsystem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static org.mirage.gfbs.MirageGFBS.BLOCKS;
import static org.mirage.gfbs.MirageGFBS.ITEMS;
import static org.mirage.gfbs.MirageGFBS.MODID;

public class BroadSystemRegistry {
    
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    
    public static final RegistryObject<Block> SPEAKER_BLOCK = BLOCKS.register("speaker",
            () -> new SpeakerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(2.0F)
                    .noOcclusion()));
    
    public static final RegistryObject<Item> SPEAKER_BLOCK_ITEM = ITEMS.register("speaker",
            () -> new BlockItem(SPEAKER_BLOCK.get(), new Item.Properties()));
    
    public static final RegistryObject<BlockEntityType<SpeakerBlockEntity>> SPEAKER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("speaker",
                    () -> BlockEntityType.Builder.of(SpeakerBlockEntity::new, SPEAKER_BLOCK.get()).build(null));
    
    public static void init() {
    }
}
