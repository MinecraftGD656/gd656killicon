package org.mods.gd656killicon.client.render.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.mods.gd656killicon.common.KillType;

public final class IconRingEffect {
    public static final boolean DEFAULT_ENABLE_ICON_EFFECT = true;

    private static final long EFFECT_DELAY_MS = 100L;
    private static final long EFFECT_DURATION_MS = 300L;
    private static final long EXPLOSION_SECOND_RING_DELAY_MS = 100L;
    private static final float BASE_RADIUS_PX = 10.0f;
    private static final float RADIUS_GROWTH_PX = 32.0f;
    private static final float BASE_THICKNESS_PX = 3.0f;

    private float scale = 1.0f;

    public void setScale(float scale) {
        this.scale = scale;
    }

    private static final int SEGMENTS = 72;

    private static final float EXPLOSION_SECOND_RING_THICKNESS_MULTIPLIER = 1.8f;
    private static final float CRIT_RING_THICKNESS_MULTIPLIER = 0.6f;

    private int killType = KillType.NORMAL;
    private long effectStartTimeMs = -1;
    private int headshotRgb = 0;
    private int explosionRgb = 0;
    private int critRgb = 0;

    public void trigger(long triggerTimeMs, boolean enabled, int killType, int headshotRgb, int explosionRgb, int critRgb) {
        this.killType = killType;
        this.headshotRgb = headshotRgb & 0x00FFFFFF;
        this.explosionRgb = explosionRgb & 0x00FFFFFF;
        this.critRgb = critRgb & 0x00FFFFFF;

        if (!enabled || (killType != KillType.HEADSHOT && killType != KillType.EXPLOSION && killType != KillType.CRIT)) {
            this.effectStartTimeMs = -1;
            return;
        }
        this.effectStartTimeMs = triggerTimeMs + EFFECT_DELAY_MS;
    }

    public void render(GuiGraphics guiGraphics, float centerX, float centerY, long currentTimeMs) {
        if (effectStartTimeMs <= 0) {
            return;
        }

        long effectElapsed = currentTimeMs - effectStartTimeMs;
        long maxEffectDuration = killType == KillType.EXPLOSION
                ? EFFECT_DURATION_MS + EXPLOSION_SECOND_RING_DELAY_MS
                : EFFECT_DURATION_MS;

        if (effectElapsed < 0 || effectElapsed > maxEffectDuration) {
            return;
        }

        float t = Mth.clamp((float) effectElapsed / (float) EFFECT_DURATION_MS, 0.0f, 1.0f);
        float eased = 1.0f - (float) Math.pow(1.0f - t, 3);
        float radius = (BASE_RADIUS_PX + eased * RADIUS_GROWTH_PX) * scale;
        float effectAlpha = 1.0f - t;
        effectAlpha = effectAlpha * effectAlpha;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (killType == KillType.HEADSHOT) {
            float thickness = BASE_THICKNESS_PX * (1.0f - t) * scale;
            drawRing(guiGraphics, centerX, centerY, radius, thickness, headshotRgb, effectAlpha);
            return;
        }

        if (killType == KillType.EXPLOSION) {
            float thickness1 = BASE_THICKNESS_PX * (1.0f - t) * scale;
            drawRing(guiGraphics, centerX, centerY, radius, thickness1, headshotRgb, effectAlpha);

            long ring2Elapsed = effectElapsed - EXPLOSION_SECOND_RING_DELAY_MS;
            if (ring2Elapsed < 0 || ring2Elapsed > EFFECT_DURATION_MS) {
                return;
            }

            float t2 = (float) ring2Elapsed / (float) EFFECT_DURATION_MS;
            float eased2 = 1.0f - (float) Math.pow(1.0f - t2, 3);
            float radius2 = (BASE_RADIUS_PX + eased2 * RADIUS_GROWTH_PX) * scale;
            float alpha2 = 1.0f - t2;
            alpha2 = alpha2 * alpha2;
            float thickness2 = BASE_THICKNESS_PX * EXPLOSION_SECOND_RING_THICKNESS_MULTIPLIER * (1.0f - t2) * scale;
            drawRing(guiGraphics, centerX, centerY, radius2, thickness2, explosionRgb, alpha2);
            return;
        }

        if (killType == KillType.CRIT) {
            float thickness = BASE_THICKNESS_PX * CRIT_RING_THICKNESS_MULTIPLIER * (1.0f - t) * scale;
            drawRing(guiGraphics, centerX, centerY, radius, thickness, critRgb, effectAlpha);
        }
    }

    private static void drawRing(GuiGraphics guiGraphics, float centerX, float centerY, float radius, float thickness, int rgb, float alpha) {
        if (thickness <= 0.0f || alpha <= 0.0f || radius <= 0.0f) {
            return;
        }

        float rOuter = radius + thickness * 0.5f;
        float rInner = Math.max(0.0f, radius - thickness * 0.5f);

        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float) (Math.PI * 2.0 * i / SEGMENTS);
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);

            float xo = centerX + cos * rOuter;
            float yo = centerY + sin * rOuter;
            float xi = centerX + cos * rInner;
            float yi = centerY + sin * rInner;

            builder.vertex(matrix, xo, yo, 0.0f).color(red, green, blue, a).endVertex();
            builder.vertex(matrix, xi, yi, 0.0f).color(red, green, blue, a).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
    }
}
