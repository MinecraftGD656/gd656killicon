package org.mods.gd656killicon.common.scoreboard;

/** The meaning of the data, independent of the client's panel layout and filters. */
public enum ScoreboardType {
    TOTAL,
    CONQUEST_TEAM,
    CONQUEST_SOLO;

    public static ScoreboardType fromConquestLayout(int columns, String[] panelTeams) {
        if (columns == 1 && panelTeams != null && panelTeams.length > 0
            && "__conquest_solo__".equals(panelTeams[0])) {
            return CONQUEST_SOLO;
        }
        return columns == 2 || columns == 4 ? CONQUEST_TEAM : TOTAL;
    }
}
