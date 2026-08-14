package org.mods.gd656killicon.common.bonus;

/** 计算因子类型（声明性元数据）：最终加分 = 因子 × 表达式。 */
public enum FactorType {
    /** 固定加分（因子恒为 1）。 */
    NONE,
    /** 因子 = 被击杀者最大生命值。 */
    VICTIM_MAX_HEALTH,
    /** 因子 = 伤害量。 */
    DAMAGE,
    /** 因子 = 距离。 */
    DISTANCE,
    /** 因子 = 连杀数。 */
    COMBO,
    /** 因子 = 连续击杀数（streak）。 */
    STREAK,
    /** 因子 = 数量（如一枪多杀）。 */
    COUNT,
    /** 因子 = 载具价值/累计伤害。 */
    SCORE
}
