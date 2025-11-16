package org.mirage.Phenomenon.network.versioncheck;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mirage.Mirage_gfbs;

public class NetworkHandler {

    public static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Mirage_gfbs.MODID, "gfbs_ver_check_main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        // 服务端 -> 客户端：请求版本
        CHANNEL.messageBuilder(VersionRequestPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(VersionRequestPacket::decode)
                .encoder(VersionRequestPacket::encode)
                .consumerMainThread(VersionRequestPacket::handle)
                .add();

        // 客户端 -> 服务端：回传版本
        CHANNEL.messageBuilder(VersionResponsePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(VersionResponsePacket::decode)
                .encoder(VersionResponsePacket::encode)
                .consumerMainThread(VersionResponsePacket::handle)
                .add();
    }
}