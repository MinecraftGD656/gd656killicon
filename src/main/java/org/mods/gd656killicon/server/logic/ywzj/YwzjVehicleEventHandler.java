package org.mods.gd656killicon.server.logic.ywzj;

import org.mods.gd656killicon.server.util.ServerLog;

/**
 * YWZJ 车辆集成（Fabric 版暂不接入）。
 * 可选前置 ywzj_vehicle 不在本次移植支持范围内，事件全部由 Dummy 兜底。
 */
public class YwzjVehicleEventHandler implements IYwzjVehicleHandler {
    @Override
    public void init() {
        ServerLog.info("YWZJ Vehicle integration is not supported on Fabric; using dummy handler.");
    }

    @Override
    public void tick() {
    }
}
