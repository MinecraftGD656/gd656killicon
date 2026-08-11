package org.mods.gd656killicon.server.logic.conquest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.logic.core.SpottingRewardTracker;
import org.mods.gd656killicon.server.logic.spotting.ISpottingHandler;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Method;

/**
 * GD656Conquest 索敌标记接入: 监听 ConquestMarkerSpotPlacedEvent(手动索敌 DIRECT_ENEMY 放置成功),
 * 记录标记者与目标 → 30s 内目标被同队队友击杀时触发标示助攻(SPOT_ASSIST)。
 */
public class ConquestSpottingEventHandler implements ISpottingHandler {
    private static final String CONQUEST_SPOT_EVENT_CLASS =
            "org.mods.gd656conquest.server.room.runtime.event.ConquestMarkerSpotPlacedEvent";

    private final SpottingRewardTracker rewardTracker = new SpottingRewardTracker(false);

    @Override
    public void init() {
        ServerBridge.loader().registerForgeEventBusSubscriber(this);
        boolean ok = ServerBridge.loader().registerForgeDynamicListener(CONQUEST_SPOT_EVENT_CLASS, this::onMarkerSpotPlaced);
        ServerLog.info(ok
                ? "GD656Conquest marker spot event listener registered."
                : "Failed to register GD656Conquest marker spot event listener.");
    }

    @Override
    public void tick() {
        rewardTracker.tick();
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        rewardTracker.handleTargetKilled(event.getEntity(), resolveKiller(event));
    }

    /** conquest 索敌标记放置成功 → 记录标记者与目标(30s 标示助攻窗口); 斥候计数在 recordSpot 统一处理 */
    private void onMarkerSpotPlaced(Object event) {
        ServerPlayer spotter = getSpotter(event);
        ServerPlayer target = getTargetPlayer(event);
        if (spotter == null || target == null) {
            return;
        }
        rewardTracker.recordSpot(spotter, target);
    }

    private ServerPlayer getSpotter(Object event) {
        return invokePlayerGetter(event, "getSpotter");
    }

    private ServerPlayer getTargetPlayer(Object event) {
        return invokePlayerGetter(event, "getTargetPlayer");
    }

    private ServerPlayer invokePlayerGetter(Object event, String methodName) {
        try {
            Method method = event.getClass().getMethod(methodName);
            Object result = method.invoke(event);
            return result instanceof ServerPlayer player ? player : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ServerPlayer resolveKiller(LivingDeathEvent event) {
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer player) {
            return player;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof ServerPlayer player) {
                return player;
            }
        }
        return null;
    }
}
