package org.mods.gd656killicon.server.logic.integration;

import org.mods.gd656killicon.server.logic.tacz.DummyTaczHandler;
import org.mods.gd656killicon.server.logic.tacz.ITaczHandler;

import java.util.UUID;

public class TaczIntegration {
    private static final TaczIntegration INSTANCE = new TaczIntegration();
    private final ITaczHandler handler = new DummyTaczHandler();
    private boolean initialized = false;

    private TaczIntegration() {}

    public static TaczIntegration get() {
        return INSTANCE;
    }

    /**
     * Initializes the TACZ integration.
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

    public boolean isLastBulletKill(UUID victimId) {
        return handler.isLastBulletKill(victimId);
    }

    public boolean isGunKill(UUID victimId) {
        return handler.isGunKill(victimId);
    }
}
