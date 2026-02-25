package org.mods.gd656killicon.server.logic.spotting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SpottingEventHandler implements ISpottingHandler {
    private static final long SPOTTING_BONUS_WINDOW_MS = 60000L;
    private static final long SPOTTING_LOCK_WINDOW_MS = 7000L;
    private final Map<UUID, Map<UUID, Long>> spottedTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> spottingBonusTimes = new ConcurrentHashMap<>();

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        registerSpottingPostListener();
        ServerLog.info("Spotting event handler registered.");
    }

    @Override
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

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        LivingEntity victim = event.getEntity();
        Map<UUID, Long> spotters = spottedTargets.get(victim.getUUID());
        if (spotters == null || spotters.isEmpty()) return;
        long now = System.currentTimeMillis();
        spotters.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (spotters.isEmpty()) {
            spottedTargets.remove(victim.getUUID());
            return;
        }
        ServerPlayer killer = resolveKiller(event);
        if (killer == null) {
            spottedTargets.remove(victim.getUUID());
            return;
        }
        if (spotters.containsKey(killer.getUUID())) {
            if (ServerData.get().isBonusEnabled(BonusType.SPOTTING_KILL)) {
                ServerCore.BONUS.add(killer, BonusType.SPOTTING_KILL, 1.0f, "");
            }
            spottedTargets.remove(victim.getUUID());
            return;
        }
        for (UUID spotterId : spotters.keySet()) {
            ServerPlayer spotter = killer.getServer().getPlayerList().getPlayer(spotterId);
            if (spotter != null && isSameTeam(spotter, killer)) {
                if (ServerData.get().isBonusEnabled(BonusType.SPOTTING_TEAM_ASSIST)) {
                    ServerCore.BONUS.add(spotter, BonusType.SPOTTING_TEAM_ASSIST, 1.0f, "");
                }
            }
        }
        spottedTargets.remove(victim.getUUID());
    }

    private void registerSpottingPostListener() {
        try {
            Class<?> postClass = Class.forName("committee.nova.spotting.common.event.impl.SpottingEvent$Post");
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, (Class) postClass, (Consumer) this::onSpottingPost);
        } catch (Exception e) {
            ServerLog.error("Failed to register Spotting event listener: %s", e.getMessage());
        }
    }

    private void onSpottingPost(Object event) {
        ServerPlayer spotter = getSpotter(event);
        LivingEntity spottee = getSpottee(event);
        if (spotter == null || spottee == null) return;
        if (spotter.level().isClientSide) return;
        long expireAt = System.currentTimeMillis() + SPOTTING_LOCK_WINDOW_MS;
        spottedTargets.computeIfAbsent(spottee.getUUID(), k -> new ConcurrentHashMap<>()).put(spotter.getUUID(), expireAt);
        if (ServerData.get().isBonusEnabled(BonusType.SPOTTING) && tryConsumeSpottingBonus(spotter)) {
            ServerCore.BONUS.add(spotter, BonusType.SPOTTING, 1.0f, "");
        }
    }

    private boolean tryConsumeSpottingBonus(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Deque<Long> times = spottingBonusTimes.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
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

    private ServerPlayer getSpotter(Object event) {
        try {
            Method method = event.getClass().getMethod("getSpotter");
            Object result = method.invoke(event);
            return result instanceof ServerPlayer spotter ? spotter : null;
        } catch (Exception e) {
            return null;
        }
    }

    private LivingEntity getSpottee(Object event) {
        try {
            Method method = event.getClass().getMethod("getSpottee");
            Object result = method.invoke(event);
            return result instanceof LivingEntity spottee ? spottee : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSameTeam(ServerPlayer a, ServerPlayer b) {
        return a.getTeam() != null && b.getTeam() != null && a.getTeam().getName().equals(b.getTeam().getName());
    }

    private ServerPlayer resolveKiller(LivingDeathEvent event) {
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer player) {
            return player;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof ServerPlayer player) {
                return player;
            }
        }
        return null;
    }
}
