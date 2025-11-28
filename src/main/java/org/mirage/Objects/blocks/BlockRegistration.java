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

package org.mirage.Objects.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.Objects.blocks.classs.FluorescentTubeBlock;
import org.mirage.Objects.blocks.classs.GateBlock;
import org.mirage.Objects.blocks.classs.RedAlarmLampBlock;
import org.mirage.Objects.blocks.classs.WhiteCubeLampBlock;

import static org.mirage.Mirage_gfbs.BLOCKS;

public class BlockRegistration {
    public static final RegistryObject<Block> DARK_MATTER_REACTOR_BLOCK = BLOCKS.register("darkmatterreactor",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0F).noOcclusion()));

    public static final RegistryObject<Block> FLUORESCENT_TUBE =
            BLOCKS.register("fluorescent_tube",
                    () -> new FluorescentTubeBlock(
                            BlockBehaviour.Properties
                                    .of()
                                    .mapColor(MapColor.METAL)
                                    .strength(0.3F)
                                    .noOcclusion()
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

    public static final RegistryObject<Block> GATE =
            BLOCKS.register("big_gate", () ->
                    new GateBlock(Block.Properties.of().strength(5.0F).noOcclusion()));

    public static void init(){}
}
