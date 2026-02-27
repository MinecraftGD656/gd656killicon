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
import org.mods.gd656killicon.network.packet.KillIconPacket;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static boolean wasInGame = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ElementConfigManager.tryApplyLocalization();
            while (KeyBindings.OPEN_CONFIG.consumeClick()) {
                Minecraft.getInstance().setScreen(new MainConfigScreen(Minecraft.getInstance().screen));
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                if (!wasInGame) {
                    if (org.mods.gd656killicon.client.config.ClientConfigManager.isEnableAceLag()) {
                        org.mods.gd656killicon.client.render.HudElementManager.trigger("global", "ace_logo", org.mods.gd656killicon.client.render.IHudRenderer.TriggerContext.of(0, -1));
                    }
                    wasInGame = true;
                }
            } else {
                wasInGame = false;
            }
            KillIconPacket.processPendingTriggers();
        }
    }


    @SubscribeEvent
    public static void onClientPlayerLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        org.mods.gd656killicon.client.render.impl.ComboSubtitleRenderer.getInstance().onPlayerLogout();
    }

    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && event.getEntity() == mc.player) {
            org.mods.gd656killicon.client.render.impl.ComboSubtitleRenderer.getInstance().onPlayerDeath();
        }
    }
}
