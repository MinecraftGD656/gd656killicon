package org.mods.gd656killicon.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import org.mods.gd656killicon.client.bridge.ClientBridge;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class ScoreboardLoadoutConfigManager {
    public static final String TEAM_ALL = "__all__";
    public static final String TEAM_CONQUEST_SOLO = "__conquest_solo__";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONQUEST_MOD_ID = "gd656conquest";

    public enum DisplayMode {
        AUTO,
        SINGLE,
        DOUBLE,
        QUAD
    }

    private static DisplayMode persistedMode = DisplayMode.AUTO;
    private static final String[] persistedPanelTeams = new String[]{TEAM_ALL, TEAM_ALL, TEAM_ALL, TEAM_ALL};

    private static DisplayMode tempMode = DisplayMode.AUTO;
    private static final String[] tempPanelTeams = new String[]{TEAM_ALL, TEAM_ALL, TEAM_ALL, TEAM_ALL};

    private static boolean isEditing = false;
    private static String loadedContextId = null;
    private static int serverSuggestedColumns = 1;
    private static final String[] serverSuggestedPanelTeams = new String[]{TEAM_ALL, TEAM_ALL, TEAM_ALL, TEAM_ALL};

    private ScoreboardLoadoutConfigManager() {
    }

    public static void startEditing() {
        ensureContextLoaded();
        tempMode = persistedMode;
        copyArray(persistedPanelTeams, tempPanelTeams);
        isEditing = true;
    }

    public static void saveChanges() {
        if (!isEditing) {
            return;
        }
        persistedMode = tempMode;
        copyArray(tempPanelTeams, persistedPanelTeams);
        isEditing = false;
        saveCurrentContext();
    }

    public static void discardChanges() {
        if (!isEditing) {
            return;
        }
        isEditing = false;
        tempMode = persistedMode;
        copyArray(persistedPanelTeams, tempPanelTeams);
    }

    public static boolean hasUnsavedChanges() {
        if (!isEditing) {
            return false;
        }
        if (tempMode != persistedMode) {
            return true;
        }
        for (int i = 0; i < 4; i++) {
            if (!normalizeTeam(tempPanelTeams[i]).equals(normalizeTeam(persistedPanelTeams[i]))) {
                return true;
            }
        }
        return false;
    }

    public static void setDisplayMode(DisplayMode mode) {
        ensureContextLoaded();
        if (mode == null) {
            return;
        }
        if (isDisplayModeLockedToAuto() && mode != DisplayMode.AUTO) {
            return;
        }
        if (isEditing) {
            tempMode = mode;
        } else {
            persistedMode = mode;
            saveCurrentContext();
        }
    }

    public static DisplayMode getDisplayMode() {
        ensureContextLoaded();
        if (isDisplayModeLockedToAuto()) {
            return DisplayMode.AUTO;
        }
        return isEditing ? tempMode : persistedMode;
    }

    public static boolean isDisplayModeLockedToAuto() {
        return ClientBridge.loader().isModLoaded(CONQUEST_MOD_ID);
    }

    public static void setPanelTeamBinding(int panelIndex, String teamName) {
        ensureContextLoaded();
        if (panelIndex < 0 || panelIndex >= 4) {
            return;
        }
        String normalized = normalizeTeam(teamName);
        if (isEditing) {
            tempPanelTeams[panelIndex] = normalized;
        } else {
            persistedPanelTeams[panelIndex] = normalized;
            saveCurrentContext();
        }
    }

    public static String getPanelTeamBinding(int panelIndex) {
        ensureContextLoaded();
        if (panelIndex < 0 || panelIndex >= 4) {
            return TEAM_ALL;
        }
        return normalizeTeam(isEditing ? tempPanelTeams[panelIndex] : persistedPanelTeams[panelIndex]);
    }

    public static void setServerSuggestedColumns(int columns) {
        if (columns == 2 || columns == 4) {
            serverSuggestedColumns = columns;
        } else {
            serverSuggestedColumns = 1;
        }
    }

    public static void setServerSuggestedPanelTeams(String[] panelTeams) {
        for (int i = 0; i < 4; i++) {
            if (panelTeams != null && i < panelTeams.length) {
                serverSuggestedPanelTeams[i] = normalizeTeam(panelTeams[i]);
            } else {
                serverSuggestedPanelTeams[i] = TEAM_ALL;
            }
        }
    }

    public static int getEffectiveColumns(boolean inGame) {
        if (!inGame) {
            return 1;
        }
        if (isServerForcingSoloMode()) {
            return 1;
        }
        DisplayMode mode = getDisplayMode();
        return switch (mode) {
            case SINGLE -> 1;
            case DOUBLE -> 2;
            case QUAD -> 4;
            case AUTO -> serverSuggestedColumns;
        };
    }

    public static String getEffectivePanelTeamBinding(int panelIndex, boolean inGame) {
        if (panelIndex < 0 || panelIndex >= 4) {
            return TEAM_ALL;
        }
        if (!inGame) {
            return TEAM_ALL;
        }
        if (isServerForcingSoloMode()) {
            return panelIndex == 0 ? TEAM_ALL : "";
        }
        DisplayMode mode = getDisplayMode();
        if (mode == DisplayMode.AUTO) {
            return normalizeTeam(serverSuggestedPanelTeams[panelIndex]);
        }
        return getPanelTeamBinding(panelIndex);
    }

    public static boolean isServerForcingSoloMode() {
        return serverSuggestedColumns == 1 && TEAM_CONQUEST_SOLO.equals(normalizeTeam(serverSuggestedPanelTeams[0]));
    }

    private static void ensureContextLoaded() {
        String contextId = resolveContextId();
        if (contextId == null) {
            loadedContextId = null;
            persistedMode = DisplayMode.AUTO;
            for (int i = 0; i < 4; i++) {
                persistedPanelTeams[i] = TEAM_ALL;
            }
            if (!isEditing) {
                tempMode = DisplayMode.AUTO;
                for (int i = 0; i < 4; i++) {
                    tempPanelTeams[i] = TEAM_ALL;
                }
            }
            return;
        }
        if (contextId.equals(loadedContextId)) {
            return;
        }
        loadedContextId = contextId;
        loadCurrentContext();
        if (!isEditing) {
            tempMode = persistedMode;
            copyArray(persistedPanelTeams, tempPanelTeams);
        }
    }

    private static void loadCurrentContext() {
        persistedMode = DisplayMode.AUTO;
        for (int i = 0; i < 4; i++) {
            persistedPanelTeams[i] = TEAM_ALL;
        }
        Path file = getContextFilePath();
        if (file == null || !Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                return;
            }
            if (json.has("display_mode")) {
                try {
                    persistedMode = DisplayMode.valueOf(json.get("display_mode").getAsString().toUpperCase(Locale.ROOT));
                } catch (Exception ignored) {
                    persistedMode = DisplayMode.AUTO;
                }
            }
            for (int i = 0; i < 4; i++) {
                String key = "panel_team_" + (i + 1);
                if (json.has(key)) {
                    persistedPanelTeams[i] = normalizeTeam(json.get(key).getAsString());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveCurrentContext() {
        Path file = getContextFilePath();
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("display_mode", persistedMode.name().toLowerCase(Locale.ROOT));
            for (int i = 0; i < 4; i++) {
                json.addProperty("panel_team_" + (i + 1), normalizeTeam(persistedPanelTeams[i]));
            }
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception ignored) {
        }
    }

    private static Path getContextFilePath() {
        String contextId = resolveContextId();
        if (contextId == null) {
            return null;
        }
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        return gameDir
            .resolve("data")
            .resolve("gd656killicon")
            .resolve("scoreboard_layout")
            .resolve(contextId + ".json");
    }

    private static String resolveContextId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) {
            return null;
        }
        try {
            if (mc.getSingleplayerServer() != null) {
                Path root = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
                String folder = root.getFileName() == null ? root.toString() : root.getFileName().toString();
                return "local_" + sanitize(folder);
            }
        } catch (Exception ignored) {
        }
        try {
            if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null && !mc.getCurrentServer().ip.isEmpty()) {
                return "remote_" + sanitize(mc.getCurrentServer().ip);
            }
        } catch (Exception ignored) {
        }
        return "local_unknown";
    }

    private static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static void copyArray(String[] from, String[] to) {
        for (int i = 0; i < 4; i++) {
            to[i] = normalizeTeam(from[i]);
        }
    }

    private static String normalizeTeam(String team) {
        if (team == null || team.isEmpty()) {
            return TEAM_ALL;
        }
        return team;
    }
}
