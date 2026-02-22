package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.effect.IconRingEffect;
import org.mods.gd656killicon.client.textures.ModTextures;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import org.mods.gd656killicon.common.KillType;

/**
 * Renderer for the Combo Kill Icon.
 * <p>
 * Displays a combo kill icon with a ring progress effect.
 * Handles configuration loading, animation, and rendering logic.
 * </p>
 */
public class ComboIconRenderer implements IHudRenderer {

    // ================================================================================================================
    // Constants
    // ================================================================================================================

    private static final long DEFAULT_DISPLAY_DURATION = 1500L;
    private static final long ANIMATION_DURATION = 300L;
    private static final float START_SCALE = 2.0f;
    private static final int DEFAULT_HEADSHOT_COLOR = 0xD4B800;
    private static final int DEFAULT_EXPLOSION_COLOR = 0xF77F00;
    private static final int DEFAULT_CRIT_COLOR = 0x9CCC65;

    // ================================================================================================================
    // Static Fields
    // ================================================================================================================

    private static volatile long serverComboWindowMs = -1L;

    // ================================================================================================================
    // Instance Fields
    // ================================================================================================================

    // Config Fields
    private float configScale = 1.0f;
    private int configXOffset = 0;
    private int configYOffset = 0;
    private long displayDuration = DEFAULT_DISPLAY_DURATION;
    private boolean enableIconEffect = IconRingEffect.DEFAULT_ENABLE_ICON_EFFECT;
    private JsonObject currentConfig;

    // State Fields
    private long startTime = -1;
    private boolean isVisible = false;
    private long effectiveDisplayDuration = DEFAULT_DISPLAY_DURATION;
    private int comboCount = 0;
    private int currentKillType = KillType.NORMAL;
    private final IconRingEffect ringEffect = new IconRingEffect();

    // ================================================================================================================
    // Constructor & Static Access
    // ================================================================================================================

    /**
     * Default constructor.
     */
    public ComboIconRenderer() {
    }

    /**
     * Updates the server-side combo window duration.
     *
     * @param seconds The combo window duration in seconds.
     */
    public static void updateServerComboWindowSeconds(double seconds) {
        if (seconds <= 0) {
            return;
        }
        serverComboWindowMs = Math.max(100L, (long) (seconds * 1000.0));
    }

    public static long getServerComboWindowMs() {
        return serverComboWindowMs;
    }

    // ================================================================================================================
    // IHudRenderer Implementation
    // ================================================================================================================

