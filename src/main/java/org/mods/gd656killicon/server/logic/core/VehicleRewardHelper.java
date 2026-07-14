package org.mods.gd656killicon.server.logic.core;

import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.network.ServerPacketDispatcher;

import java.util.Map;
import java.util.UUID;

public final class VehicleRewardHelper {
    private VehicleRewardHelper() {
    }

    public static ServerPlayer resolveRecentKiller(UUID attackerUuid, boolean attackerWasPlayer, long lastAttackTime, UUID excludedDriverUuid) {
        if (!attackerWasPlayer || attackerUuid == null) {
            return null;
        }
        if (System.currentTimeMillis() - lastAttackTime >= ServerData.get().getAssistTimeoutMs()) {
            return null;
        }
        if (ServerCore.getServer() == null) {
            return null;
        }
        ServerPlayer killer = ServerCore.getServer().getPlayerList().getPlayer(attackerUuid);
        if (killer == null) {
            return null;
        }
        if (excludedDriverUuid != null && excludedDriverUuid.equals(killer.getUUID())) {
            return null;
        }
        return killer;
    }

    public static void awardDestroyAssistBonuses(
        UUID killerUuid,
        UUID excludedDriverUuid,
        int victimId,
        String victimNameKey,
        float minimumDamage,
        Map<UUID, Float> damageByAttacker,
        Map<UUID, Long> lastContributionTimeByAttacker
    ) {
        if (!ServerData.get().isBonusEnabled(BonusType.VEHICLE_DESTROY_ASSIST)) {
            return;
        }
        if (ServerCore.getServer() == null || damageByAttacker == null || lastContributionTimeByAttacker == null) {
            return;
        }

        long now = System.currentTimeMillis();
        damageByAttacker.forEach((attackerUuid, damage) -> {
            if (attackerUuid == null || attackerUuid.equals(killerUuid) || damage == null || damage <= minimumDamage) {
                return;
            }
            Long lastContributionTime = lastContributionTimeByAttacker.get(attackerUuid);
            if (lastContributionTime == null || now - lastContributionTime > ServerData.get().getAssistTimeoutMs()) {
                return;
            }
            if (excludedDriverUuid != null && excludedDriverUuid.equals(attackerUuid)) {
                return;
            }

            ServerPlayer assister = ServerCore.getServer().getPlayerList().getPlayer(attackerUuid);
            if (assister == null) {
                return;
            }
            ServerCore.BONUS.add(assister, BonusType.VEHICLE_DESTROY_ASSIST, damage, null, victimId, victimNameKey);
        });
    }

    public static void sendDestroyVehicleEffects(ServerPlayer player, int victimId, String victimName) {
        if (player == null) {
            return;
        }
        ServerPacketDispatcher.sendKillEffects(
            player,
            KillType.DESTROY_VEHICLE,
            0,
            victimId,
            ServerData.get().getComboWindowSeconds(),
            false,
            victimName,
            false,
            0.0f
        );
    }
}
