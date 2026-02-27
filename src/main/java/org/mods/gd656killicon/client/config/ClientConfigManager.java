package org.mods.gd656killicon.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;
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
    private static final String DEFAULT_LAST_LANGUAGE = "";

    
    private static String currentPresetId = DEFAULT_CURRENT_PRESET;
    private static boolean enableSound = DEFAULT_ENABLE_SOUND;
    private static boolean showBonusMessage = DEFAULT_SHOW_BONUS_MESSAGE;
    private static int soundVolume = DEFAULT_SOUND_VOLUME;
    private static boolean enableAceLag = DEFAULT_ENABLE_ACE_LAG;
    private static int aceLagIntensity = DEFAULT_ACE_LAG_INTENSITY;
    private static String lastLanguageCode = DEFAULT_LAST_LANGUAGE;

    
    private static String tempCurrentPresetId = null;
    private static Boolean tempEnableSound = null;
    private static Boolean tempShowBonusMessage = null;
    private static Integer tempSoundVolume = null;
    private static Boolean tempEnableAceLag = null;
    private static Integer tempAceLagIntensity = null;
    private static boolean isEditing = false;

    public static void startEditing() {
        tempCurrentPresetId = currentPresetId;
        tempEnableSound = enableSound;
        tempShowBonusMessage = showBonusMessage;
        tempSoundVolume = soundVolume;
        tempEnableAceLag = enableAceLag;
        tempAceLagIntensity = aceLagIntensity;
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
            isEditing = false;
            saveGlobalConfig();
            
            tempCurrentPresetId = null;
            tempEnableSound = null;
            tempShowBonusMessage = null;
            tempSoundVolume = null;
            tempEnableAceLag = null;
            tempAceLagIntensity = null;
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
            lastLanguageCode = json.has("last_language") ? json.get("last_language").getAsString() : DEFAULT_LAST_LANGUAGE;
        } catch (Exception e) {
            ClientMessageLogger.error("gd656killicon.client.config.load_fail", e.getMessage());
            e.printStackTrace();
            
            currentPresetId = DEFAULT_CURRENT_PRESET;
            enableSound = DEFAULT_ENABLE_SOUND;
            showBonusMessage = DEFAULT_SHOW_BONUS_MESSAGE;
            soundVolume = DEFAULT_SOUND_VOLUME;
            enableAceLag = DEFAULT_ENABLE_ACE_LAG;
            aceLagIntensity = DEFAULT_ACE_LAG_INTENSITY;
            lastLanguageCode = DEFAULT_LAST_LANGUAGE;
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
        json.addProperty("last_language", DEFAULT_LAST_LANGUAGE);
        
        
        currentPresetId = DEFAULT_CURRENT_PRESET;
        enableSound = DEFAULT_ENABLE_SOUND;
        showBonusMessage = DEFAULT_SHOW_BONUS_MESSAGE;
        soundVolume = DEFAULT_SOUND_VOLUME;
        enableAceLag = DEFAULT_ENABLE_ACE_LAG;
        aceLagIntensity = DEFAULT_ACE_LAG_INTENSITY;
        lastLanguageCode = DEFAULT_LAST_LANGUAGE;

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
        root.addProperty("last_language", lastLanguageCode);

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

    private static int clampSoundVolume(int volume) {
        return Math.max(0, Math.min(200, volume));
    }

    private static int clampAceLagIntensity(int intensity) {
        return Math.max(1, Math.min(10, intensity));
    }
}
