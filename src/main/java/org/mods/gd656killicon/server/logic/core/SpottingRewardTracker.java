package org.mods.gd656killicon.server.logic.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.ServerData;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpottingRewardTracker {
    private static final long SPOTTING_BONUS_WINDOW_MS = 60000L;
    private static final long SPOTTING_LOCK_WINDOW_MS = 7000L;
    /** 标示助攻窗口: 索敌后 30 秒内被队友击杀, 标记者获得标示助攻 */
    private static final long SPOT_ASSIST_WINDOW_MS = 30000L;

    /** true = 同时发放 SPOTTING/SPOTTING_KILL/SPOTTING_TEAM_ASSIST 加分(索敌模组/pingwheel);
     *  false = 仅记录并触发标示助攻, 不加分(conquest 自己已发放加分) */
    private final boolean bonusMode;

    public SpottingRewardTracker() {
        this(true);
    }

    public SpottingRewardTracker(boolean bonusMode) {
        this.bonusMode = bonusMode;
    }

    private final Map<UUID, Map<UUID, Long>> spottedTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> spottingBonusTimes = new ConcurrentHashMap<>();
    /** 标示助攻独立 30s 记录(victim -> (spotter -> 过期时间戳)), 不影响 7s 索敌锁定窗口 */
    private final Map<UUID, Map<UUID, Long>> spotAssistTargets = new ConcurrentHashMap<>();

    public void tick() {
        long now = System.currentTimeMillis();
        spottedTargets.entrySet().removeIf(entry -> {
            Map<UUID, Long> spotters = entry.getValue();
            spotters.entrySet().removeIf(item -> item.getValue() <= now);
            return spotters.isEmpty();
        });
        spotAssistTargets.entrySet().removeIf(entry -> {
            Map<UUID, Long> spotters = entry.getValue();
            spotters.entrySet().removeIf(item -> item.getValue() <= now);
            return spotters.isEmpty();
        });
        spottingBonusTimes.entrySet().removeIf(entry -> {
            Deque<Long> times = entry.getValue();
            synchronized (times) {
                while (!times.isEmpty() && now - times.peekFirst() > SPOTTING_BONUS_WINDOW_MS) {
                    times.pollFirst();
                }
                return times.isEmpty();
            }
        });
    }

    public void recordSpot(ServerPlayer spotter, LivingEntity target) {
        if (spotter == null || target == null || spotter.level().isClientSide) {
            return;
        }

        long now = System.currentTimeMillis();
        spottedTargets
            .computeIfAbsent(target.getUUID(), key -> new ConcurrentHashMap<>())
            .put(spotter.getUUID(), now + SPOTTING_LOCK_WINDOW_MS);
        // 标示助攻独立 30s 记录(与 7s 索敌锁定窗口分离)
        spotAssistTargets
            .computeIfAbsent(target.getUUID(), key -> new ConcurrentHashMap<>())
            .put(spotter.getUUID(), now + SPOT_ASSIST_WINDOW_MS);

        // 斥候荣誉: 标记事件统一入口(每次标记 +1, 无索敌加分项 CD; 各标记来源都汇聚于此)
        org.mods.gd656killicon.server.ServerCore.HONOR.onScoutMark(spotter);

        if (bonusMode && ServerData.get().isBonusEnabled(BonusType.SPOTTING) && tryConsumeSpottingBonus(spotter.getUUID(), now)) {
            ServerCore.BONUS.add(spotter, BonusType.SPOTTING, 1.0f, "");
        }
    }

    public void handleTargetKilled(LivingEntity victim, LivingEntity killerEntity) {
        if (victim == null || victim.level().isClientSide) {
            return;
        }

        UUID victimId = victim.getUUID();
        Map<UUID, Long> spotters = spottedTargets.get(victimId);
        if (spotters == null || spotters.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        spotters.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (spotters.isEmpty()) {
            spottedTargets.remove(victimId);
            return;
        }

        if (!(killerEntity instanceof ServerPlayer killer)) {
            spottedTargets.remove(victimId);
            return;
        }

        if (spotters.containsKey(killer.getUUID())) {
            if (bonusMode && ServerData.get().isBonusEnabled(BonusType.SPOTTING_KILL)) {
                ServerCore.BONUS.add(killer, BonusType.SPOTTING_KILL, 1.0f, "");
            }
            spottedTargets.remove(victimId);
            spotAssistTargets.remove(victimId);
            return;
        }

        for (UUID spotterId : spotters.keySet()) {
            ServerPlayer spotter = killer.getServer().getPlayerList().getPlayer(spotterId);
            if (spotter == null) {
                continue;
            }
            if (isSameTeam(spotter, killer) && bonusMode && ServerData.get().isBonusEnabled(BonusType.SPOTTING_TEAM_ASSIST)) {
                ServerCore.BONUS.add(spotter, BonusType.SPOTTING_TEAM_ASSIST, 1.0f, "");
            }
            // 标示助攻: 标记者(非击杀者本人)在 30s 窗口内, 目标被同队队友击杀 → 发 kill_feed + 图标
            if (isSpotAssistActive(victimId, spotterId, now)) {
                org.mods.gd656killicon.server.network.ServerPacketDispatcher.sendSpotAssistEffects(spotter, victim);
            }
        }
        spottedTargets.remove(victimId);
        spotAssistTargets.remove(victimId);
    }

    /** 标记者是否在标示助攻 30s 窗口内(独立记录) */
    private boolean isSpotAssistActive(UUID victimId, UUID spotterId, long now) {
        Map<UUID, Long> assists = spotAssistTargets.get(victimId);
        return assists != null && assists.getOrDefault(spotterId, 0L) > now;
    }

    private boolean tryConsumeSpottingBonus(UUID playerId, long now) {
        Deque<Long> times = spottingBonusTimes.computeIfAbsent(playerId, key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > SPOTTING_BONUS_WINDOW_MS) {
                times.pollFirst();
            }
            if (times.size() >= 2) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    private boolean isSameTeam(ServerPlayer a, ServerPlayer b) {
        return a.getTeam() != null && b.getTeam() != null && a.getTeam().getName().equals(b.getTeam().getName());
    }
}
