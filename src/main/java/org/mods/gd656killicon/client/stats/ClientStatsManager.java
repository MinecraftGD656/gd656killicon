package org.mods.gd656killicon.client.stats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 客户端本地统计管理器。
 *
 * <p>负责在客户端侧收集与持久化玩家相关的战斗统计数据，例如：</p>
 * <ul>
 *   <li>总击杀 / 总死亡 / 总助攻</li>
 *   <li>最大连杀数、最大击杀距离、总伤害</li>
 *   <li>武器使用次数、击杀最多的生物/玩家、宿敌（击杀你最多的玩家）</li>
 * </ul>
 *
 * <p>数据会通过 {@link StatsPersistenceManager} 持久化到本地 JSON 文件中。</p>
 */
public class ClientStatsManager {
    private static StatsPersistenceManager.ClientStatsData statsData;
    private static long currentStreak;

    /**
     * 初始化统计系统（可重复调用）。
     *
     * <p>通常在客户端启动或模组初始化阶段调用。若未显式调用，本类在首次使用时也会自动加载。</p>
     */
    public static void init() {
        ensureLoaded();
    }

    /**
     * 将当前统计数据写入本地文件。
     */
    public static void saveStats() {
        ensureLoaded();
        StatsPersistenceManager.saveStats(statsData);
    }

    /**
     * 记录一次击杀。
     *
     * <p>会自动识别受害者是否为玩家，并尝试提取可读名称用于聚合统计。</p>
     *
     * @param victim 被击杀的实体
     */
    public static void recordKill(LivingEntity victim) {
        ensureLoaded();
        String victimName = null;
        boolean isPlayer = false;

        if (victim instanceof Player) {
            victimName = victim.getName().getString();
            isPlayer = true;
        } else if (victim != null) {
            victimName = victim.getType().getDescription().getString();
        }

        recordGeneralKillStats(victimName, isPlayer);
    }

    /**
     * 记录通用击杀统计数据。
     *
     * <p>该方法用于统一维护与“击杀”相关的多个维度：</p>
     * <ul>
     *   <li>总击杀数、当前连杀、最大连杀</li>
     *   <li>武器使用次数（手持物品或载具名）</li>
     *   <li>按玩家/生物分类的击杀次数</li>
     * </ul>
     *
     * <p>调用完成后会落盘保存。</p>
     *
     * @param victimName 受害者展示名（可为 null/空）
     * @param isPlayer   是否为玩家
     */
    public static void recordGeneralKillStats(String victimName, boolean isPlayer) {
        ensureLoaded();
        statsData.totalKills++;
        currentStreak++;
        statsData.maxKillStreak = Math.max(statsData.maxKillStreak, currentStreak);

        recordWeaponKill();
        recordVictimKill(victimName, isPlayer);
        saveStats();
    }

    /**
     * 按生物名称记录击杀（并立即保存）。
     *
     * @param mobName 生物展示名
     */
    public static void recordMobKill(String mobName) {
        ensureLoaded();
        if (!isBlank(mobName)) {
            statsData.mobKillCounts.merge(mobName, 1L, Long::sum);
            saveStats();
        }
    }

    /**
     * 按玩家名称记录击杀（并立即保存）。
     *
     * @param playerName 玩家名
     */
    public static void recordPlayerKill(String playerName) {
        ensureLoaded();
        if (!isBlank(playerName)) {
            statsData.playerKillCounts.merge(playerName, 1L, Long::sum);
            saveStats();
        }
    }

    /**
     * 记录一次死亡（重置连杀计数并保存）。
     */
    public static void recordDeath() {
        ensureLoaded();
        statsData.totalDeaths++;
        currentStreak = 0;
        saveStats();
    }

    /**
     * 记录一次助攻并保存。
     */
    public static void recordAssist() {
        ensureLoaded();
        statsData.totalAssists++;
        saveStats();
    }

    /**
     * 记录造成的伤害并保存。
     *
     * @param damage 伤害值
     */
    public static void recordDamage(double damage) {
        ensureLoaded();
        statsData.totalDamageDealt += damage;
        saveStats();
    }

    /**
     * 记录一次击杀距离（更新最大值）并保存。
     *
     * @param distance 距离
     */
    public static void recordKillDistance(double distance) {
        ensureLoaded();
        statsData.maxKillDistance = Math.max(statsData.maxKillDistance, distance);
        saveStats();
    }

    /**
     * 记录“被某玩家击杀”的统计并保存，用于计算宿敌。
     *
     * @param playerName 玩家名
     */
    public static void recordDeathByPlayer(String playerName) {
        ensureLoaded();
        if (!isBlank(playerName)) {
            statsData.deathByPlayerCounts.merge(playerName, 1L, Long::sum);
            saveStats();
        }
    }

    /**
     * @return 总击杀数
     */
    public static long getTotalKills() {
        ensureLoaded();
        return statsData.totalKills;
    }

    /**
     * @return 总死亡数
     */
    public static long getTotalDeaths() {
        ensureLoaded();
        return statsData.totalDeaths;
    }

    /**
     * @return 总助攻数
     */
    public static long getTotalAssists() {
        ensureLoaded();
        return statsData.totalAssists;
    }

