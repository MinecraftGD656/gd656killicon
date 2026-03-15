package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.config.ValorantSkinProfileManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.textures.IconTextureAnimationManager;
import org.mods.gd656killicon.client.textures.IconTextureAnimationManager.TextureFrame;
import org.mods.gd656killicon.client.textures.ModTextures;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ValorantIconRenderer implements IHudRenderer {
    private static final long DEFAULT_DISPLAY_DURATION = 2600L;
    private static final long FLASH_DURATION_MS = 240L;
    private static final long FIVE_PLUS_FADE_IN_DURATION_MS = 300L;
    private static final long PARTICLE_DURATION_MS = 620L;
    private static final long ICON_RETURN_DURATION_MS = 100L;
    private static final long BAR_BOUNCE_START_MS = 20L;
    private static final long BAR_BOUNCE_PEAK_MS = 180L;
    private static final long BAR_BOUNCE_HOLD_END_MS = 270L;
    private static final long BAR_BOUNCE_END_MS = 620L;
    private static final long BAR_SPIN_START_MS = 750L;
    private static final long BAR_SPIN_DURATION_180_MS = 700L;
    private static final long BAR_SPIN_DURATION_360_MS = 1000L;
    private static final long FADE_OUT_DURATION_MS = 220L;

    private static final float ICON_START_OFFSET_PX = -16.0f;
    private static final float BAR_BOUNCE_DISTANCE_PX = 9.0f;
    private static final float VISUAL_CENTER_Y_OFFSET_PX = 6.0f;
    private static final float GLOBAL_VISUAL_SCALE = 0.95f;
    private static final float ICON_BASE_SIZE = 116.0f;
    private static final float BAR_BASE_SIZE = 32.0f;
    private static final float BAR_RING_RADIUS = 36.0f;
    private static final float PRIME_BAR_CENTER_X_OFFSET_PX = -1.0f;
    private static final float PRIME_BAR_CENTER_Y_OFFSET_PX = 2.0f;
    private static final float PRIME_BAR_RADIUS_OFFSET_PX = -2.0f;
    private static final float GAIA_BAR_SCALE = 0.82f;
    private static final float GAIA_BAR_CENTER_X_OFFSET_PX = 0.0f;
    private static final float GAIA_BAR_CENTER_Y_OFFSET_PX = -10.0f;
    private static final float GAIA_BAR_RADIUS_OFFSET_PX = 0.0f;
    private static final int FLASH_LIGHT_COLOR = 0xFF7474;
    private static final int FLASH_DARK_COLOR = 0x8C121A;
    private static final int PRIME_PARTICLE_SMALL_COLOR = 0xFFD138;
    private static final int GAIA_PARTICLE_SMALL_COLOR = 0xE2505C;
    private static final int DEFAULT_ACCENT_COLOR = 0xE2505C;
    private static final float[][] BAR_LAYOUT_ANGLES = new float[][]{
        {0.0f},
        {-90.0f, 90.0f},
        {0.0f, -120.0f, 120.0f},
        {0.0f, -90.0f, 90.0f, 180.0f},
        {0.0f, -72.0f, 72.0f, -144.0f, 144.0f},
        {0.0f, -60.0f, 60.0f, -120.0f, 120.0f, 180.0f}
    };
    private static final Map<String, ResourceLocation> PROCESSED_TEXTURE_CACHE = new HashMap<>();

    private float configScale = 1.0f;
    private int configXOffset = 0;
    private int configYOffset = 80;
    private int configBarXOffset = 0;
    private int configBarYOffset = 0;
    private int configBarRadiusOffset = 0;
    private float configIconScale = 1.0f;
    private float configBarScale = 1.0f;
    private float configBrightness = 1.0f;
    private float configContrast = 1.0f;
    private float configParticleIntensity = 1.0f;
    private float configParticleDirection = 0.0f;
    private boolean configCustomParticleColorEnabled = false;
    private int configParticleColor = PRIME_PARTICLE_SMALL_COLOR;
    private long displayDuration = DEFAULT_DISPLAY_DURATION;
    private JsonObject currentConfig;

    private long startTime = -1L;
    private boolean visible = false;
    private int comboCount = 1;
    private float spinDirection = 1.0f;
    private SmallParticle[] smallParticles = new SmallParticle[0];

    @Override
    public void trigger(TriggerContext context) {
        if (context.comboCount() <= 0) {
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
        spinDirection = ThreadLocalRandom.current().nextBoolean() ? 1.0f : -1.0f;
        generateParticleState();
        startTime = System.currentTimeMillis();
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

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > displayDuration) {
            visible = false;
            startTime = -1L;
            return;
        }

        float alpha = resolveAlpha(elapsed);
        float iconOffsetY = resolveIconOffsetY(elapsed);
        float barTravel = resolveBarTravel(elapsed);
        float barRotation = resolveBarRotation(elapsed);
        FlashState flash = resolveFlashState(elapsed);

        String iconTexturePath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "icon",
            currentConfig
        );
        String barTexturePath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "bar",
            currentConfig
        );
        boolean gaiaSkin = isGaiaSkin(iconTexturePath, barTexturePath);
        TextureFrame iconFrame = IconTextureAnimationManager.getTextureFrame(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "icon",
            iconTexturePath,
            startTime,
            currentConfig
        );
        TextureFrame barFrame = IconTextureAnimationManager.getTextureFrame(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/valorant",
            "bar",
            barTexturePath,
            startTime,
            currentConfig
        );
        boolean iconAnimEnabled = isTextureAnimationEnabled("icon");
        boolean barAnimEnabled = isTextureAnimationEnabled("bar");

        float iconWidth;
        float iconHeight;
        if (iconAnimEnabled && iconFrame != null) {
            float aspectRatio = (float) Math.max(1, iconFrame.height) / (float) Math.max(1, iconFrame.width);
            iconWidth = ICON_BASE_SIZE * configIconScale;
            iconHeight = iconWidth * aspectRatio;
        } else {
            iconWidth = ICON_BASE_SIZE * configIconScale * resolveFrameRatio("icon", "texture_frame_width_ratio");
            iconHeight = ICON_BASE_SIZE * configIconScale * resolveFrameRatio("icon", "texture_frame_height_ratio");
        }
        float barBaseSize = BAR_BASE_SIZE * configBarScale * (gaiaSkin ? GAIA_BAR_SCALE : 1.0f);
        float barWidth;
        float barHeight;
        if (barAnimEnabled && barFrame != null) {
            float aspectRatio = (float) Math.max(1, barFrame.height) / (float) Math.max(1, barFrame.width);
            barWidth = barBaseSize;
            barHeight = barBaseSize * aspectRatio;
        } else {
            barWidth = barBaseSize * resolveFrameRatio("bar", "texture_frame_width_ratio");
            barHeight = barBaseSize * resolveFrameRatio("bar", "texture_frame_height_ratio");
        }
        boolean accentTintEnabled = isAccentTintEnabled();
        int accentColor = resolveAccentColor();
        int particleColor = resolveParticleColor(accentTintEnabled, accentColor, gaiaSkin);
        particleColor = applyBrightnessContrastToRgb(particleColor, configBrightness, configContrast);
        float barRadiusOffset = (gaiaSkin ? GAIA_BAR_RADIUS_OFFSET_PX : PRIME_BAR_RADIUS_OFFSET_PX) + configBarRadiusOffset;
        float barCenterXOffset = (gaiaSkin ? GAIA_BAR_CENTER_X_OFFSET_PX : PRIME_BAR_CENTER_X_OFFSET_PX) + configBarXOffset;
        float barCenterYOffset = (gaiaSkin ? GAIA_BAR_CENTER_Y_OFFSET_PX : PRIME_BAR_CENTER_Y_OFFSET_PX) + configBarYOffset;
        float[] barLayoutAngles = BAR_LAYOUT_ANGLES[Mth.clamp(comboCount, 1, 6) - 1];

        try {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY + iconOffsetY, 0.0f);
            float renderScale = configScale * GLOBAL_VISUAL_SCALE;
            guiGraphics.pose().scale(renderScale, renderScale, 1.0f);

            renderSmallParticles(guiGraphics, elapsed, alpha, particleColor);

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
                barFrame
            );
            RenderSystem.defaultBlendFunc();

            drawColorizableTexture(guiGraphics, iconTexturePath, iconWidth, iconHeight, alpha, 0xFFFFFF, false, accentTintEnabled, accentColor, iconFrame);
            if (flash.alpha() > 0.0f) {
                drawColorizableTexture(guiGraphics, iconTexturePath, iconWidth, iconHeight, alpha * flash.alpha(), flash.color(), true, accentTintEnabled, accentColor, iconFrame);
            }
            RenderSystem.defaultBlendFunc();

            guiGraphics.pose().popPose();
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void loadConfig(JsonObject config) {
        currentConfig = config;
        configScale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
        configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 80;
        configBarXOffset = config.has("bar_x_offset") ? config.get("bar_x_offset").getAsInt() : 0;
        configBarYOffset = config.has("bar_y_offset") ? config.get("bar_y_offset").getAsInt() : 0;
        configBarRadiusOffset = config.has("bar_radius_offset") ? config.get("bar_radius_offset").getAsInt() : 0;
        configIconScale = config.has("icon_scale") ? config.get("icon_scale").getAsFloat() : 1.0f;
        configBarScale = config.has("bar_scale") ? config.get("bar_scale").getAsFloat() : 1.0f;
        configBrightness = config.has("brightness") ? config.get("brightness").getAsFloat() : 1.0f;
        configContrast = config.has("contrast") ? config.get("contrast").getAsFloat() : 1.0f;
        configParticleIntensity = config.has("particle_intensity") ? config.get("particle_intensity").getAsFloat() : 1.0f;
        configParticleDirection = config.has("particle_direction") ? config.get("particle_direction").getAsFloat() : 0.0f;
        configCustomParticleColorEnabled = config.has("enable_custom_particle_color") && config.get("enable_custom_particle_color").getAsBoolean();
        configParticleColor = resolveConfiguredColor("color_particle", PRIME_PARTICLE_SMALL_COLOR);
        displayDuration = resolveDisplayDuration();
    }

    private long resolveDisplayDuration() {
        long serverDuration = ComboIconRenderer.getServerComboWindowMs();
        if (serverDuration > 0L) {
            return serverDuration;
        }
        return DEFAULT_DISPLAY_DURATION;
    }

    private float resolveAlpha(long elapsed) {
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

    private float resolveIconOffsetY(long elapsed) {
        if (elapsed >= ICON_RETURN_DURATION_MS) {
            return 0.0f;
        }
        float progress = easeOutCubic((float) elapsed / (float) ICON_RETURN_DURATION_MS);
        return Mth.lerp(progress, ICON_START_OFFSET_PX, 0.0f);
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
        if (comboCount <= 1 || comboCount >= 6 || elapsed <= BAR_SPIN_START_MS) {
            return 0.0f;
        }
        long rotationDuration = comboCount >= 5 ? BAR_SPIN_DURATION_360_MS : BAR_SPIN_DURATION_180_MS;
        float targetRotation = comboCount >= 5 ? 360.0f : 180.0f;
        float progress = (float) (elapsed - BAR_SPIN_START_MS) / (float) rotationDuration;
        progress = Mth.clamp(progress, 0.0f, 1.0f);
        return targetRotation * easeOutCubic(progress) * spinDirection;
    }

    private FlashState resolveFlashState(long elapsed) {
        if (elapsed >= FLASH_DURATION_MS) {
            return FlashState.none();
        }
        float progress = (float) elapsed / (float) FLASH_DURATION_MS;
        float cycles = progress * 3.0f;
        int phase = Math.min(5, (int) Math.floor(cycles * 2.0f));
        float local = cycles * 2.0f - phase;
        float pulseAlpha = 1.0f - Math.abs(local - 0.5f) * 2.0f;
        pulseAlpha = Mth.clamp(pulseAlpha, 0.0f, 1.0f) * (1.0f - progress * 0.35f);
        int color = (phase & 1) == 0 ? FLASH_LIGHT_COLOR : FLASH_DARK_COLOR;
        return new FlashState(color, pulseAlpha);
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

    private void generateParticleState() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        smallParticles = createSmallParticles(random);
    }

    private SmallParticle[] createSmallParticles(ThreadLocalRandom random) {
        float intensity = Mth.clamp(configParticleIntensity, 0.0f, 3.0f);
        if (intensity <= 0.01f) {
            return new SmallParticle[0];
        }

        int baseParticleCount = comboCount <= 1 ? 28 : 22 + comboCount * 3;
        int particleCount = Math.max(1, Math.round(baseParticleCount * intensity));
        float visibilityScale = Mth.lerp(Mth.clamp(intensity / 2.0f, 0.0f, 1.0f), 0.78f, 1.16f);
        float directionOffset = configParticleDirection;
        SmallParticle[] particles = new SmallParticle[particleCount];

        for (int index = 0; index < particleCount; index++) {
            boolean rightSide = (index & 1) == 0;
            float angleBase = rightSide ? 90.0f : -90.0f;
            float sideSpread = comboCount <= 1 ? 44.0f : 56.0f;
            float angle = angleBase + directionOffset + random.nextFloat(-sideSpread, sideSpread);
            if (index % 4 == 0) {
                angle += rightSide ? random.nextFloat(-24.0f, -8.0f) : random.nextFloat(8.0f, 24.0f);
            }
            float startRadius = random.nextFloat(8.0f, 16.0f);
            float endRadius = comboCount <= 1
                ? random.nextFloat(54.0f, 82.0f)
                : random.nextFloat(62.0f, 92.0f);
            float width = random.nextFloat(7.0f, 13.5f) * visibilityScale;
            float height = random.nextFloat(1.8f, 4.0f) * visibilityScale;
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

    private int resolveParticleColor(boolean accentTintEnabled, int accentColor, boolean gaiaSkin) {
        if (configCustomParticleColorEnabled) {
            return configParticleColor;
        }
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

    private void renderSmallParticles(GuiGraphics guiGraphics, long elapsed, float alpha, int particleRgb) {
        if (alpha <= 0.0f || elapsed >= PARTICLE_DURATION_MS) {
            return;
        }
        for (SmallParticle particle : smallParticles) {
            long particleElapsed = elapsed - particle.delayMs();
            if (particleElapsed < 0L || particleElapsed > PARTICLE_DURATION_MS) {
                continue;
            }
            float progress = Mth.clamp((float) particleElapsed / (float) PARTICLE_DURATION_MS, 0.0f, 1.0f);
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

    private void drawCenteredTexture(
        GuiGraphics guiGraphics,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha,
        int rgb,
        boolean additive,
        TextureFrame frame
    ) {
        if (texturePath == null) {
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
        RenderSystem.setShaderColor(red, green, blue, alpha);
        TextureFrame renderFrame = normalizeFrame(frame);
        int drawX = Math.round(-drawWidth / 2.0f);
        int drawY = Math.round(-drawHeight / 2.0f);
        int drawW = Math.max(1, Math.round(drawWidth));
        int drawH = Math.max(1, Math.round(drawHeight));
        if (renderFrame == null) {
            guiGraphics.blit(
                ModTextures.get(texturePath),
                drawX,
                drawY,
                0,
                0,
                drawW,
                drawH,
                drawW,
                drawH
            );
            return;
        }
        guiGraphics.blit(
            ModTextures.get(texturePath),
            drawX,
            drawY,
            drawW,
            drawH,
            renderFrame.u,
            renderFrame.v,
            Math.max(1, renderFrame.width),
            Math.max(1, renderFrame.height),
            Math.max(1, renderFrame.totalWidth),
            Math.max(1, renderFrame.totalHeight)
        );
    }

    private void drawColorizableTexture(
        GuiGraphics guiGraphics,
        String texturePath,
        float drawWidth,
        float drawHeight,
        float alpha,
        int rgb,
        boolean additive,
        boolean accentTintEnabled,
        int accentColor,
        TextureFrame frame
    ) {
        if (!needsTextureProcessing(texturePath, accentTintEnabled)) {
            drawCenteredTexture(guiGraphics, texturePath, drawWidth, drawHeight, alpha, rgb, additive, frame);
            return;
        }

        ResourceLocation processedTexture = getOrCreateProcessedTexture(
            ConfigManager.getCurrentPresetId(),
            texturePath,
            accentTintEnabled,
            accentColor,
            configBrightness,
            configContrast
        );
        if (processedTexture == null) {
            drawCenteredTexture(guiGraphics, texturePath, drawWidth, drawHeight, alpha, rgb, additive, frame);
            return;
        }

        drawCenteredResourceTexture(guiGraphics, processedTexture, drawWidth, drawHeight, alpha, rgb, additive, frame);
    }

    private void drawCenteredResourceTexture(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
        float drawWidth,
        float drawHeight,
        float alpha,
        int rgb,
        boolean additive,
        TextureFrame frame
    ) {
        if (texture == null) {
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
        RenderSystem.setShaderColor(red, green, blue, alpha);
        TextureFrame renderFrame = normalizeFrame(frame);
        int drawX = Math.round(-drawWidth / 2.0f);
        int drawY = Math.round(-drawHeight / 2.0f);
        int drawW = Math.max(1, Math.round(drawWidth));
        int drawH = Math.max(1, Math.round(drawHeight));
        if (renderFrame == null) {
            guiGraphics.blit(
                texture,
                drawX,
                drawY,
                0,
                0,
                drawW,
                drawH,
                drawW,
                drawH
            );
            return;
        }
        guiGraphics.blit(
            texture,
            drawX,
            drawY,
            drawW,
            drawH,
            renderFrame.u,
            renderFrame.v,
            Math.max(1, renderFrame.width),
            Math.max(1, renderFrame.height),
            Math.max(1, renderFrame.totalWidth),
            Math.max(1, renderFrame.totalHeight)
        );
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
        TextureFrame barFrame
    ) {
        float distance = BAR_RING_RADIUS + barRadiusOffset + barTravel;

        for (float layoutAngle : layoutAngles) {
            guiGraphics.pose().pushPose();
            if (barCenterXOffset != 0.0f || barCenterYOffset != 0.0f) {
                guiGraphics.pose().translate(barCenterXOffset, barCenterYOffset, 0.0f);
            }
            guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(layoutAngle + barRotation));
            guiGraphics.pose().translate(0.0f, -distance, 0.0f);

            drawColorizableTexture(guiGraphics, barTexturePath, barWidth, barHeight, alpha, 0xFFFFFF, false, accentTintEnabled, accentColor, barFrame);
            RenderSystem.defaultBlendFunc();
            guiGraphics.pose().popPose();
        }
    }

    private TextureFrame normalizeFrame(TextureFrame frame) {
        if (frame == null || frame.width <= 0 || frame.height <= 0 || frame.totalWidth <= 0 || frame.totalHeight <= 0) {
            return null;
        }
        return frame;
    }

    private boolean isTextureAnimationEnabled(String textureKey) {
        if (currentConfig == null || textureKey == null) {
            return false;
        }
        String key = "anim_" + textureKey + "_enable_texture_animation";
        return currentConfig.has(key) && currentConfig.get(key).getAsBoolean();
    }

    private ResourceLocation getOrCreateProcessedTexture(
        String presetId,
        String texturePath,
        boolean accentTintEnabled,
        int accentColor,
        float brightness,
        float contrast
    ) {
        String cacheKey = String.format(
            Locale.ROOT,
            "%s:%s:%b:%06X:%.3f:%.3f",
            presetId,
            texturePath,
            accentTintEnabled,
            accentColor & 0xFFFFFF,
            brightness,
            contrast
        );
        ResourceLocation cached = PROCESSED_TEXTURE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            byte[] bytes = ExternalTextureManager.readTextureBytes(presetId, texturePath);
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) {
                return null;
            }

            BufferedImage processed = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getRGB(x, y);
                    processed.setRGB(x, y, processTexturePixel(argb, accentTintEnabled, accentColor, brightness, contrast));
                }
            }

            ResourceLocation processedTexture = registerBufferedTexture("valorant_fx", cacheKey, processed);
            if (processedTexture == null) {
                return null;
            }
            PROCESSED_TEXTURE_CACHE.put(cacheKey, processedTexture);
            return processedTexture;
        } catch (IOException ignored) {
            return null;
        }
    }

    private ResourceLocation registerBufferedTexture(String prefix, String cacheKey, BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        DynamicTexture texture = new DynamicTexture(NativeImage.read(new ByteArrayInputStream(output.toByteArray())));
        String dynamicName = "gd656killicon_" + prefix + "_" + Integer.toHexString(cacheKey.hashCode());
        return Minecraft.getInstance().getTextureManager().register(dynamicName, texture);
    }

    private boolean isAccentTintableTexture(String texturePath) {
        return texturePath != null && !texturePath.isEmpty();
    }

    private boolean needsTextureProcessing(String texturePath, boolean accentTintEnabled) {
        if (texturePath == null || texturePath.isEmpty()) {
            return false;
        }
        if (accentTintEnabled && isAccentTintableTexture(texturePath)) {
            return true;
        }
        return Math.abs(configBrightness - 1.0f) > 0.001f || Math.abs(configContrast - 1.0f) > 0.001f;
    }

    private boolean isAccentPixel(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha < 16) {
            return false;
        }

        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
        float saturation = hsb[1];
        float brightness = hsb[2];
        return brightness >= 0.18f
            && brightness <= 0.98f
            && saturation >= 0.18f;
    }

    private int processTexturePixel(int argb, boolean accentTintEnabled, int accentColor, float brightness, float contrast) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha <= 0) {
            return 0;
        }

        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;

        if (accentTintEnabled && isAccentPixel(argb)) {
            float luminance = Math.max(red, Math.max(green, blue)) / 255.0f;
            red = Math.round(((accentColor >> 16) & 0xFF) * luminance);
            green = Math.round(((accentColor >> 8) & 0xFF) * luminance);
            blue = Math.round((accentColor & 0xFF) * luminance);
        }

        red = applyBrightnessContrastToChannel(red, brightness, contrast);
        green = applyBrightnessContrastToChannel(green, brightness, contrast);
        blue = applyBrightnessContrastToChannel(blue, brightness, contrast);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private int applyBrightnessContrastToRgb(int rgb, float brightness, float contrast) {
        int red = applyBrightnessContrastToChannel((rgb >> 16) & 0xFF, brightness, contrast);
        int green = applyBrightnessContrastToChannel((rgb >> 8) & 0xFF, brightness, contrast);
        int blue = applyBrightnessContrastToChannel(rgb & 0xFF, brightness, contrast);
        return (red << 16) | (green << 8) | blue;
    }

    private int applyBrightnessContrastToChannel(int channel, float brightness, float contrast) {
        float normalized = channel / 255.0f;
        float contrasted = ((normalized - 0.5f) * contrast) + 0.5f;
        float adjusted = contrasted * brightness;
        return Mth.clamp(Math.round(adjusted * 255.0f), 0, 255);
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

        float[] xs = new float[]{
            -halfLength,
            tailShoulderX,
            upperKinkX,
            halfLength,
            lowerKinkX,
            tailLowerX
        };
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

    private static float easeOutCubic(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        float inverted = 1.0f - clamped;
        return 1.0f - inverted * inverted * inverted;
    }

    private static float easeInCubic(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * clamped;
    }

    private static float easeInQuad(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped;
    }

    private static float easeInQuart(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * clamped * clamped;
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

    private boolean isGaiaSkin(String iconTexturePath, String barTexturePath) {
        if (currentConfig != null && currentConfig.has("skin_style")) {
            return ElementTextureDefinition.VALORANT_SKIN_GAIA.equals(
                ValorantSkinProfileManager.resolveEffectiveBaseSkin(currentConfig)
            );
        }
        return (iconTexturePath != null && iconTexturePath.contains("valorant_gaia"))
            || (barTexturePath != null && barTexturePath.contains("valorant_gaia"));
    }

    private record FlashState(int color, float alpha) {
        private static FlashState none() {
            return new FlashState(0xFFFFFF, 0.0f);
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
}
