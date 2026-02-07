package org.mirage.gfbs.advanced.rwl.client;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.mirage.gfbs.MirageGFBS;
import org.mirage.gfbs.advanced.rwl.RWLRenderTypes;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class RWLClientRuntime {

    private RWLClientRuntime() {}

    public static final ResourceLocation VOLUME_SHADER_RL = new ResourceLocation(MirageGFBS.MODID, "rwl_volumelight");
    private static @Nullable ShaderInstance volumeShader;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent e) throws Exception {
        e.registerShader(new ShaderInstance(e.getResourceProvider(), VOLUME_SHADER_RL, RWLRenderTypes.VOLUME_FORMAT), s -> {
            volumeShader = s;
            RWLRenderTypes.bindVolumeShader(() -> volumeShader);
        });
    }

    public static @Nullable ShaderInstance getVolumeShader() {
        return volumeShader;
    }
}
