package org.mods.gd656killicon.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.gui.MainConfigScreen;
import org.mods.gd656killicon.client.stats.ClientStatsManager;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID, value = Dist.CLIENT)
public class ClientEvents {
    // Client-side game events (TickEvent.ClientTickEvent, RenderGuiOverlayEvent, etc.)

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ElementConfigManager.tryApplyLocalization();
            while (KeyBindings.OPEN_CONFIG.consumeClick()) {
                Minecraft.getInstance().setScreen(new MainConfigScreen(Minecraft.getInstance().screen));
            }
        }
    }
}
