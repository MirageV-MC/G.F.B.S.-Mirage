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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import org.mirage.gfbs.ServerConfig.ServerConfigApi;
import org.mirage.gfbs.ServerConfig.ServerConfigKey;
import org.mirage.gfbs.ServerConfig.ServerConfigType;

import java.util.Collection;

/**
 * Command for managing server-side configurations at runtime.
 * 
 * Usage:
 *   /MirageGFBsServerConfig list [category] - List all configs or configs in a category
 *   /MirageGFBsServerConfig get <configId> - Get a config value
 *   /MirageGFBsServerConfig set <configId> <value> - Set a config value
 *   /MirageGFBsServerConfig reset <configId> - Reset a config to default
 *   /MirageGFBsServerConfig reload - Reload configs from disk
 *   /MirageGFBsServerConfig save - Save configs to disk
 */
public class MirageGFBsServerConfigCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("MirageGFBsServerConfig")
                .requires(source -> source.hasPermission(2))
                
                .then(Commands.literal("list")
                    .executes(MirageGFBsServerConfigCommand::listAll)
                    .then(Commands.argument("category", StringArgumentType.word())
                        .executes(MirageGFBsServerConfigCommand::listCategory)
                    )
                )
                
                .then(Commands.literal("get")
                    .then(Commands.argument("configId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
                                builder.suggest(key.id());
                            }
                            return builder.buildFuture();
                        })
                        .executes(MirageGFBsServerConfigCommand::getConfig)
                    )
                )
                
                .then(Commands.literal("set")
                    .then(Commands.argument("configId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
                                builder.suggest(key.id());
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes(MirageGFBsServerConfigCommand::setConfigFromString)
                        )
                    )
                )
                
                .then(Commands.literal("setBool")
                    .then(Commands.argument("configId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
                                if (key.type() == ServerConfigType.BOOLEAN) {
                                    builder.suggest(key.id());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(MirageGFBsServerConfigCommand::setConfigBool)
                        )
                    )
                )
                
                .then(Commands.literal("setInt")
                    .then(Commands.argument("configId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
                                if (key.type() == ServerConfigType.INT) {
                                    builder.suggest(key.id());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                            .executes(MirageGFBsServerConfigCommand::setConfigInt)
                        )
                    )
                )
                
                .then(Commands.literal("setFloat")
                    .then(Commands.argument("configId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
                                if (key.type() == ServerConfigType.FLOAT) {
                                    builder.suggest(key.id());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", FloatArgumentType.floatArg())
                            .executes(MirageGFBsServerConfigCommand::setConfigFloat)
                        )
                    )
                )
                
                .then(Commands.literal("setDouble")
                    .then(Commands.argument("configId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
                                if (key.type() == ServerConfigType.DOUBLE) {
                                    builder.suggest(key.id());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                            .executes(MirageGFBsServerConfigCommand::setConfigDouble)
                        )
                    )
                )
                
                .then(Commands.literal("setString")
                    .then(Commands.argument("configId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
                                if (key.type() == ServerConfigType.STRING) {
                                    builder.suggest(key.id());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes(MirageGFBsServerConfigCommand::setConfigString)
                        )
                    )
                )
                
                .then(Commands.literal("setPos")
                    .then(Commands.argument("configId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
                                if (key.type() == ServerConfigType.BLOCK_POS) {
                                    builder.suggest(key.id());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .executes(MirageGFBsServerConfigCommand::setConfigPos)
                        )
                    )
                )
                
                .then(Commands.literal("reset")
                    .then(Commands.argument("configId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
                                builder.suggest(key.id());
                            }
                            return builder.buildFuture();
                        })
                        .executes(MirageGFBsServerConfigCommand::resetConfig)
                    )
                )
                
                .then(Commands.literal("reload")
                    .executes(MirageGFBsServerConfigCommand::reload)
                )
                
                .then(Commands.literal("save")
                    .executes(MirageGFBsServerConfigCommand::save)
                )
                
                .then(Commands.literal("categories")
                    .executes(MirageGFBsServerConfigCommand::listCategories)
                )
        );
    }

    private static int listAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerConfigKey<?>> keys = ServerConfigApi.allKeys();
        
        if (keys.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e[ServerConfig] 没有注册任何服务端配置项。"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§e[ServerConfig] 已注册的服务端配置项 (" + keys.size() + "):"), false);
        
        for (ServerConfigKey<?> key : keys) {
            String valueStr = formatValue(key);
            source.sendSuccess(() -> Component.literal("§7  " + key.id() + " §8[" + key.type().name() + "]§r = §a" + valueStr), false);
        }
        
        return keys.size();
    }

    private static int listCategory(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String category = StringArgumentType.getString(ctx, "category");
        
        int count = 0;
        source.sendSuccess(() -> Component.literal("§e[ServerConfig] 分类 '" + category + "' 中的配置项:"), false);
        
        for (ServerConfigKey<?> key : ServerConfigApi.allKeys()) {
            if (key.category().equals(category)) {
                String valueStr = formatValue(key);
                source.sendSuccess(() -> Component.literal("§7  " + key.id() + " §8[" + key.type().name() + "]§r = §a" + valueStr), false);
                count++;
            }
        }
        
        if (count == 0) {
            source.sendSuccess(() -> Component.literal("§c[ServerConfig] 分类 '" + category + "' 中没有配置项。"), false);
        }
        
        return count;
    }

    private static int listCategories(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        java.util.List<String> categories = ServerConfigApi.categories();
        
        if (categories.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e[ServerConfig] 没有任何分类。"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§e[ServerConfig] 可用分类:"), false);
        for (String cat : categories) {
            source.sendSuccess(() -> Component.literal("§7  - §b" + cat), false);
        }
        
        return categories.size();
    }

    private static int getConfig(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String configId = StringArgumentType.getString(ctx, "configId");
        
        ServerConfigKey<?> key = ServerConfigApi.getKey(configId);
        if (key == null) {
            source.sendFailure(Component.literal("§c[ServerConfig] 未找到配置项: " + configId));
            return 0;
        }
        
        String valueStr = formatValue(key);
        source.sendSuccess(() -> Component.literal("§e[ServerConfig] " + key.id() + " §8[" + key.type().name() + "]§r = §a" + valueStr), false);
        source.sendSuccess(() -> Component.literal("§7  显示名: " + key.displayName()), false);
        source.sendSuccess(() -> Component.literal("§7  注释: " + key.comment()), false);
        source.sendSuccess(() -> Component.literal("§7  默认值: " + formatDefaultValue(key)), false);
        
        return 1;
    }

    private static int setConfigFromString(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String configId = StringArgumentType.getString(ctx, "configId");
        String valueStr = StringArgumentType.getString(ctx, "value");
        
        ServerConfigKey<?> key = ServerConfigApi.getKey(configId);
        if (key == null) {
            source.sendFailure(Component.literal("§c[ServerConfig] 未找到配置项: " + configId));
            return 0;
        }
        
        boolean success = false;
        String oldValue = formatValue(key);
        
        try {
            switch (key.type()) {
                case BOOLEAN -> {
                    boolean val = Boolean.parseBoolean(valueStr);
                    success = ServerConfigApi.setBoolean(configId, val);
                }
                case INT -> {
                    int val = Integer.parseInt(valueStr);
                    success = ServerConfigApi.setInt(configId, val);
                }
                case FLOAT -> {
                    float val = Float.parseFloat(valueStr);
                    success = ServerConfigApi.setFloat(configId, val);
                }
                case DOUBLE -> {
                    double val = Double.parseDouble(valueStr);
                    success = ServerConfigApi.setDouble(configId, val);
                }
                case STRING -> {
                    success = ServerConfigApi.setString(configId, valueStr);
                }
                case BLOCK_POS -> {
                    String[] parts = valueStr.split(",");
                    if (parts.length == 3) {
                        BlockPos pos = new BlockPos(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim())
                        );
                        success = ServerConfigApi.setBlockPos(configId, pos);
                    }
                }
                case ENUM -> {
                    success = ServerConfigApi.setString(configId, valueStr);
                }
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[ServerConfig] 解析值失败: " + e.getMessage()));
            return 0;
        }
        
        if (success) {
            String newValue = formatValue(key);
            source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已更新 " + configId + ": " + oldValue + " -> " + newValue), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[ServerConfig] 设置失败，类型不匹配。"));
            return 0;
        }
    }

    private static int setConfigBool(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String configId = StringArgumentType.getString(ctx, "configId");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        
        String oldValue = formatValue(ServerConfigApi.getKey(configId));
        
        if (ServerConfigApi.setBoolean(configId, value)) {
            source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已更新 " + configId + ": " + oldValue + " -> " + value), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[ServerConfig] 设置失败，配置项不存在或类型不匹配。"));
            return 0;
        }
    }

    private static int setConfigInt(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String configId = StringArgumentType.getString(ctx, "configId");
        int value = IntegerArgumentType.getInteger(ctx, "value");
        
        String oldValue = formatValue(ServerConfigApi.getKey(configId));
        
        if (ServerConfigApi.setInt(configId, value)) {
            source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已更新 " + configId + ": " + oldValue + " -> " + value), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[ServerConfig] 设置失败，配置项不存在或类型不匹配。"));
            return 0;
        }
    }

    private static int setConfigFloat(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String configId = StringArgumentType.getString(ctx, "configId");
        float value = FloatArgumentType.getFloat(ctx, "value");
        
        String oldValue = formatValue(ServerConfigApi.getKey(configId));
        
        if (ServerConfigApi.setFloat(configId, value)) {
            source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已更新 " + configId + ": " + oldValue + " -> " + value), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[ServerConfig] 设置失败，配置项不存在或类型不匹配。"));
            return 0;
        }
    }

    private static int setConfigDouble(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String configId = StringArgumentType.getString(ctx, "configId");
        double value = DoubleArgumentType.getDouble(ctx, "value");
        
        String oldValue = formatValue(ServerConfigApi.getKey(configId));
        
        if (ServerConfigApi.setDouble(configId, value)) {
            source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已更新 " + configId + ": " + oldValue + " -> " + value), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[ServerConfig] 设置失败，配置项不存在或类型不匹配。"));
            return 0;
        }
    }

    private static int setConfigString(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String configId = StringArgumentType.getString(ctx, "configId");
        String value = StringArgumentType.getString(ctx, "value");
        
        String oldValue = formatValue(ServerConfigApi.getKey(configId));
        
        if (ServerConfigApi.setString(configId, value)) {
            source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已更新 " + configId + ": " + oldValue + " -> " + value), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[ServerConfig] 设置失败，配置项不存在或类型不匹配。"));
            return 0;
        }
    }

    private static int setConfigPos(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String configId = StringArgumentType.getString(ctx, "configId");
        BlockPos value = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        
        String oldValue = formatValue(ServerConfigApi.getKey(configId));
        String newValue = value.getX() + "," + value.getY() + "," + value.getZ();
        
        if (ServerConfigApi.setBlockPos(configId, value)) {
            source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已更新 " + configId + ": " + oldValue + " -> " + newValue), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[ServerConfig] 设置失败，配置项不存在或类型不匹配。"));
            return 0;
        }
    }

    private static int resetConfig(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String configId = StringArgumentType.getString(ctx, "configId");
        
        ServerConfigKey<?> key = ServerConfigApi.getKey(configId);
        if (key == null) {
            source.sendFailure(Component.literal("§c[ServerConfig] 未找到配置项: " + configId));
            return 0;
        }
        
        String oldValue = formatValue(key);
        resetToDefault(key);
        String newValue = formatValue(key);
        
        source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已重置 " + configId + " 到默认值: " + oldValue + " -> " + newValue), false);
        return 1;
    }

    @SuppressWarnings("unchecked")
    private static <T> void resetToDefault(ServerConfigKey<T> key) {
        ServerConfigApi.set(key, key.defaultValue());
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerConfigApi.reloadFromDisk();
        source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已从磁盘重新加载配置。"), false);
        return 1;
    }

    private static int save(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerConfigApi.saveToDisk();
        source.sendSuccess(() -> Component.literal("§a[ServerConfig] 已保存配置到磁盘。"), false);
        return 1;
    }

    private static String formatValue(ServerConfigKey<?> key) {
        if (key == null) return "null";
        
        Object value = ServerConfigApi.get(key);
        if (value == null) return "null";
        
        return switch (key.type()) {
            case BOOLEAN -> value.toString();
            case INT -> value.toString();
            case FLOAT -> String.format("%.2f", (Float) value);
            case DOUBLE -> String.format("%.4f", (Double) value);
            case STRING -> "\"" + value + "\"";
            case BLOCK_POS -> {
                BlockPos pos = (BlockPos) value;
                yield pos.getX() + "," + pos.getY() + "," + pos.getZ();
            }
            case ENUM -> value.toString();
        };
    }

    private static String formatDefaultValue(ServerConfigKey<?> key) {
        if (key == null) return "null";
        
        Object value = key.defaultValue();
        if (value == null) return "null";
        
        return switch (key.type()) {
            case BOOLEAN -> value.toString();
            case INT -> value.toString();
            case FLOAT -> String.format("%.2f", (Float) value);
            case DOUBLE -> String.format("%.4f", (Double) value);
            case STRING -> "\"" + value + "\"";
            case BLOCK_POS -> {
                BlockPos pos = (BlockPos) value;
                yield pos.getX() + "," + pos.getY() + "," + pos.getZ();
            }
            case ENUM -> value.toString();
        };
    }
}
