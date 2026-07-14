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

    private final Map<UUID, Map<UUID, Long>> spottedTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> spottingBonusTimes = new ConcurrentHashMap<>();

    public void tick() {
        long now = System.currentTimeMillis();
        spottedTargets.entrySet().removeIf(entry -> {
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

        if (ServerData.get().isBonusEnabled(BonusType.SPOTTING) && tryConsumeSpottingBonus(spotter.getUUID(), now)) {
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
            if (ServerData.get().isBonusEnabled(BonusType.SPOTTING_KILL)) {
                ServerCore.BONUS.add(killer, BonusType.SPOTTING_KILL, 1.0f, "");
            }
            spottedTargets.remove(victimId);
            return;
        }

        for (UUID spotterId : spotters.keySet()) {
            ServerPlayer spotter = killer.getServer().getPlayerList().getPlayer(spotterId);
            if (spotter != null && isSameTeam(spotter, killer) && ServerData.get().isBonusEnabled(BonusType.SPOTTING_TEAM_ASSIST)) {
                ServerCore.BONUS.add(spotter, BonusType.SPOTTING_TEAM_ASSIST, 1.0f, "");
            }
        }
        spottedTargets.remove(victimId);
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
