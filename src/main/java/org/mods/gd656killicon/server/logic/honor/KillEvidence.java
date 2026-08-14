package org.mods.gd656killicon.server.logic.honor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 击杀证据: 一次玩家击杀的归一化信息。
 * <p>
 * 由 {@link HonorEngine} 从事件链路上采集, 分型判定器只消费此对象,
 * 不直接接触 Forge 事件, 从而让新增荣誉不依赖新增监听。
 * </p>
 *
 * @param killer          击杀者(玩家)
 * @param victim          受害者实体
 * @param headshot        是否爆头击杀
 * @param victimVehicle   受害者是否为载具
 * @param victimAirVehicle 受害者是否为空中载具(依赖集成层, 未判定时 false)
 * @param killerRidingAir 击杀者是否正搭乘空中载具(依赖集成层, 未判定时 false)
 * @param killType        击杀类型(org.mods.gd656killicon.common.KillType 常量值)
 * @param avenge          是否复仇击杀(此前击杀过你的凶手, 与加分项 AVENGE 同条件)
 * @param weapon          击杀物品显示名(如 "Diamond Sword" / 改名后的自定义名; 即使 id 相同名字不同也算不同武器; 空手为空串)
 * @param victimTopScorer 受害者是否为当前最高得分者(与加分项 SLAY_THE_LEADER 同条件)
 * @param executioner     刽子手判定: 背刺(与加分项 BACKSTAB 同判定) && 距离 < 2 米 && 手持原版近战武器或 LR 战术工坊(lrtactical)武器
 * @param distance        击杀距离(米)
 * @param moving          击杀时击杀者是否处于移动中(行走或疾跑, 非静止)
 * @param victimRidingAir 受害者死亡瞬间正搭乘空中载具(载具未被摧毁, 否则乘客会脱离; 飞行调度员)
 */
public record KillEvidence(
        ServerPlayer killer,
        LivingEntity victim,
        boolean headshot,
        boolean victimVehicle,
        boolean victimAirVehicle,
        boolean killerRidingAir,
        int killType,
        boolean avenge,
        String weapon,
        boolean victimTopScorer,
        boolean executioner,
        float distance,
        boolean moving,
        boolean victimRidingAir
) {
}
