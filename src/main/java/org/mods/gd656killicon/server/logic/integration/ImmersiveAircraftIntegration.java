package org.mods.gd656killicon.server.logic.integration;

import net.minecraftforge.fml.ModList;
import org.mods.gd656killicon.server.logic.immersiveaircraft.DummyImmersiveAircraftHandler;
import org.mods.gd656killicon.server.logic.immersiveaircraft.IImmersiveAircraftHandler;
import org.mods.gd656killicon.server.util.ServerLog;

public class ImmersiveAircraftIntegration {
    private static final ImmersiveAircraftIntegration INSTANCE = new ImmersiveAircraftIntegration();
    private IImmersiveAircraftHandler handler;
    private boolean initialized = false;

    private ImmersiveAircraftIntegration() {
        this.handler = new DummyImmersiveAircraftHandler();
    }

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
        try {
            if (ModList.get().isLoaded("immersive_aircraft")) {
                Class<?> handlerClass = Class.forName("org.mods.gd656killicon.server.logic.immersiveaircraft.ImmersiveAircraftEventHandler");
                handler = (IImmersiveAircraftHandler) handlerClass.getDeclaredConstructor().newInstance();
                handler.init();
                ServerLog.info("Immersive Aircraft mod detected.");
            } else {
                handler = new DummyImmersiveAircraftHandler();
            }
        } catch (Exception e) {
            ServerLog.error("Failed to initialize Immersive Aircraft integration: %s", e.getMessage());
            handler = new DummyImmersiveAircraftHandler();
        }
    }

    public void tick() {
        handler.tick();
    }
}
