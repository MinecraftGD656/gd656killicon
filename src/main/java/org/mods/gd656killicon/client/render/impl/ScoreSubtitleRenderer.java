package org.mods.gd656killicon.client.render.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.effect.DigitalScrollEffect;
import org.mods.gd656killicon.client.sounds.SoundTriggerManager;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import com.google.gson.JsonObject;

/**
 * Renderer for the score subtitle element.
 * Displays a cumulative score with digital scroll animation and flicker effects.
 * Implements the Singleton pattern.
 */
public class ScoreSubtitleRenderer implements IHudRenderer {

    private static final long FADE_IN_DURATION = 250L;     private static final long FADE_OUT_DURATION = 300L;     private static final long SCALE_ANIMATION_PHASE_DURATION = 100L;     private static final float SCALE_ANIMATION_MAX_MULTIPLIER = 1.2f;
    private static final float GLOW_OFFSET = 0.3f;

    private static ScoreSubtitleRenderer instance;

    
    private int configXOffset = 0;
    private int configYOffset = 80;
    private long displayDuration = 3500L;     private String scoreFormat = "<score>";
    private int scoreThreshold = 1000;
    private String highScoreColor = "#D4B800";
    private String flashColor = "#D0D0D0";
    private float scale = 2.0f;
    private float animationDuration = 1.25f;
    private float animationRefreshRate = 0.01f;
    private boolean enableNumberSegmentation = false;     private boolean enableFlash = true;     private boolean alignLeft = false;     private boolean alignRight = false;     private boolean enableScoreScalingEffect = false;     private boolean enableDigitalScroll = true;     private boolean enableGlowEffect = false;     private float glowIntensity = 0.5f;     private int normalTextColor = 0xFFFFFFFF;

    private boolean visible = false;
    private float currentScore = 0.0f;
    private long lastScoreTime = -1;
    private long startTime = -1;
    private long textHideTime = -1;
    private boolean isFadingOut = false;
    private DigitalScrollEffect scrollEffect;
    private long scaleAnimationStartTime = -1;

    private ScoreSubtitleRenderer() {
    }

    public static synchronized ScoreSubtitleRenderer getInstance() {
        if (instance == null) {
            instance = new ScoreSubtitleRenderer();
        }
        return instance;
    }

