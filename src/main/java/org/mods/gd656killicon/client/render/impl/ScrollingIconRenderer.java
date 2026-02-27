package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.effect.IconRingEffect;
import org.mods.gd656killicon.client.textures.ModTextures;
import org.mods.gd656killicon.client.textures.IconTextureAnimationManager;
import org.mods.gd656killicon.client.textures.IconTextureAnimationManager.TextureFrame;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import org.mods.gd656killicon.common.KillType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Renderer for the Scrolling Kill Icons.
 * <p>
 * Displays a queue of kill icons that scroll across the screen.
 * Supports animation, scaling, and configuration for icon spacing and duration.
 * </p>
 */
public class ScrollingIconRenderer implements IHudRenderer {

    
    
    

    private static final long DEFAULT_DISPLAY_DURATION = 3000L;
    private static final long DEFAULT_ANIMATION_DURATION = 300L;
    private static final long DEFAULT_POSITION_ANIMATION_DURATION = 300L;
    private static final float DEFAULT_START_SCALE = 2.0f;
    private static final int BASE_ICON_SIZE = 64;
    private static final float DEFAULT_ICON_SPACING = 8.0f;
    private static final int DEFAULT_MAX_VISIBLE_ICONS = 7;
    private static final int DEFAULT_MAX_PENDING_ICONS = 30;
    private static final long DEFAULT_DISPLAY_INTERVAL_MS = 100L;

    private static final int DEFAULT_HEADSHOT_COLOR = 0xD4B800;
    private static final int DEFAULT_EXPLOSION_COLOR = 0xF77F00;
    private static final int DEFAULT_CRIT_COLOR = 0x9CCC65;

    
    
    

    
    private float configScale = 1.0f;
    private int configXOffset = 0;
    private int configYOffset = 0;
    private long displayDuration = DEFAULT_DISPLAY_DURATION;
    private boolean enableCritRing = true;
    private boolean enableHeadshotRing = true;
    private boolean enableExplosionRing = true;
    private long animationDuration = DEFAULT_ANIMATION_DURATION;
    private long positionAnimationDuration = DEFAULT_POSITION_ANIMATION_DURATION;
    private float startScale = DEFAULT_START_SCALE;
    private float iconSpacing = DEFAULT_ICON_SPACING;
    private int maxVisibleIcons = DEFAULT_MAX_VISIBLE_ICONS;
    private long displayIntervalMs = DEFAULT_DISPLAY_INTERVAL_MS;
    private int maxPendingIcons = DEFAULT_MAX_PENDING_ICONS;
    private float ringCritRadius = 42.0f;
    private float ringCritThickness = 1.8f;
    private float ringHeadshotRadius = 42.0f;
    private float ringHeadshotThickness = 3.0f;
    private float ringExplosionRadius = 42.0f;
    private float ringExplosionThickness = 5.4f;

    
    private boolean isVisible = false;
    private final List<ScrollingIcon> activeIcons = new ArrayList<>();
    private final List<ScrollingIcon> pendingIcons = new ArrayList<>();
    private long lastIconDisplayTime = 0L;
    private boolean hasCustomCenter = false;
    private float lastCustomCenterX = 0f;
    private JsonObject currentConfig;

    
    
    

    public ScrollingIconRenderer() {
    }

    
    
    

    @Override
    public void trigger(TriggerContext context) {
        
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "scrolling");
        if (config == null) {
            return;
        }

        boolean visible = !config.has("visible") || config.get("visible").getAsBoolean();
        if (!visible) {
            this.isVisible = false;
            this.activeIcons.clear();
            return;
        }

        loadConfig(config);

        if (this.displayDuration < animationDuration) {
            this.displayDuration = animationDuration;
        }

        this.isVisible = true;
        
        ScrollingIcon icon = new ScrollingIcon(context.type(), 0, displayDuration);
        
