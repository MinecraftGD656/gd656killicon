package org.mods.gd656killicon.server.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.ScoreboardSyncPacket;
import org.mods.gd656killicon.server.logic.conquest.ConquestScoreboardAdapter;
import org.mods.gd656killicon.server.util.ServerLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PlayerDataManager {
    private static final PlayerDataManager INSTANCE = new PlayerDataManager();

    private static final String PLAYERDATA_DIR = "playerdata";
    private static final long AUTO_SAVE_INTERVAL_MINUTES = 5;
    private static final int SCOREBOARD_PAGE_LIMIT_MAX = 100;
    private static final String[] DEFAULT_PANEL_TEAMS = new String[]{"", "", "", ""};
    private final Map<UUID, PlayerData> playerDataCache;
    /** 每 honor 全服最高累计次数(内存缓存, 不落盘; 启动时从所有玩家 playerdata 构建, recordHonor 时更新)。 */
    private final Map<String, Integer> globalBestByHonor = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers;
    private final Set<UUID> pendingRemovalPlayers;
    private Path playerdataDir;
    private ScheduledExecutorService autoSaveExecutor;
    private boolean initialized = false;

    private PlayerDataManager() {
        this.playerDataCache = new ConcurrentHashMap<>();
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();
        this.pendingRemovalPlayers = ConcurrentHashMap.newKeySet();
    }

    public static PlayerDataManager get() {
        return INSTANCE;
    }

    public void init(MinecraftServer server) {
        if (initialized) {
            return;
        }

        Path root = server.getWorldPath(LevelResource.ROOT).resolve("gd656killicon");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            ServerLog.error("Failed to create player data root directory: %s", e.getMessage());
        }

        this.playerdataDir = root.resolve(PLAYERDATA_DIR);
        try {
            Files.createDirectories(playerdataDir);
        } catch (IOException e) {
            ServerLog.error("Failed to create player data directory: %s", e.getMessage());
        }

        loadAllPlayerData();
        rebuildGlobalBestByHonor();
        startAutoSaveTask();
        initialized = true;
    }

    public void shutdown() {
        if (autoSaveExecutor != null) {
            autoSaveExecutor.shutdown();
            try {
                if (!autoSaveExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    autoSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                autoSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        saveAllPlayerData();
        playerDataCache.clear();
        dirtyPlayers.clear();
        pendingRemovalPlayers.clear();
        initialized = false;
    }

    private void startAutoSaveTask() {
        if (autoSaveExecutor != null) {
            autoSaveExecutor.shutdownNow();
        }

        autoSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "PlayerData-AutoSave");
            thread.setDaemon(true);
            return thread;
        });

        autoSaveExecutor.scheduleAtFixedRate(() -> {
                try {
                    flushDirtyPlayerData();
                } catch (Exception e) {
                    ServerLog.error("Failed to flush dirty player data: %s", e.getMessage());
                }
            },
            AUTO_SAVE_INTERVAL_MINUTES,
            AUTO_SAVE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
    }

    private void loadAllPlayerData() {
        if (!Files.exists(playerdataDir)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(playerdataDir, 1)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(this::loadPlayerDataFromFile);
        } catch (IOException e) {
            ServerLog.error("Failed to load player data: %s", e.getMessage());
        }
    }

    private void loadPlayerDataFromFile(Path file) {
        try {
            String fileName = file.getFileName().toString();
            String uuidStr = fileName.substring(0, fileName.length() - 5);

            UUID uuid = UUID.fromString(uuidStr);
            String json = Files.readString(file, StandardCharsets.UTF_8);
            PlayerData playerData = PlayerData.fromJson(json, uuid);

            playerDataCache.put(uuid, playerData);
        } catch (Exception e) {
            ServerLog.error("Failed to load player data file %s: %s", file.getFileName().toString(), e.getMessage());
        }
    }

    private void saveAllPlayerData() {
        playerDataCache.forEach((uuid, playerData) -> {
            if (hasTrackedStats(playerData)) {
                savePlayerData(uuid);
            } else {
                removePlayerData(uuid);
            }
        });
    }

    private void flushDirtyPlayerData() {
        if (dirtyPlayers.isEmpty() && pendingRemovalPlayers.isEmpty()) {
            return;
        }

        Set<UUID> dirtySnapshot = new HashSet<>(dirtyPlayers);
        for (UUID uuid : dirtySnapshot) {
            savePlayerData(uuid);
        }

        Set<UUID> removalSnapshot = new HashSet<>(pendingRemovalPlayers);
        for (UUID uuid : removalSnapshot) {
            removePlayerData(uuid);
        }
    }

    private void savePlayerData(UUID uuid) {
        PlayerData playerData = playerDataCache.get(uuid);
        if (playerData == null) {
            dirtyPlayers.remove(uuid);
            pendingRemovalPlayers.remove(uuid);
            return;
        }

        Path file = getPlayerDataFile(uuid);
        try {
            Files.createDirectories(file.getParent());
            String json = playerData.toJson();
            Files.writeString(file, json, StandardCharsets.UTF_8);
            dirtyPlayers.remove(uuid);
            pendingRemovalPlayers.remove(uuid);
        } catch (IOException e) {
            ServerLog.error("Failed to save player data for %s", uuid.toString());
        }
    }

    private Path getPlayerDataFile(UUID uuid) {
        return playerdataDir.resolve(uuid.toString() + ".json");
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, k -> new PlayerData(k));
    }

    /**
     * 记录一次 honor 获得(仅无 Conquest 分支使用): 累计到该玩家 PlayerData, 并更新全服最高缓存。
     * @return 该玩家该 honor 新的累计次数(未记录返回 0)。
     */
    public int recordHonor(UUID playerId, String honorId) {
        if (playerId == null || honorId == null || honorId.isBlank()) {
            return 0;
        }
        final int[] newCount = {0};
        mutateTrackedStats(playerId, pd -> newCount[0] = pd.addHonor(honorId));
        if (newCount[0] > 0) {
            globalBestByHonor.merge(honorId, newCount[0], Math::max);
        }
        return newCount[0];
    }

    /** 某 honor 全服最高累计次数(内存缓存, 启动时从所有玩家 playerdata 构建; 未记录过返回 0)。 */
    public int getGlobalBest(String honorId) {
        if (honorId == null) {
            return 0;
        }
        Integer best = globalBestByHonor.get(honorId);
        return best != null ? best : 0;
    }

    /** 全部 honor 全服最高累计次数(快照, 指令查询用)。 */
    public Map<String, Integer> getAllGlobalBest() {
        return new java.util.HashMap<>(globalBestByHonor);
    }

    /** 设置某 honor 全服最高累计次数(<=0 视为清零删除), 指令用。 */
    public void setGlobalBest(String honorId, int value) {
        if (honorId == null || honorId.isBlank()) {
            return;
        }
        if (value <= 0) {
            globalBestByHonor.remove(honorId);
        } else {
            globalBestByHonor.put(honorId, value);
        }
    }

    /** 对某 honor 全服最高累计次数加值(可为负), 指令用。 */
    public void addGlobalBest(String honorId, int amount) {
        if (honorId == null || honorId.isBlank() || amount == 0) {
            return;
        }
        int result = globalBestByHonor.merge(honorId, amount, Integer::sum);
        if (result <= 0) {
            globalBestByHonor.remove(honorId);
        }
    }

    /** 某玩家某 honor 获取数量。 */
    public int getHonorCount(UUID playerId, String honorId) {
        if (playerId == null || honorId == null || honorId.isBlank()) {
            return 0;
        }
        return getPlayerData(playerId).getHonorCount(honorId);
    }

    /** 设置某玩家某 honor 获取数量(<=0 清零), 并刷新该 honor 的全服最高缓存。 */
    public void setHonorCount(UUID playerId, String honorId, int value) {
        if (playerId == null || honorId == null || honorId.isBlank()) {
            return;
        }
        mutateTrackedStats(playerId, pd -> pd.setHonorCount(honorId, value));
        refreshGlobalBest(honorId);
    }

    /** 对某玩家某 honor 获取数量加值(可为负), 并刷新该 honor 的全服最高缓存。 */
    public void addHonorCount(UUID playerId, String honorId, int amount) {
        if (playerId == null || honorId == null || honorId.isBlank() || amount == 0) {
            return;
        }
        mutateTrackedStats(playerId, pd -> pd.addHonorCount(honorId, amount));
        refreshGlobalBest(honorId);
    }

    /** 重算某 honor 全服最高缓存(遍历所有玩家取 max), 玩家计数被指令修改后调用。 */
    private void refreshGlobalBest(String honorId) {
        if (honorId == null) {
            return;
        }
        int best = 0;
        for (PlayerData playerData : playerDataCache.values()) {
            if (playerData != null) {
                best = Math.max(best, playerData.getHonorCount(honorId));
            }
        }
        if (best <= 0) {
            globalBestByHonor.remove(honorId);
        } else {
            globalBestByHonor.put(honorId, best);
        }
    }

    /** 从内存 playerDataCache 重建全服最高缓存(服务器启动时调用, 不落盘)。 */
    private void rebuildGlobalBestByHonor() {
        globalBestByHonor.clear();
        for (PlayerData playerData : playerDataCache.values()) {
            if (playerData == null) {
                continue;
            }
            for (Map.Entry<String, Integer> entry : playerData.getAllHonorCounts().entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    globalBestByHonor.merge(entry.getKey(), entry.getValue(), Math::max);
                }
            }
        }
    }

    public PlayerData getOrCreatePlayerData(UUID uuid) {
        return getPlayerData(uuid);
    }

    public float getScore(UUID uuid) {
        PlayerData playerData = getPlayerData(uuid);
        return playerData.getScore();
    }

    public void setScore(UUID uuid, float score) {
        mutateTrackedStats(uuid, playerData -> playerData.setScore(score));
    }

    public void addScore(UUID uuid, float amount) {
        mutateTrackedStats(uuid, playerData -> playerData.addScore(amount));
    }

    public void reduceScore(UUID uuid, float amount) {
        mutateTrackedStats(uuid, playerData -> playerData.reduceScore(amount));
    }

    public int getKill(UUID uuid) {
        PlayerData playerData = getPlayerData(uuid);
        return playerData.getKill();
    }

    public Map<UUID, Integer> getAllKills() {
        Map<UUID, Integer> kills = new java.util.concurrent.ConcurrentHashMap<>();
        playerDataCache.forEach((uuid, data) -> {
            int kill = data.getKill();
            if (kill > 0) {
                kills.put(uuid, kill);
            }
        });
        return kills;
    }

    public void setKill(UUID uuid, int kill) {
        mutateTrackedStats(uuid, playerData -> playerData.setKill(kill));
    }

    public void addKill(UUID uuid, int amount) {
        mutateTrackedStats(uuid, playerData -> playerData.addKill(amount));
    }

    public void reduceKill(UUID uuid, int amount) {
        mutateTrackedStats(uuid, playerData -> playerData.reduceKill(amount));
    }

    public int getDeath(UUID uuid) {
        PlayerData playerData = getPlayerData(uuid);
        return playerData.getDeath();
    }

    public Map<UUID, Integer> getAllDeaths() {
        Map<UUID, Integer> deaths = new java.util.concurrent.ConcurrentHashMap<>();
        playerDataCache.forEach((uuid, data) -> {
            int death = data.getDeath();
            if (death > 0) {
                deaths.put(uuid, death);
            }
        });
        return deaths;
    }

    public void setDeath(UUID uuid, int death) {
        mutateTrackedStats(uuid, playerData -> playerData.setDeath(death));
    }

    public void addDeath(UUID uuid, int amount) {
        mutateTrackedStats(uuid, playerData -> playerData.addDeath(amount));
    }

    public void reduceDeath(UUID uuid, int amount) {
        mutateTrackedStats(uuid, playerData -> playerData.reduceDeath(amount));
    }

    public int getAssist(UUID uuid) {
        PlayerData playerData = getPlayerData(uuid);
        return playerData.getAssist();
    }

    public Map<UUID, Integer> getAllAssists() {
        Map<UUID, Integer> assists = new java.util.concurrent.ConcurrentHashMap<>();
        playerDataCache.forEach((uuid, data) -> {
            int assist = data.getAssist();
            if (assist > 0) {
                assists.put(uuid, assist);
            }
        });
        return assists;
    }

    public void setAssist(UUID uuid, int assist) {
        mutateTrackedStats(uuid, playerData -> playerData.setAssist(assist));
    }

    public void addAssist(UUID uuid, int amount) {
        mutateTrackedStats(uuid, playerData -> playerData.addAssist(amount));
    }

    public void reduceAssist(UUID uuid, int amount) {
        mutateTrackedStats(uuid, playerData -> playerData.reduceAssist(amount));
    }

    public int getRevive(UUID uuid) {
        PlayerData playerData = getPlayerData(uuid);
        return playerData.getRevive();
    }

    public Map<UUID, Integer> getAllRevives() {
        Map<UUID, Integer> revives = new java.util.concurrent.ConcurrentHashMap<>();
        playerDataCache.forEach((uuid, data) -> {
            int revive = data.getRevive();
            if (revive > 0) {
                revives.put(uuid, revive);
            }
        });
        return revives;
    }

    public void setRevive(UUID uuid, int revive) {
        mutateTrackedStats(uuid, playerData -> playerData.setRevive(revive));
    }

    public void addRevive(UUID uuid, int amount) {
        mutateTrackedStats(uuid, playerData -> playerData.addRevive(amount));
    }

    public void reduceRevive(UUID uuid, int amount) {
        mutateTrackedStats(uuid, playerData -> playerData.reduceRevive(amount));
    }

    public void updateLastLoginName(UUID uuid, String name) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.setLastLoginName(name);
        savePlayerData(uuid);
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        return Map.copyOf(playerDataCache);
    }

    public Map<UUID, Float> getAllScores() {
        Map<UUID, Float> scores = new ConcurrentHashMap<>();
        playerDataCache.forEach((uuid, data) -> {
            if (data.getScore() > 0) {
                scores.put(uuid, data.getScore());
            }
        });
        return scores;
    }

    public boolean hasPlayerData(UUID uuid) {
        return playerDataCache.containsKey(uuid);
    }

    public void removePlayerData(UUID uuid) {
        dirtyPlayers.remove(uuid);
        pendingRemovalPlayers.remove(uuid);
        playerDataCache.remove(uuid);
        Path file = getPlayerDataFile(uuid);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            ServerLog.error("Failed to delete player data file for %s", uuid.toString());
        }
    }

    public void clearAllPlayerData() {
        playerDataCache.clear();
        dirtyPlayers.clear();
        pendingRemovalPlayers.clear();
        if (Files.exists(playerdataDir)) {
            try (Stream<Path> paths = Files.walk(playerdataDir, 1)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                ServerLog.error("Failed to delete player data file %s", path.getFileName().toString());
                            }
                        });
            } catch (IOException e) {
                ServerLog.error("Failed to clear player data: %s", e.getMessage());
            }
        }
    }

    public int getPlayerCount() {
        return playerDataCache.size();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void forceSave() {
        saveAllPlayerData();
    }

    public void forceSave(UUID uuid) {
        savePlayerData(uuid);
    }

    private void mutateTrackedStats(UUID uuid, Consumer<PlayerData> mutation) {
        PlayerData playerData = getPlayerData(uuid);
        mutation.accept(playerData);
        if (hasTrackedStats(playerData)) {
            dirtyPlayers.add(uuid);
            pendingRemovalPlayers.remove(uuid);
        } else {
            dirtyPlayers.remove(uuid);
            pendingRemovalPlayers.add(uuid);
        }
    }

    /**
     * 处理客户端发来的排行榜请求
     * 使用快照缓存优化高频请求
     */
    public void handleScoreboardRequest(ServerPlayer player, int offset, int limit, long requestId) {
        if (player == null || player.server == null) {
            return;
        }
        ResolvedScoreboardData resolved = resolveScoreboardData(player);
        ScoreboardPage page = buildScoreboardPage(resolved.entries(), offset, limit);
        NetworkHandler.sendToPlayer(new ScoreboardSyncPacket(
            page.entries(),
            page.offset(),
            resolved.entries().size(),
            requestId,
            resolved.columns(),
            resolved.panelTeams()
        ), player);
    }

    private ResolvedScoreboardData resolveScoreboardData(ServerPlayer requester) {
        ConquestScoreboardAdapter.Result conquestResult = ConquestScoreboardAdapter.resolve(requester);
        if (conquestResult != null) {
            return new ResolvedScoreboardData(
                buildPrioritizedEntries(requester, new ArrayList<>(conquestResult.entries())),
                conquestResult.columns(),
                conquestResult.panelTeams()
            );
        }
        return new ResolvedScoreboardData(
            buildPrioritizedEntries(requester, buildScoreboardEntries(requester.server)),
            1,
            DEFAULT_PANEL_TEAMS
        );
    }

    private ScoreboardPage buildScoreboardPage(List<ScoreboardSyncPacket.Entry> allEntries, int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(SCOREBOARD_PAGE_LIMIT_MAX, limit));
        int fromIndex = Math.min(safeOffset, allEntries.size());
        int toIndex = Math.min(fromIndex + safeLimit, allEntries.size());
        return new ScoreboardPage(new ArrayList<>(allEntries.subList(fromIndex, toIndex)), safeOffset);
    }

    private List<ScoreboardSyncPacket.Entry> buildPrioritizedEntries(ServerPlayer requester, List<ScoreboardSyncPacket.Entry> allEntries) {
        if (requester == null || allEntries.isEmpty()) {
            return allEntries;
        }
        List<ScoreboardSyncPacket.Entry> prioritized = new ArrayList<>(allEntries.size());
        java.util.Set<UUID> added = new java.util.HashSet<>();
        ScoreboardSyncPacket.Entry self = null;
        for (ScoreboardSyncPacket.Entry entry : allEntries) {
            if (entry.uuid.equals(requester.getUUID())) {
                self = entry;
                break;
            }
        }
        if (self != null) {
            prioritized.add(self);
            added.add(self.uuid);
        }
        String requesterTeam = requester.getTeam() == null ? "" : requester.getTeam().getName();
        if (!requesterTeam.isEmpty()) {
            for (ScoreboardSyncPacket.Entry entry : allEntries) {
                if (!added.contains(entry.uuid) && requesterTeam.equals(entry.teamName)) {
                    prioritized.add(entry);
                    added.add(entry.uuid);
                }
            }
        }
        for (ScoreboardSyncPacket.Entry entry : allEntries) {
            if (!added.contains(entry.uuid) && entry.online) {
                prioritized.add(entry);
                added.add(entry.uuid);
            }
        }
        for (ScoreboardSyncPacket.Entry entry : allEntries) {
            if (!added.contains(entry.uuid)) {
                prioritized.add(entry);
            }
        }
        return prioritized;
    }

    /**
     * 更新排行榜快照
     * 遍历缓存中的所有玩家数据，构建同步条目
     */
    private List<ScoreboardSyncPacket.Entry> buildScoreboardEntries(MinecraftServer server) {
        List<ScoreboardSyncPacket.Entry> entries = new ArrayList<>();
        playerDataCache.forEach((uuid, data) -> {
            String lastLoginName = data.getLastLoginName();
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
            
            if ((lastLoginName != null && !lastLoginName.isEmpty()) || onlinePlayer != null) {
                String name = ServerData.get().getScoreHolderName(server, uuid);
                boolean isOnline = onlinePlayer != null;
                entries.add(new ScoreboardSyncPacket.Entry(
                    uuid,
                    name,
                    (lastLoginName != null && !lastLoginName.isEmpty()) ? lastLoginName : (isOnline ? onlinePlayer.getScoreboardName() : ""),
                    resolveTeamName(server, onlinePlayer, name),
                    "",
                    Math.round(data.getScore()),
                    data.getKill(),
                    data.getDeath(),
                    data.getAssist(),
                    data.getRevive(),
                    isOnline ? onlinePlayer.connection.latency() : -1,
                    isOnline,
                    isOnline && onlinePlayer.isSpectator()                 ));
            }
        });
        entries.sort((a, b) -> {
            int scoreCompare = Integer.compare(b.score, a.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return a.uuid.compareTo(b.uuid);
        });
        return entries;
    }

    private String resolveTeamName(MinecraftServer server, ServerPlayer onlinePlayer, String scoreHolderName) {
        if (onlinePlayer != null && onlinePlayer.getTeam() != null) {
            return onlinePlayer.getTeam().getName();
        }
        if (server != null && scoreHolderName != null && !scoreHolderName.isEmpty()) {
            var team = server.getScoreboard().getPlayersTeam(scoreHolderName);
            if (team != null) {
                return team.getName();
            }
        }
        return "";
    }

    private boolean hasTrackedStats(PlayerData playerData) {
        return playerData.getScore() > 0
            || playerData.getKill() > 0
            || playerData.getDeath() > 0
            || playerData.getAssist() > 0
            || playerData.getRevive() > 0
            || playerData.hasAnyHonor();
    }

    private record ResolvedScoreboardData(List<ScoreboardSyncPacket.Entry> entries, int columns, String[] panelTeams) {
        private ResolvedScoreboardData {
            entries = entries == null ? List.of() : entries;
            panelTeams = panelTeams == null ? DEFAULT_PANEL_TEAMS : panelTeams;
        }
    }

    private record ScoreboardPage(List<ScoreboardSyncPacket.Entry> entries, int offset) {
    }
}
