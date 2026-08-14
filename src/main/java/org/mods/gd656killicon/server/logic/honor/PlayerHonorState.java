package org.mods.gd656killicon.server.logic.honor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * 单个玩家的荣誉会话状态。
 * <p>
 * 核心是「存活段」({@link LifeSegment}): 玩家在一次存活内累积的计数,
 * 死亡/重生时重置。KILL_STREAK / ACCUMULATE 类荣誉的累积判定依赖它。
 * </p>
 */
public final class PlayerHonorState {

    /**
     * 存活段状态: 按谓词/累计键维护计数器, 玩家死亡时整体重置。
     * <pre>
     * state.life().increment("headshot");   // 存活内爆头计数 +1
     * state.life().get("headshot");         // 当前存活内爆头计数
     * </pre>
     */
    public static final class LifeSegment {
        private final Map<String, Integer> counters = new HashMap<>();
        /** 本条命已达成的荣誉 id(达成后本条命内不再触发, 死亡/重生时清空)。 */
        private final java.util.Set<String> achieved = new java.util.HashSet<>();

        /** 计数 +1, 返回更新后的值。 */
        public int increment(String key) {
            return counters.merge(key, 1, Integer::sum);
        }

        /** 计数增加任意量, 返回更新后的值(ACCUMULATE 类用)。 */
        public int add(String key, int amount) {
            return counters.merge(key, amount, Integer::sum);
        }

        public int get(String key) {
            return counters.getOrDefault(key, 0);
        }

        /** 本次存活内达到 4 连杀的时刻(掠夺者窗口起点, 0 = 未达到)。 */
        private long combo4Time = 0L;
        /** 军械库窗口: 起始时刻(0 = 未开始)与已击杀物品集合。 */
        private long arsenalWindowStart = 0L;
        private final java.util.Set<String> arsenalWeapons = new java.util.HashSet<>();
        /** 急救窗口: 上次急救时刻与连续急救数(相邻间隔 ≤ 8 秒)。 */
        private long reviveLastTime = 0L;
        private int reviveStreak = 0;
        /** 兵种专家荣誉(支援/侦察/突击/工程): 按荣誉 id 累计本次存活获得的 gdki 分数(死亡 reset 清 0)。 */
        private final Map<String, Float> classScore = new HashMap<>();
        /** 烟幕: 本次存活在烟雾内完成的救援次数(死亡 reset 清 0)。 */
        private int smokeReviveCount = 0;
        /** 快枪手: 上次击杀的武器标识与时刻(死亡 reset 清 0)。 */
        private String lastKillWeapon = null;
        private long lastKillTime = 0L;
        /** 跳机火箭筒: 跳出空中载具的实体 id(0 = 未跳出)与跳出后是否已摧毁另一架空中载具。 */
        private int bailVehicleEntityId = 0;
        private boolean bailAirDestroyed = false;

        public boolean isAchieved(String honorId) {
            return achieved.contains(honorId);
        }

        public void markAchieved(String honorId) {
            achieved.add(honorId);
        }

        public void markCombo4(long time) {
            combo4Time = time;
        }

        /** 军械库窗口维护: 超时重置, 加入物品, 返回当前不同物品数。 */
        public int markArsenalWeapon(String weaponId, long now) {
            if (arsenalWindowStart == 0 || now - arsenalWindowStart > 30000) {
                arsenalWindowStart = now;
                arsenalWeapons.clear();
            }
            arsenalWeapons.add(weaponId);
            return arsenalWeapons.size();
        }

        public int getArsenalDistinct() {
            return arsenalWeapons.size();
        }

        public float getClassScore(String honorId) {
            return classScore.getOrDefault(honorId, 0F);
        }

        public void addClassScore(String honorId, float amount) {
            classScore.merge(honorId, amount, Float::sum);
        }

        public int getSmokeReviveCount() {
            return smokeReviveCount;
        }

        public void incrementSmokeRevive() {
            smokeReviveCount++;
        }

        public String getLastKillWeapon() {
            return lastKillWeapon;
        }

        public long getLastKillTime() {
            return lastKillTime;
        }

        /** 快枪手: 记录本次击杀的武器标识与时刻。 */
        public void markKill(String weapon, long time) {
            lastKillWeapon = weapon;
            lastKillTime = time;
        }

        /** 跳机火箭筒: 记录跳出空中载具(返回实体 id, 0 = 未跳出)。 */
        public int getBailVehicleEntityId() {
            return bailVehicleEntityId;
        }

        public boolean isBailAirDestroyed() {
            return bailAirDestroyed;
        }

        public void markBailOut(int vehicleEntityId) {
            bailVehicleEntityId = vehicleEntityId;
            bailAirDestroyed = false;
        }

        public void markBailAirDestroyed() {
            bailAirDestroyed = true;
        }

        public void clearBail() {
            bailVehicleEntityId = 0;
            bailAirDestroyed = false;
        }

        /**
         * 急救窗口维护(急救使者): 相邻急救间隔 ≤ 8 秒则连续计数, 达到 3 次(前 2 次 8 秒内 + 之后 8 秒内第 3 次)达成。
         * 达成后计数重置。
         */
        public boolean markRevive(long now) {
            if (reviveLastTime == 0 || now - reviveLastTime > 8000) {
                reviveStreak = 0;
            }
            reviveStreak++;
            reviveLastTime = now;
            if (reviveStreak >= 3) {
                reviveStreak = 0;
                return true;
            }
            return false;
        }

        public long getCombo4Time() {
            return combo4Time;
        }

        /** 清除该存活段全部计数与达成标记(死亡/重生时调用)。 */
        public void reset() {
            counters.clear();
            achieved.clear();
            combo4Time = 0L;
            arsenalWindowStart = 0L;
            arsenalWeapons.clear();
            reviveLastTime = 0L;
            reviveStreak = 0;
            classScore.clear();
            smokeReviveCount = 0;
            lastKillWeapon = null;
            lastKillTime = 0L;
            bailVehicleEntityId = 0;
            bailAirDestroyed = false;
        }
    }

    private final LifeSegment life = new LifeSegment();

    /** 掠夺者上次触发时刻(跨死亡保留的 30 秒冷却, 0 = 从未触发)。 */
    private long raiderLastTriggerTime = 0L;

    public LifeSegment life() {
        return life;
    }

    public long getRaiderLastTriggerTime() {
        return raiderLastTriggerTime;
    }

    public void markRaiderTriggered(long time) {
        raiderLastTriggerTime = time;
    }

    /** 玩家死亡/重生: 重置存活段状态。 */
    public void onDeath() {
        life.reset();
    }

    /**
     * 按玩家维护会话状态的容器。
     */
    public static final class Store {
        private final Map<UUID, PlayerHonorState> states = new ConcurrentHashMap<>();

        public PlayerHonorState getOrCreate(UUID playerId) {
            return states.computeIfAbsent(playerId, k -> new PlayerHonorState());
        }

        public PlayerHonorState get(UUID playerId) {
            return states.get(playerId);
        }

        public void remove(UUID playerId) {
            states.remove(playerId);
        }

        public void clear() {
            states.clear();
        }
    }
}
