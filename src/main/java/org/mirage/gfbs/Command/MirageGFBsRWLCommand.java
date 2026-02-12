//package org.mirage.gfbs.Command;
//
//import com.mojang.brigadier.CommandDispatcher;
//import com.mojang.brigadier.arguments.IntegerArgumentType;
//import com.mojang.brigadier.arguments.LongArgumentType;
//import com.mojang.brigadier.context.CommandContext;
//import com.mojang.brigadier.exceptions.CommandSyntaxException;
//import net.minecraft.commands.CommandSourceStack;
//import net.minecraft.commands.Commands;
//import net.minecraft.commands.arguments.DimensionArgument;
//import net.minecraft.commands.arguments.ResourceLocationArgument;
//import net.minecraft.network.chat.Component;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.level.ServerLevel;
//import org.mirage.gfbs.advanced.rwl.RWLLevelState;
//import org.mirage.gfbs.advanced.rwl.RWLServerRegistry;
//
//public final class MirageGFBsRWLCommand {
//
//    private MirageGFBsRWLCommand() {}
//
//    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
//        dispatcher.register(
//                Commands.literal("MirageGFBsRWL")
//                        .requires(src -> src.hasPermission(2))
//
//                        // 当前维度
//                        .then(Commands.literal("on")
//                                .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
//                                        .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
//                                                .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
//                                                        .then(Commands.argument("msPerRev", LongArgumentType.longArg(50, 60_000))
//                                                                .then(Commands.argument("soundId", ResourceLocationArgument.id())
//                                                                        .executes(ctx ->
//                                                                                executeOn(
//                                                                                        ctx,
//                                                                                        ctx.getSource().getLevel()
//                                                                                )
//                                                                        )
//                                                                )
//                                                        )
//                                                )
//                                        )
//                                )
//                        )
//
//                        .then(Commands.literal("off")
//                                .executes(ctx -> executeOff(ctx, ctx.getSource().getLevel()))
//                        )
//
//                        .then(Commands.literal("status")
//                                .executes(ctx -> executeStatus(ctx, ctx.getSource().getLevel()))
//                        )
//
//                        // 指定维度
//                        .then(Commands.literal("level")
//                                .then(Commands.argument("dimension", DimensionArgument.dimension())
//                                        .then(Commands.literal("on")
//                                                .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
//                                                        .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
//                                                                .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
//                                                                        .then(Commands.argument("msPerRev", LongArgumentType.longArg(50, 60_000))
//                                                                                .then(Commands.argument("soundId", ResourceLocationArgument.id())
//                                                                                        .executes(ctx ->
//                                                                                                executeOn(
//                                                                                                        ctx,
//                                                                                                        DimensionArgument.getDimension(ctx, "dimension")
//                                                                                                )
//                                                                                        )
//                                                                                )
//                                                                        )
//                                                                )
//                                                        )
//                                                )
//                                        )
//                                        .then(Commands.literal("off")
//                                                .executes(ctx ->
//                                                        executeOff(ctx, DimensionArgument.getDimension(ctx, "dimension"))
//                                                )
//                                        )
//                                        .then(Commands.literal("status")
//                                                .executes(ctx ->
//                                                        executeStatus(ctx, DimensionArgument.getDimension(ctx, "dimension"))
//                                                )
//                                        )
//                                )
//                        )
//        );
//    }
//
//    private static int executeOn(CommandContext<CommandSourceStack> ctx, ServerLevel level) throws CommandSyntaxException {
//        int r = IntegerArgumentType.getInteger(ctx, "r");
//        int g = IntegerArgumentType.getInteger(ctx, "g");
//        int b = IntegerArgumentType.getInteger(ctx, "b");
//        long ms = LongArgumentType.getLong(ctx, "msPerRev");
//
//        ResourceLocation soundId = ResourceLocationArgument.getId(ctx, "soundId");
//
//        RWLLevelState st = RWLLevelState.get(level);
//        st.setConfig(r, g, b, soundId.toString(), ms);
//        st.setEnabled(true);
//        st.setDirty();
//
//        RWLServerRegistry.applyLevelState(level, st, true);
//
//        ctx.getSource().sendSuccess(
//                () -> Component.literal(
//                        "RWL 已开启 @ " + level.dimension().location() +
//                                " | RGB(" + r + "," + g + "," + b + ")" +
//                                " | msPerRev=" + ms +
//                                " | sound=" + soundId
//                ),
//                true
//        );
//        return 1;
//    }
//
//    private static int executeOff(CommandContext<CommandSourceStack> ctx, ServerLevel level) {
//        RWLLevelState st = RWLLevelState.get(level);
//        st.setEnabled(false);
//        st.setDirty();
//
//        RWLServerRegistry.applyLevelState(level, st, true);
//
//        ctx.getSource().sendSuccess(
//                () -> Component.literal("RWL 已关闭 @ " + level.dimension().location()),
//                true
//        );
//        return 1;
//    }
//
//    private static int executeStatus(CommandContext<CommandSourceStack> ctx, ServerLevel level) {
//        RWLLevelState st = RWLLevelState.get(level);
//        ctx.getSource().sendSuccess(
//                () -> Component.literal(
//                        "ℹ RWL 状态 @ " + level.dimension().location() +
//                                " | enabled=" + st.isEnabled() +
//                                " | RGB(" + st.getColorR() + "," + st.getColorG() + "," + st.getColorB() + ")" +
//                                " | msPerRev=" + st.getMsPerRevolution() +
//                                " | sound=" + st.getSoundId()
//                ),
//                false
//        );
//        return 1;
//    }
//}
