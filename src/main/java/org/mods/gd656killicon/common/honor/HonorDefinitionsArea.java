package org.mods.gd656killicon.common.honor;

import org.mods.gd656killicon.common.honor.HonorRegistry;

/**
 * 荣誉定义声明区(仿 BonusDefinitionsArea)。
 * <p>
 * 所有荣誉在此集中声明, 每条为一个 {@link HonorDefinition}:
 * <pre>
 * HonorRegistry.register(new HonorDefinition(
 *     "headhunter",                        // 荣誉 ID(纹理 honor_headhunter.png)
 *     ConditionType.KILL_STREAK,           // 条件类型
 *     "headshot:3"                         // 判定参数(存活段内爆头击杀 ≥ 3)
 * ));
 * </pre>
 * 判定逻辑无需新增, 分型判定器(server/logic/honor/HonorJudges)按条件类型自动处理。
 * </p>
 */
public final class HonorDefinitionsArea {

    private HonorDefinitionsArea() {
    }

    public static void register() {
        // 实例荣誉在此声明。
        HonorRegistry.register(new HonorDefinition(
                "headhunter",
                org.mods.gd656killicon.common.honor.ConditionType.KILL_STREAK,
                "headshot:3",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "warrior",
                org.mods.gd656killicon.common.honor.ConditionType.KILL_STREAK,
                "moving:3",
                50
        ));
        HonorRegistry.register(new HonorDefinition(
                "avenge",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "avenge",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "bomb_technician",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "rush_bomb_defused",
                50
        ));
        HonorRegistry.register(new HonorDefinition(
                "combat_medic",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "round_revive_20",
                8
        ));
        HonorRegistry.register(new HonorDefinition(
                "detonator",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "rush_objective_destroyed",
                35
        ));
        HonorRegistry.register(new HonorDefinition(
                "raider",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "kill_within_8s_after_combo4",
                150
        ));
        HonorRegistry.register(new HonorDefinition(
                "scout",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "spot_20",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "arsenal",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "arsenal_in_30s_3_items",
                5
        ));
        HonorRegistry.register(new HonorDefinition(
                "artillery",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "vehicle_damage_800",
                8
        ));
        HonorRegistry.register(new HonorDefinition(
                "avenger",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "leave_it_to_me",
                65
        ));
        HonorRegistry.register(new HonorDefinition(
                "ballistics",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "destroy_vehicle_outside",
                20
        ));
        HonorRegistry.register(new HonorDefinition(
                "ace_pilot",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "destroy_air_vehicle_in_air",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "bail_rocket",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "bail_out_destroy_air_return",
                1
        ));
        HonorRegistry.register(new HonorDefinition(
                "breaker",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "conquest_stage_breaker",
                60
        ));
        HonorRegistry.register(new HonorDefinition(
                "top",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "victim_top_scorer",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "medic",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "revive_3_consecutive",
                60
        ));
        HonorRegistry.register(new HonorDefinition(
                "mechanic",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "repair_vehicle_25",
                20
        ));
        HonorRegistry.register(new HonorDefinition(
                "minelayer",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "mine_destroy_vehicle",
                8
        ));
        HonorRegistry.register(new HonorDefinition(
                "fire_support",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "fire_suppression_5",
                45
        ));
        HonorRegistry.register(new HonorDefinition(
                "field_medic",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "revive_5_in_life",
                40
        ));
        HonorRegistry.register(new HonorDefinition(
                "flight_dispatcher",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "victim_riding_air",
                25
        ));
        HonorRegistry.register(new HonorDefinition(
                "pathfinder",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "squad_deploy_3",
                35
        ));
        HonorRegistry.register(new HonorDefinition(
                "demolition",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "destroy_7_vehicles",
                5
        ));
        HonorRegistry.register(new HonorDefinition(
                "destroyer",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "tactical_gadget_3",
                25
        ));
        HonorRegistry.register(new HonorDefinition(
                "executioner",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "backstab",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "flank",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "kill_blinded",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "frenzy",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "buff_kill",
                50
        ));
        HonorRegistry.register(new HonorDefinition(
                "garrison",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "garrison_kill_3",
                50
        ));
        HonorRegistry.register(new HonorDefinition(
                "ground_control",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "conquest_stage_ground_control",
                25
        ));
        HonorRegistry.register(new HonorDefinition(
                "guardian",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "leave_it_to_me",
                50
        ));
        HonorRegistry.register(new HonorDefinition(
                "gunner",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "gunner_kill_5",
                35
        ));
        HonorRegistry.register(new HonorDefinition(
                "quickdraw",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "quickdraw",
                8
        ));
        HonorRegistry.register(new HonorDefinition(
                "roadhog",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "roadkill",
                50
        ));
        HonorRegistry.register(new HonorDefinition(
                "quartermaster",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "supply_revive_25",
                35
        ));
        HonorRegistry.register(new HonorDefinition(
                "support",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "support_1000_score",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "tank_destroyer",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "destroy_tank",
                10
        ));
        HonorRegistry.register(new HonorDefinition(
                "recon",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "recon_1000_score",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "assault",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "assault_1000_score",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "engineer",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "engineer_1000_score",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "sharp_shooter",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "long_range_headshot",
                30
        ));
        HonorRegistry.register(new HonorDefinition(
                "long_shot",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "long_shot_range",
                60
        ));
        HonorRegistry.register(new HonorDefinition(
                "marksman",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "sniper_kill_10_15",
                8
        ));
        HonorRegistry.register(new HonorDefinition(
                "rifleman",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "rifle_kill_15_20",
                8
        ));
        HonorRegistry.register(new HonorDefinition(
                "sniper_duel",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "sniper_duel",
                5
        ));
        HonorRegistry.register(new HonorDefinition(
                "smoke_screen",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "smoke_revive_5",
                5
        ));
        HonorRegistry.register(new HonorDefinition(
                "unstoppable",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "mg_kill_5_no_reload",
                10
        ));
        HonorRegistry.register(new HonorDefinition(
                "vanguard",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "first_kill_in_team",
                3
        ));
        HonorRegistry.register(new HonorDefinition(
                "warhead",
                org.mods.gd656killicon.common.honor.ConditionType.CONDITIONAL,
                "heavy_headshot",
                8
        ));
    }
}
