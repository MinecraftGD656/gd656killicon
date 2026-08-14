package org.mods.gd656killicon.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("uuid")
    private UUID uuid;

    @SerializedName("score")
    private float score;

    @SerializedName("kill")
    private int kill;

    @SerializedName("death")
    private int death;

    @SerializedName("assist")
    private int assist;

    @SerializedName("revive")
    private int revive;

    @SerializedName("last_login_name")
    private String lastLoginName;

    @SerializedName("last_modified")
    private long lastModified;

    @SerializedName("metadata")
    private Map<String, Object> metadata;

    /** 该玩家各 honor 累计获得次数(稀疏, 只存数量非 0 的条目)。 */
    @SerializedName("honor_counts")
    private Map<String, Integer> honorCounts;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.score = 0.0f;
        this.kill = 0;
        this.death = 0;
        this.assist = 0;
        this.revive = 0;
        this.lastLoginName = "";
        this.lastModified = System.currentTimeMillis();
        this.metadata = new HashMap<>();
        this.honorCounts = new HashMap<>();
    }

    public UUID getUUID() {
        return uuid;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = Math.max(0, score);
        this.lastModified = System.currentTimeMillis();
    }

    public void addScore(float amount) {
        this.score = Math.max(0, this.score + amount);
        this.lastModified = System.currentTimeMillis();
    }

    public int getKill() {
        return kill;
    }

    public void setKill(int kill) {
        this.kill = Math.max(0, kill);
        this.lastModified = System.currentTimeMillis();
    }

    public void addKill(int amount) {
        this.kill = Math.max(0, this.kill + amount);
        this.lastModified = System.currentTimeMillis();
    }

    public void reduceScore(float amount) {
        this.score = Math.max(0, this.score - amount);
        this.lastModified = System.currentTimeMillis();
    }

    public void reduceKill(int amount) {
        this.kill = Math.max(0, this.kill - amount);
        this.lastModified = System.currentTimeMillis();
    }

    public int getDeath() {
        return death;
    }

    public void setDeath(int death) {
        this.death = Math.max(0, death);
        this.lastModified = System.currentTimeMillis();
    }

    public void addDeath(int amount) {
        this.death = Math.max(0, this.death + amount);
        this.lastModified = System.currentTimeMillis();
    }

    public void reduceDeath(int amount) {
        this.death = Math.max(0, this.death - amount);
        this.lastModified = System.currentTimeMillis();
    }

    public int getAssist() {
        return assist;
    }

    public void setAssist(int assist) {
        this.assist = Math.max(0, assist);
        this.lastModified = System.currentTimeMillis();
    }

    public void addAssist(int amount) {
        this.assist = Math.max(0, this.assist + amount);
        this.lastModified = System.currentTimeMillis();
    }

    public void reduceAssist(int amount) {
        this.assist = Math.max(0, this.assist - amount);
        this.lastModified = System.currentTimeMillis();
    }

    public int getRevive() {
        return revive;
    }

    public void setRevive(int revive) {
        this.revive = Math.max(0, revive);
        this.lastModified = System.currentTimeMillis();
    }

    public void addRevive(int amount) {
        this.revive = Math.max(0, this.revive + amount);
        this.lastModified = System.currentTimeMillis();
    }

    public void reduceRevive(int amount) {
        this.revive = Math.max(0, this.revive - amount);
        this.lastModified = System.currentTimeMillis();
    }

    public String getLastLoginName() {
        return lastLoginName != null ? lastLoginName : "";
    }

    public void setLastLoginName(String lastLoginName) {
        this.lastLoginName = lastLoginName;
        this.lastModified = System.currentTimeMillis();
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void setMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        this.lastModified = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        if (this.metadata == null) {
            return null;
        }
        Object value = this.metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public Map<String, Object> getAllMetadata() {
        return this.metadata != null ? new HashMap<>(this.metadata) : new HashMap<>();
    }

    /** 该玩家某 honor 累计获得次数(未获得过返回 0)。 */
    public int getHonorCount(String honorId) {
        if (honorId == null || honorCounts == null) {
            return 0;
        }
        Integer count = honorCounts.get(honorId);
        return count != null ? count : 0;
    }

    /** 该玩家各 honor 累计次数(副本; 未记录过返回空 map)。 */
    public Map<String, Integer> getAllHonorCounts() {
        return honorCounts != null ? new HashMap<>(honorCounts) : new HashMap<>();
    }

    /** 是否记录过任何 honor(供 hasTrackedStats 判断, 防止只有 honor 数据的玩家文件被清理)。 */
    public boolean hasAnyHonor() {
        return honorCounts != null && !honorCounts.isEmpty();
    }

    /** 记录一次 honor 获得, 返回该 honor 新的累计次数(只保存数量非 0 的条目)。 */
    public int addHonor(String honorId) {
        if (honorId == null || honorId.isBlank()) {
            return 0;
        }
        if (honorCounts == null) {
            honorCounts = new HashMap<>();
        }
        int newCount = honorCounts.merge(honorId, 1, Integer::sum);
        if (newCount <= 0) {
            honorCounts.remove(honorId);
        }
        this.lastModified = System.currentTimeMillis();
        return newCount;
    }

    /** 设置某 honor 获取数量(<=0 视为清零删除), 供指令修改玩家荣誉计数使用。 */
    public void setHonorCount(String honorId, int count) {
        if (honorId == null || honorId.isBlank()) {
            return;
        }
        if (honorCounts == null) {
            honorCounts = new HashMap<>();
        }
        if (count <= 0) {
            honorCounts.remove(honorId);
        } else {
            honorCounts.put(honorId, count);
        }
        this.lastModified = System.currentTimeMillis();
    }

    /** 对某 honor 获取数量加值(可为负), 供指令修改玩家荣誉计数使用。 */
    public void addHonorCount(String honorId, int amount) {
        if (honorId == null || honorId.isBlank() || amount == 0) {
            return;
        }
        if (honorCounts == null) {
            honorCounts = new HashMap<>();
        }
        int result = honorCounts.merge(honorId, amount, Integer::sum);
        if (result <= 0) {
            honorCounts.remove(honorId);
        }
        this.lastModified = System.currentTimeMillis();
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static PlayerData fromJson(String json) {
        return GSON.fromJson(json, PlayerData.class);
    }

    public static PlayerData fromJson(String json, UUID uuid) {
        PlayerData data = GSON.fromJson(json, PlayerData.class);
        if (data == null) {
            return new PlayerData(uuid);
        }
        data.uuid = uuid;
        return data;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", score=" + score +
                ", kill=" + kill +
                ", death=" + death +
                ", assist=" + assist +
                ", revive=" + revive +
                ", lastLoginName='" + lastLoginName + '\'' +
                ", lastModified=" + lastModified +
                ", metadata=" + metadata +
                '}';
    }
}
