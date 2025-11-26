package org.mirage.Objects.blocks.Control;

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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FluorescentTubeSavedData extends SavedData {

    private static final String DATA_NAME = "mirage_fluorescent_tubes";

    private final Set<BlockPos> tubes = new HashSet<>();

    public FluorescentTubeSavedData() {
    }

    public static FluorescentTubeSavedData load(CompoundTag tag) {
        FluorescentTubeSavedData data = new FluorescentTubeSavedData();
        if (tag.contains("tubes", Tag.TAG_LIST)) {
            ListTag list = tag.getList("tubes", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                int x = t.getInt("x");
                int y = t.getInt("y");
                int z = t.getInt("z");
                data.tubes.add(new BlockPos(x, y, z));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos pos : tubes) {
            CompoundTag t = new CompoundTag();
            t.putInt("x", pos.getX());
            t.putInt("y", pos.getY());
            t.putInt("z", pos.getZ());
            list.add(t);
        }
        tag.put("tubes", list);
        return tag;
    }

    public static FluorescentTubeSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                FluorescentTubeSavedData::load,
                FluorescentTubeSavedData::new,
                DATA_NAME
        );
    }

    public void add(BlockPos pos) {
        if (pos == null) return;
        tubes.add(pos.immutable());
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (pos == null) return;
        tubes.remove(pos);
        setDirty();
    }

    public Set<BlockPos> getAll() {
        return Collections.unmodifiableSet(tubes);
    }
}
