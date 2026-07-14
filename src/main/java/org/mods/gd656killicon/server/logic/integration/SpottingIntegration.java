package org.mods.gd656killicon.server.logic.integration;

import net.minecraft.world.entity.LivingEntity;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.logic.spotting.DummySpottingHandler;
import org.mods.gd656killicon.server.logic.spotting.ISpottingHandler;
import org.mods.gd656killicon.server.util.ServerLog;

public class SpottingIntegration {
    private static final SpottingIntegration INSTANCE = new SpottingIntegration();
    private ISpottingHandler handler;
    private boolean initialized = false;

    private SpottingIntegration() {
        this.handler = new DummySpottingHandler();
    }

    public static SpottingIntegration get() {
        return INSTANCE;
    }

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            if (ServerBridge.loader().isModLoaded("spotting")) {
                Class<?> handlerClass = Class.forName("org.mods.gd656killicon.server.logic.spotting.SpottingEventHandler");
                handler = (ISpottingHandler) handlerClass.getDeclaredConstructor().newInstance();
                handler.init();
                ServerLog.info("Spotting mod detected.");
            } else {
                handler = new DummySpottingHandler();
            }
        } catch (Exception e) {
            ServerLog.error("Failed to initialize Spotting integration: %s", e.getMessage());
            handler = new DummySpottingHandler();
        }
    }

    public void tick() {
        handler.tick();
    }

    public void onLivingDeath(LivingEntity victim, LivingEntity killer) {
        handler.onLivingDeath(victim, killer);
    }
}
