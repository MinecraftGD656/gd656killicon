package org.mods.gd656killicon.server.logic.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.bonus.BonusDefinition;
import org.mods.gd656killicon.common.bonus.BonusRegistry;
import org.mods.gd656killicon.common.bonus.MergeBehavior;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.BonusScorePacket;
import org.mods.gd656killicon.server.data.ServerData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BonusEngine {
    private record Entry(int type, float score, String extra, int victimId, String victimName) {}

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
                BonusDefinition def = BonusRegistry.get(e.type);
                String key = (def != null && def.mergeBehavior() == MergeBehavior.BY_COMBO) ? "COMBO" : (e.type + "|" + e.extra);
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
        BonusDefinition def = BonusRegistry.get(type);
        return def != null && def.priorityKill();
    }

    private float applyScoreLimits(int type, float score) {
        float limited = score;
        BonusDefinition def = BonusRegistry.get(type);
        if (def != null && def.scoreCap() > 0 && limited > def.scoreCap()) {
            limited = def.scoreCap();
        }
        int max = ServerData.get().getScoreMaxLimit();
        if (limited > max) {
            limited = max;
        }
        return limited;
    }
}