    @Override
    public void trigger(TriggerContext context) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        RenderState state = resolveRenderState();
        if (state == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int centerX = screenWidth / 2 + configXOffset;
        int textY = screenHeight - configYOffset;
        renderInternal(guiGraphics, font, centerX, textY, state);
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        RenderState state = resolveRenderState();
        if (state == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int resolvedCenterX = Math.round(centerX);
        int resolvedTextY = Math.round(centerY);
        renderInternal(guiGraphics, font, resolvedCenterX, resolvedTextY, state);
    }

    public void resetPreview() {
        resetState();
    }

    private RenderState resolveRenderState() {
        if (!visible || startTime == -1) return null;

        long currentTime = System.currentTimeMillis();

        if (!isFadingOut && currentTime > textHideTime) {
            isFadingOut = true;
        }

        float alpha = calculateAlpha(currentTime);
        if (alpha <= 0.05f) {
            visible = false;
            resetState();
            return null;
        }

        updateAnimatedScore(currentTime);

        float currentScale = this.scale;
        long elapsed = currentTime - startTime;

        if (elapsed < FADE_IN_DURATION) {
            float progress = (float) elapsed / FADE_IN_DURATION;
            float easedProgress = 1.0f - (float) Math.pow(1.0f - progress, 3);
            currentScale = Mth.lerp(easedProgress, 1.5f, this.scale);
        }

        if (this.enableScoreScalingEffect && this.scaleAnimationStartTime != -1) {
            long scaleElapsed = currentTime - this.scaleAnimationStartTime;
            if (scaleElapsed < SCALE_ANIMATION_PHASE_DURATION * 2) {
                float animationScaleMultiplier;
                if (scaleElapsed < SCALE_ANIMATION_PHASE_DURATION) {
                    float t = (float) scaleElapsed / SCALE_ANIMATION_PHASE_DURATION;
                    float progress = 1.0f - (float) Math.pow(1.0f - t, 3);
                    animationScaleMultiplier = 1.0f + (SCALE_ANIMATION_MAX_MULTIPLIER - 1.0f) * progress;
                } else {
                    float t = (float) (scaleElapsed - SCALE_ANIMATION_PHASE_DURATION) / SCALE_ANIMATION_PHASE_DURATION;
                    float progress = 1.0f - (float) Math.pow(1.0f - t, 3);
                    animationScaleMultiplier = SCALE_ANIMATION_MAX_MULTIPLIER - (SCALE_ANIMATION_MAX_MULTIPLIER - 1.0f) * progress;
                }
                currentScale *= animationScaleMultiplier;
            }
        }

        return new RenderState(currentTime, currentScale, alpha);
    }

    private void renderInternal(GuiGraphics guiGraphics, Font font, int centerX, int textY, RenderState state) {
        float displayScore;
        if (this.enableDigitalScroll) {
            displayScore = scrollEffect != null ? scrollEffect.getCurrentValue() : 0;
        } else {
            displayScore = this.currentScore;
        }

        String scoreStr = formatNumberWithSegmentation(displayScore, enableNumberSegmentation);

        String prefix = "";
        String suffix = "";
        boolean hasPlaceholder = scoreFormat.contains("<score>");

        if (hasPlaceholder) {
            int placeholderIndex = scoreFormat.indexOf("<score>");
            prefix = scoreFormat.substring(0, placeholderIndex);
            suffix = scoreFormat.substring(placeholderIndex + 7);
        } else {
            prefix = scoreFormat;
        }

        int prefixWidth = font.width(prefix);
        int scoreWidth = hasPlaceholder ? font.width(scoreStr) : 0;
        int suffixWidth = font.width(suffix);
        int totalWidth = prefixWidth + scoreWidth + suffixWidth;

        int defaultColorWithAlpha = (normalTextColor & 0x00FFFFFF) | ((int) (state.alpha * 255.0f) << 24);
        int scoreColorWithAlpha = calculateScoreColor(displayScore, hasPlaceholder, state.alpha, state.currentTime, defaultColorWithAlpha);

        if (enableFlash && scoreColorWithAlpha != defaultColorWithAlpha && isFlickering(state.currentTime)) {
            defaultColorWithAlpha = scoreColorWithAlpha;
        }

        int textX;
        if (alignLeft && !alignRight) {
            textX = centerX;
        } else if (alignRight && !alignLeft) {
            textX = centerX - totalWidth;
        } else {
            textX = centerX - totalWidth / 2;
        }
        int currentX = textX;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        float pivotX;
        float pivotY = textY + font.lineHeight / 2.0f;

        if (alignLeft && !alignRight) {
            pivotX = textX;
        } else if (alignRight && !alignLeft) {
            pivotX = textX + totalWidth;
        } else {
            pivotX = textX + totalWidth / 2.0f;
        }

        poseStack.translate(pivotX, pivotY, 0);
        poseStack.scale(state.currentScale, state.currentScale, 1.0f);
        poseStack.translate(-pivotX, -pivotY, 0);

        if (!prefix.isEmpty()) {
            drawTextWithGlow(guiGraphics, font, prefix, currentX, textY, defaultColorWithAlpha, true);
            currentX += prefixWidth;
        }

        if (hasPlaceholder) {
            drawTextWithGlow(guiGraphics, font, scoreStr, currentX, textY, scoreColorWithAlpha, true);
            currentX += scoreWidth;
        }

        if (!suffix.isEmpty()) {
            drawTextWithGlow(guiGraphics, font, suffix, currentX, textY, defaultColorWithAlpha, true);
        }

        poseStack.popPose();
    }

    private static final class RenderState {
        private final long currentTime;
        private final float currentScale;
        private final float alpha;

        private RenderState(long currentTime, float currentScale, float alpha) {
            this.currentTime = currentTime;
            this.currentScale = currentScale;
            this.alpha = alpha;
        }
    }

    private void drawTextWithGlow(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color, boolean dropShadow) {
        if (this.enableGlowEffect) {
            int alpha = (color >> 24) & 0xFF;
            int glowAlpha = (int) (alpha * this.glowIntensity);
            glowAlpha = Math.max(0, Math.min(255, glowAlpha));
            
            int glowColor = (color & 0x00FFFFFF) | (glowAlpha << 24);
            
            PoseStack poseStack = guiGraphics.pose();
            
            float[][] offsets = {
                {-GLOW_OFFSET, 0}, {GLOW_OFFSET, 0}, {0, -GLOW_OFFSET}, {0, GLOW_OFFSET},
                {-GLOW_OFFSET, -GLOW_OFFSET}, {GLOW_OFFSET, -GLOW_OFFSET},
                {-GLOW_OFFSET, GLOW_OFFSET}, {GLOW_OFFSET, GLOW_OFFSET}
            };
            
            for (float[] offset : offsets) {
                poseStack.pushPose();
                poseStack.translate(offset[0], offset[1], 0);
                guiGraphics.drawString(font, text, x, y, glowColor, false);
                poseStack.popPose();
            }
        }
        guiGraphics.drawString(font, text, x, y, color, dropShadow);
    }


    /**
     * Add score to the display
     * @param score The score to add (will be rounded to integer)
     */
    public void addScore(float score) {
        JsonObject config = ConfigManager.getElementConfig("subtitle", "score");
        if (config == null) {
            return;
        }

        boolean configVisible = !config.has("visible") || config.get("visible").getAsBoolean();
        if (!configVisible) {
            this.visible = false;
            return;
        }

        loadConfig(config);

        if (this.displayDuration < FADE_IN_DURATION) {
            this.displayDuration = FADE_IN_DURATION;
        }

        long currentTime = System.currentTimeMillis();
        
        boolean shouldReset = false;
        if (this.isFadingOut) {
            shouldReset = true;
        } else if (this.textHideTime > 0 && currentTime > this.textHideTime) {
            shouldReset = true;
        }
        
        if (shouldReset) {
            resetState();
            this.currentScore = score;
        } else {
            this.currentScore += score;
        }
        
        if (scrollEffect != null) {
            scrollEffect.startAnimation(scrollEffect.getCurrentValue(), this.currentScore);
        }
        
        this.lastScoreTime = currentTime;
        this.scaleAnimationStartTime = currentTime;
        
        if (this.startTime == -1) {
            this.startTime = currentTime;
            this.visible = true;
            this.isFadingOut = false;
        }
        
        this.textHideTime = currentTime + this.displayDuration;
        
        SoundTriggerManager.playScoreSound();
    }


    /**
     * Loads configuration from the JSON object.
     * @param config The configuration JSON object.
     */
    private void loadConfig(JsonObject config) {
        try {
            this.configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
            this.configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 40;
            this.displayDuration = config.has("display_duration")
                ? (long)(config.get("display_duration").getAsFloat() * 1000)
                : 4000L;
            this.scale = config.has("scale") ? config.get("scale").getAsFloat() : 2.0f;
            this.scoreFormat = config.has("format_score") ? config.get("format_score").getAsString() : "<score>";
            this.scoreThreshold = config.has("score_threshold") ? config.get("score_threshold").getAsInt() : 1000;
            this.highScoreColor = config.has("color_high_score") ? config.get("color_high_score").getAsString() : "#D4B800";
            this.flashColor = config.has("color_flash") ? config.get("color_flash").getAsString() : "#D0D0D0";
            this.animationDuration = config.has("animation_duration") ? config.get("animation_duration").getAsFloat() : 1.25f;
            this.animationRefreshRate = config.has("animation_refresh_rate") ? config.get("animation_refresh_rate").getAsFloat() : 0.01f;
            this.enableNumberSegmentation = config.has("enable_number_segmentation") ? config.get("enable_number_segmentation").getAsBoolean() : false;
            this.enableFlash = config.has("enable_flash") ? config.get("enable_flash").getAsBoolean() : true;
            this.alignLeft = config.has("align_left") ? config.get("align_left").getAsBoolean() : false;
            this.alignRight = config.has("align_right") ? config.get("align_right").getAsBoolean() : false;
            this.enableScoreScalingEffect = config.has("enable_score_scaling_effect") ? config.get("enable_score_scaling_effect").getAsBoolean() : false;
            this.enableDigitalScroll = config.has("enable_digital_scroll") ? config.get("enable_digital_scroll").getAsBoolean() : true;
            this.enableGlowEffect = config.has("enable_glow_effect") && config.get("enable_glow_effect").getAsBoolean();
            this.glowIntensity = config.has("glow_intensity") ? config.get("glow_intensity").getAsFloat() : 0.5f;
            this.normalTextColor = hexColorToInt(config.has("color_normal_text") ? config.get("color_normal_text").getAsString() : "#FFFFFF");
            
            if (scrollEffect == null) {
                scrollEffect = new DigitalScrollEffect(animationDuration, animationRefreshRate, DigitalScrollEffect.Easing.QUINTIC_OUT);
            } else {
                scrollEffect.setAnimationDuration(animationDuration);
                scrollEffect.setAnimationRefreshRate(animationRefreshRate);
            }
        } catch (Exception e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.subtitle.config_error");
            this.configXOffset = 0;
            this.configYOffset = 80;
            this.displayDuration = 4000L;
            this.scale = 2.0f;
            this.scoreFormat = "<score>";
            this.scoreThreshold = 1000;
            this.highScoreColor = "#D4B800";
            this.flashColor = "#D0D0D0";
            this.animationDuration = 1.25f;
            this.animationRefreshRate = 0.01f;
            this.enableNumberSegmentation = false;
            this.enableFlash = true;
            this.alignLeft = false;
            this.alignRight = false;
            this.enableScoreScalingEffect = false;
            this.normalTextColor = 0xFFFFFFFF;
            
            if (scrollEffect == null) {
                scrollEffect = new DigitalScrollEffect(animationDuration, animationRefreshRate, DigitalScrollEffect.Easing.QUINTIC_OUT);
            } else {
                scrollEffect.setAnimationDuration(animationDuration);
                scrollEffect.setAnimationRefreshRate(animationRefreshRate);
            }
        }
    }

    /**
     * Calculates the alpha transparency based on fade-out duration.
     * @param currentTime Current system time.
     * @return Alpha value between 0.0 and 1.0.
     */
    private float calculateAlpha(long currentTime) {
        if (this.textHideTime > 0) {
            if (currentTime < this.textHideTime) {
                return 1.0f;
            } else {
                long fadeElapsed = currentTime - this.textHideTime;
                return Math.max(0.0f, 1.0f - (float) fadeElapsed / FADE_OUT_DURATION);
            }
        }
        return 1.0f;
    }

    /**
     * Resets all state variables.
     */
    private void resetState() {
        this.currentScore = 0.0f;
        this.startTime = -1;
        this.lastScoreTime = -1;
        this.textHideTime = -1;
        this.isFadingOut = false;
        this.visible = false;
        this.scrollEffect = null;
    }

    /**
     * Round float to integer (nearest whole number).
     */
    private int roundToInteger(float value) {
        return Math.round(value);
    }

    /**
     * Updates animated score based on animation progress.
     */
    private void updateAnimatedScore(long currentTime) {
        if (scrollEffect != null) {
            scrollEffect.update(currentTime);
        }
    }

    /**
     * Checks if the flicker effect is currently active (ON state).
     */
    private boolean isFlickering(long currentTime) {
        if (this.textHideTime > 0) {
            long timeRemaining = this.textHideTime - currentTime;
            if (timeRemaining > 0 && timeRemaining <= 2000) {
                return ((timeRemaining - 1) / 500) % 2 == 1;
            }
        }
        return false;
    }

    /**
     * Calculates the score text color including high score and flicker logic.
     */
    private int calculateScoreColor(float roundedScore, boolean hasPlaceholder, float alpha, long currentTime, int defaultColorWithAlpha) {
        int scoreColorWithAlpha;

        if (roundedScore >= scoreThreshold && hasPlaceholder) {
            int highScoreColorInt = hexColorToInt(highScoreColor);
            scoreColorWithAlpha = (highScoreColorInt & 0x00FFFFFF) | ((int) (alpha * 255.0f) << 24);
        } else {
            scoreColorWithAlpha = defaultColorWithAlpha;
        }

        if (enableFlash && isFlickering(currentTime)) {
            int flashColorInt = hexColorToInt(flashColor);
            int flashColorWithAlpha = (flashColorInt & 0x00FFFFFF) | ((int) (alpha * 255.0f) << 24);
            scoreColorWithAlpha = flashColorWithAlpha;
        }

        return scoreColorWithAlpha;
    }

    /**
     * Converts hex color string (e.g., "#RRGGBB" or "#RGB") to integer color.
     */
    private int hexColorToInt(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return 0xFFFFFFFF;         }
        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        try {
            if (hex.length() == 6) {
                return Integer.parseInt(hex, 16) | 0xFF000000;             } else if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            } else if (hex.length() == 3) {
                int r = Integer.parseInt(hex.substring(0, 1), 16) * 17;
                int g = Integer.parseInt(hex.substring(1, 2), 16) * 17;
                int b = Integer.parseInt(hex.substring(2, 3), 16) * 17;
                return (r << 16) | (g << 8) | b | 0xFF000000;
            }
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.score_subtitle.invalid_color", hexColor);
        }
        return 0xFFFFFFFF;     }

    /**
     * Formats a number with segmentation (thousands separator) if enabled.
     * Adds commas every three digits from right to left.
     * When number < 1, displays as float with 0.1 precision.
     * When number >= 1, displays as rounded integer.
     * Example: 0.123 → "0.1" (if enabled), 1.56 → "2" (if enabled)
     */
    private String formatNumberWithSegmentation(float number, boolean enabled) {
        boolean isNegative = number < 0;
        float absNumber = Math.abs(number);
        
        String formattedNumber;
        
        if (absNumber < 1.0f) {
            formattedNumber = String.format("%.1f", absNumber);
        } else {
            int roundedNumber = Math.round(absNumber);
            formattedNumber = String.valueOf(roundedNumber);
        }
        
        if (isNegative) {
            formattedNumber = "-" + formattedNumber;
        }
        
        if (enabled) {
            String[] parts = formattedNumber.split("\\.");
            String integerPart = parts[0];
            String decimalPart = parts.length > 1 ? parts[1] : "";
            
            StringBuilder result = new StringBuilder();
            int length = integerPart.length();
            
            for (int i = 0; i < length; i++) {
                if (i > 0 && (length - i) % 3 == 0 && integerPart.charAt(i) != '-') {
                    result.append(",");
                }
                result.append(integerPart.charAt(i));
            }
            
            if (!decimalPart.isEmpty()) {
                result.append(".").append(decimalPart);
            }
            
            return result.toString();
        }
        
        return formattedNumber;
    }
}
