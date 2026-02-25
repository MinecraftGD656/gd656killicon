package org.mods.gd656killicon.client.util;

import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.stats.ClientStatsManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * ACE (腾讯ACE反作弊) Lag Simulator
 * 
 * Simulates disk scanning lag or system freezes during intense combat scenarios.
 * Designed to be annoying but not constantly debilitating.
 */
public class AceLagSimulator {
    private static final Random RANDOM = new Random();
    private static final Deque<Long> killTimestamps = new ArrayDeque<>();
    private static final int MAX_HISTORY = 10;
    
    // Cooldown to prevent back-to-back major freezes
    private static long lastLagTime = 0;
    private static final long MIN_LAG_INTERVAL = 2000; // 2 seconds minimum between lag spikes

    /**
     * Called when a kill event occurs.
     * Evaluates the current combat intensity and potentially triggers a lag spike.
     */
    public static void onKillEvent() {
        if (!ClientConfigManager.isEnableAceLag()) {
            return;
        }

        long now = System.currentTimeMillis();
        
        // Update history
        killTimestamps.addLast(now);
        while (killTimestamps.size() > MAX_HISTORY) {
            killTimestamps.removeFirst();
        }
        
        // Remove old entries (> 10 seconds ago)
        while (!killTimestamps.isEmpty() && now - killTimestamps.peekFirst() > 10000) {
            killTimestamps.removeFirst();
        }

        // Calculate intensity metrics
        int recentKills = killTimestamps.size();
        long streak = ClientStatsManager.getCurrentStreak();
        
        // Calculate "Combat Intensity Score" (0.0 to 1.0+)
        // - Streak contributes: +0.05 per streak count (capped at 0.5)
        // - Recent kills density: +0.1 per kill in last 10s
        double streakScore = Math.min(streak * 0.05, 0.5);
        double densityScore = recentKills * 0.1;
        double intensity = streakScore + densityScore;

        // Get config intensity (1-10, default 5)
        int configIntensity = ClientConfigManager.getAceLagIntensity();
        // Factor: 0.2 (at 1) to 2.0 (at 10), default 1.0 (at 5)
        double intensityFactor = configIntensity / 5.0;

        // Base probability starts low, increases with intensity and config setting
        // If intensity is 0.5 and factor is 1.0 -> chance 0.1
        // If intensity is 1.0 and factor is 2.0 -> chance 0.4
        double triggerChance = Math.min(intensity * 0.2 * intensityFactor, 0.8);

        if (RANDOM.nextDouble() < triggerChance) {
            triggerLag(intensity, intensityFactor);
        }
    }

    private static void triggerLag(double intensity, double intensityFactor) {
        long now = System.currentTimeMillis();
        
        // Cooldown decreases slightly with higher intensity (down to 1s at max)
        long currentCooldown = (long) (MIN_LAG_INTERVAL / Math.max(1.0, intensityFactor * 0.8));
        
        if (now - lastLagTime < currentCooldown) {
            return;
        }

        // Determine lag type based on intensity and randomness
        // Types:
        // 1. Micro Stutter (20-50ms) - Common
        // 2. Frame Drop (80-150ms) - Medium
        // 3. System Freeze (300-800ms) - Rare, requires high intensity

        double roll = RANDOM.nextDouble();
        long sleepTime = 0;

        // Adjust probability thresholds based on intensityFactor
        // Higher factor -> higher chance of severe lag
        if (intensity > 0.8 && roll < (0.1 * intensityFactor)) {
            // Major Freeze
            // 300ms to 800ms base
            sleepTime = 300 + RANDOM.nextInt(500);
        } else if (intensity > 0.4 && roll < (0.3 * intensityFactor)) {
            // Frame Drop
            // 80ms to 150ms base
            sleepTime = 80 + RANDOM.nextInt(70);
        } else {
            // Micro Stutter
            // 20ms to 50ms base
            sleepTime = 20 + RANDOM.nextInt(30);
        }

        // Apply intensity factor to duration
        // e.g. at max intensity (2.0x), 800ms becomes 1600ms
        sleepTime = (long) (sleepTime * intensityFactor);
        
        // Hard cap at 2.5s to prevent complete timeout/disconnect
        sleepTime = Math.min(sleepTime, 2500);

        try {
            // Simulate "Scan Disk" or GC pause by sleeping on the main thread
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
            // Ignore
        }
        
        lastLagTime = System.currentTimeMillis();
    }
}
