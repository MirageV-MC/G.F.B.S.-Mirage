package org.mirage.Utils.SyncField;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static org.mirage.Mirage_gfbs.MODID;

public class SyncManager {

    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation(MODID, "sync_field_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /** 每个 BlockPos 对应一个注册过的 BlockEntity 同步数据 */
    private static final Map<BlockPos, SyncedBlockEntityData> BLOCK_ENTITY_DATA = new HashMap<>();

    private static boolean INITIALIZED = false;

    /** 在 CommonSetup 里调用：SyncManager.init(); */
    public static void init() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        int id = 0;
        CHANNEL.registerMessage(
                id++,
                SyncMessage.class,
                SyncMessage::encode,
                SyncMessage::decode,
                SyncManager::handleSyncMessage
        );

        MinecraftForge.EVENT_BUS.register(SyncManager.class);
    }

    /** BlockEntity 构造中调用：SyncManager.registerBlockEntity(this); */
    public static void registerBlockEntity(BlockEntity be) {
        if (be == null) return;

        Map<String, Field> syncFields = new HashMap<>();
        for (Field f : be.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(SyncField.class)) {
                f.setAccessible(true);
                syncFields.put(f.getName(), f);
            }
        }

        if (syncFields.isEmpty()) return;

        BlockPos pos = be.getBlockPos();
        SyncedBlockEntityData data = new SyncedBlockEntityData(be, syncFields);
        data.lastTag = readFieldsToTag(data);
        BLOCK_ENTITY_DATA.put(pos, data);
    }

    /** BlockEntity 移除时可选调用，避免内存泄露 */
    public static void unregisterBlockEntity(BlockEntity be) {
        if (be == null) return;
        BLOCK_ENTITY_DATA.remove(be.getBlockPos());
    }

    // =================  服务端自动检测变化并广播  =================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (SyncedBlockEntityData data : BLOCK_ENTITY_DATA.values()) {
            BlockEntity be = data.blockEntity;
            if (be == null) continue;

            Level level = be.getLevel();
            if (level == null || level.isClientSide) continue; // 只在服务端跑

            CompoundTag current = readFieldsToTag(data);
            if (!Objects.equals(current, data.lastTag)) {
                data.lastTag = current;

                SyncMessage msg = new SyncMessage(be.getBlockPos(), current);
                LevelChunk chunk = level.getChunkAt(be.getBlockPos());

                CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), msg);
                CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), msg);
            }
        }
    }


    private static void handleSyncMessage(SyncMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            LogicalSide side = ctx.getDirection().getReceptionSide();
            if (side != LogicalSide.CLIENT) return;

            Optional<Level> opt = LogicalSidedProvider.CLIENTWORLD.get(side);
            if (!opt.isPresent()) return;
            Level level = opt.get();

            if (level == null) return;

            BlockEntity be = level.getBlockEntity(msg.pos);
            if (be == null) return;

            SyncedBlockEntityData data = BLOCK_ENTITY_DATA.get(msg.pos);
            if (data == null) {
                registerBlockEntity(be);
                data = BLOCK_ENTITY_DATA.get(msg.pos);
                if (data == null) return;
            }

            writeTagToFields(data, msg.data);
        });

        ctx.setPacketHandled(true);
    }

    private static CompoundTag readFieldsToTag(SyncedBlockEntityData data) {
        CompoundTag tag = new CompoundTag();
        Object obj = data.blockEntity;

        for (Map.Entry<String, Field> entry : data.fields.entrySet()) {
            String name = entry.getKey();
            Field field = entry.getValue();
            try {
                Object value = field.get(obj);
                if (value == null) continue;

                if (value instanceof Integer i) {
                    tag.putInt(name, i);
                } else if (value instanceof Float f) {
                    tag.putFloat(name, f);
                } else if (value instanceof Double d) {
                    tag.putDouble(name, d);
                } else if (value instanceof Boolean b) {
                    tag.putBoolean(name, b);
                } else if (value instanceof String s) {
                    tag.putString(name, s);
                }
                // 其他类型你以后要扩展再加
            } catch (IllegalAccessException ignored) {
            }
        }

        return tag;
    }

    private static void writeTagToFields(SyncedBlockEntityData data, CompoundTag tag) {
        Object obj = data.blockEntity;

        for (Map.Entry<String, Field> entry : data.fields.entrySet()) {
            String name = entry.getKey();
            Field field = entry.getValue();

            if (!tag.contains(name)) continue;

            Class<?> type = field.getType();
            try {
                if (type == int.class || type == Integer.class) {
                    field.set(obj, tag.getInt(name));
                } else if (type == float.class || type == Float.class) {
                    field.set(obj, tag.getFloat(name));
                } else if (type == double.class || type == Double.class) {
                    field.set(obj, tag.getDouble(name));
                } else if (type == boolean.class || type == Boolean.class) {
                    field.set(obj, tag.getBoolean(name));
                } else if (type == String.class) {
                    field.set(obj, tag.getString(name));
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    private static class SyncedBlockEntityData {
        final BlockEntity blockEntity;
        final Map<String, Field> fields;
        CompoundTag lastTag;

        SyncedBlockEntityData(BlockEntity be, Map<String, Field> fields) {
            this.blockEntity = be;
            this.fields = fields;
        }
    }

    public static class SyncMessage {
        final BlockPos pos;
        final CompoundTag data;

        public SyncMessage(BlockPos pos, CompoundTag data) {
            this.pos = pos;
            this.data = data;
        }

        public static void encode(SyncMessage msg, net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeBlockPos(msg.pos);
            buf.writeNbt(msg.data);
        }

        public static SyncMessage decode(net.minecraft.network.FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            CompoundTag tag = buf.readNbt();
            if (tag == null) tag = new CompoundTag();
            return new SyncMessage(pos, tag);
        }
    }
}