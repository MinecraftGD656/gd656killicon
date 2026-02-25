package org.mods.gd656killicon.client.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.util.ClientMessageLogger;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.stb.STBVorbis.*;

public class ExternalSoundManager {
    private static final Path CONFIG_ASSETS_DIR = FMLPaths.CONFIGDIR.get().resolve("gd656killicon/assets");
    private static final Map<String, SoundData> SOUND_CACHE = new HashMap<>();
    private static final Map<String, Long> SOUND_LAST_PLAY_TIMES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> BLOCKING_STATUS = new ConcurrentHashMap<>();
    private static final ExecutorService SOUND_THREAD_POOL = Executors.newCachedThreadPool();
    private static final long SOUND_COOLDOWN = 10L; // 0.01 seconds minimum interval per sound

    private static final String[] DEFAULT_SOUNDS = {
        "combokillsound_1_cf.ogg",
        "combokillsound_2_cf.ogg",
        "combokillsound_3_cf.ogg",
        "combokillsound_4_cf.ogg",
        "combokillsound_5_cf.ogg",
        "combokillsound_6_cf.ogg",
        "explosionkillsound_df.ogg",
        "headshotkillsound_df.ogg",
        "critkillsound_df.ogg",
        "hitsound_df.ogg",
        "killsound_df.ogg",
        "defaulticonsound_df.ogg",
        "cardkillsound_default_cs.ogg",
        "cardkillsound_headshot_cs.ogg",
        "cardkillsound_explosion_cs.ogg",
        "cardkillsound_crit_cs.ogg",
        "cardkillsound_armorheadshot_cs.ogg",
        "killsound_bf1.ogg",
        "headshotkillsound_bf1.ogg",
        "addscore_df.ogg"
    };
    private static final Set<String> DEFAULT_SOUND_SET = new HashSet<>(Arrays.asList(DEFAULT_SOUNDS));
    private static final Map<String, Map<String, SoundBackup>> PENDING_SOUND_BACKUPS = new HashMap<>();
    private static final Map<String, byte[]> DEFAULT_SOUND_BYTES = new HashMap<>();

    public static void init() {
        ensureAllPresetsSoundFiles(false);
        reload();
    }

    public static void reload() {
        ensureAllPresetsSoundFiles(false);
        clearCache();
        ClientMessageLogger.chatInfo("gd656killicon.client.sound.reloading");

        String currentPresetId = ConfigManager.getCurrentPresetId();
        int loadedCount = loadSoundsForPreset(currentPresetId);

        ClientMessageLogger.chatSuccess("gd656killicon.client.sound.reload_success", currentPresetId, loadedCount);
    }

    public static void reloadAsync() {
        SOUND_THREAD_POOL.submit(() -> {
            try {
                // Send start message on main thread
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatInfo("gd656killicon.client.sound.reload_start");
                });

                // Ensure sound files exist for all presets
                ensureAllPresetsSoundFiles(false);
                
                // Clear cache on worker thread (thread-safe operation)
                clearCache();

                // Send reloading message on main thread
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatInfo("gd656killicon.client.sound.reloading");
                });

                // Load sounds with progress tracking
                String currentPresetId = ConfigManager.getCurrentPresetId();
                final int[] totalSounds = {0};
                final int[] loadedCount = {0};
                
                // First count total sounds to load
                Path presetDir = CONFIG_ASSETS_DIR.resolve(currentPresetId).resolve("sounds");
                if (Files.exists(presetDir)) {
                    try (var stream = Files.list(presetDir)) {
                        totalSounds[0] = (int) stream
                            .filter(path -> Files.isRegularFile(path))
                            .filter(path -> {
                                String fileName = path.getFileName().toString();
                                return fileName.endsWith(".ogg") || fileName.endsWith(".wav");
                            })
                            .count();
                    } catch (IOException e) {
                        ClientMessageLogger.error("gd656killicon.client.sound.scan_fail", e.getMessage());
                    }
                }

