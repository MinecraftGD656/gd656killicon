package org.mods.gd656killicon.server.logic.core;

import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CritTracker {
    private static final long WINDOW = 1500L;
    private static final int MAX_ENTRIES = 512;
    
    /**
     * Stores recent critical hits. Victim UUID -> Attacker/Time record.
     */
    private final Map<UUID, Record> records = new LinkedHashMap<>();

    private record Record(UUID attacker, long time) {}

    /**
     * Records a potential critical hit if it matches vanilla criteria.
     */
    public void recordCrit(ServerPlayer attacker, UUID victim) {
        if (!isVanillaCrit(attacker)) return;
        records.put(victim, new Record(attacker.getUUID(), System.currentTimeMillis()));
        trim();
    }

    /**
     * Consumes a critical hit record if it exists and is within the time window.
     */
    public boolean consumeCrit(UUID attacker, UUID victim) {
        if (check(attacker, victim)) {
            records.remove(victim);
            return true;
        }
        return false;
    }

    public boolean isRecentCrit(UUID attacker, UUID victim) {
        return check(attacker, victim);
    }

    private boolean check(UUID attacker, UUID victim) {
        Record r = records.get(victim);
        if (r == null || !r.attacker.equals(attacker)) return false;
        return System.currentTimeMillis() - r.time <= WINDOW;
    }

    private void trim() {
        long now = System.currentTimeMillis();
        records.entrySet().removeIf(e -> now - e.getValue().time > WINDOW);
        
        if (records.size() > MAX_ENTRIES) {
            Iterator<UUID> it = records.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    /**
     * Checks if the player meets the vanilla Minecraft criteria for a critical hit.
     */
    private boolean isVanillaCrit(ServerPlayer p) {
        return !p.onGround() && !p.isPassenger() && !p.isInWater() && !p.isFallFlying() && 
               !p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && !p.isSprinting() && p.fallDistance > 0;
    }
}
