package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Team;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
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

public class CardBarRenderer implements IHudRenderer {
    
    // Cache for texture aspect ratios to avoid IO on every frame
    private static final Map<ResourceLocation, Float> ASPECT_RATIO_CACHE = new ConcurrentHashMap<>();
    private static final float DEFAULT_ASPECT_RATIO = 1.0f;
    private static final int BASE_LOGICAL_HEIGHT = 40; // Standard HUD element height reference
    
    private long flashStartTime = -1;
    private final WaveEffectSystem waveSystem = new WaveEffectSystem();
    private final IconRingEffect ringEffect = new IconRingEffect();
    {
        ringEffect.setScale(1.0f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "card_bar");

        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) {
            return;
        }

        // Load config
        float scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        int xOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
        int yOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 0;
        String team = config.has("team") ? config.get("team").getAsString() : "ct";
        boolean dynamicCardStyle = config.has("dynamic_card_style") && config.get("dynamic_card_style").getAsBoolean();
        float animationDuration = config.has("animation_duration") ? config.get("animation_duration").getAsFloat() : 0.2f;
        
        Minecraft mc = Minecraft.getInstance();

        // Dynamic Team Logic
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
        
        // Light effect config
        boolean showLight = config.has("show_light") && config.get("show_light").getAsBoolean();
        float lightWidth = config.has("light_width") ? config.get("light_width").getAsFloat() : 300.0f;
        float lightHeight = config.has("light_height") ? config.get("light_height").getAsFloat() : 20.0f;
        String lightColorCt = config.has("color_light_ct") ? config.get("color_light_ct").getAsString() : "9cc1eb";
        String lightColorT = config.has("color_light_t") ? config.get("color_light_t").getAsString() : "d9ac5b";

        // Determine texture
        String textureName = "killicon_card_bar_ct.png";
        boolean isT = "t".equalsIgnoreCase(team);
        if (isT) {
            textureName = "killicon_card_bar_t.png";
        }
        
        ResourceLocation texture = ExternalTextureManager.getTexture(textureName);
        if (texture == null) return;

