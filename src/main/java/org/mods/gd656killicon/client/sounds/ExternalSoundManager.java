package org.mods.gd656killicon.client.sounds;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private static final String SOUND_SELECTION_FILE = "sound_selection.json";
    private static final String CUSTOM_SOUND_LABELS_FILE = "custom_sound_labels.json";
    private static final String CUSTOM_SOUND_PREFIX = "custom_";
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
    private static final Map<String, Map<String, String>> SOUND_SELECTIONS = new HashMap<>();
    private static final Map<String, Map<String, String>> SOUND_LABELS = new HashMap<>();
    private static final Map<String, Set<String>> PENDING_CUSTOM_SOUNDS = new HashMap<>();
    private static final Set<String> PENDING_SOUND_RESETS = new HashSet<>();
    private static Map<String, Map<String, String>> TEMP_SOUND_SELECTIONS = null;
    private static Map<String, Map<String, String>> TEMP_SOUND_LABELS = null;
    private static boolean isEditing = false;

    public static final String SLOT_COMMON_SCORE = "common_score";
    public static final String SLOT_COMMON_HIT = "common_hit";
    public static final String SLOT_SCROLLING_DEFAULT = "scrolling_default";
    public static final String SLOT_SCROLLING_HEADSHOT = "scrolling_headshot";
    public static final String SLOT_SCROLLING_EXPLOSION = "scrolling_explosion";
    public static final String SLOT_SCROLLING_CRIT = "scrolling_crit";
    public static final String SLOT_SCROLLING_ASSIST = "scrolling_assist";
    public static final String SLOT_SCROLLING_VEHICLE = "scrolling_vehicle";
    public static final String SLOT_BF1_DEFAULT = "bf1_default";
    public static final String SLOT_BF1_HEADSHOT = "bf1_headshot";
    public static final String SLOT_CARD_DEFAULT = "card_default";
    public static final String SLOT_CARD_HEADSHOT = "card_headshot";
    public static final String SLOT_CARD_EXPLOSION = "card_explosion";
    public static final String SLOT_CARD_CRIT = "card_crit";
    public static final String SLOT_CARD_ARMOR_HEADSHOT = "card_armor_headshot";
    public static final String SLOT_COMBO_1 = "combo_1";
    public static final String SLOT_COMBO_2 = "combo_2";
    public static final String SLOT_COMBO_3 = "combo_3";
    public static final String SLOT_COMBO_4 = "combo_4";
    public static final String SLOT_COMBO_5 = "combo_5";
    public static final String SLOT_COMBO_6 = "combo_6";
    private static final List<String> SOUND_SLOT_IDS = Arrays.asList(
        SLOT_COMMON_SCORE,
        SLOT_COMMON_HIT,
        SLOT_SCROLLING_DEFAULT,
        SLOT_SCROLLING_HEADSHOT,
        SLOT_SCROLLING_EXPLOSION,
        SLOT_SCROLLING_CRIT,
        SLOT_SCROLLING_ASSIST,
        SLOT_SCROLLING_VEHICLE,
        SLOT_BF1_DEFAULT,
        SLOT_BF1_HEADSHOT,
        SLOT_CARD_DEFAULT,
        SLOT_CARD_HEADSHOT,
        SLOT_CARD_EXPLOSION,
        SLOT_CARD_CRIT,
        SLOT_CARD_ARMOR_HEADSHOT,
        SLOT_COMBO_1,
        SLOT_COMBO_2,
        SLOT_COMBO_3,
        SLOT_COMBO_4,
        SLOT_COMBO_5,
        SLOT_COMBO_6
    );

    public static void init() {
        ensureAllPresetsSoundFiles(false);
        loadSoundSelections();
        loadSoundLabels();
        reload();
    }

    public static void startEditing() {
        if (isEditing) {
            return;
        }
        TEMP_SOUND_SELECTIONS = deepCopyStringMap(SOUND_SELECTIONS);
        TEMP_SOUND_LABELS = deepCopyStringMap(SOUND_LABELS);
        isEditing = true;
    }

    public static void saveChanges() {
        if (isEditing) {
            SOUND_SELECTIONS.clear();
            SOUND_SELECTIONS.putAll(TEMP_SOUND_SELECTIONS);
            SOUND_LABELS.clear();
            SOUND_LABELS.putAll(TEMP_SOUND_LABELS);
            TEMP_SOUND_SELECTIONS = null;
            TEMP_SOUND_LABELS = null;
            isEditing = false;
        }
        applyPendingSoundResets();
        saveSoundSelections();
        saveSoundLabels();
        confirmPendingSoundReplacements();
        clearPendingCustomSounds();
    }

    public static void discardChanges() {
        if (isEditing) {
            TEMP_SOUND_SELECTIONS = null;
            TEMP_SOUND_LABELS = null;
            isEditing = false;
        }
        PENDING_SOUND_RESETS.clear();
        revertPendingSoundReplacements();
        revertPendingCustomSounds();
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

    public static List<String> getOfficialSoundFileNames() {
        return Arrays.asList(DEFAULT_SOUNDS);
    }

    public static List<String> getCustomSoundBaseNames(String presetId) {
        if (presetId == null) {
            return Collections.emptyList();
        }
        if (PENDING_SOUND_RESETS.contains(presetId)) {
            return Collections.emptyList();
        }
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        if (!Files.exists(presetDir)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        try {
            Files.list(presetDir).forEach(path -> {
                String name = path.getFileName().toString();
                if (name.startsWith(CUSTOM_SOUND_PREFIX) && (name.endsWith(".ogg") || name.endsWith(".wav"))) {
                    result.add(resolveBaseName(name));
                }
            });
        } catch (IOException ignored) {
        }
        Collections.sort(result);
        return result;
    }

    public static Map<String, String> getCustomSoundLabels(String presetId) {
        ensureSoundLabelsLoaded(presetId);
        return getActiveSoundLabels().getOrDefault(presetId, new HashMap<>());
    }

    public static String getSoundDisplayName(String presetId, String baseName) {
        if (baseName == null) {
            return "";
        }
        if (baseName.startsWith(CUSTOM_SOUND_PREFIX)) {
            Map<String, String> labels = getCustomSoundLabels(presetId);
            return labels.getOrDefault(baseName, baseName);
        }
        for (String soundName : DEFAULT_SOUNDS) {
            if (resolveBaseName(soundName).equals(baseName)) {
                return soundName;
            }
        }
        return baseName + ".ogg";
    }

    public static String getSelectedSoundBaseName(String presetId, String slotId) {
        ensureSoundSelectionsLoaded(presetId);
        String selected = getActiveSoundSelections().getOrDefault(presetId, new HashMap<>()).get(slotId);
        if (selected == null || selected.isEmpty()) {
            return getDefaultSoundBaseName(presetId, slotId);
        }
        return selected;
    }

    public static boolean isSoundSelectionModified(String presetId, String slotId) {
        String selected = getSelectedSoundBaseName(presetId, slotId);
        String def = getDefaultSoundBaseName(presetId, slotId);
        return def != null && !def.equals(selected);
    }

    public static void setSoundSelection(String presetId, String slotId, String baseName) {
        if (presetId == null || slotId == null || baseName == null) {
            return;
        }
        ensureSoundSelectionsLoaded(presetId);
        Map<String, String> selections = getActiveSoundSelections().computeIfAbsent(presetId, k -> new HashMap<>());
        selections.put(slotId, baseName);
        refreshSoundCache(presetId, baseName);
        if (!isEditing) {
            saveSoundSelections();
        }
    }

    public static void resetSoundSelectionToDefault(String presetId, String slotId) {
        String def = getDefaultSoundBaseName(presetId, slotId);
        if (def != null) {
            setSoundSelection(presetId, slotId, def);
        }
    }

    public static void markPendingSoundReset(String presetId) {
        if (presetId == null) {
            return;
        }
        ensureSoundSelectionsLoaded(presetId);
        ensureSoundLabelsLoaded(presetId);
        PENDING_SOUND_RESETS.add(presetId);
        for (String soundName : DEFAULT_SOUNDS) {
            resetSoundWithBackup(presetId, soundName);
        }
        Map<String, Map<String, String>> selections = getActiveSoundSelections();
        Map<String, String> defaults = new HashMap<>();
        for (String slotId : SOUND_SLOT_IDS) {
            String def = getDefaultSoundBaseName(presetId, slotId);
            if (def != null) {
                defaults.put(slotId, def);
            }
        }
        selections.put(presetId, defaults);
        getActiveSoundLabels().remove(presetId);
    }

    public static void playConfiguredSound(String presetId, String slotId) {
        playConfiguredSound(presetId, slotId, false);
    }

    public static void playConfiguredSound(String presetId, String slotId, boolean blocking) {
        String baseName = getSelectedSoundBaseName(presetId, slotId);
        if (baseName == null || baseName.isEmpty()) {
            return;
        }
        playSound(baseName, blocking);
    }

    public static String createCustomSoundFromFile(String presetId, Path sourcePath, String originalName) {
        if (presetId == null || sourcePath == null) {
            return null;
        }
        String lower = sourcePath.toString().toLowerCase();
        String sourceExt = lower.endsWith(".ogg") ? ".ogg" : (lower.endsWith(".wav") ? ".wav" : null);
        if (sourceExt == null) {
            return null;
        }
        ensureSoundFilesForPreset(presetId, false);
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        int index = 1;
        String baseName;
        Path targetPath;
        do {
            baseName = CUSTOM_SOUND_PREFIX + index;
            targetPath = presetDir.resolve(baseName + sourceExt);
            index++;
        } while (Files.exists(targetPath));
        try {
            if (!Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            if (isEditing) {
                markPendingCustomSound(presetId, targetPath.getFileName().toString());
            }
            updateCustomSoundLabel(presetId, baseName, originalName);
            refreshSoundCache(presetId, baseName);
            return baseName;
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.sound.replace_fail", presetId, sourcePath.toString(), e.getMessage());
            return null;
        }
    }

    public static void deleteCustomSound(String presetId, String baseName) {
        if (presetId == null || baseName == null) {
            return;
        }
        ensureSoundSelectionsLoaded(presetId);
        String fileNameOgg = baseName + ".ogg";
        String fileNameWav = baseName + ".wav";
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
        Path oggPath = presetDir.resolve(fileNameOgg);
        Path wavPath = presetDir.resolve(fileNameWav);
        Map<String, SoundBackup> presetBackups = PENDING_SOUND_BACKUPS.computeIfAbsent(presetId, k -> new HashMap<>());
        Set<String> pending = PENDING_CUSTOM_SOUNDS.get(presetId);
        try {
            if (pending == null || (!pending.contains(fileNameOgg) && !pending.contains(fileNameWav))) {
                backupFileIfNeeded(presetBackups, oggPath);
                backupFileIfNeeded(presetBackups, wavPath);
            }
            Files.deleteIfExists(oggPath);
            Files.deleteIfExists(wavPath);
            removeCustomSoundLabel(presetId, baseName);
            refreshSoundCache(presetId, baseName);
            if (pending != null && (pending.contains(fileNameOgg) || pending.contains(fileNameWav))) {
                pending.add(fileNameOgg);
                pending.add(fileNameWav);
            } else if (pending != null) {
                pending.remove(fileNameOgg);
                pending.remove(fileNameWav);
            }
            Map<String, String> selections = getActiveSoundSelections().get(presetId);
            if (selections != null) {
                for (Map.Entry<String, String> entry : new HashMap<>(selections).entrySet()) {
                    if (baseName.equals(entry.getValue())) {
                        String def = getDefaultSoundBaseName(presetId, entry.getKey());
                        if (def != null) {
                            selections.put(entry.getKey(), def);
                        }
                    }
                }
            }
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.sound.revert_fail", presetId, baseName, e.getMessage());
        }
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
        if (hasPendingSoundSelectionChanges()) {
            return true;
        }
        if (hasPendingSoundLabelChanges()) {
            return true;
        }
        if (!PENDING_CUSTOM_SOUNDS.isEmpty()) {
            return true;
        }
        if (!PENDING_SOUND_RESETS.isEmpty()) {
            return true;
        }
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

    private static void ensureSoundSelectionsLoaded(String presetId) {
        if (presetId == null) {
            return;
        }
        if (getActiveSoundSelections().containsKey(presetId)) {
            return;
        }
        Map<String, String> selections = new HashMap<>();
        Path path = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds").resolve(SOUND_SELECTION_FILE);
        if (Files.exists(path)) {
            try {
                String content = Files.readString(path);
                JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
                for (String key : obj.keySet()) {
                    selections.put(key, obj.get(key).getAsString());
                }
            } catch (Exception ignored) {
            }
        }
        getActiveSoundSelections().put(presetId, selections);
    }

    private static void ensureSoundLabelsLoaded(String presetId) {
        if (presetId == null) {
            return;
        }
        if (getActiveSoundLabels().containsKey(presetId)) {
            return;
        }
        Map<String, String> labels = new HashMap<>();
        Path path = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds").resolve(CUSTOM_SOUND_LABELS_FILE);
        if (Files.exists(path)) {
            try {
                String content = Files.readString(path);
                JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
                for (String key : obj.keySet()) {
                    labels.put(key, obj.get(key).getAsString());
                }
            } catch (Exception ignored) {
            }
        }
        getActiveSoundLabels().put(presetId, labels);
    }

    private static Map<String, Map<String, String>> getActiveSoundSelections() {
        return isEditing && TEMP_SOUND_SELECTIONS != null ? TEMP_SOUND_SELECTIONS : SOUND_SELECTIONS;
    }

    private static Map<String, Map<String, String>> getActiveSoundLabels() {
        return isEditing && TEMP_SOUND_LABELS != null ? TEMP_SOUND_LABELS : SOUND_LABELS;
    }

    private static void saveSoundSelections() {
        for (Map.Entry<String, Map<String, String>> entry : SOUND_SELECTIONS.entrySet()) {
            String presetId = entry.getKey();
            Map<String, String> selections = entry.getValue();
            Path path = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds").resolve(SOUND_SELECTION_FILE);
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, String> sel : selections.entrySet()) {
                obj.addProperty(sel.getKey(), sel.getValue());
            }
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, obj.toString());
            } catch (IOException ignored) {
            }
        }
    }

    private static void saveSoundLabels() {
        for (Map.Entry<String, Map<String, String>> entry : SOUND_LABELS.entrySet()) {
            String presetId = entry.getKey();
            Map<String, String> labels = entry.getValue();
            Path path = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds").resolve(CUSTOM_SOUND_LABELS_FILE);
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, String> label : labels.entrySet()) {
                obj.addProperty(label.getKey(), label.getValue());
            }
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, obj.toString());
            } catch (IOException ignored) {
            }
        }
    }

    private static void applyPendingSoundResets() {
        if (PENDING_SOUND_RESETS.isEmpty()) {
            return;
        }
        Set<String> pending = new HashSet<>(PENDING_SOUND_RESETS);
        PENDING_SOUND_RESETS.clear();
        for (String presetId : pending) {
            Map<String, String> defaults = new HashMap<>();
            for (String slotId : SOUND_SLOT_IDS) {
                String def = getDefaultSoundBaseName(presetId, slotId);
                if (def != null) {
                    defaults.put(slotId, def);
                }
            }
            SOUND_SELECTIONS.put(presetId, defaults);
            SOUND_LABELS.remove(presetId);
            try {
                Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds");
                if (Files.exists(presetDir)) {
                    try (java.util.stream.Stream<Path> paths = Files.list(presetDir)) {
                        paths.forEach(path -> {
                            String name = path.getFileName().toString();
                            if (name.startsWith(CUSTOM_SOUND_PREFIX) && (name.endsWith(".ogg") || name.endsWith(".wav"))) {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }
                            }
                        });
                    }
                }
                Files.deleteIfExists(presetDir.resolve(CUSTOM_SOUND_LABELS_FILE));
                ensureSoundFilesForPreset(presetId, true);
            } catch (IOException ignored) {
            }
            clearPendingSoundReplacementsForPreset(presetId);
            PENDING_CUSTOM_SOUNDS.remove(presetId);
            if (presetId.equals(ConfigManager.getCurrentPresetId())) {
                reloadAsync();
            }
        }
    }

    private static void loadSoundSelections() {
        SOUND_SELECTIONS.clear();
    }

    private static void loadSoundLabels() {
        SOUND_LABELS.clear();
    }

    private static Map<String, Map<String, String>> deepCopyStringMap(Map<String, Map<String, String>> source) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
            result.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return result;
    }

    private static boolean hasPendingSoundSelectionChanges() {
        if (!isEditing || TEMP_SOUND_SELECTIONS == null) {
            return false;
        }
        if (TEMP_SOUND_SELECTIONS.size() != SOUND_SELECTIONS.size()) {
            return true;
        }
        for (Map.Entry<String, Map<String, String>> entry : SOUND_SELECTIONS.entrySet()) {
            Map<String, String> temp = TEMP_SOUND_SELECTIONS.get(entry.getKey());
            if (temp == null || !temp.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPendingSoundLabelChanges() {
        if (!isEditing || TEMP_SOUND_LABELS == null) {
            return false;
        }
        if (TEMP_SOUND_LABELS.size() != SOUND_LABELS.size()) {
            return true;
        }
        for (Map.Entry<String, Map<String, String>> entry : SOUND_LABELS.entrySet()) {
            Map<String, String> temp = TEMP_SOUND_LABELS.get(entry.getKey());
            if (temp == null || !temp.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static void updateCustomSoundLabel(String presetId, String baseName, String originalName) {
        if (presetId == null || baseName == null) {
            return;
        }
        ensureSoundLabelsLoaded(presetId);
        String label = originalName == null ? baseName : originalName;
        getActiveSoundLabels().computeIfAbsent(presetId, k -> new HashMap<>()).put(baseName, label);
        if (!isEditing) {
            saveSoundLabels();
        }
    }

    private static void removeCustomSoundLabel(String presetId, String baseName) {
        if (presetId == null || baseName == null) {
            return;
        }
        ensureSoundLabelsLoaded(presetId);
        Map<String, String> labels = getActiveSoundLabels().get(presetId);
        if (labels != null) {
            labels.remove(baseName);
            if (!isEditing) {
                saveSoundLabels();
            }
        }
    }

    private static void markPendingCustomSound(String presetId, String fileName) {
        if (presetId == null || fileName == null) {
            return;
        }
        PENDING_CUSTOM_SOUNDS.computeIfAbsent(presetId, k -> new HashSet<>()).add(fileName);
    }

    private static void clearPendingCustomSounds() {
        PENDING_CUSTOM_SOUNDS.clear();
    }

    private static void revertPendingCustomSounds() {
        for (Map.Entry<String, Set<String>> entry : PENDING_CUSTOM_SOUNDS.entrySet()) {
            String presetId = entry.getKey();
            for (String fileName : entry.getValue()) {
                Path path = CONFIG_ASSETS_DIR.resolve(presetId).resolve("sounds").resolve(fileName);
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            }
        }
        PENDING_CUSTOM_SOUNDS.clear();
    }

    public static String getDefaultSoundBaseName(String presetId, String slotId) {
        if (slotId == null) {
            return null;
        }
        boolean isBf5 = "00007".equals(presetId);
        return switch (slotId) {
            case SLOT_COMMON_SCORE -> "addscore_df";
            case SLOT_COMMON_HIT -> "hitsound_df";
            case SLOT_SCROLLING_DEFAULT -> isBf5 ? "killsound_bf5" : "killsound_df";
            case SLOT_SCROLLING_HEADSHOT -> isBf5 ? "headshotkillsound_bf5" : "headshotkillsound_df";
            case SLOT_SCROLLING_EXPLOSION -> isBf5 ? "headshotkillsound_bf5" : "explosionkillsound_df";
            case SLOT_SCROLLING_CRIT -> isBf5 ? "killsound_bf5" : "critkillsound_df";
            case SLOT_SCROLLING_ASSIST -> "defaulticonsound_df";
            case SLOT_SCROLLING_VEHICLE -> isBf5 ? "vehiclekillsound_bf5" : "explosionkillsound_df";
            case SLOT_BF1_DEFAULT -> "killsound_bf1";
            case SLOT_BF1_HEADSHOT -> "headshotkillsound_bf1";
            case SLOT_CARD_DEFAULT -> "cardkillsound_default_cs";
            case SLOT_CARD_HEADSHOT -> "cardkillsound_headshot_cs";
            case SLOT_CARD_EXPLOSION -> "cardkillsound_explosion_cs";
            case SLOT_CARD_CRIT -> "cardkillsound_crit_cs";
            case SLOT_CARD_ARMOR_HEADSHOT -> "cardkillsound_armorheadshot_cs";
            case SLOT_COMBO_1 -> "combokillsound_1_cf";
            case SLOT_COMBO_2 -> "combokillsound_2_cf";
            case SLOT_COMBO_3 -> "combokillsound_3_cf";
            case SLOT_COMBO_4 -> "combokillsound_4_cf";
            case SLOT_COMBO_5 -> "combokillsound_5_cf";
            case SLOT_COMBO_6 -> "combokillsound_6_cf";
            default -> null;
        };
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
