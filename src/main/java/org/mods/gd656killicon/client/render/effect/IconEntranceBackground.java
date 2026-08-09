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

/**
 * 图标入场背景渲染器。
 *
 * <p>当 kill_icon/scrolling 的 entrance_background 配置项启用时,
 * 主图标入场推迟(时长 = 背景入场持续时间), 期间在图标位置渲染一个正方形矩形:
 * <ul>
 *   <li>t0: 矩形 100% 透明度(完全透明, 不可见);</li>
 *   <li>0 ~ fadeIn: 透明度平滑降至配置的"淡入完成时透明度", 此时主图标开始显示;</li>
 *   <li>fadeIn ~ fadeIn+fadeOut: 矩形渐隐(透明度回到 100%),
 *       同时以矩形几何中心为锚点平滑缩小到不可见(scale 1 → 0)。</li>
 * </ul>
 * 矩形为正方形, 边长为配置的像素值(不再跟随材质大小)。
 *
 * <p>渲染分层: {@link #drawRect} 在图标之前绘制(作为背景), {@link #drawBorder}
 * 在图标之后绘制(永远不透明且处于最上层, 不会被图标遮挡)。
 */
public final class IconEntranceBackground {

    private IconEntranceBackground() {
    }

    /**
     * 绘制入场背景矩形(应在图标之前绘制, 作为背景层)。
     *
     * @param guiGraphics       渲染上下文(使用屏幕坐标, 不依赖图标的 pose 变换)
     * @param elapsedMs         图标自出现以来的毫秒数
     * @param centerX           矩形中心 x(屏幕坐标)
     * @param centerY           矩形中心 y(屏幕坐标)
     * @param size              矩形边长(像素, 正方形)
     * @param color             RGB 颜色(不含 alpha)
     * @param fadeInMs          入场(淡入)持续时间毫秒
     * @param fadeOutMs         出场(淡出+缩小)持续时间毫秒
     * @param peakTransparency  淡入完成时的透明度(0=不透明, 1=完全透明; 渲染 alpha 峰值 = 1 - 该值)
     */
    public static void drawRect(GuiGraphics guiGraphics, long elapsedMs,
                                float centerX, float centerY,
                                float size, int color,
                                long fadeInMs, long fadeOutMs,
                                float peakTransparency) {
        if (elapsedMs < 0 || fadeInMs <= 0 || fadeOutMs <= 0 || elapsedMs >= fadeInMs + fadeOutMs) {
            return;
        }

        float peakAlpha = Mth.clamp(1.0f - peakTransparency, 0.0f, 1.0f);
        float alpha;
        float scale;
        if (elapsedMs < fadeInMs) {
            // 入场: 透明度 100% → 配置值(渲染 alpha 0 → peak), 大小不变
            alpha = peakAlpha * (elapsedMs / (float) fadeInMs);
            scale = 1.0f;
        } else {
            // 出场: 透明度 配置值 → 100%(alpha peak → 0), 同时以几何中心为锚点缩小到不可见
            float progress = (elapsedMs - fadeInMs) / (float) fadeOutMs;
            alpha = peakAlpha * (1.0f - progress);
            scale = 1.0f - progress;
        }

        if (alpha <= 0.001f || scale <= 0.001f) {
            return;
        }

        // 与滚动主图标相同的 pose 变换渲染: translate 到中心 → scale(浮点缩放) → 反平移半尺寸,
        // 顶点经矩阵变换为浮点坐标, 实现亚像素级平滑缩放(不再取整到单个像素)。
        int argb = (color & 0xFFFFFF) | ((int) (Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f) << 24);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.pose().translate(-size / 2f, -size / 2f, 0);
        guiGraphics.fill(0, 0, Math.round(size), Math.round(size), argb);
        guiGraphics.pose().popPose();
    }

