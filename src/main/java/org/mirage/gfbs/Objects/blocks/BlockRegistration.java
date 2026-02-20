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

package org.mirage.gfbs.Objects.blocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.gfbs.Objects.blocks.classs.*;
import org.mirage.gfbs.Objects.blocks.classs.FlBlock.ArcLampBlock;
import org.mirage.gfbs.Objects.blocks.classs.FlBlock.FluorescentTubeBlock;
import org.mirage.gfbs.Objects.blocks.classs.FlBlock.RedAlarmLampBlock;
import org.mirage.gfbs.Objects.blocks.classs.FlBlock.WhiteCubeLampBlock;
import org.mirage.gfbs.Objects.blocks.classs.Gate.GateBlock;
import org.mirage.gfbs.Objects.blocks.classs.Gate.TartarusGateBlock;
import org.mirage.gfbs.Objects.blocks.Control.Gate.GateTypes;

import static org.mirage.gfbs.MirageGFBS.BLOCKS;
import static org.mirage.gfbs.MirageGFBS.MODID;

public class BlockRegistration {
    public static final RegistryObject<Block> DARK_MATTER_REACTOR_BLOCK = BLOCKS.register("darkmatterreactor",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0F)));

    // 灯

    public static final RegistryObject<Block> FLUORESCENT_TUBE =
            BLOCKS.register("fluorescent_tube",
                    () -> new FluorescentTubeBlock(
                            BlockBehaviour.Properties
                                    .of()
                                    .mapColor(MapColor.METAL)
                                    .strength(0.3F)
                                    .lightLevel(state -> state.getValue(FluorescentTubeBlock.LIT) ? 14 : 0)
                                    .pushReaction(PushReaction.DESTROY)
                    ));

    public static final RegistryObject<Block> RED_ALARM_LAMP =
            BLOCKS.register("red_alarm_lamp",
                    RedAlarmLampBlock::new
                    );

    public static final RegistryObject<Block> WHITE_CUBE_LAMP =
            BLOCKS.register("white_cube_lamp",
                    WhiteCubeLampBlock::new
            );

    public static final RegistryObject<Block> ARC_LAMP =
            BLOCKS.register("arclamp",
                    () -> new ArcLampBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(5F, 3F)
                                    .lightLevel(state -> 15)
                                    .noOcclusion()
                    ));

    // 特效类

//    public static final RegistryObject<Block> RWL =
//            BLOCKS.register("rwl",
//                    () -> new RotatingWarningLightBlock(
//                            BlockBehaviour.Properties.of()
//                    )
//            );

    // 辅助

    public static final RegistryObject<Block> GATE_COLLISION =
            BLOCKS.register("gate_collision", ()->
                    new GateCollisionBlock(Block.Properties.of().strength(5.0F)
                            .isViewBlocking((state, level, pos) -> false)
                    )
            );

    public static final RegistryObject<Block> TARTARUS_GATE_COLLISION =
            BLOCKS.register("tartarus_gate_collision", ()->
                    new TartarusGateCollisionBlock(Block.Properties.of().strength(50.0F)
                            .isViewBlocking((state, level, pos) -> false)
                    )
            );

    // 门

    public static final RegistryObject<Block> GATE =
            BLOCKS.register("gate", () ->
                    new GateBlock(Block.Properties.of().strength(5.0F), BlockRegistration.GATE_COLLISION, GateTypes.STANDARD));

    public static final RegistryObject<Block> CHECK_POINT_GATE =
            BLOCKS.register("check_point_gate", () ->
                    new GateBlock(Block.Properties.of().strength(5.0F), BlockRegistration.GATE_COLLISION, GateTypes.CHECK_POINT));

    public static final RegistryObject<Block> TARTARUS_GATE =
            BLOCKS.register("tartarus_gate", () ->
                    new TartarusGateBlock(Block.Properties.of().strength(5.0F), GateTypes.TARTARUS_GATE));

    // 建筑方块

    public static final RegistryObject<Block> QS_WALL =
            BLOCKS.register("qs_wall", () ->
                    new Block(BlockBehaviour.Properties.of().strength(20.0F, 18.0F)));

    //画

    public static final RegistryObject<Block> QS_TRADEMARK_PICTURE_BLOCK =
            BLOCKS.register("qs_trademark_picture_block",
                    () -> new QSTrademarkPictureBlock(
                            Block.Properties.of().strength(2.0f),
                            new ResourceLocation(MODID, "textures/block/picture/qs_trademark"),
                            256,
                            128
                    ));

    public static void init(){}
}
