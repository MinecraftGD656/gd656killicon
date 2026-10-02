package org.mods.gd656killicon.server.logic.integration;

import org.mods.gd656killicon.server.logic.ywzj.DummyYwzjVehicleHandler;
import org.mods.gd656killicon.server.logic.ywzj.IYwzjVehicleHandler;

public class YwzjVehicleIntegration {
    private static final YwzjVehicleIntegration INSTANCE = new YwzjVehicleIntegration();
    private final IYwzjVehicleHandler handler = new DummyYwzjVehicleHandler();
    private boolean initialized = false;

    private YwzjVehicleIntegration() {}

    public static YwzjVehicleIntegration get() {
        return INSTANCE;
    }

    /**
     * Initializes the YWZJ Vehicle integration.
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
}
