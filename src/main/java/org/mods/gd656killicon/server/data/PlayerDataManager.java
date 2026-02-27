package org.mods.gd656killicon.server.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.ScoreboardSyncPacket;
import org.mods.gd656killicon.server.util.ServerLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class PlayerDataManager {
    private static final PlayerDataManager INSTANCE = new PlayerDataManager();

    private static final String PLAYERDATA_DIR = "playerdata";
    private static final long AUTO_SAVE_INTERVAL_MINUTES = 5;
    private static final long CACHE_EXPIRE_TIME_MS = 3000; 

    private final Map<UUID, PlayerData> playerDataCache;
    private Path playerdataDir;
    private ScheduledExecutorService autoSaveExecutor;
    private boolean initialized = false;

    
    private List<ScoreboardSyncPacket.Entry> scoreboardSnapshot = null;
    private long lastSnapshotTime = 0;

    private PlayerDataManager() {
        this.playerDataCache = new ConcurrentHashMap<>();
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

        autoSaveExecutor.scheduleAtFixedRate(
                this::saveAllPlayerData,
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
            if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
                savePlayerData(uuid);
            } else {
                
                removePlayerData(uuid);
            }
        });
    }

    private void savePlayerData(UUID uuid) {
        PlayerData playerData = playerDataCache.get(uuid);
        if (playerData == null) {
            return;
        }

        Path file = getPlayerDataFile(uuid);
        try {
            Files.createDirectories(file.getParent());
            String json = playerData.toJson();
            Files.writeString(file, json, StandardCharsets.UTF_8);
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

    public PlayerData getOrCreatePlayerData(UUID uuid) {
        return getPlayerData(uuid);
    }

    public float getScore(UUID uuid) {
        PlayerData playerData = getPlayerData(uuid);
        return playerData.getScore();
    }

    public void setScore(UUID uuid, float score) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.setScore(score);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
    }

    public void addScore(UUID uuid, float amount) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.addScore(amount);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
    }

    public void reduceScore(UUID uuid, float amount) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.reduceScore(amount);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
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
        PlayerData playerData = getPlayerData(uuid);
        playerData.setKill(kill);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
    }

    public void addKill(UUID uuid, int amount) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.addKill(amount);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
    }

    public void reduceKill(UUID uuid, int amount) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.reduceKill(amount);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
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
        PlayerData playerData = getPlayerData(uuid);
        playerData.setDeath(death);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
    }

    public void addDeath(UUID uuid, int amount) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.addDeath(amount);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
    }

    public void reduceDeath(UUID uuid, int amount) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.reduceDeath(amount);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
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
        PlayerData playerData = getPlayerData(uuid);
        playerData.setAssist(assist);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
    }

    public void addAssist(UUID uuid, int amount) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.addAssist(amount);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
    }

    public void reduceAssist(UUID uuid, int amount) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.reduceAssist(amount);
        if (playerData.getScore() > 0 || playerData.getKill() > 0 || playerData.getDeath() > 0 || playerData.getAssist() > 0) {
            savePlayerData(uuid);
        } else {
            removePlayerData(uuid);
        }
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

    /**
     * 处理客户端发来的排行榜请求
     * 使用快照缓存优化高频请求
     */
    public void handleScoreboardRequest(ServerPlayer player) {
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (scoreboardSnapshot == null || now - lastSnapshotTime > CACHE_EXPIRE_TIME_MS) {
                updateScoreboardSnapshot(player.server);
            }
        }
        NetworkHandler.sendToPlayer(new ScoreboardSyncPacket(new ArrayList<>(scoreboardSnapshot)), player);
    }

    /**
     * 更新排行榜快照
     * 遍历缓存中的所有玩家数据，构建同步条目
     */
    private void updateScoreboardSnapshot(MinecraftServer server) {
        List<ScoreboardSyncPacket.Entry> newSnapshot = new ArrayList<>();
        playerDataCache.forEach((uuid, data) -> {
            
            
            String lastLoginName = data.getLastLoginName();
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
            
            if ((lastLoginName != null && !lastLoginName.isEmpty()) || onlinePlayer != null) {
                String name = ServerData.get().getScoreHolderName(server, uuid);
                newSnapshot.add(new ScoreboardSyncPacket.Entry(
                    uuid,
                    name,
                    (lastLoginName != null && !lastLoginName.isEmpty()) ? lastLoginName : (onlinePlayer != null ? onlinePlayer.getScoreboardName() : ""),
                    Math.round(data.getScore()),
                    data.getKill(),
                    data.getDeath(),
                    data.getAssist(),
                    onlinePlayer != null ? onlinePlayer.latency : -1 
                ));
            }
        });
        this.scoreboardSnapshot = newSnapshot;
        this.lastSnapshotTime = System.currentTimeMillis();
    }
}
