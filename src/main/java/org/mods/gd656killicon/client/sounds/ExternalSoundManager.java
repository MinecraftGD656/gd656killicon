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
    private static final Path COMMON_SOUNDS_DIR = CONFIG_ASSETS_DIR.resolve("common").resolve("sounds");
    private static final Map<String, SoundData> SOUND_CACHE = new HashMap<>();
    private static final Map<String, Long> SOUND_LAST_PLAY_TIMES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> BLOCKING_STATUS = new ConcurrentHashMap<>();
    private static final ExecutorService SOUND_THREAD_POOL = Executors.newCachedThreadPool();
    private static final long SOUND_COOLDOWN = 10L; 
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
        "killsound_bf5.ogg",
        "headshotkillsound_bf5.ogg",
        "vehiclekillsound_bf5.ogg",
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
                ensureAllPresetsSoundFiles(false);
                
                clearCache();

                String currentPresetId = ConfigManager.getCurrentPresetId();
                int finalLoadedCount = loadSoundsForPreset(currentPresetId);

                ClientMessageLogger.info("Async sound reload complete for preset %s: %d loaded.", currentPresetId, finalLoadedCount);
            } catch (Exception e) {
                ClientMessageLogger.error("Async sound reload failed: %s", e.getMessage());
            }
        });
    }

    public static void clearCache() {
        SOUND_CACHE.clear();
    }

    private static void ensureAllPresetsSoundFiles(boolean forceReset) {
        ensureCommonSoundFiles(forceReset);
        Set<String> presets = ConfigManager.getPresetIds();
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
                    Files.deleteIfExists(targetPath);
                    Files.deleteIfExists(wavPath);
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
        Path commonOgg = COMMON_SOUNDS_DIR.resolve(baseName + ".ogg");
        if (Files.exists(commonOgg)) {
            return "OGG";
        }
        Path commonWav = COMMON_SOUNDS_DIR.resolve(baseName + ".wav");
        if (Files.exists(commonWav)) {
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
            Files.deleteIfExists(oggPath);
            Files.deleteIfExists(wavPath);
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
        String baseName = resolveBaseName(soundName);
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        Path oggPath = presetDir.resolve(baseName + ".ogg");
        Path wavPath = presetDir.resolve(baseName + ".wav");
        Path currentPath = Files.exists(oggPath) ? oggPath : (Files.exists(wavPath) ? wavPath : null);
        if (currentPath == null) {
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

        if (isSoundModified(presetId, soundName)) {
            try {
                Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
                Path targetPath = presetDir.resolve(soundName);
                String baseName = resolveBaseName(soundName);
                
                Files.deleteIfExists(presetDir.resolve(baseName + ".ogg"));
                Files.deleteIfExists(presetDir.resolve(baseName + ".wav"));
                
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
        ensureCommonSoundFiles(false);
        ensureSoundFilesForPreset(presetId, true);
        ClientMessageLogger.chatSuccess("gd656killicon.client.sound.reset_success", presetId);

        if (presetId.equals(ConfigManager.getCurrentPresetId())) {
            reload();
        }
    }

    public static void resetSoundsAsync(String presetId) {
        SOUND_THREAD_POOL.submit(() -> {
            try {
                ensureCommonSoundFiles(false);
                ensureSoundFilesForPreset(presetId, true);
                
                ClientMessageLogger.info("Async sound reset complete for preset %s.", presetId);

                if (presetId.equals(ConfigManager.getCurrentPresetId())) {
                    reloadAsync();
                }
            } catch (Exception e) {
                ClientMessageLogger.error("Async sound reset failed for preset %s: %s", presetId, e.getMessage());
            }
        });
    }

    public static void resetAllSounds() {
        ensureCommonSoundFiles(true);
        Set<String> presets = ConfigManager.getPresetIds();
        for (String presetId : presets) {
            resetSounds(presetId);
        }
    }

    public static void resetAllSoundsAsync() {
        SOUND_THREAD_POOL.submit(() -> {
            try {
                ensureCommonSoundFiles(true);
                Set<String> presets = ConfigManager.getPresetIds();
                
                for (String presetId : presets) {
                    ensureSoundFilesForPreset(presetId, true);
                }
                
                ClientMessageLogger.info("Async sound reset complete for all presets.");
                
                reloadAsync();
            } catch (Exception e) {
                ClientMessageLogger.error("Async sound reset failed: %s", e.getMessage());
            }
        });
    }

    private static int loadSoundsForPreset(String presetId) {
        return loadSoundsForPreset(presetId, null);
    }

    private static int loadSoundsForPreset(String presetId, Consumer<Integer> progressCallback) {
        int count = 0;
        count += loadSoundsFromDirectory(COMMON_SOUNDS_DIR, false, progressCallback, count);
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        count += loadSoundsFromDirectory(presetDir, true, progressCallback, count);
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

                byte[] pcmBytes = new byte[pcm.capacity() * 2];
                for (int i = 0; i < pcm.capacity(); i++) {
                    short sample = pcm.get(i);
                    pcmBytes[i * 2] = (byte) (sample & 0xFF);
                    pcmBytes[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                }

                AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
                return new SoundData(pcmBytes, format);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (vorbisBuffer != null) {
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
            if (!name.endsWith("_cf") && !name.endsWith("_df")) {
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
        
        long frameLength = finalData.pcmData.length / finalData.format.getFrameSize();
        double durationInSeconds = frameLength / finalData.format.getFrameRate();
        long durationMs = (long) (durationInSeconds * 1000);
        SOUND_DURATIONS_MS.put(name, durationMs); 
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
                
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
                    masterVolume *= Math.max(0.0f, soundVolume / 100.0f);
                    
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
        Path commonOgg = COMMON_SOUNDS_DIR.resolve(baseName + ".ogg");
        if (Files.exists(commonOgg)) {
            return commonOgg;
        }
        Path commonWav = COMMON_SOUNDS_DIR.resolve(baseName + ".wav");
        if (Files.exists(commonWav)) {
            return commonWav;
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

    private static void ensureCommonSoundFiles(boolean forceReset) {
        try {
            if (!Files.exists(COMMON_SOUNDS_DIR)) {
                Files.createDirectories(COMMON_SOUNDS_DIR);
            }
            for (String soundName : DEFAULT_SOUNDS) {
                String baseName = resolveBaseName(soundName);
                Path targetPath = COMMON_SOUNDS_DIR.resolve(baseName + ".ogg");
                if (forceReset || !Files.exists(targetPath)) {
                    ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "sounds/" + soundName);
                    try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(resourceLocation).get().open()) {
                        Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
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
            ClientMessageLogger.error("gd656killicon.client.sound.init_fail", "common", e.getMessage());
        }
    }

    private static int loadSoundsFromDirectory(Path directory, boolean override, Consumer<Integer> progressCallback, int currentCount) {
        if (!Files.exists(directory)) {
            return 0;
        }
        int count = 0;
        try (var stream = Files.list(directory)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isRegularFile(path)) {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".ogg") || fileName.endsWith(".wav")) {
                        try {
                            String key = fileName.replaceFirst("[.][^.]+$", "");
                            if (!override && SOUND_CACHE.containsKey(key)) {
                                continue;
                            }
                            SoundData data = loadSoundFile(path);
                            if (data != null) {
                                SOUND_CACHE.put(key, data);
                                count++;
                                if (progressCallback != null) {
                                    progressCallback.accept(currentCount + count);
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
