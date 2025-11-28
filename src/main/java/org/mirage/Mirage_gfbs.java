/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524

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

package org.mirage;

import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import org.mirage.Command.*;
import org.mirage.Event.Dmr_Meltdown;
import org.mirage.Event.Main90Alpha;
import org.mirage.Objects.CreativeModeTabRegistration;
import org.mirage.Objects.ModBlockEntities;
import org.mirage.Objects.Structure.Registrar;
import org.mirage.Objects.blocks.BlockRegistration;
import org.mirage.Objects.items.ItemRegistration;
import org.mirage.Phenomenon.CameraShake.CameraShakeModule;
import org.mirage.Phenomenon.FogApi.CustomFogModule;
import org.mirage.Phenomenon.network.HexCrackerNetwork;
import org.mirage.Phenomenon.network.Notification.PacketHandler;
import org.mirage.Phenomenon.network.ScriptSystem.NetworkHandler;
import org.mirage.Phenomenon.network.packets.GlobalSoundPlayer;
import org.mirage.Tools.HexCrackerUI;
import org.mirage.Tools.Task;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Mod(Mirage_gfbs.MODID)
public class Mirage_gfbs {

    public static final String MODID = "mirage_gfbs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("Mirage_gfbs");
    public static final Path SCRIPTS_DIR = FMLPaths.CONFIGDIR.get().resolve("Mirage_gfbs/scripts");

    public static MinecraftServer server;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final DeferredRegister<SoundEvent> SOUND = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public Mirage_gfbs() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("MOD "+MODID+" INIT...");

        MinecraftForge.EVENT_BUS.register(org.mirage.Phenomenon.network.versioncheck.ServerEvents.class);

        var modVersion = ModList.get()
                .getModContainerById(MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");

        LOGGER.info("G.F.B.S. Mod Version: {}", modVersion);

        BlockRegistration.init();
        ItemRegistration.init();
        Registrar.init();

        modEventBus.addListener(this::commonSetup);

        ModSoundEvents.register(modEventBus);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CreativeModeTabRegistration.CREATIVE_MODE_TABS.register(modEventBus);

        SOUND.register(modEventBus);
        SoundEventRegister.SOUND_EVENTS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        GlobalSoundPlayer.registerNetworkMessages();
        GlobalSoundPlayCommand.registerNetworkMessages();

        createscriptdir();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        if (Config.logDirtBlock) {
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        }

        LOGGER.info("{} {}", Config.magicNumberIntroduction, Config.magicNumber);

        Config.items.forEach(item -> LOGGER.info("ITEM >> {}", item));

        event.enqueueWork(() -> {
            PacketHandler.register();
            LOGGER.info("Registered notification network channel");
        });

        event.enqueueWork(NetworkHandler::register);

        Registrar.onSetup(event);

        CameraShakeModule.registerNetwork(event);

        event.enqueueWork(() -> {
            org.mirage.Phenomenon.network.ScriptSystem.NetworkHandler.register();
            LOGGER.info("Registered ScriptSystem network channel");
        });

        event.enqueueWork(() -> {
            LOGGER.debug("Register the NetworkHandler...");
            org.mirage.Phenomenon.network.Network.NetworkHandler.register();
        });

        event.enqueueWork(() -> {
            LOGGER.debug("Register all CommandEventExecs");
            onRegisterAllCommandExecs();
        });

        event.enqueueWork(org.mirage.Phenomenon.network.versioncheck.NetworkHandler::register);

        event.enqueueWork(HexCrackerNetwork::register);
    }

    private void onRegisterAllCommandExecs(){
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("main90_alpha", (context)->{
                Main90Alpha.execute(context);
            });
        });
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("dmr_meltdown_new", (context)->{
                Dmr_Meltdown.execute(context, true);
            });
        });
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("dmr_meltdown_old", (context)->{
                Dmr_Meltdown.execute(context, false);
            });
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            CompoundTag fogSettings = FogCommand.getCurrentFogSettings();
            org.mirage.Phenomenon.network.Network.NetworkHandler.sendToPlayer(player, "fog_settings", fogSettings);
            LOGGER.debug("Synchronized fog settings to player: {}", player.getName().getString());

            if (!(server.isDedicatedServer() && server.isPublished())){
                ClientGameType = ((ServerPlayer) event.getEntity()).gameMode.getGameModeForPlayer();
            }

            player.displayClientMessage(Component.literal("[G.F.B.S.]欢迎使用GFBS模组,本模组使用LGPL-v3协议开源.(@Con89524)"), false);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("server starting");

        setServerInstance(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event){
        LOGGER.info("server started.");
    }

    public static void setServerInstance(MinecraftServer serverInstance) {
        server = serverInstance;
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event){
        NotificationCommand.register(event.getDispatcher());
        CameraShakeCommand.register(event.getDispatcher());

        UploadScriptCommand.register(event.getDispatcher());
        CallScriptCommand.register(event.getDispatcher());
        DeleteScriptCommand.register(event.getDispatcher());

        FogCommand.register(event.getDispatcher());

        PrivilegeCommand.register(event.getDispatcher());

        MirageGFBsEventCommand.register(event.getDispatcher());

        MiragePlaysoundCommand.register(event.getDispatcher());
        MirageStopsoundCommand.register(event.getDispatcher());

        FluorescentTubeCommandRegistry.onRegisterCommands(event);
    }

    public static CustomFogModule customFogModule;

    public static GameType ClientGameType;

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("CLIENT SETUP");

            customFogModule = new CustomFogModule();

            ModelResourceLocation modelLocation = new ModelResourceLocation(
                    new ResourceLocation("mirage_gfbs", "darkmatterreactor"), "inventory");

//            event.enqueueWork(GravLensClient::init);
        }

        @SubscribeEvent
        public static void onModelRegistry(ModelEvent.RegisterGeometryLoaders event) {
        }
    }

    private void createscriptdir() {
        File dir = SCRIPTS_DIR.toFile();
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                LOGGER.info("Created scripts directory: {}", SCRIPTS_DIR);
            } else {
                LOGGER.error("Failed to create scripts directory: {}", SCRIPTS_DIR);
            }
        }
    }
}
