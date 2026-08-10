package org.mods.gd656killicon.server.logic.integration;

import net.minecraft.world.entity.LivingEntity;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.logic.conquest.DummyConquestSpottingHandler;
import org.mods.gd656killicon.server.logic.spotting.ISpottingHandler;
import org.mods.gd656killicon.server.util.ServerLog;

/**
 * GD656Conquest 索敌接入(标示助攻): 模组加载时实例化 ConquestSpottingEventHandler。
 */
public class ConquestSpottingIntegration {
    private static final ConquestSpottingIntegration INSTANCE = new ConquestSpottingIntegration();
    private ISpottingHandler handler;
    private boolean initialized = false;

    private ConquestSpottingIntegration() {
        this.handler = new DummyConquestSpottingHandler();
    }

    public static ConquestSpottingIntegration get() {
        return INSTANCE;
    }

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            if (ServerBridge.loader().isModLoaded("gd656conquest")) {
                Class<?> handlerClass = Class.forName("org.mods.gd656killicon.server.logic.conquest.ConquestSpottingEventHandler");
                handler = (ISpottingHandler) handlerClass.getDeclaredConstructor().newInstance();
                handler.init();
                ServerLog.info("GD656Conquest marker spotting integration detected.");
            } else {
                handler = new DummyConquestSpottingHandler();
            }
        } catch (Exception e) {
            ServerLog.error("Failed to initialize GD656Conquest marker spotting integration: %s", e.getMessage());
            handler = new DummyConquestSpottingHandler();
        }
    }

    public void tick() {
        handler.tick();
    }

    public void onLivingDeath(LivingEntity victim, LivingEntity killer) {
        handler.onLivingDeath(victim, killer);
    }
}
