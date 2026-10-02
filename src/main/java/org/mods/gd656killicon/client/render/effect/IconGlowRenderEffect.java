package org.mods.gd656killicon.client.render.effect;


import net.minecraft.client.renderer.RenderPipelines;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
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
        GuiGraphicsExtractor guiGraphics,
        Identifier texture,
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

        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        int r8 = (int) (r * 255.0f); int g8 = (int) (g * 255.0f); int b8 = (int) (b * 255.0f);
        int aOuter = (int) (glowAlpha * 0.16f * 255.0f); if (aOuter < 0) aOuter = 0; if (aOuter > 255) aOuter = 255;
        int aInner = (int) (glowAlpha * 0.11f * 255.0f); if (aInner < 0) aInner = 0; if (aInner > 255) aInner = 255;
        int aCore = (int) (glowAlpha * 0.09f * 255.0f); if (aCore < 0) aCore = 0; if (aCore > 255) aCore = 255;
        int argbOuter = (aOuter << 24) | (r8 << 16) | (g8 << 8) | b8;
        int argbInner = (aInner << 24) | (r8 << 16) | (g8 << 8) | b8;
        int argbCore = (aCore << 24) | (r8 << 16) | (g8 << 8) | b8;
        for (float[] offset : OFFSETS) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, Math.round(x + offset[0] * outerSpread), Math.round(y + offset[1] * outerSpread), u / (float) totalWidth, v / (float) totalHeight, width, height, frameWidth, frameHeight, totalWidth, totalHeight, argbOuter);
        }
        for (float[] offset : OFFSETS) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, Math.round(x + offset[0] * innerSpread), Math.round(y + offset[1] * innerSpread), u / (float) totalWidth, v / (float) totalHeight, width, height, frameWidth, frameHeight, totalWidth, totalHeight, argbInner);
        }
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u / (float) totalWidth, v / (float) totalHeight, width, height, frameWidth, frameHeight, totalWidth, totalHeight, argbCore);
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
