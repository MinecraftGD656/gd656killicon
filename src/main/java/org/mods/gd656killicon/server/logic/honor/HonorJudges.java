package org.mods.gd656killicon.server.logic.honor;

import org.mods.gd656killicon.common.honor.ConditionType;
import org.mods.gd656killicon.common.honor.HonorDefinition;

import java.util.EnumMap;
import java.util.Map;

/**
 * 分型判定器注册表(仿 BonusRegistry 的声明式思路)。
 * <p>
 * 每种 {@link ConditionType} 对应一个通用判定器, 荣誉声明只需提供参数,
 * 不需要为每个荣誉编写新判定类。新增条件类型时在此注册新判定器。
 * </p>
 * <ul>
 *   <li>KILL_STREAK: 存活段内满足谓词的击杀计数 ≥ 阈值(参数 "&lt;谓词&gt;:&lt;阈值&gt;")</li>
 *   <li>CONDITIONAL: 单事件谓词(参数 "&lt;谓词键&gt;")</li>
 *   <li>ACCUMULATE: 存活段内数值累计 ≥ 阈值(预留, 暂不触发)</li>
 * </ul>
 */
public final class HonorJudges {

    private static final Map<ConditionType, HonorJudge> JUDGES = new EnumMap<>(ConditionType.class);

    static {
        JUDGES.put(ConditionType.KILL_STREAK, HonorJudges::killStreak);
        JUDGES.put(ConditionType.CONDITIONAL, HonorJudges::conditional);
        JUDGES.put(ConditionType.ACCUMULATE, HonorJudges::accumulate);
    }

    private HonorJudges() {
    }

    public static HonorJudge get(ConditionType type) {
        return JUDGES.get(type);
    }

    /** 分派入口: 引擎对每条注册荣誉调用。 */
    public static boolean evaluate(HonorDefinition def, KillEvidence evidence, PlayerHonorState state) {
        HonorJudge judge = JUDGES.get(def.conditionType());
        return judge != null && judge.evaluate(def, evidence, state);
    }

    // ==================== 分型判定器 ====================

