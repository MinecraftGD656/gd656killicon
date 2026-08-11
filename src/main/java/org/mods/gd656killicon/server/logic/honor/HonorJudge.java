package org.mods.gd656killicon.server.logic.honor;

import org.mods.gd656killicon.common.honor.HonorDefinition;

/**
 * 荣誉判定器接口: 判定一条荣誉在给定证据下是否达成。
 * <p>
 * 判定器按 {@link org.mods.gd656killicon.common.honor.ConditionType} 分型,
 * 通过 {@link HonorJudges} 注册与分派。判定器可读/写
 * {@link PlayerHonorState}(如存活段计数), 达成时返回 true 即触发下发。
 * </p>
 */
public interface HonorJudge {

    /**
     * 判定该荣誉在当前证据下是否达成。
     *
     * @param def     荣誉定义(含条件类型与参数)
     * @param evidence 击杀证据
     * @param state   击杀者(玩家)的会话状态
     * @return true 表示达成(引擎负责下发显示包)
     */
    boolean evaluate(HonorDefinition def, KillEvidence evidence, PlayerHonorState state);
}
