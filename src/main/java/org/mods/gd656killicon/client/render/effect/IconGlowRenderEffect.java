package org.mods.gd656killicon.client.render.effect;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class IconGlowRenderEffect {
    private static final float[][] OFFSETS = new float[][]{
        {-1.0f, -1.0f},
        {0.0f, -1.0f},
        {1.0f, -1.0f},
        {-1.0f, 0.0f},
        {1.0f, 0.0f},
        {-1.0f, 1.0f},
        {0.0f, 1.0f},
        {1.0f, 1.0f}
    };

    private IconGlowRenderEffect() {
    }

    public static boolean isEnabled(JsonObject config) {
        return config != null && config.has("enable_icon_glow") && config.get("enable_icon_glow").getAsBoolean();
    }

    public static int resolveColor(JsonObject config) {
        return parseColor(config, "color_icon_glow", 0xFFFFFF);
    }

    public static float resolveIntensity(JsonObject config) {
        if (config == null || !config.has("icon_glow_intensity")) {
            return 0.45f;
        }
        return Mth.clamp(config.get("icon_glow_intensity").getAsFloat(), 0.0f, 1.0f);
    }

    public static float resolveSize(JsonObject config) {
        if (config == null || !config.has("icon_glow_size")) {
            return 4.0f;
        }
        return Math.max(0.0f, config.get("icon_glow_size").getAsFloat());
    }

    public static void drawGlowFrame(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        int x,
        int y,
        int width,
        int height,
        int u,
        int v,
        int frameWidth,
        int frameHeight,
        int totalWidth,
        int totalHeight,
        float alpha,
        int rgb,
        float intensity,
        float size
    ) {
        if (texture == null || alpha <= 0.001f || intensity <= 0.001f || size <= 0.01f || width <= 0 || height <= 0) {
            return;
        }
        float glowAlpha = Mth.clamp(alpha * intensity, 0.0f, 1.0f);
        float outerSpread = Math.max(0.5f, size);
        float innerSpread = outerSpread * 0.55f;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
            com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        for (float[] offset : OFFSETS) {
            RenderSystem.setShaderColor(r, g, b, glowAlpha * 0.16f);
            guiGraphics.blit(texture, Math.round(x + offset[0] * outerSpread), Math.round(y + offset[1] * outerSpread), width, height, u, v, frameWidth, frameHeight, totalWidth, totalHeight);
        }
        for (float[] offset : OFFSETS) {
            RenderSystem.setShaderColor(r, g, b, glowAlpha * 0.11f);
            guiGraphics.blit(texture, Math.round(x + offset[0] * innerSpread), Math.round(y + offset[1] * innerSpread), width, height, u, v, frameWidth, frameHeight, totalWidth, totalHeight);
        }
        RenderSystem.setShaderColor(r, g, b, glowAlpha * 0.09f);
        guiGraphics.blit(texture, x, y, width, height, u, v, frameWidth, frameHeight, totalWidth, totalHeight);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static int parseColor(JsonObject config, String key, int fallback) {
        if (config == null || !config.has(key)) {
            return fallback;
        }
        String hex = config.get(key).getAsString();
        if (hex == null || hex.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(hex.replace("#", ""), 16) & 0x00FFFFFF;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
