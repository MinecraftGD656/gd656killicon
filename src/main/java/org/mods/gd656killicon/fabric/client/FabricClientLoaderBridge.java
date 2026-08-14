package org.mods.gd656killicon.fabric.client;

import dev.architectury.platform.Platform;
import org.mods.gd656killicon.client.bridge.ClientLoaderBridge;

import java.nio.file.Path;

public class FabricClientLoaderBridge implements ClientLoaderBridge {
    @Override
    public Path getConfigDir() {
        return Platform.getConfigFolder();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return Platform.isModLoaded(modId);
    }

    @Override
    public void registerConfigScreen() {
        // 配置界面由 FabricModMenuIntegration 通过 ModMenu 提供
    }

    @Override
    public void runOnClient(Runnable task) {
        task.run();
    }

    @Override
    public void registerForgeEventBusSubscriber(Object subscriber) {
        // Fabric 下无 Forge 事件总线, 业务 handler 改为显式注册 Architectury/Fabric 事件
    }
}
