package org.mods.gd656killicon.client.cache;

import dev.architectury.platform.Platform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import org.mods.gd656killicon.network.packet.ScoreboardSyncPacket;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端本地排行榜缓存，以服务器/存档为单位持久化。
 *
 * <p>每次打开榜单时优先读取已有本地数据；收到服务端数据后，
 * 按 UUID 合并更新并写回本地缓存。</p>
 */
public final class LocalLeaderboardCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CACHE_DIR_NAME = "leaderboard_cache";

    private static String currentServerKey;
    private static final Map<UUID, ScoreboardSyncPacket.Entry> entryMap = new LinkedHashMap<>();

    private LocalLeaderboardCache() {
    }

    /**
     * 获取缓存目录路径。
     */
    private static Path getCacheDir() {
        Path dir = Platform.getGameFolder().resolve("data").resolve("gd656killicon").resolve(CACHE_DIR_NAME);
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
        }
        return dir;
    }

    /**
     * 推断当前服务器/存档的唯一标识。
     *
     * <p>多人游戏：使用服务器地址（ip）；单人游戏：使用存档名。</p>
     */
    public static String resolveServerKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return "unknown";
        }
        // 多人服务器
        if (mc.getCurrentServer() != null) {
            String ip = mc.getCurrentServer().ip;
            return sanitizeKey(ip != null && !ip.isEmpty() ? ip : "multiplayer");
        }
        // 单人存档
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            String saveName = mc.getSingleplayerServer().getWorldData().getLevelName();
            return sanitizeKey(saveName != null && !saveName.isEmpty() ? saveName : "singleplayer");
        }
        // 回退：主菜单等场景
        return "unknown";
    }

    /**
     * 将 key 中的非法文件名字符替换为下划线。
     */
    private static String sanitizeKey(String raw) {
        return raw.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 获取当前服务器对应的缓存文件路径。
     */
    private static Path getCacheFile(String serverKey) {
        return getCacheDir().resolve(serverKey + ".json");
    }

    /**
     * 加载指定服务器的本地缓存到内存。
     *
     * @param serverKey 服务器标识
     * @return 缓存的条目列表（可能为空）
     */
    public static List<ScoreboardSyncPacket.Entry> load(String serverKey) {
        currentServerKey = serverKey;
        entryMap.clear();
        Path file = getCacheFile(serverKey);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            CacheData data = GSON.fromJson(reader, CacheData.class);
            if (data != null && data.entries != null) {
                for (CachedEntry ce : data.entries) {
                    ScoreboardSyncPacket.Entry entry = ce.toEntry();
                    entryMap.put(entry.uuid, entry);
                }
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(entryMap.values());
    }

    /**
     * 用服务端传来的新数据合并更新本地缓存。
     *
     * <p>规则：以 UUID 为键，新数据覆盖已有条目；本地独有条目保留。
     * 合并后自动写回本地文件。</p>
     *
     * @param newEntries 服务端传来的条目列表
     */
    public static void mergeAndSave(List<ScoreboardSyncPacket.Entry> newEntries) {
        if (currentServerKey == null) {
            return;
        }
        for (ScoreboardSyncPacket.Entry entry : newEntries) {
            entryMap.put(entry.uuid, entry);
        }
        save(currentServerKey);
    }

    /**
     * 将当前内存中的缓存写入指定服务器的文件。
     */
    private static void save(String serverKey) {
        Path file = getCacheFile(serverKey);
        CacheData data = new CacheData();
        data.entries = new ArrayList<>();
        for (ScoreboardSyncPacket.Entry entry : entryMap.values()) {
            data.entries.add(CachedEntry.fromEntry(entry));
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (Exception ignored) {
        }
    }

    /**
     * 清除当前内存缓存并重置 key。
     */
    public static void clear() {
        currentServerKey = null;
        entryMap.clear();
    }

    /**
     * JSON 序列化用的缓存数据结构。
     */
    private static class CacheData {
        @SerializedName("entries")
        List<CachedEntry> entries;
    }

    /**
     * 可序列化的单条条目。
     */
    private static class CachedEntry {
        @SerializedName("uuid")
        String uuid;
        @SerializedName("name")
        String name;
        @SerializedName("lastLoginName")
        String lastLoginName;
        @SerializedName("teamName")
        String teamName;
        @SerializedName("squadLabel")
        String squadLabel;
        @SerializedName("score")
        int score;
        @SerializedName("kill")
        int kill;
        @SerializedName("death")
        int death;
        @SerializedName("assist")
        int assist;
        @SerializedName("revive")
        int revive;
        @SerializedName("ping")
        int ping;
        @SerializedName("online")
        boolean online;
        @SerializedName("spectator")
        boolean spectator;

        static CachedEntry fromEntry(ScoreboardSyncPacket.Entry entry) {
            CachedEntry ce = new CachedEntry();
            ce.uuid = entry.uuid.toString();
            ce.name = entry.name;
            ce.lastLoginName = entry.lastLoginName;
            ce.teamName = entry.teamName;
            ce.squadLabel = entry.squadLabel;
            ce.score = entry.score;
            ce.kill = entry.kill;
            ce.death = entry.death;
            ce.assist = entry.assist;
            ce.revive = entry.revive;
            ce.ping = entry.ping;
            ce.online = entry.online;
            ce.spectator = entry.spectator;
            return ce;
        }

        ScoreboardSyncPacket.Entry toEntry() {
            UUID id;
            try {
                id = UUID.fromString(uuid);
            } catch (Exception e) {
                return null;
            }
            // 从缓存加载时无法确定在线状态，统一置为离线
            return new ScoreboardSyncPacket.Entry(
                id, name, lastLoginName, teamName, squadLabel,
                score, kill, death, assist, revive, -1, false, false
            );
        }
    }
}
