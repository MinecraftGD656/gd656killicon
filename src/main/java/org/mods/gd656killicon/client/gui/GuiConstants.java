package org.mods.gd656killicon.client.gui;

/**
 * GUI系统的全局常量。
 */
public class GuiConstants {
    // 布局相关
    public static final int HEADER_HEIGHT = 25; // 标题栏高度
    public static final int GOLD_BAR_HEIGHT = 1; // 金色装饰条高度
    public static final int DEFAULT_PADDING = 8; // 默认内边距

    // 颜色相关
    public static final int COLOR_GOLD = 0xFFFFD700; // 金色
    public static final int COLOR_GOLD_ORANGE = 0xFFFFA500; // 金橙色 (更偏橙)
    public static final int COLOR_DARK_GOLD_ORANGE = 0xFFCC8400; // 暗金橙色 (比金橙更暗)
    public static final int COLOR_WHITE = 0xFFFFFFFF; // 白色
    public static final int COLOR_GRAY = 0xFFAAAAAA; // 灰色
    public static final int COLOR_DARK_GRAY = 0xFF666666; // 深灰色
    public static final int COLOR_BG = 0x4D444444; // 背景色（半透明黑）
    public static final int COLOR_HOVER_BORDER = 0x40808080; // 悬停时的边框颜色
    public static final int COLOR_BLACK = 0xFF444444; // 黑色
    public static final int COLOR_RED = 0xFFFF0000; // 红色
    public static final int COLOR_GREEN = 0xFF00FF00; // 绿色
    public static final int COLOR_DARK_RED = 0xFF8B0000; // 深红色
    public static final int COLOR_SKY_BLUE = 0xFF87CEEB; // 天蓝色

    // 动画相关
    public static final long INTRO_DURATION_MS = 600; // 入场动画持续时间（毫秒）
    public static final long SLICE_DURATION_MS = 800; // 切片动画持续时间（毫秒）
    public static final long BUTTON_ANIM_DURATION_MS = 200; // 按钮悬停动画持续时间（毫秒）
    public static final int TAB_DELAY_MS = 100; // 标签之间动画延迟（毫秒）
    public static final float ANIMATION_SPEED = 10.0f; // 动画平滑速度
    public static final float TITLE_ANIM_OFFSET = 100.0f; // 标题动画初始偏移距离

    // 滚动与物理相关
    public static final double SCROLL_AMOUNT = 40.0; // 鼠标滚轮单次滚动量
    public static final double SCROLL_SMOOTHING = 15.0; // 滚动平滑系数
    public static final int TAB_SPACING = 2; // 标签之间的间距
    public static final int TAB_Y_OFFSET = 2; // 标签在Y轴上的偏移
    public static final int SPLIT_POINT_OFFSET = 25; // 分割点（标题与标签之间）的额外偏移
    public static final int HEADER_CLICK_ZONE = 5; // 标题栏点击判定的额外区域高度
    public static final int HEADER_SCROLL_ZONE = 20; // 标题栏触发滚动的判定区域高度

    // 区域布局相关
    public static final int REGION_4_HEIGHT = 35; // 区域4（底部控制区）的高度
    public static final int AREA1_BOTTOM_OFFSET = 40; // 区域1（大标题区）的底部偏移
    public static final int ROW_HEADER_HEIGHT = 17; // 表头高度
    public static final int FLEX_COLUMN_MIN_WIDTH = 60; // 自由压缩列的最小宽度
}
