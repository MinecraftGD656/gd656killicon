package org.mods.gd656killicon.server.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

record DamageRecord(UUID attackerId, int amount, long timestamp) {}

record TeamKillRecord(UUID victimId, long timestamp) {}

record FireAttribution(UUID attackerId, long timestamp) {}

final class CombatState {
    boolean flawless = true;
    long firstInteractionTime;
    long lastInteractionTime;
    Vec3 initialPosition;

    CombatState(long time, Vec3 position) {
        this.firstInteractionTime = time;
        this.lastInteractionTime = time;
        this.initialPosition = position;
    }
}

final class PendingKill {
    ServerPlayer player;
    UUID victimId;
    int victimIdInt;
    Vec3 victimPos;
    String victimName;
    int combo;
    int damageType;
    float maxHealth;
    float distance;
    int delay;
    int sourceEntityId;
    long tick;
    boolean isGun;
    boolean isVictimThreat;
    boolean isBackstab;
    boolean isGliding;
    boolean isJusticeFromAbove;
    boolean isFlawless;
    boolean isVictimBlinded;
    boolean hasHelmet;
    boolean isVictimPlayer;
    boolean isLockedTarget;
    boolean isHoldPosition;
    long streakCount;

    PendingKill(ServerPlayer player, UUID victimId, int victimIdInt, Vec3 victimPos, String victimName, int combo, int damageType, float maxHealth, float distance, int delay, int sourceEntityId, long tick, boolean isGun, boolean isVictimThreat, boolean isBackstab, boolean isGliding, boolean isJusticeFromAbove, boolean isFlawless, boolean isVictimBlinded, boolean hasHelmet, boolean isVictimPlayer, boolean isLockedTarget, boolean isHoldPosition, long streakCount) {
        this.player = player;
        this.victimId = victimId;
        this.victimIdInt = victimIdInt;
        this.victimPos = victimPos;
        this.victimName = victimName;
        this.combo = combo;
        this.damageType = damageType;
        this.maxHealth = maxHealth;
        this.distance = distance;
        this.delay = delay;
        this.sourceEntityId = sourceEntityId;
        this.tick = tick;
        this.isGun = isGun;
        this.isVictimThreat = isVictimThreat;
        this.isBackstab = isBackstab;
        this.isGliding = isGliding;
        this.isJusticeFromAbove = isJusticeFromAbove;
        this.isFlawless = isFlawless;
        this.isVictimBlinded = isVictimBlinded;
        this.hasHelmet = hasHelmet;
        this.isVictimPlayer = isVictimPlayer;
        this.isLockedTarget = isLockedTarget;
        this.isHoldPosition = isHoldPosition;
        this.streakCount = streakCount;
    }
}
