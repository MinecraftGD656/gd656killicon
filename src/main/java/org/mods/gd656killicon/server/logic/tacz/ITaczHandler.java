package org.mods.gd656killicon.server.logic.tacz;

import java.util.UUID;

public interface ITaczHandler {
    void init();
    void tick();
    boolean isHeadshotKill(UUID attackerId, UUID victimId);
    boolean isHeadshotDamage(UUID attackerId, UUID victimId);
    boolean isLastBulletKill(UUID victimId);
    boolean isGunKill(UUID victimId);
}
