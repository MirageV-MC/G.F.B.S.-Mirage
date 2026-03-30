/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package org.mirage.gfbs;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
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

import org.mirage.gfbs.Client.ClientShake.ShakeQsClient;
import org.mirage.gfbs.ClientConfig.GFBSClientConfigAPI;
import org.mirage.gfbs.Command.*;
import org.mirage.gfbs.ServerConfig.ServerConfigApi;
import org.mirage.gfbs.Event.EccEvent;
import org.mirage.gfbs.Phenomenon.event.BlackHoleCommand;
import org.mirage.gfbs.Event.DmrMeltdown;
import org.mirage.gfbs.Event.DmrexAfter;
import org.mirage.gfbs.Event.Main90Alpha;
import org.mirage.gfbs.ServerConfig.instance.GFBSServerConfig;
import org.mirage.gfbs.objects.CreativeModeTabRegistration;
import org.mirage.gfbs.objects.ModBlockEntities;
import org.mirage.gfbs.objects.ModEntities;
import org.mirage.gfbs.objects.Structure.Registrar;
import org.mirage.gfbs.objects.blocks.BlockRegistration;
import org.mirage.gfbs.objects.blocks.Control.Gate.GateTypes;
import org.mirage.gfbs.objects.items.ItemRegistration;
import org.mirage.gfbs.objects.renderer.Gate.GateBlockRenderer;
import org.mirage.gfbs.objects.renderer.PictureBlockRenderer;
import org.mirage.gfbs.Phenomenon.CameraShake.CameraShakeModule;
import org.mirage.gfbs.Phenomenon.FogApi.CustomFogModule;
import org.mirage.gfbs.Phenomenon.network.HexCrackerNetwork;
import org.mirage.gfbs.Phenomenon.network.Network.ClientEventHandler;
import org.mirage.gfbs.Phenomenon.network.Network.ClientToServer;
import org.mirage.gfbs.Phenomenon.network.Notification.PacketHandler;
import org.mirage.gfbs.Phenomenon.network.ScriptSystem.NetworkHandler;
import org.mirage.gfbs.Phenomenon.network.packets.GlobalSoundPlayer;
import org.mirage.gfbs.Utils.GateUtils;
import org.mirage.gfbs.Utils.SyncField.SyncManager;
import org.mirage.gfbs.Utils.WorldWriteQueue;
import org.mirage.gfbs.advanced.broadsystem.BroadSystemNetwork;
import org.mirage.gfbs.advanced.broadsystem.BroadSystemRegistry;
import org.mirage.gfbs.api.GateClientAPI;

import org.mirage.gfbs.ccio.CCIoInit;
import org.mirage.gfbs.ccio.CCIoRegistry;
import org.mirage.gfbs.ccio.app.ApiRegisterer;

import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Mod(MirageGFBS.MODID)
public class MirageGFBS {

    public static final String MODID = "mirage_gfbs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("Mirage_gfbs");
    public static final Path SCRIPTS_DIR = FMLPaths.CONFIGDIR.get().resolve("Mirage_gfbs/scripts");

    public static MinecraftServer server;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final DeferredRegister<SoundEvent> SOUND = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public MirageGFBS() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("MOD "+MODID+" INIT...");

        CCIoInit.init(modEventBus);

        MinecraftForge.EVENT_BUS.register(org.mirage.gfbs.Phenomenon.network.versioncheck.ServerEvents.class);

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
        org.mirage.gfbs.objects.items.BuildItemRegistration.init();
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
        BroadSystemRegistry.BLOCK_ENTITIES.register(modEventBus);

        SOUND.register(modEventBus);

        BroadSystemRegistry.init();

        MinecraftForge.EVENT_BUS.register(this);

        GlobalSoundPlayer.registerNetworkMessages();
        GlobalSoundPlayCommand.registerNetworkMessages();

        createscriptdir();

        if (FMLEnvironment.dist == Dist.CLIENT){
            ClientEventHandler.registerEvent("gfbs_gate_upd_joined", (data)->{
                GateClientAPI.applyClientState(GateTypes.CHECK_POINT, data.getBoolean("check_point_gate"));
                GateClientAPI.applyClientState(GateTypes.CHECK_POINT_X6, data.getBoolean("check_point_gate_x6"));
                GateClientAPI.applyClientState(GateTypes.STANDARD, data.getBoolean("gate"));

                GateClientAPI.applyClientState(GateTypes.TARTARUS_GATE, data.getBoolean("tartarus_gate"));
            });
        }

        modEventBus.register(org.mirage.gfbs.Tools.CountdownPopup.ClientBootstrap.class);
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

        event.enqueueWork(org.mirage.gfbs.advanced.team.network.TeamNetwork::register);

        Registrar.onSetup(event);

        CameraShakeModule.registerNetwork(event);

        event.enqueueWork(() -> {
            org.mirage.gfbs.Phenomenon.network.ScriptSystem.NetworkHandler.register();
            LOGGER.info("Registered ScriptSystem network channel");
        });

        event.enqueueWork(() -> {
            LOGGER.debug("Register the NetworkHandler...");
            org.mirage.gfbs.Phenomenon.network.Network.NetworkHandler.register();
        });

        event.enqueueWork(() -> {
            LOGGER.debug("Register all CommandEventExecs");
            onRegisterAllCommandExecs();
        });

