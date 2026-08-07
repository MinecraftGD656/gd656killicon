package org.mods.gd656killicon.common.bonus;

/** 客户端统计归类（记录到 ClientStatsManager）。 */
public enum StatCategory {
    /** 不记录统计。 */
    NONE,
    ASSIST,
    REVIVE,
    /** 伤害类（计入累计伤害）。 */
    DAMAGE
}