        pendingIcons.add(icon);
        if (pendingIcons.size() > maxPendingIcons) {
            pendingIcons.remove(0);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        if (!isVisible || (activeIcons.isEmpty() && pendingIcons.isEmpty())) {
            isVisible = false;
            return;
        }

        long currentTime = System.currentTimeMillis();
        float centerX = resolveCenterX();

        processPendingIcons(currentTime, centerX);

        if (activeIcons.isEmpty()) {
            return;
        }

        
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        
        
        
        int centerY = screenHeight - configYOffset;

        boolean removedAny = false;
        Iterator<ScrollingIcon> iterator = activeIcons.iterator();
        while (iterator.hasNext()) {
            ScrollingIcon icon = iterator.next();
            long elapsed = currentTime - icon.startTime;

            updatePosition(icon, currentTime);

            float currentScale = resolveScale(elapsed);
            float alpha = resolveAlpha(icon, currentTime, elapsed);
            if (shouldRemoveIcon(icon, currentTime, elapsed)) {
                iterator.remove();
                removedAny = true;
                continue;
            }

            String texturePath = getTexturePath(icon.killType);
            String textureKey = getTextureKey(icon.killType);
            
            TextureFrame frame = IconTextureAnimationManager.getTextureFrame(
                ConfigManager.getCurrentPresetId(), 
                "kill_icon/scrolling", 
                textureKey,
                texturePath,
                icon.startTime, 
                currentConfig
            );

            float drawWidth, drawHeight;
            String prefix = "anim_" + textureKey + "_";
            boolean animEnabled = currentConfig != null && currentConfig.has(prefix + "enable_texture_animation") && currentConfig.get(prefix + "enable_texture_animation").getAsBoolean();
            
            if (animEnabled) {
                float aspectRatio = (float) frame.height / (float) Math.max(1, frame.width);
                drawWidth = BASE_ICON_SIZE;
                drawHeight = BASE_ICON_SIZE * aspectRatio;
            } else {
                float frameWidthRatio = resolveFrameRatio(textureKey, "texture_frame_width_ratio");
                float frameHeightRatio = resolveFrameRatio(textureKey, "texture_frame_height_ratio");
                drawWidth = BASE_ICON_SIZE * frameWidthRatio;
                drawHeight = BASE_ICON_SIZE * frameHeightRatio;
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(icon.currentX, centerY, 0);
            guiGraphics.pose().scale(currentScale, currentScale, 1.0f);
            guiGraphics.pose().translate(-drawWidth / 2f, -drawHeight / 2f, 0);
            guiGraphics.blit(ModTextures.get(texturePath), 0, 0, (int)drawWidth, (int)drawHeight, frame.u, frame.v, frame.width, frame.height, frame.totalWidth, frame.totalHeight);
            guiGraphics.pose().popPose();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            icon.ringEffect.render(guiGraphics, icon.currentX, centerY, currentTime);
        }

        if (removedAny) {
            updateAllIconTargetPositions(currentTime, centerX);
        }

        if (activeIcons.isEmpty() && pendingIcons.isEmpty()) {
            isVisible = false;
        }
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float originX, float originY) {
        if (!isVisible || (activeIcons.isEmpty() && pendingIcons.isEmpty())) {
            isVisible = false;
            return;
        }

        long currentTime = System.currentTimeMillis();

        syncCustomCenter(originX);
        processPendingIcons(currentTime, originX);

        if (activeIcons.isEmpty()) {
            return;
        }

        updateAllIconTargetPositions(currentTime, originX);

        boolean removedAny = false;
        Iterator<ScrollingIcon> iterator = activeIcons.iterator();
        while (iterator.hasNext()) {
            ScrollingIcon icon = iterator.next();
            long elapsed = currentTime - icon.startTime;

            updatePosition(icon, currentTime);

            float currentScale = resolveScale(elapsed);
            float alpha = resolveAlpha(icon, currentTime, elapsed);
            if (shouldRemoveIcon(icon, currentTime, elapsed)) {
                iterator.remove();
                removedAny = true;
                continue;
            }

            String texturePath = getTexturePath(icon.killType);
            String textureKey = getTextureKey(icon.killType);
            
            TextureFrame frame = IconTextureAnimationManager.getTextureFrame(
                ConfigManager.getCurrentPresetId(), 
                "kill_icon/scrolling", 
                textureKey,
                texturePath,
                icon.startTime, 
                currentConfig
            );

            float drawWidth, drawHeight;
            String prefix = "anim_" + textureKey + "_";
            boolean animEnabled = currentConfig != null && currentConfig.has(prefix + "enable_texture_animation") && currentConfig.get(prefix + "enable_texture_animation").getAsBoolean();
            
            if (animEnabled) {
                float aspectRatio = (float) frame.height / (float) Math.max(1, frame.width);
                drawWidth = BASE_ICON_SIZE;
                drawHeight = BASE_ICON_SIZE * aspectRatio;
            } else {
                float frameWidthRatio = resolveFrameRatio(textureKey, "texture_frame_width_ratio");
                float frameHeightRatio = resolveFrameRatio(textureKey, "texture_frame_height_ratio");
                drawWidth = BASE_ICON_SIZE * frameWidthRatio;
                drawHeight = BASE_ICON_SIZE * frameHeightRatio;
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(icon.currentX, originY, 0);
            guiGraphics.pose().scale(currentScale, currentScale, 1.0f);
            guiGraphics.pose().translate(-drawWidth / 2f, -drawHeight / 2f, 0);
            guiGraphics.blit(ModTextures.get(texturePath), 0, 0, (int)drawWidth, (int)drawHeight, frame.u, frame.v, frame.width, frame.height, frame.totalWidth, frame.totalHeight);
            guiGraphics.pose().popPose();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            icon.ringEffect.render(guiGraphics, icon.currentX, originY, currentTime);
        }

        if (removedAny) {
            updateAllIconTargetPositions(currentTime, originX);
        }

        if (activeIcons.isEmpty() && pendingIcons.isEmpty()) {
            isVisible = false;
        }
    }

    
    
    

    private void loadConfig(JsonObject config) {
        try {
            this.currentConfig = config;
            this.configScale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
            this.configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
            this.configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 0;
            this.displayDuration = config.has("display_duration")
                    ? (long)(config.get("display_duration").getAsFloat() * 1000)
                    : DEFAULT_DISPLAY_DURATION;
            boolean defaultRingEnable = !config.has("enable_icon_effect") || config.get("enable_icon_effect").getAsBoolean();
            this.enableCritRing = config.has("enable_ring_effect_crit") ? config.get("enable_ring_effect_crit").getAsBoolean() : defaultRingEnable;
            this.enableHeadshotRing = config.has("enable_ring_effect_headshot") ? config.get("enable_ring_effect_headshot").getAsBoolean() : defaultRingEnable;
            this.enableExplosionRing = config.has("enable_ring_effect_explosion") ? config.get("enable_ring_effect_explosion").getAsBoolean() : defaultRingEnable;
            this.animationDuration = config.has("animation_duration")
                    ? (long)(config.get("animation_duration").getAsFloat() * 1000)
                    : DEFAULT_ANIMATION_DURATION;
            this.positionAnimationDuration = config.has("position_animation_duration")
                    ? (long)(config.get("position_animation_duration").getAsFloat() * 1000)
                    : DEFAULT_POSITION_ANIMATION_DURATION;
            this.startScale = config.has("start_scale")
                    ? config.get("start_scale").getAsFloat()
                    : DEFAULT_START_SCALE;
            this.iconSpacing = config.has("icon_spacing")
                    ? config.get("icon_spacing").getAsFloat()
                    : DEFAULT_ICON_SPACING;
            this.maxVisibleIcons = config.has("max_visible_icons")
                    ? config.get("max_visible_icons").getAsInt()
                    : DEFAULT_MAX_VISIBLE_ICONS;
            this.displayIntervalMs = config.has("display_interval_ms")
                    ? config.get("display_interval_ms").getAsInt()
                    : DEFAULT_DISPLAY_INTERVAL_MS;
            this.maxPendingIcons = config.has("max_pending_icons")
                    ? config.get("max_pending_icons").getAsInt()
                    : DEFAULT_MAX_PENDING_ICONS;
            this.ringCritRadius = resolveRingFloat(config, "ring_effect_crit_radius", "ring_effect_normal_radius", 42.0f);
            this.ringCritThickness = resolveRingFloat(config, "ring_effect_crit_thickness", "ring_effect_normal_thickness", 1.8f);
            this.ringHeadshotRadius = config.has("ring_effect_headshot_radius")
                    ? config.get("ring_effect_headshot_radius").getAsFloat()
                    : 42.0f;
            this.ringHeadshotThickness = config.has("ring_effect_headshot_thickness")
                    ? config.get("ring_effect_headshot_thickness").getAsFloat()
                    : 3.0f;
            this.ringExplosionRadius = config.has("ring_effect_explosion_radius")
                    ? config.get("ring_effect_explosion_radius").getAsFloat()
                    : 42.0f;
            this.ringExplosionThickness = config.has("ring_effect_explosion_thickness")
                    ? config.get("ring_effect_explosion_thickness").getAsFloat()
                    : 5.4f;

        } catch (Exception e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.scrolling.config_error");
            this.currentConfig = null;
            this.configScale = 1.0f;
            this.configXOffset = 0;
            this.configYOffset = 0;
            this.displayDuration = DEFAULT_DISPLAY_DURATION;
            this.enableCritRing = true;
            this.enableHeadshotRing = true;
            this.enableExplosionRing = true;
            this.animationDuration = DEFAULT_ANIMATION_DURATION;
            this.positionAnimationDuration = DEFAULT_POSITION_ANIMATION_DURATION;
            this.startScale = DEFAULT_START_SCALE;
            this.iconSpacing = DEFAULT_ICON_SPACING;
            this.maxVisibleIcons = DEFAULT_MAX_VISIBLE_ICONS;
            this.displayIntervalMs = DEFAULT_DISPLAY_INTERVAL_MS;
            this.maxPendingIcons = DEFAULT_MAX_PENDING_ICONS;
            this.ringCritRadius = 42.0f;
            this.ringCritThickness = 1.8f;
            this.ringHeadshotRadius = 42.0f;
            this.ringHeadshotThickness = 3.0f;
            this.ringExplosionRadius = 42.0f;
            this.ringExplosionThickness = 5.4f;
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

    private void processPendingIcons(long currentTime, float centerX) {
        while (!pendingIcons.isEmpty() && currentTime - lastIconDisplayTime >= displayIntervalMs) {
            ScrollingIcon nextIcon = pendingIcons.remove(0);
            nextIcon.startTime = currentTime;
            lastIconDisplayTime = currentTime;

            boolean ringEnabled = isRingEnabledForKillType(nextIcon.killType);
            if (ringEnabled) {
                nextIcon.ringEffect.setRingParams(
                        ringCritRadius,
                        ringCritThickness,
                        ringHeadshotRadius,
                        ringHeadshotThickness,
                        ringExplosionRadius,
                        ringExplosionThickness
                );
                nextIcon.ringEffect.trigger(
                        currentTime,
                        true,
                        nextIcon.killType,
                        resolveHeadshotEffectRgb(),
                        resolveExplosionEffectRgb(),
                        resolveCritEffectRgb()
                );
            } else {
                nextIcon.ringEffect.trigger(currentTime, false, nextIcon.killType, 0, 0, 0);
            }

            addIcon(nextIcon, currentTime, centerX);
        }
    }

    private void addIcon(ScrollingIcon icon, long currentTime, float centerX) {
        activeIcons.add(icon);
        updateAllIconTargetPositions(currentTime, centerX);
        icon.prevX = icon.targetX;
        icon.currentX = icon.targetX;
        icon.positionAnimationStart = currentTime;
    }

    private void updateAllIconTargetPositions(long currentTime, float centerX) {
        if (activeIcons.isEmpty()) {
            return;
        }

        float spacing = resolveIconSpacing();
        int size = activeIcons.size();
        int visibleStart = Math.max(0, size - maxVisibleIcons);
        int visibleCount = size - visibleStart;
        float rightmostSlotX = centerX + ((visibleCount - 1) / 2f) * spacing;

        for (int i = 0; i < visibleStart; i++) {
            ScrollingIcon icon = activeIcons.get(i);
            float overflowX = rightmostSlotX + (visibleStart - i) * spacing;
            updateTarget(icon, overflowX, currentTime);
            if (icon.forcedFadeStartTime < 0) {
                icon.forcedFadeStartTime = currentTime;
            }
        }

        for (int i = visibleStart; i < size; i++) {
            ScrollingIcon icon = activeIcons.get(i);
            float position = (i - visibleStart) - (visibleCount - 1) / 2f;
            float newTargetX = centerX - position * spacing;
            updateTarget(icon, newTargetX, currentTime);
        }
    }

    private void updateTarget(ScrollingIcon icon, float newTargetX, long currentTime) {
        if (Math.abs(icon.targetX - newTargetX) > 0.1f) {
            icon.prevX = icon.currentX;
            icon.targetX = newTargetX;
            icon.positionAnimationStart = currentTime;
        }
    }

    private float resolveCenterX() {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        return screenWidth / 2f + configXOffset;
    }

    private float resolveIconSpacing() {
        float baseSize = BASE_ICON_SIZE * configScale;
        return baseSize + iconSpacing;
    }

    private void updatePosition(ScrollingIcon icon, long currentTime) {
        if (Math.abs(icon.currentX - icon.targetX) <= 0.1f) {
            return;
        }
        long moveElapsed = currentTime - icon.positionAnimationStart;
        float progress = Math.min(moveElapsed / (float) positionAnimationDuration, 1.0f);
        float easedProgress = 1.0f - (1.0f - progress) * (1.0f - progress);
        icon.currentX = Mth.lerp(easedProgress, icon.prevX, icon.targetX);
    }

    private float resolveScale(long elapsed) {
        float endScale = 1.0f * configScale;
        if (elapsed >= animationDuration) {
            return endScale;
        }
        float initialScale = startScale * configScale;
        float progress = (float) elapsed / animationDuration;
        progress = 1.0f - (float) Math.pow(1.0f - progress, 3);
        return Mth.lerp(progress, initialScale, endScale);
    }

    private float resolveAlpha(ScrollingIcon icon, long currentTime, long elapsed) {
        long fadeDuration = Math.max(1L, animationDuration);
        float fadeInProgress = Math.min(elapsed / (float) fadeDuration, 1.0f);
        float easedIn = 1.0f - (float) Math.pow(1.0f - fadeInProgress, 3);
        float baseAlpha = Mth.clamp(easedIn, 0.0f, 1.0f);
        
        if (icon.forcedFadeStartTime >= 0) {
            long fadeElapsed = currentTime - icon.forcedFadeStartTime;
            float fadeProgress = (float) fadeElapsed / (float) fadeDuration;
            float alpha = 1.0f - fadeProgress;
            return Mth.clamp(baseAlpha * alpha, 0.0f, 1.0f);
        }
        
        if (elapsed <= icon.displayDuration) {
            return baseAlpha;
        }
        
        long fadeElapsed = elapsed - icon.displayDuration;
        float fadeProgress = (float) fadeElapsed / (float) fadeDuration;
        float alpha = 1.0f - fadeProgress;
        return Mth.clamp(baseAlpha * alpha, 0.0f, 1.0f);
    }

    private boolean shouldRemoveIcon(ScrollingIcon icon, long currentTime, long elapsed) {
        long fadeDuration = Math.max(1L, animationDuration);
        if (icon.forcedFadeStartTime >= 0) {
            long fadeElapsed = currentTime - icon.forcedFadeStartTime;
            return fadeElapsed >= fadeDuration;
        }
        return elapsed >= icon.displayDuration + fadeDuration;
    }

    private String getTexturePath(int killType) {
        String textureKey = getTextureKey(killType);
        return ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/scrolling",
            textureKey,
            currentConfig
        );
    }

    private String getTextureKey(int killType) {
        return switch (killType) {
            case KillType.HEADSHOT -> "headshot";
            case KillType.EXPLOSION -> "explosion";
            case KillType.CRIT -> "crit";
            case KillType.ASSIST -> "assist";
            case KillType.DESTROY_VEHICLE -> "destroy_vehicle";
            case KillType.NORMAL -> "default";
            default -> "default";
        };
    }

    private int resolveHeadshotEffectRgb() {
        return resolveEffectRgb("ring_effect_headshot_color", DEFAULT_HEADSHOT_COLOR);
    }

    private int resolveExplosionEffectRgb() {
        return resolveEffectRgb("ring_effect_explosion_color", DEFAULT_EXPLOSION_COLOR);
    }

    private int resolveCritEffectRgb() {
        if (currentConfig != null) {
            if (currentConfig.has("ring_effect_crit_color")) {
                return resolveEffectRgb("ring_effect_crit_color", DEFAULT_CRIT_COLOR);
            }
            if (currentConfig.has("ring_effect_normal_color")) {
                return resolveEffectRgb("ring_effect_normal_color", DEFAULT_CRIT_COLOR);
            }
        }
        return DEFAULT_CRIT_COLOR;
    }

    private int resolveEffectRgb(String key, int defaultValue) {
        if (currentConfig == null) {
            return defaultValue;
        }
        String hex = currentConfig.has(key) ? currentConfig.get(key).getAsString() : null;
        return parseRgbHexOrDefault(hex, defaultValue);
    }

    private float resolveRingFloat(JsonObject config, String key, String legacyKey, float defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        if (config.has(key)) {
            return config.get(key).getAsFloat();
        }
        if (legacyKey != null && config.has(legacyKey)) {
            return config.get(legacyKey).getAsFloat();
        }
        return defaultValue;
    }

    private boolean isRingEnabledForKillType(int killType) {
        return switch (killType) {
            case KillType.HEADSHOT -> enableHeadshotRing;
            case KillType.EXPLOSION -> enableExplosionRing;
            case KillType.CRIT -> enableCritRing;
            default -> false;
        };
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

    private void syncCustomCenter(float centerX) {
        if (!hasCustomCenter) {
            hasCustomCenter = true;
            lastCustomCenterX = centerX;
            return;
        }
        float delta = centerX - lastCustomCenterX;
        if (Math.abs(delta) <= 0.1f) {
            return;
        }
        lastCustomCenterX = centerX;
        if (activeIcons.isEmpty()) {
            return;
        }
        for (ScrollingIcon icon : activeIcons) {
            icon.prevX += delta;
            icon.currentX += delta;
            icon.targetX += delta;
        }
    }

    
    
    

    /**
     * Represents a single icon in the scrolling queue.
     */
    private static final class ScrollingIcon {
        private final int killType;
        private long startTime;
        private final long displayDuration;
        private final IconRingEffect ringEffect = new IconRingEffect();

        private float prevX;
        private float currentX;
        private float targetX;
        private long positionAnimationStart;
        private long forcedFadeStartTime = -1L;

        private ScrollingIcon(int killType, long startTime, long displayDuration) {
            this.killType = killType;
            this.startTime = startTime;
            this.displayDuration = displayDuration;
        }
    }
}
