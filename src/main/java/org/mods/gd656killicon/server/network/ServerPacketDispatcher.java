package org.mods.gd656killicon.server.network;

import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.DamageSoundPacket;
import org.mods.gd656killicon.network.packet.DeathPacket;
import org.mods.gd656killicon.network.packet.KillDistancePacket;
import org.mods.gd656killicon.network.packet.KillIconPacket;

import java.util.function.Supplier;

public final class ServerPacketDispatcher {
    private ServerPacketDispatcher() {}

    private static void dispatch(ServerPlayer player, ServerPacketType type, Supplier<Object> packetFactory) {
        if (player == null) {
            return;
        }
        NetworkHandler.sendToPlayer(packetFactory.get(), player);
    }

    public static void sendDamageSound(ServerPlayer player, boolean headshotDamage) {
        dispatch(player, ServerPacketType.DAMAGE_SOUND, () -> new DamageSoundPacket(headshotDamage));
    }

    public static void sendDeath(ServerPlayer player, String playerName, String deathCause, String killerName) {
        dispatch(player, ServerPacketType.PLAYER_DEATH, () -> new DeathPacket(playerName, deathCause, killerName));
    }

    public static void sendKillDistance(ServerPlayer player, double distance) {
        dispatch(player, ServerPacketType.KILL_DISTANCE, () -> new KillDistancePacket(distance));
    }

    public static void sendKillEffects(ServerPlayer player, int killType, int combo, int victimId, double comboWindowSeconds, boolean hasHelmet, String victimName, boolean isVictimPlayer, float distance) {
        boolean recordStats = killType != KillType.ASSIST && killType != KillType.DESTROY_VEHICLE;

        dispatch(player, ServerPacketType.KILL_ICON_SCROLLING, () -> new KillIconPacket("kill_icon", "scrolling", killType, combo, victimId, comboWindowSeconds, hasHelmet, victimName, isVictimPlayer, recordStats));
        dispatch(player, ServerPacketType.KILL_ICON_VALORANT, () -> new KillIconPacket("kill_icon", "valorant", killType, combo, victimId, comboWindowSeconds, hasHelmet, victimName, isVictimPlayer, false));

        if (combo > 0) {
            dispatch(player, ServerPacketType.KILL_ICON_COMBO, () -> new KillIconPacket("kill_icon", "combo", killType, combo, victimId, comboWindowSeconds, hasHelmet, victimName, isVictimPlayer, false));
        }
        dispatch(player, ServerPacketType.KILL_ICON_CARD, () -> new KillIconPacket("kill_icon", "card", killType, combo, victimId, comboWindowSeconds, hasHelmet, victimName, isVictimPlayer, false));
        dispatch(player, ServerPacketType.KILL_ICON_CARD_BAR, () -> new KillIconPacket("kill_icon", "card_bar", killType, combo, victimId, comboWindowSeconds, hasHelmet, victimName, isVictimPlayer, false));
        dispatch(player, ServerPacketType.KILL_ICON_BATTLEFIELD1, () -> new KillIconPacket("kill_icon", "battlefield1", killType, combo, victimId, comboWindowSeconds, hasHelmet, victimName, isVictimPlayer, false));

        dispatch(player, ServerPacketType.SUBTITLE_KILL_FEED, () -> new KillIconPacket("subtitle", "kill_feed", killType, combo, victimId, comboWindowSeconds, hasHelmet, victimName, isVictimPlayer, false, distance));
        if ((combo > 0 || killType == KillType.ASSIST) && killType != KillType.DESTROY_VEHICLE) {
            dispatch(player, ServerPacketType.SUBTITLE_COMBO, () -> new KillIconPacket("subtitle", "combo", killType, combo, victimId, comboWindowSeconds, hasHelmet, victimName, isVictimPlayer, false));
        }
    }
}
