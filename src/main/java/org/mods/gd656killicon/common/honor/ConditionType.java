package org.mods.gd656killicon.common.honor;

/**
 * 荣誉条件类型(判定器分型)。
 * <p>
 * 每个荣誉通过 {@link HonorDefinition#conditionType()} 声明其判定方式,
 * 判定逻辑由 {@link org.mods.gd656killicon.server.logic.honor.HonorJudges}
 * 按类型统一分派, 新荣誉只需声明参数, 无需编写新判定类。
 * </p>
 */
public enum ConditionType {
    /** 存活段内满足条件的击杀计数达到阈值(如猎头者: 单次存活内爆头击杀 ≥ 3)。参数格式 "&lt;谓词&gt;:&lt;阈值&gt;"。 */
    KILL_STREAK,

    /** 单事件谓词(如弹道: 击毁空中载具 且 未搭乘空中载具)。参数格式 "&lt;谓词键&gt;"。 */
    CONDITIONAL,

    /** 存活段内事件数值累计达到阈值(预留: 修复量/伤害累计等非击杀类, 暂未实现)。参数格式 "&lt;累计键&gt;:&lt;阈值&gt;"。 */
    ACCUMULATE
}
