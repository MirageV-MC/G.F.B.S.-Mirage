package org.mirage.gfbs.advanced.team.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import org.mirage.gfbs.advanced.team.client.TeamClientState;

import java.util.function.Supplier;

public final class TeamStateS2CPacket {
    private final CompoundTag state;

    public TeamStateS2CPacket(CompoundTag state) {
        this.state = state == null ? new CompoundTag() : state;
    }

    public CompoundTag state() {
        return state;
    }

    public static void encode(TeamStateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.state);
    }

    public static TeamStateS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new TeamStateS2CPacket(tag);
    }

    public static void handle(TeamStateS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> TeamClientState.apply(msg.state)
        ));
        ctx.get().setPacketHandled(true);
    }
}
