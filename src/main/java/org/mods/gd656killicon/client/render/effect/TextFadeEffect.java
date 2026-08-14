package org.mods.gd656killicon.client.render.effect;

import net.minecraft.util.Mth;

/**
 * 字幕淡出动画工具(与滚动图标 {@link IconEntranceBackground} 系同一套闪动语义)。
 *
 * <p>淡出阶段的透明度曲线(alpha 0=不透明, 1=完全透明):
 * <ul>
 *   <li>未启用闪动: 线性 1 → 0;</li>
 *   <li>启用闪动时(与滚动图标 blink_fade_animation 同款公式):
 *       前 1/3 alpha 1 → 0.2(透明度升至 80%), 中 1/3 0.2 → 1(回到不透明),
 *       后 1/3 1 → 0(趋于完全透明)。</li>
 * </ul>
 * 所有字幕的淡出 alpha 均不低于 {@link #MIN_ALPHA}(0.1, 近乎透明但可见)。
 */
public final class TextFadeEffect {

    /** 字幕渐隐的最小透明度(原版 alpha): 0.1 ≈ 近乎透明, 永不小于该值。 */
    public static final float MIN_ALPHA = 0.1f;

    private TextFadeEffect() {
    }

    /**
     * 计算淡出阶段的 alpha(已夹取到 ≥ {@link #MIN_ALPHA})。
     *
     * @param fadeProgress 淡出进度 0~1
     * @param blink        是否启用闪动出场动画(滚动图标同款三段曲线)
     * @return alpha 值(0~1, 最小 {@link #MIN_ALPHA})
     */
    public static float fadeAlpha(float fadeProgress, boolean blink) {
        float alpha;
        if (blink) {
            if (fadeProgress < 1.0f / 3.0f) {
                alpha = 1.0f - 2.4f * fadeProgress;      // alpha 1 → 0.2(透明度升到 80%)
            } else if (fadeProgress < 2.0f / 3.0f) {
                alpha = 2.4f * fadeProgress - 0.6f;      // alpha 0.2 → 1(回到不透明)
            } else {
                alpha = 3.0f * (1.0f - fadeProgress);    // alpha 1 → 0(趋于完全透明)
            }
        } else {
            alpha = 1.0f - fadeProgress;                 // 原线性淡出
        }
        return Math.max(MIN_ALPHA, Mth.clamp(alpha, 0.0f, 1.0f));
    }
}
