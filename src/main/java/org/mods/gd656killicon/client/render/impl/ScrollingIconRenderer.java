package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.gui.tabs.PreviewTextureFocusContext;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.PreviewRenderTimeContext;
import org.mods.gd656killicon.client.render.effect.IconEntranceBackground;
import org.mods.gd656killicon.client.render.effect.IconGlowRenderEffect;
import org.mods.gd656killicon.client.render.effect.IconRingEffect;
import org.mods.gd656killicon.client.textures.ModTextures;
import org.mods.gd656killicon.client.textures.IconTextureAnimationManager;
import org.mods.gd656killicon.client.textures.IconTextureAnimationManager.TextureFrame;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.common.killtype.KillTypeDefinition;
import org.mods.gd656killicon.common.killtype.KillTypeRegistry;

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

    private static final ScrollingIconRenderer INSTANCE = new ScrollingIconRenderer();

    public static ScrollingIconRenderer getInstance() {
        return INSTANCE;
    }

    /** 联动查询前确保配置已加载(直接进游戏时图标未触发, render/trigger 未跑过 loadConfig, configYOffset 停留在默认值) */
    private boolean linkageConfigLoaded = false;

    private void ensureConfigLoaded() {
        if (!this.linkageConfigLoaded) {
            JsonObject config = ConfigManager.getElementConfig("kill_icon", "scrolling");
            if (config != null) {
                loadConfig(config);
            }
            this.linkageConfigLoaded = true;
        }
    }

    /**
     * 击杀图标队列当前是否有图标显示(队列联动用)。
     */
    public boolean hasVisibleIcons() {
        return !activeIcons.isEmpty() || !pendingIcons.isEmpty();
    }

    /**
     * 击杀图标队列的锚点 y(渲染中心, 队列联动用, 与 render 中 centerY 计算一致)。
     */
    public float getIconsAnchorY() {
        ensureConfigLoaded();
        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        return screenHeight - configYOffset;
    }


    private static final long DEFAULT_DISPLAY_DURATION = 3000L;
    private static final long DEFAULT_ANIMATION_DURATION = 300L;
    private static final long DEFAULT_FADE_OUT_DURATION_MS = 100L; // fade_out_duration 默认 0.1s
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
    private int configScrollDirection = 1;
    private boolean configPinNewestIcon = false;
    private boolean configBlinkFadeAnimation = false;
    private boolean configEntranceBackground = false;
    private float configEntranceBgSize = 64.0f;
    private long configEntranceBgFadeInMs = 200L;
    private long configEntranceBgFadeOutMs = 200L;
    private int configEntranceBgColor = 0xFFFFFF;
    private int configEntranceBgHeadshotColor = 0xFF5000;
    private float configEntranceBgPeakTransparency = 0.2f;
    private float configEntranceBgBorder = 0.5f;
    private int configEntranceBgBorderColor = 0xFFFFFF;
    private int configEntranceBgHeadshotBorderColor = 0xFF4300;
    private float configEntranceBgBorderAlpha = 0.2f;
    private long displayDuration = DEFAULT_DISPLAY_DURATION;
    /** ring 效果开关（KillTypeRegistry.ringEnableKey 驱动；无开关键的类型无条目） */
    private final java.util.Map<String, Boolean> ringEnableFlags = new java.util.HashMap<>();
    private long animationDuration = DEFAULT_ANIMATION_DURATION;
    private long fadeOutDurationMs = DEFAULT_FADE_OUT_DURATION_MS;
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
    private boolean configIconGlowEnabled = false;
    private int configIconGlowColor = 0xFFFFFF;
    private float configIconGlowIntensity = 0.45f;
    private float configIconGlowSize = 4.0f;

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

        long currentTime = PreviewRenderTimeContext.currentTimeMillis();
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
            // 入场背景启用时主图标入场推迟 0.2s(生命周期相应顺延, 淡出/移除判定同步推迟)
            long animElapsed = configEntranceBackground ? Math.max(0L, elapsed - configEntranceBgFadeInMs) : elapsed;

            updatePosition(icon, currentTime);

            float currentScale = resolveScale(animElapsed);
            float alpha = resolveAlpha(icon, currentTime, animElapsed);
            if (shouldRemoveIcon(icon, currentTime, animElapsed)) {
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

            if (configEntranceBackground) {
                // 爆头击杀使用独立颜色(矩形/边框)
                boolean headshot = icon.killType == KillType.HEADSHOT;
                int rectColor = headshot ? configEntranceBgHeadshotColor : configEntranceBgColor;
                int borderColor = headshot ? configEntranceBgHeadshotBorderColor : configEntranceBgBorderColor;
                // 入场背景矩形(背景层, 在图标之前绘制)
                IconEntranceBackground.drawRect(guiGraphics, elapsed, icon.currentX, centerY,
                        configEntranceBgSize, rectColor,
                        configEntranceBgFadeInMs, configEntranceBgFadeOutMs,
                        configEntranceBgPeakTransparency);
                // 入场背景边框(与矩形同处背景层, 位于主图标之下, 透明度与矩形同一状态机)
                IconEntranceBackground.drawBorder(guiGraphics, elapsed, icon.currentX, centerY,
                        configEntranceBgSize, borderColor,
                        configEntranceBgFadeInMs, configEntranceBgFadeOutMs,
                        configEntranceBgBorder, configEntranceBgBorderAlpha);
            }

            float focusedAlpha = alpha * PreviewTextureFocusContext.alphaMultiplier("kill_icon/scrolling", textureKey);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, focusedAlpha);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(icon.currentX, centerY, 0);
            guiGraphics.pose().scale(currentScale, currentScale, 1.0f);
            guiGraphics.pose().translate(-drawWidth / 2f, -drawHeight / 2f, 0);
            if (configIconGlowEnabled) {
                // 闪烁开启时发光层按 alpha 平方衰减: 半透明阶段发光急剧减弱, 放大闪烁对比
                // (发光为加法混合, 会让半透明阶段过亮导致闪烁不明显)
                float glowFactor = configBlinkFadeAnimation ? focusedAlpha * focusedAlpha : focusedAlpha;
                IconGlowRenderEffect.drawGlowFrame(
                    guiGraphics,
                    ModTextures.get(texturePath),
                    0,
                    0,
                    (int) drawWidth,
                    (int) drawHeight,
                    frame.u,
                    frame.v,
                    frame.width,
                    frame.height,
                    frame.totalWidth,
                    frame.totalHeight,
                    glowFactor,
                    configIconGlowColor,
                    configIconGlowIntensity,
                    configIconGlowSize
                );
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, focusedAlpha);
            }
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

        long currentTime = PreviewRenderTimeContext.currentTimeMillis();

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
            // 入场背景启用时主图标入场推迟 0.2s(生命周期相应顺延, 淡出/移除判定同步推迟)
            long animElapsed = configEntranceBackground ? Math.max(0L, elapsed - configEntranceBgFadeInMs) : elapsed;

            updatePosition(icon, currentTime);

            float currentScale = resolveScale(animElapsed);
            float alpha = resolveAlpha(icon, currentTime, animElapsed);
            if (shouldRemoveIcon(icon, currentTime, animElapsed)) {
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

            if (configEntranceBackground) {
                // 爆头击杀使用独立颜色(矩形/边框)
                boolean headshot = icon.killType == KillType.HEADSHOT;
                int rectColor = headshot ? configEntranceBgHeadshotColor : configEntranceBgColor;
                int borderColor = headshot ? configEntranceBgHeadshotBorderColor : configEntranceBgBorderColor;
                // 入场背景矩形(背景层, 在图标之前绘制)
                IconEntranceBackground.drawRect(guiGraphics, elapsed, icon.currentX, originY,
                        configEntranceBgSize, rectColor,
                        configEntranceBgFadeInMs, configEntranceBgFadeOutMs,
                        configEntranceBgPeakTransparency);
                // 入场背景边框(与矩形同处背景层, 位于主图标之下, 透明度与矩形同一状态机)
                IconEntranceBackground.drawBorder(guiGraphics, elapsed, icon.currentX, originY,
                        configEntranceBgSize, borderColor,
                        configEntranceBgFadeInMs, configEntranceBgFadeOutMs,
                        configEntranceBgBorder, configEntranceBgBorderAlpha);
            }

            float focusedAlpha = alpha * PreviewTextureFocusContext.alphaMultiplier("kill_icon/scrolling", textureKey);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, focusedAlpha);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(icon.currentX, originY, 0);
            guiGraphics.pose().scale(currentScale, currentScale, 1.0f);
            guiGraphics.pose().translate(-drawWidth / 2f, -drawHeight / 2f, 0);
            if (configIconGlowEnabled) {
                // 闪烁开启时发光层按 alpha 平方衰减: 半透明阶段发光急剧减弱, 放大闪烁对比
                // (发光为加法混合, 会让半透明阶段过亮导致闪烁不明显)
                float glowFactor = configBlinkFadeAnimation ? focusedAlpha * focusedAlpha : focusedAlpha;
                IconGlowRenderEffect.drawGlowFrame(
                    guiGraphics,
                    ModTextures.get(texturePath),
                    0,
                    0,
                    (int) drawWidth,
                    (int) drawHeight,
                    frame.u,
                    frame.v,
                    frame.width,
                    frame.height,
                    frame.totalWidth,
                    frame.totalHeight,
                    glowFactor,
                    configIconGlowColor,
                    configIconGlowIntensity,
                    configIconGlowSize
                );
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, focusedAlpha);
            }
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
            // 图标滚动方向: 左 = 新图标出现在老图标左侧(默认); 右 = 完全镜像
            this.configScrollDirection = config.has("scroll_direction") && "right".equals(config.get("scroll_direction").getAsString()) ? -1 : 1;
            // 固定最新图标: 启用后最新图标不随队列移动(保持在屏幕上的相对位置)
            this.configPinNewestIcon = config.has("pin_newest_icon") && config.get("pin_newest_icon").getAsBoolean();
            // 闪动淡出动画: 启用后图标隐藏时按三段曲线闪烁(0→50%→0%→100% 透明度)
            this.configBlinkFadeAnimation = config.has("blink_fade_animation") && config.get("blink_fade_animation").getAsBoolean();
            // 图标入场背景: 启用后主图标入场推迟(时长为背景入场持续时间), 期间先显示渐显/渐隐缩小的背景矩形
            this.configEntranceBackground = config.has("entrance_background") && config.get("entrance_background").getAsBoolean();
            this.configEntranceBgSize = config.has("entrance_background_size") ? config.get("entrance_background_size").getAsFloat() : 64.0f;
            this.configEntranceBgFadeInMs = config.has("entrance_background_fade_in")
                    ? Math.max(1L, (long) (config.get("entrance_background_fade_in").getAsFloat() * 1000))
                    : 200L;
            this.configEntranceBgFadeOutMs = config.has("entrance_background_fade_out")
                    ? Math.max(1L, (long) (config.get("entrance_background_fade_out").getAsFloat() * 1000))
                    : 200L;
            this.configEntranceBgColor = config.has("entrance_background_color")
                    ? IconEntranceBackground.parseColor(config.get("entrance_background_color").getAsString(), 0xFFFFFF)
                    : 0xFFFFFF;
            this.configEntranceBgPeakTransparency = config.has("entrance_background_alpha")
                    ? Mth.clamp(config.get("entrance_background_alpha").getAsFloat(), 0.0f, 1.0f)
                    : 0.2f;
            this.configEntranceBgBorder = config.has("entrance_background_border")
                    ? Math.max(0.0f, config.get("entrance_background_border").getAsFloat())
                    : 0.5f;
            this.configEntranceBgBorderColor = config.has("entrance_background_border_color")
                    ? IconEntranceBackground.parseColor(config.get("entrance_background_border_color").getAsString(), 0xFFFFFF)
                    : 0xFFFFFF;
            this.configEntranceBgBorderAlpha = config.has("entrance_background_border_alpha")
                    ? Mth.clamp(config.get("entrance_background_border_alpha").getAsFloat(), 0.0f, 1.0f)
                    : 0.2f;
            this.configEntranceBgHeadshotColor = config.has("entrance_background_headshot_color")
                    ? IconEntranceBackground.parseColor(config.get("entrance_background_headshot_color").getAsString(), 0xFF5000)
                    : 0xFF5000;
            this.configEntranceBgHeadshotBorderColor = config.has("entrance_background_headshot_border_color")
                    ? IconEntranceBackground.parseColor(config.get("entrance_background_headshot_border_color").getAsString(), 0xFF4300)
                    : 0xFF4300;
            this.displayDuration = config.has("display_duration")
                    ? (long)(config.get("display_duration").getAsFloat() * 1000)
                    : DEFAULT_DISPLAY_DURATION;
            boolean defaultRingEnable = !config.has("enable_icon_effect") || config.get("enable_icon_effect").getAsBoolean();
            this.ringEnableFlags.put("enable_ring_effect_crit", config.has("enable_ring_effect_crit") ? config.get("enable_ring_effect_crit").getAsBoolean() : defaultRingEnable);
            this.ringEnableFlags.put("enable_ring_effect_headshot", config.has("enable_ring_effect_headshot") ? config.get("enable_ring_effect_headshot").getAsBoolean() : defaultRingEnable);
            this.ringEnableFlags.put("enable_ring_effect_explosion", config.has("enable_ring_effect_explosion") ? config.get("enable_ring_effect_explosion").getAsBoolean() : defaultRingEnable);
            this.animationDuration = config.has("animation_duration")
                    ? (long)(config.get("animation_duration").getAsFloat() * 1000)
                    : DEFAULT_ANIMATION_DURATION;
            // 淡出动画时间: 由 fade_out_duration 配置项控制(秒 → 毫秒), 不再复用 animation_duration
            this.fadeOutDurationMs = config.has("fade_out_duration")
                    ? Math.max(1L, (long)(config.get("fade_out_duration").getAsFloat() * 1000))
                    : DEFAULT_FADE_OUT_DURATION_MS;
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
            this.configIconGlowEnabled = IconGlowRenderEffect.isEnabled(config);
            this.configIconGlowColor = IconGlowRenderEffect.resolveColor(config);
            this.configIconGlowIntensity = IconGlowRenderEffect.resolveIntensity(config);
            this.configIconGlowSize = IconGlowRenderEffect.resolveSize(config);

        } catch (Exception e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.scrolling.config_error");
            this.currentConfig = null;
            this.configScale = 1.0f;
            this.configXOffset = 0;
            this.configYOffset = 0;
            this.displayDuration = DEFAULT_DISPLAY_DURATION;
            this.ringEnableFlags.clear();
            this.ringEnableFlags.put("enable_ring_effect_crit", true);
            this.ringEnableFlags.put("enable_ring_effect_headshot", true);
            this.ringEnableFlags.put("enable_ring_effect_explosion", true);
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
            this.configIconGlowEnabled = false;
            this.configIconGlowColor = 0xFFFFFF;
            this.configIconGlowIntensity = 0.45f;
            this.configIconGlowSize = 4.0f;
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

        int direction = configScrollDirection; // 1 = 左(默认, 新图标在左), -1 = 右(镜像, 新图标在右)
        float spacing = resolveIconSpacing();
        int size = activeIcons.size();
        int visibleStart = Math.max(0, size - maxVisibleIcons);
        int visibleCount = size - visibleStart;

        if (configPinNewestIcon) {
            // 固定最新图标: 最新图标直接定位在元素配置坐标处(centerX = screenWidth/2 + configXOffset,
            // y 沿用 screenHeight - configYOffset), x_offset 语义与自由模式一致(中心偏移);
            // 老图标从固定位向溢出方向依次排开, 溢出区继续滚动淡出。
            float newestFixedX = centerX;
            int newestIndex = size - 1;
            for (int i = 0; i < visibleStart; i++) {
                ScrollingIcon icon = activeIcons.get(i);
                float overflowX = newestFixedX + direction * (newestIndex - i) * spacing;
                updateTarget(icon, overflowX, currentTime);
                if (icon.forcedFadeStartTime < 0) {
                    icon.forcedFadeStartTime = currentTime;
                }
            }
            for (int i = visibleStart; i < size; i++) {
                ScrollingIcon icon = activeIcons.get(i);
                float newTargetX = newestFixedX + direction * (newestIndex - i) * spacing;
                updateTarget(icon, newTargetX, currentTime);
            }
            return;
        }

        float rightmostSlotX = centerX + direction * ((visibleCount - 1) / 2f) * spacing;

        for (int i = 0; i < visibleStart; i++) {
            ScrollingIcon icon = activeIcons.get(i);
            float overflowX = rightmostSlotX + direction * (visibleStart - i) * spacing;
            updateTarget(icon, overflowX, currentTime);
            if (icon.forcedFadeStartTime < 0) {
                icon.forcedFadeStartTime = currentTime;
            }
        }

        for (int i = visibleStart; i < size; i++) {
            ScrollingIcon icon = activeIcons.get(i);
            float position = (i - visibleStart) - (visibleCount - 1) / 2f;
            float newTargetX = centerX - direction * position * spacing;
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
        // 图标间距 = 图标大小的倍数: 0 = 完全重叠, 1 = 紧挨排列(一个图标宽度)
        return iconSpacing * baseSize;
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
        long fadeInDuration = Math.max(1L, animationDuration);
        float fadeInProgress = Math.min(elapsed / (float) fadeInDuration, 1.0f);
        float easedIn = 1.0f - (float) Math.pow(1.0f - fadeInProgress, 3);
        float baseAlpha = Mth.clamp(easedIn, 0.0f, 1.0f);
        
        if (icon.forcedFadeStartTime >= 0) {
            long fadeElapsed = currentTime - icon.forcedFadeStartTime;
            float fadeProgress = (float) fadeElapsed / (float) fadeOutDurationMs;
            float alpha = resolveFadeAlpha(fadeProgress);
            return Mth.clamp(baseAlpha * alpha, 0.0f, 1.0f);
        }
        
        if (elapsed <= icon.displayDuration) {
            return baseAlpha;
        }
        
        long fadeElapsed = elapsed - icon.displayDuration;
        float fadeProgress = (float) fadeElapsed / (float) fadeOutDurationMs;
        float alpha = resolveFadeAlpha(fadeProgress);
        return Mth.clamp(baseAlpha * alpha, 0.0f, 1.0f);
    }

    /**
     * 淡出阶段的透明度曲线。
     * 未启用闪动时: 线性 1 → 0(现有行为)。
     * 启用闪动时(在淡出动画时间内分段, 以透明度 0=不透明/100%=完全透明计):
     * 前 1/3 透明度 0% → 50%(alpha 1 → 0.5), 中 1/3 50% → 0%(alpha 0.5 → 1),
     * 后 1/3 0% → 100%(alpha 1 → 0, 完全透明), 最后隐藏。
     */
    private float resolveFadeAlpha(float fadeProgress) {
        if (configBlinkFadeAnimation) {
            if (fadeProgress < 1.0f / 3.0f) {
                return 1.0f - 2.4f * fadeProgress;      // alpha 1 → 0.2(透明度升到 80%)
            } else if (fadeProgress < 2.0f / 3.0f) {
                return 2.4f * fadeProgress - 0.6f;      // alpha 0.2 → 1(透明度回到 0%, 不透明)
            }
            return 3.0f * (1.0f - fadeProgress);        // alpha 1 → 0(透明度到 100%, 完全透明后隐藏)
        }
        return 1.0f - fadeProgress;                         // 原线性淡出
    }

    private boolean shouldRemoveIcon(ScrollingIcon icon, long currentTime, long elapsed) {
        if (icon.forcedFadeStartTime >= 0) {
            long fadeElapsed = currentTime - icon.forcedFadeStartTime;
            return fadeElapsed >= fadeOutDurationMs;
        }
        return elapsed >= icon.displayDuration + fadeOutDurationMs;
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
        return KillTypeRegistry.get(killType).textureKey();
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
        KillTypeDefinition def = KillTypeRegistry.get(killType);
        if (def == null || def.ringEnableKey() == null) {
            return false;
        }
        return ringEnableFlags.getOrDefault(def.ringEnableKey(), false);
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
