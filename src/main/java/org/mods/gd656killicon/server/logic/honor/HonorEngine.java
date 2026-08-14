package org.mods.gd656killicon.server.logic.honor;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.mods.gd656killicon.common.honor.HonorDefinition;
import org.mods.gd656killicon.common.honor.HonorRegistry;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.HonorPacket;
import dev.architectury.platform.Platform;

import java.util.UUID;

/**
 * 荣誉引擎(服务端核心)。
 * <p>
 * 职责:
 * <ol>
 *   <li>维护每玩家会话状态({@link PlayerHonorState.Store})</li>
 *   <li>接收事件链路归一后的证据(当前为击杀证据), 遍历注册荣誉分派判定</li>
 *   <li>达成时下发 {@link HonorPacket} 给客户端显示</li>
 *   <li>死亡/登入/登出时维护状态(存活段重置、清理)</li>
 * </ol>
 * 事件来源: ForgeServerGameEvents(死亡/登入/登出)与 ServerCombatEngine(击杀)。
 * 实例荣誉无需改动本引擎, 只需在 HonorDefinitionsArea 声明。
 * </p>
 */
public final class HonorEngine {

    private final PlayerHonorState.Store states = new PlayerHonorState.Store();
    /** 对局级(Conquest 单次对局)状态: 跨玩家登出保留, 对局结束(onRoundEnd)清空。 */
    private final java.util.Map<java.util.UUID, Integer> roundDestroyCounts = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<java.util.UUID> roundAchieved = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** 医护兵: 对局级累计救援次数与已达成标记(对局结束清空)。 */
    private final java.util.Map<java.util.UUID, Integer> roundReviveCounts = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<java.util.UUID> roundReviveAchieved = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** 神射手: 对局级(Conquest)狙击枪击杀累计与已达成标记(对局结束清空)。 */
    private final java.util.Map<java.util.UUID, Integer> roundSniperKills = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<java.util.UUID> roundSniperAchieved = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** 炮兵: 对局级(Conquest)载具伤害加分项分数累计与已达成标记(对局结束清空)。 */
    private final java.util.Map<java.util.UUID, Integer> roundArtilleryScores = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<java.util.UUID> roundArtilleryAchieved = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** 急先锋: 对局级已触发阵营(roomId:teamName), 对局结束清空。 */
    private final java.util.Set<String> roundFirstKillTeams = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** 步枪手: 对局级(Conquest)突击步枪击杀累计与已达成标记(对局结束清空)。 */
    private final java.util.Map<java.util.UUID, Integer> roundRifleKills = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<java.util.UUID> roundRifleAchieved = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** 势不可挡: 机枪连续击杀计数(换弹/换非机枪武器时清零)。 */
    private final java.util.Map<java.util.UUID, Integer> machineGunStreak = new java.util.concurrent.ConcurrentHashMap<>();
    /** 地面控制/突破者: 阶段级(Conquest 突破/突袭房间单阶段)统计, 阶段结束时结算并清空。 */
    private final java.util.Map<java.util.UUID, Integer> stageCaptureScores = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<java.util.UUID> stageBombPlanted = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<java.util.UUID> stageBombDefused = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static final String KEY_LR_SMOKE = "lrtactical:smoke_grenade";
    private static final String KEY_SBW_SMOKE = "superbwarfare:m18_smoke_grenade";
    private static final double SMOKE_RADIUS = 5.0;

    // ==================== 事件入口 ====================

    /**
     * 击杀事件(由 ServerCombatEngine 转发)。
     *
     * @param killer           击杀者
     * @param victim           受害者
     * @param headshot         是否爆头
     * @param victimVehicle    受害者是否为载具
     * @param victimAirVehicle 受害者是否为空中载具(集成层未判定时传 false)
     * @param killerRidingAir  击杀者是否搭乘空中载具(集成层未判定时传 false)
     * @param killType         击杀类型(KillType 常量值)
     * @param executioner      刽子手判定: 背刺 && 距离 < 2 米 && 手持原版近战武器或 LR 战术工坊武器
     * @param distance         击杀距离(米)
     * @param moving           击杀时击杀者是否处于移动中(行走或疾跑)
     * @param victimRidingAir  受害者死亡瞬间正搭乘空中载具(载具未被摧毁; 飞行调度员)
     */
    public void onKill(ServerPlayer killer, LivingEntity victim, boolean headshot,
                       boolean victimVehicle, boolean victimAirVehicle, boolean killerRidingAir, int killType,
                       boolean avenge, String weapon, boolean victimTopScorer, boolean executioner, float distance, boolean moving,
                       boolean victimRidingAir) {
        if (killer == null) {
            return;
        }
        KillEvidence evidence = new KillEvidence(killer, victim, headshot,
                victimVehicle, victimAirVehicle, killerRidingAir, killType, avenge, weapon, victimTopScorer, executioner, distance, moving,
                victimRidingAir);
        PlayerHonorState state = states.getOrCreate(killer.getUUID());
        for (HonorDefinition def : HonorRegistry.getAll()) {
            if (HonorJudges.evaluate(def, evidence, state)) {
                deliver(killer, def);
            }
        }
    }

