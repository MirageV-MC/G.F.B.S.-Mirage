package org.mirage.gfbs.advanced.team.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.mirage.gfbs.MirageGFBS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MirageGFBS.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TeamOverlay {
    private static final TeamOverlay INSTANCE = new TeamOverlay();

    private static final int ANIM_TICKS = 8;
    private static final int PANEL_W = 210;
    private static final int PANEL_MARGIN = 8;
    private static final int PANEL_TOP = 20;

    private float progress = 0f;

    public static final IGuiOverlay OVERLAY = (ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) ->
            INSTANCE.render(graphics, partialTick, screenWidth, screenHeight);

    private TeamOverlay() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        if (mc.isPaused()) return;
        INSTANCE.tick(mc);
    }

    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_LIST.type()) {
            if (INSTANCE.isActive()) {
                event.setCanceled(true);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MirageGFBS.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.PLAYER_LIST.id(), "gfbs_teams", TeamOverlay.OVERLAY);
        }
    }

    private void tick(Minecraft mc) {
        boolean down = mc.options.keyPlayerList.isDown();
        float step = 1f / ANIM_TICKS;
        if (down) {
            progress = Math.min(1f, progress + step);
        } else {
            progress = Math.max(0f, progress - step);
        }
    }

    private boolean isActive() {
        return progress > 0.001f;
    }

    private static float smoothInOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private void render(GuiGraphics g, float partialTick, int sw, int sh) {
        if (!isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.getConnection() == null) return;

        float eased = smoothInOut(progress);
        int hideOffset = PANEL_W + 10;
        int right = sw - PANEL_MARGIN;
        int x = (int) (right - PANEL_W + (1f - eased) * hideOffset);
        int y = PANEL_TOP;

        int headerH = 18;
        int lineH = 12;

        Map<String, List<String>> teamToPlayers = new HashMap<>();
        List<String> noTeamPlayers = new ArrayList<>();

        Map<UUID, String> membership = TeamClientState.membershipsOnlineView();
        List<PlayerInfo> online = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        online.sort(Comparator.comparing(p -> p.getProfile().getName(), String::compareToIgnoreCase));
        for (PlayerInfo info : online) {
            UUID uuid = info.getProfile().getId();
            String name = info.getProfile().getName();
            String team = membership.get(uuid);
            if (team == null) {
                noTeamPlayers.add(name);
            } else {
                teamToPlayers.computeIfAbsent(team, k -> new ArrayList<>()).add(name);
            }
        }

        List<TeamClientState.ClientTeam> teams = new ArrayList<>(TeamClientState.teamsView().values());
        teams.sort(Comparator.comparing(TeamClientState.ClientTeam::name, String::compareToIgnoreCase));

        int maxLines = Math.max(4, (sh - y - 16) / lineH);
        int usedLines = 0;
        int contentLines = 1;
        for (TeamClientState.ClientTeam t : teams) {
            List<String> players = teamToPlayers.getOrDefault(t.id(), List.of());
            contentLines += 1 + players.size();
        }
        if (!noTeamPlayers.isEmpty()) contentLines += 1 + noTeamPlayers.size();

        int panelH = headerH + 8 + Math.min(maxLines, contentLines) * lineH + 6;
        int bg = 0xAA0E0E0E;
        int header = 0xCC1A1A1A;

        RenderSystem.enableBlend();
        g.fill(x, y, x + PANEL_W, y + panelH, bg);
        g.fill(x, y, x + PANEL_W, y + headerH, header);
        g.drawString(mc.font, "Teams", x + 8, y + 5, 0xFFFFFF, false);
        String brand = "G.F.B.S. Sys.";
        int brandX = x + PANEL_W - 8 - mc.font.width(brand);
        g.drawString(mc.font, brand, brandX, y + 5, 0xE0E0E0, false);

        int cy = y + headerH + 6;
        usedLines++;

        for (TeamClientState.ClientTeam t : teams) {
            if (usedLines >= maxLines) break;
            int barColor = 0xFF000000 | (t.rgb() & 0x00FFFFFF);
            g.fill(x + 6, cy + 2, x + 10, cy + lineH - 2, barColor);
            g.drawString(mc.font, t.name(), x + 14, cy + 2, 0xFFFFFF, false);
            cy += lineH;
            usedLines++;

            List<String> players = teamToPlayers.getOrDefault(t.id(), List.of());
            for (String p : players) {
                if (usedLines >= maxLines) break;
                g.drawString(mc.font, p, x + 22, cy + 2, 0xEAEAEA, false);
                cy += lineH;
                usedLines++;
            }
        }

        if (usedLines < maxLines && !noTeamPlayers.isEmpty()) {
            g.drawString(mc.font, "No Team", x + 14, cy + 2, 0xBEBEBE, false);
            cy += lineH;
            usedLines++;

            for (String p : noTeamPlayers) {
                if (usedLines >= maxLines) break;
                g.drawString(mc.font, p, x + 22, cy + 2, 0xD0D0D0, false);
                cy += lineH;
                usedLines++;
            }
        }
    }
}
