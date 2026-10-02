package org.mods.gd656killicon.fabric.server;

import dev.architectury.platform.Platform;
import net.minecraft.server.MinecraftServer;
import org.mods.gd656killicon.server.bridge.ServerLoaderBridge;

import java.util.function.Consumer;

public class FabricServerLoaderBridge implements ServerLoaderBridge {
    private static MinecraftServer currentServer;

    public static void setCurrentServer(MinecraftServer server) {
        currentServer = server;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return Platform.isModLoaded(modId);
    }

    @Override
    public MinecraftServer getCurrentServer() {
        return currentServer;
    }

    @Override
    public void registerForgeEventBusSubscriber(Object subscriber) {
        // Fabric 下无 Forge 事件总线, 业务 handler 改为显式注册 Architectury/Fabric 事件
    }

    @Override
    public boolean registerForgeDynamicListener(String eventClassName, Consumer<Object> handler) {
        return false;
    }
}
