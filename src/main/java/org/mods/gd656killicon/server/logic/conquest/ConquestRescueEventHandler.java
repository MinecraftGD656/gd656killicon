package org.mods.gd656killicon.server.logic.conquest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.network.ServerPacketDispatcher;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Method;

/**
 * GD656Conquest 救援接入: 监听 ConquestPlayerRevivedEvent(救援成功),
 * 给救援者发送救援 kill_feed(RESCUE)。救援加分由 conquest 自行发放(带被救援者 victimId,
 * 客户端据此关联 +<score> 占位符)。
 */
public class ConquestRescueEventHandler {
    private static final String CONQUEST_REVIVED_EVENT_CLASS =
            "org.mods.gd656conquest.server.room.runtime.event.ConquestPlayerRevivedEvent";

    private boolean listenerRegistered = false;

    public void init() {
        if (listenerRegistered) {
            return;
        }
        listenerRegistered = true;
        ServerBridge.loader().registerForgeEventBusSubscriber(this);
        boolean ok = ServerBridge.loader().registerForgeDynamicListener(CONQUEST_REVIVED_EVENT_CLASS, this::onPlayerRevived);
        ServerLog.info(ok
                ? "GD656Conquest player revived event listener registered."
                : "Failed to register GD656Conquest player revived event listener.");
    }

    /** conquest 救援成功 → 给救援者发救援 kill_feed */
    private void onPlayerRevived(Object event) {
        ServerPlayer reviver = invokePlayerGetter(event, "getReviver");
        ServerPlayer target = invokePlayerGetter(event, "getTargetPlayer");
        if (reviver == null || target == null) {
            return;
        }
        ServerPacketDispatcher.sendRescueEffects(reviver, target);
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
}
