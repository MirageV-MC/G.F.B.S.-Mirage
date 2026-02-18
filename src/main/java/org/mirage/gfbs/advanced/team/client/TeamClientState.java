package org.mirage.gfbs.advanced.team.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import org.mirage.gfbs.advanced.team.TeamRelation;
import org.mirage.gfbs.advanced.team.TeamSavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TeamClientState {
    public record ClientTeam(String id, String name, int rgb, boolean sameTeamPvp) {
    }

    private static Map<String, ClientTeam> teams = new HashMap<>();
    private static Map<UUID, String> membershipsOnline = new HashMap<>();
    private static Map<String, TeamSavedData.Relation> relations = new HashMap<>();

    private TeamClientState() {
    }

    public static void apply(CompoundTag tag) {
        if (tag == null) return;

        Map<String, ClientTeam> newTeams = new HashMap<>();
        Map<UUID, String> newMembers = new HashMap<>();
        Map<String, TeamSavedData.Relation> newRelations = new HashMap<>();

        if (tag.contains("teams", Tag.TAG_LIST)) {
            ListTag list = tag.getList("teams", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                String id = t.getString("id");
                if (id == null || id.isBlank()) continue;
                String name = t.getString("name");
                int rgb = t.contains("rgb", Tag.TAG_INT) ? t.getInt("rgb") : 0xFFFFFF;
                boolean samePvp = t.contains("same_pvp", Tag.TAG_BYTE) && t.getBoolean("same_pvp");
                newTeams.put(id, new ClientTeam(id, name, rgb, samePvp));
            }
        }

        if (tag.contains("memberships_online", Tag.TAG_LIST)) {
            ListTag list = tag.getList("memberships_online", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag m = list.getCompound(i);
                String uuidStr = m.getString("uuid");
                String team = m.getString("team");
                if (uuidStr == null || uuidStr.isBlank()) continue;
                if (team == null || team.isBlank()) continue;
                try {
                    newMembers.put(UUID.fromString(uuidStr), team);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (tag.contains("relations", Tag.TAG_LIST)) {
            ListTag list = tag.getList("relations", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag r = list.getCompound(i);
                String a = r.getString("a");
                String b = r.getString("b");
                if (a == null || a.isBlank() || b == null || b.isBlank() || a.equals(b)) continue;
                TeamRelation rel;
                try {
                    rel = TeamRelation.valueOf(r.getString("rel").toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    rel = TeamRelation.ENEMY;
                }
                boolean pvp = !r.contains("pvp", Tag.TAG_BYTE) || r.getBoolean("pvp");
                newRelations.put(TeamSavedData.pairKey(a, b), new TeamSavedData.Relation(rel, pvp));
            }
        }

        teams = newTeams;
        membershipsOnline = newMembers;
        relations = newRelations;
    }

    public static Map<String, ClientTeam> teamsView() {
        return Collections.unmodifiableMap(teams);
    }

    public static Map<UUID, String> membershipsOnlineView() {
        return Collections.unmodifiableMap(membershipsOnline);
    }

    public static TeamSavedData.Relation relation(String a, String b) {
        return relations.getOrDefault(TeamSavedData.pairKey(a, b), new TeamSavedData.Relation(TeamRelation.ENEMY, true));
    }
}