                // Load sounds with progress callback
                int finalLoadedCount = loadSoundsForPreset(currentPresetId, (progress) -> {
                    loadedCount[0] = progress;
                    // Send progress message every 5 sounds or when complete
                    if (progress % 5 == 0 || progress == totalSounds[0]) {
                        Minecraft.getInstance().execute(() -> {
                            ClientMessageLogger.chatInfo("gd656killicon.client.sound.reload_progress", progress, totalSounds[0]);
                        });
                    }
                });

                // Send success message on main thread
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatSuccess("gd656killicon.client.sound.reload_success", currentPresetId, finalLoadedCount);
                });
            } catch (Exception e) {
                // Log any unexpected errors
                ClientMessageLogger.error("gd656killicon.client.sound.reload_error", e.getMessage());
            }
        });
    }

    public static void clearCache() {
        SOUND_CACHE.clear();
    }

    private static void ensureAllPresetsSoundFiles(boolean forceReset) {
        Set<String> presets = ConfigManager.getPresetIds();
        // Always ensure default presets exist
        ensureSoundFilesForPreset("00001", forceReset);
        ensureSoundFilesForPreset("00002", forceReset);
        
        for (String presetId : presets) {
            ensureSoundFilesForPreset(presetId, forceReset);
        }
    }

    public static void ensureSoundFilesForPreset(String presetId) {
        ensureSoundFilesForPreset(presetId, false);
    }

    public static void ensureSoundFilesForPreset(String presetId, boolean forceReset) {
        try {
            Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
            if (!Files.exists(presetDir)) {
                Files.createDirectories(presetDir);
            }

            for (String soundName : DEFAULT_SOUNDS) {
                Path targetPath = presetDir.resolve(soundName);
                String baseName = resolveBaseName(soundName);
                Path wavPath = presetDir.resolve(baseName + ".wav");
                if (forceReset) {
                    Files.deleteIfExists(wavPath);
                }
                if (forceReset || (!Files.exists(targetPath) && !Files.exists(wavPath))) {
                    // Try to copy from JAR using ResourceLocation
                    ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "sounds/" + soundName);
                    try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(resourceLocation).get().open()) {
                        Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        // Fallback to class resource stream if ResourceManager fails (e.g. dev environment sometimes)
                        try (InputStream stream = ExternalSoundManager.class.getResourceAsStream("/assets/gd656killicon/sounds/" + soundName)) {
                             if (stream != null) {
                                 Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                             } else {
                                 ClientMessageLogger.error("gd656killicon.client.sound.extract_fail", soundName, e.getMessage());
                             }
                        } catch (Exception ex) {
                             ClientMessageLogger.error("gd656killicon.client.sound.extract_fail", soundName, ex.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.sound.init_fail", presetId, e.getMessage());
        }
    }

    public static java.util.List<String> getDefaultSoundNames() {
        return Arrays.asList(DEFAULT_SOUNDS);
    }

    public static String getSoundExtensionForPreset(String presetId, String soundName) {
        if (presetId == null || soundName == null) {
            return "OGG";
        }
        String baseName = resolveBaseName(soundName);
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        Path oggPath = presetDir.resolve(baseName + ".ogg");
        if (Files.exists(oggPath)) {
            return "OGG";
        }
        Path wavPath = presetDir.resolve(baseName + ".wav");
        if (Files.exists(wavPath)) {
            return "WAV";
        }
        return "OGG";
    }

    public static boolean replaceSoundWithBackup(String presetId, String soundName, Path sourcePath) {
        if (presetId == null || soundName == null || sourcePath == null) {
            return false;
        }
        if (!DEFAULT_SOUND_SET.contains(soundName)) {
            return false;
        }
        if (!Files.exists(sourcePath)) {
            return false;
        }
        String lower = sourcePath.toString().toLowerCase();
        String sourceExt = lower.endsWith(".ogg") ? ".ogg" : (lower.endsWith(".wav") ? ".wav" : null);
        if (sourceExt == null) {
            return false;
        }
        ensureSoundFilesForPreset(presetId, false);
        String baseName = resolveBaseName(soundName);
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        Path targetPath = presetDir.resolve(baseName + sourceExt);
        Path otherPath = presetDir.resolve(baseName + (".ogg".equals(sourceExt) ? ".wav" : ".ogg"));
        try {
            if (!Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            Map<String, SoundBackup> presetBackups = PENDING_SOUND_BACKUPS.computeIfAbsent(presetId, k -> new HashMap<>());
            backupFileIfNeeded(presetBackups, targetPath);
            backupFileIfNeeded(presetBackups, otherPath);
            Files.deleteIfExists(otherPath);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            refreshSoundCache(presetId, baseName);
            return true;
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.sound.replace_fail", presetId, soundName, e.getMessage());
            return false;
        }
    }

    public static boolean resetSoundWithBackup(String presetId, String soundName) {
        if (presetId == null || soundName == null) {
            return false;
        }
        if (!DEFAULT_SOUND_SET.contains(soundName)) {
            return false;
        }
        ensureSoundFilesForPreset(presetId, false);
        String baseName = resolveBaseName(soundName);
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        Path oggPath = presetDir.resolve(baseName + ".ogg");
        Path wavPath = presetDir.resolve(baseName + ".wav");
        try {
            if (!Files.exists(presetDir)) {
                Files.createDirectories(presetDir);
            }
            Map<String, SoundBackup> presetBackups = PENDING_SOUND_BACKUPS.computeIfAbsent(presetId, k -> new HashMap<>());
            backupFileIfNeeded(presetBackups, oggPath);
            backupFileIfNeeded(presetBackups, wavPath);
            Files.deleteIfExists(wavPath);
            ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "sounds/" + soundName);
            try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(resourceLocation).get().open()) {
                Files.copy(stream, oggPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                try (InputStream stream = ExternalSoundManager.class.getResourceAsStream("/assets/gd656killicon/sounds/" + soundName)) {
                    if (stream != null) {
                        Files.copy(stream, oggPath, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        ClientMessageLogger.error("gd656killicon.client.sound.extract_fail", soundName, e.getMessage());
                        return false;
                    }
                }
            }
            refreshSoundCache(presetId, baseName);
            return true;
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.sound.reset_error", presetId, soundName, e.getMessage());
            return false;
        }
    }

    public static void confirmPendingSoundReplacements() {
        PENDING_SOUND_BACKUPS.clear();
    }

    public static void clearPendingSoundReplacementsForPreset(String presetId) {
        if (presetId == null) {
            return;
        }
        PENDING_SOUND_BACKUPS.remove(presetId);
    }

    public static void revertPendingSoundReplacements() {
        if (PENDING_SOUND_BACKUPS.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Map<String, SoundBackup>> presetEntry : PENDING_SOUND_BACKUPS.entrySet()) {
            String presetId = presetEntry.getKey();
            for (Map.Entry<String, SoundBackup> entry : presetEntry.getValue().entrySet()) {
                String fileName = entry.getKey();
                SoundBackup backup = entry.getValue();
                Path targetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds").resolve(fileName);
                try {
                    if (backup.existed) {
                        Files.write(targetPath, backup.data);
                    } else {
                        Files.deleteIfExists(targetPath);
                    }
                    refreshSoundCache(presetId, resolveBaseName(fileName));
                } catch (IOException e) {
                    ClientMessageLogger.error("gd656killicon.client.sound.revert_fail", presetId, fileName, e.getMessage());
                }
            }
        }
        PENDING_SOUND_BACKUPS.clear();
    }

    public static void revertPendingSoundReplacementsForPreset(String presetId) {
        if (presetId == null) {
            return;
        }
        Map<String, SoundBackup> presetBackups = PENDING_SOUND_BACKUPS.get(presetId);
        if (presetBackups == null || presetBackups.isEmpty()) {
            return;
        }
        for (Map.Entry<String, SoundBackup> entry : presetBackups.entrySet()) {
            String fileName = entry.getKey();
            SoundBackup backup = entry.getValue();
            Path targetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds").resolve(fileName);
            try {
                if (backup.existed) {
                    Files.write(targetPath, backup.data);
                } else {
                    Files.deleteIfExists(targetPath);
                }
                refreshSoundCache(presetId, resolveBaseName(fileName));
            } catch (IOException e) {
                ClientMessageLogger.error("gd656killicon.client.sound.revert_fail", presetId, fileName, e.getMessage());
            }
        }
        PENDING_SOUND_BACKUPS.remove(presetId);
    }

    public static boolean hasPendingSoundChanges() {
        for (Map<String, SoundBackup> presetBackups : PENDING_SOUND_BACKUPS.values()) {
            if (presetBackups != null && !presetBackups.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSoundModified(String presetId, String soundName) {
        if (presetId == null || soundName == null) {
            return false;
        }
        if (!DEFAULT_SOUND_SET.contains(soundName)) {
            return false;
        }
        Path currentPath = getSoundPathForPreset(presetId, resolveBaseName(soundName));
        if (currentPath == null || !Files.exists(currentPath)) {
            return false;
        }
        byte[] defaultBytes = getDefaultSoundBytes(soundName);
        if (defaultBytes == null) {
            return false;
        }
        try {
            byte[] currentBytes = Files.readAllBytes(currentPath);
            return !Arrays.equals(defaultBytes, currentBytes);
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean resetSound(String presetId, String soundName) {
        if (presetId == null || soundName == null) return false;
        
        // Check pending first
        Map<String, SoundBackup> presetBackups = PENDING_SOUND_BACKUPS.get(presetId);
        if (presetBackups != null && presetBackups.containsKey(soundName)) {
            SoundBackup backup = presetBackups.remove(soundName);
            Path targetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds").resolve(soundName);
            try {
                if (backup.existed) {
                    Files.write(targetPath, backup.data);
                } else {
                    Files.deleteIfExists(targetPath);
                }
                if (presetBackups.isEmpty()) {
                    PENDING_SOUND_BACKUPS.remove(presetId);
                }
                refreshSoundCache(presetId, resolveBaseName(soundName));
                return true;
            } catch (IOException e) {
                ClientMessageLogger.error("gd656killicon.client.sound.revert_fail", presetId, soundName, e.getMessage());
                return false;
            }
        }

        // If not pending, check if modified on disk and reset to default
        if (isSoundModified(presetId, soundName)) {
            // Force extract this specific sound
            try {
                Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
                Path targetPath = presetDir.resolve(soundName);
                String baseName = resolveBaseName(soundName);
                
                // Remove both .ogg and .wav variants to be clean
                Files.deleteIfExists(presetDir.resolve(baseName + ".ogg"));
                Files.deleteIfExists(presetDir.resolve(baseName + ".wav"));
                
                // Copy default
                ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "sounds/" + soundName);
                try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(resourceLocation).get().open()) {
                    Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    try (InputStream stream = ExternalSoundManager.class.getResourceAsStream("/assets/gd656killicon/sounds/" + soundName)) {
                         if (stream != null) {
                             Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                         }
                    }
                }
                refreshSoundCache(presetId, baseName);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        
        return false;
    }

    public static void resetSounds(String presetId) {
        ensureSoundFilesForPreset(presetId, true);
        ClientMessageLogger.chatSuccess("gd656killicon.client.sound.reset_success", presetId);

        // If resetting current preset, reload cache
        if (presetId.equals(ConfigManager.getCurrentPresetId())) {
            reload();
        }
    }

    public static void resetSoundsAsync(String presetId) {
        SOUND_THREAD_POOL.submit(() -> {
            try {
                // Send start message on main thread
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatInfo("gd656killicon.client.sound.reset_start");
                });

                // Use the shared method with forceReset=true
                ensureSoundFilesForPreset(presetId, true);
                
                // Send success message on main thread
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatSuccess("gd656killicon.client.sound.reset_success", presetId);
                });

                // If resetting current preset, reload cache asynchronously
                if (presetId.equals(ConfigManager.getCurrentPresetId())) {
                    reloadAsync();
                }
            } catch (Exception e) {
                // Log any unexpected errors
                ClientMessageLogger.error("gd656killicon.client.sound.reset_error", e.getMessage());
            }
        });
    }

    public static void resetAllSounds() {
        Set<String> presets = ConfigManager.getPresetIds();
        for (String presetId : presets) {
            resetSounds(presetId);
        }
    }

    public static void resetAllSoundsAsync() {
        SOUND_THREAD_POOL.submit(() -> {
            try {
                // Send start message on main thread
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatInfo("gd656killicon.client.sound.reset_start");
                });

                Set<String> presets = ConfigManager.getPresetIds();
                int totalPresets = presets.size();
                int completedPresets = 0;
                
                for (String presetId : presets) {
                    // Call the synchronous resetSounds (which now uses ensureSoundFilesForPreset with force=true)
                    // We avoid calling resetSounds(presetId) because it logs success per preset.
                    // We just want to do the work.
                    ensureSoundFilesForPreset(presetId, true);
                    
                    completedPresets++;
                    
                    // Send progress message every preset or when complete
                    final int currentProgress = completedPresets;
                    Minecraft.getInstance().execute(() -> {
                        ClientMessageLogger.chatInfo("gd656killicon.client.sound.reset_progress", currentProgress, totalPresets);
                    });
                }
                
                // Send success message on main thread
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatSuccess("gd656killicon.client.sound.reset_success", "all presets");
                });
                
                // Reload async after all resets
                reloadAsync();
            } catch (Exception e) {
                // Log any unexpected errors
                ClientMessageLogger.error("gd656killicon.client.sound.reset_error", e.getMessage());
            }
        });
    }

    private static int loadSoundsForPreset(String presetId) {
        return loadSoundsForPreset(presetId, null);
    }

    private static int loadSoundsForPreset(String presetId, Consumer<Integer> progressCallback) {
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        if (!Files.exists(presetDir)) return 0;

        int count = 0;
        try (var stream = Files.list(presetDir)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isRegularFile(path)) {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".ogg") || fileName.endsWith(".wav")) {
                        try {
                            SoundData data = loadSoundFile(path);
                            if (data != null) {
                                // Remove extension for key
                                String key = fileName.replaceFirst("[.][^.]+$", "");
                                SOUND_CACHE.put(key, data);
                                count++;
                                if (progressCallback != null) {
                                    progressCallback.accept(count);
                                }
                            }
                        } catch (Exception e) {
                            ClientMessageLogger.error("Failed to load sound: " + fileName + " - " + e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    private static SoundData loadSoundFile(Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".wav")) {
            return loadWav(path);
        } else if (fileName.endsWith(".ogg")) {
            return loadOgg(path);
        }
        return null;
    }

    private static SoundData loadWav(Path path) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(path.toFile());
            AudioFormat format = ais.getFormat();
            
            // Convert to PCM_SIGNED if needed (Java Sound compatibility)
            // But usually WAV is already PCM. We just read all bytes.
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = ais.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return new SoundData(buffer.toByteArray(), format);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static SoundData loadOgg(Path path) {
        ByteBuffer vorbisBuffer = null;
        try {
            // Read file to ByteBuffer
            byte[] bytes = Files.readAllBytes(path);
            vorbisBuffer = BufferUtils.createByteBuffer(bytes.length);
            vorbisBuffer.put(bytes);
            vorbisBuffer.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer channelsBuffer = stack.mallocInt(1);
                IntBuffer sampleRateBuffer = stack.mallocInt(1);

                ShortBuffer pcm = stb_vorbis_decode_memory(vorbisBuffer, channelsBuffer, sampleRateBuffer);
                if (pcm == null) {
                    return null;
                }

                int channels = channelsBuffer.get(0);
                int sampleRate = sampleRateBuffer.get(0);

                // Convert ShortBuffer to byte[]
                byte[] pcmBytes = new byte[pcm.capacity() * 2];
                for (int i = 0; i < pcm.capacity(); i++) {
                    short sample = pcm.get(i);
                    pcmBytes[i * 2] = (byte) (sample & 0xFF);
                    pcmBytes[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                }

                // STB Vorbis always returns 16-bit PCM
                AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
                return new SoundData(pcmBytes, format);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (vorbisBuffer != null) {
                // BufferUtils.createByteBuffer allocates direct memory, but it's managed by GC if created via BufferUtils? 
                // Wait, BufferUtils.createByteBuffer uses Unsafe or direct allocation. 
                // LWJGL's BufferUtils.createByteBuffer returns a ByteBuffer.
                // It should be fine.
            }
        }
    }

    private static final Map<String, Long> SOUND_START_TIMES = new ConcurrentHashMap<>();
    private static final Map<String, Long> SOUND_DURATIONS_MS = new ConcurrentHashMap<>();
    private static final Map<String, java.util.concurrent.atomic.AtomicInteger> ACTIVE_PLAY_COUNTS = new ConcurrentHashMap<>();

    public static void playSound(String name) {
        playSound(name, false);
    }

    public static void playSound(String name, boolean blocking) {
        if (!ClientConfigManager.isEnableSound()) return;
        int soundVolume = ClientConfigManager.getSoundVolume();
        if (soundVolume <= 0) return;

        if (blocking && BLOCKING_STATUS.getOrDefault(name, false)) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastPlayTime = SOUND_LAST_PLAY_TIMES.getOrDefault(name, 0L);
        if (now - lastPlayTime < SOUND_COOLDOWN) return;
        SOUND_LAST_PLAY_TIMES.put(name, now);

        SoundData data = SOUND_CACHE.get(name);
        String resolvedName = name;
        if (data == null) {
            // Try looking for default suffix if exact match failed (compatibility)
            if (!name.endsWith("_cf") && !name.endsWith("_df")) {
                // Try finding keys that start with name
                for (String key : SOUND_CACHE.keySet()) {
                    if (key.startsWith(name)) {
                        data = SOUND_CACHE.get(key);
                        resolvedName = key;
                        break;
                    }
                }
            }
            if (data == null) return;
        }

        final SoundData finalData = data;
        final String finalName = resolvedName;
        
        // Calculate duration
        long frameLength = finalData.pcmData.length / finalData.format.getFrameSize();
        double durationInSeconds = frameLength / finalData.format.getFrameRate();
        long durationMs = (long) (durationInSeconds * 1000);
        SOUND_DURATIONS_MS.put(name, durationMs); // Use original name for lookup key consistency

        if (blocking) {
            BLOCKING_STATUS.put(name, true);
        }
        
        ACTIVE_PLAY_COUNTS.compute(name, (key, count) -> {
            if (count == null) {
                count = new java.util.concurrent.atomic.AtomicInteger(0);
            }
            count.incrementAndGet();
            return count;
        });
        SOUND_START_TIMES.put(name, System.currentTimeMillis());

        SOUND_THREAD_POOL.submit(() -> {
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, finalData.format);
                if (!AudioSystem.isLineSupported(info)) {
                    return;
                }

                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(finalData.format);
                
                // Volume control
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
                    masterVolume *= Math.max(0.0f, soundVolume / 100.0f);
                    
                    // Convert linear volume (0.0 - 1.0) to decibels
                    if (masterVolume <= 0.0001f) {
                        masterVolume = 0.0001f;
                    }
                    float dB = (float) (20.0 * Math.log10(masterVolume));
                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    
                    gainControl.setValue(dB);
                }

                line.start();
                line.write(finalData.pcmData, 0, finalData.pcmData.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (blocking) {
                    BLOCKING_STATUS.put(name, false);
                }
                ACTIVE_PLAY_COUNTS.computeIfPresent(name, (key, count) -> {
                    if (count.decrementAndGet() <= 0) {
                        return null;
                    }
                    return count;
                });
            }
        });
    }

    public static boolean isSoundPlaying(String name) {
        java.util.concurrent.atomic.AtomicInteger count = ACTIVE_PLAY_COUNTS.get(name);
        return count != null && count.get() > 0;
    }

    public static float getSoundProgress(String name) {
        if (!isSoundPlaying(name)) return 0.0f;
        Long start = SOUND_START_TIMES.get(name);
        Long duration = SOUND_DURATIONS_MS.get(name);
        if (start == null || duration == null || duration == 0) return 0.0f;
        
        long elapsed = System.currentTimeMillis() - start;
        float progress = (float) elapsed / duration;
        return Math.min(1.0f, Math.max(0.0f, progress));
    }

    private static void backupFileIfNeeded(Map<String, SoundBackup> presetBackups, Path path) throws IOException {
        String fileName = path.getFileName().toString();
        if (presetBackups.containsKey(fileName)) {
            return;
        }
        if (Files.exists(path)) {
            byte[] original = Files.readAllBytes(path);
            presetBackups.put(fileName, new SoundBackup(true, original));
        } else {
            presetBackups.put(fileName, new SoundBackup(false, null));
        }
    }

    private static Path getSoundPathForPreset(String presetId, String baseName) {
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        Path oggPath = presetDir.resolve(baseName + ".ogg");
        if (Files.exists(oggPath)) {
            return oggPath;
        }
        Path wavPath = presetDir.resolve(baseName + ".wav");
        if (Files.exists(wavPath)) {
            return wavPath;
        }
        return oggPath;
    }

    private static String resolveBaseName(String soundName) {
        return soundName.replaceFirst("[.][^.]+$", "");
    }

    private static byte[] getDefaultSoundBytes(String soundName) {
        if (DEFAULT_SOUND_BYTES.containsKey(soundName)) {
            return DEFAULT_SOUND_BYTES.get(soundName);
        }
        byte[] bytes = null;
        try {
            ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "sounds/" + soundName);
            try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(resourceLocation).get().open()) {
                bytes = stream.readAllBytes();
            }
        } catch (Exception e) {
            try (InputStream stream = ExternalSoundManager.class.getResourceAsStream("/assets/gd656killicon/sounds/" + soundName)) {
                if (stream != null) {
                    bytes = stream.readAllBytes();
                }
            } catch (IOException ex) {
                bytes = null;
            }
        }
        if (bytes != null) {
            DEFAULT_SOUND_BYTES.put(soundName, bytes);
        }
        return bytes;
    }

    private static void refreshSoundCache(String presetId, String baseName) {
        if (!ConfigManager.getCurrentPresetId().equals(presetId)) {
            return;
        }
        SOUND_CACHE.remove(baseName);
        Path path = getSoundPathForPreset(presetId, baseName);
        if (path != null && Files.exists(path)) {
            try {
                SoundData data = loadSoundFile(path);
                if (data != null) {
                    SOUND_CACHE.put(baseName, data);
                }
            } catch (Exception e) {
                SOUND_CACHE.remove(baseName);
            }
        }
    }

    public static SoundData getSoundData(String name) {
        return SOUND_CACHE.get(name);
    }

    private static class SoundBackup {
        private final boolean existed;
        private final byte[] data;

        private SoundBackup(boolean existed, byte[] data) {
            this.existed = existed;
            this.data = data;
        }
    }

    public static class SoundData {
        public final byte[] pcmData;
        public final AudioFormat format;

        public SoundData(byte[] pcmData, AudioFormat format) {
            this.pcmData = pcmData;
            this.format = format;
        }
    }
}
