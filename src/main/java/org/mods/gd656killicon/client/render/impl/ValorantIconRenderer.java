package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.config.ValorantStyleCatalog;
import org.mods.gd656killicon.client.gui.tabs.PreviewTextureFocusContext;
import org.mods.gd656killicon.client.render.effect.IconGlowProcessor;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.PreviewRenderTimeContext;
import org.mods.gd656killicon.client.textures.IconTextureAnimationManager;
import org.mods.gd656killicon.client.textures.IconTextureAnimationManager.TextureFrame;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.textures.ModTextures;
import org.mods.gd656killicon.common.KillType;

import java.util.concurrent.ThreadLocalRandom;

public class ValorantIconRenderer implements IHudRenderer {
    private static final long DEFAULT_DISPLAY_DURATION = 2600L;
    private static final long FIVE_PLUS_FADE_IN_DURATION_MS = 300L;
    private static final long PARTICLE_BASE_DURATION_MS = 620L;
    private static final long BAR_BOUNCE_START_MS = 20L;
    private static final long BAR_BOUNCE_PEAK_MS = 180L;
    private static final long BAR_BOUNCE_HOLD_END_MS = 270L;
    private static final long BAR_BOUNCE_END_MS = 620L;
    private static final long BAR_SPIN_START_MS = 750L;
    private static final long BAR_SPIN_DURATION_180_MS = 700L;
    private static final long BAR_SPIN_DURATION_360_MS = 1000L;
    private static final long FADE_OUT_DURATION_MS = 220L;

    private static final float DEFAULT_ICON_ENTRY_OFFSET_PX = -16.0f;
    private static final float DEFAULT_ICON_ENTRY_DURATION_SECONDS = 0.10f;
    private static final float BAR_BOUNCE_DISTANCE_PX = 9.0f;
    private static final float VISUAL_CENTER_Y_OFFSET_PX = 6.0f;
    private static final float GLOBAL_VISUAL_SCALE = 0.95f;
    private static final float ICON_BASE_SIZE = 116.0f;
    private static final float BAR_BASE_SIZE = 32.0f;
    private static final float HEADSHOT_SIZE_MULTIPLIER = 0.16f;
    private static final float PARTICLE_BASE_SIZE = 112.0f;
    private static final float PARTICLE_FRAME_ANCHOR_Y_RATIO = 206.0f / 256.0f;
    private static final float BAR_RING_RADIUS = 36.0f;
    private static final int HALO_SEGMENTS = 72;
    private static final float GAIA_DEFAULT_BRIGHTNESS = 1.3f;
    private static final float GAIA_DEFAULT_CONTRAST = 1.1f;
    private static final int DEFAULT_ICON_FLASH_COLOR = 0xFF2A36;
    private static final int MAX_ICON_FLASH_COUNT = 12;
    private static final int PRIME_PARTICLE_SMALL_COLOR = 0xFFD138;
    private static final int GAIA_PARTICLE_SMALL_COLOR = 0xE2505C;
    private static final int DEFAULT_ACCENT_COLOR = 0xE2505C;
    private static final float DEFAULT_ICON_FLASH_HOLD_DURATION_SECONDS = 0.08f;
    private static final float ICON_FLASH_MAX_ALPHA = 0.9f;
    private static final float ICON_TEXTURE_SOURCE_SIZE = 1729.0f;
    private static final float DEFAULT_HERO_FLAME_SOURCE_SCALE = 8.0f;
    private static final float DEFAULT_LARGE_SPARKS_SOURCE_SCALE = 6.8f;
    private static final float DEFAULT_X_SPARKS_SOURCE_SCALE = 11.0f;
    private static final float HERO_FLAME_Y_OFFSET_PX = -30.0f;
    private static final float LARGE_SPARKS_Y_OFFSET_PX = 2.0f;
    private static final float X_SPARKS_Y_OFFSET_PX = -8.0f;
    private static final ParticleOverlaySpec HERO_FLAME_PARTICLE = new ParticleOverlaySpec(
        "hero_flame",
        0.4934375f,
        0.49079242f,
        DEFAULT_HERO_FLAME_SOURCE_SCALE,
        0.0f,
        HERO_FLAME_Y_OFFSET_PX,
        false
    );
    private static final ParticleOverlaySpec LARGE_SPARKS_PARTICLE = new ParticleOverlaySpec(
        "large_sparks",
        0.49052733f,
        0.5185547f,
        DEFAULT_LARGE_SPARKS_SOURCE_SCALE,
        0.0f,
        LARGE_SPARKS_Y_OFFSET_PX,
        true
    );
    private static final ParticleOverlaySpec X_SPARKS_PARTICLE = new ParticleOverlaySpec(
        "x_sparks",
        0.48015872f,
        0.56722003f,
        DEFAULT_X_SPARKS_SOURCE_SCALE,
        0.0f,
        X_SPARKS_Y_OFFSET_PX,
        false
    );
    private static final float[][] BAR_LAYOUT_ANGLES = new float[][]{
        {0.0f},
        {-90.0f, 90.0f},
        {0.0f, -120.0f, 120.0f},
        {0.0f, -90.0f, 90.0f, 180.0f},
        {0.0f, -72.0f, 72.0f, -144.0f, 144.0f},
        {0.0f, -60.0f, 60.0f, -120.0f, 120.0f, 180.0f}
    };
    private static final float[][] GLOW_OFFSETS = new float[][]{
        {-1.0f, 0.0f},
        {1.0f, 0.0f},
        {0.0f, -1.0f},
        {0.0f, 1.0f},
        {-0.70710677f, -0.70710677f},
        {-0.70710677f, 0.70710677f},
        {0.70710677f, -0.70710677f},
        {0.70710677f, 0.70710677f}
    };

    private float configScale = 1.0f;
    private float configXOffset = 0.0f;
    private float configYOffset = 80.0f;
    private float configBarXOffset = 0.0f;
    private float configBarYOffset = 0.0f;
    private float configBarRadiusOffset = 0.0f;
    private float configIconScale = 1.0f;
    private float configHeadshotAnimInitialScale = 1.8f;
    private float configHeadshotAnimDuration = 0.25f;
    private float configBarEntryInitialScale = 1.6f;
    private float configBarEntryDuration = 0.18f;
    private float configBrightness = 1.0f;
    private boolean configIconGlowEnabled = false;
    private int configIconGlowColor = 0xFFFFFF;
    private float configIconGlowIntensity = 0.45f;
    private float configIconGlowSize = 4.0f;
    private boolean configHaloRingEnabled = false;
    private float configHaloRingRadius = 30.0f;
    private float configHaloRingWidth = 1.50f;
    private int configHaloRingColor = 0xFFFFFF;
    private boolean configBladeEffect = true;
    private boolean configBladeRotationEffect = true;
    private float configBladeDecelerationWindowSec = 1.5f;
    private float configMathParticleDensity = 1.0f;
    private float configMathParticleSpread = 1.0f;
    private float configMathParticleSize = 1.0f;
    private JsonObject currentConfig;
    private boolean configMathParticleEffect = false;

    private long startTime = -1L;
    private boolean visible = false;
    private int comboCount = 1;
    private boolean headshotTrigger = false;
    private float spinDirection = 1.0f;
    private SmallParticle[] smallParticles = new SmallParticle[0];

    private record ParticleOverlaySpec(
        String configKeyPrefix,
        float anchorXRatio,
        float anchorYRatio,
        float defaultSourceScale,
        float offsetXPx,
        float offsetYPx,
        boolean additive
    ) {
    }

    public static void clearProcessedTextureCache() {
        IconGlowProcessor.clearCache();
    }

