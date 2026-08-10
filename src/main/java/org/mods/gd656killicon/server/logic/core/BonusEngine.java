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
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.packet.KillIconPacket;

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
                // 救援加分(conquest 触发, 带被救援者 victimId) → 直接发救援 kill_feed, scoreOverride 携带实际分数
                org.mods.gd656killicon.common.bonus.BonusDefinition reviveDef = BonusRegistry.get("REVIVE");
                if (reviveDef != null && e.type == reviveDef.type() && e.victimId != -1) {
                    NetworkHandler.sendToPlayer(new KillIconPacket(
                            "subtitle", "kill_feed", KillType.RESCUE, 0, e.victimId, 0, false,
                            e.victimName != null ? e.victimName : "", true, false, 0.0f, score), player);
                }
                ServerData.get().addScore(player, score);
            }
            list.clear();
        }
    }

    private boolean isPriorityKillBonus(int type) {
        BonusDefinition def = BonusRegistry.get(type);
        return def != null && def.priorityKill();
    }

    /** 计算加分实际分数(含加分项上限与全局上限), 供 kill_feed 的 <score> 直带使用。 */
    public static float resolveScore(int type, float rawScore) {
        float limited = rawScore;
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

    private float applyScoreLimits(int type, float score) {
        return resolveScore(type, score);
    }
}
