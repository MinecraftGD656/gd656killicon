package org.mods.gd656killicon.server.logic.pingwheel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.logic.core.SpottingRewardTracker;
import org.mods.gd656killicon.server.logic.pingwheel.IPingWheelHandler;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class PingWheelEventHandler implements IPingWheelHandler {
    private final SpottingRewardTracker rewardTracker = new SpottingRewardTracker();
    private boolean listenerRegistered = false;

    @Override
    public void init() {
        ServerBridge.loader().registerForgeEventBusSubscriber(this);
        registerPingListener();
        ServerLog.info("Ping Wheel event handler registered.");
    }

    @Override
    public void tick() {
        rewardTracker.tick();
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        rewardTracker.handleTargetKilled(event.getEntity(), resolveKiller(event));
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
        rewardTracker.recordSpot(spotter, target);
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
