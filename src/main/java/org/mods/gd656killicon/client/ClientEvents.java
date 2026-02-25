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

    @SubscribeEvent
    public static void onClientPlayerLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        // Reset on logout if configured to "logout" or just always?
        // User said "退出游戏时" (On Game Exit).
        // I should check config inside renderer? 
        // Renderer config is loaded on trigger. If I haven't triggered, config might be stale.
        // But if I haven't triggered, combo is 0 anyway.
        // Wait, "never" option means it persists? Across servers? No, "LoggingOut" clears everything usually.
        // "Never" probably means "Never reset while in game".
        // Logout should probably always reset for safety, or check config.
        // The user option "reset_logout" implies it resets specifically on logout.
        // If "never" is selected, does it persist across logout?
        // Unlikely to persist across memory clear.
        // So "reset_logout" is redundant if logout always resets?
        // Maybe "Never" means "don't reset on death or time", but logout is inevitable?
        // Or maybe "Never" means "Save to disk"? No, "persist across sessions" is usually explicit.
        // I'll assume "logout" option means "Reset when logging out". 
        // If "Never" is selected, it might still reset on logout because memory is gone.
        // Unless I save it. But I don't have persistence implemented.
        // I'll implement checking config in renderer.
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