    /**
     * @return 当前连杀数
     */
    public static long getCurrentStreak() {
        ensureLoaded();
        return currentStreak;
    }

    /**
     * @return 最大击杀距离
     */
    public static double getMaxKillDistance() {
        ensureLoaded();
        return statsData.maxKillDistance;
    }

    /**
     * @return 最大连杀数
     */
    public static long getMaxKillStreak() {
        ensureLoaded();
        return statsData.maxKillStreak;
    }

    /**
     * @return 总造成伤害
     */
    public static double getTotalDamageDealt() {
        ensureLoaded();
        return statsData.totalDamageDealt;
    }

    /**
     * @return 使用次数最多的武器名（并列时用逗号拼接），无数据返回“无”
     */
    public static String getMostUsedWeapon() {
        ensureLoaded();
        return getMostFrequentKeys(statsData.weaponUseCounts);
    }

    /**
     * 获取击杀你最多的玩家列表（按次数降序）。
     *
     * @param count 返回条数
     * @return 宿敌玩家列表
     */
    public static List<PlayerStat> getTopNemesisPlayers(int count) {
        ensureLoaded();
        if (count <= 0) {
            return Collections.emptyList();
        }
        return statsData.deathByPlayerCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(count)
                .map(entry -> new PlayerStat(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * @return 被击杀次数最多的生物展示名，无数据返回“无”
     */
    public static String getMostKilledMob() {
        ensureLoaded();
        return statsData.mobKillCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("无");
    }

    /**
     * @return 被击杀次数最多的玩家名（并列时用逗号拼接），无数据返回“无”
     */
    public static String getMostKilledPlayer() {
        ensureLoaded();
        return getMostFrequentKeys(statsData.playerKillCounts);
    }

    /**
     * 获取宿敌玩家名（击杀你次数最多）。
     *
     * @return 宿敌名（并列时用逗号拼接），无数据返回“无”
     */
    public static String getNemesis() {
        ensureLoaded();
        return getMostFrequentKeys(statsData.deathByPlayerCounts);
    }

    /**
     * 简单统计项：名称 + 次数（生物）。
     */
    public static final class MobStat {
        public final String name;
        public final long count;

        public MobStat(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }

    /**
     * 简单统计项：名称 + 次数（玩家）。
     */
    public static final class PlayerStat {
        public final String name;
        public final long count;

        public PlayerStat(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }

    /**
     * 获取击杀次数最多的生物列表（按次数降序）。
     *
     * @param count 返回条数
     * @return 生物击杀榜
     */
    public static List<MobStat> getTopKilledMobs(int count) {
        ensureLoaded();
        if (count <= 0) {
            return Collections.emptyList();
        }
        return statsData.mobKillCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(count)
                .map(entry -> new MobStat(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 获取击杀次数最多的玩家列表（按次数降序）。
     *
     * @param count 返回条数
     * @return 玩家击杀榜
     */
    public static List<PlayerStat> getTopKilledPlayers(int count) {
        ensureLoaded();
        if (count <= 0) {
            return Collections.emptyList();
        }
        return statsData.playerKillCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(count)
                .map(entry -> new PlayerStat(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 简单统计项：名称 + 次数（武器/载具）。
     */
    public static final class WeaponStat {
        public final String name;
        public final long count;

        public WeaponStat(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }

    /**
     * 获取使用次数最多的武器列表（按次数降序）。
     *
     * @param count 返回条数
     * @return 武器使用榜
     */
    public static List<WeaponStat> getTopUsedWeapons(int count) {
        ensureLoaded();
        if (count <= 0) {
            return Collections.emptyList();
        }
        return statsData.weaponUseCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(count)
                .map(entry -> new WeaponStat(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static void ensureLoaded() {
        if (statsData == null) {
            statsData = StatsPersistenceManager.loadStats();
            currentStreak = 0;
        }
    }

    private static void recordWeaponKill() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        String weaponName;
        if (mc.player.getVehicle() != null) {
            weaponName = mc.player.getVehicle().getDisplayName().getString();
        } else {
            ItemStack heldItem = mc.player.getMainHandItem();
            weaponName = heldItem.isEmpty() ? I18n.get("gd656killicon.client.text.bare_hand") : heldItem.getHoverName().getString();
        }

        if (!isBlank(weaponName)) {
            statsData.weaponUseCounts.merge(weaponName, 1L, Long::sum);
        }
    }

    private static void recordVictimKill(String victimName, boolean isPlayer) {
        if (isBlank(victimName)) {
            return;
        }
        if (isPlayer) {
            statsData.playerKillCounts.merge(victimName, 1L, Long::sum);
        } else {
            statsData.mobKillCounts.merge(victimName, 1L, Long::sum);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String getMostFrequentKeys(Map<String, Long> counts) {
        if (counts == null || counts.isEmpty()) {
            return "无";
        }

        long maxCount = counts.values().stream()
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        if (maxCount <= 0) {
            return "无";
        }

        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (entry == null) {
                continue;
            }
            if (!Objects.equals(entry.getValue(), maxCount)) {
                continue;
            }
            if (isBlank(entry.getKey())) {
                continue;
            }
            keys.add(entry.getKey());
        }

        if (keys.isEmpty()) {
            return "无";
        }
        return String.join(" , ", keys);
    }
}
