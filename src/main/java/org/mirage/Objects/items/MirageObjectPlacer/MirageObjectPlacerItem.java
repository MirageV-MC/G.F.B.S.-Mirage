package org.mirage.Objects.items.MirageObjectPlacer;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.mirage.Encapsulation.MirageObject.MirageObject;
import org.mirage.Encapsulation.MirageObject.MirageObjectEntity;
import org.mirage.Encapsulation.MirageObject.MirageObjectRegistry;
import org.mirage.Phenomenon.network.Network.NetworkHandler;

import javax.annotation.Nullable;

public class MirageObjectPlacerItem extends Item {

    private static final String KEY_OBJECT_ID = "MirageObjectId";

    private final EntityType<? extends MirageObjectEntity> entityType;

    public MirageObjectPlacerItem(EntityType<? extends MirageObjectEntity> entityType, Properties props) {
        super(props);
        this.entityType = entityType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    MirageObjectPlacerClient.open(hand);
                });
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                openSettingsMenu((ServerPlayer) player, stack, ctx.getHand());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {

            MirageObject definition = getMirageObject(stack);
            if (definition == null) {
                player.displayClientMessage(Component.literal("§c未选择要放置的 MirageObject"), true);
                return InteractionResult.FAIL;
            }

            BlockPos placePos = ctx.getClickedPos().relative(ctx.getClickedFace());
            spawn(level, placePos, player, definition);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void spawn(Level level, BlockPos pos, Player player, MirageObject definition) {

        MirageObjectEntity entity = entityType.create(level);
        if (entity == null) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        entity.moveTo(x, y, z, player.getYRot(), 0);
        entity.setDefinition(definition);  // ★ 绑定 MirageObject 定义

        level.addFreshEntity(entity);
    }

    private void openSettingsMenu(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        ModNetwork.CHANNEL.sendToServer(new PacketOpenObjectPlacerMenu(hand));
    }

    public static void setMirageObject(ItemStack stack, MirageObject obj) {
        stack.getOrCreateTag().putString(KEY_OBJECT_ID, obj.getId().toString());
    }

    @Nullable
    public static MirageObject getMirageObject(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;

        String id = tag.getString(KEY_OBJECT_ID);
        if (id.isEmpty()) return null;

        return MirageObjectRegistry.get(new ResourceLocation(id));
    }
}
