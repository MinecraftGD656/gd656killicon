package org.mods.gd656killicon.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class BonusType {
    public static final int DAMAGE = 0; // 造成伤害
    public static final int KILL = 1; // 击杀
    public static final int EXPLOSION = 2; // 爆炸伤害
    public static final int HEADSHOT = 3; // 爆头伤害
    public static final int CRIT = 4; // 暴击伤害
    public static final int KILL_EXPLOSION = 5; // 爆炸击杀
    public static final int KILL_HEADSHOT = 6; // 爆头击杀
    public static final int KILL_CRIT = 7; // 暴击击杀
    public static final int KILL_COMBO = 8; // 连击击杀
    public static final int KILL_LONG_DISTANCE = 9; // 远距离击杀
    public static final int KILL_INVISIBLE = 10; // 视野被遮蔽时击杀
    public static final int ASSIST = 11; // 助攻
    public static final int DESPERATE_COUNTERATTACK = 12; //  绝境反击
    public static final int AVENGE = 13; // 复仇
    public static final int SHOCKWAVE = 14; // 冲击波
    public static final int BLIND_KILL = 15; // 盲杀
    public static final int BUFF_KILL = 16; // 增益击杀
    public static final int DEBUFF_KILL = 17; // 减益击杀
    public static final int BOTH_BUFF_DEBUFF_KILL = 18; // 同时增益减益击杀
    public static final int LAST_BULLET_KILL = 19; // 最后一枪击杀
    public static final int ONE_BULLET_MULTI_KILL = 20; // 一箭x雕
    public static final int EFFORTLESS_KILL = 21; // 游刃有余
    public static final int BACKSTAB_KILL = 22; // 背袭
    public static final int BACKSTAB_MELEE_KILL = 23; // 背刺
    public static final int BRAVE_RETURN = 24; // 三次死亡后击杀
    public static final int JUSTICE_FROM_ABOVE = 25; // 天降正义
    public static final int ABSOLUTE_AIR_CONTROL = 26; // 绝对空中控制
    public static final int BERSERKER = 27; // 狂战士
    public static final int INTERRUPTED_STREAK = 28; // 打断连击
    public static final int LEAVE_IT_TO_ME = 29; // 交给我
    public static final int SAVIOR = 30; // 救星
    public static final int SLAY_THE_LEADER = 31; // 斩落榜首
    public static final int PURGE = 32; // 肃清
    public static final int QUICK_SWITCH = 33; // 切枪制人
    public static final int SEIZE_OPPORTUNITY = 34; // 机不可失
    public static final int BLOODTHIRSTY = 35; // 嗜血
    public static final int MERCILESS = 36; // 无情
    public static final int VALIANT = 37; // 勇猛
    public static final int FIERCE = 38; // 凶狠
    public static final int SAVAGE = 39; // 野蛮
    public static final int POTATO_AIM = 40; // 马枪怪
    public static final int HIT_VEHICLE_ARMOR = 41; // 命中装甲
    public static final int DESTROY_VEHICLE = 42; // 摧毁装甲
    public static final int VEHICLE_REPAIR = 43; // 载具修理
    public static final int VALUE_TARGET_DESTROYED = 44; // 价值目标摧毁

    private static final Map<String, Integer> NAME_TO_TYPE = new HashMap<>();
    private static final Map<Integer, String> TYPE_TO_NAME = new HashMap<>();

    static {
        register("DAMAGE", DAMAGE);
        register("KILL", KILL);
        register("EXPLOSION", EXPLOSION);
        register("HEADSHOT", HEADSHOT);
        register("CRIT", CRIT);
        register("KILL_EXPLOSION", KILL_EXPLOSION);
        register("KILL_HEADSHOT", KILL_HEADSHOT);
        register("KILL_CRIT", KILL_CRIT);
        register("KILL_COMBO", KILL_COMBO);
        register("KILL_LONG_DISTANCE", KILL_LONG_DISTANCE);
        register("KILL_INVISIBLE", KILL_INVISIBLE);
        register("ASSIST", ASSIST);
        register("DESPERATE_COUNTERATTACK", DESPERATE_COUNTERATTACK);
        register("AVENGE", AVENGE);
        register("SHOCKWAVE", SHOCKWAVE);
        register("BLIND_KILL", BLIND_KILL);
        register("BUFF_KILL", BUFF_KILL);
        register("DEBUFF_KILL", DEBUFF_KILL);
        register("BOTH_BUFF_DEBUFF_KILL", BOTH_BUFF_DEBUFF_KILL);
        register("LAST_BULLET_KILL", LAST_BULLET_KILL);
        register("ONE_BULLET_MULTI_KILL", ONE_BULLET_MULTI_KILL);
        register("EFFORTLESS_KILL", EFFORTLESS_KILL);
        register("BACKSTAB_KILL", BACKSTAB_KILL);
        register("BACKSTAB_MELEE_KILL", BACKSTAB_MELEE_KILL);
        register("BRAVE_RETURN", BRAVE_RETURN);
        register("JUSTICE_FROM_ABOVE", JUSTICE_FROM_ABOVE);
        register("ABSOLUTE_AIR_CONTROL", ABSOLUTE_AIR_CONTROL);
        register("BERSERKER", BERSERKER);
        register("INTERRUPTED_STREAK", INTERRUPTED_STREAK);
        register("LEAVE_IT_TO_ME", LEAVE_IT_TO_ME);
        register("SAVIOR", SAVIOR);
        register("SLAY_THE_LEADER", SLAY_THE_LEADER);
        register("PURGE", PURGE);
        register("QUICK_SWITCH", QUICK_SWITCH);
        register("SEIZE_OPPORTUNITY", SEIZE_OPPORTUNITY);
        register("BLOODTHIRSTY", BLOODTHIRSTY);
        register("MERCILESS", MERCILESS);
        register("VALIANT", VALIANT);
        register("FIERCE", FIERCE);
        register("SAVAGE", SAVAGE);
        register("POTATO_AIM", POTATO_AIM);
        register("HIT_VEHICLE_ARMOR", HIT_VEHICLE_ARMOR);
        register("DESTROY_VEHICLE", DESTROY_VEHICLE);
        register("VEHICLE_REPAIR", VEHICLE_REPAIR);
        register("VALUE_TARGET_DESTROYED", VALUE_TARGET_DESTROYED);
    }

    private static void register(String name, int type) {
        NAME_TO_TYPE.put(name.toUpperCase(), type);
        TYPE_TO_NAME.put(type, name.toUpperCase());
    }

    public static int getTypeByName(String name) {
        return NAME_TO_TYPE.getOrDefault(name.toUpperCase(), -1);
    }

    public static String getNameByType(int type) {
        return TYPE_TO_NAME.getOrDefault(type, "UNKNOWN");
    }

    public static Set<String> getAllNames() {
        return NAME_TO_TYPE.keySet();
    }

    private BonusType() {}
}
