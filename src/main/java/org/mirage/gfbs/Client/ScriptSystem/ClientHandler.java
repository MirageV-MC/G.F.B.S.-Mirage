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
package org.mirage.gfbs.Client.ScriptSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.Mirage_gfbs;
import org.mirage.gfbs.Phenomenon.network.ScriptSystem.NetworkHandler;
import org.mirage.gfbs.Phenomenon.network.ScriptSystem.UploadScriptPacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mod.EventBusSubscriber(modid = Mirage_gfbs.MODID, value = Dist.CLIENT)
public class ClientHandler {
    public static void uploadScript(String scriptId, Path filePath) {
        try {
            byte[] fileData = Files.readAllBytes(filePath);
            NetworkHandler.sendToServer(new UploadScriptPacket(scriptId, fileData));
        } catch (IOException e) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("读取文件失败: " + e.getMessage())
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void uploadScriptFromPath(String scriptId, String filePathStr, CommandSourceStack source) {
        try {
            Path filePath = Paths.get(filePathStr);
            if (!Files.exists(filePath)) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.literal("文件不存在: " + filePathStr)
                    );
                }
                return;
            }

            if (!Files.isRegularFile(filePath)) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.literal("路径不是文件: " + filePathStr)
                    );
                }
                return;
            }

            byte[] fileData = Files.readAllBytes(filePath);
            NetworkHandler.sendToServer(new UploadScriptPacket(scriptId, fileData));

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("脚本上传成功: " + scriptId)
                );
            }
        } catch (IOException e) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("读取文件失败: " + e.getMessage())
                );
            }
        }
    }
}