package org.mirage.gfbs.advanced.team;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.mirage.gfbs.MirageGFBS;

@Mod.EventBusSubscriber(modid = MirageGFBS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TeamHooks {
    private TeamHooks() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TeamService.syncToAll(player.server);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TeamService.syncToAll(player.server);
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (victim.level().isClientSide()) return;

        var sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof ServerPlayer attacker)) return;

        if (!TeamService.canAttack(attacker.server, attacker.getUUID(), victim.getUUID())) {
            event.setCanceled(true);
        }
    }
}