        // Calculate logical dimensions based on texture aspect ratio
        float aspectRatio = getTextureAspectRatio(texture);
        int drawHeight = BASE_LOGICAL_HEIGHT;
        int drawWidth = (int) (drawHeight * aspectRatio);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Calculate position (bottom center + offset)
        float centerX = screenWidth / 2.0f + xOffset;
        float centerY = screenHeight - yOffset;
        renderInternal(guiGraphics, partialTick, centerX, centerY, scale, isT, texture, drawWidth, drawHeight, showLight, lightWidth, lightHeight, lightColorCt, lightColorT, animationDuration);
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "card_bar");

        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) {
            return;
        }

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

        String textureName = "killicon_card_bar_ct.png";
        boolean isT = "t".equalsIgnoreCase(team);
        if (isT) {
            textureName = "killicon_card_bar_t.png";
        }
        
        ResourceLocation texture = ExternalTextureManager.getTexture(textureName);
        if (texture == null) return;

        float aspectRatio = getTextureAspectRatio(texture);
        int drawHeight = BASE_LOGICAL_HEIGHT;
        int drawWidth = (int) (drawHeight * aspectRatio);

        renderInternal(guiGraphics, partialTick, centerX, centerY, scale, isT, texture, drawWidth, drawHeight, showLight, lightWidth, lightHeight, lightColorCt, lightColorT, animationDuration);
    }

    public void renderPreviewAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY, JsonObject config) {
        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) {
            return;
        }

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

        String textureName = "killicon_card_bar_ct.png";
        boolean isT = "t".equalsIgnoreCase(team);
        if (isT) {
            textureName = "killicon_card_bar_t.png";
        }
        
        ResourceLocation texture = ExternalTextureManager.getTexture(textureName);
        if (texture == null) return;

        float aspectRatio = getTextureAspectRatio(texture);
        int drawHeight = BASE_LOGICAL_HEIGHT;
        int drawWidth = (int) (drawHeight * aspectRatio);

        renderInternal(guiGraphics, partialTick, centerX, centerY, scale, isT, texture, drawWidth, drawHeight, showLight, lightWidth, lightHeight, lightColorCt, lightColorT, animationDuration);
    }

    private void renderInternal(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY, float scale, boolean isT, ResourceLocation texture, int drawWidth, int drawHeight, boolean showLight, float lightWidth, float lightHeight, String lightColorCt, String lightColorT, float animationDuration) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        poseStack.translate(centerX, centerY, 10.0f);
        poseStack.scale(scale, scale, 1.0f);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        float flashAlpha = 0.0f;
        long animDurMs = (long) (animationDuration * 1000);
        
        if (flashStartTime != -1) {
            long elapsed = System.currentTimeMillis() - flashStartTime;
            
            long flashHold = animDurMs / 2;
            long flashFade = animDurMs * 4;
            
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
            
            ringEffect.render(guiGraphics, 0, 0, System.currentTimeMillis());
        }
        
        if (showLight) {
             waveSystem.updateAndRender(guiGraphics, lightWidth, isT ? lightColorT : lightColorCt, animationDuration, flashAlpha);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.blit(texture, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
        
        if (flashAlpha > 0.01f) {
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, flashAlpha);
            guiGraphics.blit(texture, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
            
            if (flashAlpha > 0.5f) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (flashAlpha - 0.5f) * 2.0f);
                guiGraphics.blit(texture, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
            }
            
            RenderSystem.defaultBlendFunc();
        }
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }
    
    /**
     * Retrieves the aspect ratio (width / height) of the texture.
     * Caches the result to minimize I/O operations.
     */
    private float getTextureAspectRatio(ResourceLocation texture) {
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
            // Fallback if texture read fails
        }
        
        ASPECT_RATIO_CACHE.put(texture, DEFAULT_ASPECT_RATIO);
        return DEFAULT_ASPECT_RATIO;
    }

    private void renderLightEffect(GuiGraphics guiGraphics, float width, float height, String colorHex, float alphaMultiplier) {
        int color = parseColor(colorHex);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        
        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder buffer = tesselator.getBuilder();
        
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_STRIP, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        
        // Draw multiple segments to approximate non-linear fade
        // Use a segmented fade function:
        // Core region: alpha = 1.0
        // Fade region: alpha decays rapidly
        
        int segments = 50;
        float coreRatio = 0.5f; // 50% of the half-width is core (fully opaque)
        
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments; // 0 to 1
            float x = (t - 0.5f) * 2.0f * halfWidth; // range -halfWidth to halfWidth
            float dist = Math.abs(x) / halfWidth; // 0 (center) to 1 (edge)
            
            float alphaVal;
            if (dist <= coreRatio) {
                alphaVal = 1.0f;
            } else {
                // Map dist from [coreRatio, 1] to [0, 1] for decay calculation
                float decayProgress = (dist - coreRatio) / (1.0f - coreRatio);
                // Quadratic decay for the tail
                alphaVal = (float) Math.pow(1.0f - decayProgress, 2.0);
            }
            
            int a = (int) (alphaVal * 255 * alphaMultiplier);
            
            // Top vertex
            buffer.vertex(guiGraphics.pose().last().pose(), x, -halfHeight, 0).color(r, g, b, a).endVertex();
            // Bottom vertex
            buffer.vertex(guiGraphics.pose().last().pose(), x, halfHeight, 0).color(r, g, b, a).endVertex();
        }
        
        tesselator.end();
    }

    private int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
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
        this.flashStartTime = System.currentTimeMillis();
        
        // Trigger Ring Effect
        // Need to parse colors from config. 
        // We can re-fetch config or store current colors. 
        // Since trigger can happen anytime, fetching config is safer or pass from render?
        // But trigger doesn't have render context.
        // Using the already fetched config
        if (config != null) {
            String lightColorCt = config.has("color_light_ct") ? config.get("color_light_ct").getAsString() : "9cc1eb";
            String lightColorT = config.has("color_light_t") ? config.get("color_light_t").getAsString() : "d9ac5b";
            
            // Determine Team logic again? 
            // Or just use context? 
            // Context doesn't have team info. 
            // We'll assume the player's current team or config team.
            // But trigger is called when packet arrives.
            // We should use the same logic as render if possible, but we don't have player instance easily without MC.
            // We'll use MC instance.
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
            
            // Explosion Ring 2 Color: Whiter version.
            // Let's mix 50% white.
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int r2 = (r + 255) / 2;
            int g2 = (g + 255) / 2;
            int b2 = (b + 255) / 2;
            int explosionColor2 = (r2 << 16) | (g2 << 8) | b2;
            
            ringEffect.trigger(System.currentTimeMillis(), true, context.type(), color, explosionColor2, color);
        }
        
        int combo = context.comboCount();
        // 5-kill wave logic: "Five kills needs to release five virtual points"
        int waveCount = Math.min(combo, 5);
        if (combo >= 6) {
            waveCount = 1;
        }

        // Trigger waves
        if (waveCount > 0) {
            waveSystem.trigger(waveCount);
        }
    }

    private static class WaveEffectSystem {
        private static class Wave {
            long startTime;
            int direction; // -1 (left) or 1 (right)
            
            Wave(long startTime, int direction) {
                this.startTime = startTime;
                this.direction = direction;
            }
        }
        
        private final List<Wave> waves = new ArrayList<>();
        private final Queue<Long> pendingSpawns = new LinkedList<>();
        private long lastSpawnTime = 0;
        
        // Configuration Constants
        private static final float WAVE_RADIUS_RATIO = 0.15f; // Radius relative to width
        private static final float MAX_STRETCH_PIXELS = 25.0f; // Increased max height
        
        public void trigger(int pairCount) {
            long now = System.currentTimeMillis();
            for (int i = 0; i < pairCount; i++) {
                pendingSpawns.offer(now);
            }
        }
        
        public void updateAndRender(GuiGraphics guiGraphics, float width, String colorHex, float animationDuration, float flashAlpha) {
            long now = System.currentTimeMillis();
            long interval = (long) ((animationDuration * 1000) / 2.0f);
            
            // Process Pending Spawns
            if (!pendingSpawns.isEmpty()) {
                if (now - lastSpawnTime >= interval) {
                    pendingSpawns.poll(); 
                    waves.add(new Wave(now, -1)); // Left
                    waves.add(new Wave(now, 1));  // Right
                    lastSpawnTime = now;
                }
            } else {
                if (now - lastSpawnTime > interval * 2) {
                   lastSpawnTime = now - interval; 
                }
            }
            
            // Update Waves
            long waveLifeTime = (long) (animationDuration * 1000 * 4.0f); // 4x animation duration
            Iterator<Wave> it = waves.iterator();
            while (it.hasNext()) {
                if (now - it.next().startTime > waveLifeTime) {
                    it.remove();
                }
            }
            
            if (waves.isEmpty()) return;
            
            // Render
            renderWaves(guiGraphics, width, colorHex, animationDuration, flashAlpha, now, waveLifeTime);
        }
        
        private void renderWaves(GuiGraphics guiGraphics, float width, String colorHex, float animationDuration, float flashAlpha, long now, long lifeTime) {
            int color = parseColor(colorHex);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            
            int fr = 255, fg = 255, fb = 255;
            
            float halfWidth = width / 2.0f;
            float waveRadius = width * WAVE_RADIUS_RATIO;
            float speed = halfWidth / lifeTime; 
            
            com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
            com.mojang.blaze3d.vertex.BufferBuilder buffer = tesselator.getBuilder();
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
            // Disable depth test to avoid z-fighting with background line
            RenderSystem.disableDepthTest(); 
            
            buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
            
            for (float x = -halfWidth; x <= halfWidth; x += 1.0f) {
                float distFromCenter = Math.abs(x);
                float maxDistRatio = distFromCenter / halfWidth; 
                
                // Culling: if too far and no active waves near, skip? 
                // Simplification: just skip if calculated alpha is near zero
                
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
                
                // Special Highlight Unit logic: every 20px
                boolean isSpecial = (Math.abs(x) % 20) < 1.0f;
                float alphaMultiplier = isSpecial ? 1.5f : 1.0f;
                float lengthMultiplier = isSpecial ? 1.5f : 1.0f;
                
                float finalAlpha = baseAlpha * fadeMask * alphaMultiplier;
                finalAlpha = Math.min(finalAlpha, 1.0f);
                
                // Random length variation (deterministic based on x)
                float randomLen = (float) (Math.sin(x * 123.456f) * 2.5 + 2.5); // 0 to 5
                
                float stretch = totalInfluence * MAX_STRETCH_PIXELS * fadeMask * lengthMultiplier + randomLen;
                
                if (finalAlpha < 0.01f) continue;
                
                int aBottom = (int) (finalAlpha * 255);
                int aTop = 0; // Top is transparent
                
                float yBottom = 0.5f;
                float yTop = -0.5f - stretch;
                
                float xLeft = x;
                float xRight = x + 1.0f;
                
                // Draw Quad with Gradient Alpha (Bottom -> Top)
                // Bottom-Right
                buffer.vertex(guiGraphics.pose().last().pose(), xRight, yBottom, 0).color(r, g, b, aBottom).endVertex();
                // Top-Right
                buffer.vertex(guiGraphics.pose().last().pose(), xRight, yTop, 0).color(r, g, b, aTop).endVertex();
                // Top-Left
                buffer.vertex(guiGraphics.pose().last().pose(), xLeft, yTop, 0).color(r, g, b, aTop).endVertex();
                // Bottom-Left
                buffer.vertex(guiGraphics.pose().last().pose(), xLeft, yBottom, 0).color(r, g, b, aBottom).endVertex();
                
                // Flash Overlay
                if (flashAlpha > 0.01f) {
                    int faBottom = (int) (finalAlpha * flashAlpha * 255);
                    int faTop = 0;
                    if (faBottom > 0) {
                        buffer.vertex(guiGraphics.pose().last().pose(), xRight, yBottom, 0).color(fr, fg, fb, faBottom).endVertex();
                        buffer.vertex(guiGraphics.pose().last().pose(), xRight, yTop, 0).color(fr, fg, fb, faTop).endVertex();
                        buffer.vertex(guiGraphics.pose().last().pose(), xLeft, yTop, 0).color(fr, fg, fb, faTop).endVertex();
                        buffer.vertex(guiGraphics.pose().last().pose(), xLeft, yBottom, 0).color(fr, fg, fb, faBottom).endVertex();
                    }
                }
            }
            
            tesselator.end();
            RenderSystem.enableDepthTest(); // Restore
            // 不在这里禁用混合模式，让主渲染方法处理
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
