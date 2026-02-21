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

    // Default Values
    private static final String DEFAULT_CURRENT_PRESET = "00001";
    private static final boolean DEFAULT_ENABLE_SOUND = true;
    private static final boolean DEFAULT_SHOW_BONUS_MESSAGE = false;

    // Current Config Values
    private static String currentPresetId = DEFAULT_CURRENT_PRESET;
    private static boolean enableSound = DEFAULT_ENABLE_SOUND;
    private static boolean showBonusMessage = DEFAULT_SHOW_BONUS_MESSAGE;

    // Temporary Config Values (Editing Mode)
    private static String tempCurrentPresetId = null;
    private static Boolean tempEnableSound = null;
    private static Boolean tempShowBonusMessage = null;
    private static boolean isEditing = false;

    public static void startEditing() {
        tempCurrentPresetId = currentPresetId;
        tempEnableSound = enableSound;
        tempShowBonusMessage = showBonusMessage;
        isEditing = true;
    }

    public static void saveChanges() {
        if (isEditing) {
            currentPresetId = tempCurrentPresetId;
            enableSound = tempEnableSound;
            showBonusMessage = tempShowBonusMessage;
            isEditing = false;
            saveGlobalConfig();
            
            tempCurrentPresetId = null;
            tempEnableSound = null;
            tempShowBonusMessage = null;
        }
    }

    public static void discardChanges() {
        if (isEditing) {
            isEditing = false;
            tempCurrentPresetId = null;
            tempEnableSound = null;
            tempShowBonusMessage = null;
        }
    }

    public static boolean hasUnsavedChanges() {
        if (!isEditing) return false;
        if (tempCurrentPresetId != null && !tempCurrentPresetId.equals(currentPresetId)) return true;
        if (tempEnableSound != null && !tempEnableSound.equals(enableSound)) return true;
        if (tempShowBonusMessage != null && !tempShowBonusMessage.equals(showBonusMessage)) return true;
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
            // Use defaults as fallback
            currentPresetId = json.has("current_preset") ? json.get("current_preset").getAsString() : DEFAULT_CURRENT_PRESET;
            enableSound = json.has("enable_sound") ? json.get("enable_sound").getAsBoolean() : DEFAULT_ENABLE_SOUND;
            showBonusMessage = json.has("show_bonus_message") ? json.get("show_bonus_message").getAsBoolean() : DEFAULT_SHOW_BONUS_MESSAGE;
        } catch (Exception e) {
            ClientMessageLogger.error("gd656killicon.client.config.load_fail", e.getMessage());
            e.printStackTrace();
            // Fallback to defaults on error
            currentPresetId = DEFAULT_CURRENT_PRESET;
            enableSound = DEFAULT_ENABLE_SOUND;
            showBonusMessage = DEFAULT_SHOW_BONUS_MESSAGE;
        }
    }

    public static void createDefaultConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("current_preset", DEFAULT_CURRENT_PRESET);
        json.addProperty("enable_sound", DEFAULT_ENABLE_SOUND);
        json.addProperty("show_bonus_message", DEFAULT_SHOW_BONUS_MESSAGE);
        
        // Update memory state as well
        currentPresetId = DEFAULT_CURRENT_PRESET;
        enableSound = DEFAULT_ENABLE_SOUND;
        showBonusMessage = DEFAULT_SHOW_BONUS_MESSAGE;

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

        try (FileWriter writer = new FileWriter(GLOBAL_CONFIG_FILE)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.config.save_fail", e.getMessage());
        }
    }

    public static void resetToDefaults() {
        createDefaultConfig();
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
}
