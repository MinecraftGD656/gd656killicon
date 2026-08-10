package org.mods.gd656killicon.server.logic.integration;

import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.util.ServerLog;

/**
 * GD656Conquest 救援接入: 模组加载时实例化 ConquestRescueEventHandler。
 */
public class ConquestRescueIntegration {
    private static final ConquestRescueIntegration INSTANCE = new ConquestRescueIntegration();
    private Object handler;
    private boolean initialized = false;

    private ConquestRescueIntegration() {
    }

    public static ConquestRescueIntegration get() {
        return INSTANCE;
    }

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            if (ServerBridge.loader().isModLoaded("gd656conquest")) {
                Class<?> handlerClass = Class.forName("org.mods.gd656killicon.server.logic.conquest.ConquestRescueEventHandler");
                handler = handlerClass.getDeclaredConstructor().newInstance();
                handlerClass.getMethod("init").invoke(handler);
                ServerLog.info("GD656Conquest rescue integration detected.");
            }
        } catch (Exception e) {
            ServerLog.error("Failed to initialize GD656Conquest rescue integration: %s", e.getMessage());
        }
    }
}
