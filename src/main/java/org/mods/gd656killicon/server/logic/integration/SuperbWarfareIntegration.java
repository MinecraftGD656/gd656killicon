package org.mods.gd656killicon.server.logic.integration;

import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.logic.superbwarfare.DummySuperbWarfareHandler;
import org.mods.gd656killicon.server.logic.superbwarfare.ISuperbWarfareHandler;
import org.mods.gd656killicon.server.util.ServerLog;

import java.util.UUID;

public class SuperbWarfareIntegration {
    private static final SuperbWarfareIntegration INSTANCE = new SuperbWarfareIntegration();
    private ISuperbWarfareHandler handler;
    private boolean initialized = false;

    private SuperbWarfareIntegration() {
        this.handler = new DummySuperbWarfareHandler();
    }

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
        try {
            if (ServerBridge.loader().isModLoaded("superbwarfare")) {
                Class<?> handlerClass = Class.forName("org.mods.gd656killicon.server.logic.superbwarfare.SuperbWarfareEventHandler");
                handler = (ISuperbWarfareHandler) handlerClass.getDeclaredConstructor().newInstance();
                handler.init();
                ServerLog.info("SuperbWarfare mod detected.");
            } else {
                handler = new DummySuperbWarfareHandler();
            }
        } catch (Exception e) {
            ServerLog.error("Failed to initialize SuperbWarfare integration: %s", e.getMessage());
            handler = new DummySuperbWarfareHandler();
        }
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
