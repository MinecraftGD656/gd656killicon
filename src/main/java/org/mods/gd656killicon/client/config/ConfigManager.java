package org.mods.gd656killicon.client.config;

import com.google.gson.JsonObject;
import java.util.Set;
import org.mods.gd656killicon.client.sounds.ExternalSoundManager;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;

public class ConfigManager {

    public static void init() {
        ClientConfigManager.init();
        ElementConfigManager.init();
    }

    public static void startEditing() {
        ClientConfigManager.startEditing();
        ElementConfigManager.startEditing();
        ExternalSoundManager.startEditing();
    }

    public static void saveChanges() {
        ClientConfigManager.saveChanges();
        ElementConfigManager.saveChanges();
        ExternalTextureManager.confirmPendingTextureReplacements();
        ExternalSoundManager.saveChanges();
    }

    public static void discardChanges() {
        ClientConfigManager.discardChanges();
        ElementConfigManager.discardChanges();
        ExternalTextureManager.revertPendingTextureReplacements();
        ExternalSoundManager.discardChanges();
    }

    public static boolean hasUnsavedChanges() {
        return ClientConfigManager.hasUnsavedChanges()
                || ElementConfigManager.hasUnsavedChanges()
                || ExternalTextureManager.hasPendingTextureChanges()
                || ExternalSoundManager.hasPendingSoundChanges();
    }

    public static void loadConfig() {
        ElementConfigManager.loadConfig();
        ClientConfigManager.loadGlobalConfig();
    }

    public static void resetConfig() {
        ElementConfigManager.resetConfig();
        ClientConfigManager.resetToDefaults();
        ElementConfigManager.loadConfig();
    }

    public static void resetOfficialPreset(String presetId) {
        ElementConfigManager.resetOfficialPreset(presetId);
    }

    public static void clearPresetElements(String presetId) {
        ElementConfigManager.clearPresetElements(presetId);
    }

    public static void resetPresetConfig(String presetId) {
        ElementConfigManager.resetPresetConfig(presetId);
    }

    public static void setCurrentPresetId(String id) {
        String previousId = ClientConfigManager.getCurrentPresetId();
        ClientConfigManager.setCurrentPresetId(id);
        if (id != null && !id.equals(previousId)) {
            ExternalSoundManager.reloadAsync();
        }
    }

    public static void setEnableSound(boolean enable) {
        ClientConfigManager.setEnableSound(enable);
    }

    public static void setShowBonusMessage(boolean show) {
        ClientConfigManager.setShowBonusMessage(show);
    }

    public static void setSoundVolume(int volume) {
        ClientConfigManager.setSoundVolume(volume);
    }

    public static String getCurrentPresetId() {
        return ClientConfigManager.getCurrentPresetId();
    }

    public static JsonObject getElementConfig(String category, String name) {
        return ElementConfigManager.getElementConfig(ClientConfigManager.getCurrentPresetId(), category + "/" + name);
    }

    public static Set<String> getPresetIds() {
        return ElementConfigManager.getPresetIds();
    }

    public static Set<String> getElementIds(String presetId) {
        return ElementConfigManager.getElementIds(presetId);
    }

    public static Set<String> getAllElementTypes() {
        return ElementConfigManager.getAllElementTypes();
    }

    public static Set<String> getAvailableElementTypes(String presetId) {
        return ElementConfigManager.getAvailableElementTypes(presetId);
    }

    public static Set<String> getConfigKeys(String presetId, String elementId) {
        return ElementConfigManager.getConfigKeys(presetId, elementId);
    }

    public static void createPreset(String presetId) {
        ElementConfigManager.createPreset(presetId);
    }

    public static void addElementToPreset(String presetId, String elementId) {
        ElementConfigManager.addElementToPreset(presetId, elementId);
    }

    public static void removeElementFromPreset(String presetId, String elementId) {
        ElementConfigManager.removeElementFromPreset(presetId, elementId);
    }

    public static void updateConfigValue(String presetId, String elementId, String key, String value) {
        ElementConfigManager.updateConfigValue(presetId, elementId, key, value);
    }

    public static JsonObject getDefaultElementConfig(String name) {
        return ElementConfigManager.getDefaultElementConfig(name);
    }

    public static boolean isOfficialPreset(String presetId) {
        return ElementConfigManager.isOfficialPresetId(presetId);
    }

    public static void resetFull() {
        resetConfig();

        java.nio.file.Path assetsDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("gd656killicon/assets");
        if (java.nio.file.Files.exists(assetsDir)) {
            try {
                java.nio.file.Files.list(assetsDir).forEach(presetPath -> {
                    String presetName = presetPath.getFileName().toString();
                    boolean isOfficial = isOfficialPreset(presetName);

                    if (java.nio.file.Files.isDirectory(presetPath)) {
                        try {
                            if (isOfficial) {
                                java.nio.file.Files.list(presetPath).forEach(subPath -> {
                                    String subName = subPath.getFileName().toString();
                                    if (!"sounds".equals(subName)) {
                                        deleteRecursively(subPath);
                                    }
                                });
                            } else {
                                deleteRecursively(presetPath);
                            }
                        } catch (java.io.IOException e) {
                            org.mods.gd656killicon.client.util.ClientMessageLogger.error("Failed to clean preset folder: " + presetName, e.getMessage());
                        }
                    }
                });
            } catch (java.io.IOException e) {
                org.mods.gd656killicon.client.util.ClientMessageLogger.error("Failed to list assets directory", e.getMessage());
            }
        }

        org.mods.gd656killicon.client.textures.ExternalTextureManager.resetAllTexturesAsync();
        org.mods.gd656killicon.client.sounds.ExternalSoundManager.resetAllSoundsAsync();

        org.mods.gd656killicon.client.util.ClientMessageLogger.chatSuccess("gd656killicon.client.command.reset_success");
    }

    private static void deleteRecursively(java.nio.file.Path path) {
        try {
            if (java.nio.file.Files.isDirectory(path)) {
                try (java.util.stream.Stream<java.nio.file.Path> entries = java.nio.file.Files.list(path)) {
                    entries.forEach(ConfigManager::deleteRecursively);
                }
            }
            java.nio.file.Files.deleteIfExists(path);
        } catch (java.io.IOException e) {
        }
    }
}
