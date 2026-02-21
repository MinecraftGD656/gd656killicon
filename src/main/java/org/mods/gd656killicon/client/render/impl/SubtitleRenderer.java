package org.mods.gd656killicon.client.render.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import com.google.gson.JsonObject;

/**
 * Renderer for the kill feed subtitle element.
 * Displays a customizable message when a kill occurs, e.g., "You killed <target> with <weapon>".
 */
public class SubtitleRenderer implements IHudRenderer {

    // ================================================================================================================
    // Constants
    // ================================================================================================================
    private static final long FADE_IN_DURATION = 250L; // 0.25s
    private static final long FADE_OUT_DURATION = 300L; // 0.3s
    private static final int DEFAULT_PLACEHOLDER_COLOR = 0xFF008B8B;

    // ================================================================================================================
    // Instance Fields
    // ================================================================================================================
    
    // Config Fields
    private int configXOffset = 0;
    private int configYOffset = 20;
    private long displayDuration = 3000L;
    private String format = "gd656killicon.client.format.normal";
    private int placeholderColor = DEFAULT_PLACEHOLDER_COLOR;
    private boolean enablePlaceholderBold = false;
    private float scale = 1.0f;

    // State Fields
    private long startTime = -1;
    private boolean isVisible = false;
    private long textHideTime = -1;
    private int currentKillType = KillType.NORMAL;
    private int victimId = -1;
    private String victimName = "";
    private ItemStack heldItem = ItemStack.EMPTY;
    private String currentWeaponName = "";

    // Delay Logic
    private long delayedTriggerTime = -1;
    private TriggerContext delayedContext = null;

    // ================================================================================================================
    // Constructor
    // ================================================================================================================
    public SubtitleRenderer() {
    }

    // ================================================================================================================
    // IHudRenderer Implementation
    // ================================================================================================================
    @Override
    public void trigger(TriggerContext context) {
        // Handle delayed trigger for DESTROY_VEHICLE
        if (context.type() == KillType.DESTROY_VEHICLE) {
            // Check if a normal kill is currently displaying and started recently (within 500ms)
            if (this.isVisible && isNormalKillType(this.currentKillType) && (System.currentTimeMillis() - this.startTime < 500)) {
                this.delayedContext = context;
                this.delayedTriggerTime = this.startTime + 500;
                return;
            }
        }

        this.currentKillType = context.type();
        this.victimId = context.entityId();
        Minecraft mc = Minecraft.getInstance();
        
        if (context.extraData() != null && !context.extraData().isEmpty()) {
            String extra = context.extraData();
            if (this.currentKillType == KillType.DESTROY_VEHICLE) {
                if (extra.contains("|")) {
                    String[] parts = extra.split("\\|", 2);
                    this.victimName = parts[0];
                } else {
                    this.victimName = extra;
                }
            } else {
                // For normal assists, extraData might be the localized victim name
                this.victimName = extra;
            }
        } else if (mc.level != null && this.victimId != -1) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(this.victimId);
            if (entity != null) {
                this.victimName = entity.getDisplayName().getString();
            } else {
                this.victimName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.unknown");
            }
        } else {
            this.victimName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.unknown");
        }

        if (mc.player != null) {
            if (mc.player.getVehicle() != null) {
                this.heldItem = ItemStack.EMPTY;
                this.currentWeaponName = mc.player.getVehicle().getDisplayName().getString();
            } else {
                this.heldItem = mc.player.getMainHandItem();
                this.currentWeaponName = this.heldItem.isEmpty() 
                    ? net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.bare_hand") 
                    : this.heldItem.getHoverName().getString();
            }
        } else {
            this.heldItem = ItemStack.EMPTY;
            this.currentWeaponName = "Unknown";
        }

        JsonObject config = ConfigManager.getElementConfig("subtitle", "kill_feed");
        if (config == null) {
            return;
        }

        boolean visible = !config.has("visible") || config.get("visible").getAsBoolean();
        if (!visible) {
            this.isVisible = false;
            return;
        }

        loadConfig(config);

        if (this.displayDuration < FADE_IN_DURATION) {
            this.displayDuration = FADE_IN_DURATION;
        }

