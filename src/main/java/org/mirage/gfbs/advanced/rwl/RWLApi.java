package org.mirage.gfbs.advanced.rwl;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public final class RWLApi {
    private RWLApi() {}

    public static final String NBT_ROOT = "rwl";
    public static final String NBT_R = "r";
    public static final String NBT_G = "g";
    public static final String NBT_B = "b";
    public static final String NBT_SOUND = "sound";
    public static final String NBT_MS_PER_REV = "msPerRev";

    public static ItemStack createConfiguredItem(ItemStack base, int r, int g, int b, String soundId, long msPerRevolution) {
        CompoundTag root = base.getOrCreateTag();
        CompoundTag rwl = root.getCompound(NBT_ROOT);
        rwl.putInt(NBT_R, clamp255(r));
        rwl.putInt(NBT_G, clamp255(g));
        rwl.putInt(NBT_B, clamp255(b));
        if (soundId != null) rwl.putString(NBT_SOUND, soundId);
        rwl.putLong(NBT_MS_PER_REV, clampMs(msPerRevolution));
        root.put(NBT_ROOT, rwl);
        return base;
    }

    public static void setItemColor(ItemStack stack, int r, int g, int b) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag rwl = root.getCompound(NBT_ROOT);
        rwl.putInt(NBT_R, clamp255(r));
        rwl.putInt(NBT_G, clamp255(g));
        rwl.putInt(NBT_B, clamp255(b));
        root.put(NBT_ROOT, rwl);
    }

    public static void setItemSound(ItemStack stack, String soundId) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag rwl = root.getCompound(NBT_ROOT);
        if (soundId != null) rwl.putString(NBT_SOUND, soundId);
        root.put(NBT_ROOT, rwl);
    }

    public static void setItemSpeed(ItemStack stack, long msPerRevolution) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag rwl = root.getCompound(NBT_ROOT);
        rwl.putLong(NBT_MS_PER_REV, clampMs(msPerRevolution));
        root.put(NBT_ROOT, rwl);
    }

    public static boolean hasItemConfig(ItemStack stack) {
        if (!stack.hasTag()) return false;
        CompoundTag t = stack.getTag();
        return t != null && t.contains(NBT_ROOT, CompoundTag.TAG_COMPOUND);
    }

    public static void applyItemConfigToBlockEntity(ItemStack stack, RotatingWarningLightBlockEntity be) {
        if (!hasItemConfig(stack)) return;
        CompoundTag root = stack.getTag();
        if (root == null) return;
        CompoundTag rwl = root.getCompound(NBT_ROOT);
        int r = rwl.contains(NBT_R) ? rwl.getInt(NBT_R) : be.getColorR();
        int g = rwl.contains(NBT_G) ? rwl.getInt(NBT_G) : be.getColorG();
        int b = rwl.contains(NBT_B) ? rwl.getInt(NBT_B) : be.getColorB();
        String sound = rwl.contains(NBT_SOUND) ? rwl.getString(NBT_SOUND) : be.getSoundId();
        long ms = rwl.contains(NBT_MS_PER_REV) ? rwl.getLong(NBT_MS_PER_REV) : be.getMsPerRevolution();
        be.setConfig(r, g, b, sound, ms);
    }

    @Nullable
    public static RotatingWarningLightBlockEntity getBlockEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return (be instanceof RotatingWarningLightBlockEntity rwl) ? rwl : null;
    }

    @Nullable
    public static SoundEvent resolveSoundEvent(String soundId) {
        if (soundId == null || soundId.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(soundId);
        if (rl == null) return null;
        return ForgeRegistries.SOUND_EVENTS.getValue(rl);
    }

    public static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static long clampMs(long ms) {
        return Math.max(50L, Math.min(60_000L, ms));
    }
}
