package org.mods.gd656killicon.server.logic.integration;

import net.minecraftforge.fml.ModList;
import org.mods.gd656killicon.server.logic.pingwheel.DummyPingWheelHandler;
import org.mods.gd656killicon.server.logic.pingwheel.IPingWheelHandler;
import org.mods.gd656killicon.server.util.ServerLog;

public class PingWheelIntegration {
    private static final PingWheelIntegration INSTANCE = new PingWheelIntegration();
    private IPingWheelHandler handler;
    private boolean initialized = false;

    private PingWheelIntegration() {
        this.handler = new DummyPingWheelHandler();
    }

    public static PingWheelIntegration get() {
        return INSTANCE;
    }

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            if (ModList.get().isLoaded("pingwheel")) {
                Class<?> handlerClass = Class.forName("org.mods.gd656killicon.server.logic.pingwheel.PingWheelEventHandler");
                handler = (IPingWheelHandler) handlerClass.getDeclaredConstructor().newInstance();
                handler.init();
                ServerLog.info("Ping Wheel mod detected.");
            } else {
                handler = new DummyPingWheelHandler();
            }
        } catch (Exception e) {
            ServerLog.error("Failed to initialize Ping Wheel integration: %s", e.getMessage());
            handler = new DummyPingWheelHandler();
        }
    }

    public void tick() {
        handler.tick();
    }
}
