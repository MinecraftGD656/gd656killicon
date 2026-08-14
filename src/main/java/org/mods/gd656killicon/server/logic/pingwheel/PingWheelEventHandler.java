package org.mods.gd656killicon.server.logic.pingwheel;

import org.mods.gd656killicon.server.util.ServerLog;

/**
 * Ping Wheel 集成（Fabric 版暂不接入）。
 * 可选前置 ping-wheel 不在本次移植支持范围内，事件全部由 Dummy 兜底。
 */
public class PingWheelEventHandler implements IPingWheelHandler {
    @Override
    public void init() {
        ServerLog.info("Ping Wheel integration is not supported on Fabric; using dummy handler.");
    }

    @Override
    public void tick() {
    }
}
