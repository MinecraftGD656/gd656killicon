package org.mods.gd656killicon.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.util.ClientMessageLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ClientConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("gd656killicon").toFile();
    private static final File GLOBAL_CONFIG_FILE = new File(CONFIG_DIR, "client_config.json");

    private static final String DEFAULT_CURRENT_PRESET = "00001";
    private static final boolean DEFAULT_ENABLE_SOUND = true;
    private static final boolean DEFAULT_SHOW_BONUS_MESSAGE = false;
    private static final int DEFAULT_SOUND_VOLUME = 100;
    private static final boolean DEFAULT_ENABLE_ACE_LAG = false;
    private static final int DEFAULT_ACE_LAG_INTENSITY = 5;
    private static final boolean DEFAULT_DISABLE_TACZ_KILL_SOUND = false;
    private static final boolean DEFAULT_SHOW_CONFIG_INTRO = true;
    private static final boolean DEFAULT_SHOW_PRESET_INTRO = true;
    private static final boolean DEFAULT_SHOW_ELEMENT_INTRO = true;
    private static final boolean DEFAULT_SHOW_SOUND_INTRO = true;
    private static final boolean DEFAULT_SHOW_SCOREBOARD_INTRO = true;
    private static final String DEFAULT_LAST_LANGUAGE = "";

    private static String currentPresetId = DEFAULT_CURRENT_PRESET;
    private static boolean enableSound = DEFAULT_ENABLE_SOUND;
    private static boolean showBonusMessage = DEFAULT_SHOW_BONUS_MESSAGE;
    private static int soundVolume = DEFAULT_SOUND_VOLUME;
    private static boolean enableAceLag = DEFAULT_ENABLE_ACE_LAG;
    private static int aceLagIntensity = DEFAULT_ACE_LAG_INTENSITY;
    private static boolean disableTaczKillSound = DEFAULT_DISABLE_TACZ_KILL_SOUND;
    private static boolean showConfigIntro = DEFAULT_SHOW_CONFIG_INTRO;
    private static boolean showPresetIntro = DEFAULT_SHOW_PRESET_INTRO;
    private static boolean showElementIntro = DEFAULT_SHOW_ELEMENT_INTRO;
    private static boolean showSoundIntro = DEFAULT_SHOW_SOUND_INTRO;
    private static boolean showSoundSelectIntro = true;
    private static boolean showScoreboardIntro = DEFAULT_SHOW_SCOREBOARD_INTRO;
    private static String lastLanguageCode = DEFAULT_LAST_LANGUAGE;
    private static String lastModVersion = "";

    private static String tempCurrentPresetId = null;
    private static Boolean tempEnableSound = null;
    private static Boolean tempShowBonusMessage = null;
    private static Integer tempSoundVolume = null;
    private static Boolean tempEnableAceLag = null;
    private static Integer tempAceLagIntensity = null;
    private static Boolean tempDisableTaczKillSound = null;
    private static boolean isEditing = false;

    public static void startEditing() {
        tempCurrentPresetId = currentPresetId;
        tempEnableSound = enableSound;
        tempShowBonusMessage = showBonusMessage;
        tempSoundVolume = soundVolume;
        tempEnableAceLag = enableAceLag;
        tempAceLagIntensity = aceLagIntensity;
        tempDisableTaczKillSound = disableTaczKillSound;
        isEditing = true;
    }

    public static void saveChanges() {
        if (isEditing) {
            currentPresetId = tempCurrentPresetId;
            enableSound = tempEnableSound;
            showBonusMessage = tempShowBonusMessage;
            soundVolume = tempSoundVolume == null ? soundVolume : clampSoundVolume(tempSoundVolume);
            enableAceLag = tempEnableAceLag != null ? tempEnableAceLag : enableAceLag;
            aceLagIntensity = tempAceLagIntensity == null ? aceLagIntensity : clampAceLagIntensity(tempAceLagIntensity);
            disableTaczKillSound = tempDisableTaczKillSound != null ? tempDisableTaczKillSound : disableTaczKillSound;
            isEditing = false;
            saveGlobalConfig();
            
            tempCurrentPresetId = null;
            tempEnableSound = null;
            tempShowBonusMessage = null;
            tempSoundVolume = null;
            tempEnableAceLag = null;
            tempAceLagIntensity = null;
            tempDisableTaczKillSound = null;
        }
    }

    public static void discardChanges() {
        if (isEditing) {
            isEditing = false;
            tempCurrentPresetId = null;
            tempEnableSound = null;
            tempShowBonusMessage = null;
            tempSoundVolume = null;
            tempEnableAceLag = null;
            tempAceLagIntensity = null;
            tempDisableTaczKillSound = null;
        }
    }

    public static boolean hasUnsavedChanges() {
        if (!isEditing) return false;
        if (tempCurrentPresetId != null && !tempCurrentPresetId.equals(currentPresetId)) return true;
        if (tempEnableSound != null && !tempEnableSound.equals(enableSound)) return true;
        if (tempShowBonusMessage != null && !tempShowBonusMessage.equals(showBonusMessage)) return true;
        if (tempSoundVolume != null && tempSoundVolume != soundVolume) return true;
        if (tempEnableAceLag != null && !tempEnableAceLag.equals(enableAceLag)) return true;
        if (tempAceLagIntensity != null && tempAceLagIntensity != aceLagIntensity) return true;
        if (tempDisableTaczKillSound != null && !tempDisableTaczKillSound.equals(disableTaczKillSound)) return true;
        return false;
    }

    public static boolean isAceLagConfigChangedInEdit() {
        if (!isEditing) return false;
        if (tempEnableAceLag != null && !tempEnableAceLag.equals(enableAceLag)) return true;
        if (tempAceLagIntensity != null && tempAceLagIntensity != aceLagIntensity) return true;
        return false;
    }

    public static void init() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        loadGlobalConfig();
    }

    public static void loadGlobalConfig() {
        if (!GLOBAL_CONFIG_FILE.exists()) {
            createDefaultConfig();
            return;
        }

        try (FileReader reader = new FileReader(GLOBAL_CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            currentPresetId = json.has("current_preset") ? json.get("current_preset").getAsString() : DEFAULT_CURRENT_PRESET;
            enableSound = json.has("enable_sound") ? json.get("enable_sound").getAsBoolean() : DEFAULT_ENABLE_SOUND;
            showBonusMessage = json.has("show_bonus_message") ? json.get("show_bonus_message").getAsBoolean() : DEFAULT_SHOW_BONUS_MESSAGE;
            soundVolume = json.has("sound_volume") ? clampSoundVolume(json.get("sound_volume").getAsInt()) : DEFAULT_SOUND_VOLUME;
            enableAceLag = json.has("enable_ace_lag") ? json.get("enable_ace_lag").getAsBoolean() : DEFAULT_ENABLE_ACE_LAG;
            aceLagIntensity = json.has("ace_lag_intensity") ? clampAceLagIntensity(json.get("ace_lag_intensity").getAsInt()) : DEFAULT_ACE_LAG_INTENSITY;
            disableTaczKillSound = json.has("disable_tacz_kill_sound") ? json.get("disable_tacz_kill_sound").getAsBoolean() : DEFAULT_DISABLE_TACZ_KILL_SOUND;
            showConfigIntro = json.has("show_config_intro") ? json.get("show_config_intro").getAsBoolean() : DEFAULT_SHOW_CONFIG_INTRO;
            showPresetIntro = json.has("show_preset_intro") ? json.get("show_preset_intro").getAsBoolean() : DEFAULT_SHOW_PRESET_INTRO;
            showElementIntro = json.has("show_element_intro") ? json.get("show_element_intro").getAsBoolean() : DEFAULT_SHOW_ELEMENT_INTRO;
            showSoundIntro = json.has("show_sound_intro") ? json.get("show_sound_intro").getAsBoolean() : DEFAULT_SHOW_SOUND_INTRO;
            showSoundSelectIntro = json.has("show_sound_select_intro") ? json.get("show_sound_select_intro").getAsBoolean() : true;
            showScoreboardIntro = json.has("show_scoreboard_intro") ? json.get("show_scoreboard_intro").getAsBoolean() : DEFAULT_SHOW_SCOREBOARD_INTRO;
            lastLanguageCode = json.has("last_language") ? json.get("last_language").getAsString() : DEFAULT_LAST_LANGUAGE;
            lastModVersion = json.has("last_mod_version") ? json.get("last_mod_version").getAsString() : "";
        } catch (Exception e) {
            ClientMessageLogger.error("gd656killicon.client.config.load_fail", e.getMessage());
            e.printStackTrace();
            currentPresetId = DEFAULT_CURRENT_PRESET;
            enableSound = DEFAULT_ENABLE_SOUND;
            showBonusMessage = DEFAULT_SHOW_BONUS_MESSAGE;
            soundVolume = DEFAULT_SOUND_VOLUME;
            enableAceLag = DEFAULT_ENABLE_ACE_LAG;
            aceLagIntensity = DEFAULT_ACE_LAG_INTENSITY;
            disableTaczKillSound = DEFAULT_DISABLE_TACZ_KILL_SOUND;
            showConfigIntro = DEFAULT_SHOW_CONFIG_INTRO;
            showPresetIntro = DEFAULT_SHOW_PRESET_INTRO;
            showElementIntro = DEFAULT_SHOW_ELEMENT_INTRO;
            showSoundIntro = DEFAULT_SHOW_SOUND_INTRO;
            showSoundSelectIntro = true;
            showScoreboardIntro = DEFAULT_SHOW_SCOREBOARD_INTRO;
            lastLanguageCode = DEFAULT_LAST_LANGUAGE;
            lastModVersion = "";
        }
    }

    public static void createDefaultConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("current_preset", DEFAULT_CURRENT_PRESET);
        json.addProperty("enable_sound", DEFAULT_ENABLE_SOUND);
        json.addProperty("show_bonus_message", DEFAULT_SHOW_BONUS_MESSAGE);
        json.addProperty("sound_volume", DEFAULT_SOUND_VOLUME);
        json.addProperty("enable_ace_lag", DEFAULT_ENABLE_ACE_LAG);
        json.addProperty("ace_lag_intensity", DEFAULT_ACE_LAG_INTENSITY);
        json.addProperty("disable_tacz_kill_sound", DEFAULT_DISABLE_TACZ_KILL_SOUND);
        json.addProperty("show_config_intro", DEFAULT_SHOW_CONFIG_INTRO);
        json.addProperty("show_preset_intro", DEFAULT_SHOW_PRESET_INTRO);
        json.addProperty("show_element_intro", DEFAULT_SHOW_ELEMENT_INTRO);
        json.addProperty("show_sound_intro", DEFAULT_SHOW_SOUND_INTRO);
        json.addProperty("show_sound_select_intro", true);
        json.addProperty("show_scoreboard_intro", DEFAULT_SHOW_SCOREBOARD_INTRO);
        json.addProperty("last_language", DEFAULT_LAST_LANGUAGE);
        json.addProperty("last_mod_version", GuiConstants.MOD_VERSION);
        
        currentPresetId = DEFAULT_CURRENT_PRESET;
        enableSound = DEFAULT_ENABLE_SOUND;
        showBonusMessage = DEFAULT_SHOW_BONUS_MESSAGE;
        soundVolume = DEFAULT_SOUND_VOLUME;
        enableAceLag = DEFAULT_ENABLE_ACE_LAG;
        aceLagIntensity = DEFAULT_ACE_LAG_INTENSITY;
        disableTaczKillSound = DEFAULT_DISABLE_TACZ_KILL_SOUND;
        showConfigIntro = DEFAULT_SHOW_CONFIG_INTRO;
        showPresetIntro = DEFAULT_SHOW_PRESET_INTRO;
        showElementIntro = DEFAULT_SHOW_ELEMENT_INTRO;
        showSoundIntro = DEFAULT_SHOW_SOUND_INTRO;
        showSoundSelectIntro = true;
        showScoreboardIntro = DEFAULT_SHOW_SCOREBOARD_INTRO;
        lastLanguageCode = DEFAULT_LAST_LANGUAGE;
        lastModVersion = GuiConstants.MOD_VERSION;

        try (FileWriter writer = new FileWriter(GLOBAL_CONFIG_FILE)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.config.save_fail", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void saveGlobalConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("current_preset", currentPresetId);
        root.addProperty("enable_sound", enableSound);
        root.addProperty("show_bonus_message", showBonusMessage);
        root.addProperty("sound_volume", soundVolume);
        root.addProperty("enable_ace_lag", enableAceLag);
        root.addProperty("ace_lag_intensity", aceLagIntensity);
        root.addProperty("disable_tacz_kill_sound", disableTaczKillSound);
        root.addProperty("show_config_intro", showConfigIntro);
        root.addProperty("show_preset_intro", showPresetIntro);
        root.addProperty("show_element_intro", showElementIntro);
        root.addProperty("show_sound_intro", showSoundIntro);
        root.addProperty("show_sound_select_intro", showSoundSelectIntro);
        root.addProperty("show_scoreboard_intro", showScoreboardIntro);
        root.addProperty("last_language", lastLanguageCode);
        root.addProperty("last_mod_version", lastModVersion);

        try (FileWriter writer = new FileWriter(GLOBAL_CONFIG_FILE)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.config.save_fail", e.getMessage());
        }
    }

    public static void resetToDefaults() {
        createDefaultConfig();
    }

    public static boolean checkLanguageChangedAndUpdate(String currentLanguage) {
        if (currentLanguage == null || currentLanguage.isEmpty()) return false;
        if (lastLanguageCode == null || lastLanguageCode.isEmpty()) {
            lastLanguageCode = currentLanguage;
            saveGlobalConfig();
            return false;
        }
        if (!currentLanguage.equals(lastLanguageCode)) {
            lastLanguageCode = currentLanguage;
            saveGlobalConfig();
            return true;
        }
        return false;
    }

    public static String getCurrentPresetId() {
        return isEditing && tempCurrentPresetId != null ? tempCurrentPresetId : currentPresetId;
    }

    public static void setCurrentPresetId(String id) {
        if (isEditing) {
            tempCurrentPresetId = id;
        } else {
            currentPresetId = id;
            saveGlobalConfig();
        }
    }

    public static boolean isEnableSound() {
        return isEditing && tempEnableSound != null ? tempEnableSound : enableSound;
    }

    public static void setEnableSound(boolean enable) {
        if (isEditing) {
            tempEnableSound = enable;
        } else {
            enableSound = enable;
            saveGlobalConfig();
        }
    }

    public static boolean isShowBonusMessage() {
        return isEditing && tempShowBonusMessage != null ? tempShowBonusMessage : showBonusMessage;
    }

    public static void setShowBonusMessage(boolean show) {
        if (isEditing) {
            tempShowBonusMessage = show;
        } else {
            showBonusMessage = show;
            saveGlobalConfig();
        }
    }

    public static int getSoundVolume() {
        return isEditing && tempSoundVolume != null ? tempSoundVolume : soundVolume;
    }

    public static void setSoundVolume(int volume) {
        int clamped = clampSoundVolume(volume);
        if (isEditing) {
            tempSoundVolume = clamped;
        } else {
            soundVolume = clamped;
            saveGlobalConfig();
        }
    }

    public static boolean isEnableAceLag() {
        return isEditing && tempEnableAceLag != null ? tempEnableAceLag : enableAceLag;
    }

    public static void setEnableAceLag(boolean enable) {
        if (isEditing) {
            tempEnableAceLag = enable;
        } else {
            enableAceLag = enable;
            saveGlobalConfig();
        }
    }

    public static int getAceLagIntensity() {
        return isEditing && tempAceLagIntensity != null ? tempAceLagIntensity : aceLagIntensity;
    }

    public static void setAceLagIntensity(int intensity) {
        int clamped = clampAceLagIntensity(intensity);
        if (isEditing) {
            tempAceLagIntensity = clamped;
        } else {
            aceLagIntensity = clamped;
            saveGlobalConfig();
        }
    }

    public static boolean isDisableTaczKillSound() {
        return isEditing && tempDisableTaczKillSound != null ? tempDisableTaczKillSound : disableTaczKillSound;
    }

    public static void setDisableTaczKillSound(boolean disable) {
        if (isEditing) {
            tempDisableTaczKillSound = disable;
        } else {
            disableTaczKillSound = disable;
            saveGlobalConfig();
        }
    }

    public static boolean shouldShowConfigIntro() {
        return showConfigIntro;
    }

    public static void markConfigIntroShown() {
        if (showConfigIntro) {
            showConfigIntro = false;
            saveGlobalConfig();
        }
    }

    public static boolean shouldShowPresetIntro() {
        return showPresetIntro;
    }

    public static void markPresetIntroShown() {
        if (showPresetIntro) {
            showPresetIntro = false;
            saveGlobalConfig();
        }
    }

    public static boolean shouldShowElementIntro() {
        return showElementIntro;
    }

    public static void markElementIntroShown() {
        if (showElementIntro) {
            showElementIntro = false;
            saveGlobalConfig();
        }
    }

    public static boolean shouldShowSoundIntro() {
        return showSoundIntro;
    }

    public static void markSoundIntroShown() {
        if (showSoundIntro) {
            showSoundIntro = false;
            saveGlobalConfig();
        }
    }

    public static boolean shouldShowSoundSelectIntro() {
        return showSoundSelectIntro;
    }

    public static void markSoundSelectIntroShown() {
        if (showSoundSelectIntro) {
            showSoundSelectIntro = false;
            saveGlobalConfig();
        }
    }

    public static boolean shouldShowScoreboardIntro() {
        return showScoreboardIntro;
    }

    public static void markScoreboardIntroShown() {
        if (showScoreboardIntro) {
            showScoreboardIntro = false;
            saveGlobalConfig();
        }
    }

    public static void resetIntroPrompts() {
        showConfigIntro = true;
        showPresetIntro = true;
        showElementIntro = true;
        showSoundIntro = true;
        showSoundSelectIntro = true;
        showScoreboardIntro = true;
        saveGlobalConfig();
    }

    public static boolean checkModVersionChangedAndUpdate(String currentVersion) {
        if (currentVersion == null || currentVersion.isEmpty()) {
            return false;
        }
        if (lastModVersion == null || lastModVersion.isEmpty()) {
            lastModVersion = currentVersion;
            saveGlobalConfig();
            return true;
        }
        if (!currentVersion.equals(lastModVersion)) {
            lastModVersion = currentVersion;
            saveGlobalConfig();
            return true;
        }
        return false;
    }

    public static void setRecordedModVersion(String version) {
        lastModVersion = version == null ? "" : version;
        saveGlobalConfig();
    }

    private static int clampSoundVolume(int volume) {
        return Math.max(0, Math.min(200, volume));
    }

    private static int clampAceLagIntensity(int intensity) {
        return Math.max(1, Math.min(100, intensity));
    }
}
