package org.mods.gd656killicon.server.logic.integration;

import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.logic.conquest.DummyConquestBattlefieldHandler;
import org.mods.gd656killicon.server.logic.conquest.IConquestBattlefieldHandler;
import org.mods.gd656killicon.server.util.ServerLog;

public class ConquestBattlefieldIntegration {
    private static final ConquestBattlefieldIntegration INSTANCE = new ConquestBattlefieldIntegration();
    private IConquestBattlefieldHandler handler;
    private boolean initialized = false;

    private ConquestBattlefieldIntegration() {
        this.handler = new DummyConquestBattlefieldHandler();
    }

    public static ConquestBattlefieldIntegration get() {
        return INSTANCE;
    }

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            if (ServerBridge.loader().isModLoaded("gd656conquest")) {
                Class<?> handlerClass = Class.forName("org.mods.gd656killicon.server.logic.conquest.ConquestBattlefieldEventHandler");
                handler = (IConquestBattlefieldHandler) handlerClass.getDeclaredConstructor().newInstance();
                handler.init();
                ServerLog.info("GD656Conquest mod detected.");
            } else {
                handler = new DummyConquestBattlefieldHandler();
            }
        } catch (Exception e) {
            ServerLog.error("Failed to initialize GD656Conquest integration: %s", e.getMessage());
            handler = new DummyConquestBattlefieldHandler();
        }
    }

    public void tick() {
        handler.tick();
    }
}
