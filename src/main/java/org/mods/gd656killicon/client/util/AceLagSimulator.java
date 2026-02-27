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
    
    private static long lastLagTime = 0;
    private static final long MIN_LAG_INTERVAL = 2000; 
    /**
     * Called when a kill event occurs.
     * Evaluates the current combat intensity and potentially triggers a lag spike.
     */
    public static void onKillEvent() {
        if (!ClientConfigManager.isEnableAceLag()) {
            return;
        }

        long now = System.currentTimeMillis();
        
        killTimestamps.addLast(now);
        while (killTimestamps.size() > MAX_HISTORY) {
            killTimestamps.removeFirst();
        }
        
        while (!killTimestamps.isEmpty() && now - killTimestamps.peekFirst() > 10000) {
            killTimestamps.removeFirst();
        }

        int recentKills = killTimestamps.size();
        long streak = ClientStatsManager.getCurrentStreak();
        
        double streakScore = Math.min(streak * 0.05, 0.5);
        double densityScore = recentKills * 0.1;
        double intensity = streakScore + densityScore;

        int configIntensity = ClientConfigManager.getAceLagIntensity();
        double intensityFactor = configIntensity / 5.0;

        double triggerChance = Math.min(intensity * 0.2 * intensityFactor, 0.8);

        if (RANDOM.nextDouble() < triggerChance) {
            triggerLag(intensity, intensityFactor);
        }
    }

    private static void triggerLag(double intensity, double intensityFactor) {
        long now = System.currentTimeMillis();
        
        long currentCooldown = (long) (MIN_LAG_INTERVAL / Math.max(1.0, intensityFactor * 0.8));
        
        if (now - lastLagTime < currentCooldown) {
            return;
        }


        double roll = RANDOM.nextDouble();
        long sleepTime = 0;

        if (intensity > 0.8 && roll < (0.1 * intensityFactor)) {
            sleepTime = 300 + RANDOM.nextInt(500);
        } else if (intensity > 0.4 && roll < (0.3 * intensityFactor)) {
            sleepTime = 80 + RANDOM.nextInt(70);
        } else {
            sleepTime = 20 + RANDOM.nextInt(30);
        }

        sleepTime = (long) (sleepTime * intensityFactor);
        
        sleepTime = Math.min(sleepTime, 2500);

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
        }
        
        lastLagTime = System.currentTimeMillis();
    }
}
