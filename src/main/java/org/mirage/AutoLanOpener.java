package org.mirage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.server.ServerStartedEvent;

import static org.mirage.CommandExecutor.executeCommand;

public class AutoLanOpener {
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) {
            if(!server.isPublished()){
                if (Mirage_gfbs.ClientGameType != null){
                    server.publishServer(Mirage_gfbs.ClientGameType, true, 0);
                }
            }
        }
    }
}
