package org.mirage.gfbs.advanced.team;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import org.mirage.gfbs.advanced.team.network.TeamNetwork;
import org.mirage.gfbs.advanced.team.network.TeamStateS2CPacket;

import java.util.Optional;
import java.util.UUID;

public final class TeamService {
    private TeamService() {
    }

    public static Optional<TeamSavedData> get(MinecraftServer server) {
        if (server == null) return Optional.empty();
        var level = server.getLevel(Level.OVERWORLD);
        if (level == null) return Optional.empty();
        return Optional.of(TeamSavedData.get(level));
    }

    public static CompoundTag buildSyncTag(MinecraftServer server) {
        CompoundTag tag = new CompoundTag();
        var dataOpt = get(server);
        if (dataOpt.isEmpty()) return tag;
        TeamSavedData data = dataOpt.get();

        ListTag teams = new ListTag();
        for (TeamDefinition team : data.teamsView().values()) {
            CompoundTag t = new CompoundTag();
            t.putString("id", team.id());
            t.putString("name", team.displayName());
            t.putInt("rgb", team.rgb());
            t.putBoolean("same_pvp", team.sameTeamPvp());
            teams.add(t);
        }
        tag.put("teams", teams);

        ListTag members = new ListTag();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            UUID uuid = p.getUUID();
            data.getMembership(uuid).ifPresent(teamId -> {
                CompoundTag m = new CompoundTag();
                m.putString("uuid", uuid.toString());
                m.putString("team", teamId);
                members.add(m);
            });
        }
        tag.put("memberships_online", members);

        ListTag relations = new ListTag();
        for (var e : data.relationsView().entrySet()) {
            String[] ab = e.getKey().split("\\|", 2);
            if (ab.length != 2) continue;
            CompoundTag r = new CompoundTag();
            r.putString("a", ab[0]);
            r.putString("b", ab[1]);
            r.putString("rel", e.getValue().relation().name());
            r.putBoolean("pvp", e.getValue().pvpEnabled());
            relations.add(r);
        }
        tag.put("relations", relations);

        tag.putInt("v", 1);
        return tag;
    }

    public static void syncTo(ServerPlayer player) {
        if (player == null) return;
        TeamNetwork.sendToPlayer(player, new TeamStateS2CPacket(buildSyncTag(player.server)));
    }

    public static void syncToAll(MinecraftServer server) {
        if (server == null) return;
        TeamNetwork.sendToAll(server, new TeamStateS2CPacket(buildSyncTag(server)));
    }

    public static boolean canAttack(MinecraftServer server, UUID attacker, UUID victim) {
        if (server == null || attacker == null || victim == null) return true;
        if (attacker.equals(victim)) return true;

        var dataOpt = get(server);
        if (dataOpt.isEmpty()) return true;
        TeamSavedData data = dataOpt.get();

        String aTeam = data.getMembership(attacker).orElse(null);
        String vTeam = data.getMembership(victim).orElse(null);
        if (aTeam == null || vTeam == null) return true;

        if (aTeam.equals(vTeam)) {
            return data.getTeam(aTeam).map(TeamDefinition::sameTeamPvp).orElse(false);
        }

        TeamSavedData.Relation rel = data.getRelation(aTeam, vTeam);
        if (rel.relation() == TeamRelation.ALLY) return false;
        return rel.pvpEnabled();
    }
}
