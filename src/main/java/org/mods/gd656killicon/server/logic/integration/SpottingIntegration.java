package org.mods.gd656killicon.server.logic.integration;

import net.minecraft.world.entity.LivingEntity;
import org.mods.gd656killicon.server.logic.spotting.DummySpottingHandler;
import org.mods.gd656killicon.server.logic.spotting.ISpottingHandler;

public class SpottingIntegration {
    private static final SpottingIntegration INSTANCE = new SpottingIntegration();
    private final ISpottingHandler handler = new DummySpottingHandler();
    private boolean initialized = false;

    private SpottingIntegration() {}

    public static SpottingIntegration get() {
        return INSTANCE;
    }

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

    public void onLivingDeath(LivingEntity victim, LivingEntity killer) {
        handler.onLivingDeath(victim, killer);
    }
}
