package org.mods.gd656killicon.server.logic.conquest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Method;
import java.util.UUID;

public final class ConquestRuntimeStatsAdapter {
    private static final String BRIDGE_CLASS_NAME = "org.mods.gd656conquest.server.integration.KilliconScoreboardBridge";
    private static Method routeStatDeltaMethod;
    private static Method shouldBlockGlobalStatDeltaMethod;
    private static boolean initialized;

    private ConquestRuntimeStatsAdapter() {
    }

    public static boolean routeStatDelta(
        ServerPlayer player,
        float scoreDelta,
        int killDelta,
        int deathDelta,
        int assistDelta,
        int reviveDelta
    ) {
        if (player == null || player.server == null) {
            return false;
        }
        if (scoreDelta == 0.0F && killDelta == 0 && deathDelta == 0 && assistDelta == 0 && reviveDelta == 0) {
            return false;
        }
        if (!isConquestRuntimeAvailable(player.server)) {
            return false;
        }
        try {
            ensureInitialized();
            return routeStatDeltaMethod != null && Boolean.TRUE.equals(routeStatDeltaMethod.invoke(
                null,
                player.server,
                player.getUUID(),
                scoreDelta,
                killDelta,
                deathDelta,
                assistDelta,
                reviveDelta
            ));
        } catch (Exception exception) {
            ServerLog.error("Failed to route conquest runtime stats: %s", exception.getMessage());
            return false;
        }
    }

    public static boolean shouldBlockGlobalStatDelta(ServerPlayer player) {
        if (player == null || player.server == null || !isConquestRuntimeAvailable(player.server)) {
            return false;
        }
        try {
            ensureInitialized();
            return shouldBlockGlobalStatDeltaMethod != null && Boolean.TRUE.equals(
                shouldBlockGlobalStatDeltaMethod.invoke(null, player.server, player.getUUID())
            );
        } catch (Exception exception) {
            ServerLog.error("Failed to query conquest stat block state: %s", exception.getMessage());
            return false;
        }
    }

    private static boolean isConquestRuntimeAvailable(MinecraftServer server) {
        return server != null && ServerBridge.loader().isModLoaded("gd656conquest");
    }

    private static void ensureInitialized() throws Exception {
        if (initialized) {
            return;
        }
        Class<?> bridgeClass = Class.forName(BRIDGE_CLASS_NAME);
        routeStatDeltaMethod = bridgeClass.getMethod(
            "routeStatDelta",
            MinecraftServer.class,
            UUID.class,
            float.class,
            int.class,
            int.class,
            int.class,
            int.class
        );
        shouldBlockGlobalStatDeltaMethod = bridgeClass.getMethod(
            "shouldBlockGlobalStatDelta",
            MinecraftServer.class,
            UUID.class
        );
        initialized = true;
    }
}
