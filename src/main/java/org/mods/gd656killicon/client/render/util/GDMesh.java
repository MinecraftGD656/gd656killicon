package org.mods.gd656killicon.client.render.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2fc;

import java.lang.reflect.Field;

/**
 * 26.1 自定义顶点几何提交。
 * 原版 1.20.1: positionColorShader + BufferUploader.drawWithShader (立即上屏)
 * 26.1 等价:   GuiRenderState.addGuiElement, pipeline = RenderPipelines.GUI (blend=TRANSLUCENT)
 * 顶点以原始坐标记录, 在 buildVertices 中经 addVertexWith2DPose(pose, x, y) 重放,
 * 与官方 ColoredRectangleRenderState 行为一致 (不预先乘 pose, 避免双重变换)。
 */
public final class GDMesh {
    private static final Field GRS;
    private static volatile boolean disabled = false;

    static {
        Field f = null;
        try {
            f = GuiGraphicsExtractor.class.getDeclaredField("guiRenderState");
            f.setAccessible(true);
        } catch (Throwable t) { f = null; }
        GRS = f;
    }

    public static Builder begin() { return new Builder(); }

    /** 可选: 绑定单张纹理。不调用则保持无纹理(GUI 管线), 现有调用点行为不变。 */
    public static Builder beginTextured(Identifier id) {
        Builder b = new Builder();
        try {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(id);
            if (tex != null) {
                b.texSetup = TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler());
            }
        } catch (Throwable ignored) { }
        return b;
    }

    public static void submit(GuiGraphicsExtractor g, Builder b) {
        if (disabled || GRS == null || b == null || b.count == 0) return;
        GuiRenderState state;
        try { state = (GuiRenderState) GRS.get(g); }
        catch (Throwable t) { disabled = true; return; }
        if (state == null) return;
        try {
                        state.addGuiElement(new Element(b)); }
        catch (Throwable t) { disabled = true; }
    }

    /** 采集器: 签名与 BufferBuilder 的 addVertexWith2DPose / setColor 一致, 调用点无需改动。 */
    public static final class Builder implements VertexConsumer {
        float[] xs = new float[256];
        float[] ys = new float[256];
        int[] cols = new int[256];
        float[] us = new float[256];
        float[] vs = new float[256];
        TextureSetup texSetup = null;
        int count = 0;
        Matrix3x2fc pose;

        private void ensure() {
            if (count + 1 > xs.length) {
                float[] nx = new float[xs.length * 2]; System.arraycopy(xs, 0, nx, 0, count); xs = nx;
                float[] ny = new float[ys.length * 2]; System.arraycopy(ys, 0, ny, 0, count); ys = ny;
                int[] nc = new int[cols.length * 2]; System.arraycopy(cols, 0, nc, 0, count); cols = nc;
                float[] nu = new float[us.length * 2]; System.arraycopy(us, 0, nu, 0, count); us = nu;
                float[] nv = new float[vs.length * 2]; System.arraycopy(vs, 0, nv, 0, count); vs = nv;
            }
        }

        @Override public VertexConsumer addVertexWith2DPose(Matrix3x2fc pose, float x, float y) {
            this.pose = pose; ensure();
            xs[count] = x; ys[count] = y; cols[count] = 0xFFFFFFFF; us[count] = 0.0f; vs[count] = 0.0f; count++;
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            if (count > 0) cols[count - 1] = (a << 24) | (r << 16) | (g << 8) | b;
            return this;
        }
        @Override public VertexConsumer setColor(int argb) {
            if (count > 0) cols[count - 1] = argb;
            return this;
        }
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            ensure(); xs[count] = x; ys[count] = y; cols[count] = 0xFFFFFFFF; us[count] = 0.0f; vs[count] = 0.0f; count++; return this;
        }
        @Override public VertexConsumer setUv(float u, float v) {
            if (count > 0) { us[count - 1] = u; vs[count - 1] = v; }
            return this;
        }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public VertexConsumer setLineWidth(float w) { return this; }
    }

    private static final class Element implements GuiElementRenderState {
        private final Builder b;
        Element(Builder b) { this.b = b; }

        @Override public void buildVertices(VertexConsumer vc) {
            Matrix3x2fc p = b.pose;
            for (int i = 0; i < b.count; i++) {
                vc.addVertexWith2DPose(p, b.xs[i], b.ys[i]).setColor(b.cols[i]).setUv(b.us[i], b.vs[i]);
            }
        }
        @Override public RenderPipeline pipeline() {
            return b.texSetup != null ? RenderPipelines.GUI_TEXTURED : RenderPipelines.GUI;
        }
        @Override public TextureSetup textureSetup() {
            return b.texSetup != null ? b.texSetup : TextureSetup.noTexture();
        }
        @Override public ScreenRectangle scissorArea() { return null; }
        @Override public ScreenRectangle bounds() {
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (int i = 0; i < b.count; i++) {
                if (b.xs[i] < minX) minX = b.xs[i];
                if (b.xs[i] > maxX) maxX = b.xs[i];
                if (b.ys[i] < minY) minY = b.ys[i];
                if (b.ys[i] > maxY) maxY = b.ys[i];
            }
            return new ScreenRectangle((int) minX, (int) minY,
                    Math.max(1, (int) (maxX - minX)), Math.max(1, (int) (maxY - minY)))
                    .transformMaxBounds(b.pose);
        }
    }
}
