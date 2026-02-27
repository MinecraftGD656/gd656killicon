package org.mods.gd656killicon.server.logic.pingwheel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PingWheelEventHandler implements IPingWheelHandler {
    private static final long SPOTTING_BONUS_WINDOW_MS = 60000L;
    private static final long SPOTTING_LOCK_WINDOW_MS = 7000L;
    private final Map<UUID, Map<UUID, Long>> spottedTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> spottingBonusTimes = new ConcurrentHashMap<>();
    private boolean listenerRegistered = false;

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        registerPingListener();
        ServerLog.info("Ping Wheel event handler registered.");
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

    private void registerPingListener() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        try {
            Class<?> forgeMain = Class.forName("nx.pingwheel.forge.ForgeMain");
            Field channelField = forgeMain.getDeclaredField("PING_LOCATION_CHANNEL_C2S");
            Object channelObj = channelField.get(null);
            if (!(channelObj instanceof EventNetworkChannel channel)) {
                return;
            }
            Class<?> packetClass = Class.forName("nx.pingwheel.common.network.PingLocationC2SPacket");
            Method readSafe = packetClass.getMethod("readSafe", FriendlyByteBuf.class);
            Method entityMethod = packetClass.getMethod("entity");
            channel.addListener((event) -> {
                NetworkEvent.Context ctx = event.getSource().get();
                FriendlyByteBuf payload = event.getPayload();
                ServerPlayer sender = ctx.getSender();
                if (payload != null && sender != null) {
                    Object packet = null;
                    FriendlyByteBuf safeBuf = null;
                    int savedIndex = payload.readerIndex();
                    try {
                        payload.readerIndex(0);
                        safeBuf = new FriendlyByteBuf(payload.copy());
                    } catch (Exception ignored) {
                    } finally {
                        try {
                            payload.readerIndex(savedIndex);
                        } catch (Exception ignored) {}
                    }
                    try {
                        packet = safeBuf == null ? null : readSafe.invoke(null, safeBuf);
                    } catch (Exception ignored) {}
                    if (packet != null) {
                        UUID entityId = null;
                        try {
                            entityId = (UUID) entityMethod.invoke(packet);
                        } catch (Exception ignored) {}
                        if (entityId != null) {
                            UUID finalEntityId = entityId;
                            ctx.enqueueWork(() -> onPingEntity(sender, finalEntityId));
                        }
                    }
                }
                ctx.setPacketHandled(true);
            });
        } catch (Exception e) {
            ServerLog.error("Failed to register Ping Wheel listener: %s", e.getMessage());
        }
    }

    private void onPingEntity(ServerPlayer spotter, UUID targetId) {
        if (spotter == null || targetId == null) return;
        if (spotter.level().isClientSide) return;
        LivingEntity target = findLivingEntity(spotter.getServer(), targetId);
        if (target == null) return;
        long expireAt = System.currentTimeMillis() + SPOTTING_LOCK_WINDOW_MS;
        spottedTargets.computeIfAbsent(target.getUUID(), k -> new ConcurrentHashMap<>()).put(spotter.getUUID(), expireAt);
        if (ServerData.get().isBonusEnabled(BonusType.SPOTTING) && tryConsumeSpottingBonus(spotter)) {
            ServerCore.BONUS.add(spotter, BonusType.SPOTTING, 1.0f, "");
        }
    }

    private LivingEntity findLivingEntity(MinecraftServer server, UUID targetId) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(targetId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
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
