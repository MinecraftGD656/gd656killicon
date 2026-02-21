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
import java.util.HashMap;
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
                if (forceReset || !Files.exists(targetPath)) {
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

    public static void playSound(String name) {
        playSound(name, false);
    }

    public static void playSound(String name, boolean blocking) {
        if (!ClientConfigManager.isEnableSound()) return;

        if (blocking && BLOCKING_STATUS.getOrDefault(name, false)) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastPlayTime = SOUND_LAST_PLAY_TIMES.getOrDefault(name, 0L);
        if (now - lastPlayTime < SOUND_COOLDOWN) return;
        SOUND_LAST_PLAY_TIMES.put(name, now);

        SoundData data = SOUND_CACHE.get(name);
        if (data == null) {
            // Try looking for default suffix if exact match failed (compatibility)
            if (!name.endsWith("_cf") && !name.endsWith("_df")) {
                // Try finding keys that start with name
                for (String key : SOUND_CACHE.keySet()) {
                    if (key.startsWith(name)) {
                        data = SOUND_CACHE.get(key);
                        break;
                    }
                }
            }
            if (data == null) return;
        }

        final SoundData finalData = data;
        if (blocking) {
            BLOCKING_STATUS.put(name, true);
        }
        
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
                    
                    // Convert linear volume (0.0 - 1.0) to decibels
                    // Formula: 20 * log10(volume)
                    // But we should clamp low values to avoid -Infinity
                    if (masterVolume <= 0.0001f) {
                        masterVolume = 0.0001f;
                    }
                    float dB = (float) (20.0 * Math.log10(masterVolume));
                    // Clamp to min/max of control
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
            }
        });
    }

    private static class SoundData {
        final byte[] pcmData;
        final AudioFormat format;

        public SoundData(byte[] pcmData, AudioFormat format) {
            this.pcmData = pcmData;
            this.format = format;
        }
    }
}
