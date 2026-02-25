package org.mods.gd656killicon.client.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端统计数据的持久化管理器。
 *
 * <p>负责将 {@link ClientStatsData} 以 JSON 形式保存到本地文件，并在启动时加载。</p>
 *
 * <p>注意：为了避免影响游戏流程，本类对 I/O 异常采用“静默失败”的策略：
 * 读取失败时返回空数据对象，写入失败时直接忽略异常。</p>
 */
public class StatsPersistenceManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String STATS_FILE_NAME = "client_stats.json";

    /**
     * 获取统计文件路径，并确保父目录存在。
     *
     * @return 统计文件路径
     */
    private static Path getStatsFilePath() {
        Path gameDirectory = Minecraft.getInstance().gameDirectory.toPath();
        Path dataDirectory = gameDirectory.resolve("data").resolve("gd656killicon");

        try {
            Files.createDirectories(dataDirectory);
        } catch (Exception ignored) {
        }

        return dataDirectory.resolve(STATS_FILE_NAME);
    }

    /**
     * 保存统计数据到本地文件。
     *
     * @param data 要保存的数据；为 null 时不执行写入
     */
    public static void saveStats(ClientStatsData data) {
        if (data == null) {
            return;
        }
        Path statsFile = getStatsFilePath();

        try (Writer writer = Files.newBufferedWriter(statsFile, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (Exception ignored) {
        }
    }

    /**
     * 从本地文件加载统计数据。
     *
     * <p>当文件不存在或读取失败时，返回一个新的空数据对象。</p>
     *
     * @return 已加载的统计数据
     */
    public static ClientStatsData loadStats() {
        Path statsFile = getStatsFilePath();

        if (!Files.exists(statsFile)) {
            return new ClientStatsData();
        }

        try (Reader reader = Files.newBufferedReader(statsFile, StandardCharsets.UTF_8)) {
            ClientStatsData data = GSON.fromJson(reader, ClientStatsData.class);
            return data != null ? data : new ClientStatsData();
        } catch (Exception ignored) {
            return new ClientStatsData();
        }
    }

    /**
     * 客户端统计数据模型。
     *
     * <p>字段均为可序列化的简单类型与 Map，便于 JSON 持久化。</p>
     */
    public static class ClientStatsData {
        /** 总击杀数 */
        public long totalKills = 0;
        /** 总死亡数 */
        public long totalDeaths = 0;
        /** 总助攻数 */
        public long totalAssists = 0;
        /** 最大击杀距离 */
        public double maxKillDistance = 0.0;
        /** 最大连杀数 */
        public long maxKillStreak = 0;
        /** 总造成伤害 */
        public double totalDamageDealt = 0.0;
        /** 按生物展示名统计击杀次数 */
        public Map<String, Long> mobKillCounts = new ConcurrentHashMap<>();
        /** 按玩家名统计击杀次数 */
        public Map<String, Long> playerKillCounts = new ConcurrentHashMap<>();
        /** 按武器/载具展示名统计使用次数 */
        public Map<String, Long> weaponUseCounts = new ConcurrentHashMap<>();
        /** 按玩家名统计“击杀你”的次数 */
        public Map<String, Long> deathByPlayerCounts = new ConcurrentHashMap<>();
    }
}
