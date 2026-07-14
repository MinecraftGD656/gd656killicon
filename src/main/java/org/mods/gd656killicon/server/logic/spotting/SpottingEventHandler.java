package org.mods.gd656killicon.server.logic.spotting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.logic.core.SpottingRewardTracker;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Method;

public class SpottingEventHandler implements ISpottingHandler {
    private final SpottingRewardTracker rewardTracker = new SpottingRewardTracker();

    @Override
    public void init() {
        ServerBridge.loader().registerForgeEventBusSubscriber(this);
        registerSpottingPostListener();
        ServerLog.info("Spotting event handler registered.");
    }

    @Override
    public void tick() {
        rewardTracker.tick();
    }

    @Override
    public void onLivingDeath(LivingEntity victim, LivingEntity killerEntity) {
        rewardTracker.handleTargetKilled(victim, killerEntity);
    }

    private void registerSpottingPostListener() {
        boolean ok = ServerBridge.loader().registerForgeDynamicListener(
                "committee.nova.spotting.common.event.impl.SpottingEvent$Post",
                this::onSpottingPost
        );
        if (!ok) {
            ServerLog.error("Failed to register Spotting event listener.");
        }
    }

    private void onSpottingPost(Object event) {
        ServerPlayer spotter = getSpotter(event);
        LivingEntity spottee = getSpottee(event);
        rewardTracker.recordSpot(spotter, spottee);
    }

    private ServerPlayer getSpotter(Object event) {
        try {
            Method method = event.getClass().getMethod("getSpotter");
            Object result = method.invoke(event);
            return result instanceof ServerPlayer spotter ? spotter : null;
        } catch (Exception e) {
            return null;
        }
    }

    private LivingEntity getSpottee(Object event) {
        try {
            Method method = event.getClass().getMethod("getSpottee");
            Object result = method.invoke(event);
            return result instanceof LivingEntity spottee ? spottee : null;
        } catch (Exception e) {
            return null;
        }
    }

}
