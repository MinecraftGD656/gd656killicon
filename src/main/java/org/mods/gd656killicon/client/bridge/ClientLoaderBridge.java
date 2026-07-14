package org.mods.gd656killicon.client.bridge;

import java.nio.file.Path;

public interface ClientLoaderBridge {
    Path getConfigDir();
    boolean isModLoaded(String modId);
    void registerConfigScreen();
    void runOnClient(Runnable task);
    void registerForgeEventBusSubscriber(Object subscriber);
}
