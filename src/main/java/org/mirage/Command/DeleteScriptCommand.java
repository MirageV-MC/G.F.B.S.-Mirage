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

package org.mirage.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.mirage.Mirage_gfbs;
import org.mirage.PrivilegeManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class DeleteScriptCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("MirageDeleteScript")
                .requires(source -> source.hasPermission(3) || PrivilegeManager.hasPrivilege(source))
                .then(Commands.argument("script_id", StringArgumentType.string())
                        .executes(context -> {
                            String scriptId = StringArgumentType.getString(context, "script_id");
                            CommandSourceStack source = context.getSource();

                            // 删除脚本文件
                            boolean success = deleteScript(scriptId, source);

                            if (success) {
                                source.sendSuccess(
                                        (Supplier<Component>) Component.literal("成功删除脚本: " + scriptId),
                                        true
                                );
                            } else {
                                source.sendFailure(
                                        Component.literal("删除脚本失败: " + scriptId + " (脚本可能不存在)")
                                );
                            }
                            return success ? 1 : 0;
                        })
                )
        );
    }

    private static boolean deleteScript(String scriptId, CommandSourceStack source) {
        try {
            Path scriptPath = Mirage_gfbs.SCRIPTS_DIR.resolve(scriptId + ".txt");

            if (!Files.exists(scriptPath)) {
                return false;
            }

            Files.delete(scriptPath);
            return true;
        } catch (Exception e) {
            source.sendFailure(Component.literal("删除脚本时出错: " + e.getMessage()));
            return false;
        }
    }

    public static boolean deleteScriptDirectly(CommandSourceStack source, String scriptId) {
        try {
            Path scriptPath = Mirage_gfbs.SCRIPTS_DIR.resolve(scriptId + ".txt");

            if (!Files.exists(scriptPath)) {
                source.sendFailure(Component.literal("脚本不存在: " + scriptId));
                return false;
            }

            Files.delete(scriptPath);
            source.sendSuccess(() -> Component.literal("成功删除脚本: " + scriptId), true);
            return true;
        } catch (Exception e) {
            source.sendFailure(Component.literal("删除脚本时出错: " + e.getMessage()));
            return false;
        }
    }
}