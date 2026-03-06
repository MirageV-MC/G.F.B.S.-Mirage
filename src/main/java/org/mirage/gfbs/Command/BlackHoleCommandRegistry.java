package org.mirage.gfbs.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.mirage.gfbs.Phenomenon.BlackHole.BlackHole;
import org.mirage.gfbs.Phenomenon.BlackHole.BlackHoleManager;
import org.mirage.gfbs.Phenomenon.network.BlackHole.NetworkHandler;
import org.mirage.gfbs.Phenomenon.network.packets.BlackHole.BlackHoleCreatePacket;
import org.mirage.gfbs.Phenomenon.network.packets.BlackHole.BlackHoleRemovePacket;

public class BlackHoleCommandRegistry {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("MirageGFBsBlackHole")
                .requires(source -> source.hasPermission(3))
                
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("position", Vec3Argument.vec3())
                                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.1, 100.0))
                                                .executes(context -> createBlackHole(
                                                        context,
                                                        StringArgumentType.getString(context, "name"),
                                                        Vec3Argument.getVec3(context, "position"),
                                                        DoubleArgumentType.getDouble(context, "radius")
                                                ))
                                        )
                                )
                        )
                )
                
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(context -> deleteBlackHole(
                                        context,
                                        StringArgumentType.getString(context, "name")
                                ))
                        )
                )
                
                .then(Commands.literal("setSize")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.1, 100.0))
                                        .executes(context -> setBlackHoleSize(
                                                context,
                                                StringArgumentType.getString(context, "name"),
                                                DoubleArgumentType.getDouble(context, "radius")
                                        ))
                                )
                        )
                )
                
                .then(Commands.literal("setOpacity")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("opacity", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .executes(context -> setAccretionDiskOpacity(
                                                context,
                                                StringArgumentType.getString(context, "name"),
                                                DoubleArgumentType.getDouble(context, "opacity")
                                        ))
                                )
                        )
                )
                
                .then(Commands.literal("list")
                        .executes(BlackHoleCommandRegistry::listBlackHoles)
                )
                
                .then(Commands.literal("info")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(context -> getBlackHoleInfo(
                                        context,
                                        StringArgumentType.getString(context, "name")
                                ))
                        )
                )
        );
    }

    private static int createBlackHole(CommandContext<CommandSourceStack> context, String name, Vec3 position, double radius) {
        BlackHole existing = BlackHoleManager.getBlackHole(name);
        if (existing != null) {
            context.getSource().sendFailure(
                    Component.literal("创建黑洞失败: 黑洞 '" + name + "' 已存在")
            );
            return 0;
        }

        BlackHoleManager.createBlackHole(name, radius, 1.0, position);
        BlackHoleCreatePacket packet = new BlackHoleCreatePacket(name, position, radius, 1.0);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);

        context.getSource().sendSuccess(() ->
                        Component.literal("成功创建黑洞 '" + name + "'，位置: " + 
                                String.format("%.1f, %.1f, %.1f", position.x, position.y, position.z) + 
                                "，半径: " + String.format("%.1f", radius)),
                true
        );
        return 1;
    }

    private static int deleteBlackHole(CommandContext<CommandSourceStack> context, String name) {
        BlackHoleManager.setLevel(context.getSource().getLevel());
        BlackHole blackHole = BlackHoleManager.getBlackHole(name);

        if (blackHole == null) {
            context.getSource().sendFailure(
                    Component.literal("删除黑洞失败: 黑洞 '" + name + "' 不存在")
            );
            return 0;
        }

        BlackHoleRemovePacket packet = new BlackHoleRemovePacket(name);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);

        BlackHoleManager.removeBlackHole(name);

        context.getSource().sendSuccess(() ->
                        Component.literal("开始删除黑洞 '" + name + "' (播放消失动画)"),
                true
        );
        return 1;
    }

    private static int setBlackHoleSize(CommandContext<CommandSourceStack> context, String name, double radius) {
        boolean success = BlackHoleManager.updateBlackHoleSize(name, radius);

        if (success) {
            context.getSource().sendSuccess(() ->
                            Component.literal("成功将黑洞 '" + name + "' 的大小设置为 " + String.format("%.1f", radius)),
                    true
            );
            return 1;
        } else {
            context.getSource().sendFailure(
                    Component.literal("设置失败: 黑洞 '" + name + "' 不存在")
            );
            return 0;
        }
    }

    private static int setAccretionDiskOpacity(CommandContext<CommandSourceStack> context, String name, double opacity) {
        BlackHole blackHole = BlackHoleManager.getBlackHole(name);
        if (blackHole != null) {
            BlackHoleManager.updateAccretionDiskOpacity(name, opacity);
            
            context.getSource().sendSuccess(() ->
                            Component.literal("成功将黑洞 '" + name + "' 的吸积盘透明度设置为 " + String.format("%.2f", opacity)),
                    true
            );
            return 1;
        } else {
            context.getSource().sendFailure(
                    Component.literal("设置失败: 黑洞 '" + name + "' 不存在")
            );
            return 0;
        }
    }

    private static int listBlackHoles(CommandContext<CommandSourceStack> context) {
        java.util.List<String> names = BlackHoleManager.getBlackHoleNames();
        
        if (names.isEmpty()) {
            context.getSource().sendSuccess(() ->
                            Component.literal("当前没有活跃的黑洞"),
                    false
            );
        } else {
            context.getSource().sendSuccess(() ->
                            Component.literal("当前活跃的黑洞 (" + names.size() + "): " + String.join(", ", names)),
                    false
            );
        }
        return 1;
    }

    private static int getBlackHoleInfo(CommandContext<CommandSourceStack> context, String name) {
        BlackHole blackHole = BlackHoleManager.getBlackHole(name);
        
        if (blackHole != null) {
            Vec3 pos = blackHole.getPosition();
            context.getSource().sendSuccess(() ->
                            Component.literal("黑洞 '" + name + "' 的信息:\n" +
                                    "  位置: " + String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z) + "\n" +
                                    "  半径: " + String.format("%.1f", blackHole.getEventHorizonRadius()) + "\n" +
                                    "  吸积盘透明度: " + String.format("%.2f", blackHole.getAccretionDiskOpacity())),
                    false
            );
            return 1;
        } else {
            context.getSource().sendFailure(
                    Component.literal("黑洞 '" + name + "' 不存在")
            );
            return 0;
        }
    }
}
