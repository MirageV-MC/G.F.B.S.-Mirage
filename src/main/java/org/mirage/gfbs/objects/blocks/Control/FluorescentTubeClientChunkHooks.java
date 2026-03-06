package org.mirage.gfbs.objects.blocks.Control;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.objects.blocks.Bases.FlBlock.AbstractFluorescentLampBlock;

import static org.mirage.gfbs.objects.blocks.Control.FluorescentTubeClientAPI.globalState;

@Mod.EventBusSubscriber(modid = "mirage_gfbs", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FluorescentTubeClientChunkHooks {

    private FluorescentTubeClientChunkHooks() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        boolean desired = globalState;
        scanChunk(chunk, level, desired, true);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ClientLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        scanChunk(chunk, level, false, false);
    }

    private static void scanChunk(LevelChunk chunk, ClientLevel level, boolean desiredLit, boolean applyState) {
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();

        LevelChunkSection[] sections = chunk.getSections();
        for (int si = 0; si < sections.length; si++) {
            LevelChunkSection section = sections[si];
            if (section == null || section.hasOnlyAir()) continue;

            int sectionY = chunk.getSectionYFromSectionIndex(si);
            int baseY = SectionPos.sectionToBlockCoord(sectionY);

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state == null) continue;

                        Block b = state.getBlock();
                        if (!(b instanceof AbstractFluorescentLampBlock)) continue;

                        BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);

                        FluorescentTubeClientAPI.registerTube(pos);

                        if (applyState) {
                            Boolean lit = state.getValue(AbstractFluorescentLampBlock.LIT);
                            if (lit == null || lit != desiredLit) {
                                level.setBlock(
                                        pos,
                                        state.setValue(AbstractFluorescentLampBlock.LIT, desiredLit),
                                        Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS
                                );
                            } else {
                                level.setBlock(
                                        pos,
                                        state.setValue(AbstractFluorescentLampBlock.LIT, desiredLit),
                                        Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS | Block.UPDATE_IMMEDIATE
                                );
                            }
                        } else {
                            FluorescentTubeClientAPI.unregisterTube(pos);
                        }
                    }
                }
            }
        }
    }
}