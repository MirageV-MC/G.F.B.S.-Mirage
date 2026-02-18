package org.mirage.gfbs.advanced.team.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import org.mirage.gfbs.MirageGFBS;

public final class TeamNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MirageGFBS.MODID, "teams"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static boolean registered = false;
    private static int packetId = 0;

    private TeamNetwork() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        CHANNEL.messageBuilder(TeamStateS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TeamStateS2CPacket::encode)
                .decoder(TeamStateS2CPacket::decode)
                .consumerMainThread(TeamStateS2CPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, TeamStateS2CPacket packet) {
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToAll(MinecraftServer server, TeamStateS2CPacket packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
