package org.mods.gd656killicon.server.bridge;

import org.mods.gd656killicon.fabric.server.FabricServerLoaderBridge;

public final class ServerBridge {
    private static final ServerLoaderBridge LOADER = new FabricServerLoaderBridge();

    private ServerBridge() {
    }

    public static ServerLoaderBridge loader() {
        return LOADER;
    }
}