    /** KILL_STREAK: "headshot:3" → 存活段内爆头击杀计数 ≥ 3 时达成; 每条命只触发一次, 死亡/重生后重新可触发。 */
    private static boolean killStreak(HonorDefinition def, KillEvidence evidence, PlayerHonorState state) {
        // 本条命已达成过 → 不再触发
        if (state.life().isAchieved(def.id())) {
            return false;
        }
        String[] parts = splitParams(def.conditionParams(), 2);
        if (parts == null) {
            return false;
        }
        String predicate = parts[0];
        int threshold;
        try {
            threshold = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (threshold <= 0 || !matchesPredicate(predicate, evidence, state)) {
            return false;
        }
        int count = state.life().increment(predicate);
        if (count < threshold) {
            return false;
        }
        // 达成: 标记本条命已达成(不再触发), 重置计数供下条命重新累计
        state.life().markAchieved(def.id());
        state.life().add(predicate, -threshold);
        return true;
    }

    /** CONDITIONAL: 单事件谓词; 每次满足条件都触发(可一条命多次触发, 与 KILL_STREAK 的"一条命一次"不同)。 */
    private static boolean conditional(HonorDefinition def, KillEvidence evidence, PlayerHonorState state) {
        return matchesPredicate(def.conditionParams(), evidence, state);
    }

    /** ACCUMULATE: 预留(修复/伤害等非击杀累计), 当前无证据源, 恒不达成。 */
    private static boolean accumulate(HonorDefinition def, KillEvidence evidence, PlayerHonorState state) {
        return false;
    }

    // ==================== 基础谓词库 ====================

    /**
     * 击杀谓词: 框架内置基础谓词, 实例荣誉与后续扩展在此扩充。
     * 谓词为单事件判定(某次击杀是否满足条件)。
     */
    private static boolean matchesPredicate(String predicate, KillEvidence evidence, PlayerHonorState state) {
        if (predicate == null || predicate.isBlank()) {
            return false;
        }
        return switch (predicate) {
            case "any" -> true;                                      // 任意击杀
            case "headshot" -> evidence.headshot();                  // 爆头击杀
            case "moving" -> evidence.moving();                       // 战士: 行走或疾跑(移动中)状态下的击杀
            case "vehicle_kill" -> evidence.victimVehicle();         // 击毁载具
            case "air_vehicle_kill" -> evidence.victimAirVehicle();  // 击毁空中载具
            case "victim_riding_air" -> evidence.victimRidingAir();  // 飞行调度员: 击杀正搭乘空中载具的玩家(载具未摧毁)
            case "avenge" -> evidence.avenge();                      // 复仇击杀(与加分项 AVENGE 同条件)
            case "backstab" -> evidence.executioner();                // 刽子手: 背刺(与加分项 BACKSTAB 同判定) && 距离<2 米 && 手持近战武器(原版剑/斧或 LR 战术工坊)
            case "long_range_headshot" ->                           // 神枪手: 手持 TACZ/SBW 武器 && 距离 ≥ 100 米 && 爆头击杀
                    isTaczOrSbwWeapon(evidence) && evidence.distance() >= 100.0f && evidence.headshot();
            case "long_shot_range" ->                               // 远射: 手持 TACZ/SBW 武器 && 距离 50~100 米(100 米归神枪手)
                    isTaczOrSbwWeapon(evidence) && evidence.distance() >= 50.0f && evidence.distance() < 100.0f;
            case "victim_top_scorer" -> evidence.victimTopScorer();  // 高层: 击杀最高得分者(与加分项 SLAY_THE_LEADER 同条件)
            case "kill_within_8s_after_combo4" -> {                   // 掠夺者: 4 连杀后 8 秒内再击杀, 触发后 30 秒冷却
                long combo4 = state.life().getCombo4Time();
                if (combo4 <= 0 || System.currentTimeMillis() - combo4 > 8000) {
                    yield false;
                }
                long lastTrigger = state.getRaiderLastTriggerTime();
                if (lastTrigger != 0 && System.currentTimeMillis() - lastTrigger < 30000) {
                    yield false;                                       // 30 秒冷却中
                }
                state.markRaiderTriggered(System.currentTimeMillis());
                yield true;
            }
            case "arsenal_in_30s_3_items" -> {                         // 军械库: 30 秒内用 3 种不同物品击杀 3 生物
                String weapon = evidence.weapon();
                if (weapon == null || weapon.isEmpty()) {
                    yield false;                                       // 空手不算物品
                }
                int distinct = state.life().markArsenalWeapon(weapon, System.currentTimeMillis());
                if (distinct < 3) {
                    yield false;
                }
                // 安装了 GD656Conquest 时: 仅突击兵可触发
                if (isConquestAssaultCheck(evidence)) {
                    yield false;
                }
                yield true;
            }
            case "quickdraw" -> {                                       // 快枪手: 4 秒内用不同武器击杀两个生物
                String weapon = evidence.weapon();
                long now = System.currentTimeMillis();
                long lastTime = state.life().getLastKillTime();
                String lastWeapon = state.life().getLastKillWeapon();
                state.life().markKill(weapon, now);
                yield lastTime > 0 && now - lastTime <= 4000
                        && lastWeapon != null && weapon != null
                        && !lastWeapon.equals(weapon);
            }
            // TODO: 更多谓词(按需扩展, 如 未搭乘空中载具击杀 等组合谓词)
            default -> false;
        };
    }

    // ==================== Conquest 突击兵判定(反射, 可选模组) ====================

    /**
     * 手持武器是否为 TACZ 或 SBW 武器(远射/神枪手限定):
     * 用物品注册名命名空间判断("tacz" / "superbwarfare"), 避免编译期依赖可选模组。
     */
    private static boolean isTaczOrSbwWeapon(KillEvidence evidence) {
        if (evidence.killer() == null) {
            return false;
        }
        net.minecraft.world.item.ItemStack hand = evidence.killer().getMainHandItem();
        if (hand == null || hand.isEmpty()) {
            return false;
        }
        net.minecraft.resources.ResourceLocation key =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(hand.getItem());
        if (key == null) {
            return false;
        }
        String ns = key.getNamespace();
        return "tacz".equals(ns) || "superbwarfare".equals(ns);
    }

    private static boolean conquestAssaultReady = false;
    private static boolean conquestAssaultChecked = false;
    private static java.lang.reflect.Method conquestOfMethod;
    private static java.lang.reflect.Method conquestPlayerDataMethod;
    private static java.lang.reflect.Method conquestGetPlayerDataMethod;
    private static java.lang.reflect.Method conquestGetClassTypeMethod;

    /** 安装了 GD656Conquest 且玩家不是突击兵 → 返回 true(拦截军械库)。 */
    private static boolean isConquestAssaultCheck(KillEvidence evidence) {
        try {
            if (!conquestAssaultChecked) {
                conquestAssaultChecked = true;
                if (dev.architectury.platform.Platform.isModLoaded("gd656conquest")) {
                    Class<?> dataManager = Class.forName("org.mods.gd656conquest.server.data.ConquestDataManager");
                    conquestOfMethod = dataManager.getMethod("of", net.minecraft.server.MinecraftServer.class);
                    conquestPlayerDataMethod = dataManager.getMethod("playerData");
                    Class<?> store = Class.forName("org.mods.gd656conquest.server.data.playerdata.PlayerDataStore");
                    conquestGetPlayerDataMethod = store.getMethod("getPlayerData", java.util.UUID.class);
                    Class<?> model = Class.forName("org.mods.gd656conquest.server.data.playerdata.PlayerDataModel");
                    conquestGetClassTypeMethod = model.getMethod("getCurrentClassType");
                    conquestAssaultReady = true;
                }
            }
            if (!conquestAssaultReady || evidence.killer() == null) {
                return false;
            }
            Object dataManager = conquestOfMethod.invoke(null, evidence.killer().server);
            Object store = conquestPlayerDataMethod.invoke(dataManager);
            Object model = conquestGetPlayerDataMethod.invoke(store, evidence.killer().getUUID());
            String classKey = (String) conquestGetClassTypeMethod.invoke(model);
            return !"assault".equals(classKey); // 非突击兵 → 拦截
        } catch (Exception e) {
            return false;
        }
    }

    /** 按分隔符 ":" 拆分参数, 段数不符返回 null。 */
    private static String[] splitParams(String params, int expect) {
        if (params == null || params.isBlank()) {
            return null;
        }
        String[] parts = params.split(":", -1);
        return parts.length == expect ? parts : null;
    }
}
