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

package org.mirage.gfbs.Command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.gfbs.Mirage_gfbs;
import org.mirage.gfbs.Phenomenon.network.packets.GlobalSoundPlayer;
import org.mirage.gfbs.PrivilegeManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GlobalSoundPlayCommand {

    private static final SuggestionProvider<CommandSourceStack> SOUND_SUGGESTIONS =
            (context, builder) -> getSoundSuggestions(context, builder);

    private static final SuggestionProvider<CommandSourceStack> SOUND_SOURCE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(SoundSource.values())
                            .map(Enum::name)
                            .collect(Collectors.toList()),
                    builder
            );

    private static CompletableFuture<Suggestions> getSoundSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase(Locale.ROOT);
        List<ResourceLocation> soundIds = new ArrayList<>(
                context.getSource().registryAccess().registryOrThrow(Registries.SOUND_EVENT).keySet()
        );

        List<String> suggestions = soundIds.stream()
                .map(id -> new SoundSuggestion(id, calculateRelevance(id, input)))
                .filter(suggestion -> suggestion.relevance > 0)
                .sorted(Comparator.comparingInt((SoundSuggestion s) -> s.relevance).reversed()
                        .thenComparing(s -> s.id.toString())) // 相同相关性按字母顺序排序
                .map(s -> s.id.toString())
                .collect(Collectors.toList());

        if (input.isEmpty() && suggestions.size() > 50) {
            suggestions = suggestions.subList(0, 50);
        }

        return SharedSuggestionProvider.suggest(suggestions, builder);
    }

    private static int calculateRelevance(ResourceLocation soundId, String input) {
        String idStr = soundId.toString().toLowerCase(Locale.ROOT);
        String namespace = soundId.getNamespace().toLowerCase(Locale.ROOT);
        String path = soundId.getPath().toLowerCase(Locale.ROOT);

        if (input.isEmpty()) {
            return 1;
        }

        // 完全匹配得最高分
        if (idStr.equals(input)) {
            return 100;
        }

        // 开头匹配得高分
        if (idStr.startsWith(input)) {
            return 90;
        }

        // 包含输入内容
        if (idStr.contains(input)) {
            return 80;
        }

        // 路径部分匹配
        if (path.contains(input)) {
            return 70;
        }

        // 命名空间匹配
        if (namespace.contains(input)) {
            return 60;
        }

        // 单词开头匹配
        if (Arrays.stream(path.split("[_.]"))
                .anyMatch(part -> part.startsWith(input))) {
            return 50;
        }

        return 0;
    }

    private static class SoundSuggestion {
        public final ResourceLocation id;
        public final int relevance;

        public SoundSuggestion(ResourceLocation id, int relevance) {
            this.id = id;
            this.relevance = relevance;
        }
    }

    public static void registerNetworkMessages() {
        int packetId = 1;
        GlobalSoundPlayer.CHANNEL.registerMessage(
                packetId++,
                SoundCommandPacket.class,
                SoundCommandPacket::encode,
                SoundCommandPacket::decode,
                GlobalSoundPlayCommand::handleSoundCommand
        );
    }

    private static void handleSoundCommand(SoundCommandPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                GlobalSoundPlayer.playToAllClients(player, packet.soundId, packet.soundSource, packet.volume);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @Mod.EventBusSubscriber
    public static class ServerCommandHandler {
        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("playsoundglobal")
                    .requires(source -> source.hasPermission(2) || PrivilegeManager.hasPrivilege(source))
                    .then(Commands.argument("sound_id", ResourceLocationArgument.id())
                            .suggests(SOUND_SUGGESTIONS) // 使用改进的建议提供器
                            .executes(context -> {
                                ResourceLocation soundId = ResourceLocationArgument.getId(context, "sound_id");
                                ServerPlayer player = context.getSource().getPlayer();

                                if (player != null) {
                                    // 使用默认的SoundSource (MASTER)
                                    GlobalSoundPlayer.playToAllClients(player, soundId, SoundSource.MASTER, 1.0f);
                                }
                                return 1;
                            })
                            .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f, 10.0f))
                                    .executes(context -> {
                                        ResourceLocation soundId = ResourceLocationArgument.getId(context, "sound_id");
                                        float volume = FloatArgumentType.getFloat(context, "volume");
                                        ServerPlayer player = context.getSource().getPlayer();

                                        if (player != null) {
                                            // 使用默认的SoundSource (MASTER)
                                            GlobalSoundPlayer.playToAllClients(player, soundId, SoundSource.MASTER, volume);
                                        }
                                        return 1;
                                    })
                                    .then(Commands.argument("sound_source", StringArgumentType.word())
                                            .suggests(SOUND_SOURCE_SUGGESTIONS) // 添加SoundSource建议
                                            .executes(context -> {
                                                ResourceLocation soundId = ResourceLocationArgument.getId(context, "sound_id");
                                                float volume = FloatArgumentType.getFloat(context, "volume");
                                                String soundSourceStr = StringArgumentType.getString(context, "sound_source");
                                                ServerPlayer player = context.getSource().getPlayer();

                                                if (player != null) {
                                                    try {
                                                        SoundSource soundSource = SoundSource.valueOf(soundSourceStr.toUpperCase());
                                                        GlobalSoundPlayer.playToAllClients(player, soundId, soundSource, volume);
                                                    } catch (IllegalArgumentException e) {
                                                        // 无效的SoundSource，使用默认值
                                                        GlobalSoundPlayer.playToAllClients(player, soundId, SoundSource.MASTER, volume);
                                                    }
                                                }
                                                return 1;
                                            })
                                    )
                            )
                    )
            );
        }
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class ClientCommandHandler {
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("playsoundglobal")
                    .then(Commands.argument("sound_id", ResourceLocationArgument.id())
                            .suggests(SOUND_SUGGESTIONS) // 客户端也使用相同的建议提供器
                            .executes(context -> {
                                ResourceLocation soundId = ResourceLocationArgument.getId(context, "sound_id");

                                GlobalSoundPlayer.CHANNEL.sendToServer(
                                        new SoundCommandPacket(soundId, SoundSource.MASTER, 1.0f)
                                );
                                return 1;
                            })
                            .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f, 10.0f))
                                    .executes(context -> {
                                        ResourceLocation soundId = ResourceLocationArgument.getId(context, "sound_id");
                                        float volume = FloatArgumentType.getFloat(context, "volume");

                                        GlobalSoundPlayer.CHANNEL.sendToServer(
                                                new SoundCommandPacket(soundId, SoundSource.MASTER, volume)
                                        );
                                        return 1;
                                    })
                                    .then(Commands.argument("sound_source", StringArgumentType.word())
                                            .suggests(SOUND_SOURCE_SUGGESTIONS) // 添加SoundSource建议
                                            .executes(context -> {
                                                ResourceLocation soundId = ResourceLocationArgument.getId(context, "sound_id");
                                                float volume = FloatArgumentType.getFloat(context, "volume");
                                                String soundSourceStr = StringArgumentType.getString(context, "sound_source");

                                                try {
                                                    SoundSource soundSource = SoundSource.valueOf(soundSourceStr.toUpperCase());
                                                    GlobalSoundPlayer.CHANNEL.sendToServer(
                                                            new SoundCommandPacket(soundId, soundSource, volume)
                                                    );
                                                } catch (IllegalArgumentException e) {
                                                    // 无效的SoundSource，使用默认值
                                                    GlobalSoundPlayer.CHANNEL.sendToServer(
                                                            new SoundCommandPacket(soundId, SoundSource.MASTER, volume)
                                                    );
                                                }
                                                return 1;
                                            })
                                    )
                            )
                    )
            );
        }
    }

    public static class SoundCommandPacket {
        public final ResourceLocation soundId;
        public final SoundSource soundSource; // 新增字段
        public final float volume;

        public SoundCommandPacket(ResourceLocation soundId, SoundSource soundSource, float volume) {
            this.soundId = soundId;
            this.soundSource = soundSource;
            this.volume = volume;
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(soundId);
            buffer.writeEnum(soundSource); // 写入枚举
            buffer.writeFloat(volume);
        }

        public static SoundCommandPacket decode(FriendlyByteBuf buffer) {
            return new SoundCommandPacket(
                    buffer.readResourceLocation(),
                    buffer.readEnum(SoundSource.class), // 读取枚举
                    buffer.readFloat()
            );
        }
    }

    /**
     * 直接播放全局声音的通用方法
     * @param player 触发玩家
     * @param soundId 声音ID
     * @param soundSource 声音分类
     * @param volume 音量(0-10)
     */
    public static void playGlobalSound(ServerPlayer player, ResourceLocation soundId,
                                       SoundSource soundSource, float volume) {
        try {
            GlobalSoundPlayer.playToAllClients(player, soundId, soundSource, volume);
        } catch (Exception e) {
            Mirage_gfbs.LOGGER.error("播放全局声音失败", e);
        }
    }

    /**
     * 客户端请求播放全局声音的通用方法
     * @param soundId 声音ID
     * @param soundSource 声音分类
     * @param volume 音量(0-10)
     */
    public static void requestGlobalSound(ResourceLocation soundId, SoundSource soundSource, float volume) {
        try {
            GlobalSoundPlayer.CHANNEL.sendToServer(
                    new SoundCommandPacket(soundId, soundSource, volume)
            );
        } catch (Exception e) {
            Mirage_gfbs.LOGGER.error("请求全局声音失败", e);
        }
    }
}