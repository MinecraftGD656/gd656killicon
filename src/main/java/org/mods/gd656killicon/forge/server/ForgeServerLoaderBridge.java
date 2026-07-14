package org.mods.gd656killicon.forge.server;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.mods.gd656killicon.server.bridge.ServerLoaderBridge;

import java.util.function.Consumer;

public class ForgeServerLoaderBridge implements ServerLoaderBridge {
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public MinecraftServer getCurrentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public void registerForgeEventBusSubscriber(Object subscriber) {
        MinecraftForge.EVENT_BUS.register(subscriber);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean registerForgeDynamicListener(String eventClassName, Consumer<Object> handler) {
        try {
            Class<?> eventClass = Class.forName(eventClassName);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, (Class) eventClass, (Consumer) handler);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
