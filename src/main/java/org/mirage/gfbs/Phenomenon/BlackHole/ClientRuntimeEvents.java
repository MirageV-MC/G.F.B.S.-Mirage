package org.mirage.gfbs.Phenomenon.BlackHole;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientRuntimeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        // 只有在没有本地集成服务端（即连接到专用服务器或局域网主机）时，才手动 tick
        // 因为如果存在本地服务端，ServerTickEvent 已经在同一个进程中驱动了 BlackHoleManager
        if (mc.level != null && mc.getSingleplayerServer() == null) {
            BlackHoleManager.tick();
        }
    }
}
