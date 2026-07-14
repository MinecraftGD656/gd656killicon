package org.mods.gd656killicon.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.ClientSetup;
import org.mods.gd656killicon.client.KeyBindings;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeClientLifecycleEvents {
    private ForgeClientLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::initializeClient);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_CONFIG);
        event.register(KeyBindings.OPEN_SCOREBOARD);
    }
}
