package org.mirage.gfbs.Objects;

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

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.gfbs.Objects.blockEntity.Gate.GateBlockEntity;
import org.mirage.gfbs.Objects.blockEntity.QSTrademarkPictureBlockEntity;
import org.mirage.gfbs.Objects.blocks.BlockRegistration;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "mirage_gfbs");

    public static final RegistryObject<BlockEntityType<GateBlockEntity>> GATE =
            BLOCK_ENTITIES.register("gate",
                    () -> BlockEntityType.Builder.of(
                            GateBlockEntity::new,
                            BlockRegistration.GATE.get(),
                            BlockRegistration.TARTARUS_GATE.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<GateBlockEntity>> CHECK_POINT_GATE =
            BLOCK_ENTITIES.register("check_point_gate",
                    () -> BlockEntityType.Builder.of(
                            GateBlockEntity::new,
                            BlockRegistration.CHECK_POINT_GATE.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<QSTrademarkPictureBlockEntity>> QS_TRADEMARK_PICTURE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("qs_trademark_picture",
                    () -> BlockEntityType.Builder.of(QSTrademarkPictureBlockEntity::new, BlockRegistration.QS_TRADEMARK_PICTURE_BLOCK.get()).build(null));

//    public static final RegistryObject<BlockEntityType<RotatingWarningLightBlockEntity>> RWL_ENTITY =
//            BLOCK_ENTITIES.register("rwl",
//                    () -> BlockEntityType.Builder.of(RotatingWarningLightBlockEntity::new, BlockRegistration.RWL.get()).build(null));
}
