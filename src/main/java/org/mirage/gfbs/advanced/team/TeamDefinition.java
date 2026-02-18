package org.mirage.gfbs.advanced.team;

import java.util.Locale;
import java.util.Objects;

public final class TeamDefinition {
    private final String id;
    private String displayName;
    private int rgb;
    private boolean sameTeamPvp;

    public TeamDefinition(String id, String displayName, int rgb, boolean sameTeamPvp) {
        this.id = normalizeId(id);
        this.displayName = Objects.requireNonNullElse(displayName, this.id);
        this.rgb = rgb;
        this.sameTeamPvp = sameTeamPvp;
    }

    public static String normalizeId(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = Objects.requireNonNullElse(displayName, id);
    }

    public int rgb() {
        return rgb;
    }

    public void setRgb(int rgb) {
        this.rgb = rgb;
    }

    public boolean sameTeamPvp() {
        return sameTeamPvp;
    }

    public void setSameTeamPvp(boolean sameTeamPvp) {
        this.sameTeamPvp = sameTeamPvp;
    }
}
