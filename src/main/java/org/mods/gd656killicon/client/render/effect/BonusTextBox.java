package org.mods.gd656killicon.client.render.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * 加分项字幕的文本框: 包裹单行文本的矩形边框。
 * <p>边框线位于文本外缘: 上下边框距文本上下边缘 1 像素, 左右边框距文本左右边缘 2 像素;
 * 边框线沿该边界以 {@code borderWidth} 厚度向两侧扩展(连续浮点 quad, 亚像素平滑)。
 * <p>透明度与移动逻辑均与单行文本一致: 调用方需在文本相同的 pose 变换下绘制
 * (文本左上角位于原点 0,0), 并传入文本相同的 alpha。
 * <p>渲染方式参考 {@link IconEntranceBackground} 的边框线条(同款 quad + 混合状态)。
 */
public final class BonusTextBox {

    /** 左右边框距文本左右边缘的像素数 */
    private static final float PAD_X = 2.0f;
    /** 上下边框距文本上下边缘的像素数 */
    private static final float PAD_Y = 1.0f;

    private BonusTextBox() {
    }

    /**
     * 绘制文本框边框(应在文本绘制之前调用, 边框在文本下层)。
     *
     * @param guiGraphics 渲染上下文(当前 pose 应已平移到文本左上角)
     * @param textWidth   单行文本宽度(像素, font.width)
     * @param lineHeight  单行文本高度(像素, font.lineHeight)
     * @param borderWidth 边框线条宽度(像素)
     * @param rgb         边框颜色 RGB(不含 alpha)
     * @param alpha       边框透明度(0~255, 与文本相同)
     */
    public static void draw(GuiGraphics guiGraphics, float textWidth, float lineHeight,
                            float borderWidth, int rgb, int alpha) {
        if (borderWidth <= 0.0f || alpha <= 0) {
            return;
        }
        float x0 = -PAD_X;
        float x1 = textWidth + PAD_X;
        float y0 = -PAD_Y;
        float y1 = lineHeight + PAD_Y;
        float halfThickness = borderWidth * 0.5f;

        int a = Math.max(0, Math.min(255, alpha));
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float leftOuter = x0 - halfThickness;
        float leftInner = x0 + halfThickness;
        float rightInner = x1 - halfThickness;
        float rightOuter = x1 + halfThickness;
        float topOuter = y0 - halfThickness;
        float topInner = y0 + halfThickness;
        float bottomInner = y1 - halfThickness;
        float bottomOuter = y1 + halfThickness;
        // 上 / 下 / 左 / 右 四条细边(沿边界向两侧扩展厚度 t)
        quad(builder, matrix, leftOuter, topOuter, rightOuter, topInner, r, g, b, a);
        quad(builder, matrix, leftOuter, bottomInner, rightOuter, bottomOuter, r, g, b, a);
        quad(builder, matrix, leftOuter, topInner, leftInner, bottomInner, r, g, b, a);
        quad(builder, matrix, rightInner, topInner, rightOuter, bottomInner, r, g, b, a);

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix,
                             float x1, float y1, float x2, float y2,
                             int r, int g, int b, int a) {
        builder.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y2, 0).color(r, g, b, a).endVertex();
    }
}
