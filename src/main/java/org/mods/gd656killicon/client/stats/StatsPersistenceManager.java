package org.mods.gd656killicon.client.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatsPersistenceManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String STATS_FILE_NAME = "client_stats.json";
    
    private static Path getStatsFilePath() {
        Path gameDirectory = Minecraft.getInstance().gameDirectory.toPath();
        Path dataDirectory = gameDirectory.resolve("data").resolve("gd656killicon");
        
        // Create directories if they don't exist
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            // 静默处理目录创建失败
        }
        
        return dataDirectory.resolve(STATS_FILE_NAME);
    }
    
    public static void saveStats(ClientStatsData data) {
        Path statsFile = getStatsFilePath();
        
        try (Writer writer = Files.newBufferedWriter(statsFile)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            // 静默处理保存失败
        }
    }
    
    public static ClientStatsData loadStats() {
        Path statsFile = getStatsFilePath();
        
        if (!Files.exists(statsFile)) {
            return new ClientStatsData();
        }
        
        try (Reader reader = Files.newBufferedReader(statsFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return GSON.fromJson(json, ClientStatsData.class);
        } catch (IOException e) {
            return new ClientStatsData();
        }
    }
    
    public static class ClientStatsData {
        public long totalKills = 0;
        public long totalDeaths = 0;
        public long totalAssists = 0;
        public double maxKillDistance = 0.0;
        public long maxKillStreak = 0;
        public double totalDamageDealt = 0.0;
        public Map<String, Long> mobKillCounts = new ConcurrentHashMap<>();
        public Map<String, Long> playerKillCounts = new ConcurrentHashMap<>();
        public Map<String, Long> weaponUseCounts = new ConcurrentHashMap<>();
        public Map<String, Long> deathByPlayerCounts = new ConcurrentHashMap<>();
    }
}
