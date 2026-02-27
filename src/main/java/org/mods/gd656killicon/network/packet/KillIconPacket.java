package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.client.render.HudElementManager;
import org.mods.gd656killicon.client.render.impl.ComboIconRenderer;
import org.mods.gd656killicon.client.sounds.SoundTriggerManager;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.IPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class KillIconPacket implements IPacket {
    private static final long VEHICLE_PRIORITY_DELAY_MS = 700L;
    private static final Deque<PendingTrigger> PENDING_TRIGGERS = new ArrayDeque<>();
    private static long lastVehicleDestroyTime = -1L;

    private final String category;
    private final String name;
    private final int killType;
    private final int comboCount;
    private final int victimId;
    private final double comboWindowSeconds;
    private final boolean hasHelmet;
    private final String customVictimName;
    private final boolean isVictimPlayer;
    private final boolean shouldRecordStats;
    private final float distance;

    public KillIconPacket(String category, String name, int killType, int victimId) {
        this(category, name, killType, 0, victimId, -1.0, false, "", false, false);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId) {
        this(category, name, killType, comboCount, victimId, -1.0, false, "", false, false);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds) {
        this(category, name, killType, comboCount, victimId, comboWindowSeconds, false, "", false, false, 0.0f);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds, boolean hasHelmet) {
        this(category, name, killType, comboCount, victimId, comboWindowSeconds, hasHelmet, "", false, false, 0.0f);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds, boolean hasHelmet, String customVictimName) {
        this(category, name, killType, comboCount, victimId, comboWindowSeconds, hasHelmet, customVictimName, false, false, 0.0f);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds, boolean hasHelmet, String customVictimName, boolean isVictimPlayer) {
        this(category, name, killType, comboCount, victimId, comboWindowSeconds, hasHelmet, customVictimName, isVictimPlayer, false, 0.0f);
    }
    
    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds, boolean hasHelmet, String customVictimName, boolean isVictimPlayer, boolean shouldRecordStats) {
        this(category, name, killType, comboCount, victimId, comboWindowSeconds, hasHelmet, customVictimName, isVictimPlayer, shouldRecordStats, 0.0f);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds, boolean hasHelmet, String customVictimName, boolean isVictimPlayer, boolean shouldRecordStats, float distance) {
        this.category = category;
        this.name = name;
        this.killType = killType;
        this.comboCount = comboCount;
        this.victimId = victimId;
        this.comboWindowSeconds = comboWindowSeconds;
        this.hasHelmet = hasHelmet;
        this.customVictimName = customVictimName == null ? "" : customVictimName;
        this.isVictimPlayer = isVictimPlayer;
        this.shouldRecordStats = shouldRecordStats;
        this.distance = distance;
    }

    public KillIconPacket(FriendlyByteBuf buffer) {
        this.category = buffer.readUtf();
        this.name = buffer.readUtf();
        this.killType = buffer.readInt();
        this.comboCount = buffer.readInt();
        this.victimId = buffer.readInt();
        this.comboWindowSeconds = buffer.readDouble();
        this.hasHelmet = buffer.readBoolean();
        this.customVictimName = buffer.readUtf();
        this.isVictimPlayer = buffer.readBoolean();
        this.shouldRecordStats = buffer.readBoolean();
        this.distance = buffer.readFloat();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.category);
        buffer.writeUtf(this.name);
        buffer.writeInt(this.killType);
        buffer.writeInt(this.comboCount);
        buffer.writeInt(this.victimId);
        buffer.writeDouble(this.comboWindowSeconds);
        buffer.writeBoolean(this.hasHelmet);
        buffer.writeUtf(this.customVictimName);
        buffer.writeBoolean(this.isVictimPlayer);
        buffer.writeBoolean(this.shouldRecordStats);
        buffer.writeFloat(this.distance);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ComboIconRenderer.updateServerComboWindowSeconds(this.comboWindowSeconds);
            long now = System.currentTimeMillis();
            if (this.killType == KillType.DESTROY_VEHICLE) {
                lastVehicleDestroyTime = now;
                processTrigger(this, now);
                return;
            }

            long delayUntil = lastVehicleDestroyTime > 0 && now - lastVehicleDestroyTime < VEHICLE_PRIORITY_DELAY_MS
                ? lastVehicleDestroyTime + VEHICLE_PRIORITY_DELAY_MS
                : -1L;

            if (delayUntil > now) {
                PENDING_TRIGGERS.add(new PendingTrigger(this, delayUntil));
            } else {
                processTrigger(this, now);
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void processPendingTriggers() {
        if (PENDING_TRIGGERS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        while (!PENDING_TRIGGERS.isEmpty()) {
            PendingTrigger pending = PENDING_TRIGGERS.peekFirst();
            if (pending == null || pending.executeAt > now) {
                return;
            }
            PENDING_TRIGGERS.pollFirst();
            processTrigger(pending.packet, now);
        }
    }

    private static void processTrigger(KillIconPacket packet, long now) {
        SoundTriggerManager.tryPlaySound(packet.category, packet.name, packet.killType, packet.comboCount, packet.hasHelmet);

        String displayName = packet.customVictimName;
        if (packet.customVictimName != null && !packet.customVictimName.isEmpty() && !packet.isVictimPlayer) {
            try {
                displayName = net.minecraft.client.resources.language.I18n.get(packet.customVictimName);
            } catch (Exception ignored) {
            }
        }

        HudElementManager.trigger(packet.category, packet.name, 
            new org.mods.gd656killicon.client.render.IHudRenderer.TriggerContext(
                packet.killType, packet.victimId, packet.comboCount, displayName, packet.distance
            )
        );

        if (packet.shouldRecordStats && displayName != null && !displayName.isEmpty()) {
            org.mods.gd656killicon.client.stats.ClientStatsManager.recordGeneralKillStats(displayName, packet.isVictimPlayer);
        }

        org.mods.gd656killicon.client.util.AceLagSimulator.onKillEvent();
    }

    private static final class PendingTrigger {
        private final KillIconPacket packet;
        private final long executeAt;

        private PendingTrigger(KillIconPacket packet, long executeAt) {
            this.packet = packet;
            this.executeAt = executeAt;
        }
    }
}
