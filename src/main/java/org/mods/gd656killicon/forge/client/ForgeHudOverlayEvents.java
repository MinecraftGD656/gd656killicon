package org.mods.gd656killicon.forge.client;

import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.client.render.HudElementManager;

public final class ForgeHudOverlayEvents {
    private ForgeHudOverlayEvents() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        String overlayPath = event.getOverlay().id().getPath();
        boolean isMainOverlayPass = "player_list".equals(overlayPath);
        HudElementManager.onRenderGuiOverlay(event.getGuiGraphics(), event.getPartialTick(), isMainOverlayPass);
    }
}
