package org.mods.gd656killicon.server.bridge;

import org.mods.gd656killicon.forge.server.ForgeServerLoaderBridge;

public final class ServerBridge {
    private static final ServerLoaderBridge LOADER = new ForgeServerLoaderBridge();

    private ServerBridge() {
    }

    public static ServerLoaderBridge loader() {
        return LOADER;
    }
}
