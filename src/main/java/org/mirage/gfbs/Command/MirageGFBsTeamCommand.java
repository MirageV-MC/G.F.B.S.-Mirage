package org.mirage.gfbs.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import org.mirage.gfbs.advanced.team.TeamRelation;
import org.mirage.gfbs.advanced.team.TeamDefinition;
import org.mirage.gfbs.advanced.team.TeamSavedData;
import org.mirage.gfbs.advanced.team.TeamService;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class MirageGFBsTeamCommand {
    private static final DynamicCommandExceptionType ERROR_NO_SERVER =
            new DynamicCommandExceptionType(x -> Component.literal("无法获取服务器实例"));
    private static final DynamicCommandExceptionType ERROR_TEAM_EXISTS =
            new DynamicCommandExceptionType(id -> Component.literal("团队已存在: " + id));
    private static final DynamicCommandExceptionType ERROR_TEAM_NOT_FOUND =
            new DynamicCommandExceptionType(id -> Component.literal("团队不存在: " + id));
    private static final DynamicCommandExceptionType ERROR_PLAYER_ONLY =
            new DynamicCommandExceptionType(x -> Component.literal("该子命令只能由玩家执行"));
    private static final DynamicCommandExceptionType ERROR_BAD_COLOR =
            new DynamicCommandExceptionType(x -> Component.literal("颜色格式错误: " + x + " (示例: #FF0000)"));

    private MirageGFBsTeamCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("MirageGFBsTeam")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("create")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .executes(ctx -> {
                                    var server = ctx.getSource().getServer();
                                    if (server == null) throw ERROR_NO_SERVER.create("server");
                                    String id = StringArgumentType.getString(ctx, "team");
                                    var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                    if (!data.createTeam(id, id, 0xFFFFFF)) throw ERROR_TEAM_EXISTS.create(id);
                                    TeamService.syncToAll(server);
                                    ctx.getSource().sendSuccess(() -> Component.literal("已创建团队: " + TeamDefinition.normalizeId(id)), true);
                                    return 1;
                                })
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            var server = ctx.getSource().getServer();
                                            if (server == null) throw ERROR_NO_SERVER.create("server");
                                            String id = StringArgumentType.getString(ctx, "team");
                                            String name = StringArgumentType.getString(ctx, "name");
                                            var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                            if (!data.createTeam(id, name, 0xFFFFFF)) throw ERROR_TEAM_EXISTS.create(id);
                                            TeamService.syncToAll(server);
                                            ctx.getSource().sendSuccess(() -> Component.literal("已创建团队: " + id + " (" + name + ")"), true);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("delete")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .suggests(MirageGFBsTeamCommand::suggestTeams)
                                .executes(ctx -> {
                                    var server = ctx.getSource().getServer();
                                    if (server == null) throw ERROR_NO_SERVER.create("server");
                                    String id = StringArgumentType.getString(ctx, "team");
                                    var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                    if (!data.deleteTeam(id)) throw ERROR_TEAM_NOT_FOUND.create(id);
                                    TeamService.syncToAll(server);
                                    ctx.getSource().sendSuccess(() -> Component.literal("已删除团队: " + id), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("color")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .suggests(MirageGFBsTeamCommand::suggestTeams)
                                .then(Commands.argument("rgb", StringArgumentType.word())
                                        .executes(ctx -> {
                                            var server = ctx.getSource().getServer();
                                            if (server == null) throw ERROR_NO_SERVER.create("server");
                                            String id = StringArgumentType.getString(ctx, "team");
                                            String raw = StringArgumentType.getString(ctx, "rgb");
                                            int rgb = parseRgb(raw);
                                            var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                            if (!data.setTeamColor(id, rgb)) throw ERROR_TEAM_NOT_FOUND.create(id);
                                            TeamService.syncToAll(server);
                                            ctx.getSource().sendSuccess(() -> Component.literal("已设置团队颜色: " + id + " = " + raw), true);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("join")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .suggests(MirageGFBsTeamCommand::suggestTeams)
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer self)) {
                                        throw ERROR_PLAYER_ONLY.create("player");
                                    }
                                    var server = ctx.getSource().getServer();
                                    if (server == null) throw ERROR_NO_SERVER.create("server");
                                    String id = StringArgumentType.getString(ctx, "team");
                                    var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                    if (!data.joinTeam(self.getUUID(), id)) throw ERROR_TEAM_NOT_FOUND.create(id);
                                    TeamService.syncToAll(server);
                                    ctx.getSource().sendSuccess(() -> Component.literal("已加入团队: " + id), true);
                                    return 1;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            var server = ctx.getSource().getServer();
                                            if (server == null) throw ERROR_NO_SERVER.create("server");
                                            String id = StringArgumentType.getString(ctx, "team");
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                            if (!data.joinTeam(p.getUUID(), id)) throw ERROR_TEAM_NOT_FOUND.create(id);
                                            TeamService.syncToAll(server);
                                            ctx.getSource().sendSuccess(() -> Component.literal("已将玩家加入团队: " + p.getName().getString() + " -> " + id), true);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer self)) {
                                throw ERROR_PLAYER_ONLY.create("player");
                            }
                            var server = ctx.getSource().getServer();
                            if (server == null) throw ERROR_NO_SERVER.create("server");
                            var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                            boolean ok = data.leaveTeam(self.getUUID());
                            TeamService.syncToAll(server);
                            ctx.getSource().sendSuccess(() -> Component.literal(ok ? "已退出当前团队" : "你当前没有团队"), true);
                            return 1;
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    var server = ctx.getSource().getServer();
                                    if (server == null) throw ERROR_NO_SERVER.create("server");
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                    boolean ok = data.leaveTeam(p.getUUID());
                                    TeamService.syncToAll(server);
                                    ctx.getSource().sendSuccess(() -> Component.literal(ok ? "已移除玩家团队状态: " + p.getName().getString() : "该玩家没有团队"), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("samepvp")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .suggests(MirageGFBsTeamCommand::suggestTeams)
                                .then(Commands.argument("canAttack", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            var server = ctx.getSource().getServer();
                                            if (server == null) throw ERROR_NO_SERVER.create("server");
                                            String id = StringArgumentType.getString(ctx, "team");
                                            boolean can = BoolArgumentType.getBool(ctx, "canAttack");
                                            var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                            if (!data.setSameTeamPvp(id, can)) throw ERROR_TEAM_NOT_FOUND.create(id);
                                            TeamService.syncToAll(server);
                                            ctx.getSource().sendSuccess(() -> Component.literal("已设置同队可互打: " + id + " = " + can), true);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("relation")
                        .then(Commands.argument("a", StringArgumentType.word()).suggests(MirageGFBsTeamCommand::suggestTeams)
                                .then(Commands.argument("b", StringArgumentType.word()).suggests(MirageGFBsTeamCommand::suggestTeams)
                                        .then(Commands.argument("rel", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("ally", "enemy"), b))
                                                .executes(ctx -> {
                                                    var server = ctx.getSource().getServer();
                                                    if (server == null) throw ERROR_NO_SERVER.create("server");
                                                    String a = StringArgumentType.getString(ctx, "a");
                                                    String bTeam = StringArgumentType.getString(ctx, "b");
                                                    String relRaw = StringArgumentType.getString(ctx, "rel");
                                                    TeamRelation rel = "ally".equalsIgnoreCase(relRaw) ? TeamRelation.ALLY : TeamRelation.ENEMY;
                                                    var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                                    if (!data.setRelation(a, bTeam, rel, true)) throw ERROR_TEAM_NOT_FOUND.create(a + "," + bTeam);
                                                    TeamService.syncToAll(server);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("已设置团队关系: " + a + " <-> " + bTeam + " = " + rel.name()), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .then(Commands.literal("pvp")
                        .then(Commands.argument("a", StringArgumentType.word()).suggests(MirageGFBsTeamCommand::suggestTeams)
                                .then(Commands.argument("b", StringArgumentType.word()).suggests(MirageGFBsTeamCommand::suggestTeams)
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    var server = ctx.getSource().getServer();
                                                    if (server == null) throw ERROR_NO_SERVER.create("server");
                                                    String a = StringArgumentType.getString(ctx, "a");
                                                    String bTeam = StringArgumentType.getString(ctx, "b");
                                                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                                    var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                                                    TeamSavedData.Relation current = data.getRelation(a, bTeam);
                                                    if (!data.setRelation(a, bTeam, current.relation(), enabled)) throw ERROR_TEAM_NOT_FOUND.create(a + "," + bTeam);
                                                    TeamService.syncToAll(server);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("已设置团队互打: " + a + " <-> " + bTeam + " = " + enabled), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            if (server == null) throw ERROR_NO_SERVER.create("server");
                            var data = TeamService.get(server).orElseThrow(() -> ERROR_NO_SERVER.create("teams"));
                            ctx.getSource().sendSuccess(() -> Component.literal("= Teams ="), false);
                            if (data.teamsView().isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("(无团队)"), false);
                                return 1;
                            }
                            for (var t : data.teamsView().values()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("- " + t.id() + " (" + t.displayName() + "), color=" + String.format("#%06X", (t.rgb() & 0xFFFFFF))), false);
                            }
                            return 1;
                        })
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestTeams(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var server = ctx.getSource().getServer();
        if (server == null) return builder.buildFuture();
        var dataOpt = TeamService.get(server);
        if (dataOpt.isEmpty()) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(dataOpt.get().teamsView().keySet(), builder);
    }

    private static int parseRgb(String raw) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (raw == null) throw ERROR_BAD_COLOR.create("null");
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x")) s = s.substring(2);
        if (s.length() != 6) throw ERROR_BAD_COLOR.create(raw);
        try {
            int rgb = Integer.parseInt(s, 16);
            if (rgb < 0 || rgb > 0xFFFFFF) throw ERROR_BAD_COLOR.create(raw);
            return rgb;
        } catch (NumberFormatException e) {
            throw ERROR_BAD_COLOR.create(raw);
        }
    }
}
