package org.mirage.Objects.blocks.classs;

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
