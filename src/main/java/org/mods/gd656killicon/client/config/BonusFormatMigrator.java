package org.mods.gd656killicon.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * 加分项系统大清洗升级器（一次性，v1 → v2）。
 *
 * <p>处理历史遗留：
 * 1. 6 个 format 键名与 ID 不一致（统一为 format_&lt;id&gt;）→ 旧键值搬入新键；
 * 2. 玩家配置中存着旧 lang key 的默认值 → 删除该键（视为未自定义，回落到注册表当前语言默认）。
 *
 * <p>孤儿 format_* 键由 normalizePresets 自动清理（不在默认集合即删），此处不处理。
 * 必须在 normalizePresets 之前执行，否则旧键会被先删、值丢失。</p>
 *
 * <p>非运行时代码：升级时执行一次（版本标记控制），不进入渲染/计分热路径。</p>
 */
public final class BonusFormatMigrator {
    public static final int TARGET_VERSION = 3;

    /** 旧键名 → 新键名（唯一规则 format_&lt;id&gt;）。 */
    private static final Map<String, String> LEGACY_KEY_MOVES = Map.ofEntries(
        Map.entry("format_backstab", "format_backstab_kill"),
        Map.entry("format_backstab_melee", "format_backstab_melee_kill"),
        Map.entry("format_crit_damage", "format_crit"),
        Map.entry("format_explosion_damage", "format_explosion"),
        Map.entry("format_headshot_damage", "format_headshot"),
        Map.entry("format_combo", "format_kill_combo")
    );

