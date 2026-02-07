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

package org.mirage.gfbs.Phenomenon.network.ScriptSystem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.gfbs.MirageGFBS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

public class UploadScriptPacket {
    private final String scriptId;
    private final byte[] fileData;

    public UploadScriptPacket(String scriptId, byte[] fileData) {
        this.scriptId = scriptId;
        this.fileData = fileData;
    }

    public UploadScriptPacket(FriendlyByteBuf buf) {
        this.scriptId = buf.readUtf();
        this.fileData = buf.readByteArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(scriptId);
        buf.writeByteArray(fileData);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MinecraftServer server = ctx.get().getSender().getServer();
            server.execute(() -> {
                try {
                    Path scriptPath = MirageGFBS.SCRIPTS_DIR.resolve(scriptId + ".txt");
                    Files.write(scriptPath, fileData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                    ctx.get().getSender().sendSystemMessage(
                            Component.literal("脚本上传成功: " + scriptId)
                    );
                } catch (Exception e) {
                    ctx.get().getSender().sendSystemMessage(
                            Component.literal("脚本保存失败: " + e.getMessage())
                    );
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}