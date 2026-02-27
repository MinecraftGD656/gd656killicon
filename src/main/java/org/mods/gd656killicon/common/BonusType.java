package org.mods.gd656killicon.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        register("LOCKED_TARGET", LOCKED_TARGET);
        register("HOLD_POSITION", HOLD_POSITION);
        register("CHARGE_ASSAULT", CHARGE_ASSAULT);
        register("FIRE_SUPPRESSION", FIRE_SUPPRESSION);
        register("DESTROY_BLOCK", DESTROY_BLOCK);
        register("SPOTTING", SPOTTING);
        register("SPOTTING_KILL", SPOTTING_KILL);
        register("SPOTTING_TEAM_ASSIST", SPOTTING_TEAM_ASSIST);
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
