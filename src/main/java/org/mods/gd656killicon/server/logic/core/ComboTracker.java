package org.mods.gd656killicon.server.logic.core;

import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.server.data.ServerData;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ComboTracker {
    private static final int MAX_ENTRIES = 2048;
    
    /**
     * Stores the current combo state for each player.
     * Uses LinkedHashMap as an LRU cache to limit memory usage.
     */
    private final Map<UUID, State> states = new LinkedHashMap<>();

    private record State(int count, long time) {}

    /**
     * Records a kill for a player and returns the updated combo count.
     */
    public int recordKill(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long window = ServerData.get().getComboWindowMs();
        UUID id = player.getUUID();
        
        State current = states.get(id);
        int next = (current == null || now - current.time > window) ? 1 : current.count + 1;
        
        states.put(id, new State(next, now));
        trim(now, window);
        return next;
    }

    /**
     * Removes expired entries and limits the map size.
     */
    private void trim(long now, long window) {
        states.entrySet().removeIf(e -> now - e.getValue().time > window);
        
        if (states.size() > MAX_ENTRIES) {
            Iterator<UUID> it = states.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }
}