    /** 旧版默认值（lang key），命中即视为"未自定义"，迁移时删除该键。 */
    private static final Set<String> LEGACY_LANG_KEYS = Set.of(
        "gd656killicon.client.format.bonus_absolute_air_control",
        "gd656killicon.client.format.bonus_ammo_supply",
        "gd656killicon.client.format.bonus_assist",
        "gd656killicon.client.format.bonus_avenge",
        "gd656killicon.client.format.bonus_backstab",
        "gd656killicon.client.format.bonus_backstab_melee",
        "gd656killicon.client.format.bonus_berserker",
        "gd656killicon.client.format.bonus_blind_kill",
        "gd656killicon.client.format.bonus_bloodthirsty",
        "gd656killicon.client.format.bonus_both_buff_debuff_kill",
        "gd656killicon.client.format.bonus_brave_return",
        "gd656killicon.client.format.bonus_buff_kill",
        "gd656killicon.client.format.bonus_charge_assault",
        "gd656killicon.client.format.bonus_combo",
        "gd656killicon.client.format.bonus_conquest_capture_control",
        "gd656killicon.client.format.bonus_conquest_capture_neutralize",
        "gd656killicon.client.format.bonus_conquest_capture_progress",
        "gd656killicon.client.format.bonus_crit",
        "gd656killicon.client.format.bonus_damage",
        "gd656killicon.client.format.bonus_debuff_kill",
        "gd656killicon.client.format.bonus_desperate_counterattack",
        "gd656killicon.client.format.bonus_destroy_block",
        "gd656killicon.client.format.bonus_destroy_vehicle",
        "gd656killicon.client.format.bonus_effortless_kill",
        "gd656killicon.client.format.bonus_emergency_reinforcement",
        "gd656killicon.client.format.bonus_explosion",
        "gd656killicon.client.format.bonus_fierce",
        "gd656killicon.client.format.bonus_fire_suppression",
        "gd656killicon.client.format.bonus_friendly_deploy_on_your_vehicle",
        "gd656killicon.client.format.bonus_ground_sensor_scan",
        "gd656killicon.client.format.bonus_headshot",
        "gd656killicon.client.format.bonus_healing",
        "gd656killicon.client.format.bonus_hit_vehicle_armor",
        "gd656killicon.client.format.bonus_hold_position",
        "gd656killicon.client.format.bonus_interrupted_streak",
        "gd656killicon.client.format.bonus_justice_from_above",
        "gd656killicon.client.format.bonus_kill",
        "gd656killicon.client.format.bonus_kill_crit",
        "gd656killicon.client.format.bonus_kill_explosion",
        "gd656killicon.client.format.bonus_kill_headshot",
        "gd656killicon.client.format.bonus_kill_invisible",
        "gd656killicon.client.format.bonus_kill_long_distance",
        "gd656killicon.client.format.bonus_last_bullet_kill",
        "gd656killicon.client.format.bonus_leave_it_to_me",
        "gd656killicon.client.format.bonus_locked_target",
        "gd656killicon.client.format.bonus_merciless",
        "gd656killicon.client.format.bonus_one_bullet_multi_kill",
        "gd656killicon.client.format.bonus_potato_aim",
        "gd656killicon.client.format.bonus_purge",
        "gd656killicon.client.format.bonus_quick_switch",
        "gd656killicon.client.format.bonus_revive",
        "gd656killicon.client.format.bonus_rush_bomb_defused",
        "gd656killicon.client.format.bonus_rush_bomb_planted",
        "gd656killicon.client.format.bonus_rush_objective_destroyed",
        "gd656killicon.client.format.bonus_savage",
        "gd656killicon.client.format.bonus_savior",
        "gd656killicon.client.format.bonus_seize_opportunity",
        "gd656killicon.client.format.bonus_shockwave",
        "gd656killicon.client.format.bonus_slay_the_leader",
        "gd656killicon.client.format.bonus_spotting",
        "gd656killicon.client.format.bonus_spotting_kill",
        "gd656killicon.client.format.bonus_spotting_team_assist",
        "gd656killicon.client.format.bonus_squad_beacon_deploy",
        "gd656killicon.client.format.bonus_squad_deploy_on_you",
        "gd656killicon.client.format.bonus_squad_last_member_kill",
        "gd656killicon.client.format.bonus_squad_wipe_completion",
        "gd656killicon.client.format.bonus_tactical_gadget_destroyed",
        "gd656killicon.client.format.bonus_valiant",
        "gd656killicon.client.format.bonus_value_objective_support_beacon_deploy",
        "gd656killicon.client.format.bonus_value_target_destroyed",
        "gd656killicon.client.format.bonus_vehicle_destroy_assist",
        "gd656killicon.client.format.bonus_vehicle_repair"
    );

    private BonusFormatMigrator() {
    }

    /** 迁移全部预设的 subtitle/bonus_list 元素。返回是否有改动。 */
    public static boolean migrate() {
        boolean changed = false;
        for (ElementConfigManager.ElementPreset preset : ElementConfigManager.getActivePresets().values()) {
            JsonObject bonusList = preset.getConfig("subtitle/bonus_list");
            if (bonusList == null) {
                continue;
            }
            changed |= migrateBonusList(bonusList);
        }
        if (changed) {
            ElementConfigManager.saveConfig();
        }
        return changed;
    }

    private static boolean migrateBonusList(JsonObject config) {
        boolean changed = false;
        // 1) 旧键搬家：值搬入新键（新键已有值则不覆盖），删除旧键
        for (Map.Entry<String, String> move : LEGACY_KEY_MOVES.entrySet()) {
            if (config.has(move.getKey())) {
                JsonElement value = config.remove(move.getKey());
                if (!config.has(move.getValue())) {
                    config.add(move.getValue(), value);
                }
                changed = true;
            }
        }
        // 2) 旧 key 值清理：命中旧 lang key 或中间版注册表 key 前缀 → 视为未自定义，删键回落默认
        for (String key : new ArrayList<>(config.keySet())) {
            JsonElement element = config.get(key);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String value = element.getAsString();
                if (LEGACY_LANG_KEYS.contains(value) || value.startsWith("gd656killicon.bonus_format.")) {
                    config.remove(key);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
