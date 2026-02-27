package org.mods.gd656killicon.client.textures;

import com.google.gson.JsonObject;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.mods.gd656killicon.client.textures.ExternalTextureManager.TextureDimensions;

public class IconTextureAnimationManager {
    private static final RandomSource RANDOM = RandomSource.create();

    public static String resolveTexturePath(String path) {
        return path;
    }

    public static class TextureFrame {
        public final int u;
        public final int v;
        public final int width;
        public final int height;
        public final int totalWidth;
        public final int totalHeight;

        public TextureFrame(int u, int v, int width, int height, int totalWidth, int totalHeight) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
            this.totalWidth = totalWidth;
            this.totalHeight = totalHeight;
        }
    }

    public static TextureFrame getTextureFrame(String presetId, String elementId, String textureKey, String textureFileName, long startTime, JsonObject config) {
        TextureDimensions dims = ExternalTextureManager.getTextureDimensions(presetId, textureFileName);
        int totalW = dims.width > 0 ? dims.width : 64; 
        int totalH = dims.height > 0 ? dims.height : 64;

        String prefix = "anim_" + textureKey + "_";
        
        boolean enabled = getBoolean(config, prefix + "enable_texture_animation", false);
        
        if (!enabled) {
             return new TextureFrame(0, 0, totalW, totalH, totalW, totalH);
        }
        
        int totalFrames = Math.max(1, getInt(config, prefix + "texture_animation_total_frames", 1));
        String orientation = getString(config, prefix + "texture_animation_orientation", "vertical");
        int interval = Math.max(1, getInt(config, prefix + "texture_animation_interval_ms", 100));
        boolean loop = getBoolean(config, prefix + "texture_animation_loop", false);
        String style = getString(config, prefix + "texture_animation_play_style", "sequential");
        
        
        int frameW, frameH;
        if ("horizontal".equalsIgnoreCase(orientation)) {
            frameW = totalW / totalFrames;
            frameH = totalH;
        } else { 
            frameW = totalW;
            frameH = totalH / totalFrames;
        }
        
        
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed < 0) elapsed = 0;
        int frameIndex = 0;
        
        long currentTick = elapsed / interval;
        
        if ("random".equalsIgnoreCase(style)) {
             long seed = startTime ^ (currentTick * 0x9E3779B97F4A7C15L);
             seed = seed * 31 + (textureKey != null ? textureKey.hashCode() : 0);
             seed = seed * 31 + (elementId != null ? elementId.hashCode() : 0);
             RandomSource random = RandomSource.create(seed);
             frameIndex = random.nextInt(totalFrames);
        } else if ("reverse".equalsIgnoreCase(style)) {
             if (loop) {
                 frameIndex = totalFrames - 1 - (int)(currentTick % totalFrames);
             } else {
                 frameIndex = totalFrames - 1 - (int)Math.min(currentTick, totalFrames - 1);
             }
        } else if ("pingpong".equalsIgnoreCase(style) || "ping-pong".equalsIgnoreCase(style) || "ping_pong".equalsIgnoreCase(style) || "round_trip".equalsIgnoreCase(style)) {
             if (totalFrames > 1) {
                 int cycleLen = (totalFrames - 1) * 2;
                 if (loop) {
                     int cyclePos = (int)(currentTick % cycleLen);
                     if (cyclePos < totalFrames) {
                         frameIndex = cyclePos;
                     } else {
                         frameIndex = cycleLen - cyclePos;
                     }
                 } else {
                     
                     long effectiveTick = Math.min(currentTick, cycleLen);
                     int cyclePos = (int) effectiveTick;
                     if (cyclePos < totalFrames) {
                         frameIndex = cyclePos;
                     } else {
                         frameIndex = cycleLen - cyclePos;
                     }
                 }
             } else {
                 frameIndex = 0;
             }
        } else { 
             if (loop) {
                 frameIndex = (int)(currentTick % totalFrames);
             } else {
                 frameIndex = (int)Math.min(currentTick, totalFrames - 1);
             }
        }
        
        
        frameIndex = Mth.clamp(frameIndex, 0, totalFrames - 1);
        
        
        int u = 0, v = 0;
        if ("horizontal".equalsIgnoreCase(orientation)) {
            u = frameIndex * frameW;
        } else {
            v = frameIndex * frameH;
        }
        
        return new TextureFrame(u, v, frameW, frameH, totalW, totalH);
    }
    
    private static boolean getBoolean(JsonObject config, String key, boolean def) {
        if (config == null || !config.has(key)) return def;
        return config.get(key).getAsBoolean();
    }
    
    private static int getInt(JsonObject config, String key, int def) {
        if (config == null || !config.has(key)) return def;
        return config.get(key).getAsInt();
    }
    
    private static String getString(JsonObject config, String key, String def) {
        if (config == null || !config.has(key)) return def;
        return config.get(key).getAsString();
    }
}