    @Override
    public void trigger(TriggerContext context) {
        this.currentKillType = context.type();
        this.comboCount = context.comboCount();

        JsonObject config = ConfigManager.getElementConfig("kill_icon", "combo");
        if (config == null) {
            return;
        }

        boolean visible = !config.has("visible") || config.get("visible").getAsBoolean();
        if (!visible) {
            this.isVisible = false;
            return;
        }

        loadConfig(config);

        if (this.displayDuration < ANIMATION_DURATION) {
            this.displayDuration = ANIMATION_DURATION;
        }
        this.effectiveDisplayDuration = this.displayDuration + ANIMATION_DURATION;

        this.startTime = System.currentTimeMillis();
        this.isVisible = true;

        triggerRingEffect();
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        if (!isVisible || startTime == -1) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float centerX = screenWidth / 2f + configXOffset;
        float centerY = screenHeight - configYOffset;
        renderAt(guiGraphics, partialTick, centerX, centerY);
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        if (!isVisible || startTime == -1) return;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTime;

        if (elapsed > effectiveDisplayDuration) {
            isVisible = false;
            startTime = -1;
            return;
        }

        float endScale = 1.0f * configScale;
        float initialScale = START_SCALE * configScale;
        float currentScale = endScale;
        float alpha = 1.0f;

        if (elapsed < ANIMATION_DURATION) {
            float progress = (float) elapsed / ANIMATION_DURATION;
            progress = 1.0f - (float) Math.pow(1.0f - progress, 3);
            currentScale = Mth.lerp(progress, initialScale, endScale);
            alpha = Mth.clamp(progress, 0.0f, 1.0f);
        }

        if (elapsed > displayDuration) {
            long fadeElapsed = elapsed - displayDuration;
            float fadeProgress = (float) fadeElapsed / (float) ANIMATION_DURATION;
            float fadeAlpha = 1.0f - fadeProgress;
            alpha = Mth.clamp(alpha * fadeAlpha, 0.0f, 1.0f);
        }

        int displayCombo = Mth.clamp(this.comboCount, 1, 6);
        String texturePath = "killicon_combo_" + displayCombo + ".png";
        String textureKey = "combo_" + displayCombo;
        float frameWidthRatio = resolveFrameRatio(textureKey, "texture_frame_width_ratio");
        float frameHeightRatio = resolveFrameRatio(textureKey, "texture_frame_height_ratio");
        float drawWidth = 64.0f * frameWidthRatio;
        float drawHeight = 64.0f * frameHeightRatio;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        try {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY, 0);
            guiGraphics.pose().scale(currentScale, currentScale, 1.0f);
            guiGraphics.pose().translate(-drawWidth / 2.0f, -drawHeight / 2.0f, 0);
            guiGraphics.blit(ModTextures.get(texturePath), 0, 0, 0, 0, (int) drawWidth, (int) drawHeight, (int) drawWidth, (int) drawHeight);
            guiGraphics.pose().popPose();
            ringEffect.render(guiGraphics, centerX, centerY, currentTime);
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    // ================================================================================================================
    // Private Helper Methods
    // ================================================================================================================

    /**
     * Loads configuration values from the JsonObject.
     *
     * @param config The configuration object.
     */
    private void loadConfig(JsonObject config) {
        try {
            this.currentConfig = config;
            this.configScale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
            this.configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
            this.configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 0;
            this.displayDuration = resolveDisplayDuration(config);
            this.enableIconEffect = !config.has("enable_icon_effect") || config.get("enable_icon_effect").getAsBoolean();
        } catch (Exception e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.combo.config_error");
            this.currentConfig = null;
            this.configScale = 1.0f;
            this.configXOffset = 0;
            this.configYOffset = 0;
            this.displayDuration = resolveDisplayDuration(null);
            this.enableIconEffect = IconRingEffect.DEFAULT_ENABLE_ICON_EFFECT;
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
        int value = currentConfig.get(key).getAsInt();
        return value > 0 ? value : 1.0f;
    }

    /**
     * Resolves the display duration, prioritizing server settings.
     *
     * @param config The configuration object.
     * @return The resolved display duration in milliseconds.
     */
    private long resolveDisplayDuration(JsonObject config) {
        long serverDuration = serverComboWindowMs;
        if (serverDuration > 0) {
            return serverDuration;
        }
        if (config != null && config.has("display_duration")) {
            return (long) (config.get("display_duration").getAsFloat() * 1000);
        }
        return DEFAULT_DISPLAY_DURATION;
    }

    /**
     * Triggers the ring effect animation.
     */
    private void triggerRingEffect() {
        if (this.enableIconEffect && isSpecialKillType(this.currentKillType)) {
            ringEffect.trigger(
                    this.startTime,
                    true,
                    this.currentKillType,
                    resolveHeadshotEffectRgb(),
                    resolveExplosionEffectRgb(),
                    resolveCritEffectRgb()
            );
        } else {
            ringEffect.trigger(this.startTime, false, this.currentKillType, 0, 0, 0);
        }
    }

    private boolean isSpecialKillType(int type) {
        return type == KillType.HEADSHOT || type == KillType.EXPLOSION || type == KillType.CRIT;
    }

    private static int resolveHeadshotEffectRgb() {
        return resolveEffectRgb("color_headshot_placeholder", "color_normal_placeholder", DEFAULT_HEADSHOT_COLOR);
    }

    private static int resolveExplosionEffectRgb() {
        return resolveEffectRgb("color_explosion_placeholder", null, DEFAULT_EXPLOSION_COLOR);
    }

    private static int resolveCritEffectRgb() {
        return resolveEffectRgb("color_crit_placeholder", null, DEFAULT_CRIT_COLOR);
    }

    private static int resolveEffectRgb(String primaryKey, String secondaryKey, int defaultValue) {
        JsonObject subtitleConfig = ConfigManager.getElementConfig("subtitle", "kill_feed");
        if (subtitleConfig == null) {
            return defaultValue;
        }
        String hex = subtitleConfig.has(primaryKey)
                ? subtitleConfig.get(primaryKey).getAsString()
                : (secondaryKey != null && subtitleConfig.has(secondaryKey) ? subtitleConfig.get(secondaryKey).getAsString() : null);
        return parseRgbHexOrDefault(hex, defaultValue);
    }

    private static int parseRgbHexOrDefault(String hex, int fallbackRgb) {
        if (hex == null || hex.isEmpty()) {
            return fallbackRgb;
        }
        try {
            int rgb = Integer.parseInt(hex.replace("#", ""), 16);
            return rgb & 0x00FFFFFF;
        } catch (NumberFormatException e) {
            return fallbackRgb;
        }
    }
}