    /**
     * 绘制矩形最外侧的亚像素细边框(应在图标之前调用, 与矩形同处背景层, 位于主图标之下)。
     * 使用浮点顶点(自定义 buffer, 参考 GD656Conquest ClientCaptureOutlineController),
     * 宽度可为亚像素值(<1px); 透明度与中心背景矩形使用同一状态机
     * (淡入 0 → 峰值, 淡出 峰值 → 0); 位置/尺寸由当前矩形缩放连续变化(始终跟随矩形, 平滑无步进)。
     *
     * @param color            边框 RGB 颜色(不含 alpha)
     * @param borderWidth      边框宽度(像素, 浮点亚像素; ≤0 不绘制)
     * @param peakTransparency 边框淡入完成时的透明度(0=不透明, 1=完全透明; 渲染 alpha 峰值 = 1 - 该值)
     */
    public static void drawBorder(GuiGraphics guiGraphics, long elapsedMs,
                                  float centerX, float centerY,
                                  float size, int color,
                                  long fadeInMs, long fadeOutMs,
                                  float borderWidth, float peakTransparency) {
        if (borderWidth <= 0.0f) {
            return;
        }
        if (elapsedMs < 0 || fadeInMs <= 0 || fadeOutMs <= 0 || elapsedMs >= fadeInMs + fadeOutMs) {
            return;
        }
        // 与中心背景矩形相同的透明度状态机
        float peakAlpha = Mth.clamp(1.0f - peakTransparency, 0.0f, 1.0f);
        float alpha;
        float scale;
        if (elapsedMs < fadeInMs) {
            alpha = peakAlpha * (elapsedMs / (float) fadeInMs);
            scale = 1.0f;
        } else {
            float progress = (elapsedMs - fadeInMs) / (float) fadeOutMs;
            alpha = peakAlpha * (1.0f - progress);
            scale = 1.0f - progress;
        }
        if (alpha <= 0.001f || scale <= 0.001f) {
            return;
        }
        // × 对角线动画: 四条射线以矩形中心为中点辐射向四角, 长度 y 随时间变化
        // (y1 = 中心到顶点的距离 = half × √2, x = 入场时长, z = 出场时长):
        //   [0, x/4]        y: y1 → 0        (先收缩到中心)
        //   [x/4, x]        y: 0  → y1       (辐射到四角, 恰在入场完成时拉满)
        //   [x, x+2z/3]     y: y1 → 0        (背景已进入出场, 射线再收缩)
        //   [x+2z/3, x+z]   y: 0             (× 已消失)
        float half = size * scale / 2f;
        float maxDiag = half * 1.41421356f;
        float diagLen;
        float quarterX = fadeInMs / 4f;
        if (elapsedMs < quarterX) {
            diagLen = maxDiag * (1.0f - elapsedMs / quarterX);
        } else if (elapsedMs < fadeInMs) {
            diagLen = maxDiag * ((elapsedMs - quarterX) / (fadeInMs - quarterX));
        } else {
            float twoThirdsZ = fadeOutMs * 2f / 3f;
            float outProgress = elapsedMs - fadeInMs;
            diagLen = outProgress < twoThirdsZ ? maxDiag * (1.0f - outProgress / twoThirdsZ) : 0.0f;
        }
        // 边框在矩形外侧(外扩, 与矩形不重叠), 由图标随后 flush 盖住 → 位于主图标之下;
        // 不调用 flush, 避免每帧打断 GuiGraphics 批量提交造成刷新率下降。
        drawBorderQuads(guiGraphics, centerX, centerY, half, color, borderWidth, alpha, diagLen);
    }

    private static void drawBorderQuads(GuiGraphics guiGraphics, float centerX, float centerY, float half, int color, float borderWidth, float alpha, float diagLen) {
        float x1 = centerX - half;
        float x2 = centerX + half;
        float y1 = centerY - half;
        float y2 = centerY + half;
        float halfThickness = borderWidth * 0.5f;
        // 边框透明度跟随状态机(与中心背景一致)
        int a = (int) (Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // 直接使用连续浮点坐标(不做像素中心吸附):
        // 吸附会让边框边缘在缩放/移动时按整像素步进跳变, 导致快速缩小时出现明显卡顿;
        // 连续浮点顶点保证边框随矩形平滑变化(亚像素级)。
        float leftOuter = x1 - halfThickness;
        float leftInner = x1 + halfThickness;
        float rightInner = x2 - halfThickness;
        float rightOuter = x2 + halfThickness;
        float topOuter = y1 - halfThickness;
        float topInner = y1 + halfThickness;
        float bottomInner = y2 - halfThickness;
        float bottomOuter = y2 + halfThickness;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // 上 / 下 / 左 / 右 四条细边(向外扩展厚度 t)
        quad(builder, matrix, leftOuter, topOuter, rightOuter, topInner, r, g, b, a);
        quad(builder, matrix, leftOuter, bottomInner, rightOuter, bottomOuter, r, g, b, a);
        quad(builder, matrix, leftOuter, topInner, leftInner, bottomInner, r, g, b, a);
        quad(builder, matrix, rightInner, topInner, rightOuter, bottomInner, r, g, b, a);
        // × 对角线动画: 以矩形中心为中点, 辐射出四条射线到四角方向, 当前长度为 diagLen
        // (射线端点沿 45° 方向, 在 x/y 轴上的投影 = diagLen / √2)
        float rayProj = diagLen * 0.70710678f;
        lineQuad(builder, matrix, centerX, centerY, centerX - rayProj, centerY - rayProj, halfThickness, r, g, b, a); // 左上
        lineQuad(builder, matrix, centerX, centerY, centerX + rayProj, centerY - rayProj, halfThickness, r, g, b, a); // 右上
        lineQuad(builder, matrix, centerX, centerY, centerX + rayProj, centerY + rayProj, halfThickness, r, g, b, a); // 左下
        lineQuad(builder, matrix, centerX, centerY, centerX - rayProj, centerY + rayProj, halfThickness, r, g, b, a); // 右下
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * 用薄 quad 模拟一条斜线(亚像素宽度), 用于绘制矩形对角线。
     * 以线段端点沿法线方向偏移半宽构成四边形, 顶点为连续浮点坐标(平滑无步进)。
     */
    private static void lineQuad(BufferBuilder builder, Matrix4f matrix,
                                 float ax, float ay, float bx, float by,
                                 float halfThickness, int r, int g, int b, int a) {
        float dx = bx - ax;
        float dy = by - ay;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) {
            return;
        }
        float nx = -dy / len * halfThickness;
        float ny = dx / len * halfThickness;
        builder.vertex(matrix, ax + nx, ay + ny, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, ax - nx, ay - ny, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, bx - nx, by - ny, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, bx + nx, by + ny, 0).color(r, g, b, a).endVertex();
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2, int r, int g, int b, int a) {
        builder.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y2, 0).color(r, g, b, a).endVertex();
    }

    /**
     * 解析 "#RRGGBB" 颜色字符串为 RGB int。
     *
     * @param hex      颜色字符串(如 "#FFFFFF"), 非法时返回 fallback
     * @param fallback 解析失败时的默认颜色
     */
    public static int parseColor(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
