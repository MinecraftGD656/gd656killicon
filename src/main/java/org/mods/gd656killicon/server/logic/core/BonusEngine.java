package org.mods.gd656killicon.server.logic.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.BonusScorePacket;
import org.mods.gd656killicon.server.data.ServerData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BonusEngine {
    private record Entry(int type, float score, String extra, int victimId, String victimName) {}
    private static final float VALUE_TARGET_DESTROYED_MAX_SCORE = 2000.0f;

    /**
     * Map of player UUID to a list of pending bonus entries.
     * Uses ConcurrentHashMap and synchronized lists for thread safety.
     */
    private final Map<UUID, List<Entry>> pending = new ConcurrentHashMap<>();

    public void add(ServerPlayer player, int type, float scale, String extra) {
        add(player, type, scale, extra, -1, null);
    }

    public void add(ServerPlayer player, int type, float scale, String extra, int victimId) {
        add(player, type, scale, extra, victimId, null);
    }

    /**
     * Adds a bonus entry for a player.
     */
    public void add(ServerPlayer player, int type, float scale, String extra, int victimId, String victimName) {
        if (!ServerData.get().isBonusEnabled(type)) return;
        
        double multiplier = ServerData.get().getBonusMultiplier(type);
        if (multiplier <= 0) return;

        float score = (float) (scale * multiplier);
        if (score <= 0) return;

        score = applyScoreLimits(type, score);

        pending.computeIfAbsent(player.getUUID(), k -> Collections.synchronizedList(new ArrayList<>()))
            .add(new Entry(type, score, extra == null ? "" : extra, victimId, victimName));
    }

    /**
     * Processes pending bonuses and sends packets to players.
     * Runs every 2 ticks to batch updates.
     */
    public void tick(MinecraftServer server) {
        if (server.getTickCount() % 2 != 0 || pending.isEmpty()) return;

        Iterator<Map.Entry<UUID, List<Entry>>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<Entry>> mapEntry = it.next();
            UUID playerId = mapEntry.getKey();
            List<Entry> list = mapEntry.getValue();
            
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            
            if (player == null) {
                it.remove();
                continue;
            }

            processPlayerBonuses(player, list);
        }
    }

    private void processPlayerBonuses(ServerPlayer player, List<Entry> list) {
        synchronized (list) {
            if (list.isEmpty()) return;

            Map<String, Entry> merged = new LinkedHashMap<>();
            for (Entry e : list) {
                String key = (e.type == BonusType.KILL_COMBO) ? "COMBO" : (e.type + "|" + e.extra);
                merged.merge(key, e, (old, val) -> new Entry(
                    old.type, 
                    old.score + val.score, 
                    val.extra, 
                    old.victimId != -1 ? old.victimId : val.victimId,
                    old.victimName != null ? old.victimName : val.victimName
                ));
            }

            List<Entry> ordered = new ArrayList<>(merged.values());
            ordered.sort((a, b) -> {
                boolean aPriority = isPriorityKillBonus(a.type);
                boolean bPriority = isPriorityKillBonus(b.type);
                return aPriority == bPriority ? 0 : (aPriority ? 1 : -1);
            });
            for (Entry e : ordered) {
                float score = applyScoreLimits(e.type, e.score);
                NetworkHandler.sendToPlayer(new BonusScorePacket(e.type, score, e.extra, e.victimId, e.victimName), player);
                ServerData.get().addScore(player, score);
            }
            list.clear();
        }
    }

    private boolean isPriorityKillBonus(int type) {
        return type == BonusType.KILL
            || type == BonusType.KILL_HEADSHOT
            || type == BonusType.KILL_EXPLOSION
            || type == BonusType.KILL_CRIT
            || type == BonusType.KILL_LONG_DISTANCE
            || type == BonusType.KILL_COMBO;
    }

    private float applyScoreLimits(int type, float score) {
        float limited = score;
        if (type == BonusType.VALUE_TARGET_DESTROYED && limited > VALUE_TARGET_DESTROYED_MAX_SCORE) {
            limited = VALUE_TARGET_DESTROYED_MAX_SCORE;
        }
        int max = ServerData.get().getScoreMaxLimit();
        if (limited > max) {
            limited = max;
        }
        return limited;
    }
}
