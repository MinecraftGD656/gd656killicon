package org.mods.gd656killicon.server.logic.integration;

import org.mods.gd656killicon.server.logic.immersiveaircraft.DummyImmersiveAircraftHandler;
import org.mods.gd656killicon.server.logic.immersiveaircraft.IImmersiveAircraftHandler;

public class ImmersiveAircraftIntegration {
    private static final ImmersiveAircraftIntegration INSTANCE = new ImmersiveAircraftIntegration();
    private final IImmersiveAircraftHandler handler = new DummyImmersiveAircraftHandler();
    private boolean initialized = false;

    private ImmersiveAircraftIntegration() {}

    public static ImmersiveAircraftIntegration get() {
        return INSTANCE;
    }

    /**
     * Initializes the Immersive Aircraft integration.
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
