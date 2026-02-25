package org.mods.gd656killicon.server.logic.spotting;

public interface ISpottingHandler {
    void init();
    default void tick() {}
}
