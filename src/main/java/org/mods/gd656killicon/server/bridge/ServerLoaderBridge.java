package org.mods.gd656killicon.server.bridge;

import net.minecraft.server.MinecraftServer;
import java.util.function.Consumer;

public interface ServerLoaderBridge {
    boolean isModLoaded(String modId);
    MinecraftServer getCurrentServer();
    void registerForgeEventBusSubscriber(Object subscriber);
    boolean registerForgeDynamicListener(String eventClassName, Consumer<Object> handler);
}
