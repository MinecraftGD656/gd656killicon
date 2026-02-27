package org.mods.gd656killicon.server.logic.integration;

import net.minecraftforge.fml.ModList;
import org.mods.gd656killicon.server.logic.ywzj.DummyYwzjVehicleHandler;
import org.mods.gd656killicon.server.logic.ywzj.IYwzjVehicleHandler;
import org.mods.gd656killicon.server.util.ServerLog;

public class YwzjVehicleIntegration {
    private static final YwzjVehicleIntegration INSTANCE = new YwzjVehicleIntegration();
    private IYwzjVehicleHandler handler;
    private boolean initialized = false;

    private YwzjVehicleIntegration() {
        this.handler = new DummyYwzjVehicleHandler();
    }

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
        try {
            if (ModList.get().isLoaded("ywzj_vehicle")) {
                Class<?> handlerClass = Class.forName("org.mods.gd656killicon.server.logic.ywzj.YwzjVehicleEventHandler");
                handler = (IYwzjVehicleHandler) handlerClass.getDeclaredConstructor().newInstance();
                handler.init();
                ServerLog.info("YWZJ Vehicle mod detected.");
            } else {
                handler = new DummyYwzjVehicleHandler();
            }
        } catch (Exception e) {
            ServerLog.error("Failed to initialize YWZJ Vehicle integration: %s", e.getMessage());
            handler = new DummyYwzjVehicleHandler();
        }
    }

    public void tick() {
        handler.tick();
    }
}
