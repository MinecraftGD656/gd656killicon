package org.mods.gd656killicon.fabric.client;

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.mods.gd656killicon.client.ClientSetup;
import org.mods.gd656killicon.client.KeyBindings;
import org.mods.gd656killicon.fabric.events.FabricClientEvents;

public class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMappingRegistry.register(KeyBindings.OPEN_CONFIG);
        KeyMappingRegistry.register(KeyBindings.OPEN_SCOREBOARD);
        FabricClientEvents.init();
        // 纹理/渲染资源初始化需要 GL 上下文就绪(客户端入口点在 Minecraft 构造器内过早触发),
        // 延迟到客户端启动完成事件(Forge FMLClientSetupEvent 的 Fabric 等价点)。
        ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> ClientSetup.initializeClient());
    }
}
