package org.mods.gd656killicon.client.bridge;

import org.mods.gd656killicon.fabric.client.FabricClientLoaderBridge;

public final class ClientBridge {
    private static final ClientLoaderBridge LOADER = new FabricClientLoaderBridge();

    private ClientBridge() {
    }

    public static ClientLoaderBridge loader() {
        return LOADER;
    }
}
