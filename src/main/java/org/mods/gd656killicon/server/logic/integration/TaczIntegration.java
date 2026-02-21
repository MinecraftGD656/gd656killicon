package org.mods.gd656killicon.server.logic.integration;

import net.minecraftforge.fml.ModList;
import org.mods.gd656killicon.server.logic.tacz.DummyTaczHandler;
import org.mods.gd656killicon.server.logic.tacz.ITaczHandler;
import org.mods.gd656killicon.server.util.ServerLog;

import java.util.UUID;

public class TaczIntegration {
    private static final TaczIntegration INSTANCE = new TaczIntegration();
    private ITaczHandler handler;

    private TaczIntegration() {
        this.handler = new DummyTaczHandler();
    }

    public static TaczIntegration get() {
        return INSTANCE;
    }

    /**
     * Initializes the TACZ integration.
     * Attempts to load the real handler if the mod is present, otherwise falls back to a dummy.
     */
    public void init() {
        try {
            if (ModList.get().isLoaded("tacz")) {
                Class<?> handlerClass = Class.forName("org.mods.gd656killicon.server.logic.tacz.TaczEventHandler");
                handler = (ITaczHandler) handlerClass.getDeclaredConstructor().newInstance();
                handler.init();
                ServerLog.info("TACZ mod detected.");
            } else {
                handler = new DummyTaczHandler();
            }
        } catch (Exception e) {
            ServerLog.error("Failed to initialize TACZ integration: %s", e.getMessage());
            handler = new DummyTaczHandler();
        }
    }

    public void tick() {
        handler.tick();
    }

    public boolean isHeadshotKill(UUID victimId) {
        return handler.isHeadshotKill(victimId);
    }

    public boolean isHeadshotDamage(UUID victimId) {
        return handler.isHeadshotDamage(victimId);
    }

    public boolean isLastBulletKill(UUID victimId) {
        return handler.isLastBulletKill(victimId);
    }

    public boolean isGunKill(UUID victimId) {
        return handler.isGunKill(victimId);
    }
}
