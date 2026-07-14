package org.mods.gd656killicon.server.logic.conquest;

public interface IConquestBattlefieldHandler {
    void init();
    default void tick() {}
}
