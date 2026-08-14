package org.mods.gd656killicon.common;

import org.mods.gd656killicon.common.bonus.BonusDefinition;
import org.mods.gd656killicon.common.bonus.BonusRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 加分项类型编号（常量保留，兼容全部编译期引用）。
 *
 * <p>编号与名称映射由 {@link BonusRegistry} 驱动（注册表是唯一数据源），
 * 本类的常量仅供编译期引用与序列化编号。</p>
 */
public final class BonusType {
    public static final int DAMAGE = 0;
    public static final int KILL = 1;
    public static final int EXPLOSION = 2;
    public static final int HEADSHOT = 3;
    public static final int CRIT = 4;
    public static final int KILL_EXPLOSION = 5;
    public static final int KILL_HEADSHOT = 6;
    public static final int KILL_CRIT = 7;
    public static final int KILL_COMBO = 8;
    public static final int KILL_LONG_DISTANCE = 9;
    public static final int KILL_INVISIBLE = 10;
    public static final int ASSIST = 11;
    public static final int DESPERATE_COUNTERATTACK = 12;
    public static final int AVENGE = 13;
    public static final int SHOCKWAVE = 14;
    public static final int BLIND_KILL = 15;
    public static final int BUFF_KILL = 16;
    public static final int DEBUFF_KILL = 17;
    public static final int BOTH_BUFF_DEBUFF_KILL = 18;
    public static final int LAST_BULLET_KILL = 19;
    public static final int ONE_BULLET_MULTI_KILL = 20;
    public static final int EFFORTLESS_KILL = 21;
    public static final int BACKSTAB_KILL = 22;
    public static final int BACKSTAB_MELEE_KILL = 23;
    public static final int BRAVE_RETURN = 24;
    public static final int JUSTICE_FROM_ABOVE = 25;
    public static final int ABSOLUTE_AIR_CONTROL = 26;
    public static final int BERSERKER = 27;
    public static final int INTERRUPTED_STREAK = 28;
    public static final int LEAVE_IT_TO_ME = 29;
    public static final int SAVIOR = 30;
    public static final int SLAY_THE_LEADER = 31;
    public static final int PURGE = 32;
    public static final int QUICK_SWITCH = 33;
    public static final int SEIZE_OPPORTUNITY = 34;
    public static final int BLOODTHIRSTY = 35;
    public static final int MERCILESS = 36;
    public static final int VALIANT = 37;
    public static final int FIERCE = 38;
    public static final int SAVAGE = 39;
    public static final int POTATO_AIM = 40;
    public static final int HIT_VEHICLE_ARMOR = 41;
    public static final int DESTROY_VEHICLE = 42;
    public static final int VEHICLE_REPAIR = 43;
    public static final int VALUE_TARGET_DESTROYED = 44;
    public static final int LOCKED_TARGET = 45;
    public static final int HOLD_POSITION = 46;
    public static final int CHARGE_ASSAULT = 47;
    public static final int FIRE_SUPPRESSION = 48;
    public static final int DESTROY_BLOCK = 49;
    public static final int SPOTTING = 50;
    public static final int SPOTTING_KILL = 51;
    public static final int SPOTTING_TEAM_ASSIST = 52;
    public static final int CONQUEST_CAPTURE_PROGRESS = 53;
    public static final int CONQUEST_CAPTURE_NEUTRALIZE = 54;
    public static final int CONQUEST_CAPTURE_CONTROL = 55;
    public static final int VEHICLE_DESTROY_ASSIST = 66;

    private static final Map<String, Integer> NAME_TO_TYPE = new HashMap<>();
    private static final Map<Integer, String> TYPE_TO_NAME = new HashMap<>();

    static {
        for (BonusDefinition def : BonusRegistry.getAll()) {
            register(def.id(), def.type());
        }
    }

    private static void register(String name, int type) {
        NAME_TO_TYPE.put(name, type);
        TYPE_TO_NAME.put(type, name);
    }

    public static int getTypeByName(String name) {
        return NAME_TO_TYPE.getOrDefault(name, -1);
    }

    public static String getNameByType(int type) {
        return TYPE_TO_NAME.getOrDefault(type, "UNKNOWN");
    }

    public static Set<String> getAllNames() {
        return NAME_TO_TYPE.keySet();
    }

    private BonusType() {}
}
