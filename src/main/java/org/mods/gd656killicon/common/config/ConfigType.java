package org.mods.gd656killicon.common.config;

/**
 * 配置项值类型(声明式配置注册表用)。
 */
public enum ConfigType {
    BOOLEAN,
    INT,
    FLOAT,
    COLOR,   // "#RRGGBB" 字符串
    STRING,  // 普通文本(所见即所得)
    CHOICE   // 固定选项(FixedChoiceConfigEntry, choices 字段)
}
