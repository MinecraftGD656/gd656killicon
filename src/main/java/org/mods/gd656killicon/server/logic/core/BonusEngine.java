package org.mods.gd656killicon.server.logic.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.bonus.BonusDefinition;
import org.mods.gd656killicon.common.bonus.BonusRegistry;
import org.mods.gd656killicon.common.bonus.MergeBehavior;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.BonusScorePacket;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.packet.KillIconPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BonusEngine {
    private record Entry(int type, float score, String extra, int victimId, String victimName, float scale) {}

    /**
     * Map of player UUID to a list of pending bonus entries.
     * Uses ConcurrentHashMap and synchronized lists for thread safety.
     */
    private final Map<UUID, List<Entry>> pending = new ConcurrentHashMap<>();

    /**
     * 空中载具判定(不联动 Conquest, 不靠名字猜):
     * - SBW: 反射 `VehicleEntity.getVehicleType()`(VehicleType 枚举) ∈ {AIRPLANE, HELICOPTER, AIRSHIP}
     * - YWZJ: 反射 instanceof `RotaryWingVehicle` / `FixedWingVehicle`(空中载具基类, 含直升机/无人机/固定翼)
     */
    public static boolean isAircraftEntity(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return false;
        }
        if (isSbwAircraft(entity) || isYwzjAircraft(entity)) {
            return true;
        }
        return false;
    }

    /** SBW 空中载具: VehicleEntity.getVehicleType() 返回 VehicleType 枚举。 */
    private static boolean isSbwAircraft(net.minecraft.world.entity.Entity entity) {
        try {
            if (sbwVehicleTypeMethod == null) {
                Class<?> vehicleClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
                sbwVehicleTypeMethod = vehicleClass.getMethod("getVehicleType");
            }
            if (sbwVehicleTypeMethod == null) {
                return false;
            }
            Object type = sbwVehicleTypeMethod.invoke(entity);
            if (type == null) {
                return false;
            }
            String typeName = type.toString();
            return "AIRPLANE".equalsIgnoreCase(typeName)
                    || "HELICOPTER".equalsIgnoreCase(typeName)
                    || "AIRSHIP".equalsIgnoreCase(typeName);
        } catch (Exception e) {
            return false;
        }
    }

    /** YWZJ 空中载具: RotaryWingVehicle(旋翼/直升机/无人机)或 FixedWingVehicle(固定翼飞机)子类。 */
    private static boolean isYwzjAircraft(net.minecraft.world.entity.Entity entity) {
        try {
            if (ywzjRotaryClass == null) {
                ywzjRotaryClass = Class.forName("org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle");
                ywzjFixedWingClass = Class.forName("org.ywzj.vehicle.entity.vehicle.FixedWingVehicle");
            }
            return (ywzjRotaryClass != null && ywzjRotaryClass.isInstance(entity))
                    || (ywzjFixedWingClass != null && ywzjFixedWingClass.isInstance(entity));
        } catch (Exception e) {
            return false;
        }
    }

    /** 坦克判定: SBW VehicleType.TANK 或 YWZJ TrackedVehicle(履带坦克基类, 含 M1a2/Ztz99a)。 */
    private static boolean isTankEntity(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return false;
        }
        // SBW: getVehicleType() == TANK
        try {
            if (sbwVehicleTypeMethod == null) {
                Class<?> vehicleClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
                sbwVehicleTypeMethod = vehicleClass.getMethod("getVehicleType");
            }
            if (sbwVehicleTypeMethod != null) {
                Object type = sbwVehicleTypeMethod.invoke(entity);
                if (type != null && "TANK".equalsIgnoreCase(type.toString())) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        // YWZJ: instanceof TrackedVehicle
        try {
            if (ywzjTrackedClass == null) {
                ywzjTrackedClass = Class.forName("org.ywzj.vehicle.entity.vehicle.TrackedVehicle");
            }
            return ywzjTrackedClass != null && ywzjTrackedClass.isInstance(entity);
        } catch (Exception e) {
            return false;
        }
    }

    private static java.lang.reflect.Method sbwVehicleTypeMethod;
    private static Class<?> ywzjRotaryClass;
    private static Class<?> ywzjFixedWingClass;
    private static Class<?> ywzjTrackedClass;

    public void add(ServerPlayer player, int type, float scale, String extra) {
        add(player, type, scale, extra, -1, null);
    }

    public void add(ServerPlayer player, int type, float scale, String extra, int victimId) {
        add(player, type, scale, extra, victimId, null);
    }

    /**
     * Adds a bonus entry for a player.
     */
    public void add(ServerPlayer player, int type, float scale, String extra, int victimId, String victimName) {
        // 载具摧毁事件(爆破专家荣誉): 由 Killicon 各载具层触发加分项时挂钩
        org.mods.gd656killicon.common.bonus.BonusDefinition destroyDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("DESTROY_VEHICLE");
        if (destroyDef != null && type == destroyDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onVehicleDestroy(player);
            // 弹道荣誉: 玩家不在任何载具内摧毁空中载具(可一命多次)
            if (victimId >= 0 && player != null && !player.isPassenger()) {
                net.minecraft.world.entity.Entity vehicle = player.level().getEntity(victimId);
                if (vehicle != null && isAircraftEntity(vehicle)) {
                    org.mods.gd656killicon.server.ServerCore.HONOR.onBallistics(player);
                }
            }
            // 王牌飞行员荣誉: 玩家正搭乘空中载具时摧毁敌军空中载具(可一命多次, 与弹道条件对称)
            if (victimId >= 0 && player != null && player.isPassenger()
                    && isAircraftEntity(player.getVehicle())) {
                net.minecraft.world.entity.Entity vehicle = player.level().getEntity(victimId);
                if (vehicle != null && isAircraftEntity(vehicle)) {
                    org.mods.gd656killicon.server.ServerCore.HONOR.onAcePilot(player);
                }
            }
            // 跳机火箭筒荣誉: 玩家跳出空中载具后摧毁另一架空中载具(阶段 2)
            if (victimId >= 0 && player != null) {
                net.minecraft.world.entity.Entity vehicle = player.level().getEntity(victimId);
                if (vehicle != null && isAircraftEntity(vehicle)) {
                    org.mods.gd656killicon.server.ServerCore.HONOR.onBailAirVehicleDestroyed(player, victimId);
                }
            }
            // 坦克破坏者荣誉: 摧毁敌方坦克(可一命多次)
            if (victimId >= 0 && player != null) {
                net.minecraft.world.entity.Entity vehicle = player.level().getEntity(victimId);
                if (vehicle != null && isTankEntity(vehicle)) {
                    org.mods.gd656killicon.server.ServerCore.HONOR.onTankDestroyed(player);
                }
            }
        }
        // 摧毁通讯设施事件(引爆器荣誉): RUSH_OBJECTIVE_DESTROYED 加分项触发时挂钩
        org.mods.gd656killicon.common.bonus.BonusDefinition rushDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("RUSH_OBJECTIVE_DESTROYED");
        if (rushDef != null && type == rushDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onDetonator(player);
        }
        // 拆除炸弹事件(炸弹技术员荣誉): RUSH_BOMB_DEFUSED 加分项触发时挂钩
        org.mods.gd656killicon.common.bonus.BonusDefinition defuseDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("RUSH_BOMB_DEFUSED");
        if (defuseDef != null && type == defuseDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onBombDefuse(player);
        }
        // 击杀仇敌事件(护卫者荣誉): SAVIOR 救星加分项触发时挂钩
        org.mods.gd656killicon.common.bonus.BonusDefinition guardianDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("SAVIOR");
        if (guardianDef != null && type == guardianDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onGuardian(player);
        }
        // 复仇者荣誉: LEAVE_IT_TO_ME 交给我加分项触发时挂钩
        org.mods.gd656killicon.common.bonus.BonusDefinition avengerDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("LEAVE_IT_TO_ME");
        if (avengerDef != null && type == avengerDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onAvenger(player);
        }
        // 荣誉狂战士: BUFF_KILL(16, 凭效诛敌)加分项触发时挂钩(与加分项 BERSERKER 区分)
        org.mods.gd656killicon.common.bonus.BonusDefinition frenzyDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("BUFF_KILL");
        if (frenzyDef != null && type == frenzyDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onFrenzy(player);
        }
        // 引路者荣誉: SQUAD_DEPLOY_ON_YOU(小队在你的位置部署)加分项触发时挂钩
        org.mods.gd656killicon.common.bonus.BonusDefinition squadDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("SQUAD_DEPLOY_ON_YOU");
        if (squadDef != null && type == squadDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onSquadDeploy(player);
        }
        // 火力支援荣誉: FIRE_SUPPRESSION(火力压制)加分项触发时挂钩
        org.mods.gd656killicon.common.bonus.BonusDefinition fireDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("FIRE_SUPPRESSION");
        if (fireDef != null && type == fireDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onFireSupport(player);
        }
        // 破坏者荣誉: TACTICAL_GADGET_DESTROYED(摧毁战术道具)加分项触发时挂钩
        org.mods.gd656killicon.common.bonus.BonusDefinition gadgetDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("TACTICAL_GADGET_DESTROYED");
        if (gadgetDef != null && type == gadgetDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onTacticalGadgetDestroyed(player);
        }
        // 机械师荣誉: VEHICLE_REPAIR(载具修理)加分项触发时挂钩
        org.mods.gd656killicon.common.bonus.BonusDefinition repairDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("VEHICLE_REPAIR");
        if (repairDef != null && type == repairDef.type()) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onRepairVehicle(player);
        }
        // 军需官荣誉: AMMO_SUPPLY(64)或 REVIVE(65)加分项触发时挂钩(需 Conquest 或 GD656FrontVoice)
        org.mods.gd656killicon.common.bonus.BonusDefinition ammoDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("AMMO_SUPPLY");
        org.mods.gd656killicon.common.bonus.BonusDefinition reviveDef =
                org.mods.gd656killicon.common.bonus.BonusRegistry.get("REVIVE");
        if ((ammoDef != null && type == ammoDef.type()) || (reviveDef != null && type == reviveDef.type())) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onQuartermaster(player);
        }
        if (!ServerData.get().isBonusEnabled(type)) return;
        
        double multiplier = ServerData.get().getBonusMultiplier(type);
        if (multiplier <= 0) return;

        float score = (float) (scale * multiplier);
        if (score <= 0) return;

        score = applyScoreLimits(type, score);

        pending.computeIfAbsent(player.getUUID(), k -> Collections.synchronizedList(new ArrayList<>()))
            .add(new Entry(type, score, extra == null ? "" : extra, victimId, victimName, scale));
    }

    /**
     * Processes pending bonuses and sends packets to players.
     * Runs every 2 ticks to batch updates.
     */
    public void tick(MinecraftServer server) {
        if (server.getTickCount() % 2 != 0 || pending.isEmpty()) return;

        Iterator<Map.Entry<UUID, List<Entry>>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<Entry>> mapEntry = it.next();
            UUID playerId = mapEntry.getKey();
            List<Entry> list = mapEntry.getValue();
            
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            
            if (player == null) {
                it.remove();
                continue;
            }

            processPlayerBonuses(player, list);
        }
    }

    private void processPlayerBonuses(ServerPlayer player, List<Entry> list) {
        synchronized (list) {
            if (list.isEmpty()) return;

            Map<String, Entry> merged = new LinkedHashMap<>();
            for (Entry e : list) {
                BonusDefinition def = BonusRegistry.get(e.type);
                String key = (def != null && def.mergeBehavior() == MergeBehavior.BY_COMBO) ? "COMBO" : (e.type + "|" + e.extra);
                merged.merge(key, e, (old, val) -> new Entry(
                    old.type, 
                    old.score + val.score, 
                    val.extra, 
                    old.victimId != -1 ? old.victimId : val.victimId,
                    old.victimName != null ? old.victimName : val.victimName,
                    old.scale + val.scale
                ));
            }

            List<Entry> ordered = new ArrayList<>(merged.values());
            ordered.sort((a, b) -> {
                boolean aPriority = isPriorityKillBonus(a.type);
                boolean bPriority = isPriorityKillBonus(b.type);
                return aPriority == bPriority ? 0 : (aPriority ? 1 : -1);
            });
            for (Entry e : ordered) {
                float score = applyScoreLimits(e.type, e.score);
                NetworkHandler.sendToPlayer(new BonusScorePacket(e.type, score, e.extra, e.victimId, e.victimName), player);
                // 炮兵荣誉: 累计收到的载具伤害加分项(HIT_VEHICLE_ARMOR)分数(单次生命累计 ≥ 800)
                org.mods.gd656killicon.common.bonus.BonusDefinition armorDef = BonusRegistry.get("HIT_VEHICLE_ARMOR");
                if (armorDef != null && e.type == armorDef.type() && score > 0) {
                    org.mods.gd656killicon.server.ServerCore.HONOR.onArtilleryHit(player, score);
                }
                // 地面控制/突破者(突袭模式): 累计阶段内占领据点加分项分数
                if (isCaptureBonusType(e.type) && score > 0) {
                    org.mods.gd656killicon.server.ServerCore.HONOR.onConquestCaptureScore(player, score);
                }
                // 地面控制/突破者(突破模式): 记录本阶段安放/拆除炸弹加分项
                org.mods.gd656killicon.common.bonus.BonusDefinition plantedDef = BonusRegistry.get("RUSH_BOMB_PLANTED");
                if (plantedDef != null && e.type == plantedDef.type()) {
                    org.mods.gd656killicon.server.ServerCore.HONOR.onRushBombPlanted(player);
                }
                org.mods.gd656killicon.common.bonus.BonusDefinition defusedDef = BonusRegistry.get("RUSH_BOMB_DEFUSED");
                if (defusedDef != null && e.type == defusedDef.type()) {
                    org.mods.gd656killicon.server.ServerCore.HONOR.onRushBombDefused(player);
                }
                // 救援加分(conquest 触发, 带被救援者 victimId) → 发救援 kill_feed, 带加分项表达式与附加数据
                org.mods.gd656killicon.common.bonus.BonusDefinition reviveDef = BonusRegistry.get("REVIVE");
                if (reviveDef != null && e.type == reviveDef.type() && e.victimId != -1) {
                    float multiplier = (float) ServerData.get().getBonusMultiplier(e.type);
                    NetworkHandler.sendToPlayer(new KillIconPacket(
                            "subtitle", "kill_feed", KillType.RESCUE, 0, e.victimId, 0, false,
                            e.victimName != null ? e.victimName : "", true, false, 0.0f, multiplier, e.scale), player);
                }
                ServerData.get().addScore(player, score);
            }
            list.clear();
        }
    }

    private boolean isPriorityKillBonus(int type) {
        BonusDefinition def = BonusRegistry.get(type);
        return def != null && def.priorityKill();
    }

    /** 计算加分实际分数(含加分项上限与全局上限), 供 kill_feed 的 <score> 直带使用。 */
    public static float resolveScore(int type, float rawScore) {
        float limited = rawScore;
        BonusDefinition def = BonusRegistry.get(type);
        if (def != null && def.scoreCap() > 0 && limited > def.scoreCap()) {
            limited = def.scoreCap();
        }
        int max = ServerData.get().getScoreMaxLimit();
        if (limited > max) {
            limited = max;
        }
        return limited;
    }

    private float applyScoreLimits(int type, float score) {
        return resolveScore(type, score);
    }

    /** 是否为占领据点加分项(CONQUEST_CAPTURE_*): 地面控制/突破者(突袭模式)的阶段分数累计源。 */
    private static boolean isCaptureBonusType(int type) {
        org.mods.gd656killicon.common.bonus.BonusDefinition progressDef = BonusRegistry.get("CONQUEST_CAPTURE_PROGRESS");
        org.mods.gd656killicon.common.bonus.BonusDefinition neutralizeDef = BonusRegistry.get("CONQUEST_CAPTURE_NEUTRALIZE");
        org.mods.gd656killicon.common.bonus.BonusDefinition controlDef = BonusRegistry.get("CONQUEST_CAPTURE_CONTROL");
        return (progressDef != null && type == progressDef.type())
                || (neutralizeDef != null && type == neutralizeDef.type())
                || (controlDef != null && type == controlDef.type());
    }
}
