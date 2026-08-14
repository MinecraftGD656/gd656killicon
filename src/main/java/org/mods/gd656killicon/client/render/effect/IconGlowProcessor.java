package org.mods.gd656killicon.client.render.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class IconGlowProcessor {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private IconGlowProcessor() {
    }

    public static void clearCache() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            CACHE.clear();
            return;
        }
        for (ResourceLocation texture : CACHE.values()) {
            minecraft.getTextureManager().release(texture);
        }
        CACHE.clear();
    }

    public static ResourceLocation getOrCreateProcessedTexture(String presetId, String texturePath, boolean accentTintEnabled, int accentColor, float brightness, float contrast) {
        String cacheKey = String.format(
            Locale.ROOT,
            "%s:%s:%b:%06X:%.3f:%.3f",
            presetId,
            texturePath,
            accentTintEnabled,
            accentColor & 0xFFFFFF,
            brightness,
            contrast
        );
        ResourceLocation cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            byte[] bytes = ExternalTextureManager.readTextureBytes(presetId, texturePath);
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) {
                return null;
            }
            BufferedImage processed = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getRGB(x, y);
                    processed.setRGB(x, y, processTexturePixel(argb, accentTintEnabled, accentColor, brightness, contrast));
                }
            }
            ResourceLocation processedTexture = registerBufferedTexture("processed", cacheKey, processed);
            CACHE.put(cacheKey, processedTexture);
            return processedTexture;
        } catch (IOException ignored) {
            return null;
        }
    }

    public static int applyBrightnessContrastToRgb(int rgb, float brightness, float contrast) {
        int red = applyBrightnessContrastToChannel((rgb >> 16) & 0xFF, brightness, contrast);
        int green = applyBrightnessContrastToChannel((rgb >> 8) & 0xFF, brightness, contrast);
        int blue = applyBrightnessContrastToChannel(rgb & 0xFF, brightness, contrast);
        return (red << 16) | (green << 8) | blue;
    }

    private static ResourceLocation registerBufferedTexture(String prefix, String cacheKey, BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        DynamicTexture texture = new DynamicTexture(com.mojang.blaze3d.platform.NativeImage.read(new ByteArrayInputStream(output.toByteArray())));
        String dynamicName = "gd656killicon_" + prefix + "_" + Integer.toHexString(cacheKey.hashCode());
        return Minecraft.getInstance().getTextureManager().register(dynamicName, texture);
    }

    private static int processTexturePixel(int argb, boolean accentTintEnabled, int accentColor, float brightness, float contrast) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha <= 0) {
            return 0;
        }

        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;

        if (accentTintEnabled && isAccentPixel(argb)) {
            float luminance = Math.max(red, Math.max(green, blue)) / 255.0f;
            red = Math.round(((accentColor >> 16) & 0xFF) * luminance);
            green = Math.round(((accentColor >> 8) & 0xFF) * luminance);
            blue = Math.round((accentColor & 0xFF) * luminance);
        }

        red = applyBrightnessContrastToChannel(red, brightness, contrast);
        green = applyBrightnessContrastToChannel(green, brightness, contrast);
        blue = applyBrightnessContrastToChannel(blue, brightness, contrast);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static boolean isAccentPixel(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha < 16) {
            return false;
        }
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
        float saturation = hsb[1];
        float brightness = hsb[2];
        return brightness >= 0.18f && brightness <= 0.98f && saturation >= 0.18f;
    }

    private static int applyBrightnessContrastToChannel(int channel, float brightness, float contrast) {
        float normalized = channel / 255.0f;
        float contrasted = ((normalized - 0.5f) * contrast) + 0.5f;
        float adjusted = contrasted * brightness;
        return Mth.clamp(Math.round(adjusted * 255.0f), 0, 255);
    }
}
