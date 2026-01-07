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

package org.mirage.Phenomenon.network.Notification;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.Tools.NotifucationGUI.NotificationGUI;

import java.util.function.Supplier;

public class NotificationPacket {

    private final String title;
    private final String message;
    private final int displayTime;

    public NotificationPacket(String title, String message, int displayTime) {
        this.title = title;
        this.message = message;
        this.displayTime = displayTime;
    }

    public NotificationPacket(FriendlyByteBuf buf) {
        this.title = buf.readUtf(32767);
        this.message = buf.readUtf(32767);
        this.displayTime = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(title);
        buf.writeUtf(message);
        buf.writeInt(displayTime);
    }

    public static void handle(NotificationPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            try {
                NotificationGUI.showNotification(msg.title, msg.message, msg.displayTime);
            } catch (Exception e) {
                System.err.println("Error in showNotification: " + e.getMessage());
                e.printStackTrace();
            }
        });
        ctx.setPacketHandled(true);
    }
}
