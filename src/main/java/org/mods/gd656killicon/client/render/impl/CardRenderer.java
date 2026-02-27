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
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.common.KillType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CardRenderer implements IHudRenderer {

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

    private static final int CARD_SIZE = 256;
    private static final float MOVE_DISTANCE_MULTIPLIER = 0.9f;

    private final List<CardInstance> activeCards = new ArrayList<>();
    private PendingTrigger pendingTrigger;

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
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

        Iterator<CardInstance> it = activeCards.iterator();
        while (it.hasNext()) {
            CardInstance card = it.next();

            
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
            
            if (card.isPendingRemoval) {
                long transitionElapsed = currentTime - card.lastLayoutUpdateTime;
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

        if (!activeCards.isEmpty()) {
            CardInstance newest = activeCards.get(activeCards.size() - 1);
            if (newest.state == CardState.DISPLAYING) {
                long newestAge = currentTime - newest.spawnTime;
                if (newestAge > displayDuration) {
                    long exitTime = currentTime;
                    for (CardInstance card : activeCards) {
                        if (card.state != CardState.EXITING) {
                            card.startExit(exitTime);
                        }
                    }
                }
            }
        }

        for (CardInstance card : activeCards) {
            renderCard(guiGraphics, card, currentTime, standardX, standardY, animDurMs, mc);
        }
    }

    private void renderCard(GuiGraphics guiGraphics, CardInstance card, long currentTime, float standardX, float standardY, long animDurMs, Minecraft mc) {
        long elapsed = currentTime - card.spawnTime;
        float maxDist = CARD_SIZE * configScale * MOVE_DISTANCE_MULTIPLIER;
        
        
        float currentDist = 0;
        if (card.state == CardState.ENTERING) {
            float progress = (float) elapsed / animDurMs;
            progress = Mth.clamp(progress, 0.0f, 1.0f);
            float moveProgress = calculateSegmentedEaseOut(progress);
            currentDist = moveProgress * maxDist;
        } else if (card.state == CardState.EXITING) {
            long exitElapsed = currentTime - card.exitStartTime;
            float progress = (float) exitElapsed / animDurMs;
            progress = Mth.clamp(progress, 0.0f, 1.0f);
            float moveProgress = calculateSegmentedEaseIn(progress);
            currentDist = Mth.lerp(moveProgress, maxDist, 0);
        } else {
            currentDist = maxDist;
        }
        
        float currentAngle = card.targetAngle;
        if (currentTime - card.lastLayoutUpdateTime < animDurMs) {
             float layoutProgress = (float) (currentTime - card.lastLayoutUpdateTime) / animDurMs;
             layoutProgress = Mth.clamp(layoutProgress, 0.0f, 1.0f);
             float angleEase = calculateSegmentedEaseOut(layoutProgress); 
             currentAngle = Mth.lerp(angleEase, card.startAngle, card.targetAngle);
        }
        card.currentAngle = currentAngle; 
        
        double rad = Math.toRadians(currentAngle);
        float offsetX = (float) Math.sin(rad) * currentDist;
        float offsetY = (float) -Math.cos(rad) * currentDist;
        
        float renderX = standardX + offsetX;
        float renderY = standardY + offsetY;
        
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
        String cardTextureKey = getCardTextureKey(card.killType, isT);
        String lightTextureKey = isT ? "light_t" : "light_ct";
        String cardTextureName = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/card",
            cardTextureKey,
            currentConfig
        );
        String lightTextureName = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/card",
            lightTextureKey,
            currentConfig
        );
        
        ResourceLocation cardTexture = ExternalTextureManager.getTexture(cardTextureName);
        ResourceLocation lightTexture = ExternalTextureManager.getTexture(lightTextureName);

        if (cardTexture == null) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, activeCards.indexOf(card));

        if (lightTexture != null && lightAlpha > 0.01f) {
            float lightWidthRatio = resolveFrameRatio(lightTextureKey, "texture_frame_width_ratio");
            float lightHeightRatio = resolveFrameRatio(lightTextureKey, "texture_frame_height_ratio");
            float lightW = CARD_SIZE * configScale * lightWidthRatio;
            float lightH = CARD_SIZE * configScale * lightHeightRatio; 
            
            poseStack.pushPose();
            poseStack.translate(renderX, renderY, 0); 
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(currentAngle));
            poseStack.translate(0, CARD_SIZE * configScale / 2.0f, 0);
            
            poseStack.scale(lightScale, lightScale, 1.0f);
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, lightAlpha * alpha); 
            
            guiGraphics.blit(lightTexture, (int)(-lightW / 2), (int)(-lightH), (int)lightW, (int)lightH, 0, 0, (int)lightW, (int)lightH, (int)lightW, (int)lightH);
            poseStack.popPose();
        }

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

        poseStack.popPose();         poseStack.popPose();         RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private float calculateSegmentedEaseOut(float t) {
        if (t < 0.5f) {
            return t * 1.8f;
        } else {
            float t2 = (t - 0.5f) * 2.0f;
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
    

    @Override
    public void trigger(TriggerContext context) {
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
        boolean isStackingMode = newest.comboCount >= this.maxStackCount;

        if (isStackingMode) {
            for (int i = 0; i < count; i++) {
                CardInstance card = activeCards.get(i);
                
                if (card.targetAngle != 0f) {
                    card.startAngle = card.currentAngle;                     card.targetAngle = 0f;
                    card.lastLayoutUpdateTime = now;
                }
                
                if (i < count - 1) {
                    if (!card.isPendingRemoval) {
                        card.isPendingRemoval = true;
                        card.lastLayoutUpdateTime = now; 
                    }
                }
            }
        } else {
            float centerIndex = (count - 1) / 2.0f;
            for (int i = 0; i < count; i++) {
                CardInstance card = activeCards.get(i);
                float newTarget = (i - centerIndex) * 10.0f;
                
                if (Math.abs(card.targetAngle - newTarget) > 0.01f || (i == count - 1 && card.state == CardState.ENTERING)) {
                    if (i == count - 1) {
                        card.targetAngle = newTarget;
                        card.currentAngle = newTarget; 
                        card.startAngle = newTarget;
                    } else {
                        card.startAngle = card.currentAngle;
                        card.targetAngle = newTarget;
                        card.lastLayoutUpdateTime = now;
                    }
                }
            }
        }
    }
    
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
