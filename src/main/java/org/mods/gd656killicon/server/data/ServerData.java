package org.mods.gd656killicon.server.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.util.ServerLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ServerData INSTANCE = new ServerData();

    private double comboWindowSeconds = 5.0;
    private int assistTimeoutSeconds = 180;
    private int scoreMaxLimit = Integer.MAX_VALUE;
    private String scoreboardDisplayName = "Player Score";
    private String killboardDisplayName = "Player Kills";
    private String deathboardDisplayName = "Player Deaths";
    private String assistboardDisplayName = "Player Assists";
    private final Set<Integer> disabledBonusTypes = ConcurrentHashMap.newKeySet();
    private final Map<Integer, String> bonusExpressions = new ConcurrentHashMap<>();
    private final Map<Integer, String> defaultExpressions = new HashMap<>();

    private Path configPath;
    private boolean loaded = false;

    private ServerData() {
        initDefaults();
    }

    private void initDefaults() {
        defaultExpressions.put(BonusType.KILL, "5");
        defaultExpressions.put(BonusType.KILL_HEADSHOT, "5");
        defaultExpressions.put(BonusType.KILL_EXPLOSION, "5");
        defaultExpressions.put(BonusType.KILL_CRIT, "5");
        defaultExpressions.put(BonusType.KILL_LONG_DISTANCE, "1");
        defaultExpressions.put(BonusType.KILL_COMBO, "8");
        defaultExpressions.put(BonusType.ONE_BULLET_MULTI_KILL, "8");
        defaultExpressions.put(BonusType.SHOCKWAVE, "20");
        defaultExpressions.put(BonusType.LAST_BULLET_KILL, "30");
        defaultExpressions.put(BonusType.EFFORTLESS_KILL, "8");
        defaultExpressions.put(BonusType.BRAVE_RETURN, "12");
        defaultExpressions.put(BonusType.BERSERKER, "3");
        defaultExpressions.put(BonusType.KILL_INVISIBLE, "10");
        defaultExpressions.put(BonusType.DESPERATE_COUNTERATTACK, "12");
        defaultExpressions.put(BonusType.ABSOLUTE_AIR_CONTROL, "20");
        defaultExpressions.put(BonusType.JUSTICE_FROM_ABOVE, "10");
        defaultExpressions.put(BonusType.BACKSTAB_MELEE_KILL, "15");
        defaultExpressions.put(BonusType.BACKSTAB_KILL, "8");
        defaultExpressions.put(BonusType.BLIND_KILL, "12");
        defaultExpressions.put(BonusType.BOTH_BUFF_DEBUFF_KILL, "10");
        defaultExpressions.put(BonusType.BUFF_KILL, "5");
        defaultExpressions.put(BonusType.DEBUFF_KILL, "7");
        defaultExpressions.put(BonusType.AVENGE, "25");
        defaultExpressions.put(BonusType.ASSIST, "1");
        defaultExpressions.put(BonusType.INTERRUPTED_STREAK, "6");
        defaultExpressions.put(BonusType.LEAVE_IT_TO_ME, "20");
        defaultExpressions.put(BonusType.SAVIOR, "10");
        defaultExpressions.put(BonusType.SLAY_THE_LEADER, "18");
        defaultExpressions.put(BonusType.PURGE, "30");
        defaultExpressions.put(BonusType.QUICK_SWITCH, "3");
        defaultExpressions.put(BonusType.SEIZE_OPPORTUNITY, "5");
        defaultExpressions.put(BonusType.BLOODTHIRSTY, "5");
        defaultExpressions.put(BonusType.MERCILESS, "10");
        defaultExpressions.put(BonusType.VALIANT, "15");
        defaultExpressions.put(BonusType.FIERCE, "20");
        defaultExpressions.put(BonusType.SAVAGE, "25");
        defaultExpressions.put(BonusType.POTATO_AIM, "5");
        defaultExpressions.put(BonusType.HIT_VEHICLE_ARMOR, "1.5");
        defaultExpressions.put(BonusType.DESTROY_VEHICLE, "1.5");
        defaultExpressions.put(BonusType.VEHICLE_REPAIR, "10");
        defaultExpressions.put(BonusType.VALUE_TARGET_DESTROYED, "1");
    
        defaultExpressions.put(BonusType.DAMAGE, "1");
        defaultExpressions.put(BonusType.HEADSHOT, "1");
        defaultExpressions.put(BonusType.EXPLOSION, "1");
        defaultExpressions.put(BonusType.CRIT, "1");
    }

    public static ServerData get() { return INSTANCE; }

    public void init(MinecraftServer server) {
        if (loaded) return;
        Path root = server.getWorldPath(LevelResource.ROOT).resolve("gd656killicon");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            ServerLog.error("Failed to create config directory: %s", e.getMessage());
        }
        this.configPath = root.resolve("server_config.json");
        load();
        PlayerDataManager.get().init(server);
        initScoreboard(server);
        loaded = true;
    }

    public void saveAll() {
        if (!loaded) return;
        saveConfig();
        PlayerDataManager.get().forceSave();
    }

    // --- Config Accessors ---
    public double getComboWindowSeconds() { return comboWindowSeconds; }
    public long getComboWindowMs() { return (long) (comboWindowSeconds * 1000.0); }
    public void setComboWindowSeconds(double val) { this.comboWindowSeconds = Math.max(0.1, val); saveConfig(); }

    public int getAssistTimeoutSeconds() { return assistTimeoutSeconds; }
    public long getAssistTimeoutMs() { return (long) assistTimeoutSeconds * 1000L; }
    public void setAssistTimeoutSeconds(int val) { this.assistTimeoutSeconds = Math.max(1, val); saveConfig(); }

    public int getScoreMaxLimit() { return scoreMaxLimit; }
    public void setScoreMaxLimit(int val) { this.scoreMaxLimit = Math.max(0, val); saveConfig(); }

    public String getScoreboardDisplayName() { return scoreboardDisplayName; }

    public void setScoreboardDisplayName(String name) {
        this.scoreboardDisplayName = name;
        saveConfig();
        
        // Update existing scoreboard display name
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            net.minecraft.world.scores.Scoreboard scoreboard = server.getScoreboard();
            net.minecraft.world.scores.Objective objective = scoreboard.getObjective(SCOREBOARD_OBJECTIVE);
            if (objective != null) {
                objective.setDisplayName(net.minecraft.network.chat.Component.literal(name));
            }
        }
    }

    public String getKillboardDisplayName() { return killboardDisplayName; }

    public void setKillboardDisplayName(String name) {
        this.killboardDisplayName = name;
        saveConfig();
        
        // Update existing killboard display name
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            net.minecraft.world.scores.Scoreboard scoreboard = server.getScoreboard();
            net.minecraft.world.scores.Objective objective = scoreboard.getObjective(KILLBOARD_OBJECTIVE);
            if (objective != null) {
                objective.setDisplayName(net.minecraft.network.chat.Component.literal(name));
            }
        }
    }

    public String getDeathboardDisplayName() { return deathboardDisplayName; }

    public void setDeathboardDisplayName(String name) {
        this.deathboardDisplayName = name;
        saveConfig();
        
        // Update existing deathboard display name
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            net.minecraft.world.scores.Scoreboard scoreboard = server.getScoreboard();
            net.minecraft.world.scores.Objective objective = scoreboard.getObjective(DEATHBOARD_OBJECTIVE);
            if (objective != null) {
                objective.setDisplayName(net.minecraft.network.chat.Component.literal(name));
            }
        }
    }

    public String getAssistboardDisplayName() { return assistboardDisplayName; }

    public void setAssistboardDisplayName(String name) {
        this.assistboardDisplayName = name;
        saveConfig();
        
        // Update existing assistboard display name
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            net.minecraft.world.scores.Scoreboard scoreboard = server.getScoreboard();
            net.minecraft.world.scores.Objective objective = scoreboard.getObjective(ASSISTBOARD_OBJECTIVE);
            if (objective != null) {
                objective.setDisplayName(net.minecraft.network.chat.Component.literal(name));
            }
        }
    }
    
    public boolean isBonusEnabled(int type) {
        return !disabledBonusTypes.contains(type);
    }

    public void setBonusEnabled(int type, boolean enabled) {
        if (enabled) {
            disabledBonusTypes.remove(type);
        } else {
            disabledBonusTypes.add(type);
        }
        saveConfig();
    }
    
    public String getBonusExpression(int type) {
        return bonusExpressions.getOrDefault(type, defaultExpressions.getOrDefault(type, "0"));
    }

    public void setBonusExpression(int type, String expression) {
        bonusExpressions.put(type, expression);
        saveConfig();
    }

    public double getBonusMultiplier(int type) {
        String expr = getBonusExpression(type);
        try {
            return Double.parseDouble(expr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public void resetConfig() {
        comboWindowSeconds = 5.0;
        assistTimeoutSeconds = 180;
        scoreMaxLimit = Integer.MAX_VALUE;
        scoreboardDisplayName = "玩家分数";
        killboardDisplayName = "玩家击杀";
        deathboardDisplayName = "玩家死亡";
        assistboardDisplayName = "玩家助攻";
        disabledBonusTypes.clear();
        bonusExpressions.clear();
        saveConfig();
    }
    
    public void resetBonusConfig() {
        disabledBonusTypes.clear();
        bonusExpressions.clear();
        saveConfig();
    }

    public float getScore(UUID uuid) {
        return PlayerDataManager.get().getScore(uuid);
    }

    public Map<UUID, Float> getAllScores() {
        return PlayerDataManager.get().getAllScores();
    }

    public boolean isTopScorer(UUID uuid) {
        Map<UUID, Float> allScores = getAllScores();
        if (allScores.isEmpty()) return false;
        float score = getScore(uuid);
        if (score <= 0) return false;

        for (float s : allScores.values()) {
            if (s > score) return false;
        }
        return true;
    }

    public void addScore(ServerPlayer player, float amount) {
        if (amount == 0) return;
        UUID uuid = player.getUUID();
        float current = getScore(uuid);

        double potential = (double) current + amount;
        float next = (float) Math.min(potential, (double) scoreMaxLimit);

        PlayerDataManager.get().setScore(uuid, next);
        updateScoreboard(player, next);
    }

    public void setScore(ServerPlayer player, float amount) {
        UUID uuid = player.getUUID();
        float next = Math.max(0, Math.min(amount, scoreMaxLimit));
        PlayerDataManager.get().setScore(uuid, next);
        updateScoreboard(player, next);
    }

    public void setAllScores(MinecraftServer server, float amount) {
        float next = Math.max(0, Math.min(amount, scoreMaxLimit));
        PlayerDataManager.get().getAllScores().keySet().forEach(uuid -> {
            PlayerDataManager.get().setScore(uuid, next);
        });

        refreshScoreboard(server);
    }

    public int getKill(UUID uuid) {
        return PlayerDataManager.get().getKill(uuid);
    }

    public Map<UUID, Integer> getAllKills() {
        return PlayerDataManager.get().getAllKills();
    }

    public boolean isTopKiller(UUID uuid) {
        Map<UUID, Integer> allKills = getAllKills();
        if (allKills.isEmpty()) return false;
        int kill = getKill(uuid);
        if (kill <= 0) return false;

        for (int k : allKills.values()) {
            if (k > kill) return false;
        }
        return true;
    }

    public void addKill(ServerPlayer player, int amount) {
        if (amount == 0) return;
        UUID uuid = player.getUUID();
        int current = getKill(uuid);

        long potential = (long) current + amount;
        int next = (int) Math.min(potential, (long) Integer.MAX_VALUE);

        PlayerDataManager.get().setKill(uuid, next);
        updateKillboard(player, next);
    }

    public void setKill(ServerPlayer player, int amount) {
        UUID uuid = player.getUUID();
        int next = Math.max(0, amount);
        PlayerDataManager.get().setKill(uuid, next);
        updateKillboard(player, next);
    }

    public void setAllKills(MinecraftServer server, int amount) {
        int next = Math.max(0, amount);
        PlayerDataManager.get().getAllKills().keySet().forEach(uuid -> {
            PlayerDataManager.get().setKill(uuid, next);
        });

        refreshScoreboard(server);
    }

    public int getDeath(UUID uuid) {
        return PlayerDataManager.get().getDeath(uuid);
    }

    public Map<UUID, Integer> getAllDeaths() {
        return PlayerDataManager.get().getAllDeaths();
    }

    public boolean isTopDead(UUID uuid) {
        Map<UUID, Integer> allDeaths = getAllDeaths();
        if (allDeaths.isEmpty()) return false;
        int death = getDeath(uuid);
        if (death <= 0) return false;

        for (int d : allDeaths.values()) {
            if (d > death) return false;
        }
        return true;
    }

    public void addDeath(ServerPlayer player, int amount) {
        if (amount == 0) return;
        UUID uuid = player.getUUID();
        int current = getDeath(uuid);

        long potential = (long) current + amount;
        int next = (int) Math.min(potential, (long) Integer.MAX_VALUE);

        PlayerDataManager.get().setDeath(uuid, next);
        updateDeathboard(player, next);
    }

    public void setDeath(ServerPlayer player, int amount) {
        UUID uuid = player.getUUID();
        int next = Math.max(0, amount);
        PlayerDataManager.get().setDeath(uuid, next);
        updateDeathboard(player, next);
    }

    public void setAllDeaths(MinecraftServer server, int amount) {
        int next = Math.max(0, amount);
        PlayerDataManager.get().getAllDeaths().keySet().forEach(uuid -> {
            PlayerDataManager.get().setDeath(uuid, next);
        });

        refreshScoreboard(server);
    }

    public int getAssist(UUID uuid) {
        return PlayerDataManager.get().getAssist(uuid);
    }

    public Map<UUID, Integer> getAllAssists() {
        return PlayerDataManager.get().getAllAssists();
    }

    public void addAssist(ServerPlayer player, int amount) {
        if (amount == 0) return;
        UUID uuid = player.getUUID();
        int current = getAssist(uuid);

        long potential = (long) current + amount;
        int next = (int) Math.min(potential, (long) Integer.MAX_VALUE);

        PlayerDataManager.get().setAssist(uuid, next);
        updateAssistboard(player, next);
    }

    public void setAssist(ServerPlayer player, int amount) {
        UUID uuid = player.getUUID();
        int next = Math.max(0, amount);
        PlayerDataManager.get().setAssist(uuid, next);
        updateAssistboard(player, next);
    }

    public void setAllAssists(MinecraftServer server, int amount) {
        int next = Math.max(0, amount);
        PlayerDataManager.get().getAllAssists().keySet().forEach(uuid -> {
            PlayerDataManager.get().setAssist(uuid, next);
        });

        refreshScoreboard(server);
    }

    public void refreshScoreboard(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        
        // Refresh score objective
        Objective scoreObjective = scoreboard.getObjective(SCOREBOARD_OBJECTIVE);
        if (scoreObjective != null) {
            // Clear existing scores
            clearScoreboardScores(scoreboard, scoreObjective);
            
            // Sync all scores
            PlayerDataManager.get().getAllScores().forEach((uuid, score) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, scoreObjective).setScore(Math.round(score));
            });
        }
        
        // Refresh kill objective
        Objective killObjective = scoreboard.getObjective(KILLBOARD_OBJECTIVE);
        if (killObjective != null) {
            // Clear existing kills
            clearScoreboardScores(scoreboard, killObjective);
            
            // Sync all kills
            PlayerDataManager.get().getAllKills().forEach((uuid, kill) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, killObjective).setScore(kill);
            });
        }
        
        // Refresh death objective
        Objective deathObjective = scoreboard.getObjective(DEATHBOARD_OBJECTIVE);
        if (deathObjective != null) {
            // Clear existing deaths
            clearScoreboardScores(scoreboard, deathObjective);
            
            // Sync all deaths
            PlayerDataManager.get().getAllDeaths().forEach((uuid, death) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, deathObjective).setScore(death);
            });
        }
        
        // Refresh assist objective
        Objective assistObjective = scoreboard.getObjective(ASSISTBOARD_OBJECTIVE);
        if (assistObjective != null) {
            // Clear existing assists
            clearScoreboardScores(scoreboard, assistObjective);
            
            // Sync all assists
            PlayerDataManager.get().getAllAssists().forEach((uuid, assist) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, assistObjective).setScore(assist);
            });
        }
    }

    private void clearScoreboardScores(Scoreboard scoreboard, Objective objective) {
        // Clear all scores for this objective
        scoreboard.getPlayerScores(objective).forEach(score -> {
            scoreboard.resetPlayerScore(score.getOwner(), objective);
        });
    }

    // --- Scoreboard Logic ---
    public static final String SCOREBOARD_OBJECTIVE = "gd656killicon.score";
    public static final String KILLBOARD_OBJECTIVE = "gd656killicon.kill";
    public static final String DEATHBOARD_OBJECTIVE = "gd656killicon.death";
    public static final String ASSISTBOARD_OBJECTIVE = "gd656killicon.assist";
    
    public void initScoreboard(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        
        // Initialize score objective
        Objective scoreObjective = scoreboard.getObjective(SCOREBOARD_OBJECTIVE);
        if (scoreObjective == null) {
            scoreObjective = scoreboard.addObjective(SCOREBOARD_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(scoreboardDisplayName), ObjectiveCriteria.RenderType.INTEGER);
        } else {
            scoreObjective.setDisplayName(Component.literal(scoreboardDisplayName));
        }

        // Sync all existing scores to the objective
        final Objective finalScoreObj = scoreObjective;
        PlayerDataManager.get().getAllScores().forEach((uuid, score) -> {
            String scoreHolderName = getScoreHolderName(server, uuid);
            scoreboard.getOrCreatePlayerScore(scoreHolderName, finalScoreObj).setScore(Math.round(score));
        });
        
        // Initialize kill objective
        Objective killObjective = scoreboard.getObjective(KILLBOARD_OBJECTIVE);
        if (killObjective == null) {
            killObjective = scoreboard.addObjective(KILLBOARD_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(killboardDisplayName), ObjectiveCriteria.RenderType.INTEGER);
        } else {
            killObjective.setDisplayName(Component.literal(killboardDisplayName));
        }

        // Sync all existing kills to the objective
        final Objective finalKillObj = killObjective;
        PlayerDataManager.get().getAllKills().forEach((uuid, kill) -> {
            String scoreHolderName = getScoreHolderName(server, uuid);
            scoreboard.getOrCreatePlayerScore(scoreHolderName, finalKillObj).setScore(kill);
        });
        
        // Initialize death objective
        Objective deathObjective = scoreboard.getObjective(DEATHBOARD_OBJECTIVE);
        if (deathObjective == null) {
            deathObjective = scoreboard.addObjective(DEATHBOARD_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(deathboardDisplayName), ObjectiveCriteria.RenderType.INTEGER);
        } else {
            deathObjective.setDisplayName(Component.literal(deathboardDisplayName));
        }

        // Sync all existing deaths to the objective
        final Objective finalDeathObj = deathObjective;
        PlayerDataManager.get().getAllDeaths().forEach((uuid, death) -> {
            String scoreHolderName = getScoreHolderName(server, uuid);
            scoreboard.getOrCreatePlayerScore(scoreHolderName, finalDeathObj).setScore(death);
        });
        
        // Initialize assist objective
        Objective assistObjective = scoreboard.getObjective(ASSISTBOARD_OBJECTIVE);
        if (assistObjective == null) {
            assistObjective = scoreboard.addObjective(ASSISTBOARD_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(assistboardDisplayName), ObjectiveCriteria.RenderType.INTEGER);
        } else {
            assistObjective.setDisplayName(Component.literal(assistboardDisplayName));
        }

        // Sync all existing assists to the objective
        final Objective finalAssistObj = assistObjective;
        PlayerDataManager.get().getAllAssists().forEach((uuid, assist) -> {
            String scoreHolderName = getScoreHolderName(server, uuid);
            scoreboard.getOrCreatePlayerScore(scoreHolderName, finalAssistObj).setScore(assist);
        });
    }

    public String getScoreHolderName(MinecraftServer server, UUID uuid) {
        // 1. Try online players
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getScoreboardName();

        // 2. Try profile cache
        var profile = server.getProfileCache().get(uuid);
        if (profile.isPresent()) return profile.get().getName();

        // 3. Fallback to UUID string
        return uuid.toString();
    }

    public void syncScoreToPlayer(ServerPlayer player) {
        updateScoreboard(player, getScore(player.getUUID()));
    }

    private void updateScoreboard(ServerPlayer player, float score) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(SCOREBOARD_OBJECTIVE);
        if (objective != null) {
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(Math.round(score));
        }
    }

    private void updateKillboard(ServerPlayer player, int kill) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(KILLBOARD_OBJECTIVE);
        if (objective != null) {
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(kill);
        }
    }

    private void updateDeathboard(ServerPlayer player, int death) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(DEATHBOARD_OBJECTIVE);
        if (objective != null) {
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(death);
        }
    }

    private void updateAssistboard(ServerPlayer player, int assist) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(ASSISTBOARD_OBJECTIVE);
        if (objective != null) {
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(assist);
        }
    }

    private void load() {
        try {
            if (Files.exists(configPath)) {
                JsonObject json = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
                if (json.has("combo_window")) comboWindowSeconds = json.get("combo_window").getAsDouble();
                if (json.has("assist_timeout")) assistTimeoutSeconds = json.get("assist_timeout").getAsInt();
                if (json.has("max_limit")) scoreMaxLimit = json.get("max_limit").getAsInt();
            if (json.has("scoreboard_display_name")) scoreboardDisplayName = json.get("scoreboard_display_name").getAsString();
            if (json.has("killboard_display_name")) killboardDisplayName = json.get("killboard_display_name").getAsString();
            if (json.has("deathboard_display_name")) deathboardDisplayName = json.get("deathboard_display_name").getAsString();
            if (json.has("assistboard_display_name")) assistboardDisplayName = json.get("assistboard_display_name").getAsString();
                if (json.has("disabled_bonuses")) {
                    JsonArray array = json.getAsJsonArray("disabled_bonuses");
                    disabledBonusTypes.clear();
                    for (JsonElement e : array) {
                        disabledBonusTypes.add(e.getAsInt());
                    }
                }
                if (json.has("bonus_expressions")) {
                    JsonObject exprObj = json.getAsJsonObject("bonus_expressions");
                    bonusExpressions.clear();
                    for (Map.Entry<String, JsonElement> entry : exprObj.entrySet()) {
                        try {
                            bonusExpressions.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            ServerLog.error("Failed to load server config: %s", e.getMessage());
        }
    }

    private void saveConfig() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("combo_window", comboWindowSeconds);
            json.addProperty("assist_timeout", assistTimeoutSeconds);
            json.addProperty("max_limit", scoreMaxLimit);
        json.addProperty("scoreboard_display_name", scoreboardDisplayName);
        json.addProperty("killboard_display_name", killboardDisplayName);
        json.addProperty("deathboard_display_name", deathboardDisplayName);
        json.addProperty("assistboard_display_name", assistboardDisplayName);

            JsonArray disabledArray = new JsonArray();
            disabledBonusTypes.forEach(disabledArray::add);
            json.add("disabled_bonuses", disabledArray);

            JsonObject exprObj = new JsonObject();
            bonusExpressions.forEach((k, v) -> exprObj.addProperty(k.toString(), v));
            json.add("bonus_expressions", exprObj);

            Files.writeString(configPath, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ServerLog.error("Failed to save server config: %s", e.getMessage());
        }
    }
}
