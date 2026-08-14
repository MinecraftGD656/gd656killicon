package org.mods.gd656killicon.server.logic.conquest;

import net.minecraft.world.entity.LivingEntity;
import org.mods.gd656killicon.server.util.ServerLog;

/**
 * GD656Conquest 索敌标记集成（Fabric 版暂不接入）。
 * 可选前置 gd656conquest 不在本次移植支持范围内，事件全部由 Dummy 兜底。
 */
public class ConquestSpottingEventHandler implements org.mods.gd656killicon.server.logic.spotting.ISpottingHandler {
    @Override
    public void init() {
        ServerLog.info("GD656Conquest marker spot integration is not supported on Fabric; using dummy handler.");
    }

    @Override
    public void tick() {
    }

    @Override
    public void onLivingDeath(LivingEntity victim, LivingEntity killer) {
    }
}
