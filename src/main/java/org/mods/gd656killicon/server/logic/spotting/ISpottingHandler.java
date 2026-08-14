package org.mods.gd656killicon.server.logic.spotting;

import net.minecraft.world.entity.LivingEntity;

public interface ISpottingHandler {
    void init();
    default void tick() {}
    default void onLivingDeath(LivingEntity victim, LivingEntity killer) {}
}
