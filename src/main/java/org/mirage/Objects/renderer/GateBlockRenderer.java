package org.mirage.Objects.renderer;

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

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.mirage.Objects.blockEntity.GateBlockEntity;
import org.mirage.Objects.blockEntity.GateBlockModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GateBlockRenderer extends GeoBlockRenderer<GateBlockEntity> {

    public GateBlockRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new GateBlockModel());
    }
}