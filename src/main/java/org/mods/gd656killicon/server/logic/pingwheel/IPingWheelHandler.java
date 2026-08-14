package org.mods.gd656killicon.server.logic.pingwheel;

public interface IPingWheelHandler {
    void init();
    default void tick() {}
}