    /**
     * 急救事件(急救使者): 由 BonusEngine.add 在 REVIVE 加分项触发时转发(Conquest 救援完成 → 反射 BONUS.add)。
     * 连续 3 次急救(相邻 ≤ 8 秒: 前 2 次 8 秒内 + 之后 8 秒内第 3 次)达成"急救使者"。
     */
    public void onRevive(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().markRevive(System.currentTimeMillis())) {
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("medic");
            if (def != null) {
                deliver(player, def);
            }
        }
        // 烟幕: 烟雾内救援计数, 一条命 5 次达成(一命一次)
        if (isPlayerInSmoke(player)) {
            state.life().incrementSmokeRevive();
            if (!state.life().isAchieved("smoke_screen") && state.life().getSmokeReviveCount() >= 5) {
                state.life().markAchieved("smoke_screen");
                org.mods.gd656killicon.common.honor.HonorDefinition smokeDef =
                        org.mods.gd656killicon.common.honor.HonorRegistry.get("smoke_screen");
                if (smokeDef != null) {
                    deliver(player, smokeDef);
                }
            }
        }
        // 医护兵: 对局级累计救援 20 人触发一次, 之后不再触发(对局结束重置)
        int roundRevives = roundReviveCounts.merge(player.getUUID(), 1, Integer::sum);
        if (roundRevives >= 20 && !roundReviveAchieved.contains(player.getUUID())) {
            roundReviveAchieved.add(player.getUUID());
            org.mods.gd656killicon.common.honor.HonorDefinition medicDef =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("combat_medic");
            if (medicDef != null) {
                deliver(player, medicDef);
            }
        }
        // 战地医师: 单次存活急救 5 次(一命一次, 死亡重置)
        if (!state.life().isAchieved("field_medic")) {
            if (state.life().increment("field_medic_revive") >= 5) {
                state.life().markAchieved("field_medic");
                org.mods.gd656killicon.common.honor.HonorDefinition fieldMedicDef =
                        org.mods.gd656killicon.common.honor.HonorRegistry.get("field_medic");
                if (fieldMedicDef != null) {
                    deliver(player, fieldMedicDef);
                }
            }
        }
    }

    /**
     * 狙击枪击杀事件(神射手): 由 TACZ/SBW 集成层确认狙击枪类型后转发。
     * 未安装 Conquest → 一命狙击枪击杀 10 触发(一命一次); 安装 Conquest → 侦察兵职业, 对局级累计 15 触发(对局一次性)。
     */
    public void onSniperKill(ServerPlayer player) {
        if (player == null) {
            return;
        }
        java.util.UUID id = player.getUUID();
        boolean conquest = Platform.isModLoaded("gd656conquest");
        if (conquest) {
            if (!"recon".equals(getConquestClassType(player))) {
                return;
            }
            int count = roundSniperKills.merge(id, 1, Integer::sum);
            if (count >= 15 && !roundSniperAchieved.contains(id)) {
                roundSniperAchieved.add(id);
                org.mods.gd656killicon.common.honor.HonorDefinition def =
                        org.mods.gd656killicon.common.honor.HonorRegistry.get("marksman");
                if (def != null) {
                    deliver(player, def);
                }
            }
        } else {
            PlayerHonorState state = states.getOrCreate(id);
            if (state.life().isAchieved("marksman")) {
                return;
            }
            int count = state.life().increment("sniper_kill");
            if (count >= 10) {
                state.life().markAchieved("marksman");
                org.mods.gd656killicon.common.honor.HonorDefinition def =
                        org.mods.gd656killicon.common.honor.HonorRegistry.get("marksman");
                if (def != null) {
                    deliver(player, def);
                }
            }
        }
    }

    /**
     * 突击步枪击杀事件(步枪手): 由 TACZ/SBW 集成层确认突击步枪类型后转发。
     * 未安装 Conquest → 一命突击步枪击杀 15 触发(一命一次); 安装 Conquest → 突击兵职业, 对局级累计 20 触发(对局一次性)。
     */
    public void onRifleKill(ServerPlayer player) {
        if (player == null) {
            return;
        }
        java.util.UUID id = player.getUUID();
        boolean conquest = Platform.isModLoaded("gd656conquest");
        if (conquest) {
            if (!"assault".equals(getConquestClassType(player))) {
                return;
            }
            int count = roundRifleKills.merge(id, 1, Integer::sum);
            if (count >= 20 && !roundRifleAchieved.contains(id)) {
                roundRifleAchieved.add(id);
                org.mods.gd656killicon.common.honor.HonorDefinition def =
                        org.mods.gd656killicon.common.honor.HonorRegistry.get("rifleman");
                if (def != null) {
                    deliver(player, def);
                }
            }
        } else {
            PlayerHonorState state = states.getOrCreate(id);
            if (state.life().isAchieved("rifleman")) {
                return;
            }
            int count = state.life().increment("rifle_kill");
            if (count >= 15) {
                state.life().markAchieved("rifleman");
                org.mods.gd656killicon.common.honor.HonorDefinition def =
                        org.mods.gd656killicon.common.honor.HonorRegistry.get("rifleman");
                if (def != null) {
                    deliver(player, def);
                }
            }
        }
    }

    /**
     * 玩家是否处于烟雾弹实体半径 5 格内。
     * 支持 LR 战术工坊(lrtactical:smoke_grenade)与 SuperbWarfare(superbwarfare:m18_smoke_grenade)的烟雾弹,
     * 均为可选模组, 按实体注册名判断, 避免编译期依赖。
     */
    private static boolean isPlayerInSmoke(ServerPlayer player) {
        if (player == null || player.level() == null) {
            return false;
        }
        double radiusSqr = SMOKE_RADIUS * SMOKE_RADIUS;
        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(SMOKE_RADIUS);
        for (net.minecraft.world.entity.Entity e : player.level().getEntities(player, box)) {
            net.minecraft.resources.ResourceLocation key =
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            if (key == null) {
                continue;
            }
            String keyStr = key.toString();
            if ((KEY_LR_SMOKE.equals(keyStr) || KEY_SBW_SMOKE.equals(keyStr))
                    && e.distanceToSqr(player) <= radiusSqr) {
                return true;
            }
        }
        return false;
    }

    /**
     * 载具非主驾驶位击杀事件(炮手): 由 ServerCombatEngine 在玩家乘坐载具且非主驾驶位击杀时转发。
     * 单次存活累计 5 次触发(一命一次, 死亡重置重新累计)。
     */
    public void onGunnerKill(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().isAchieved("gunner")) {
            return;
        }
        int count = state.life().increment("gunner");
        if (count >= 5) {
            state.life().markAchieved("gunner");
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("gunner");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 载具碾压事件(路霸): 由 ServerCombatEngine 在载具压死生物时转发给驾驶员玩家。
     * 每次满足触发(可一命多次)。
     */
    public void onRoadkill(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("roadhog");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 坦克摧毁事件(坦克破坏者): 由 BonusEngine.add 在 DESTROY_VEHICLE 加分项且被摧毁载具为坦克时转发。
     * 每次满足触发(可一命多次)。
     */
    public void onTankDestroyed(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("tank_destroyer");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 空中载具摧毁事件(弹道): 由 BonusEngine.add 在 DESTROY_VEHICLE 加分项且玩家不在载具内时转发。
     * 每次满足触发(可一命多次)。
     */
    public void onBallistics(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("ballistics");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 王牌飞行员: 由 BonusEngine.add 在 DESTROY_VEHICLE 加分项且玩家正搭乘空中载具时转发
     * (与弹道条件对称: 弹道=不在载具内, 王牌飞行员=在空中载具内)。
     * 每次满足触发(可一命多次)。
     */
    public void onAcePilot(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("ace_pilot");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 布雷者: 使用 SBW 反坦克地雷(TM-62)摧毁一辆载具(由 SuperbWarfareEventHandler 在载具被地雷伤害后摧毁时转发)。
     * 每次满足触发(可一命多次)。
     */
    public void onMineLayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("minelayer");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 载具摧毁事件(爆破专家): 由 BonusEngine.add 在 DESTROY_VEHICLE 加分项触发时挂钩。
     * 安装了 GD656Conquest → 对局级累计(跨登出保留, 对局结束重置); 否则 → 单次生命累计。
     * 累计 7 次摧毁载具达成"爆破专家"。
     */
    public void onVehicleDestroy(ServerPlayer player) {
        if (player == null) {
            return;
        }
        java.util.UUID id = player.getUUID();
        boolean conquest = Platform.isModLoaded("gd656conquest");
        if (conquest) {
            int count = roundDestroyCounts.merge(id, 1, Integer::sum);
            if (count >= 7 && !roundAchieved.contains(id)) {
                roundAchieved.add(id);
                deliverDemolition(player);
            }
        } else {
            PlayerHonorState state = states.getOrCreate(id);
            if (!state.life().isAchieved("demolition") && state.life().increment("destroy_vehicle") >= 7) {
                state.life().markAchieved("demolition");
                deliverDemolition(player);
            }
        }
    }

    /** 对局结束(Conquest 单次对局结束): 清空全部对局级荣誉状态, 由 Conquest 在房间重置时调用。 */
    public void onRoundEnd() {        roundDestroyCounts.clear();
        roundAchieved.clear();
        roundReviveCounts.clear();
        roundReviveAchieved.clear();
        roundSniperKills.clear();
        roundSniperAchieved.clear();
        roundRifleKills.clear();
        roundRifleAchieved.clear();
        roundFirstKillTeams.clear();
        roundArtilleryScores.clear();
        roundArtilleryAchieved.clear();
        clearStageState();
    }

    private void deliverDemolition(ServerPlayer player) {
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("demolition");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 跳机火箭筒 阶段 1: 玩家跳出空中载具(EntityMountEvent dismount)。
     * 记录跳出的载具实体 id, 等待后续"摧毁另一架空中载具"→"回到原载具"。
     */
    public void onVehicleBailOut(ServerPlayer player, net.minecraft.world.entity.Entity vehicle) {
        if (player == null || vehicle == null) {
            return;
        }
        if (!org.mods.gd656killicon.server.logic.core.BonusEngine.isAircraftEntity(vehicle)) {
            return;
        }
        states.getOrCreate(player.getUUID()).life().markBailOut(vehicle.getId());
    }

    /**
     * 跳机火箭筒 阶段 2: 玩家跳出空中载具后摧毁另一架空中载具(BonusEngine DESTROY_VEHICLE 挂钩)。
     * 已跳出且摧毁的不是原载具 → 标记阶段 2 达成。
     */
    public void onBailAirVehicleDestroyed(ServerPlayer player, int victimEntityId) {
        if (player == null) {
            return;
        }
        PlayerHonorState.LifeSegment life = states.getOrCreate(player.getUUID()).life();
        if (life.getBailVehicleEntityId() == 0 || life.isBailAirDestroyed()) {
            return;
        }
        if (victimEntityId == life.getBailVehicleEntityId()) {
            return; // 摧毁的是原载具不算
        }
        life.markBailAirDestroyed();
    }

    /**
     * 跳机火箭筒 阶段 3: 玩家回到刚才乘坐过的载具(EntityMountEvent mount)。
     * 已跳出 && 摧毁过另一架空中载具 && 乘坐的是原载具 → 触发, 并清除状态。
     */
    public void onVehicleMountBack(ServerPlayer player, net.minecraft.world.entity.Entity vehicle) {
        if (player == null || vehicle == null) {
            return;
        }
        PlayerHonorState.LifeSegment life = states.getOrCreate(player.getUUID()).life();
        if (life.getBailVehicleEntityId() == 0 || !life.isBailAirDestroyed()) {
            return;
        }
        if (vehicle.getId() != life.getBailVehicleEntityId()) {
            return;
        }
        life.clearBail();
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("bail_rocket");
        if (def != null) {
            deliver(player, def);
        }
    }

    /** 记录玩家达到 4 连杀的时刻(掠夺者 8 秒窗口起点, 由 ServerCombatEngine 在 KILL_COMBO combo==4 时调用)。 */
    public void recordCombo4(ServerPlayer player) {
        if (player == null) {
            return;
        }
        states.getOrCreate(player.getUUID()).life().markCombo4(System.currentTimeMillis());
    }

    /** Conquest 兵种 → 兵种专家荣誉 id 映射(支援/侦察/突击/工程, 均为 3000 分一命一次)。 */
    private static final java.util.Map<String, String> CLASS_HONOR_IDS = java.util.Map.of(
            "support", "support",
            "recon", "recon",
            "assault", "assault",
            "engineer", "engineer"
    );

    /**
     * 分数获得事件(兵种专家荣誉): 由 ServerData.addScore 挂钩转发。
     * 需要 GD656Conquest 且玩家为对应兵种; 本次存活(不死亡)累计获得 gdki 分数 ≥ 1000 触发一次, 一条命一次。
     */
    public void onScoreGain(ServerPlayer player, float amount) {
        if (player == null || amount <= 0) {
            return;
        }
        String classKey = getConquestClassType(player);
        if (classKey == null) {
            return;
        }
        String honorId = CLASS_HONOR_IDS.get(classKey);
        if (honorId == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().isAchieved(honorId)) {
            return;
        }
        state.life().addClassScore(honorId, amount);
        if (state.life().getClassScore(honorId) >= 1000) {
            state.life().markAchieved(honorId);
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get(honorId);
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 补给/救援加分项事件(军需官): 由 BonusEngine.add 在 AMMO_SUPPLY(64)或 REVIVE(65)加分项触发时挂钩。
     * 需要 Conquest 或 GD656FrontVoice 模组; 单次存活累计 25 次触发(一命一次, 死亡重置重新累计)。
     */
    public void onQuartermaster(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().isAchieved("quartermaster")) {
            return;
        }        int count = state.life().increment("quartermaster");
        if (count >= 25) {
            state.life().markAchieved("quartermaster");
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("quartermaster");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 修理载具加分项事件(机械师): 由 BonusEngine.add 在 VEHICLE_REPAIR 加分项触发时挂钩。
     * 单次存活累计 25 次触发(一命一次, 死亡重置重新累计)。
     */
    public void onRepairVehicle(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().isAchieved("mechanic")) {
            return;
        }
        int count = state.life().increment("repair_vehicle");
        if (count >= 25) {
            state.life().markAchieved("mechanic");
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("mechanic");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 载具伤害加分项分数事件(炮兵): 由 BonusEngine.processPlayerBonuses 在 HIT_VEHICLE_ARMOR
     * 加分项实际发放时挂钩(按玩家收到的分数累计)。
     * 安装了 GD656Conquest → 单次对局累计分数 ≥ 1500 触发一次(对局结束重置, 跨登出保留);
     * 否则 → 单次生命累计分数 ≥ 800 触发一次(死亡重置重新累计)。
     */
    public void onArtilleryHit(ServerPlayer player, float score) {
        if (player == null || score <= 0) {
            return;
        }
        java.util.UUID id = player.getUUID();
        boolean conquest = Platform.isModLoaded("gd656conquest");
        if (conquest) {
            int total = roundArtilleryScores.merge(id, Math.round(score), Integer::sum);
            if (total >= 1500 && !roundArtilleryAchieved.contains(id)) {
                roundArtilleryAchieved.add(id);
                deliverArtillery(player);
            }
        } else {
            PlayerHonorState state = states.getOrCreate(id);
            if (!state.life().isAchieved("artillery")) {
                int total = state.life().add("artillery_vehicle_damage", Math.round(score));
                if (total >= 800) {
                    state.life().markAchieved("artillery");
                    deliverArtillery(player);
                }
            }
        }
    }

    private void deliverArtillery(ServerPlayer player) {
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("artillery");
        if (def != null) {
            deliver(player, def);
        }
    }

    // ==================== 地面控制 / 突破者(Conquest 阶段级) ====================

    /** 突袭模式阶段占领分累计: 由 BonusEngine.processPlayerBonuses 在 CONQUEST_CAPTURE_* 发放时挂钩。 */
    public void onConquestCaptureScore(ServerPlayer player, float score) {
        if (player == null || score <= 0) {
            return;
        }
        stageCaptureScores.merge(player.getUUID(), Math.round(score), Integer::sum);
    }

    /** 突破模式阶段安放炸弹标记: 由 BonusEngine.processPlayerBonuses 在 RUSH_BOMB_PLANTED 发放时挂钩。 */
    public void onRushBombPlanted(ServerPlayer player) {
        if (player != null) {
            stageBombPlanted.add(player.getUUID());
        }
    }

    /** 突破模式阶段拆除炸弹标记: 由 BonusEngine.processPlayerBonuses 在 RUSH_BOMB_DEFUSED 发放时挂钩。 */
    public void onRushBombDefused(ServerPlayer player) {
        if (player != null) {
            stageBombDefused.add(player.getUUID());
        }
    }

    /**
     * Conquest 突破/突袭房间单阶段结束结算(由 Conquest 侧 KilliconHonorBridge 在阶段推进点调用)。
     * <ul>
     *   <li>进攻方(CAMP_A): 阶段击杀 > 5 且(突袭: 占领分 > 150 / 突破: 安放过炸弹) → 突破者</li>
     *   <li>防守方(CAMP_B): 阶段击杀 > 7 且(突袭: 占领分 > 50 / 突破: 拆除过炸弹) → 地面控制</li>
     * </ul>
     * 结算后清空本阶段统计。
     *
     * @param stageKills 本阶段每玩家击杀数(Conquest 侧统计, 击杀敌方玩家/载具)
     * @param teams      本阶段玩家阵营(CAMP_A / CAMP_B)
     * @param isRush     是否突破(rush)房间: true → 用炸弹事件替代占领分; false → 突袭用占领分
     */
    public void onConquestStageEnd(
        net.minecraft.server.MinecraftServer server,
        java.util.Map<java.util.UUID, Integer> stageKills,
        java.util.Map<java.util.UUID, String> teams,
        boolean isRush
    ) {
        if (server == null || stageKills == null || teams == null) {
            clearStageState();
            return;
        }
        for (java.util.Map.Entry<java.util.UUID, Integer> entry : stageKills.entrySet()) {
            java.util.UUID playerId = entry.getKey();
            int kills = entry.getValue() == null ? 0 : entry.getValue();
            String team = teams.get(playerId);
            if (team == null || playerId == null) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }
            boolean attacker = "CAMP_A".equals(team);
            if (attacker) {
                boolean objective = isRush
                        ? stageBombPlanted.contains(playerId)
                        : stageCaptureScores.getOrDefault(playerId, 0) > 150;
                if (kills > 5 && objective) {
                    org.mods.gd656killicon.common.honor.HonorDefinition def =
                            org.mods.gd656killicon.common.honor.HonorRegistry.get("breaker");
                    if (def != null) {
                        deliver(player, def);
                    }
                }
            } else {
                boolean objective = isRush
                        ? stageBombDefused.contains(playerId)
                        : stageCaptureScores.getOrDefault(playerId, 0) > 50;
                if (kills > 7 && objective) {
                    org.mods.gd656killicon.common.honor.HonorDefinition def =
                            org.mods.gd656killicon.common.honor.HonorRegistry.get("ground_control");
                    if (def != null) {
                        deliver(player, def);
                    }
                }
            }
        }
        clearStageState();
    }

    private void clearStageState() {
        stageCaptureScores.clear();
        stageBombPlanted.clear();
        stageBombDefused.clear();
    }

    /**
     * 对局开始后该阵营第一个击杀事件(急先锋): 由 ServerCombatEngine 在 Conquest 对局中击杀时转发。
     * 每个 Conquest 阵营(CAMP_A/CAMP_B)对局内第一个击杀者触发一次; 对局结束(onRoundEnd)重置。
     */
    public void onFirstKill(ServerPlayer player, String roomTeamKey) {
        if (player == null || roomTeamKey == null || roomTeamKey.isBlank()) {
            return;
        }
        if (roundFirstKillTeams.add(roomTeamKey)) {
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("vanguard");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 标记敌人事件(斥候): 由 Conquest 索敌标记放置成功事件(ConquestMarkerSpotPlacedEvent)转发。
     * 单次存活累计 20 次触发(一命一次, 死亡重置重新累计); 无索敌加分项的 CD, 每次标记 +1。
     */
    public void onScoutMark(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().isAchieved("scout")) {
            return;
        }
        int count = state.life().increment("scout");
        if (count >= 20) {
            state.life().markAchieved("scout");
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("scout");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 据点内击杀事件(戍卫): 由 ServerCombatEngine 在 Conquest 据点内击杀且据点有敌军时转发。
     * 单次存活累计 3 次触发(一命一次, 死亡重置重新累计)。
     */
    public void onGarrisonKill(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().isAchieved("garrison")) {
            return;
        }
        int count = state.life().increment("garrison");
        if (count >= 3) {
            state.life().markAchieved("garrison");
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("garrison");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 摧毁战术道具事件(破坏者): 由 BonusEngine.add 在 TACTICAL_GADGET_DESTROYED 加分项触发时挂钩。
     * 单次存活累计 3 次触发(一命一次, 死亡重置重新累计)。
     */
    public void onTacticalGadgetDestroyed(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().isAchieved("destroyer")) {
            return;
        }
        int count = state.life().increment("tactical_gadget");
        if (count >= 3) {
            state.life().markAchieved("destroyer");
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("destroyer");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 火力压制加分项事件(火力支援): 由 BonusEngine.add 在 FIRE_SUPPRESSION 加分项触发时挂钩。
     * 单次存活累计 5 次触发(一命一次, 死亡重置重新累计)。
     */
    public void onFireSupport(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().isAchieved("fire_support")) {
            return;
        }
        int count = state.life().increment("fire_support");
        if (count >= 5) {
            state.life().markAchieved("fire_support");
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("fire_support");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 小队部署到你位置事件(引路者): 由 BonusEngine.add 在 SQUAD_DEPLOY_ON_YOU 加分项触发时挂钩。
     * 单次存活累计 3 次触发(一命一次, 死亡重置重新累计)。
     */
    public void onSquadDeploy(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerHonorState state = states.getOrCreate(player.getUUID());
        if (state.life().isAchieved("pathfinder")) {
            return;
        }
        int count = state.life().increment("squad_deploy");
        if (count >= 3) {
            state.life().markAchieved("pathfinder");
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("pathfinder");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /**
     * 增益击杀事件(荣誉狂战士): 由 BonusEngine.add 在 BUFF_KILL(16, 凭效诛敌)加分项触发时挂钩。
     * 注意与加分项"狂战士"(BERSERKER=27)区分。每次触发(可一命多次)。
     * 触发条件: 击杀者身上带有速度(MOVEMENT_SPEED)或生命恢复(REGENERATION)效果才算。
     */
    public void onFrenzy(ServerPlayer player) {
        if (player == null) {
            return;
        }
        boolean hasSpeed = player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
        boolean hasRegen = player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
        if (!hasSpeed && !hasRegen) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("frenzy");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 击杀仇敌事件(护卫者): 由 BonusEngine.add 在 SAVIOR 救星加分项触发时挂钩。
     * 每次触发(可一命多次)。
     */
    public void onGuardian(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("guardian");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 击杀仇敌事件(复仇者): 由 BonusEngine.add 在 LEAVE_IT_TO_ME 交给我加分项触发时挂钩。
     * 每次触发(可一命多次)。
     */
    public void onAvenger(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("avenger");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 拆除炸弹事件(炸弹技术员): 由 BonusEngine.add 在 RUSH_BOMB_DEFUSED 加分项触发时挂钩。
     * 每次触发(可一命多次)。
     */
    public void onBombDefuse(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("bomb_technician");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 摧毁通讯设施事件(引爆器): 由 BonusEngine.add 在 RUSH_OBJECTIVE_DESTROYED 加分项触发时挂钩。
     * 每次触发(可一命多次)。
     */
    public void onDetonator(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("detonator");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 击杀致盲生物事件(侧袭): 由 ServerCombatEngine 在受害者带 LR 致盲效果时转发。
     * 每次满足触发(可一命多次)。
     */
    public void onFlankKill(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("flank");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 重型武器爆头击杀事件(弹头): 由 TACZ/SBW 集成层确认重型武器 + 爆头后转发。
     * 每次满足条件都触发(可一条命多次触发)。
     */
    public void onWarheadKill(ServerPlayer player) {
        if (player == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("warhead");
        if (def != null) {
            deliver(player, def);
        }
    }

    /**
     * 机枪击杀事件(势不可挡): 由 TACZ/SBW 集成层确认机枪类型后转发。
     * 手持机枪且不换弹连续击杀 5 生物触发; 非机枪击杀/换弹清零。可一条命多次触发(达成后计数重置)。
     */
    public void onMachineGunKill(ServerPlayer player, boolean isMachineGun) {
        if (player == null) {
            return;
        }
        if (!isMachineGun) {
            machineGunStreak.remove(player.getUUID());
            return;
        }
        int count = machineGunStreak.merge(player.getUUID(), 1, Integer::sum);
        if (count >= 5) {
            machineGunStreak.remove(player.getUUID());
            org.mods.gd656killicon.common.honor.HonorDefinition def =
                    org.mods.gd656killicon.common.honor.HonorRegistry.get("unstoppable");
            if (def != null) {
                deliver(player, def);
            }
        }
    }

    /** 换弹事件(势不可挡): 换弹中断机枪连续击杀, 由 TACZ GunReloadEvent / SBW ReloadEvent.Post 转发。 */
    public void onReload(ServerPlayer player) {
        if (player != null) {
            machineGunStreak.remove(player.getUUID());
        }
    }

    /**
     * 击杀开镜狙击手事件(对狙专家): 由 TaczEventHandler 在枪械击杀事件中判定后转发。
     * 击杀一名手持 TACZ 狙击枪且正在开镜瞄准的玩家; 每次满足都触发(可一命多次)。
     */
    public void onSniperDuel(ServerPlayer killer) {
        if (killer == null) {
            return;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get("sniper_duel");
        if (def != null) {
            deliver(killer, def);
        }
    }

    /** 玩家死亡(存活段重置; 由 ForgeServerGameEvents 转发)。 */
    public void onPlayerDeath(ServerPlayer player) {
        PlayerHonorState state = states.get(player.getUUID());
        if (state != null) {
            state.onDeath();
        }
    }

    /** 玩家登入(预创建状态, 防止首次击杀时空指针/重复分配)。 */
    public void onPlayerJoin(ServerPlayer player) {
        states.getOrCreate(player.getUUID());
    }

    /** 玩家登出(清理会话状态)。 */
    public void onPlayerLogout(ServerPlayer player) {
        states.remove(player.getUUID());
    }

    /** 服务器停止(清理全部状态)。 */
    public void onServerStop() {
        states.clear();
    }

    // ==================== 下发 ====================

    private void deliver(ServerPlayer player, HonorDefinition def) {
        boolean conquestInstalled = Platform.isModLoaded("gd656conquest");
        boolean globalBest = false;
        boolean matchBest = false;
        if (conquestInstalled) {
            // 装 Conquest: 完全不读不写 playerdata 的 honor 数据, 只做本局内存统计(单次对局)
            matchBest = org.mods.gd656killicon.server.logic.conquest.ConquestHonorAdapter.recordMatchHonor(player, def.id()) == 2;
        } else {
            // 无 Conquest: 累计到玩家 PlayerData, 与全服最高缓存比较判"全服最多"
            int newCount = org.mods.gd656killicon.server.data.PlayerDataManager.get().recordHonor(player.getUUID(), def.id());
            globalBest = newCount > 0 && newCount >= org.mods.gd656killicon.server.data.PlayerDataManager.get().getGlobalBest(def.id());
        }
        // 样式标记: g=全服最多 m=本局最多 gm=两者都是(并列都算), 仅当次触发显示
        String style = globalBest && matchBest ? "gm" : globalBest ? "g" : matchBest ? "m" : "";
        NetworkHandler.sendToPlayer(new HonorPacket(def.id(), style.isEmpty() ? "" : "style=" + style), player);
    }

    // ==================== Conquest 兵种判定(反射, 可选模组) ====================

    private static boolean conquestClassReady = false;
    private static boolean conquestClassChecked = false;
    private static java.lang.reflect.Method conquestClassOfMethod;
    private static java.lang.reflect.Method conquestClassPlayerDataMethod;
    private static java.lang.reflect.Method conquestClassGetPlayerDataMethod;
    private static java.lang.reflect.Method conquestClassGetClassTypeMethod;

    /** 安装了 GD656Conquest 时返回玩家当前兵种 key(如 "support"/"recon"/"assault"/"engineer"), 否则返回 null。 */
    private static String getConquestClassType(ServerPlayer player) {
        if (player == null || player.server == null) {
            return null;
        }
        try {
            if (!conquestClassReady && !conquestClassChecked) {
                conquestClassChecked = true;
                if (Platform.isModLoaded("gd656conquest")) {
                    try {
                        Class<?> dataManager = Class.forName("org.mods.gd656conquest.server.data.ConquestDataManager");
                        conquestClassOfMethod = dataManager.getMethod("of", net.minecraft.server.MinecraftServer.class);
                        conquestClassPlayerDataMethod = dataManager.getMethod("playerData");
                        Class<?> store = Class.forName("org.mods.gd656conquest.server.data.playerdata.PlayerDataStore");
                        conquestClassGetPlayerDataMethod = store.getMethod("getPlayerData", java.util.UUID.class);
                        Class<?> model = Class.forName("org.mods.gd656conquest.server.data.playerdata.PlayerDataModel");
                        conquestClassGetClassTypeMethod = model.getMethod("getCurrentClassType");
                        conquestClassReady = true;
                    } catch (Exception initFailure) {
                        // 反射初始化失败: 不永久缓存失败(下次调用重试, 可能 Conquest 尚未就绪)
                        conquestClassChecked = false;
                    }
                }
            }
            if (!conquestClassReady) {
                return null;
            }
            Object dataManager = conquestClassOfMethod.invoke(null, player.server);
            Object store = conquestClassPlayerDataMethod.invoke(dataManager);
            Object model = conquestClassGetPlayerDataMethod.invoke(store, player.getUUID());
            return (String) conquestClassGetClassTypeMethod.invoke(model);
        } catch (Exception e) {
            return null;
        }
    }
}
