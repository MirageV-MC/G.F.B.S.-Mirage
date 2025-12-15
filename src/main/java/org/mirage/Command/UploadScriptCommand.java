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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import org.mirage.Client.ScriptSystem.ClientHandler;
import org.mirage.PrivilegeManager;

import java.util.function.Supplier;

public class UploadScriptCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("MirageUploadedScript")
                .requires(source -> source.hasPermission(3) || PrivilegeManager.hasPrivilege(source))
                .then(Commands.argument("script_id", StringArgumentType.string())
                        .then(Commands.argument("file_path", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String scriptId = StringArgumentType.getString(context, "script_id");
                                    String filePath = StringArgumentType.getString(context, "file_path");
                                    CommandSourceStack source = context.getSource();

                                    // 在客户端执行文件上传
                                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                                        ClientHandler.uploadScriptFromPath(scriptId, filePath, source);
                                    });

                                    return 1;
                                })
                        )
                )
        );
    }

    /**
     * 直接上传脚本的通用方法
     * @param source 命令源（用于反馈）
     * @param scriptId 脚本ID
     * @param filePath 文件路径
     */
    public static void uploadScript(CommandSourceStack source, String scriptId, String filePath) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientHandler.uploadScriptFromPath(scriptId, filePath, source);
        });
    }

    /**
     * 客户端直接上传脚本的通用方法
     * @param scriptId 脚本ID
     * @param filePath 文件路径
     * @param source 命令源
     */
    @OnlyIn(Dist.CLIENT)
    public static void uploadScriptClientSide(String scriptId, String filePath, CommandSourceStack source) {
        ClientHandler.uploadScriptFromPath(scriptId, filePath, source);
    }
}