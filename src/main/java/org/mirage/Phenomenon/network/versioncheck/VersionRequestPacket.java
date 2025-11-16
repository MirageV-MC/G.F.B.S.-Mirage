package org.mirage.Phenomenon.network.versioncheck;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.Mirage_gfbs;

import java.util.function.Supplier;

public class VersionRequestPacket {

    public VersionRequestPacket() {
    }

    public static void encode(VersionRequestPacket msg, FriendlyByteBuf buf) {
    }

    public static VersionRequestPacket decode(FriendlyByteBuf buf) {
        return new VersionRequestPacket();
    }

    public static void handle(VersionRequestPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            String clientVersion = ModList.get()
                    .getModContainerById(Mirage_gfbs.MODID)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");

            NetworkHandler.CHANNEL.sendToServer(new VersionResponsePacket(clientVersion));
        });

        ctx.setPacketHandled(true);
    }
}