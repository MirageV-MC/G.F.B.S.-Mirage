package org.mirage.gfbs.mixin;

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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mirage.gfbs.objects.blocks.Bases.FlBlock.AbstractFluorescentLampBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevelForceRelightForFluorescentLamp {

    @Unique
    private static boolean mirage$isFlLamp(BlockState state) {
        return state != null && state.getBlock() instanceof AbstractFluorescentLampBlock;
    }

    @Unique
    private static void mirage$forceRelight(Level level, BlockPos pos) {
        level.getChunkSource().getLightEngine().checkBlock(pos);

        level.getChunkSource().getLightEngine().checkBlock(pos.above());
        level.getChunkSource().getLightEngine().checkBlock(pos.below());
        level.getChunkSource().getLightEngine().checkBlock(pos.north());
        level.getChunkSource().getLightEngine().checkBlock(pos.south());
        level.getChunkSource().getLightEngine().checkBlock(pos.west());
        level.getChunkSource().getLightEngine().checkBlock(pos.east());
    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            at = @At("RETURN"),
            require = 0
    )
    private void mirage$afterSetBlock3(BlockPos pos, BlockState state, int flags,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Level level = (Level) (Object) this;
        if (mirage$isFlLamp(state)) {
            mirage$forceRelight(level, pos);
        }
    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("RETURN"),
            require = 0
    )
    private void mirage$afterSetBlock4(BlockPos pos, BlockState state, int flags, int recursionLeft,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Level level = (Level) (Object) this;
        if (mirage$isFlLamp(state)) {
            mirage$forceRelight(level, pos);
        }
    }
}
