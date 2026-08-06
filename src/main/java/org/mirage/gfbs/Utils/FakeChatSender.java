package org.mirage.gfbs.Utils;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class FakeChatSender {
    private FakeChatSender() {
    }

    public static void broadcast(MinecraftServer server, String fakeName, String content) {
        Component senderName = Component.literal(fakeName);
        Component message = Component.literal(content);

        ChatType.Bound bound = ChatType.bind(ChatType.CHAT, server.registryAccess(), senderName);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.sendDisguisedChatMessage(message, bound);
        }
    }
}