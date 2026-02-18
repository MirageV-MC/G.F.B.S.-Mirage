package org.mirage.gfbs.advanced.team;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TeamSavedData extends SavedData {
    private static final String DATA_NAME = "mirage_gfbs_teams";

    public record Relation(TeamRelation relation, boolean pvpEnabled) {
    }

    private final Map<String, TeamDefinition> teams = new HashMap<>();
    private final Map<UUID, String> memberships = new HashMap<>();
    private final Map<String, Relation> relations = new HashMap<>();

    public static TeamSavedData load(CompoundTag tag) {
        TeamSavedData data = new TeamSavedData();

        if (tag.contains("teams", Tag.TAG_LIST)) {
            ListTag list = tag.getList("teams", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                String id = TeamDefinition.normalizeId(t.getString("id"));
                if (id.isEmpty()) continue;
                String name = t.getString("name");
                int rgb = t.contains("rgb", Tag.TAG_INT) ? t.getInt("rgb") : 0xFFFFFF;
                boolean samePvp = t.contains("same_pvp", Tag.TAG_BYTE) && t.getBoolean("same_pvp");
                data.teams.put(id, new TeamDefinition(id, name, rgb, samePvp));
            }
        }

        if (tag.contains("memberships", Tag.TAG_LIST)) {
            ListTag list = tag.getList("memberships", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag m = list.getCompound(i);
                if (!m.contains("uuid", Tag.TAG_STRING)) continue;
                String teamId = TeamDefinition.normalizeId(m.getString("team"));
                if (teamId.isEmpty()) continue;
                try {
                    UUID uuid = UUID.fromString(m.getString("uuid"));
                    data.memberships.put(uuid, teamId);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (tag.contains("relations", Tag.TAG_LIST)) {
            ListTag list = tag.getList("relations", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag r = list.getCompound(i);
                String a = TeamDefinition.normalizeId(r.getString("a"));
                String b = TeamDefinition.normalizeId(r.getString("b"));
                if (a.isEmpty() || b.isEmpty() || a.equals(b)) continue;
                TeamRelation rel;
                try {
                    rel = TeamRelation.valueOf(r.getString("rel").toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    rel = TeamRelation.ENEMY;
                }
                boolean pvp = !r.contains("pvp", Tag.TAG_BYTE) || r.getBoolean("pvp");
                data.relations.put(pairKey(a, b), new Relation(rel, pvp));
            }
        }

        data.sanitize();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag teamsList = new ListTag();
        for (TeamDefinition team : teams.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("id", team.id());
            t.putString("name", team.displayName());
            t.putInt("rgb", team.rgb());
            t.putBoolean("same_pvp", team.sameTeamPvp());
            teamsList.add(t);
        }
        tag.put("teams", teamsList);

        ListTag membersList = new ListTag();
        for (var e : memberships.entrySet()) {
            CompoundTag m = new CompoundTag();
            m.putString("uuid", e.getKey().toString());
            m.putString("team", e.getValue());
            membersList.add(m);
        }
        tag.put("memberships", membersList);

        ListTag relList = new ListTag();
        for (var e : relations.entrySet()) {
            String[] ab = e.getKey().split("\\|", 2);
            if (ab.length != 2) continue;
            CompoundTag r = new CompoundTag();
            r.putString("a", ab[0]);
            r.putString("b", ab[1]);
            r.putString("rel", e.getValue().relation().name());
            r.putBoolean("pvp", e.getValue().pvpEnabled());
            relList.add(r);
        }
        tag.put("relations", relList);

        return tag;
    }

    public static TeamSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(TeamSavedData::load, TeamSavedData::new, DATA_NAME);
    }

    public Map<String, TeamDefinition> teamsView() {
        return Collections.unmodifiableMap(teams);
    }

    public Map<UUID, String> membershipsView() {
        return Collections.unmodifiableMap(memberships);
    }

    public Map<String, Relation> relationsView() {
        return Collections.unmodifiableMap(relations);
    }

    public Optional<TeamDefinition> getTeam(String teamId) {
        return Optional.ofNullable(teams.get(TeamDefinition.normalizeId(teamId)));
    }

    public Optional<String> getMembership(UUID uuid) {
        return Optional.ofNullable(memberships.get(uuid));
    }

    public Relation getRelation(String a, String b) {
        String aa = TeamDefinition.normalizeId(a);
        String bb = TeamDefinition.normalizeId(b);
        if (aa.isEmpty() || bb.isEmpty() || aa.equals(bb)) return new Relation(TeamRelation.ENEMY, true);
        return relations.getOrDefault(pairKey(aa, bb), new Relation(TeamRelation.ENEMY, true));
    }

    public boolean createTeam(String id, String displayName, int rgb) {
        String norm = TeamDefinition.normalizeId(id);
        if (norm.isEmpty() || teams.containsKey(norm)) return false;
        teams.put(norm, new TeamDefinition(norm, displayName, rgb, false));
        setDirty();
        return true;
    }

    public boolean deleteTeam(String id) {
        String norm = TeamDefinition.normalizeId(id);
        if (!teams.containsKey(norm)) return false;
        teams.remove(norm);
        memberships.entrySet().removeIf(e -> Objects.equals(e.getValue(), norm));
        relations.keySet().removeIf(k -> k.startsWith(norm + "|") || k.endsWith("|" + norm));
        setDirty();
        return true;
    }

    public boolean setTeamColor(String id, int rgb) {
        String norm = TeamDefinition.normalizeId(id);
        TeamDefinition team = teams.get(norm);
        if (team == null) return false;
        team.setRgb(rgb);
        setDirty();
        return true;
    }

    public boolean setSameTeamPvp(String id, boolean canAttack) {
        String norm = TeamDefinition.normalizeId(id);
        TeamDefinition team = teams.get(norm);
        if (team == null) return false;
        team.setSameTeamPvp(canAttack);
        setDirty();
        return true;
    }

    public boolean joinTeam(UUID uuid, String teamId) {
        String norm = TeamDefinition.normalizeId(teamId);
        if (uuid == null || !teams.containsKey(norm)) return false;
        memberships.put(uuid, norm);
        setDirty();
        return true;
    }

    public boolean leaveTeam(UUID uuid) {
        if (uuid == null) return false;
        if (memberships.remove(uuid) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean setRelation(String a, String b, TeamRelation relation, boolean pvpEnabled) {
        String aa = TeamDefinition.normalizeId(a);
        String bb = TeamDefinition.normalizeId(b);
        if (aa.isEmpty() || bb.isEmpty() || aa.equals(bb)) return false;
        if (!teams.containsKey(aa) || !teams.containsKey(bb)) return false;
        TeamRelation rel = relation == null ? TeamRelation.ENEMY : relation;
        boolean pvp = rel != TeamRelation.ALLY && pvpEnabled;
        relations.put(pairKey(aa, bb), new Relation(rel, pvp));
        setDirty();
        return true;
    }

    private void sanitize() {
        memberships.entrySet().removeIf(e -> !teams.containsKey(e.getValue()));
        relations.entrySet().removeIf(e -> {
            String[] ab = e.getKey().split("\\|", 2);
            if (ab.length != 2) return true;
            if (!teams.containsKey(ab[0]) || !teams.containsKey(ab[1])) return true;
            if (ab[0].equals(ab[1])) return true;
            Relation r = e.getValue();
            if (r == null) return true;
            if (r.relation() == TeamRelation.ALLY && r.pvpEnabled()) {
                relations.put(e.getKey(), new Relation(TeamRelation.ALLY, false));
            }
            return false;
        });
    }

    public static String pairKey(String a, String b) {
        String aa = TeamDefinition.normalizeId(a);
        String bb = TeamDefinition.normalizeId(b);
        if (aa.compareTo(bb) <= 0) return aa + "|" + bb;
        return bb + "|" + aa;
    }
}
