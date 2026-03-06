package org.mirage.gfbs.objects.blocks.Control;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.Phenomenon.network.Network.NetworkHandler;

@Mod.EventBusSubscriber(modid = "mirage_gfbs", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FluorescentTubeSyncHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.serverLevel();
            FluorescentTubeSavedData data = FluorescentTubeSavedData.get(level);
            
            CompoundTag syncData = new CompoundTag();
            syncData.putString("mode", data.getInstabilityMode());
            syncData.putBoolean("globalState", data.getGlobalState());
            
            NetworkHandler.sendToPlayer(serverPlayer, "fluorescent_tube_sync_config", syncData);
        }
    }
}
