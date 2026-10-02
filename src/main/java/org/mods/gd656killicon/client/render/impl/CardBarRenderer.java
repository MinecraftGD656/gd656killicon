package org.mods.gd656killicon.client.render.impl;




import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.RenderPipelines;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.Team;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.gui.tabs.PreviewTextureFocusContext;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.PreviewRenderTimeContext;
import org.mods.gd656killicon.client.render.effect.IconGlowRenderEffect;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;

import org.mods.gd656killicon.client.render.effect.IconRingEffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

import org.mods.gd656killicon.client.render.util.GDMesh;
import org.joml.Matrix3x2fStack;
public class CardBarRenderer implements IHudRenderer {
    
    private static final Map<Identifier, Float> ASPECT_RATIO_CACHE = new ConcurrentHashMap<>();
    private static final float DEFAULT_ASPECT_RATIO = 1.0f;
    private static final int BASE_LOGICAL_HEIGHT = 40;     
    private static final float CENTER_FLASH_SPEED_MULTIPLIER = 3.0f;
    private long flashStartTime = -1;
    private final WaveEffectSystem waveSystem = new WaveEffectSystem();
    private final IconRingEffect ringEffect = new IconRingEffect();
    private JsonObject currentConfig;
    {
        ringEffect.setScale(1.0f);
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, float partialTick) {
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "card_bar");

        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) {
            return;
        }
        this.currentConfig = config;

        float scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        int xOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
        int yOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 0;
        String screenAnchor = config.has("screen_anchor") ? config.get("screen_anchor").getAsString() : "bottom_center";
        String team = config.has("team") ? config.get("team").getAsString() : "ct";
        boolean dynamicCardStyle = config.has("dynamic_card_style") && config.get("dynamic_card_style").getAsBoolean();
        float animationDuration = config.has("animation_duration") ? config.get("animation_duration").getAsFloat() : 0.2f;
        
        Minecraft mc = Minecraft.getInstance();

        if (dynamicCardStyle && mc.player != null) {
            Team pt = mc.player.getTeam();
            if (pt != null) {
                ChatFormatting color = pt.getColor();
                if (color == ChatFormatting.BLUE || color == ChatFormatting.AQUA 
                        || color == ChatFormatting.DARK_AQUA || color == ChatFormatting.DARK_BLUE) {
                    team = "ct";
                } else if (color == ChatFormatting.YELLOW || color == ChatFormatting.GOLD) {
                    team = "t";
                }
            }
        }
        
        boolean showLight = config.has("show_light") && config.get("show_light").getAsBoolean();
        float lightWidth = config.has("light_width") ? config.get("light_width").getAsFloat() : 300.0f;
        float lightHeight = config.has("light_height") ? config.get("light_height").getAsFloat() : 20.0f;
        String lightColorCt = config.has("color_light_ct") ? config.get("color_light_ct").getAsString() : "9cc1eb";
        String lightColorT = config.has("color_light_t") ? config.get("color_light_t").getAsString() : "d9ac5b";
        boolean enableIconGlow = IconGlowRenderEffect.isEnabled(config);
        int iconGlowColor = IconGlowRenderEffect.resolveColor(config);
        float iconGlowIntensity = IconGlowRenderEffect.resolveIntensity(config);
        float iconGlowSize = IconGlowRenderEffect.resolveSize(config);

        boolean isT = "t".equalsIgnoreCase(team);
        String textureKey = isT ? "bar_t" : "bar_ct";
        float focusMultiplier = PreviewTextureFocusContext.alphaMultiplier("kill_icon/card_bar", textureKey);
        String textureName = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/card_bar",
            textureKey,
            config
        );
        
        Identifier texture = ExternalTextureManager.getTexture(textureName);
        if (texture == null) return;

        float frameWidthRatio = resolveFrameRatio(textureKey, "texture_frame_width_ratio");
        float frameHeightRatio = resolveFrameRatio(textureKey, "texture_frame_height_ratio");
        int drawHeight = Math.round(BASE_LOGICAL_HEIGHT * frameHeightRatio);
        int drawWidth = Math.round(BASE_LOGICAL_HEIGHT * frameWidthRatio);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float centerX = org.mods.gd656killicon.client.render.ScreenAnchor.resolveCenterX(screenAnchor, xOffset, screenWidth);
        float centerY = org.mods.gd656killicon.client.render.ScreenAnchor.resolveCenterY(screenAnchor, yOffset, screenHeight);
        renderInternal(guiGraphics, partialTick, centerX, centerY, scale, isT, texture, drawWidth, drawHeight, showLight, lightWidth, lightHeight, lightColorCt, lightColorT, animationDuration, enableIconGlow, iconGlowColor, iconGlowIntensity, iconGlowSize, focusMultiplier);
    }

    public void renderAt(GuiGraphicsExtractor guiGraphics, float partialTick, float centerX, float centerY) {
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "card_bar");

        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) {
            return;
        }
        this.currentConfig = config;

        float scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        String team = config.has("team") ? config.get("team").getAsString() : "ct";
        boolean dynamicCardStyle = config.has("dynamic_card_style") && config.get("dynamic_card_style").getAsBoolean();
        float animationDuration = config.has("animation_duration") ? config.get("animation_duration").getAsFloat() : 0.2f;

        Minecraft mc = Minecraft.getInstance();

        if (dynamicCardStyle && mc.player != null) {
            Team pt = mc.player.getTeam();
            if (pt != null) {
                ChatFormatting color = pt.getColor();
                if (color == ChatFormatting.BLUE || color == ChatFormatting.AQUA 
                        || color == ChatFormatting.DARK_AQUA || color == ChatFormatting.DARK_BLUE) {
                    team = "ct";
                } else if (color == ChatFormatting.YELLOW || color == ChatFormatting.GOLD) {
                    team = "t";
                }
            }
        }

        boolean showLight = config.has("show_light") && config.get("show_light").getAsBoolean();
        float lightWidth = config.has("light_width") ? config.get("light_width").getAsFloat() : 300.0f;
        float lightHeight = config.has("light_height") ? config.get("light_height").getAsFloat() : 20.0f;
        String lightColorCt = config.has("color_light_ct") ? config.get("color_light_ct").getAsString() : "9cc1eb";
        String lightColorT = config.has("color_light_t") ? config.get("color_light_t").getAsString() : "d9ac5b";
        boolean enableIconGlow = IconGlowRenderEffect.isEnabled(config);
        int iconGlowColor = IconGlowRenderEffect.resolveColor(config);
        float iconGlowIntensity = IconGlowRenderEffect.resolveIntensity(config);
        float iconGlowSize = IconGlowRenderEffect.resolveSize(config);

        String textureName = "killicon_card_bar_ct.png";
        boolean isT = "t".equalsIgnoreCase(team);
        if (isT) {
            textureName = "killicon_card_bar_t.png";
        }
        
        Identifier texture = ExternalTextureManager.getTexture(textureName);
        if (texture == null) return;

        String textureKey = isT ? "bar_t" : "bar_ct";
        float focusMultiplier = PreviewTextureFocusContext.alphaMultiplier("kill_icon/card_bar", textureKey);
        float frameWidthRatio = resolveFrameRatio(textureKey, "texture_frame_width_ratio");
        float frameHeightRatio = resolveFrameRatio(textureKey, "texture_frame_height_ratio");
        int drawHeight = Math.round(BASE_LOGICAL_HEIGHT * frameHeightRatio);
        int drawWidth = Math.round(BASE_LOGICAL_HEIGHT * frameWidthRatio);

        renderInternal(guiGraphics, partialTick, centerX, centerY, scale, isT, texture, drawWidth, drawHeight, showLight, lightWidth, lightHeight, lightColorCt, lightColorT, animationDuration, enableIconGlow, iconGlowColor, iconGlowIntensity, iconGlowSize, focusMultiplier);
    }

    public void renderPreviewAt(GuiGraphicsExtractor guiGraphics, float partialTick, float centerX, float centerY, JsonObject config) {
        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) {
            return;
        }
        this.currentConfig = config;

        float scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        String team = config.has("team") ? config.get("team").getAsString() : "ct";
        boolean dynamicCardStyle = config.has("dynamic_card_style") && config.get("dynamic_card_style").getAsBoolean();
        float animationDuration = config.has("animation_duration") ? config.get("animation_duration").getAsFloat() : 0.2f;

        Minecraft mc = Minecraft.getInstance();
        if (dynamicCardStyle && mc.player != null) {
            Team pt = mc.player.getTeam();
            if (pt != null) {
                ChatFormatting color = pt.getColor();
                if (color == ChatFormatting.BLUE || color == ChatFormatting.AQUA 
                        || color == ChatFormatting.DARK_AQUA || color == ChatFormatting.DARK_BLUE) {
                    team = "ct";
                } else if (color == ChatFormatting.YELLOW || color == ChatFormatting.GOLD) {
                    team = "t";
                }
            }
        }

        boolean showLight = config.has("show_light") && config.get("show_light").getAsBoolean();
        float lightWidth = config.has("light_width") ? config.get("light_width").getAsFloat() : 300.0f;
        float lightHeight = config.has("light_height") ? config.get("light_height").getAsFloat() : 20.0f;
        String lightColorCt = config.has("color_light_ct") ? config.get("color_light_ct").getAsString() : "9cc1eb";
        String lightColorT = config.has("color_light_t") ? config.get("color_light_t").getAsString() : "d9ac5b";
        boolean enableIconGlow = IconGlowRenderEffect.isEnabled(config);
        int iconGlowColor = IconGlowRenderEffect.resolveColor(config);
        float iconGlowIntensity = IconGlowRenderEffect.resolveIntensity(config);
        float iconGlowSize = IconGlowRenderEffect.resolveSize(config);

        String textureName = "killicon_card_bar_ct.png";
        boolean isT = "t".equalsIgnoreCase(team);
        if (isT) {
            textureName = "killicon_card_bar_t.png";
        }
        
        Identifier texture = ExternalTextureManager.getTexture(textureName);
        if (texture == null) return;

        String textureKey = isT ? "bar_t" : "bar_ct";
        float focusMultiplier = PreviewTextureFocusContext.alphaMultiplier("kill_icon/card_bar", textureKey);
        float frameWidthRatio = resolveFrameRatio(textureKey, "texture_frame_width_ratio");
        float frameHeightRatio = resolveFrameRatio(textureKey, "texture_frame_height_ratio");
        int drawHeight = Math.round(BASE_LOGICAL_HEIGHT * frameHeightRatio);
        int drawWidth = Math.round(BASE_LOGICAL_HEIGHT * frameWidthRatio);

        renderInternal(guiGraphics, partialTick, centerX, centerY, scale, isT, texture, drawWidth, drawHeight, showLight, lightWidth, lightHeight, lightColorCt, lightColorT, animationDuration, enableIconGlow, iconGlowColor, iconGlowIntensity, iconGlowSize, focusMultiplier);
    }

    private void renderInternal(GuiGraphicsExtractor guiGraphics, float partialTick, float centerX, float centerY, float scale, boolean isT, Identifier texture, int drawWidth, int drawHeight, boolean showLight, float lightWidth, float lightHeight, String lightColorCt, String lightColorT, float animationDuration, boolean enableIconGlow, int iconGlowColor, float iconGlowIntensity, float iconGlowSize, float focusMultiplier) {
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        
        poseStack.translate(centerX,  centerY);
        poseStack.scale(scale,  scale);
        
        
        float flashAlpha = 0.0f;
        long animDurMs = (long) (animationDuration * 1000);
        long flashAnimDurMs = Math.max(1L, (long)(animDurMs / CENTER_FLASH_SPEED_MULTIPLIER));
        
        if (flashStartTime != -1) {
            long elapsed = PreviewRenderTimeContext.currentTimeMillis() - flashStartTime;
            
            long flashHold = flashAnimDurMs / 2;
            long flashFade = flashAnimDurMs * 4;
            
            if (elapsed < flashHold) {
                flashAlpha = 1.0f;
            } else {
                long flashElapsed = elapsed - flashHold;
                if (flashElapsed < flashFade) {
                    flashAlpha = 1.0f - ((float) flashElapsed / flashFade);
                } else {
                    flashStartTime = -1;
                }
            }
        }

        if (showLight) {
            int baseColorInt = parseColor(isT ? lightColorT : lightColorCt);
            int br = (baseColorInt >> 16) & 0xFF;
            int bg = (baseColorInt >> 8) & 0xFF;
            int bb = baseColorInt & 0xFF;
            
            int mr = (int) (br + (255 - br) * flashAlpha);
            int mg = (int) (bg + (255 - bg) * flashAlpha);
            int mb = (int) (bb + (255 - bb) * flashAlpha);
            
            String mixedColorHex = String.format("%02X%02X%02X", mr, mg, mb);
            
            renderLightEffect(guiGraphics, lightWidth, lightHeight, mixedColorHex, 1.0f);
            
            ringEffect.render(guiGraphics, 0, 0, PreviewRenderTimeContext.currentTimeMillis());
        }
        
        if (showLight) {
             waveSystem.updateAndRender(guiGraphics, lightWidth, isT ? lightColorT : lightColorCt, animationDuration, flashAlpha);
        }

        if (enableIconGlow) {
            IconGlowRenderEffect.drawGlowFrame(
                guiGraphics,
                texture,
                -drawWidth / 2,
                -drawHeight / 2,
                drawWidth,
                drawHeight,
                0,
                0,
                drawWidth,
                drawHeight,
                drawWidth,
                drawHeight,
                focusMultiplier,
                iconGlowColor,
                iconGlowIntensity,
                iconGlowSize
            );
        }
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, -drawWidth / 2, -drawHeight / 2, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight, drawWidth, drawHeight);
        
        if (flashAlpha > 0.01f) {
            
            int aFlash1 = (int) (flashAlpha * focusMultiplier * 255.0f); if (aFlash1 < 0) aFlash1 = 0; if (aFlash1 > 255) aFlash1 = 255;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, -drawWidth / 2, -drawHeight / 2, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight, drawWidth, drawHeight, (aFlash1 << 24) | 0x00FFFFFF);
            
            if (flashAlpha > 0.5f) {
                int aFlash2 = (int) ((flashAlpha - 0.5f) * 2.0f * focusMultiplier * 255.0f); if (aFlash2 < 0) aFlash2 = 0; if (aFlash2 > 255) aFlash2 = 255;
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, -drawWidth / 2, -drawHeight / 2, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight, drawWidth, drawHeight, (aFlash2 << 24) | 0x00FFFFFF);
            }
            
        }
        
        poseStack.popMatrix();
    }
    
    /**
     * Retrieves the aspect ratio (width / height) of the texture.
     * Caches the result to minimize I/O operations.
     */
    private float getTextureAspectRatio(Identifier texture) {
        if (ASPECT_RATIO_CACHE.containsKey(texture)) {
            return ASPECT_RATIO_CACHE.get(texture);
        }

        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture).orElse(null);
            if (resource != null) {
                try (var inputStream = resource.open()) {
                    var image = ImageIO.read(inputStream);
                    if (image != null && image.getHeight() > 0) {
                        float ratio = (float) image.getWidth() / (float) image.getHeight();
                        ASPECT_RATIO_CACHE.put(texture, ratio);
                        return ratio;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        
        ASPECT_RATIO_CACHE.put(texture, DEFAULT_ASPECT_RATIO);
        return DEFAULT_ASPECT_RATIO;
    }

    private void renderLightEffect(GuiGraphicsExtractor guiGraphics, float width, float height, String colorHex, float alphaMultiplier) {
        int color = parseColor(colorHex);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        
        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        
        GDMesh.Builder buffer = GDMesh.begin();
        Matrix3x2fStack pose = guiGraphics.pose();
        
        
        int segments = 50;
        float coreRatio = 0.5f;         
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float x0 = (t0 - 0.5f) * 2.0f * halfWidth;
            float x1 = (t1 - 0.5f) * 2.0f * halfWidth;
            int a0 = lightAlphaAt(x0, halfWidth, coreRatio, alphaMultiplier);
            int a1 = lightAlphaAt(x1, halfWidth, coreRatio, alphaMultiplier);
            buffer.addVertexWith2DPose(pose, x0, -halfHeight).setColor(r, g, b, a0);
            buffer.addVertexWith2DPose(pose, x1, -halfHeight).setColor(r, g, b, a1);
            buffer.addVertexWith2DPose(pose, x1, halfHeight).setColor(r, g, b, a1);
            buffer.addVertexWith2DPose(pose, x0, halfHeight).setColor(r, g, b, a0);
        }
        
        GDMesh.submit(guiGraphics, buffer);
    }

    private static int lightAlphaAt(float x, float halfWidth, float coreRatio, float alphaMultiplier) {
        float dist = Math.abs(x) / halfWidth;
        float alphaVal;
        if (dist <= coreRatio) {
            alphaVal = 1.0f;
        } else {
            float decayProgress = (dist - coreRatio) / (1.0f - coreRatio);
            alphaVal = (float) Math.pow(1.0f - decayProgress, 2.0);
        }
        return (int) (alphaVal * 255 * alphaMultiplier);
    }
    private int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    private float resolveFrameRatio(String textureKey, String suffixKey) {
        if (currentConfig == null || textureKey == null) {
            return 1.0f;
        }
        String key = "anim_" + textureKey + "_" + suffixKey;
        if (!currentConfig.has(key)) {
            return 1.0f;
        }
        float value = currentConfig.get(key).getAsFloat();
        return value > 0 ? value : 1.0f;
    }

    @Override
    public void trigger(TriggerContext context) {
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "card_bar");
        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) {
            return;
        }
        
        if (context.type() == org.mods.gd656killicon.common.KillType.ASSIST) {
            return;
        }
        this.flashStartTime = PreviewRenderTimeContext.currentTimeMillis();
        
        if (config != null) {
            String lightColorCt = config.has("color_light_ct") ? config.get("color_light_ct").getAsString() : "9cc1eb";
            String lightColorT = config.has("color_light_t") ? config.get("color_light_t").getAsString() : "d9ac5b";
            
            Minecraft mc = Minecraft.getInstance();
            String team = config.has("team") ? config.get("team").getAsString() : "ct";
            boolean dynamicCardStyle = config.has("dynamic_card_style") && config.get("dynamic_card_style").getAsBoolean();
             if (dynamicCardStyle && mc.player != null) {
                Team pt = mc.player.getTeam();
                if (pt != null) {
                    ChatFormatting color = pt.getColor();
                    if (color == ChatFormatting.BLUE || color == ChatFormatting.AQUA 
                            || color == ChatFormatting.DARK_AQUA || color == ChatFormatting.DARK_BLUE) {
                        team = "ct";
                    } else if (color == ChatFormatting.YELLOW || color == ChatFormatting.GOLD) {
                        team = "t";
                    }
                }
            }
            boolean isT = "t".equalsIgnoreCase(team);
            int color = parseColor(isT ? lightColorT : lightColorCt);
            
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int r2 = (r + 255) / 2;
            int g2 = (g + 255) / 2;
            int b2 = (b + 255) / 2;
            int explosionColor2 = (r2 << 16) | (g2 << 8) | b2;
            
            ringEffect.trigger(PreviewRenderTimeContext.currentTimeMillis(), true, context.type(), color, explosionColor2, color);
        }
        
        int combo = context.comboCount();
        int waveCount = Math.min(combo, 5);
        if (combo >= 6) {
            waveCount = 1;
        }

        if (waveCount > 0) {
            waveSystem.trigger(waveCount);
        }
    }

    private static class WaveEffectSystem {
        private static class Wave {
            long startTime;
            int direction;             
            Wave(long startTime, int direction) {
                this.startTime = startTime;
                this.direction = direction;
            }
        }
        
        private final List<Wave> waves = new ArrayList<>();
        private final Queue<Long> pendingSpawns = new LinkedList<>();
        private long lastSpawnTime = 0;
        
        private static final float WAVE_RADIUS_RATIO = 0.15f;         private static final float MAX_STRETCH_PIXELS = 25.0f;         
        public void trigger(int pairCount) {
            long now = PreviewRenderTimeContext.currentTimeMillis();
            for (int i = 0; i < pairCount; i++) {
                pendingSpawns.offer(now);
            }
        }
        
        public void updateAndRender(GuiGraphicsExtractor guiGraphics, float width, String colorHex, float animationDuration, float flashAlpha) {
            long now = PreviewRenderTimeContext.currentTimeMillis();
            long interval = (long) ((animationDuration * 1000) / 2.0f);
            
            if (!pendingSpawns.isEmpty()) {
                if (now - lastSpawnTime >= interval) {
                    pendingSpawns.poll(); 
                    waves.add(new Wave(now, -1));                     waves.add(new Wave(now, 1));                      lastSpawnTime = now;
                }
            } else {
                if (now - lastSpawnTime > interval * 2) {
                   lastSpawnTime = now - interval; 
                }
            }
            
            long waveLifeTime = (long) (animationDuration * 1000 * 4.0f);             Iterator<Wave> it = waves.iterator();
            while (it.hasNext()) {
                if (now - it.next().startTime > waveLifeTime) {
                    it.remove();
                }
            }
            
            if (waves.isEmpty()) return;
            
            renderWaves(guiGraphics, width, colorHex, animationDuration, flashAlpha, now, waveLifeTime);
        }
        
        private void renderWaves(GuiGraphicsExtractor guiGraphics, float width, String colorHex, float animationDuration, float flashAlpha, long now, long lifeTime) {
            int color = parseColor(colorHex);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            
            int fr = 255, fg = 255, fb = 255;
            
            float halfWidth = width / 2.0f;
            float waveRadius = width * WAVE_RADIUS_RATIO;
            float speed = halfWidth / lifeTime; 
            
            com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
            
            
        GDMesh.Builder buffer = GDMesh.begin();
            Matrix3x2fStack pose = guiGraphics.pose();
            
            for (float x = -halfWidth; x <= halfWidth; x += 1.0f) {
                float distFromCenter = Math.abs(x);
                float maxDistRatio = distFromCenter / halfWidth; 
                
                
                float totalInfluence = 0.0f;
                for (Wave w : waves) {
                    long elapsed = now - w.startTime;
                    float wavePos = w.direction * (speed * elapsed);
                    float distToWave = Math.abs(x - wavePos);
                    if (distToWave < waveRadius) {
                        float influence = 1.0f - (distToWave / waveRadius);
                        influence = (float) Math.pow(influence, 2.0); 
                        totalInfluence += influence;
                    }
                }
                
                if (totalInfluence <= 0.001f) continue;
                
                float fadeMask = 1.0f - (maxDistRatio * 0.99f); 
                float baseAlpha = totalInfluence;
                
                boolean isSpecial = (Math.abs(x) % 20) < 1.0f;
                float alphaMultiplier = isSpecial ? 1.5f : 1.0f;
                float lengthMultiplier = isSpecial ? 1.5f : 1.0f;
                
                float finalAlpha = baseAlpha * fadeMask * alphaMultiplier;
                finalAlpha = Math.min(finalAlpha, 1.0f);
                
                float randomLen = (float) (Math.sin(x * 123.456f) * 2.5 + 2.5);                 
                float stretch = totalInfluence * MAX_STRETCH_PIXELS * fadeMask * lengthMultiplier + randomLen;
                
                if (finalAlpha < 0.01f) continue;
                
                int aBottom = (int) (finalAlpha * 255);
                int aTop = 0;                 
                float yBottom = 0.5f;
                float yTop = -0.5f - stretch;
                
                float xLeft = x;
                float xRight = x + 1.0f;
                
                buffer.addVertexWith2DPose(pose, xRight, yBottom).setColor(r, g, b, aBottom);
                buffer.addVertexWith2DPose(pose, xRight, yTop).setColor(r, g, b, aTop);
                buffer.addVertexWith2DPose(pose, xLeft, yTop).setColor(r, g, b, aTop);
                buffer.addVertexWith2DPose(pose, xLeft, yBottom).setColor(r, g, b, aBottom);
                
                if (flashAlpha > 0.01f) {
                    int faBottom = (int) (finalAlpha * flashAlpha * 255);
                    int faTop = 0;
                    if (faBottom > 0) {
                        buffer.addVertexWith2DPose(pose, xRight, yBottom).setColor(fr, fg, fb, faBottom);
                        buffer.addVertexWith2DPose(pose, xRight, yTop).setColor(fr, fg, fb, faTop);
                        buffer.addVertexWith2DPose(pose, xLeft, yTop).setColor(fr, fg, fb, faTop);
                        buffer.addVertexWith2DPose(pose, xLeft, yBottom).setColor(fr, fg, fb, faBottom);
                    }
                }
            }
            
        GDMesh.submit(guiGraphics, buffer);
}

        private static int lightAlphaAt(float x, float halfWidth, float coreRatio, float alphaMultiplier) {
        float dist = Math.abs(x) / halfWidth;
        float alphaVal;
        if (dist <= coreRatio) {
            alphaVal = 1.0f;
        } else {
            float decayProgress = (dist - coreRatio) / (1.0f - coreRatio);
            alphaVal = (float) Math.pow(1.0f - decayProgress, 2.0);
        }
        return (int) (alphaVal * 255 * alphaMultiplier);
    }
    private int parseColor(String hex) {
            try {
                return Integer.parseInt(hex.replace("#", ""), 16);
            } catch (NumberFormatException e) {
                return 0xFFFFFF;
            }
        }
    }
}
