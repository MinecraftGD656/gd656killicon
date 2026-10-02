package org.mods.gd656killicon.server.logic.integration;

import org.mods.gd656killicon.server.logic.pingwheel.DummyPingWheelHandler;
import org.mods.gd656killicon.server.logic.pingwheel.IPingWheelHandler;

public class PingWheelIntegration {
    private static final PingWheelIntegration INSTANCE = new PingWheelIntegration();
    private final IPingWheelHandler handler = new DummyPingWheelHandler();
    private boolean initialized = false;

    private PingWheelIntegration() {}

    public static PingWheelIntegration get() {
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
}