        this.startTime = System.currentTimeMillis();
        // Calculate hide time based on start time and duration
        this.textHideTime = this.startTime + this.displayDuration;
        this.isVisible = true;
    }

    public void triggerPreview(int killType, String weaponName, String victimName) {
        this.currentKillType = killType;
        this.victimId = -1;
        this.victimName = victimName != null ? victimName : "";
        this.heldItem = ItemStack.EMPTY;
        this.currentWeaponName = weaponName != null ? weaponName : "Unknown";
        this.delayedContext = null;
        this.delayedTriggerTime = -1;

        JsonObject config = ConfigManager.getElementConfig("subtitle", "kill_feed");
        if (config == null) {
            return;
        }

        boolean visible = !config.has("visible") || config.get("visible").getAsBoolean();
        if (!visible) {
            this.isVisible = false;
            return;
        }

        loadConfig(config);

        if (this.displayDuration < FADE_IN_DURATION) {
            this.displayDuration = FADE_IN_DURATION;
        }

        this.startTime = System.currentTimeMillis();
        this.textHideTime = this.startTime + this.displayDuration;
        this.isVisible = true;
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

    private RenderState resolveRenderState() {
        if (this.delayedContext != null && System.currentTimeMillis() >= this.delayedTriggerTime) {
            trigger(this.delayedContext);
            this.delayedContext = null;
            this.delayedTriggerTime = -1;
        }

        if (!isVisible || startTime == -1) return null;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTime;

        float alpha = calculateAlpha(currentTime);
        if (alpha <= 0.05f) {
            isVisible = false;
            startTime = -1;
            return null;
        }

        float currentScale = this.scale;
        if (elapsed < FADE_IN_DURATION) {
            float progress = (float) elapsed / FADE_IN_DURATION;
            float easedProgress = 1.0f - (float) Math.pow(1.0f - progress, 3);
            currentScale = Mth.lerp(easedProgress, 1.5f, this.scale);
        }

        return new RenderState(elapsed, alpha, currentScale);
    }

    private void renderInternal(GuiGraphics guiGraphics, Font font, int centerX, int textY, RenderState state) {
        float colorProgress = getColorProgress(state.elapsed);
        Component fullText = buildFullText(colorProgress);

        int textWidth = font.width(fullText);
        int textX = centerX - textWidth / 2;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        float pivotX = textX + textWidth / 2.0f;
        float pivotY = textY + font.lineHeight / 2.0f;

        poseStack.translate(pivotX, pivotY, 0);
        poseStack.scale(state.currentScale, state.currentScale, 1.0f);
        poseStack.translate(-pivotX, -pivotY, 0);

        int alphaInt = (int) (state.alpha * 255.0f) << 24;
        int colorWithAlpha = 0x00FFFFFF | alphaInt;
        guiGraphics.drawString(font, fullText, textX, textY, colorWithAlpha, true);

        poseStack.popPose();
    }

    private static final class RenderState {
        private final long elapsed;
        private final float alpha;
        private final float currentScale;

        private RenderState(long elapsed, float alpha, float currentScale) {
            this.elapsed = elapsed;
            this.alpha = alpha;
            this.currentScale = currentScale;
        }
    }

    // ================================================================================================================
    // Private Helper Methods
    // ================================================================================================================

    /**
     * Loads configuration from the JSON object.
     * @param config The configuration JSON object.
     */
    private void loadConfig(JsonObject config) {
        try {
            this.configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
            this.configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 100;
            this.displayDuration = config.has("display_duration")
                ? (long)(config.get("display_duration").getAsFloat() * 1000)
                : 3000L;
            this.scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;

            String normalFormat = config.has("format_normal")
                    ? config.get("format_normal").getAsString()
                    : "gd656killicon.client.format.normal";
            String normalColorHex = config.has("color_normal_placeholder")
                    ? config.get("color_normal_placeholder").getAsString()
                    : "#008B8B";

            String formatKey = formatKeyForType(this.currentKillType);
            String colorKey = placeholderColorKeyForType(this.currentKillType);

            this.format = config.has(formatKey) ? config.get(formatKey).getAsString() : normalFormat;
            
            if (net.minecraft.client.resources.language.I18n.exists(this.format)) {
                this.format = net.minecraft.client.resources.language.I18n.get(this.format);
            }

            String chosenColorHex = config.has(colorKey) ? config.get(colorKey).getAsString() : normalColorHex;
            this.placeholderColor = parseColorHexOrDefault(chosenColorHex, DEFAULT_PLACEHOLDER_COLOR);
            this.enablePlaceholderBold = config.has("enable_placeholder_bold") && config.get("enable_placeholder_bold").getAsBoolean();
        } catch (Exception e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.subtitle.config_error");
            this.configXOffset = 0;
            this.configYOffset = 100;
            this.displayDuration = 3000L;
            this.scale = 1.0f;
            this.format = "gd656killicon.client.format.normal";
            if (net.minecraft.client.resources.language.I18n.exists(this.format)) {
                this.format = net.minecraft.client.resources.language.I18n.get(this.format);
            }
            this.placeholderColor = DEFAULT_PLACEHOLDER_COLOR;
            this.enablePlaceholderBold = false;
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
     * Builds the full text component by replacing placeholders and applying styles.
     * @param colorProgress Progress of the color transition (0.0 to 1.0).
     * @return The formatted text component.
     */
    private Component buildFullText(float colorProgress) {
        String weaponName = this.currentWeaponName;
        
        Component fullText = Component.empty();
        String tempFormat = this.format;

        while (!tempFormat.isEmpty()) {
            int weaponIdx = tempFormat.indexOf("<weapon>");
            int targetIdx = tempFormat.indexOf("<target>");

            if (weaponIdx == -1 && targetIdx == -1) {
                // Normal text segment: target color is white (0xFFFFFF)
                int targetColor = 0x00FFFFFF;
                int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                fullText.getSiblings().add(Component.literal(tempFormat).withStyle(style -> 
                    style.withColor(interpolatedColor & 0x00FFFFFF)));
                break;
            }

            int firstIdx;
            String placeholder;
            String replacement;
            boolean isPlaceholder;

            if (weaponIdx != -1 && (targetIdx == -1 || weaponIdx < targetIdx)) {
                firstIdx = weaponIdx;
                placeholder = "<weapon>";
                replacement = weaponName;
                isPlaceholder = true;
            } else {
                firstIdx = targetIdx;
                placeholder = "<target>";
                // Translate victim name if it's a key, otherwise return as is
                replacement = net.minecraft.client.resources.language.I18n.get(victimName);
                isPlaceholder = true;
            }

            // Normal text prefix
            if (firstIdx > 0) {
                String prefix = tempFormat.substring(0, firstIdx);
                int targetColor = 0x00FFFFFF;
                int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                fullText.getSiblings().add(Component.literal(prefix).withStyle(style -> 
                    style.withColor(interpolatedColor & 0x00FFFFFF)));
            }

            // Placeholder segment
            if (isPlaceholder) {
                int targetColor = this.placeholderColor & 0x00FFFFFF;
                int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                fullText.getSiblings().add(Component.literal(replacement).withStyle(style -> 
                    style.withColor(interpolatedColor & 0x00FFFFFF).withBold(this.enablePlaceholderBold)));
            }

            tempFormat = tempFormat.substring(firstIdx + placeholder.length());
        }
        
        return fullText;
    }

    /**
     * Returns the config key for the format string based on kill type.
     */
    private static String formatKeyForType(int killType) {
        return switch (killType) {
            case KillType.HEADSHOT -> "format_headshot";
            case KillType.EXPLOSION -> "format_explosion";
            case KillType.CRIT -> "format_crit";
            case KillType.ASSIST -> "format_assist";
            case KillType.DESTROY_VEHICLE -> "format_destroy_vehicle";
            default -> "format_normal";
        };
    }

    /**
     * Returns the config key for the placeholder color based on kill type.
     */
    private static String placeholderColorKeyForType(int killType) {
        return switch (killType) {
            case KillType.HEADSHOT -> "color_headshot_placeholder";
            case KillType.EXPLOSION -> "color_explosion_placeholder";
            case KillType.CRIT -> "color_crit_placeholder";
            case KillType.ASSIST -> "color_assist_placeholder";
            case KillType.DESTROY_VEHICLE -> "color_destroy_vehicle_placeholder";
            default -> "color_normal_placeholder";
        };
    }

    private boolean isNormalKillType(int type) {
        return type == KillType.NORMAL || type == KillType.HEADSHOT || type == KillType.EXPLOSION || type == KillType.CRIT;
    }

    /**
     * Parses a hex color string or returns a default value.
     */
    private static int parseColorHexOrDefault(String hex, int fallbackArgb) {
        if (hex == null || hex.isEmpty()) {
            return fallbackArgb;
        }
        try {
            int rgb = Integer.parseInt(hex.replace("#", ""), 16);
            return (rgb & 0x00FFFFFF) | 0xFF000000;
        } catch (NumberFormatException e) {
            return fallbackArgb;
        }
    }

    /**
     * Calculates the progress of the color transition (white to target).
     */
    private float getColorProgress(long elapsed) {
        if (elapsed < FADE_IN_DURATION) {
            return (float) elapsed / FADE_IN_DURATION;
        }
        return 1.0f;
    }

    /**
     * Interpolates color from white to target color.
     * @param targetColor Target color (ARGB).
     * @param progress Interpolation progress (0.0 to 1.0).
     * @return Interpolated color (ARGB).
     */
    private static int interpolateFromWhite(int targetColor, float progress) {
        if (progress >= 1.0f) {
            return targetColor;
        }
        int white = 0x00FFFFFF;
        int targetRGB = targetColor & 0x00FFFFFF;
        int alpha = targetColor & 0xFF000000;

        int r1 = (white >> 16) & 0xFF;
        int g1 = (white >> 8) & 0xFF;
        int b1 = white & 0xFF;
        int r2 = (targetRGB >> 16) & 0xFF;
        int g2 = (targetRGB >> 8) & 0xFF;
        int b2 = targetRGB & 0xFF;

        int r = (int)(r1 + (r2 - r1) * progress);
        int g = (int)(g1 + (g2 - g1) * progress);
        int b = (int)(b1 + (b2 - b1) * progress);

        return (alpha) | (r << 16) | (g << 8) | b;
    }
}
