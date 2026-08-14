package org.mods.gd656killicon.server.logic.spotting;

import net.minecraft.world.entity.LivingEntity;
import org.mods.gd656killicon.server.util.ServerLog;

/**
 * Spotting 集成（Fabric 版暂不接入）。
 * 可选前置 spotting 不在本次移植支持范围内，事件全部由 Dummy 兜底。
 */
public class SpottingEventHandler implements ISpottingHandler {
    @Override
    public void init() {
        ServerLog.info("Spotting integration is not supported on Fabric; using dummy handler.");
    }

    @Override
    public void tick() {
    }

    @Override
    public void onLivingDeath(LivingEntity victim, LivingEntity killer) {
    }
}
