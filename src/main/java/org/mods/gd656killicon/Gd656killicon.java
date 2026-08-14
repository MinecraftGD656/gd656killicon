package org.mods.gd656killicon;

import org.mods.gd656killicon.network.NetworkHandler;

public class Gd656killicon {
    public static final String MODID = "gd656killicon";

    public static void bootstrap() {
        NetworkHandler.register();
    }
}
