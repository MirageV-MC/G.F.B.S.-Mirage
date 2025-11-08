package org.mirage.Objects;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.Mirage_gfbs;
import org.mirage.Objects.blocks.BlockRegistration;
import org.mirage.Objects.items.ItemRegistration;

@Mod.EventBusSubscriber(modid = Mirage_gfbs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeModeTabRegistration {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mirage_gfbs.MODID);
    
    public static final RegistryObject<CreativeModeTab> GFBS_TAB = CREATIVE_MODE_TABS.register("mirage_gfbs_tab", () -> CreativeModeTab.builder()
            .title(Component.literal("G.F.B.S."))
            .icon(() -> new ItemStack(BlockRegistration.DARK_MATTER_REACTOR_BLOCK.get(),1))
            .displayItems((parameters, output) -> {
                output.accept(Items.DIAMOND_SWORD);
            })
            .build());

    @SubscribeEvent
    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == GFBS_TAB.get()) {
            event.accept(ItemRegistration.DARK_MATTER_REACTOR_ITEM.get());
        }
    }
}
