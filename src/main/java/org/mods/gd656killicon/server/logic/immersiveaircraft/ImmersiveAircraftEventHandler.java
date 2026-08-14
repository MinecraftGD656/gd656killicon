package org.mods.gd656killicon.server.logic.immersiveaircraft;

import org.mods.gd656killicon.server.util.ServerLog;

/**
 * ImmersiveAircraft 集成（Fabric 版暂不接入）。
 * 可选前置 immersive_aircraft 不在本次移植支持范围内，事件全部由 Dummy 兜底。
 */
public class ImmersiveAircraftEventHandler implements IImmersiveAircraftHandler {
    @Override
    public void init() {
        ServerLog.info("ImmersiveAircraft integration is not supported on Fabric; using dummy handler.");
    }

    @Override
    public void tick() {
    }
}
