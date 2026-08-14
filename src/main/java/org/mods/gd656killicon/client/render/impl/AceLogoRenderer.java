package org.mods.gd656killicon.client.render.impl;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;

public class AceLogoRenderer implements IHudRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "icon/ace_logo.png");
    private static final long DISPLAY_DURATION_MS = 10000L;
    private static final long FADE_OUT_DURATION_MS = 600L;
    private static final int LOGO_WIDTH = 74;
    private static final int LOGO_HEIGHT = 25;

    private long startTime = -1L;
    private boolean isVisible = false;

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        if (!isVisible || startTime < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        if (elapsed >= DISPLAY_DURATION_MS + FADE_OUT_DURATION_MS) {
            isVisible = false;
            startTime = -1L;
            return;
        }
        float alpha = 1.0f;
        if (elapsed > DISPLAY_DURATION_MS) {
            float t = (float) (elapsed - DISPLAY_DURATION_MS) / (float) FADE_OUT_DURATION_MS;
            t = Mth.clamp(t, 0.0f, 1.0f);
            float fade = 1.0f - t;
            alpha = fade * fade;
        }
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int drawX = screenWidth - LOGO_WIDTH;
        int drawY = screenHeight - LOGO_HEIGHT;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        guiGraphics.blit(TEXTURE, drawX, drawY, 0, 0, LOGO_WIDTH, LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    @Override
    public void trigger(TriggerContext context) {
        if (!ClientConfigManager.isEnableAceLag()) {
            return;
        }
        startTime = System.currentTimeMillis();
        isVisible = true;
    }
}