    @Override
    public void trigger(TriggerContext context) {
        if (context.comboCount() <= 0) {
            return;
        }
        if (context.type() == KillType.ASSIST || context.type() == KillType.DESTROY_VEHICLE) {
            return;
        }

        JsonObject config = ConfigManager.getElementConfig("kill_icon", "valorant");
        if (config == null) {
            return;
        }

        boolean enabled = !config.has("visible") || config.get("visible").getAsBoolean();
        if (!enabled) {
            visible = false;
            return;
        }

        loadConfig(config);
        comboCount = Mth.clamp(context.comboCount(), 1, 6);
        headshotTrigger = context.type() == KillType.HEADSHOT;
        spinDirection = ThreadLocalRandom.current().nextBoolean() ? 1.0f : -1.0f;
        if (configMathParticleEffect) {
            generateParticleState();
        } else {
            smallParticles = new SmallParticle[0];
        }
        startTime = PreviewRenderTimeContext.currentTimeMillis();
        visible = true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        if (!visible || startTime < 0L) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float centerX = screenWidth / 2.0f + configXOffset;
        float centerY = screenHeight - configYOffset + VISUAL_CENTER_Y_OFFSET_PX;
        renderAt(guiGraphics, partialTick, centerX, centerY);
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        if (!visible || startTime < 0L) {
            return;
        }

        JsonObject liveConfig = ConfigManager.getElementConfig("kill_icon", "valorant");
        if (liveConfig != null) {
            loadConfig(liveConfig);
        }

        long elapsed = PreviewRenderTimeContext.currentTimeMillis() - startTime;
        long displayDuration = resolveDisplayDuration();
        if (elapsed > displayDuration) {
            visible = false;
            startTime = -1L;
            return;
        }

        float alpha = resolveAlpha(elapsed, displayDuration);
        float iconOffsetY = resolveIconOffsetY(elapsed);
        float barTravel = resolveBarTravel(elapsed);
        float barRotation = resolveBarRotation(elapsed);
        FlashState flash = resolveFlashState(elapsed);

        String presetId = ConfigManager.getCurrentPresetId();
        String emblemTexturePath = resolveSelectedTexture(presetId, "emblem");
        String frameTexturePath = resolveSelectedTexture(presetId, "frame");
        String bladeTexturePath = resolveSelectedTexture(presetId, "blade");
        String barTexturePath = resolveSelectedTexture(presetId, "bar");
        String headshotTexturePath = resolveSelectedTexture(presetId, "headshot");
        String baseParticleTexturePath = resolveSelectedTexture(presetId, "base_particle");
        String heroFlameTexturePath = resolveSelectedTexture(presetId, "hero_flame");
        String largeSparksTexturePath = resolveSelectedTexture(presetId, "large_sparks");
        String xSparksTexturePath = resolveSelectedTexture(presetId, "x_sparks");
        String styleId = resolveStyleId();
        boolean gaiaSkin = isGaiaSkin(styleId, emblemTexturePath, barTexturePath);
        boolean bladeEnabled = configBladeEffect;
        String activeBladeTexturePath = bladeEnabled ? resolveBladeTexturePath(styleId, bladeTexturePath) : null;
        float emblemWidth = ICON_BASE_SIZE * configIconScale * resolveTextureScale("emblem") * resolveFrameRatio("emblem", "texture_frame_width_ratio");
        float emblemHeight = ICON_BASE_SIZE * configIconScale * resolveTextureScale("emblem") * resolveFrameRatio("emblem", "texture_frame_height_ratio");
        float frameWidth = ICON_BASE_SIZE * resolveTextureScale("frame") * resolveFrameRatio("frame", "texture_frame_width_ratio");
        float frameHeight = ICON_BASE_SIZE * resolveTextureScale("frame") * resolveFrameRatio("frame", "texture_frame_height_ratio");
        float bladeWidth = ICON_BASE_SIZE * resolveTextureScale("blade") * resolveFrameRatio("blade", "texture_frame_width_ratio");
        float bladeHeight = ICON_BASE_SIZE * resolveTextureScale("blade") * resolveFrameRatio("blade", "texture_frame_height_ratio");
        float headshotWidth = ICON_BASE_SIZE * HEADSHOT_SIZE_MULTIPLIER * resolveTextureScale("headshot") * resolveFrameRatio("headshot", "texture_frame_width_ratio");
        float headshotHeight = ICON_BASE_SIZE * HEADSHOT_SIZE_MULTIPLIER * resolveTextureScale("headshot") * resolveFrameRatio("headshot", "texture_frame_height_ratio");
        float barBaseSize = BAR_BASE_SIZE;
        float barWidth = barBaseSize * resolveTextureScale("bar") * resolveFrameRatio("bar", "texture_frame_width_ratio");
        float barHeight = barBaseSize * resolveTextureScale("bar") * resolveFrameRatio("bar", "texture_frame_height_ratio");
        float effectiveBrightness = resolveEffectiveBrightness(gaiaSkin);
        float effectiveContrast = resolveEffectiveContrast(gaiaSkin);
        boolean accentTintEnabled = isAccentTintEnabled();
        int accentColor = resolveAccentColor();
        int defaultParticleColor = resolveDefaultParticleColor(accentTintEnabled, accentColor, gaiaSkin);
        int xSparksDefaultColor = resolveConfiguredColor("color_icon_flash", DEFAULT_ICON_FLASH_COLOR);
        int headshotOverlayColor = resolveConfiguredColor("color_headshot_overlay", DEFAULT_ICON_FLASH_COLOR);
        boolean frameEntryLocked = isEntryMotionFrameLocked(styleId);
        float rootMotionOffsetY = frameEntryLocked ? 0.0f : iconOffsetY;
        float emblemMotionOffsetY = frameEntryLocked ? iconOffsetY : 0.0f;
        float resolvedHeadshotXOffset = ValorantStyleCatalog.getHeadshotOffsetX(styleId) + resolveTextureOffset("headshot", "texture_x_offset");
        float resolvedHeadshotYOffset = ValorantStyleCatalog.getHeadshotOffsetY(styleId) + resolveTextureOffset("headshot", "texture_y_offset");
        float barRadiusOffset = configBarRadiusOffset;
        float barCenterXOffset = configBarXOffset + resolveTextureOffset("bar", "texture_x_offset");
        float barCenterYOffset = configBarYOffset + resolveTextureOffset("bar", "texture_y_offset");
        float[] barLayoutAngles = BAR_LAYOUT_ANGLES[Mth.clamp(comboCount, 1, 6) - 1];

        try {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY + rootMotionOffsetY, 0.0f);
            float renderScale = configScale * GLOBAL_VISUAL_SCALE;
            guiGraphics.pose().scale(renderScale, renderScale, 1.0f);

            if (configMathParticleEffect) {
                int mathParticleColor = resolveLayerParticleColor("base_particle", defaultParticleColor);
                renderSmallParticles(guiGraphics, elapsed, alpha, mathParticleColor);
            } else {
                renderBaseParticles(guiGraphics, baseParticleTexturePath, elapsed, alpha, effectiveBrightness, effectiveContrast, defaultParticleColor);
            }

            renderGradientHaloRing(guiGraphics, elapsed, alpha);

            renderBarRing(
                guiGraphics,
                barTexturePath,
                barWidth,
                barHeight,
                barTravel,
                alpha,
                barRotation,
                barRadiusOffset,
                barCenterXOffset,
                barCenterYOffset,
                barLayoutAngles,
                accentTintEnabled,
                accentColor,
                effectiveBrightness,
                effectiveContrast,
                elapsed
            );
            RenderSystem.defaultBlendFunc();

            guiGraphics.pose().pushPose();
            drawColorizableTexture(
                guiGraphics,
                "frame",
                frameTexturePath,
                frameWidth,
                frameHeight,
                alpha,
                0xFFFFFF,
                false,
                accentTintEnabled,
                accentColor,
                effectiveBrightness,
                effectiveContrast
            );
            if (activeBladeTexturePath != null && !activeBladeTexturePath.isEmpty()) {
                guiGraphics.pose().pushPose();
                float bladeRotation = resolveFrameBladeRotation(styleId, elapsed);
                if (Math.abs(bladeRotation) > 0.01f) {
                    guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(bladeRotation));
                }
                drawColorizableTexture(
                    guiGraphics,
                    "blade",
                    activeBladeTexturePath,
                    bladeWidth,
                    bladeHeight,
                    alpha,
                    0xFFFFFF,
                    false,
                    accentTintEnabled,
                    accentColor,
                    effectiveBrightness,
                    effectiveContrast
                );
                guiGraphics.pose().popPose();
            }
            guiGraphics.pose().popPose();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0f, emblemMotionOffsetY, 0.0f);
            drawColorizableTexture(
                guiGraphics,
                "emblem",
                emblemTexturePath,
                emblemWidth,
                emblemHeight,
                alpha,
                0xFFFFFF,
                false,
                accentTintEnabled,
                accentColor,
                effectiveBrightness,
                effectiveContrast
            );
            if (flash.alpha() > 0.0f) {
                drawColorizableTexture(
                    guiGraphics,
                    "emblem",
                    emblemTexturePath,
                    emblemWidth,
                    emblemHeight,
                    alpha * flash.alpha(),
                    flash.color(),
                    false,
                    accentTintEnabled,
                    accentColor,
                    effectiveBrightness,
                    effectiveContrast
                );
            }
            if (headshotTrigger) {
                HeadshotAnimState hsAnim = resolveHeadshotAnimState(elapsed, headshotOverlayColor);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(resolvedHeadshotXOffset, resolvedHeadshotYOffset, 0.0f);
                if (Math.abs(hsAnim.scale() - 1.0f) > 0.001f) {
                    guiGraphics.pose().scale(hsAnim.scale(), hsAnim.scale(), 1.0f);
                }
                drawColorizableTexture(
                    guiGraphics,
                    "headshot",
                    headshotTexturePath,
                    headshotWidth,
                    headshotHeight,
                    alpha,
                    hsAnim.color(),
                    false,
                    false,
                    0xFFFFFF,
                    1.0f,
                    1.0f
                );
                guiGraphics.pose().popPose();
            }
            guiGraphics.pose().popPose();
            RenderSystem.defaultBlendFunc();

            if (!configMathParticleEffect) {
                renderOverlayParticles(
                    guiGraphics,
                    HERO_FLAME_PARTICLE,
                    heroFlameTexturePath,
                    elapsed,
                    alpha,
                    emblemWidth,
                    emblemHeight,
                    effectiveBrightness,
                    effectiveContrast,
                    defaultParticleColor,
                    emblemMotionOffsetY
                );
                if (comboCount >= 5) {
                    renderOverlayParticles(
                        guiGraphics,
                        LARGE_SPARKS_PARTICLE,
                        largeSparksTexturePath,
                        elapsed,
                        alpha,
                        emblemWidth,
                        emblemHeight,
                        effectiveBrightness,
                        effectiveContrast,
                        defaultParticleColor,
                        emblemMotionOffsetY
                    );
                }
                if (headshotTrigger) {
                    renderOverlayParticles(
                        guiGraphics,
                        X_SPARKS_PARTICLE,
                        xSparksTexturePath,
                        elapsed,
                        alpha,
                        emblemWidth,
                        emblemHeight,
                        effectiveBrightness,
                        effectiveContrast,
                        xSparksDefaultColor,
                        emblemMotionOffsetY
                    );
                }
            }

            guiGraphics.pose().popPose();
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void loadConfig(JsonObject config) {
        currentConfig = config;
        configScale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        configXOffset = config.has("x_offset") ? config.get("x_offset").getAsFloat() : 0.0f;
        configYOffset = config.has("y_offset") ? config.get("y_offset").getAsFloat() : 80.0f;
        configBarXOffset = config.has("bar_x_offset") ? config.get("bar_x_offset").getAsFloat() : 0.0f;
        configBarYOffset = config.has("bar_y_offset") ? config.get("bar_y_offset").getAsFloat() : 0.0f;
        configBarRadiusOffset = config.has("bar_radius_offset") ? config.get("bar_radius_offset").getAsFloat() : 0.0f;
        configIconScale = config.has("icon_scale") ? config.get("icon_scale").getAsFloat() : 1.0f;
        configHeadshotAnimInitialScale = Mth.clamp(config.has("headshot_anim_initial_scale") ? config.get("headshot_anim_initial_scale").getAsFloat() : 1.8f, 1.0f, 5.0f);
        configHeadshotAnimDuration = Math.max(0.0f, config.has("headshot_anim_duration") ? config.get("headshot_anim_duration").getAsFloat() : 0.25f);
        configBarEntryInitialScale = Mth.clamp(config.has("bar_entry_initial_scale") ? config.get("bar_entry_initial_scale").getAsFloat() : 1.6f, 1.0f, 5.0f);
        configBarEntryDuration = Math.max(0.0f, config.has("bar_entry_duration") ? config.get("bar_entry_duration").getAsFloat() : 0.18f);
        configBrightness = config.has("brightness") ? config.get("brightness").getAsFloat() : 1.0f;
        configIconGlowEnabled = config.has("enable_icon_glow") && config.get("enable_icon_glow").getAsBoolean();
        configIconGlowColor = resolveConfiguredColor("color_icon_glow", 0xFFFFFF);
        configIconGlowIntensity = Mth.clamp(config.has("icon_glow_intensity") ? config.get("icon_glow_intensity").getAsFloat() : 0.45f, 0.0f, 1.0f);
        configIconGlowSize = Math.max(0.0f, config.has("icon_glow_size") ? config.get("icon_glow_size").getAsFloat() : 4.0f);
        configHaloRingEnabled = config.has("enable_halo_ring") && config.get("enable_halo_ring").getAsBoolean();
        configHaloRingRadius = Math.max(4.0f, config.has("halo_ring_radius") ? config.get("halo_ring_radius").getAsFloat() : 30.0f);
        configHaloRingWidth = Math.max(0.1f, config.has("halo_ring_width") ? config.get("halo_ring_width").getAsFloat() : 1.50f);
        configHaloRingColor = resolveConfiguredColor("color_halo_ring", 0xFFFFFF);
        configBladeEffect = !config.has("enable_blade_effect") || config.get("enable_blade_effect").getAsBoolean();
        configBladeRotationEffect = !config.has("enable_blade_rotation_effect") || config.get("enable_blade_rotation_effect").getAsBoolean();
        configBladeDecelerationWindowSec = Mth.clamp(config.has("blade_deceleration_window") ? config.get("blade_deceleration_window").getAsFloat() : 1.5f, 0.2f, 5.0f);
        configMathParticleEffect = config.has("enable_math_particle_effect") && config.get("enable_math_particle_effect").getAsBoolean();
        configMathParticleDensity = Mth.clamp(config.has("math_particle_density") ? config.get("math_particle_density").getAsFloat() : 1.0f, 0.2f, 4.0f);
        configMathParticleSpread = Mth.clamp(config.has("math_particle_spread") ? config.get("math_particle_spread").getAsFloat() : 1.0f, 0.25f, 3.0f);
        configMathParticleSize = Mth.clamp(config.has("math_particle_size") ? config.get("math_particle_size").getAsFloat() : 1.0f, 0.25f, 3.0f);
    }

    private long resolveDisplayDuration() {
        long serverDuration = ComboIconRenderer.getServerComboWindowMs();
        if (serverDuration > 0L) {
            return serverDuration;
        }
        return DEFAULT_DISPLAY_DURATION;
    }

    private long resolveDurationMillis(String key, float fallbackSeconds) {
        float seconds = getFloatConfig(key, fallbackSeconds);
        return Math.max(0L, Math.round(seconds * 1000.0f));
    }

    private float resolveAlpha(long elapsed, long displayDuration) {
        float alpha = 1.0f;
        if (comboCount >= 5 && elapsed < FIVE_PLUS_FADE_IN_DURATION_MS) {
            float progress = (float) elapsed / (float) FIVE_PLUS_FADE_IN_DURATION_MS;
            alpha *= easeOutQuad(progress);
        }
        if (displayDuration <= FADE_OUT_DURATION_MS || elapsed <= displayDuration - FADE_OUT_DURATION_MS) {
            return alpha;
        }
        float progress = (float) (elapsed - (displayDuration - FADE_OUT_DURATION_MS)) / (float) FADE_OUT_DURATION_MS;
        return alpha * (1.0f - Mth.clamp(progress, 0.0f, 1.0f));
    }

    private String resolveSelectedTexture(String presetId, String textureKey) {
        return ElementTextureDefinition.getSelectedTextureFileName(
            presetId,
            "kill_icon/valorant",
            textureKey,
            currentConfig
        );
    }

    private float resolveIconOffsetY(long elapsed) {
        float startOffset = getFloatConfig("icon_entry_offset_y", DEFAULT_ICON_ENTRY_OFFSET_PX);
        long durationMs = resolveDurationMillis("icon_entry_duration", DEFAULT_ICON_ENTRY_DURATION_SECONDS);
        if (durationMs <= 0L || Math.abs(startOffset) < 0.01f || elapsed >= durationMs) {
            return 0.0f;
        }
        float normalizedProgress = Mth.clamp((float) elapsed / (float) durationMs, 0.0f, 1.0f);
        return Mth.lerp(easeOutCubic(normalizedProgress), startOffset, 0.0f);
    }

    private float resolveBarTravel(long elapsed) {
        if (elapsed < BAR_BOUNCE_START_MS || elapsed >= BAR_BOUNCE_END_MS) {
            return 0.0f;
        }
        if (elapsed <= BAR_BOUNCE_PEAK_MS) {
            float progress = (float) (elapsed - BAR_BOUNCE_START_MS) / (float) (BAR_BOUNCE_PEAK_MS - BAR_BOUNCE_START_MS);
            return Mth.lerp(easeOutSoft(progress), 0.0f, BAR_BOUNCE_DISTANCE_PX);
        }
        if (elapsed <= BAR_BOUNCE_HOLD_END_MS) {
            return BAR_BOUNCE_DISTANCE_PX;
        }
        float progress = (float) (elapsed - BAR_BOUNCE_HOLD_END_MS) / (float) (BAR_BOUNCE_END_MS - BAR_BOUNCE_HOLD_END_MS);
        return Mth.lerp(easeInQuart(progress), BAR_BOUNCE_DISTANCE_PX, 0.0f);
    }

    private float resolveBarRotation(long elapsed) {
        if (comboCount <= 1 || elapsed <= BAR_SPIN_START_MS) {
            return 0.0f;
        }
        long rotationDuration = comboCount >= 5 ? BAR_SPIN_DURATION_360_MS : BAR_SPIN_DURATION_180_MS;
        float targetRotation = comboCount >= 5 ? 360.0f : 180.0f;
        float progress = (float) (elapsed - BAR_SPIN_START_MS) / (float) rotationDuration;
        progress = Mth.clamp(progress, 0.0f, 1.0f);
        return targetRotation * easeOutCubic(progress) * spinDirection;
    }

    private FlashState resolveFlashState(long elapsed) {
        int flashCount = Mth.clamp(getIntConfig("icon_flash_count", 4), 0, MAX_ICON_FLASH_COUNT);
        if (flashCount <= 0) {
            return FlashState.none();
        }
        long holdDurationMs = resolveDurationMillis("icon_flash_hold_duration", DEFAULT_ICON_FLASH_HOLD_DURATION_SECONDS);
        if (holdDurationMs <= 0L) {
            return FlashState.none();
        }

        long gapDurationMs = Math.max(20L, Math.round(holdDurationMs * 0.75f));
        long cycleDurationMs = holdDurationMs + gapDurationMs;
        long totalDurationMs = holdDurationMs + cycleDurationMs * (flashCount - 1L);
        if (elapsed >= totalDurationMs) {
            return FlashState.none();
        }

        long cycleElapsed = elapsed % cycleDurationMs;
        if (cycleElapsed >= holdDurationMs) {
            return FlashState.none();
        }

        float holdMs = (float) holdDurationMs;
        float localElapsed = (float) cycleElapsed;
        float edgeDurationMs = Math.min(35.0f, holdMs * 0.25f);
        float pulseAlpha = 1.0f;
        if (edgeDurationMs > 1.0f) {
            if (localElapsed < edgeDurationMs) {
                pulseAlpha = localElapsed / edgeDurationMs;
            } else if (holdMs - localElapsed < edgeDurationMs) {
                pulseAlpha = (holdMs - localElapsed) / edgeDurationMs;
            }
        }
        int color = resolveConfiguredColor("color_icon_flash", DEFAULT_ICON_FLASH_COLOR);
        return new FlashState(color, Mth.clamp(pulseAlpha, 0.0f, 1.0f) * ICON_FLASH_MAX_ALPHA);
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
        return value > 0.0f ? value : 1.0f;
    }

    private float resolveTextureScale(String textureKey) {
        return resolveTextureFloat(textureKey, "texture_scale", 1.0f, 0.01f);
    }

    private float resolveTextureFinalOpacity(String textureKey) {
        return Mth.clamp(resolveTextureFloat(textureKey, "texture_final_opacity", 1.0f, 0.0f), 0.0f, 1.0f);
    }

    private float resolveTextureOffset(String textureKey, String suffixKey) {
        return resolveTextureFloat(textureKey, suffixKey, 0.0f, -Float.MAX_VALUE);
    }

    private float resolveTextureFloat(String textureKey, String suffixKey, float fallback, float minimum) {
        if (currentConfig == null || textureKey == null) {
            return fallback;
        }
        String key = "anim_" + textureKey + "_" + suffixKey;
        if (!currentConfig.has(key)) {
            return fallback;
        }
        float value = currentConfig.get(key).getAsFloat();
        if (value < minimum) {
            return fallback;
        }
        return value;
    }

    private void generateParticleState() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        smallParticles = createSmallParticles(random);
    }

    private SmallParticle[] createSmallParticles(ThreadLocalRandom random) {
        int baseParticleCount = comboCount <= 1 ? 28 : 22 + comboCount * 3;
        int particleCount = Math.max(1, Math.round(baseParticleCount * configMathParticleDensity));
        float visibilityScale = 0.95f;
        float directionOffset = 0.0f;
        float spreadScale = configMathParticleSpread;
        float sizeScale = configMathParticleSize;
        SmallParticle[] particles = new SmallParticle[particleCount];

        for (int index = 0; index < particleCount; index++) {
            boolean rightSide = (index & 1) == 0;
            float angleBase = rightSide ? 90.0f : -90.0f;
            float sideSpread = (comboCount <= 1 ? 44.0f : 56.0f) * spreadScale;
            float angle = angleBase + directionOffset + random.nextFloat(-sideSpread, sideSpread);
            if (index % 4 == 0) {
                angle += rightSide ? random.nextFloat(-24.0f * spreadScale, -8.0f * spreadScale) : random.nextFloat(8.0f * spreadScale, 24.0f * spreadScale);
            }
            float startRadius = random.nextFloat(8.0f, 16.0f) * spreadScale;
            float endRadius = (comboCount <= 1 ? random.nextFloat(54.0f, 82.0f) : random.nextFloat(62.0f, 92.0f)) * spreadScale;
            float width = random.nextFloat(7.0f, 13.5f) * visibilityScale * sizeScale;
            float height = random.nextFloat(1.8f, 4.0f) * visibilityScale * sizeScale;
            float tilt = angle + random.nextFloat(-28.0f, 28.0f);
            float alphaScale = random.nextFloat(0.95f, 1.45f) * visibilityScale;
            float jagScale = random.nextFloat(0.0f, 1.0f);
            float bendScale = random.nextFloat(0.0f, 1.0f);
            float lateralBoost = random.nextFloat(1.35f, 1.75f);
            long delayMs = random.nextLong(0L, 30L);
            float gravityPx = random.nextFloat(50.0f, 82.0f);
            particles[index] = new SmallParticle(
                angle,
                tilt,
                startRadius,
                endRadius,
                width,
                height,
                alphaScale,
                jagScale,
                bendScale,
                lateralBoost,
                gravityPx,
                delayMs
            );
        }

        return particles;
    }

    private void renderSmallParticles(GuiGraphics guiGraphics, long elapsed, float alpha, int particleRgb) {
        if (alpha <= 0.0f || elapsed >= PARTICLE_BASE_DURATION_MS) {
            return;
        }
        for (SmallParticle particle : smallParticles) {
            long particleElapsed = elapsed - particle.delayMs();
            if (particleElapsed < 0L || particleElapsed > PARTICLE_BASE_DURATION_MS) {
                continue;
            }
            float progress = Mth.clamp((float) particleElapsed / (float) PARTICLE_BASE_DURATION_MS, 0.0f, 1.0f);
            float move = easeOutCubic(progress);
            float distance = Mth.lerp(move, particle.startRadius(), particle.endRadius());
            float angleRad = (float) Math.toRadians(particle.angleDeg());
            float horizontalFactor = Mth.sin(angleRad) * distance * particle.lateralBoost();
            float verticalFactor = -Mth.cos(angleRad) * distance * (2.15f - particle.lateralBoost());
            float x = horizontalFactor;
            float y = verticalFactor + particle.gravityPx() * easeInQuad(progress);
            float particleAlpha = alpha * particle.alphaScale() * (1.0f - easeInQuart(progress));
            int color = applyAlpha(particleRgb, particleAlpha);
            if ((color >>> 24) <= 0) {
                continue;
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0.0f);
            guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(particle.tiltDeg()));
            drawSmallShard(guiGraphics, particle.width(), particle.height(), particle.jagScale(), particle.bendScale(), color);
            guiGraphics.pose().popPose();
        }
    }

    private void drawSmallShard(GuiGraphics guiGraphics, float length, float thickness, float jagScale, float bendScale, int color) {
        float halfLength = Math.max(1.5f, length * 0.5f);
        float halfThickness = Math.max(0.8f, thickness * 0.5f);
        float topFlare = halfThickness * (1.25f + jagScale * 0.45f);
        float bottomFlare = halfThickness * (1.05f + (1.0f - jagScale) * 0.35f);
        float tailShoulderX = -halfLength * (0.22f - bendScale * 0.08f);
        float upperKinkX = halfLength * (0.34f + jagScale * 0.12f);
        float lowerKinkX = halfLength * (0.18f + bendScale * 0.08f);
        float tailLowerX = -halfLength * (0.58f - bendScale * 0.1f);
        float skewY = (bendScale - 0.5f) * thickness * 0.55f;

        float[] xs = new float[]{-halfLength, tailShoulderX, upperKinkX, halfLength, lowerKinkX, tailLowerX};
        float[] ys = new float[]{
            halfThickness * 0.38f + skewY * 0.18f,
            topFlare + skewY,
            halfThickness * 0.22f + skewY * 0.3f,
            0.0f,
            -bottomFlare + skewY * 0.08f,
            -halfThickness * 0.62f + skewY * 0.45f
        };

        drawConvexPolygon(guiGraphics, xs, ys, color & 0xFFFFFF, ((color >>> 24) & 0xFF) / 255.0f);
    }

    private void drawConvexPolygon(GuiGraphics guiGraphics, float[] xs, float[] ys, int rgb, float alpha) {
        if (xs.length < 3 || ys.length < 3 || alpha <= 0.0f) {
            return;
        }

        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        if (a <= 0) {
            return;
        }

        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int index = 1; index < xs.length - 1; index++) {
            builder.vertex(matrix, xs[0], ys[0], 0.0f).color(red, green, blue, a).endVertex();
            builder.vertex(matrix, xs[index], ys[index], 0.0f).color(red, green, blue, a).endVertex();
            builder.vertex(matrix, xs[index + 1], ys[index + 1], 0.0f).color(red, green, blue, a).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
    }

    private int resolveAccentColor() {
        if (currentConfig != null) {
            if (currentConfig.has("color_accent")) {
                return resolveConfiguredColor("color_accent", DEFAULT_ACCENT_COLOR);
            }
            if (currentConfig.has("color_gaia_accent")) {
                return resolveConfiguredColor("color_gaia_accent", DEFAULT_ACCENT_COLOR);
            }
        }
        return DEFAULT_ACCENT_COLOR;
    }

    private int resolveDefaultParticleColor(boolean accentTintEnabled, int accentColor, boolean gaiaSkin) {
        return accentTintEnabled
            ? accentColor
            : (gaiaSkin ? GAIA_PARTICLE_SMALL_COLOR : PRIME_PARTICLE_SMALL_COLOR);
    }

    private boolean isAccentTintEnabled() {
        if (currentConfig != null && currentConfig.has("enable_accent_tint")) {
            return currentConfig.get("enable_accent_tint").getAsBoolean();
        }
        if (currentConfig != null && currentConfig.has("color_gaia_accent")) {
            String legacyColor = currentConfig.get("color_gaia_accent").getAsString();
            return legacyColor != null && !legacyColor.equalsIgnoreCase(String.format("#%06X", DEFAULT_ACCENT_COLOR));
        }
        return false;
    }

    private int resolveConfiguredColor(String key, int fallback) {
        if (currentConfig == null || key == null || !currentConfig.has(key)) {
            return fallback;
        }
        String raw = currentConfig.get(key).getAsString();
        if (raw != null && raw.matches("^#[0-9A-Fa-f]{6}$")) {
            try {
                return Integer.parseInt(raw.substring(1), 16);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private void renderGradientHaloRing(GuiGraphics guiGraphics, long elapsed, float alpha) {
        if (!configHaloRingEnabled || alpha <= 0.001f || configHaloRingWidth <= 0.0f || configHaloRingRadius <= 0.0f) {
            return;
        }
        float focusAlpha = alpha * PreviewTextureFocusContext.alphaMultiplier("kill_icon/valorant", "frame");
        if (focusAlpha <= 0.001f) {
            return;
        }
        float centerX = resolveTextureOffset("frame", "texture_x_offset");
        float centerY = resolveTextureOffset("frame", "texture_y_offset");
        float outerRadius = configHaloRingRadius + configHaloRingWidth * 0.5f;
        float innerRadius = Math.max(0.0f, configHaloRingRadius - configHaloRingWidth * 0.5f);
        float minY = centerY - outerRadius;
        float maxY = centerY + outerRadius;
        float yRange = Math.max(0.001f, maxY - minY);
        int red = (configHaloRingColor >> 16) & 0xFF;
        int green = (configHaloRingColor >> 8) & 0xFF;
        int blue = configHaloRingColor & 0xFF;
        float angleOffset = (elapsed % 2200L) / 2200.0f * Mth.TWO_PI;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= HALO_SEGMENTS; i++) {
            float angle = Mth.TWO_PI * i / HALO_SEGMENTS + angleOffset;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            float xo = centerX + cos * outerRadius;
            float yo = centerY + sin * outerRadius;
            float xi = centerX + cos * innerRadius;
            float yi = centerY + sin * innerRadius;
            float outerAlphaFactor = Mth.clamp((maxY - yo) / yRange, 0.0f, 1.0f);
            float innerAlphaFactor = Mth.clamp((maxY - yi) / yRange, 0.0f, 1.0f);
            int ao = Mth.clamp((int)(focusAlpha * outerAlphaFactor * 255.0f), 0, 255);
            int ai = Mth.clamp((int)(focusAlpha * innerAlphaFactor * 255.0f), 0, 255);
            builder.vertex(matrix, xo, yo, 0.0f).color(red, green, blue, ao).endVertex();
            builder.vertex(matrix, xi, yi, 0.0f).color(red, green, blue, ai).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private boolean getBooleanConfig(String key, boolean fallback) {
        return currentConfig != null && key != null && currentConfig.has(key)
            ? currentConfig.get(key).getAsBoolean()
            : fallback;
    }

    private float getFloatConfig(String key, float fallback) {
        return currentConfig != null && key != null && currentConfig.has(key)
            ? currentConfig.get(key).getAsFloat()
            : fallback;
    }

    private int getIntConfig(String key, int fallback) {
        return currentConfig != null && key != null && currentConfig.has(key)
            ? currentConfig.get(key).getAsInt()
            : fallback;
    }

    private int resolveLayerParticleColor(String keyPrefix, int fallbackColor) {
        return resolveConfiguredColor("color_" + keyPrefix, fallbackColor);
    }

    private void renderBaseParticles(
        GuiGraphics guiGraphics,
        String texturePath,
        long elapsed,
        float alpha,
        float brightness,
        float contrast,
        int defaultColor
    ) {
        if (!getBooleanConfig("enable_base_particle", true)) {
            return;
        }

        if (alpha <= 0.0f || texturePath == null || texturePath.isEmpty()) {
            return;
        }

        TextureFrame frame = IconTextureAnimationManager.getTextureFrame(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "base_particle",
            texturePath,
            startTime,
            currentConfig
        );
        float particleSize = PARTICLE_BASE_SIZE * resolveTextureScale("base_particle");
        float layerAlpha = alpha * PreviewTextureFocusContext.alphaMultiplier("kill_icon/valorant", "base_particle");
        if (layerAlpha <= 0.001f) {
            return;
        }
        int particleColor = resolveLayerParticleColor("base_particle", defaultColor);
        ResourceLocation texture = resolveParticleTexture(texturePath, brightness, contrast);

        renderMirroredParticleBurst(
            guiGraphics,
            texture,
            frame,
            particleSize,
            particleSize,
            resolveTextureOffset("base_particle", "texture_x_offset"),
            0.0f,
            resolveTextureOffset("base_particle", "texture_y_offset"),
            layerAlpha,
            particleColor,
            false,
            0.0f,
            PARTICLE_FRAME_ANCHOR_Y_RATIO
        );
    }

    private void renderOverlayParticles(
        GuiGraphics guiGraphics,
        ParticleOverlaySpec spec,
        String texturePath,
        long elapsed,
        float alpha,
        float iconWidth,
        float iconHeight,
        float brightness,
        float contrast,
        int defaultColor,
        float extraYOffset
    ) {
        String keyPrefix = spec.configKeyPrefix();
        if (!getBooleanConfig("enable_" + keyPrefix, true)) {
            return;
        }

        if (alpha <= 0.0f || texturePath == null || texturePath.isEmpty()) {
            return;
        }

        float sourceScale = spec.defaultSourceScale() * resolveTextureScale(keyPrefix);
        TextureFrame frame = IconTextureAnimationManager.getTextureFrame(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            keyPrefix,
            texturePath,
            startTime,
            currentConfig
        );
        float drawWidth = iconWidth * sourceScale * ((float) frame.width / ICON_TEXTURE_SOURCE_SIZE);
        float drawHeight = iconHeight * sourceScale * ((float) frame.height / ICON_TEXTURE_SOURCE_SIZE);
        if (drawWidth <= 0.0f || drawHeight <= 0.0f) {
            return;
        }

        ResourceLocation texture = resolveParticleTexture(texturePath, brightness, contrast);
        float layerAlpha = alpha * PreviewTextureFocusContext.alphaMultiplier("kill_icon/valorant", keyPrefix);
        if (layerAlpha <= 0.001f) {
            return;
        }
        int particleColor = resolveLayerParticleColor(keyPrefix, defaultColor);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(
            spec.offsetXPx() + resolveTextureOffset(keyPrefix, "texture_x_offset"),
            spec.offsetYPx() + resolveTextureOffset(keyPrefix, "texture_y_offset") + extraYOffset,
            0.0f
        );
        drawAnchoredResourceTextureFrame(
            guiGraphics,
            texture,
            frame,
            drawWidth,
            drawHeight,
            spec.anchorXRatio(),
            spec.anchorYRatio(),
            layerAlpha,
            particleColor,
            spec.additive(),
            false
        );
        guiGraphics.pose().popPose();
    }

    private void renderMirroredParticleBurst(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        TextureFrame frame,
        float drawWidth,
        float drawHeight,
        float centerOffsetX,
        float mirrorOffsetX,
        float centerOffsetY,
        float alpha,
        int rgb,
        boolean additive,
        float anchorXRatio,
        float anchorYRatio
    ) {
        if (alpha <= 0.0f || texture == null) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerOffsetX + mirrorOffsetX, centerOffsetY, 0.0f);
        drawAnchoredResourceTextureFrame(
            guiGraphics,
            texture,
            frame,
            drawWidth,
            drawHeight,
            anchorXRatio,
            anchorYRatio,
            alpha,
            rgb,
            additive,
            false
        );
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerOffsetX - mirrorOffsetX, centerOffsetY, 0.0f);
        drawAnchoredResourceTextureFrame(
            guiGraphics,
            texture,
            frame,
            drawWidth,
            drawHeight,
            1.0f - anchorXRatio,
            anchorYRatio,
            alpha,
            rgb,
            additive,
            true
        );
        guiGraphics.pose().popPose();
    }

    private ResourceLocation resolveParticleTexture(String texturePath, float brightness, float contrast) {
        if (!needsTextureProcessing(texturePath, false, brightness, contrast)) {
            return ModTextures.get(texturePath);
        }
        ResourceLocation processedTexture = IconGlowProcessor.getOrCreateProcessedTexture(
            ConfigManager.getCurrentPresetId(),
            texturePath,
            false,
            0xFFFFFF,
            brightness,
            contrast
        );
        return processedTexture != null ? processedTexture : ModTextures.get(texturePath);
    }

    private void drawCenteredTexture(
        GuiGraphics guiGraphics,
        String textureKey,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha,
        int rgb,
        boolean additive
    ) {
        if (texturePath == null) {
            return;
        }
        ResourceLocation texture = ModTextures.get(texturePath);
        if (texture == null) {
            return;
        }
        TextureFrame frame = resolveAnimatedFrame(textureKey, texturePath);
        drawAnchoredResourceTextureFrame(
            guiGraphics,
            texture,
            frame,
            drawWidth,
            drawHeight,
            0.5f,
            0.5f,
            alpha,
            rgb,
            additive,
            false
        );
    }

    private void drawColorizableTexture(
        GuiGraphics guiGraphics,
        String textureKey,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha,
        int rgb,
        boolean additive,
        boolean accentTintEnabled,
        int accentColor,
        float brightness,
        float contrast
    ) {
        alpha *= PreviewTextureFocusContext.alphaMultiplier("kill_icon/valorant", textureKey);
        if (alpha <= 0.001f) {
            return;
        }
        float finalOpacity = resolveTextureFinalOpacity(textureKey);
        if (finalOpacity <= 0.001f) {
            return;
        }
        alpha *= finalOpacity;
        if (alpha <= 0.001f) {
            return;
        }
        float offsetX = resolveTextureOffset(textureKey, "texture_x_offset");
        float offsetY = resolveTextureOffset(textureKey, "texture_y_offset");
        boolean translated = Math.abs(offsetX) > 0.001f || Math.abs(offsetY) > 0.001f;
        if (translated) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(offsetX, offsetY, 0.0f);
        }
        ResourceLocation processedTexture = null;
        boolean useProcessedTexture = needsTextureProcessing(texturePath, accentTintEnabled, brightness, contrast);
        if (useProcessedTexture) {
            processedTexture = IconGlowProcessor.getOrCreateProcessedTexture(
                ConfigManager.getCurrentPresetId(),
                texturePath,
                accentTintEnabled,
                accentColor,
                brightness,
                contrast
            );
        }

        if (configIconGlowEnabled) {
            if (processedTexture != null) {
                drawGlowResourceTexture(guiGraphics, processedTexture, textureKey, texturePath, drawWidth, drawHeight, alpha);
            } else {
                drawGlowTexture(guiGraphics, textureKey, texturePath, drawWidth, drawHeight, alpha, accentTintEnabled, accentColor, brightness, contrast);
            }
        }

        if (!useProcessedTexture || processedTexture == null) {
            drawCenteredTexture(guiGraphics, textureKey, texturePath, drawWidth, drawHeight, alpha, rgb, additive);
            if (translated) {
                guiGraphics.pose().popPose();
            }
            return;
        }

        drawCenteredResourceTexture(guiGraphics, processedTexture, textureKey, texturePath, drawWidth, drawHeight, alpha, rgb, additive);
        if (translated) {
            guiGraphics.pose().popPose();
        }
    }

    private void drawCenteredResourceTexture(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        String textureKey,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha,
        int rgb,
        boolean additive
    ) {
        if (texture == null) {
            return;
        }
        TextureFrame frame = resolveAnimatedFrame(textureKey, texturePath);
        drawAnchoredResourceTextureFrame(
            guiGraphics,
            texture,
            frame,
            drawWidth,
            drawHeight,
            0.5f,
            0.5f,
            alpha,
            rgb,
            additive,
            false
        );
    }

    private void drawGlowTexture(
        GuiGraphics guiGraphics,
        String textureKey,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha,
        boolean accentTintEnabled,
        int accentColor,
        float brightness,
        float contrast
    ) {
        if (texturePath == null || texturePath.isEmpty()) {
            return;
        }
        boolean useProcessedTexture = needsTextureProcessing(texturePath, accentTintEnabled, brightness, contrast);
        if (useProcessedTexture) {
            ResourceLocation processedTexture = IconGlowProcessor.getOrCreateProcessedTexture(
                ConfigManager.getCurrentPresetId(),
                texturePath,
                accentTintEnabled,
                accentColor,
                brightness,
                contrast
            );
            if (processedTexture != null) {
                drawGlowResourceTexture(guiGraphics, processedTexture, textureKey, texturePath, drawWidth, drawHeight, alpha);
                return;
            }
        }
        ResourceLocation texture = ModTextures.get(texturePath);
        if (texture != null) {
            drawGlowResourceTexture(guiGraphics, texture, textureKey, texturePath, drawWidth, drawHeight, alpha);
        }
    }

    private void drawGlowResourceTexture(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        String textureKey,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha
    ) {
        if (texture == null || !configIconGlowEnabled || configIconGlowSize <= 0.01f || configIconGlowIntensity <= 0.001f || alpha <= 0.001f) {
            return;
        }

        float outerSpread = Math.max(0.5f, configIconGlowSize);
        float innerSpread = outerSpread * 0.55f;
        float glowAlpha = Mth.clamp(alpha * configIconGlowIntensity, 0.0f, 1.0f);
        int color = configIconGlowColor;
        TextureFrame frame = resolveAnimatedFrame(textureKey, texturePath);

        for (float[] offset : GLOW_OFFSETS) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(offset[0] * outerSpread, offset[1] * outerSpread, 0.0f);
            drawAnchoredResourceTextureFrame(guiGraphics, texture, frame, drawWidth, drawHeight, 0.5f, 0.5f, glowAlpha * 0.16f, color, true, false);
            guiGraphics.pose().popPose();
        }

        for (float[] offset : GLOW_OFFSETS) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(offset[0] * innerSpread, offset[1] * innerSpread, 0.0f);
            drawAnchoredResourceTextureFrame(guiGraphics, texture, frame, drawWidth, drawHeight, 0.5f, 0.5f, glowAlpha * 0.11f, color, true, false);
            guiGraphics.pose().popPose();
        }

        drawAnchoredResourceTextureFrame(guiGraphics, texture, frame, drawWidth, drawHeight, 0.5f, 0.5f, glowAlpha * 0.09f, color, true, false);
    }

    private TextureFrame resolveAnimatedFrame(String textureKey, String texturePath) {
        if (texturePath == null || texturePath.isEmpty() || textureKey == null || textureKey.isEmpty()) {
            return new TextureFrame(0, 0, 1, 1, 1, 1);
        }
        return IconTextureAnimationManager.getTextureFrame(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            textureKey,
            texturePath,
            startTime,
            currentConfig
        );
    }

    private void drawAnchoredResourceTextureFrame(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        TextureFrame frame,
        float drawWidth,
        float drawHeight,
        float anchorXRatio,
        float anchorYRatio,
        float alpha,
        int rgb,
        boolean additive,
        boolean flipX
    ) {
        if (texture == null || frame == null) {
            return;
        }
        if (additive) {
            RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
            );
        } else {
            RenderSystem.defaultBlendFunc();
        }
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        float x0 = -drawWidth * anchorXRatio;
        float y0 = -drawHeight * anchorYRatio;
        float x1 = x0 + drawWidth;
        float y1 = y0 + drawHeight;
        float u0 = frame.u / (float) frame.totalWidth;
        float u1 = (frame.u + frame.width) / (float) frame.totalWidth;
        float v0 = frame.v / (float) frame.totalHeight;
        float v1 = (frame.v + frame.height) / (float) frame.totalHeight;
        if (flipX) {
            float swapped = u0;
            u0 = u1;
            u1 = swapped;
        }

        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(red, green, blue, alpha);

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, x0, y1, 0.0f).uv(u0, v1).endVertex();
        builder.vertex(matrix, x1, y1, 0.0f).uv(u1, v1).endVertex();
        builder.vertex(matrix, x1, y0, 0.0f).uv(u1, v0).endVertex();
        builder.vertex(matrix, x0, y0, 0.0f).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    private void renderBarRing(
        GuiGraphics guiGraphics,
        String barTexturePath,
        float barWidth,
        float barHeight,
        float barTravel,
        float alpha,
        float barRotation,
        float barRadiusOffset,
        float barCenterXOffset,
        float barCenterYOffset,
        float[] layoutAngles,
        boolean accentTintEnabled,
        int accentColor,
        float brightness,
        float contrast,
        long elapsed
    ) {
        float distance = BAR_RING_RADIUS + barRadiusOffset + barTravel;
        float entryScale = resolveBarEntryScale(elapsed);

        for (float layoutAngle : layoutAngles) {
            guiGraphics.pose().pushPose();
            if (barCenterXOffset != 0.0f || barCenterYOffset != 0.0f) {
                guiGraphics.pose().translate(barCenterXOffset, barCenterYOffset, 0.0f);
            }
            guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(layoutAngle + barRotation));
            guiGraphics.pose().translate(0.0f, -distance, 0.0f);
            if (Math.abs(entryScale - 1.0f) > 0.001f) {
                guiGraphics.pose().scale(entryScale, entryScale, 1.0f);
            }

            drawColorizableTexture(
                guiGraphics,
                "bar",
                barTexturePath,
                barWidth,
                barHeight,
                alpha,
                0xFFFFFF,
                false,
                accentTintEnabled,
                accentColor,
                brightness,
                contrast
            );
            RenderSystem.defaultBlendFunc();
            guiGraphics.pose().popPose();
        }
    }

    private float resolveBarEntryScale(long elapsed) {
        long durationMs = Math.round(configBarEntryDuration * 1000.0f);
        if (durationMs <= 0L || configBarEntryInitialScale <= 1.0f) {
            return 1.0f;
        }
        if (elapsed >= durationMs) {
            return 1.0f;
        }
        float t = Mth.clamp((float) elapsed / (float) durationMs, 0.0f, 1.0f);
        return Mth.lerp(easeOutCubic(t), configBarEntryInitialScale, 1.0f);
    }

    private boolean isAccentTintableTexture(String texturePath) {
        return texturePath != null && !texturePath.isEmpty();
    }

    private boolean needsTextureProcessing(String texturePath, boolean accentTintEnabled, float brightness, float contrast) {
        if (texturePath == null || texturePath.isEmpty()) {
            return false;
        }
        if (accentTintEnabled && isAccentTintableTexture(texturePath)) {
            return true;
        }
        return Math.abs(brightness - 1.0f) > 0.001f || Math.abs(contrast - 1.0f) > 0.001f;
    }

    private static float easeOutCubic(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        float inverted = 1.0f - clamped;
        return 1.0f - inverted * inverted * inverted;
    }

    private static float easeInQuart(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * clamped * clamped;
    }

    private static float easeInQuad(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped;
    }

    private static float easeOutQuad(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        float inverted = 1.0f - clamped;
        return 1.0f - inverted * inverted;
    }

    private static float easeOutSoft(float value) {
        float quad = easeOutQuad(value);
        float cubic = easeOutCubic(value);
        return Mth.lerp(0.2f, quad, cubic);
    }

    private static int applyAlpha(int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        return (a << 24) | rgb;
    }

    private float resolveFrameBladeRotation(String styleId, long elapsed) {
        if (!configBladeRotationEffect) {
            return 0.0f;
        }
        float baseDegPerSecond = 1350.0f;
        if (styleId != null && styleId.contains("gaia")) {
            baseDegPerSecond = 1100.0f;
        }
        float windowSec = Math.max(0.2f, configBladeDecelerationWindowSec);
        float t = Mth.clamp(elapsed / (windowSec * 1000.0f), 0.0f, 1.0f);
        float totalDegrees = baseDegPerSecond * windowSec * (1.0f - (float) Math.pow(1.0f - t, 4.0f)) * 0.25f;
        return -(totalDegrees * spinDirection);
    }

    private float resolveEffectiveBrightness(boolean gaiaSkin) {
        return gaiaSkin ? configBrightness * GAIA_DEFAULT_BRIGHTNESS : configBrightness;
    }

    private float resolveEffectiveContrast(boolean gaiaSkin) {
        return gaiaSkin ? GAIA_DEFAULT_CONTRAST : 1.0f;
    }

    private String resolveStyleId() {
        return ValorantStyleCatalog.resolveStyleId(ConfigManager.getCurrentPresetId(), currentConfig);
    }

    private String resolveBladeTexturePath(String styleId, String selectedBladeTexturePath) {
        if (selectedBladeTexturePath != null && !selectedBladeTexturePath.isBlank()) {
            return selectedBladeTexturePath;
        }
        String styleBlade = ValorantStyleCatalog.getFrameBladeTextureFileName(styleId);
        if (styleBlade != null && !styleBlade.isBlank()) {
            return styleBlade;
        }
        return ValorantStyleCatalog.getOfficialTextureFileNameForStyle(ValorantStyleCatalog.STYLE_BUBBLEGUM_DEATHWISH, "blade");
    }

    private boolean isGaiaSkin(String styleId, String iconTexturePath, String barTexturePath) {
        if (styleId != null && styleId.contains("gaia")) {
            return true;
        }
        return (iconTexturePath != null && iconTexturePath.contains("gaia"))
            || (barTexturePath != null && barTexturePath.contains("gaia"));
    }

    private boolean isEntryMotionFrameLocked(String styleId) {
        return ValorantStyleCatalog.useEmblemOnlyEntryMotion(styleId);
    }

    private record FlashState(int color, float alpha) {
        private static FlashState none() {
            return new FlashState(0xFFFFFF, 0.0f);
        }
    }

    private record HeadshotAnimState(float scale, int color) {
        private static HeadshotAnimState idle(int restColor) {
            return new HeadshotAnimState(1.0f, restColor);
        }
    }

    private record SmallParticle(
        float angleDeg,
        float tiltDeg,
        float startRadius,
        float endRadius,
        float width,
        float height,
        float alphaScale,
        float jagScale,
        float bendScale,
        float lateralBoost,
        float gravityPx,
        long delayMs
    ) {
    }

    private HeadshotAnimState resolveHeadshotAnimState(long elapsed, int restColor) {
        long durationMs = Math.round(configHeadshotAnimDuration * 1000.0f);
        if (durationMs <= 0L || configHeadshotAnimInitialScale <= 1.0f) {
            return HeadshotAnimState.idle(restColor);
        }
        if (elapsed >= durationMs) {
            return HeadshotAnimState.idle(restColor);
        }

        float t = Mth.clamp((float) elapsed / (float) durationMs, 0.0f, 1.0f);
        float scale = Mth.lerp(easeOutCubic(t), configHeadshotAnimInitialScale, 1.0f);
        float flickerFreq = 18.0f;
        int color;
        if (flickerFreq <= 0.0f) {
            color = restColor;
        } else {
            float phase = (float) Math.sin(elapsed * flickerFreq * Math.PI * 2.0 / 1000.0);
            boolean flashWhite = phase > 0.0f;
            float envelope = (1.0f - t) * (1.0f - t);
            color = flashWhite ? lerpColor(restColor, 0xFFFFFF, envelope) : restColor;
        }

        return new HeadshotAnimState(scale, color);
    }

    private static int lerpColor(int colorA, int colorB, float t) {
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;
        int r = Math.round(Mth.lerp(t, rA, rB));
        int g = Math.round(Mth.lerp(t, gA, gB));
        int b = Math.round(Mth.lerp(t, bA, bB));
        return (r << 16) | (g << 8) | b;
    }
}
