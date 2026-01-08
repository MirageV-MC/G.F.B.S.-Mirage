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

package org.mirage;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import org.mirage.Client.ClientShake.ShakeQsClient;
import org.mirage.ClientConfig.GFBSClientConfigAPI;
import org.mirage.Command.*;
import org.mirage.Event.Dmr_Meltdown;
import org.mirage.Event.DmrexAfter;
import org.mirage.Event.Main90Alpha;
import org.mirage.Objects.CreativeModeTabRegistration;
import org.mirage.Objects.ModBlockEntities;
import org.mirage.Objects.ModEntities;
import org.mirage.Objects.Structure.Registrar;
import org.mirage.Objects.blocks.BlockRegistration;
import org.mirage.Objects.blocks.Control.Gate.GateServerManager;
import org.mirage.Objects.blocks.Control.Gate.GateType;
import org.mirage.Objects.blocks.Control.Gate.GateTypes;
import org.mirage.Objects.blocks.classs.Gate.GateBlock;
import org.mirage.Objects.items.ItemRegistration;
import org.mirage.Objects.renderer.Gate.GateBlockRenderer;
import org.mirage.Objects.renderer.PictureBlockRenderer;
import org.mirage.Phenomenon.CameraShake.CameraShakeModule;
import org.mirage.Phenomenon.FogApi.CustomFogModule;
import org.mirage.Phenomenon.network.HexCrackerNetwork;
import org.mirage.Phenomenon.network.Network.ClientEventHandler;
import org.mirage.Phenomenon.network.Network.ClientToServer;
import org.mirage.Phenomenon.network.Notification.PacketHandler;
import org.mirage.Phenomenon.network.ScriptSystem.NetworkHandler;
import org.mirage.Phenomenon.network.packets.GlobalSoundPlayer;
import org.mirage.Tools.Task;
import org.mirage.Utils.GateUtils;
import org.mirage.Utils.SyncField.SyncManager;
import org.mirage.Utils.WorldWriteQueue;
import org.mirage.api.GateClientAPI;

import org.mirage.ccio.CCIoInit;
import org.mirage.ccio.CCIoRegistry;
import org.mirage.ccio.app.ApiRegisterer;