        event.enqueueWork(org.mirage.gfbs.Phenomenon.network.versioncheck.NetworkHandler::register);

        event.enqueueWork(HexCrackerNetwork::register);

        event.enqueueWork(org.mirage.gfbs.Tools.CountdownPopup.ModNetworking::init);

        event.enqueueWork(() -> {
            LOGGER.info("Registering BroadSystem network channel...");
            BroadSystemNetwork.register();
        });

        ClientToServer.registerChannel();

        new ShakeQsClient();
    }

    private void onRegisterAllCommandExecs(){
        MirageGFBsEventCommand.registerHandler("main90_alpha", Main90Alpha::execute);

        MirageGFBsEventCommand.registerHandler("dmr_meltdown_new", (context)->{
            DmrMeltdown.execute(context, true, true);
        });
        MirageGFBsEventCommand.registerHandler("dmr_meltdown_old", (context)->{
            DmrMeltdown.execute(context, false, true);
        });

        MirageGFBsEventCommand.registerHandler("ecc", EccEvent::execute);
        MirageGFBsEventCommand.registerHandler("ecc_p2", (context)->{
            EccEvent.p2(context, context.getSource().getServer().getPlayerList().getPlayers());
        });

        //DEBUG
        MirageGFBsEventCommand.registerHandler("debug_dmr_meltdown_none_music", (context)->{
            DmrMeltdown.execute(context, false, false);
        });
        MirageGFBsEventCommand.registerHandler("debug_dmr_meltdown_p2_old", (context)->{
            DmrMeltdown.p2(context.getSource().getServer().getPlayerList().getPlayers(), context.getSource().getLevel(), false, true);
        });
        MirageGFBsEventCommand.registerHandler("debug_dmr_meltdown_p2_new", (context)->{
            DmrMeltdown.p2(context.getSource().getServer().getPlayerList().getPlayers(), context.getSource().getLevel(), true, true);
        });
        MirageGFBsEventCommand.registerHandler("debug_dmrex_after", (context -> {
            DmrexAfter.exec(context.getSource().getServer().getPlayerList().getPlayers(), context.getSource().getLevel());
        }));
    }

    @SubscribeEvent
    public void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            CompoundTag fogSettings = FogCommand.getCurrentFogSettings();
            org.mirage.gfbs.Phenomenon.network.Network.NetworkHandler.sendToPlayer(player, "fog_settings", fogSettings);
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

                        GateUtils.getGateOpenState(level, GateTypes.CHECK_POINT_X6)
                                .ifPresent(open ->
                                        gfbs_gate_upd_joined_data.putBoolean("check_point_gate_x6", open)
                                );

                        GateUtils.getGateOpenState(level, GateTypes.TARTARUS_GATE)
                                .ifPresent(open ->
                                        gfbs_gate_upd_joined_data.putBoolean("tartarus_gate", open)
                                );

                        org.mirage.gfbs.Phenomenon.network.Network.NetworkHandler.sendToPlayer(player, "gfbs_gate_upd_joined", gfbs_gate_upd_joined_data);

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

        ServerConfigApi.init();
        GFBSServerConfig.init();
        LOGGER.info("Server config system initialized.");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event){
        LOGGER.info("server started.");

        MirageGFBsGateApiCommand.exec(server.overworld().getLevel(), true, "check_point_gate");
        MirageGFBsGateApiCommand.exec(server.overworld().getLevel(), true, "check_point_gate_x6");
        MirageGFBsGateApiCommand.exec(server.overworld().getLevel(), false, "gate");

        MirageGFBsGateApiCommand.exec(server.overworld().getLevel(), false, "tartarus_gate");
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

        BlackHoleCommand.register(event.getDispatcher());
        org.mirage.gfbs.Command.BlackHoleCommandRegistry.register(event.getDispatcher());

        // oh no...
        //MirageGFBsRWLCommand.register(event.getDispatcher());

        MirageGFBsBroadSystemCommand.register(event.getDispatcher());

        MirageGFBsTeamCommand.register(event.getDispatcher());

        MirageGFBsServerConfigCommand.register(event.getDispatcher());
    }

    public static CustomFogModule customFogModule;

    public static GameType ClientGameType;

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("CLIENT SETUP");

            customFogModule = new CustomFogModule();

            // RENDER REGISTER #114

            BlockEntityRenderers.register(ModBlockEntities.QS_TRADEMARK_PICTURE_BLOCK_ENTITY.get(), PictureBlockRenderer::new);

            // GATE
            BlockEntityRenderers.register(ModBlockEntities.GATE.get(), GateBlockRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CHECK_POINT_GATE.get(), GateBlockRenderer::new);

            BlockEntityRenderers.register(ModBlockEntities.COLORED_DOOR.get(), org.mirage.gfbs.objects.renderer.ColoredDoor.ColoredDoorRenderer::new);

            // END #114

            GateClientAPI.register();

            DmrexAfter.clientExec();
            EccEvent.clientExec();
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
        ServerConfigApi.saveToDisk();
        LOGGER.info("已保存所有本端配置.");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            WorldWriteQueue.flush();
            org.mirage.gfbs.Phenomenon.BlackHole.BlackHoleManager.tick();
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
