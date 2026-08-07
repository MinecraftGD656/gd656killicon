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
import org.mods.gd656killicon.common.bonus.BonusDefinition;
import org.mods.gd656killicon.common.bonus.BonusRegistry;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.logic.conquest.ConquestRuntimeStatsAdapter;
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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;

public class ServerData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ServerData INSTANCE = new ServerData();

    private static final double DEFAULT_COMBO_WINDOW_SECONDS = 5.0;
    private static final int DEFAULT_ASSIST_TIMEOUT_SECONDS = 180;
    private static final int DEFAULT_SCORE_MAX_LIMIT = Integer.MAX_VALUE;
    private static final String DEFAULT_SCOREBOARD_DISPLAY_NAME = "Player Score";
    private static final String DEFAULT_KILLBOARD_DISPLAY_NAME = "Player Kills";
    private static final String DEFAULT_DEATHBOARD_DISPLAY_NAME = "Player Deaths";
    private static final String DEFAULT_ASSISTBOARD_DISPLAY_NAME = "Player Assists";
    private static final String DEFAULT_REVIVEBOARD_DISPLAY_NAME = "Player Revives";

    private double comboWindowSeconds = DEFAULT_COMBO_WINDOW_SECONDS;
    private int assistTimeoutSeconds = DEFAULT_ASSIST_TIMEOUT_SECONDS;
    private int scoreMaxLimit = DEFAULT_SCORE_MAX_LIMIT;
    private String scoreboardDisplayName = DEFAULT_SCOREBOARD_DISPLAY_NAME;
    private String killboardDisplayName = DEFAULT_KILLBOARD_DISPLAY_NAME;
    private String deathboardDisplayName = DEFAULT_DEATHBOARD_DISPLAY_NAME;
    private String assistboardDisplayName = DEFAULT_ASSISTBOARD_DISPLAY_NAME;
    private String reviveboardDisplayName = DEFAULT_REVIVEBOARD_DISPLAY_NAME;
    private final Set<Integer> disabledBonusTypes = ConcurrentHashMap.newKeySet();
    private final Map<Integer, String> bonusExpressions = new ConcurrentHashMap<>();

    private Path configPath;
    private boolean loaded = false;

    private ServerData() {
        initDefaults();
    }

    private void initDefaults() {
        // 默认倍率与默认禁用集合均由 BonusRegistry 提供（唯一数据源）
        resetDisabledBonusTypes();
    }

    public static ServerData get() { return INSTANCE; }

    public void init(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT).resolve("gd656killicon");
        if (loaded && configPath != null && root.equals(configPath.getParent())) {
            return;
        }
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            ServerLog.error("Failed to create config directory: %s", e.getMessage());
        }
        this.configPath = root.resolve("server_config.json");
        applyDefaults();
        load();
        PlayerDataManager.get().init(server);
        initScoreboard(server);
        loaded = true;
    }

    public void shutdown() {
        if (!loaded) return;
        loaded = false;
        configPath = null;
        applyDefaults();
    }

    public void saveAll() {
        if (!loaded) return;
        saveConfig();
        PlayerDataManager.get().forceSave();
    }

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
        
        net.minecraft.server.MinecraftServer server = ServerBridge.loader().getCurrentServer();
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
        
        net.minecraft.server.MinecraftServer server = ServerBridge.loader().getCurrentServer();
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
        
        net.minecraft.server.MinecraftServer server = ServerBridge.loader().getCurrentServer();
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
        
        net.minecraft.server.MinecraftServer server = ServerBridge.loader().getCurrentServer();
        if (server != null) {
            net.minecraft.world.scores.Scoreboard scoreboard = server.getScoreboard();
            net.minecraft.world.scores.Objective objective = scoreboard.getObjective(ASSISTBOARD_OBJECTIVE);
            if (objective != null) {
                objective.setDisplayName(net.minecraft.network.chat.Component.literal(name));
            }
        }
    }

    public String getReviveboardDisplayName() { return reviveboardDisplayName; }

    public void setReviveboardDisplayName(String name) {
        this.reviveboardDisplayName = name;
        saveConfig();

        net.minecraft.server.MinecraftServer server = ServerBridge.loader().getCurrentServer();
        if (server != null) {
            net.minecraft.world.scores.Scoreboard scoreboard = server.getScoreboard();
            net.minecraft.world.scores.Objective objective = scoreboard.getObjective(REVIVEBOARD_OBJECTIVE);
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
        BonusDefinition def = BonusRegistry.get(type);
        if (def == null) {
            return "0";
        }
        return bonusExpressions.getOrDefault(type, def.scoreExpression());
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
        comboWindowSeconds = DEFAULT_COMBO_WINDOW_SECONDS;
        assistTimeoutSeconds = DEFAULT_ASSIST_TIMEOUT_SECONDS;
        scoreMaxLimit = DEFAULT_SCORE_MAX_LIMIT;
        scoreboardDisplayName = "鐜╁鍒嗘暟";
        killboardDisplayName = "玩家击杀";
        deathboardDisplayName = "鐜╁姝讳骸";
        assistboardDisplayName = "鐜╁鍔╂敾";
        reviveboardDisplayName = "玩家救援";
        disabledBonusTypes.clear();
        resetDisabledBonusTypes();
        bonusExpressions.clear();
        saveConfig();
    }
    
    public void resetBonusConfig() {
        disabledBonusTypes.clear();
        resetDisabledBonusTypes();
        bonusExpressions.clear();
        saveConfig();
    }

    private void applyDefaults() {
        comboWindowSeconds = DEFAULT_COMBO_WINDOW_SECONDS;
        assistTimeoutSeconds = DEFAULT_ASSIST_TIMEOUT_SECONDS;
        scoreMaxLimit = DEFAULT_SCORE_MAX_LIMIT;
        scoreboardDisplayName = DEFAULT_SCOREBOARD_DISPLAY_NAME;
        killboardDisplayName = DEFAULT_KILLBOARD_DISPLAY_NAME;
        deathboardDisplayName = DEFAULT_DEATHBOARD_DISPLAY_NAME;
        assistboardDisplayName = DEFAULT_ASSISTBOARD_DISPLAY_NAME;
        reviveboardDisplayName = DEFAULT_REVIVEBOARD_DISPLAY_NAME;
        disabledBonusTypes.clear();
        resetDisabledBonusTypes();
        bonusExpressions.clear();
    }

    /** 默认禁用集合由 BonusRegistry 驱动（disabledByDefault），唯一数据源。 */
    private void resetDisabledBonusTypes() {
        for (BonusDefinition def : BonusRegistry.getAll()) {
            if (def.disabledByDefault()) {
                disabledBonusTypes.add(def.type());
            }
        }
    }

    private boolean tryRouteConquestRuntimeStats(ServerPlayer player, float scoreDelta, int killDelta, int deathDelta, int assistDelta, int reviveDelta) {
        return ConquestRuntimeStatsAdapter.routeStatDelta(player, scoreDelta, killDelta, deathDelta, assistDelta, reviveDelta);
    }

    private boolean shouldBlockConquestGlobalStats(ServerPlayer player) {
        return ConquestRuntimeStatsAdapter.shouldBlockGlobalStatDelta(player);
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
        if (player == null || amount == 0) return;
        tryRouteConquestRuntimeStats(player, amount, 0, 0, 0, 0);
        if (shouldBlockConquestGlobalStats(player)) return;
        UUID uuid = player.getUUID();
        float current = getScore(uuid);
        double potential = (double) current + amount;
        float next = (float) Math.min(potential, (double) scoreMaxLimit);
        applyScoreValue(player, next);
    }

    public void setScore(ServerPlayer player, float amount) {
        float next = Math.max(0, Math.min(amount, scoreMaxLimit));
        applyScoreValue(player, next);
    }

    public void setAllScores(MinecraftServer server, float amount) {
        float next = Math.max(0, Math.min(amount, scoreMaxLimit));
        setAllIntentionallyTrackedStats(server, PlayerDataManager.get().getAllScores(), (uuid, value) -> PlayerDataManager.get().setScore(uuid, value), next);
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
        addIntStat(player, amount, 0.0F, amount, 0, 0, 0, this::getKill, (uuid, value) -> PlayerDataManager.get().setKill(uuid, value), this::updateKillboard);
    }

    public void setKill(ServerPlayer player, int amount) {
        setIntStat(player, amount, (uuid, value) -> PlayerDataManager.get().setKill(uuid, value), this::updateKillboard);
    }

    public void setAllKills(MinecraftServer server, int amount) {
        setAllIntentionallyTrackedStats(server, PlayerDataManager.get().getAllKills(), (uuid, value) -> PlayerDataManager.get().setKill(uuid, value), Math.max(0, amount));
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
        addIntStat(player, amount, 0.0F, 0, amount, 0, 0, this::getDeath, (uuid, value) -> PlayerDataManager.get().setDeath(uuid, value), this::updateDeathboard);
    }

    public void setDeath(ServerPlayer player, int amount) {
        setIntStat(player, amount, (uuid, value) -> PlayerDataManager.get().setDeath(uuid, value), this::updateDeathboard);
    }

    public void setAllDeaths(MinecraftServer server, int amount) {
        setAllIntentionallyTrackedStats(server, PlayerDataManager.get().getAllDeaths(), (uuid, value) -> PlayerDataManager.get().setDeath(uuid, value), Math.max(0, amount));
    }

    public int getAssist(UUID uuid) {
        return PlayerDataManager.get().getAssist(uuid);
    }

    public Map<UUID, Integer> getAllAssists() {
        return PlayerDataManager.get().getAllAssists();
    }

    public void addAssist(ServerPlayer player, int amount) {
        addIntStat(player, amount, 0.0F, 0, 0, amount, 0, this::getAssist, (uuid, value) -> PlayerDataManager.get().setAssist(uuid, value), this::updateAssistboard);
    }

    public int getRevive(UUID uuid) {
        return PlayerDataManager.get().getRevive(uuid);
    }

    public Map<UUID, Integer> getAllRevives() {
        return PlayerDataManager.get().getAllRevives();
    }

    public void addRevive(ServerPlayer player, int amount) {
        addIntStat(player, amount, 0.0F, 0, 0, 0, amount, this::getRevive, (uuid, value) -> PlayerDataManager.get().setRevive(uuid, value), this::updateReviveboard);
    }

    public void setRevive(ServerPlayer player, int amount) {
        setIntStat(player, amount, (uuid, value) -> PlayerDataManager.get().setRevive(uuid, value), this::updateReviveboard);
    }

    public void setAllRevives(MinecraftServer server, int amount) {
        setAllIntentionallyTrackedStats(server, PlayerDataManager.get().getAllRevives(), (uuid, value) -> PlayerDataManager.get().setRevive(uuid, value), Math.max(0, amount));
    }

    public void setAssist(ServerPlayer player, int amount) {
        setIntStat(player, amount, (uuid, value) -> PlayerDataManager.get().setAssist(uuid, value), this::updateAssistboard);
    }

    public void setAllAssists(MinecraftServer server, int amount) {
        setAllIntentionallyTrackedStats(server, PlayerDataManager.get().getAllAssists(), (uuid, value) -> PlayerDataManager.get().setAssist(uuid, value), Math.max(0, amount));
    }

    public void refreshScoreboard(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        
        Objective scoreObjective = scoreboard.getObjective(SCOREBOARD_OBJECTIVE);
        if (scoreObjective != null) {
            clearScoreboardScores(scoreboard, scoreObjective);
            
            PlayerDataManager.get().getAllScores().forEach((uuid, score) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, scoreObjective).setScore(Math.round(score));
            });
        }
        
        Objective killObjective = scoreboard.getObjective(KILLBOARD_OBJECTIVE);
        if (killObjective != null) {
            clearScoreboardScores(scoreboard, killObjective);
            
            PlayerDataManager.get().getAllKills().forEach((uuid, kill) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, killObjective).setScore(kill);
            });
        }
        
        Objective deathObjective = scoreboard.getObjective(DEATHBOARD_OBJECTIVE);
        if (deathObjective != null) {
            clearScoreboardScores(scoreboard, deathObjective);
            
            PlayerDataManager.get().getAllDeaths().forEach((uuid, death) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, deathObjective).setScore(death);
            });
        }
        
        Objective assistObjective = scoreboard.getObjective(ASSISTBOARD_OBJECTIVE);
        if (assistObjective != null) {
            clearScoreboardScores(scoreboard, assistObjective);
            
            PlayerDataManager.get().getAllAssists().forEach((uuid, assist) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, assistObjective).setScore(assist);
            });
        }

        Objective reviveObjective = scoreboard.getObjective(REVIVEBOARD_OBJECTIVE);
        if (reviveObjective != null) {
            clearScoreboardScores(scoreboard, reviveObjective);

            PlayerDataManager.get().getAllRevives().forEach((uuid, revive) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, reviveObjective).setScore(revive);
            });
        }
    }

    private void clearScoreboardScores(Scoreboard scoreboard, Objective objective) {
        scoreboard.getPlayerScores(objective).forEach(score -> {
            scoreboard.resetPlayerScore(score.getOwner(), objective);
        });
    }

    public static final String SCOREBOARD_OBJECTIVE = "gd656killicon.score";
    public static final String KILLBOARD_OBJECTIVE = "gd656killicon.kill";
    public static final String DEATHBOARD_OBJECTIVE = "gd656killicon.death";
    public static final String ASSISTBOARD_OBJECTIVE = "gd656killicon.assist";
    public static final String REVIVEBOARD_OBJECTIVE = "gd656killicon.revive";
    
    public void initScoreboard(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        
        Objective scoreObjective = scoreboard.getObjective(SCOREBOARD_OBJECTIVE);
        if (scoreObjective == null) {
            scoreObjective = scoreboard.addObjective(SCOREBOARD_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(scoreboardDisplayName), ObjectiveCriteria.RenderType.INTEGER);
        } else {
            scoreObjective.setDisplayName(Component.literal(scoreboardDisplayName));
        }

        final Objective finalScoreObj = scoreObjective;
        PlayerDataManager.get().getAllScores().forEach((uuid, score) -> {
            String scoreHolderName = getScoreHolderName(server, uuid);
            scoreboard.getOrCreatePlayerScore(scoreHolderName, finalScoreObj).setScore(Math.round(score));
        });
        
        Objective killObjective = scoreboard.getObjective(KILLBOARD_OBJECTIVE);
        if (killObjective == null) {
            killObjective = scoreboard.addObjective(KILLBOARD_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(killboardDisplayName), ObjectiveCriteria.RenderType.INTEGER);
        } else {
            killObjective.setDisplayName(Component.literal(killboardDisplayName));
        }

        final Objective finalKillObj = killObjective;
        PlayerDataManager.get().getAllKills().forEach((uuid, kill) -> {
            String scoreHolderName = getScoreHolderName(server, uuid);
            scoreboard.getOrCreatePlayerScore(scoreHolderName, finalKillObj).setScore(kill);
        });
        
        Objective deathObjective = scoreboard.getObjective(DEATHBOARD_OBJECTIVE);
        if (deathObjective == null) {
            deathObjective = scoreboard.addObjective(DEATHBOARD_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(deathboardDisplayName), ObjectiveCriteria.RenderType.INTEGER);
        } else {
            deathObjective.setDisplayName(Component.literal(deathboardDisplayName));
        }

        final Objective finalDeathObj = deathObjective;
        PlayerDataManager.get().getAllDeaths().forEach((uuid, death) -> {
            String scoreHolderName = getScoreHolderName(server, uuid);
            scoreboard.getOrCreatePlayerScore(scoreHolderName, finalDeathObj).setScore(death);
        });
        
        Objective assistObjective = scoreboard.getObjective(ASSISTBOARD_OBJECTIVE);
        if (assistObjective == null) {
            assistObjective = scoreboard.addObjective(ASSISTBOARD_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(assistboardDisplayName), ObjectiveCriteria.RenderType.INTEGER);
        } else {
            assistObjective.setDisplayName(Component.literal(assistboardDisplayName));
        }

        final Objective finalAssistObj = assistObjective;
        PlayerDataManager.get().getAllAssists().forEach((uuid, assist) -> {
            String scoreHolderName = getScoreHolderName(server, uuid);
            scoreboard.getOrCreatePlayerScore(scoreHolderName, finalAssistObj).setScore(assist);
        });

        if (ServerBridge.loader().isModLoaded("gd656conquest")) {
            Objective reviveObjective = scoreboard.getObjective(REVIVEBOARD_OBJECTIVE);
            if (reviveObjective == null) {
                reviveObjective = scoreboard.addObjective(REVIVEBOARD_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(reviveboardDisplayName), ObjectiveCriteria.RenderType.INTEGER);
            } else {
                reviveObjective.setDisplayName(Component.literal(reviveboardDisplayName));
            }

            final Objective finalReviveObj = reviveObjective;
            PlayerDataManager.get().getAllRevives().forEach((uuid, revive) -> {
                String scoreHolderName = getScoreHolderName(server, uuid);
                scoreboard.getOrCreatePlayerScore(scoreHolderName, finalReviveObj).setScore(revive);
            });
        }
    }

    public String getScoreHolderName(MinecraftServer server, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getScoreboardName();

        var profile = server.getProfileCache().get(uuid);
        if (profile.isPresent()) return profile.get().getName();

        return uuid.toString();
    }

    public void syncScoreToPlayer(ServerPlayer player) {
        updateScoreboard(player, getScore(player.getUUID()));
    }

    private void applyScoreValue(ServerPlayer player, float value) {
        if (player == null) {
            return;
        }
        PlayerDataManager.get().setScore(player.getUUID(), value);
        updateScoreboard(player, value);
    }

    private void addIntStat(
        ServerPlayer player,
        int amount,
        float scoreDelta,
        int killDelta,
        int deathDelta,
        int assistDelta,
        int reviveDelta,
        Function<UUID, Integer> getter,
        BiConsumer<UUID, Integer> setter,
        ObjIntConsumer<ServerPlayer> scoreboardUpdater
    ) {
        if (player == null || amount == 0) {
            return;
        }
        tryRouteConquestRuntimeStats(player, scoreDelta, killDelta, deathDelta, assistDelta, reviveDelta);
        if (shouldBlockConquestGlobalStats(player)) {
            return;
        }
        UUID uuid = player.getUUID();
        int current = getter.apply(uuid);
        long potential = (long) current + amount;
        int next = (int) Math.min(potential, (long) Integer.MAX_VALUE);
        setter.accept(uuid, next);
        scoreboardUpdater.accept(player, next);
    }

    private void setIntStat(
        ServerPlayer player,
        int amount,
        BiConsumer<UUID, Integer> setter,
        ObjIntConsumer<ServerPlayer> scoreboardUpdater
    ) {
        if (player == null) {
            return;
        }
        int next = Math.max(0, amount);
        setter.accept(player.getUUID(), next);
        scoreboardUpdater.accept(player, next);
    }

    private <T> void setAllIntentionallyTrackedStats(
        MinecraftServer server,
        Map<UUID, T> currentValues,
        BiConsumer<UUID, T> setter,
        T nextValue
    ) {
        currentValues.keySet().forEach(uuid -> setter.accept(uuid, nextValue));
        refreshScoreboard(server);
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

    private void updateReviveboard(ServerPlayer player, int revive) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(REVIVEBOARD_OBJECTIVE);
        if (objective != null) {
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(revive);
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
            if (json.has("reviveboard_display_name")) reviveboardDisplayName = json.get("reviveboard_display_name").getAsString();
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
        json.addProperty("reviveboard_display_name", reviveboardDisplayName);

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
