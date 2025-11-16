package org.mirage.Phenomenon.network.versioncheck;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.ServerVersionCheckManager;

import java.util.function.Supplier;

public class VersionResponsePacket {

    private final String clientVersion;

    public VersionResponsePacket(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public static void encode(VersionResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.clientVersion);
    }

    public static VersionResponsePacket decode(FriendlyByteBuf buf) {
        String version = buf.readUtf();
        return new VersionResponsePacket(version);
    }

    public static void handle(VersionResponsePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            ServerVersionCheckManager.onVersionResponse(player, msg.clientVersion);
        });

        ctx.setPacketHandled(true);
    }
}