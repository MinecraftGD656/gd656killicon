package org.mods.gd656killicon.client.stats;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mods.gd656killicon.Gd656killicon;

public class ClientStatsManager {
    private static StatsPersistenceManager.ClientStatsData statsData;
    private static long currentStreak = 0;

    public static void init() {
        // 加载统计数据
        statsData = StatsPersistenceManager.loadStats();
    }

    public static void saveStats() {
        StatsPersistenceManager.saveStats(statsData);
    }
    
    public static void recordKill(LivingEntity victim) {
        // 使用通用逻辑记录基础数据
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
     * 记录通用的击杀统计数据（总击杀数、连杀、武器使用、特定生物/玩家击杀）
     * 供 KillIconPacket 和 recordKill 调用
     */
    public static void recordGeneralKillStats(String victimName, boolean isPlayer) {
        statsData.totalKills++;
        currentStreak++;
        statsData.maxKillStreak = Math.max(statsData.maxKillStreak, currentStreak);

        // 记录武器击杀
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            String weaponName;
            if (mc.player.getVehicle() != null) {
                weaponName = mc.player.getVehicle().getDisplayName().getString();
            } else {
                ItemStack heldItem = mc.player.getMainHandItem();
                if (heldItem.isEmpty()) {
                    weaponName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.bare_hand");
                } else {
                    weaponName = heldItem.getHoverName().getString();
                }
            }
            if (weaponName != null && !weaponName.isEmpty()) {
                statsData.weaponUseCounts.merge(weaponName, 1L, Long::sum);
            }
        }

        if (isPlayer) {
            recordPlayerKill(victimName);
        } else {
            recordMobKill(victimName);
        }

        // 保存统计数据
        saveStats();
    }

    public static void recordMobKill(String mobName) {
        if (mobName != null && !mobName.isEmpty()) {
            statsData.mobKillCounts.merge(mobName, 1L, Long::sum);
            saveStats();
        }
    }

    public static void recordPlayerKill(String playerName) {
        if (playerName != null && !playerName.isEmpty()) {
            statsData.playerKillCounts.merge(playerName, 1L, Long::sum);
            saveStats();
        }
    }

    public static void recordDeath() {
        statsData.totalDeaths++;
        currentStreak = 0;
        saveStats();
    }

    public static void recordAssist() {
        statsData.totalAssists++;
        saveStats();
    }

    public static void recordDamage(double damage) {
        statsData.totalDamageDealt += damage;
        saveStats();
    }

    public static void recordKillDistance(double distance) {
        statsData.maxKillDistance = Math.max(statsData.maxKillDistance, distance);
        saveStats();
    }

    public static void recordDeathByPlayer(String playerName) {
        statsData.deathByPlayerCounts.merge(playerName, 1L, Long::sum);
        saveStats();
    }

    public static long getTotalKills() { return statsData.totalKills; }
    public static long getTotalDeaths() { return statsData.totalDeaths; }
    public static long getTotalAssists() { return statsData.totalAssists; }
    public static double getMaxKillDistance() { return statsData.maxKillDistance; }
    public static long getMaxKillStreak() { return statsData.maxKillStreak; }
    public static double getTotalDamageDealt() { return statsData.totalDamageDealt; }

    public static String getMostUsedWeapon() {
        if (statsData.weaponUseCounts.isEmpty()) {
            return "无";
        }
        long maxCount = statsData.weaponUseCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (maxCount == 0) {
            return "无";
        }
        List<String> list = statsData.weaponUseCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .toList();
        return String.join(" , ", list);
    }

    public static List<PlayerStat> getTopNemesisPlayers(int count) {
        return statsData.deathByPlayerCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(count)
                .map(entry -> new PlayerStat(entry.getKey(), entry.getValue()))
                .toList();
    }

    public static String getMostKilledMob() {
        return statsData.mobKillCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("无");
    }

    public static String getMostKilledPlayer() {
        if (statsData.playerKillCounts.isEmpty()) {
            return "无";
        }
        long maxCount = statsData.playerKillCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (maxCount == 0) {
            return "无";
        }
        List<String> list = statsData.playerKillCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .toList();
        return String.join(" , ", list);
    }

    public static String getNemesis() {
        if (statsData.deathByPlayerCounts.isEmpty()) {
            return "无";
        }

        long maxCount = statsData.deathByPlayerCounts.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        if (maxCount == 0) {
            return "无";
        }

        List<String> nemesisList = statsData.deathByPlayerCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .toList();

        return String.join(" , ", nemesisList);
    }

    public static class MobStat {
        public final String name;
        public final long count;

        public MobStat(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }

    public static class PlayerStat {
        public final String name;
        public final long count;

        public PlayerStat(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }

    public static List<MobStat> getTopKilledMobs(int count) {
        return statsData.mobKillCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(count)
                .map(entry -> new MobStat(entry.getKey(), entry.getValue()))
                .toList();
    }

    public static List<PlayerStat> getTopKilledPlayers(int count) {
        return statsData.playerKillCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(count)
                .map(entry -> new PlayerStat(entry.getKey(), entry.getValue()))
                .toList();
    }

    public static class WeaponStat {
        public final String name;
        public final long count;

        public WeaponStat(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }

    public static List<WeaponStat> getTopUsedWeapons(int count) {
        return statsData.weaponUseCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(count)
                .map(entry -> new WeaponStat(entry.getKey(), entry.getValue()))
                .toList();
    }
}
