package org.mods.gd656killicon.client.util;

import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.stats.ClientStatsManager;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static long nextAmbientLagTime = 0;
    private static long nextDiskLagTime = 0;
    private static Path scanFilePath;
    private static boolean scanFileReady = false;
    private static final int SCAN_FILE_SIZE = 2 * 1024 * 1024;
    private static final byte[] SCAN_BUFFER = new byte[8192];
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
        double intensityFactor = Math.min(1.0, configIntensity / 100.0);

        double triggerChance = Math.min(intensity * (0.25 + intensityFactor * 0.75), 0.95);

        if (RANDOM.nextDouble() < triggerChance) {
            triggerLag(intensity, intensityFactor);
        }
    }

    public static void onClientTick() {
        if (!ClientConfigManager.isEnableAceLag()) {
            return;
        }
        int configIntensity = ClientConfigManager.getAceLagIntensity();
        double intensityFactor = Math.min(1.0, configIntensity / 100.0);
        long now = System.currentTimeMillis();
        long minInterval = (long) (300 + 1700 * (1.0 - intensityFactor));
        long maxInterval = (long) (900 + 2400 * (1.0 - intensityFactor));
        if (maxInterval < minInterval) {
            maxInterval = minInterval;
        }
        if (nextAmbientLagTime == 0) {
            nextAmbientLagTime = now + minInterval + RANDOM.nextInt((int) (maxInterval - minInterval + 1));
        }
        if (now >= nextAmbientLagTime) {
            long base = 10 + (long) (120 * intensityFactor);
            long extra = (long) (RANDOM.nextInt((int) (40 + 160 * intensityFactor)));
            long sleepTime = base + extra;
            sleepTime = Math.min(sleepTime, (long) (400 + 2600 * intensityFactor));
            sleepMs(sleepTime);
            nextAmbientLagTime = now + minInterval + RANDOM.nextInt((int) (maxInterval - minInterval + 1));
        }

        if (configIntensity > 50) {
            long minDiskInterval = (long) (700 + 2200 * (1.0 - intensityFactor));
            long maxDiskInterval = (long) (1400 + 3500 * (1.0 - intensityFactor));
            if (maxDiskInterval < minDiskInterval) {
                maxDiskInterval = minDiskInterval;
            }
            if (nextDiskLagTime == 0) {
                nextDiskLagTime = now + minDiskInterval + RANDOM.nextInt((int) (maxDiskInterval - minDiskInterval + 1));
            }
            if (now >= nextDiskLagTime) {
                long duration = (long) (80 + 400 * intensityFactor + RANDOM.nextInt((int) (120 + 480 * intensityFactor)));
                simulateDiskScan(duration);
                nextDiskLagTime = now + minDiskInterval + RANDOM.nextInt((int) (maxDiskInterval - minDiskInterval + 1));
            }
        }
    }

    private static void triggerLag(double intensity, double intensityFactor) {
        long now = System.currentTimeMillis();
        
        long currentCooldown = (long) (MIN_LAG_INTERVAL / Math.max(0.2, 0.6 + intensityFactor * 2.5));
        
        if (now - lastLagTime < currentCooldown) {
            return;
        }


        double roll = RANDOM.nextDouble();
        long sleepTime = 0;

        if (intensity > 0.8 && roll < (0.15 + intensityFactor * 0.6)) {
            sleepTime = 400 + RANDOM.nextInt(900);
        } else if (intensity > 0.4 && roll < (0.35 + intensityFactor * 0.5)) {
            sleepTime = 160 + RANDOM.nextInt(200);
        } else {
            sleepTime = 40 + RANDOM.nextInt(80);
        }

        sleepTime = (long) (sleepTime * (0.6 + intensityFactor * 2.8));
        sleepTime = Math.min(sleepTime, 5000);
        sleepMs(sleepTime);

        if (intensityFactor > 0.5) {
            long duration = (long) (120 + 500 * intensityFactor + RANDOM.nextInt((int) (160 + 500 * intensityFactor)));
            simulateDiskScan(duration);
        }
        
        lastLagTime = System.currentTimeMillis();
    }

    private static void sleepMs(long sleepTime) {
        if (sleepTime <= 0) return;
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
        }
    }

    private static void simulateDiskScan(long durationMs) {
        if (!ensureScanFile()) return;
        long end = System.currentTimeMillis() + durationMs;
        while (System.currentTimeMillis() < end) {
            try (InputStream in = Files.newInputStream(scanFilePath)) {
                while (System.currentTimeMillis() < end) {
                    int read = in.read(SCAN_BUFFER);
                    if (read < 0) break;
                }
            } catch (IOException ignored) {
                break;
            }
        }
    }

    private static boolean ensureScanFile() {
        if (scanFileReady) return true;
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("gd656killicon");
            Files.createDirectories(configDir);
            scanFilePath = configDir.resolve("ace_scan.bin");
            if (!Files.exists(scanFilePath) || Files.size(scanFilePath) < SCAN_FILE_SIZE) {
                byte[] data = new byte[SCAN_FILE_SIZE];
                RANDOM.nextBytes(data);
                Files.write(scanFilePath, data);
            }
            scanFileReady = true;
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
