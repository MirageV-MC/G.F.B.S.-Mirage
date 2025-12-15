package org.mirage.Objects.blocks.classs;

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

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.mirage.Objects.blockEntity.QSTrademarkPictureBlockEntity;
import org.mirage.Objects.blocks.Bases.PictureBlock.PictureBlockBase;
import org.mirage.Objects.blocks.Bases.PictureBlock.PictureConfig;
import org.mirage.Objects.blocks.Control.QSTrademarkPictureConfig;

public class QSTrademarkPictureBlock extends PictureBlockBase {
    private final PictureConfig defaultConfig;

    public QSTrademarkPictureBlock(Properties props, ResourceLocation texture, int width, int height) {
        super(props);
        this.defaultConfig = new QSTrademarkPictureConfig(texture, width, height, PictureConfig.ScalingMode.FIT_INSIDE);
    }

    @Override
    public PictureConfig createConfig() {
        return defaultConfig;
    }

    @Override
    public PictureConfig createDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QSTrademarkPictureBlockEntity(pos, state);
    }
}
