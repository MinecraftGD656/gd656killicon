package org.mods.gd656killicon.common.config;

/**
 * 配置项一级分类(元素配置界面的 general 折叠分组)。
 * TEXTURE 为特殊分类: 键属于纹理 tab(anim_*\texture_*\custom_texture_*\texture_mode_*\vanilla_texture_*),
 * 不参与 general 内的折叠分组, 由纹理 tab 维度展示。
 */
public enum Category {
    VISIBILITY,  // 可见性
    CONTENT,     // 文本内容
    POSITION,    // 位置
    COLOR,       // 颜色
    EFFECT,      // 效果
    TIMING,      // 时序
    BEHAVIOR,    // 规则逻辑
    TEXTURE      // 纹理(特殊: 纹理 tab)
}
