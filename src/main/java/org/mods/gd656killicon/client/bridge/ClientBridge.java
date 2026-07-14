package org.mods.gd656killicon.client.bridge;

import org.mods.gd656killicon.forge.client.ForgeClientLoaderBridge;

public final class ClientBridge {
    private static final ClientLoaderBridge LOADER = new ForgeClientLoaderBridge();

    private ClientBridge() {
    }

    public static ClientLoaderBridge loader() {
        return LOADER;
    }
}