import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

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

        CCIoInit.init(modEventBus);

        MinecraftForge.EVENT_BUS.register(org.mirage.Phenomenon.network.versioncheck.ServerEvents.class);

        var modVersion = ModList.get()
                .getModContainerById(MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");

        LOGGER.info("G.F.B.S. Mod Version: {}", modVersion);

        registerShutdownHook();

        ModEntities.init();

        registryMirageObjects();

        BlockRegistration.init();
        ItemRegistration.init();
        CCIoRegistry.init();

        Registrar.init();

        modEventBus.addListener(this::commonSetup);

        ModSoundEvents.register(modEventBus);

        ModEntities.ENTITIES.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CreativeModeTabRegistration.CREATIVE_MODE_TABS.register(modEventBus);

        SOUND.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        GlobalSoundPlayer.registerNetworkMessages();
        GlobalSoundPlayCommand.registerNetworkMessages();

        createscriptdir();

        if (FMLEnvironment.dist == Dist.CLIENT){
            ClientEventHandler.registerEvent("gfbs_gate_upd_joined", (data)->{
                GateClientAPI.applyClientState(GateTypes.CHECK_POINT, data.getBoolean("check_point_gate"));
                GateClientAPI.applyClientState(GateTypes.STANDARD, data.getBoolean("gate"));
            });
        }

        modEventBus.register(org.mirage.Tools.CountdownPopup.ClientBootstrap.class);
    }

    private void registryMirageObjects() {
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

        event.enqueueWork(SyncManager::init);

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

        event.enqueueWork(org.mirage.Tools.CountdownPopup.ModNetworking::init);

        ClientToServer.registerChannel();

        new ShakeQsClient();
    }

    private void onRegisterAllCommandExecs(){
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("main90_alpha", (context)->{
                Main90Alpha.execute(context);
            });
        });
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("dmr_meltdown_new", (context)->{
                Dmr_Meltdown.execute(context, true, true);
            });
        });
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("dmr_meltdown_old", (context)->{
                Dmr_Meltdown.execute(context, false, true);
            });
        });
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("dmr_meltdown_none_music", (context)->{
                Dmr_Meltdown.execute(context, false, false);
            });
        });
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("dmr_meltdown_p2_old", (context)->{
                Dmr_Meltdown.p2(context.getSource().getServer().getPlayerList().getPlayers(), context.getSource().getLevel(), false, true);
            });
        });
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("dmr_meltdown_p2_new", (context)->{
                Dmr_Meltdown.p2(context.getSource().getServer().getPlayerList().getPlayers(), context.getSource().getLevel(), true, true);
            });
        });
        Task.spawn(()->{
            MirageGFBsEventCommand.registerHandler("dmrex_after", (context -> {
                DmrexAfter.exec(context.getSource().getServer().getPlayerList().getPlayers(), context.getSource().getLevel());
            }));
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

            MinecraftForge.EVENT_BUS.register(new Object() {
                int ticks = 0;

                @SubscribeEvent
                public void onServerTick(TickEvent.ServerTickEvent tickEvent) {
                    if (tickEvent.phase != TickEvent.Phase.END) return;

                    ticks++;
                    if (ticks >= 40) {
                        // GATE UPD
                        var gfbs_gate_upd_joined_data = new CompoundTag();

                        var level = tickEvent.getServer().getLevel(Level.OVERWORLD);

                        GateUtils.getGateOpenState(level, GateTypes.STANDARD)
                                .ifPresent(open ->
                                        gfbs_gate_upd_joined_data.putBoolean("gate", open)
                                );

                        GateUtils.getGateOpenState(level, GateTypes.CHECK_POINT)
                                .ifPresent(open ->
                                        gfbs_gate_upd_joined_data.putBoolean("check_point_gate", open)
                                );

                        org.mirage.Phenomenon.network.Network.NetworkHandler.sendToPlayer(player, "gfbs_gate_upd_joined", gfbs_gate_upd_joined_data);

                        // END
                        MinecraftForge.EVENT_BUS.unregister(this);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("server starting");

        setServerInstance(event.getServer());

        ApiRegisterer.register(server);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event){
        LOGGER.info("server started.");

        MirageGFBsGateApiCommand.exec(server.overworld().getLevel(), true, "check_point_gate");
        MirageGFBsGateApiCommand.exec(server.overworld().getLevel(), false, "gate");
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

        MirageGFBsGateApiCommand.register(event.getDispatcher());

        MirageGFBsEnvExplosionCommand.register(event.getDispatcher());

        CountdownCommand.register(event.getDispatcher());
    }

    public static CustomFogModule customFogModule;

    public static GameType ClientGameType;

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("CLIENT SETUP");

            customFogModule = new CustomFogModule();

            BlockEntityRenderers.register(ModBlockEntities.QS_TRADEMARK_PICTURE_BLOCK_ENTITY.get(), PictureBlockRenderer::new);

            // GATE
            BlockEntityRenderers.register(ModBlockEntities.GATE.get(), GateBlockRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CHECK_POINT_GATE.get(), GateBlockRenderer::new);

            GateClientAPI.register();

            DmrexAfter.clientExec();
        }

        @SubscribeEvent
        public static void onModelRegistry(ModelEvent.RegisterGeometryLoaders event) {
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            onJvmShutdown();
        }, "G.F.B.S.-ShutdownHook"));
    }

    private void onJvmShutdown() {
        GFBSClientConfigAPI.saveToDisk();
        LOGGER.info("已保存所有客户端配置.");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            WorldWriteQueue.flush();
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
