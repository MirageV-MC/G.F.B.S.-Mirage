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
import org.mirage.Phenomenon.ScriptSystem.ScriptExecutor;
import org.mirage.PrivilegeManager;

import java.util.function.Supplier;

public class CallScriptCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("MirageCallScript")
                .requires(source -> source.hasPermission(2) || PrivilegeManager.hasPrivilege(source))
                .then(Commands.argument("script_id", StringArgumentType.string())
                        .executes(context -> {
                            String scriptId = StringArgumentType.getString(context, "script_id");
                            CommandSourceStack source = context.getSource();

                            // 执行脚本
                            boolean success = ScriptExecutor.executeScript(scriptId, source);

                            if (success) {
                                source.sendSuccess(
                                        (Supplier<Component>) Component.literal("成功执行脚本: " + scriptId),
                                        true
                                );
                            } else {
                                source.sendFailure(
                                        Component.literal("执行脚本失败: " + scriptId)
                                );
                            }
                            return success ? 1 : 0;
                        })
                )
        );
    }

    public static boolean executeScriptDirectly(CommandSourceStack source, String scriptId) {
        boolean success = ScriptExecutor.executeScript(scriptId, source);

        if (success) {
            source.sendSuccess(() -> Component.literal("成功执行脚本: " + scriptId), true);
        } else {
            source.sendFailure(Component.literal("执行脚本失败: " + scriptId));
        }

        return success;
    }
}