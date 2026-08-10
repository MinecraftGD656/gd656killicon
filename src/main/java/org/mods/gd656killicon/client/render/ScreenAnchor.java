package org.mods.gd656killicon.client.render;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 屏幕锚点系统。
 * <p>
 * 每个元素通过 screen_anchor 选择元素在屏幕上的参考点(9 选 1)，
 * x_offset / y_offset 表示元素相对于该参考点的偏移：
 * <ul>
 *   <li>centerX = 参考点X + x_offset(x_offset 向右为正)</li>
 *   <li>centerY = 参考点Y - y_offset(y_offset 向上为正)</li>
 * </ul>
 * 默认锚点为 bottom_center(底边框中心)，即旧版全部元素的行为
 * (centerX = screenWidth/2 + x_offset, centerY = screenHeight - y_offset)。
 */
public final class ScreenAnchor {

    /** 默认锚点: 底边框中心(旧行为) */
    public static final String DEFAULT = "bottom_center";

    /** 锚点 id → 屏幕归一化坐标(0.0=左/上, 0.5=中, 1.0=右/下) */
    private static final Map<String, float[]> ANCHORS = Map.of(
            "top_left",      new float[] {0.0f, 0.0f},
            "middle_left",   new float[] {0.0f, 0.5f},
            "bottom_left",   new float[] {0.0f, 1.0f},
            "top_center",    new float[] {0.5f, 0.0f},
            "center",        new float[] {0.5f, 0.5f},
            "bottom_center", new float[] {0.5f, 1.0f},
            "top_right",     new float[] {1.0f, 0.0f},
            "middle_right",  new float[] {1.0f, 0.5f},
            "bottom_right",  new float[] {1.0f, 1.0f}
    );

    public static final List<String> ANCHOR_IDS = List.of(
            "top_left", "middle_left", "bottom_left",
            "top_center", "center", "bottom_center",
            "top_right", "middle_right", "bottom_right"
    );

    private ScreenAnchor() {
    }

    public static boolean isValid(String anchor) {
        return anchor != null && ANCHORS.containsKey(anchor);
    }

    /** 参考点 X(0=左, screenWidth/2=中, screenWidth=右) */
    public static float resolveAnchorX(String anchor, int screenWidth) {
        float[] a = ANCHORS.get(isValid(anchor) ? anchor : DEFAULT);
        return a[0] * screenWidth;
    }

    /** 参考点 Y(0=上, screenHeight/2=中, screenHeight=下) */
    public static float resolveAnchorY(String anchor, int screenHeight) {
        float[] a = ANCHORS.get(isValid(anchor) ? anchor : DEFAULT);
        return a[1] * screenHeight;
    }

    /** 元素中心 X = 参考点X + x_offset */
    public static int resolveCenterX(String anchor, int xOffset, int screenWidth) {
        return Math.round(resolveAnchorX(anchor, screenWidth) + xOffset);
    }

    /** 元素中心 Y = 参考点Y - y_offset(y_offset 向上为正) */
    public static int resolveCenterY(String anchor, int yOffset, int screenHeight) {
        return Math.round(resolveAnchorY(anchor, screenHeight) - yOffset);
    }

    /**
     * 锚点切换时计算新 x_offset，使元素在屏幕上的位置保持不变：
     * x_new = x_old + (参考点X_旧 - 参考点X_新)
     */
    public static int translateXOffset(String fromAnchor, String toAnchor, int xOffset, int screenWidth) {
        float refXFrom = resolveAnchorX(fromAnchor, screenWidth);
        float refXTo = resolveAnchorX(toAnchor, screenWidth);
        return Math.round(xOffset + (refXFrom - refXTo));
    }

    /**
     * 锚点切换时计算新 y_offset，使元素在屏幕上的位置保持不变：
     * y_new = y_old + (参考点Y_新 - 参考点Y_旧)
     */
    public static int translateYOffset(String fromAnchor, String toAnchor, int yOffset, int screenHeight) {
        float refYFrom = resolveAnchorY(fromAnchor, screenHeight);
        float refYTo = resolveAnchorY(toAnchor, screenHeight);
        return Math.round(yOffset + (refYTo - refYFrom));
    }

    public static String resolveAnchorOrDefault(String anchor) {
        return isValid(anchor) ? anchor : DEFAULT;
    }

    public static List<String> getAnchorIds() {
        return ANCHOR_IDS;
    }

    // 供调试/测试: 全部锚点 id
    public static String[] anchorIdsArray() {
        return Arrays.copyOf(ANCHOR_IDS.toArray(new String[0]), ANCHOR_IDS.size());
    }
}
