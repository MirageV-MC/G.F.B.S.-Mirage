package org.mirage.gfbs.objects.blockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.mirage.gfbs.objects.ModBlockEntities;
import org.mirage.gfbs.Phenomenon.BlackHole.BlackHole;
import org.mirage.gfbs.Phenomenon.BlackHole.BlackHoleManager;

public class BlackHoleBlockEntity extends BlockEntity {
    private String blackHoleName;
    private double size = 6.0;
    private double accretionDiskOpacity = 1.0;
    private boolean initialized = false;

    public BlackHoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLACK_HOLE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlackHoleBlockEntity blockEntity) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            BlackHoleManager.setLevel(serverLevel);
        }
        if (!blockEntity.initialized && !level.isClientSide) {
            blockEntity.initializeBlackHole();
        }
    }

    private void initializeBlackHole() {
        if (this.blackHoleName == null) {
            this.blackHoleName = "black_hole_" + worldPosition.getX() + "_" + worldPosition.getY() + "_" + worldPosition.getZ();
        }
        
        Vec3 position = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        
        if (BlackHoleManager.getBlackHole(blackHoleName) == null) {
            BlackHoleManager.createBlackHole(blackHoleName, this.size, 1.0, position);
            BlackHole blackHole = BlackHoleManager.getBlackHole(blackHoleName);
            if (blackHole != null) {
                blackHole.setBlockBased(true);
            }
        }
        
        this.initialized = true;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
            if (this.level != null && this.level.isClientSide) {
                initializeBlackHoleClient();
            }
        }
    }

    private void initializeBlackHoleClient() {
        if (this.blackHoleName == null) {
            return;
        }

        Vec3 position = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);

        if (BlackHoleManager.getBlackHole(blackHoleName) == null) {
            BlackHoleManager.createBlackHole(blackHoleName, this.size, 1.0, position);
        } else {
            BlackHoleManager.moveBlackHole(blackHoleName, position);
            BlackHoleManager.updateBlackHoleSize(blackHoleName, this.size);
        }

        BlackHole blackHole = BlackHoleManager.getBlackHole(blackHoleName);
        if (blackHole != null) {
            blackHole.setBlockBased(true);
            blackHole.setAccretionDiskOpacity(this.accretionDiskOpacity);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("BlackHoleName")) {
            this.blackHoleName = tag.getString("BlackHoleName");
        }
        if (tag.contains("BlackHoleSize")) {
            this.size = tag.getDouble("BlackHoleSize");
        }
        if (tag.contains("AccretionDiskOpacity")) {
            this.accretionDiskOpacity = tag.getDouble("AccretionDiskOpacity");
        }
        if (tag.contains("Initialized")) {
            this.initialized = tag.getBoolean("Initialized");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.blackHoleName != null) {
            tag.putString("BlackHoleName", this.blackHoleName);
        }
        tag.putDouble("BlackHoleSize", this.size);
        tag.putDouble("AccretionDiskOpacity", this.accretionDiskOpacity);
        tag.putBoolean("Initialized", this.initialized);
    }

    public String getBlackHoleName() {
        return blackHoleName;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
        updateBlackHole();
        setChanged();
    }

    public double getAccretionDiskOpacity() {
        return accretionDiskOpacity;
    }

    public void setAccretionDiskOpacity(double opacity) {
        this.accretionDiskOpacity = Mth.clamp(opacity, 0.0, 1.0);
        setChanged();
    }

    private void updateBlackHole() {
        if (this.blackHoleName != null && level != null && !level.isClientSide) {
            BlackHole blackHole = BlackHoleManager.getBlackHole(this.blackHoleName);
            if (blackHole != null) {
                Vec3 position = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
                BlackHoleManager.removeBlackHole(this.blackHoleName);
                BlackHoleManager.createBlackHole(this.blackHoleName, this.size, 1.0, position);
                blackHole = BlackHoleManager.getBlackHole(this.blackHoleName);
                if (blackHole != null) {
                    blackHole.setBlockBased(true);
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        if (this.blackHoleName != null && level != null) {
            BlackHoleManager.removeBlackHole(this.blackHoleName);
        }
        super.setRemoved();
    }
}
