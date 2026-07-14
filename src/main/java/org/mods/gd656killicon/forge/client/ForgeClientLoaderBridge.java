package org.mods.gd656killicon.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.mods.gd656killicon.client.bridge.ClientLoaderBridge;
import org.mods.gd656killicon.client.gui.MainConfigScreen;

import java.nio.file.Path;

public class ForgeClientLoaderBridge implements ClientLoaderBridge {
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    @SuppressWarnings("removal")
    public void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new MainConfigScreen(screen))
        );
    }

    @Override
    public void runOnClient(Runnable task) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> task.run());
    }

    @Override
    public void registerForgeEventBusSubscriber(Object subscriber) {
        MinecraftForge.EVENT_BUS.register(subscriber);
    }
}
