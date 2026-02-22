package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.Team;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.common.KillType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CardRenderer implements IHudRenderer {

    // Config Fields
    private float configScale = 1.0f;
    private int configXOffset = 0;
    private int configYOffset = 0;
    private float animationDuration = 0.75f;
    private String colorTextCt = "9cc1eb";
    private String colorTextT = "d9ac5b";
    private float textScale = 1.0f;
    private String team = "ct";
    private boolean dynamicCardStyle = false;
    private int maxStackCount = 5;
    private JsonObject currentConfig;

    // Constants
    private static final int CARD_SIZE = 256;
    private static final float MOVE_DISTANCE_MULTIPLIER = 0.9f;

    // State
    private final List<CardInstance> activeCards = new ArrayList<>();
    private PendingTrigger pendingTrigger;

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        // Load config
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "card");
        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) {
            activeCards.clear();
            pendingTrigger = null;
            return;
        }
        loadConfig(config);

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Standard Point (Configured Position)
        float standardX = screenWidth / 2.0f + configXOffset;
        float standardY = screenHeight - configYOffset;

        renderInternal(guiGraphics, partialTick, standardX, standardY);
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float standardX, float standardY) {
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "card");
        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) {
            activeCards.clear();
            pendingTrigger = null;
            return;
        }
        loadConfig(config);

        renderInternal(guiGraphics, partialTick, standardX, standardY);
    }

    public void renderPreviewAt(GuiGraphics guiGraphics, float partialTick, float standardX, float standardY, JsonObject config) {
        if (config == null) {
            activeCards.clear();
            pendingTrigger = null;
            return;
        }
        loadConfig(config);
        renderInternal(guiGraphics, partialTick, standardX, standardY);
    }

    private void renderInternal(GuiGraphics guiGraphics, float partialTick, float standardX, float standardY) {
        long currentTime = System.currentTimeMillis();
        long displayDuration = resolveDisplayDuration();
        long animDurMs = (long) (animationDuration * 1000);
        Minecraft mc = Minecraft.getInstance();

        // Cleanup and State Updates
        Iterator<CardInstance> it = activeCards.iterator();
        while (it.hasNext()) {
            CardInstance card = it.next();

            // 1. Check Display Duration (for the whole stack, driven by newest)
            // User: "Only when the latest card reaches the card display time... entire fan stack execute exit logic"
            // We handle this by checking the newest card below.
            
            // 2. State Transitions
            long age = currentTime - card.spawnTime;
            
            if (card.state == CardState.ENTERING) {
                if (age >= animDurMs) {
                    card.state = CardState.DISPLAYING;
                }
            } else if (card.state == CardState.EXITING) {
                long exitElapsed = currentTime - card.exitStartTime;
                if (exitElapsed > animDurMs) {
                    it.remove();
                    continue;
                }
            }
            
            // 3. Special Removal for merged cards (1-4 when 5 appears, or covered cards)
            if (card.isPendingRemoval) {
                long transitionElapsed = currentTime - card.lastLayoutUpdateTime;
                // Wait for transition animation to finish before removing?
                // User: "Animation completes... directly hide"
                // User: "Wait for previous card to complete entrance animation before deleting" (for 5+ stack)
                // Let's use animDurMs as the safe threshold.
                if (transitionElapsed >= animDurMs) {
                    it.remove();
                    continue;
                }
            }
        }

        if (activeCards.isEmpty() && pendingTrigger != null) {
            CardInstance newCard = new CardInstance(
                pendingTrigger.killType,
                pendingTrigger.comboCount,
                currentTime
            );
            activeCards.add(newCard);
            pendingTrigger = null;
            updateLayout(currentTime);
        }

        // Global Exit Logic based on Newest Card
        if (!activeCards.isEmpty()) {
            CardInstance newest = activeCards.get(activeCards.size() - 1);
            // If newest card expires, everything exits
            if (newest.state == CardState.DISPLAYING) {
                long newestAge = currentTime - newest.spawnTime;
                if (newestAge > displayDuration) {
                    // Trigger exit for ALL cards
                    long exitTime = currentTime;
                    for (CardInstance card : activeCards) {
                        if (card.state != CardState.EXITING) {
                            card.startExit(exitTime);
                        }
                    }
                }
            }
        }

        // Render Loop
        // Render from oldest to newest (Painter's algorithm for correct Z-order)
        for (CardInstance card : activeCards) {
            renderCard(guiGraphics, card, currentTime, standardX, standardY, animDurMs, mc);
        }
    }

    private void renderCard(GuiGraphics guiGraphics, CardInstance card, long currentTime, float standardX, float standardY, long animDurMs, Minecraft mc) {
        long elapsed = currentTime - card.spawnTime;
        float maxDist = CARD_SIZE * configScale * MOVE_DISTANCE_MULTIPLIER;
        
        // --- Calculate Animation Progress ---
        
        // 1. Radial Distance (Entrance)
        float currentDist = 0;
        if (card.state == CardState.ENTERING) {
            float progress = (float) elapsed / animDurMs;
            progress = Mth.clamp(progress, 0.0f, 1.0f);
            float moveProgress = calculateSegmentedEaseOut(progress);
            currentDist = moveProgress * maxDist;
        } else if (card.state == CardState.EXITING) {
            // Exit: Move back down? Or just fade out?
            // "Execute exit logic together". Usually fade out + move down.
            // Let's keep the "Move Down" logic but apply it to the radial distance.
            long exitElapsed = currentTime - card.exitStartTime;
            float progress = (float) exitElapsed / animDurMs;
            progress = Mth.clamp(progress, 0.0f, 1.0f);
            float moveProgress = calculateSegmentedEaseIn(progress);
            // Invert: maxDist -> 0
            currentDist = Mth.lerp(moveProgress, maxDist, 0);
        } else {
            currentDist = maxDist;
        }
        
        // 2. Angular Position (Lateral Move)
        float currentAngle = card.targetAngle;
        if (currentTime - card.lastLayoutUpdateTime < animDurMs) {
             float layoutProgress = (float) (currentTime - card.lastLayoutUpdateTime) / animDurMs;
             layoutProgress = Mth.clamp(layoutProgress, 0.0f, 1.0f);
             // User requested "Non-linear smooth animation"
             float angleEase = calculateSegmentedEaseOut(layoutProgress); 
             currentAngle = Mth.lerp(angleEase, card.startAngle, card.targetAngle);
        }
        card.currentAngle = currentAngle; // Update state for next layout calculation

        // --- Calculate Render Position ---
        // Angle 0 is UP. +Angle is Right (Clockwise?).
        // User: "Standard point direction right offset 5 deg".
        // In screen coords: Up is -Y. Right is +X.
        // Vector for 0 deg: (0, -1).
        // Vector for +90 deg: (1, 0).
        // Formula: x = sin(a), y = -cos(a).
        
        double rad = Math.toRadians(currentAngle);
        float offsetX = (float) Math.sin(rad) * currentDist;
        float offsetY = (float) -Math.cos(rad) * currentDist;
        
        float renderX = standardX + offsetX;
        float renderY = standardY + offsetY;
        
        // --- Calculate Alpha ---
        float alpha = 1.0f;
        if (card.state == CardState.ENTERING) {
            float fadeDur = animDurMs / 3.0f;
            if (elapsed < fadeDur) {
                alpha = (float) elapsed / fadeDur;
            }
        } else if (card.state == CardState.EXITING) {
            long exitElapsed = currentTime - card.exitStartTime;
            float progress = (float) exitElapsed / animDurMs;
             if (progress > 0.66f) {
                float fadeP = (progress - 0.66f) / 0.33f;
                alpha = 1.0f - fadeP;
            }
        }
        
        // Merging/Removal Fade Out (Optional, but user said "Directly hide". Let's stick to alpha logic if needed)
        // If pending removal, maybe we should fade it out? 
        // User said: "Animation completes... directly hide". So we just stop rendering when removed.
        // But during the transition (moving to 0 deg), it is visible.

        // --- Render ---
        
        // Light Effect
        float lightAlpha = 0.0f;
        float lightScale = 1.0f;
        
        long lightTotalDur = animDurMs * 5;
        if (elapsed < lightTotalDur) {
            float lightP = (float) elapsed / lightTotalDur;
            lightAlpha = 1.0f - (lightP * lightP); 

            long scaleUpDur = animDurMs / 3;
            if (elapsed < scaleUpDur) {
                float scaleP = (float) elapsed / scaleUpDur;
                lightScale = Mth.lerp(scaleP, 0.5f, 1.0f);
            } else {
                float scaleP = (float) (elapsed - scaleUpDur) / (lightTotalDur - scaleUpDur);
                lightScale = Mth.lerp(scaleP, 1.0f, 0.6f);
            }
        }
        
        // Determine Texture
        String currentTeam = this.team;
        if (this.dynamicCardStyle && mc.player != null) {
            Team pt = mc.player.getTeam();
            if (pt != null) {
                ChatFormatting color = pt.getColor();
                if (color == ChatFormatting.BLUE || color == ChatFormatting.AQUA 
                        || color == ChatFormatting.DARK_AQUA || color == ChatFormatting.DARK_BLUE) {
                    currentTeam = "ct";
                } else if (color == ChatFormatting.YELLOW || color == ChatFormatting.GOLD) {
                    currentTeam = "t";
                }
            }
        }
        
        boolean isT = "t".equalsIgnoreCase(currentTeam);
        String suffix = isT ? "_t.png" : "_ct.png";
        String cardTextureName = getCardTextureName(card.killType) + suffix;
        String lightTextureName = "killicon_card_light" + suffix;
        String cardTextureKey = getCardTextureKey(card.killType, isT);
        String lightTextureKey = isT ? "light_t" : "light_ct";
        
        ResourceLocation cardTexture = ExternalTextureManager.getTexture(cardTextureName);
        ResourceLocation lightTexture = ExternalTextureManager.getTexture(lightTextureName);

        if (cardTexture == null) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, activeCards.indexOf(card));

        // Light
        if (lightTexture != null && lightAlpha > 0.01f) {
            float lightWidthRatio = resolveFrameRatio(lightTextureKey, "texture_frame_width_ratio");
            float lightHeightRatio = resolveFrameRatio(lightTextureKey, "texture_frame_height_ratio");
            float lightW = CARD_SIZE * configScale * lightWidthRatio;
            float lightH = CARD_SIZE * configScale * lightHeightRatio; 
            
            poseStack.pushPose();
            // Align light with card:
            // 1. Move to Card Center
            poseStack.translate(renderX, renderY, 0); 
            // 2. Rotate around Card Center (same as card)
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(currentAngle));
            // 3. Move to Card Bottom (in local card space)
            // Card bottom is at y = +size/2 (since card is drawn from -size/2 to +size/2)
            poseStack.translate(0, CARD_SIZE * configScale / 2.0f, 0);
            
            // 4. Scale Light (from its bottom anchor)
            poseStack.scale(lightScale, lightScale, 1.0f);
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, lightAlpha * alpha); 
            
            // 5. Draw Light upwards from anchor (0,0)
            // Light texture should be drawn such that (0,0) is bottom center
            // X: -lightW/2 to +lightW/2
            // Y: -lightH to 0
            guiGraphics.blit(lightTexture, (int)(-lightW / 2), (int)(-lightH), (int)lightW, (int)lightH, 0, 0, (int)lightW, (int)lightH, (int)lightW, (int)lightH);
            poseStack.popPose();
        }

        // Card
        poseStack.pushPose();
        poseStack.translate(renderX, renderY, 0);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(currentAngle));
        poseStack.scale(configScale, configScale, 1.0f);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        
        float cardWidthRatio = resolveFrameRatio(cardTextureKey, "texture_frame_width_ratio");
        float cardHeightRatio = resolveFrameRatio(cardTextureKey, "texture_frame_height_ratio");
        int drawWidth = Math.round(CARD_SIZE * cardWidthRatio);
        int drawHeight = Math.round(CARD_SIZE * cardHeightRatio);
        guiGraphics.blit(cardTexture, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
        
        // Flash
        float flashAlpha = 0.0f;
        long flashHold = animDurMs / 2;
        long flashFade = animDurMs * 4;
        if (elapsed < flashHold) {
            flashAlpha = 1.0f;
        } else {
            long flashElapsed = elapsed - flashHold;
            if (flashElapsed < flashFade) {
                flashAlpha = 1.0f - ((float) flashElapsed / flashFade);
            }
        }
        
        if (flashAlpha > 0.01f) {
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, flashAlpha * alpha);
            guiGraphics.blit(cardTexture, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
             if (flashAlpha > 0.5f) {
                 RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (flashAlpha - 0.5f) * 2.0f * alpha);
                 guiGraphics.blit(cardTexture, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
            }
            RenderSystem.defaultBlendFunc();
        }
        
        // Combo Text
        if (card.comboCount > 0) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            String text = String.valueOf(card.comboCount);
            Font font = mc.font;
            String colorHex = isT ? colorTextT : colorTextCt;
            int color = parseColor(colorHex);
            int alphaInt = (int) (alpha * 255);
            int finalColor = (color & 0x00FFFFFF) | (alphaInt << 24);
            
            poseStack.pushPose();
            poseStack.scale(textScale, textScale, 1.0f);
            int textWidth = font.width(text);
            guiGraphics.drawString(font, text, -textWidth / 2, -font.lineHeight / 2, finalColor, true);
            poseStack.popPose();
        }

        poseStack.popPose(); // Pop Card
        poseStack.popPose(); // Pop Z
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private float calculateSegmentedEaseOut(float t) {
        if (t < 0.5f) {
            // First 50% of time covers 90% of distance (faster entrance)
            return t * 1.8f;
        } else {
            // Last 50% of time covers remaining 10% (very slow settle)
            float t2 = (t - 0.5f) * 2.0f;
            // Use Cubic ease out for even smoother deceleration at the end
            return 0.9f + (1.0f - (float)Math.pow(1.0f - t2, 3)) * 0.1f;
        }
    }
    
    private float calculateSegmentedEaseIn(float t) {
        if (t < 0.5f) {
            float t2 = t * 2.0f;
            return t2 * t2 * 0.2f;
        } else {
            return 0.2f + (t - 0.5f) * 1.6f;
        }
    }

    private void loadConfig(JsonObject config) {
        this.currentConfig = config;
        this.configScale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        this.configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
        this.configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 0;
        this.team = config.has("team") ? config.get("team").getAsString() : "ct";
        this.animationDuration = config.has("animation_duration") ? config.get("animation_duration").getAsFloat() : 0.75f;
        this.colorTextCt = config.has("color_text_ct") ? config.get("color_text_ct").getAsString() : "9cc1eb";
        this.colorTextT = config.has("color_text_t") ? config.get("color_text_t").getAsString() : "d9ac5b";
        this.textScale = config.has("text_scale") ? config.get("text_scale").getAsFloat() : 1.0f;
        this.dynamicCardStyle = config.has("dynamic_card_style") && config.get("dynamic_card_style").getAsBoolean();
        this.maxStackCount = config.has("max_stack_count") ? config.get("max_stack_count").getAsInt() : 5;
    }

    private long resolveDisplayDuration() {
        long serverDuration = ComboIconRenderer.getServerComboWindowMs();
        if (serverDuration > 0) {
            return serverDuration;
        }
        return 3000L;
    }

    private String getCardTextureName(int killType) {
        return switch (killType) {
            case KillType.HEADSHOT -> "killicon_card_headshot";
            case KillType.EXPLOSION -> "killicon_card_explosion";
            case KillType.CRIT -> "killicon_card_crit";
            default -> "killicon_card_default";
        };
    }

    private String getCardTextureKey(int killType, boolean isT) {
        String base = switch (killType) {
            case KillType.HEADSHOT -> "headshot";
            case KillType.EXPLOSION -> "explosion";
            case KillType.CRIT -> "crit";
            default -> "default";
        };
        return isT ? base + "_t" : base + "_ct";
    }

    private float resolveFrameRatio(String textureKey, String suffixKey) {
        if (currentConfig == null || textureKey == null) {
            return 1.0f;
        }
        String key = "anim_" + textureKey + "_" + suffixKey;
        if (!currentConfig.has(key)) {
            return 1.0f;
        }
        int value = currentConfig.get(key).getAsInt();
        return value > 0 ? value : 1.0f;
    }

    private int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
    
    // Removed interpolateColor as it is no longer used

    @Override
    public void trigger(TriggerContext context) {
        // Ignore Assist and Vehicle Destruction
        if (context.type() == KillType.ASSIST || 
            context.type() == KillType.DESTROY_VEHICLE) {
            return;
        }

        if (context.comboCount() == 1 && !activeCards.isEmpty()) {
            long now = System.currentTimeMillis();
            for (CardInstance card : activeCards) {
                if (card.state != CardState.EXITING) {
                    card.startExit(now);
                }
            }
            pendingTrigger = new PendingTrigger(context.type(), context.comboCount());
            return;
        }

        // If the current stack is exiting (hiding logic started), treat it as cleared
        // so the new card starts a fresh fan queue (0 degrees, standard position)
        if (!activeCards.isEmpty()) {
            CardInstance newest = activeCards.get(activeCards.size() - 1);
            if (newest.state == CardState.EXITING) {
                activeCards.clear();
                pendingTrigger = null;
            }
        }

        long now = System.currentTimeMillis();
        CardInstance newCard = new CardInstance(
            context.type(),
            context.comboCount(),
            now
        );
        activeCards.add(newCard);
        
        updateLayout(now);
    }
    
    private void updateLayout(long now) {
        int count = activeCards.size();
        if (count == 0) return;
        
        CardInstance newest = activeCards.get(count - 1);
        // User: "When 5th kill... hide cards except 5... 5 kill onwards... stack directly"
        boolean isStackingMode = newest.comboCount >= this.maxStackCount;

        if (isStackingMode) {
            // Collapse everything to 0
            for (int i = 0; i < count; i++) {
                CardInstance card = activeCards.get(i);
                
                // Ensure target is 0
                if (card.targetAngle != 0f) {
                    card.startAngle = card.currentAngle; // Start from where it is
                    card.targetAngle = 0f;
                    card.lastLayoutUpdateTime = now;
                }
                
                // Mark older cards for removal
                // "Wait for previous card to complete entrance animation before deleting"
                // This is handled by setting isPendingRemoval and relying on the timer in render()
                if (i < count - 1) {
                    if (!card.isPendingRemoval) {
                        card.isPendingRemoval = true;
                        // Reset timer to ensure they stay for the duration of the new card's entrance
                        card.lastLayoutUpdateTime = now; 
                    }
                }
            }
        } else {
            // Fan Layout (1-4 cards)
            float centerIndex = (count - 1) / 2.0f;
            for (int i = 0; i < count; i++) {
                CardInstance card = activeCards.get(i);
                float newTarget = (i - centerIndex) * 10.0f;
                
                // Only update if changed
                if (Math.abs(card.targetAngle - newTarget) > 0.01f || (i == count - 1 && card.state == CardState.ENTERING)) {
                    if (i == count - 1) {
                        // New card enters at its target angle
                        card.targetAngle = newTarget;
                        card.currentAngle = newTarget; 
                        card.startAngle = newTarget;
                        // No transition animation for entrance angle
                    } else {
                        // Existing card transitions
                        card.startAngle = card.currentAngle;
                        card.targetAngle = newTarget;
                        card.lastLayoutUpdateTime = now;
                    }
                }
            }
        }
    }
    
    // Deprecated local method, delegating to ComboIconRenderer for consistency if called externally
    public static void updateServerComboWindowSeconds(double seconds) {
        ComboIconRenderer.updateServerComboWindowSeconds(seconds);
    }

    private enum CardState { ENTERING, DISPLAYING, EXITING }
    
    private static class PendingTrigger {
        final int killType;
        final int comboCount;

        PendingTrigger(int killType, int comboCount) {
            this.killType = killType;
            this.comboCount = comboCount;
        }
    }

    private static class CardInstance {
        final int killType;
        final int comboCount;
        final long spawnTime;
        CardState state = CardState.ENTERING;
        long exitStartTime = -1;
        
        // Layout
        float currentAngle = 0f;
        float startAngle = 0f;
        float targetAngle = 0f;
        long lastLayoutUpdateTime = 0;
        boolean isPendingRemoval = false;

        CardInstance(int killType, int comboCount, long spawnTime) {
            this.killType = killType;
            this.comboCount = comboCount;
            this.spawnTime = spawnTime;
        }
        
        void startExit(long currentTime) {
            if (state != CardState.EXITING) {
                state = CardState.EXITING;
                exitStartTime = currentTime;
            }
        }
    }
}
