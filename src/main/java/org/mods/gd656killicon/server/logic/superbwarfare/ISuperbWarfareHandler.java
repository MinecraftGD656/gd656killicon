package org.mods.gd656killicon.server.logic.superbwarfare;

import java.util.UUID;

public interface ISuperbWarfareHandler {
    void init();

    void tick();

    boolean isHeadshotKill(UUID attackerId, UUID victimId);

    boolean isHeadshotDamage(UUID attackerId, UUID victimId);

    boolean isGunKill(UUID victimId);
}
