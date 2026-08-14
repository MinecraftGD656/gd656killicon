package org.mods.gd656killicon.server.logic.conquest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Method;

/**
 * 局内临时 honor 统计通知(与 {@link ConquestRuntimeStatsAdapter} 完全平行)。
 * <p>
 * HonorEngine.deliver 触发荣誉时, 若安装了 GD656Conquest 且玩家在对局房间内,
 * 反射通知 Conquest 的 {@code KilliconScoreboardBridge.recordMatchHonor} 记录本局 honor 数据
 * (honorId → playerId → 本局次数, 新对局自动清空)。该数据不显示, 供后续 killicon 侧"最佳"图标消费。
 * 未安装 GD656Conquest 时静默跳过, killicon 保留现有全局 HonorsStatsManager 数据。
 * </p>
 */
public final class ConquestHonorAdapter {
    private static final String BRIDGE_CLASS_NAME = "org.mods.gd656conquest.server.integration.KilliconScoreboardBridge";
    private static Method recordMatchHonorMethod;
    private static boolean initialized;

    private ConquestHonorAdapter() {
    }

    /**
     * 通知 Conquest 记录一次局内 honor 触发。
     * @return 0 = 未记录(未装 conquest / 非对局 / 调用失败); 1 = 已记录, 非本局最高; 2 = 已记录, 且该玩家当前为该 honor 本局最高。
     */
    public static int recordMatchHonor(ServerPlayer player, String honorId) {
        if (player == null || player.server == null || honorId == null || honorId.isBlank()) {
            return 0;
        }
        if (!isConquestRuntimeAvailable(player.server)) {
            return 0;
        }
        try {
            ensureInitialized();
            if (recordMatchHonorMethod == null) {
                return 0;
            }
            Object result = recordMatchHonorMethod.invoke(null, player, honorId);
            return result instanceof Number number ? number.intValue() : 0;
        } catch (Exception exception) {
            ServerLog.error("Failed to route conquest match honor: %s", exception.getMessage());
            return 0;
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
        recordMatchHonorMethod = bridgeClass.getMethod("recordMatchHonor", ServerPlayer.class, String.class);
        initialized = true;
    }
}
