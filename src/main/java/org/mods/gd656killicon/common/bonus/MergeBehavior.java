package org.mods.gd656killicon.common.bonus;

/** 同类型多条加分合并进同一条目时的分组行为。 */
public enum MergeBehavior {
    /** 按 type + extraData 分组（默认）。 */
    BY_TYPE_EXTRA,
    /** 所有同 type 合并为一条（KILL_COMBO 现状行为）。 */
    BY_COMBO
}
