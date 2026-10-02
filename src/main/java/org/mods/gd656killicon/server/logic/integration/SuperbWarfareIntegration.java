package org.mods.gd656killicon.server.logic.integration;

import org.mods.gd656killicon.server.logic.superbwarfare.DummySuperbWarfareHandler;
import org.mods.gd656killicon.server.logic.superbwarfare.ISuperbWarfareHandler;

import java.util.UUID;

public class SuperbWarfareIntegration {
    private static final SuperbWarfareIntegration INSTANCE = new SuperbWarfareIntegration();
    private final ISuperbWarfareHandler handler = new DummySuperbWarfareHandler();
    private boolean initialized = false;

    private SuperbWarfareIntegration() {}

    public static SuperbWarfareIntegration get() {
        return INSTANCE;
    }

    /**
     * Initializes the SuperbWarfare integration.
     * Attempts to load the real handler if the mod is present, otherwise falls back to a dummy.
     */
    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        handler.init();
    }

    public void tick() {
        handler.tick();
    }

    public boolean isHeadshotKill(UUID attackerId, UUID victimId) {
        return handler.isHeadshotKill(attackerId, victimId);
    }

    public boolean isHeadshotDamage(UUID attackerId, UUID victimId) {
        return handler.isHeadshotDamage(attackerId, victimId);
    }

    public boolean isGunKill(UUID victimId) {
        return handler.isGunKill(victimId);
    }
}
