package org.mods.gd656killicon.client.render.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * 字幕入场背景渲染器(score / kill_feed 元素 entrance_background 配置项启用时生效)。
 *
 * <p>在数字字幕之下渲染一个长方形背景(位于字幕文本之下), 由真实分数刷新触发:
 * <ul>
 *   <li>长方形高度 = 字幕文本高度 + 2 像素;y 轴始终不变, 中线与字幕文本中线同一条直线;</li>
 *   <li>左边框始终与字幕左边缘相距 2 像素(宽度变化时左边框不动, 仅右边框向右扩展);</li>
 *   <li>时间线(t0 = 分数刷新触发时刻):
 *       <ul>
 *         <li>[0, fadeIn]: 透明度 100% → 配置值(alpha 0 → 峰值), 宽度固定 2 像素;</li>
 *         <li>[fadeIn, fadeIn+sweep]: 透明度保持峰值, 宽度从 2 像素平滑拉长到 字幕宽度+4 像素
 *             (左边框不动, 右边框超出字幕右边缘 2 像素, 缓出平滑);</li>
 *         <li>[fadeIn+sweep, fadeIn+sweep+fadeOut]: 宽度保持, 长方形渐隐(alpha 峰值 → 0)。</li>
 *       </ul>
 *   </li>
 * </ul>
 * 全部坐标/尺寸为屏幕像素(不受字幕 pose 缩放影响)。
 */
public final class SubtitleEntranceBackground {

    /** 初始宽度(像素) */
    private static final float INITIAL_WIDTH = 2.0f;
    /** 与字幕边缘的间距(像素) */
    private static final float EDGE_GAP = 2.0f;

    private SubtitleEntranceBackground() {
    }

    /**
     * 解析 "#RRGGBB" 颜色字符串为 RGB int(非法时返回 fallback)。
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

    /**
     * 绘制入场背景(应在字幕文本绘制之前调用, 位于文本之下)。
     *
     * @param guiGraphics       渲染上下文(屏幕坐标)
     * @param elapsedMs         自触发以来的毫秒数
     * @param textLeft          字幕文本左边缘屏幕 x(缩放后)
     * @param textRight         字幕文本右边缘屏幕 x(缩放后)
     * @param midY              字幕文本中线屏幕 y(恒定)
     * @param textHeight        字幕文本高度(缩放后)
     * @param fadeInMs          入场(淡入)持续时间毫秒
     * @param sweepMs           宽度扫动总时长毫秒
     * @param fadeOutMs         出场(渐隐)持续时间毫秒
     * @param peakTransparency  淡入完成时的透明度(0=不透明, 1=完全透明; 渲染 alpha 峰值 = 1 - 该值)
     * @param rgb               背景 RGB 颜色(不含 alpha)
     * @param mirror            为 true 时方向完全镜像: 右边框固定(距字幕右边缘 2 像素),
     *                          宽度从右侧向左侧扫动(右对齐字幕使用); false 为左边缘固定向右扫。
     */
    public static void draw(GuiGraphics guiGraphics, long elapsedMs,
                            float textLeft, float textRight,
                            float midY, float textHeight,
                            long fadeInMs, long sweepMs, long fadeOutMs,
                            float peakTransparency, int rgb, boolean mirror) {
        if (elapsedMs < 0 || fadeInMs <= 0 || sweepMs <= 0 || fadeOutMs <= 0
                || elapsedMs >= fadeInMs + sweepMs + fadeOutMs) {
            return;
        }
        float maxWidth = (textRight - textLeft) + EDGE_GAP * 2.0f;
        float peakAlpha = Mth.clamp(1.0f - peakTransparency, 0.0f, 1.0f);
        float alpha;
        float width;
        if (elapsedMs < fadeInMs) {
            // 阶段一: 透明度 100% → 配置值(alpha 0 → 峰值), 宽度固定 2 像素
            alpha = peakAlpha * (elapsedMs / (float) fadeInMs);
            width = INITIAL_WIDTH;
        } else if (elapsedMs < fadeInMs + sweepMs) {
            // 阶段二: 透明度保持峰值, 宽度平滑拉长(缓出)到 字幕宽度+4 像素
            float t = (elapsedMs - fadeInMs) / (float) sweepMs;
            float eased = 1.0f - (float) Math.pow(1.0f - Mth.clamp(t, 0.0f, 1.0f), 3);
            alpha = peakAlpha;
            width = Mth.lerp(eased, INITIAL_WIDTH, maxWidth);
        } else {
            // 阶段三: 宽度保持, 长方形渐隐(alpha 峰值 → 0)
            float p = (elapsedMs - fadeInMs - sweepMs) / (float) fadeOutMs;
            alpha = peakAlpha * (1.0f - Mth.clamp(p, 0.0f, 1.0f));
            width = maxWidth;
        }
        if (alpha <= 0.001f || width <= 0.001f) {
            return;
        }
        // 默认: 左边框固定(距字幕左边缘 2 像素), 仅右边框向右扩展;
        // 镜像: 右边框固定(距字幕右边缘 2 像素), 仅左边框向左扩展(右对齐字幕)
        float left;
        float right;
        if (mirror) {
            right = textRight + EDGE_GAP;
            left = right - width;
        } else {
            left = textLeft - EDGE_GAP;
            right = left + width;
        }
        float top = midY - (textHeight + EDGE_GAP) / 2.0f;
        float bottom = midY + (textHeight + EDGE_GAP) / 2.0f;

        int argb = (rgb & 0xFFFFFF) | ((int) (Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f) << 24);
        int l = Math.round(left);
        int r = Math.round(right);
        int t = Math.round(top);
        int b = Math.round(bottom);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(l, t, r, b, argb);
        RenderSystem.disableBlend();
    }
}
