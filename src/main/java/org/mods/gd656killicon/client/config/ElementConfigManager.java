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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElementConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("gd656killicon").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "element_config.json");

    private static final Map<String, ElementPreset> PRESETS = new HashMap<>();
    private static boolean pendingLocalization = false;
    
    
    

    
    private static Map<String, ElementPreset> TEMP_PRESETS = null;
    private static boolean isEditing = false;

    public static void startEditing() {
        if (isEditing) return;
        TEMP_PRESETS = new HashMap<>();
        
        for (Map.Entry<String, ElementPreset> entry : PRESETS.entrySet()) {
            ElementPreset preset = new ElementPreset();
            preset.setDisplayName(entry.getValue().getDisplayName()); 
            for (Map.Entry<String, JsonObject> elementEntry : entry.getValue().elementConfigs.entrySet()) {
                preset.addElementConfig(elementEntry.getKey(), elementEntry.getValue().deepCopy());
            }
            TEMP_PRESETS.put(entry.getKey(), preset);
        }
        isEditing = true;
    }

    public static void saveChanges() {
        if (isEditing) {
            PRESETS.clear();
            PRESETS.putAll(TEMP_PRESETS);
            isEditing = false;
            TEMP_PRESETS = null;
            saveConfig();
        }
    }

    public static void discardChanges() {
        if (isEditing) {
            isEditing = false;
            TEMP_PRESETS = null;
        }
    }

    public static boolean hasUnsavedChanges() {
        if (!isEditing || TEMP_PRESETS == null) return false;
        if (TEMP_PRESETS.size() != PRESETS.size()) return true;
        
        for (Map.Entry<String, ElementPreset> entry : PRESETS.entrySet()) {
            String key = entry.getKey();
            if (!TEMP_PRESETS.containsKey(key)) return true;
            if (!TEMP_PRESETS.get(key).equals(entry.getValue())) return true;
        }
        return false;
    }
    
    public static void addElement(String presetId, String elementId) {
        ElementPreset preset = getActivePresets().get(presetId);
        if (preset == null) return;
        
        if (preset.getConfig(elementId) != null) return; 

        JsonObject safeDefaults = getDefaultElementConfig(elementId);
        if (!safeDefaults.entrySet().isEmpty()) {
            preset.addElementConfig(elementId, safeDefaults);
            if (!isEditing) {
                saveConfig();
                ClientMessageLogger.chatSuccess("gd656killicon.client.config.element.element_added", elementId, presetId);
            }
        }
    }

    public static boolean deletePreset(String presetId) {
        if (isOfficialPreset(presetId)) {
            ClientMessageLogger.chatWarn("gd656killicon.client.config.element.delete_fail_official");
            return false;
        }
        
        Map<String, ElementPreset> presets = getActivePresets();
        if (presets.containsKey(presetId)) {
            presets.remove(presetId);
            if (!isEditing) {
                saveConfig();
                ClientMessageLogger.chatInfo("gd656killicon.client.config.element.delete_success", presetId);
            }
            return true;
        }
        return false;
    }

    public static boolean renamePresetId(String oldId, String newId) {
        if (isOfficialPreset(oldId)) return false;
        if (oldId.equals(newId)) return true; 
        
        Map<String, ElementPreset> presets = getActivePresets();
        if (!presets.containsKey(oldId)) return false;
        if (presets.containsKey(newId)) return false; 
        
        ElementPreset preset = presets.remove(oldId);
        presets.put(newId, preset);
        
        if (!isEditing) {
            saveConfig();
        }
        return true;
    }

    public static boolean presetExists(String presetId) {
        return getActivePresets().containsKey(presetId);
    }

    public static String createNewPreset() {
        
        String newId;
        do {
            int randomNum = (int)(Math.random() * 100000);
            newId = String.format("%05d", randomNum);
        } while (presetExists(newId) || isOfficialPreset(newId));
        
        ElementPreset preset = new ElementPreset();
        
        
        String defaultNameKey = "gd656killicon.client.config.preset.new_preset_default_name";
        String defaultName = net.minecraft.client.resources.language.I18n.exists(defaultNameKey) 
                ? net.minecraft.client.resources.language.I18n.get(defaultNameKey) 
                : "New Custom Preset";
        
        preset.setDisplayName(defaultName);
        
        
        getActivePresets().put(newId, preset);
        ensurePresetAssets(newId);
        
        if (!isEditing) {
            saveConfig();
            ClientMessageLogger.chatSuccess("gd656killicon.client.config.element.preset_created", newId);
        }
        return newId;
    }

    public static ElementPreset getPreset(String presetId) {
        return getActivePresets().get(presetId);
    }

    public static Map<String, ElementPreset> getActivePresets() {
        return isEditing && TEMP_PRESETS != null ? TEMP_PRESETS : PRESETS;
    }

    public static boolean isOfficialPreset(String presetId) {
        return DefaultConfigRegistry.isOfficialPreset(presetId);
    }

    public static void init() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        loadConfig();
    }

    public static void tryApplyLocalization() {
        if (!pendingLocalization) return;
        if (!isLocalizationReady()) return;
        boolean changed = localizeAllPresets();
        if (changed) {
            saveConfig();
        }
        pendingLocalization = false;
    }

    private static boolean isLocalizationReady() {
        return net.minecraft.client.resources.language.I18n.exists("gd656killicon.client.format.normal");
    }

    private static boolean localizeAllPresets() {
        boolean changed = false;
        for (ElementPreset preset : PRESETS.values()) {
            for (JsonObject config : preset.elementConfigs.values()) {
                changed |= localizeConfig(config);
            }
        }
        return changed;
    }

    private static boolean localizeConfig(JsonObject config) {
        boolean changed = false;
        for (String key : config.keySet()) {
            if (!isTranslatableKey(key)) continue;
            com.google.gson.JsonElement element = config.get(key);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String value = element.getAsString();
                if (net.minecraft.client.resources.language.I18n.exists(value)) {
                    String localized = net.minecraft.client.resources.language.I18n.get(value);
                    if (!localized.equals(value)) {
                        config.addProperty(key, localized);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private static boolean isTranslatableKey(String key) {
        return key.startsWith("format_") || key.equals("kill_feed_format");
    }

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            createDefaultConfig();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            PRESETS.clear();
            
            json.entrySet().forEach(entry -> {
                String presetId = entry.getKey();
                JsonObject presetJson = entry.getValue().getAsJsonObject();
                ElementPreset preset = new ElementPreset();
                
                
                if (presetJson.has("display_name")) {
                    preset.setDisplayName(presetJson.get("display_name").getAsString());
                } else {
                    preset.setDisplayName(DefaultConfigRegistry.getOfficialPresetDisplayName(presetId));
                }
                
                presetJson.entrySet().forEach(elementEntry -> {
                    String elementKey = elementEntry.getKey(); 
                    if (elementKey.equals("display_name")) return; 

                    JsonObject elementConfig = elementEntry.getValue().getAsJsonObject();
                    preset.addElementConfig(elementKey, elementConfig);
                });
                
                PRESETS.put(presetId, preset);
            });
            
            
            boolean restored = false;
            for (String officialId : DefaultConfigRegistry.getOfficialPresetIds()) {
                if (!PRESETS.containsKey(officialId)) {
                    ElementPreset officialPreset = createOfficialPreset(officialId);
                    if (officialPreset != null) {
                        PRESETS.put(officialId, officialPreset);
                        restored = true;
                        ClientMessageLogger.info("gd656killicon.client.config.element.restored_official", officialId);
                    }
                }
            }
            
            boolean normalized = normalizePresets();
            if (normalized || restored) {
                saveConfig();
            }
            for (String presetId : PRESETS.keySet()) {
                ensurePresetAssets(presetId);
            }
            ClientMessageLogger.info("gd656killicon.client.config.element.load_success");
        } catch (com.google.gson.JsonSyntaxException e) {
            ClientMessageLogger.error("gd656killicon.client.config.element.load_fail_json");
            ClientMessageLogger.chatError("gd656killicon.client.config.element.load_fail_json");
            if (e.getMessage().contains("Unterminated object")) {
                 ClientMessageLogger.chatWarn("gd656killicon.client.config.element.missing_brace_or_comma");
            } else if (e.getMessage().contains("Expected name")) {
                 ClientMessageLogger.chatWarn("gd656killicon.client.config.element.invalid_key");
            } else {
                 ClientMessageLogger.chatWarn("gd656killicon.client.config.element.detail", e.getMessage());
            }
        } catch (Exception e) {
            ClientMessageLogger.error("gd656killicon.client.config.element.load_fail", e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean normalizePresets() {
        boolean changed = false;
        for (Map.Entry<String, ElementPreset> presetEntry : PRESETS.entrySet()) {
            String presetId = presetEntry.getKey();
            ElementPreset preset = presetEntry.getValue();
            java.util.Iterator<Map.Entry<String, JsonObject>> elementIterator = preset.elementConfigs.entrySet().iterator();
            while (elementIterator.hasNext()) {
                Map.Entry<String, JsonObject> elementEntry = elementIterator.next();
                String elementId = elementEntry.getKey();
                JsonObject config = elementEntry.getValue();
                
                
                JsonObject safeDefaults = getDefaultElementConfig(elementId);
                
                if (safeDefaults.entrySet().isEmpty()) {
                    elementIterator.remove();
                    changed = true;
                    ClientMessageLogger.chatWarn("gd656killicon.client.config.element.removed_unknown", presetId, elementId);
                    continue;
                }

                
                for (Map.Entry<String, com.google.gson.JsonElement> defaultEntry : safeDefaults.entrySet()) {
                    String key = defaultEntry.getKey();
                    if (!config.has(key)) {
                        config.add(key, defaultEntry.getValue());
                        changed = true;
                        ClientMessageLogger.chatWarn("gd656killicon.client.config.element.restored_missing", presetId, elementId, key);
                    }
                }

                
                java.util.Iterator<Map.Entry<String, com.google.gson.JsonElement>> configIterator = config.entrySet().iterator();
                while (configIterator.hasNext()) {
                    String key = configIterator.next().getKey();
                    if (!safeDefaults.has(key)) {
                        configIterator.remove();
                        changed = true;
                        ClientMessageLogger.chatWarn("gd656killicon.client.config.element.removed_extra", presetId, elementId, key);
                    }
                }
            }
        }
        return changed;
    }

    private static ElementPreset createOfficialPreset(String presetId) {
        if (!DefaultConfigRegistry.isOfficialPreset(presetId)) return null;

        ElementPreset preset = new ElementPreset();
        preset.setDisplayName(DefaultConfigRegistry.getOfficialPresetDisplayName(presetId));
        
        Set<String> elementIds = DefaultConfigRegistry.getOfficialPresetElements(presetId);
        for (String elementId : elementIds) {
            JsonObject config = DefaultConfigRegistry.getDefaultConfig(presetId, elementId);
            preset.addElementConfig(elementId, config);
        }
        
        return preset;
    }

    public static void createDefaultConfig() {
        PRESETS.clear();
        for (String officialId : DefaultConfigRegistry.getOfficialPresetIds()) {
            PRESETS.put(officialId, createOfficialPreset(officialId));
        }
        pendingLocalization = true;
        saveConfig();
        loadConfig(); 
    }

    public static void resetPresetConfig(String presetId) {
        ElementPreset currentPreset = getActivePresets().get(presetId);
        if (currentPreset == null) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.preset_not_found", presetId);
            return;
        }

        boolean updated = false;

        if (isOfficialPreset(presetId)) {
            
            
            currentPreset.elementConfigs.clear();
            
            
            Set<String> elementIds = DefaultConfigRegistry.getOfficialPresetElements(presetId);
            for (String elementId : elementIds) {
                
                JsonObject config = DefaultConfigRegistry.getDefaultConfig(presetId, elementId);
                currentPreset.addElementConfig(elementId, config);
            }
            updated = true;
        } else {
            
            Set<String> currentElements = new HashSet<>(currentPreset.elementConfigs.keySet());
            if (currentElements.isEmpty()) {
                ClientMessageLogger.chatInfo("gd656killicon.client.config.element.preset_empty", presetId);
                return;
            }

            for (String elementId : currentElements) {
                JsonObject safeDefaults = getDefaultElementConfig(elementId);
                if (!safeDefaults.entrySet().isEmpty()) {
                    currentPreset.addElementConfig(elementId, safeDefaults);
                    updated = true;
                }
            }
        }

        if (updated) {
            if (!isEditing) {
                saveConfig();
                ClientMessageLogger.chatSuccess("gd656killicon.client.config.element.reset_preset_config_success", presetId);
            }
        } else {
            ClientMessageLogger.chatInfo("gd656killicon.client.config.element.reset_preset_config_no_change", presetId);
        }
    }
    
    /**
     * 获取指定预设中特定元素的默认配置。
     * 对于官方预设，这可能是特定的覆盖配置；对于非官方预设，则是全局默认配置。
     */
    public static JsonObject getDefaultElementConfig(String presetId, String elementId) {
        return DefaultConfigRegistry.getDefaultConfig(presetId, elementId);
    }
    
    /**
     * 获取全局默认配置（第一默认配置），用于非官方预设
     */
    public static JsonObject getDefaultElementConfig(String name) {
        return DefaultConfigRegistry.getGlobalDefault(name);
    }
    
    public static JsonObject getElementConfigWithFallback(String presetId, String elementId) {
        JsonObject config = getElementConfig(presetId, elementId);
        JsonObject safeDefaults = getDefaultElementConfig(elementId);
        
        
        if (config == null) {
            return safeDefaults;
        }
        
        
        JsonObject result = config.deepCopy();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : safeDefaults.entrySet()) {
            String key = entry.getKey();
            if (!result.has(key)) {
                result.add(key, entry.getValue());
            }
        }
        
        return result;
    }

    public static void resetConfig() {
        if (CONFIG_FILE.exists()) {
            if (CONFIG_FILE.delete()) {
                ClientMessageLogger.info("gd656killicon.client.config.element.delete_old_success");
            } else {
                ClientMessageLogger.chatError("gd656killicon.client.config.element.delete_old_fail_occupied");
                ClientMessageLogger.error("gd656killicon.client.config.element.delete_old_fail");
            }
        }
        createDefaultConfig();
    }

    public static void resetOfficialPreset(String presetId) {
        if (!isOfficialPresetId(presetId)) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.reset_unofficial_fail", presetId);
            return;
        }
        ElementPreset preset = createOfficialPreset(presetId);
        if (preset == null) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.reset_preset_fail", presetId);
            return;
        }
        getActivePresets().put(presetId, preset);
        if (!isEditing) {
            saveConfig();
            ClientMessageLogger.chatSuccess("gd656killicon.client.config.element.reset_official_success", presetId);
        }
    }

    public static void clearPresetElements(String presetId) {
        ElementPreset preset = getActivePresets().get(presetId);
        if (preset == null) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.preset_not_found", presetId);
            return;
        }
        preset.elementConfigs.clear();
        if (!isEditing) {
            saveConfig();
            ClientMessageLogger.chatSuccess("gd656killicon.client.config.element.preset_cleared", presetId);
        }
    }

    public static void saveConfig() {
        JsonObject root = new JsonObject();
        PRESETS.forEach((presetId, preset) -> {
            JsonObject presetJson = new JsonObject();
            if (preset.getDisplayName() != null && !preset.getDisplayName().isEmpty()) {
                presetJson.addProperty("display_name", preset.getDisplayName());
            }
            preset.elementConfigs.forEach(presetJson::add);
            root.add(presetId, presetJson);
        });

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.config.element.save_fail", e.getMessage());
            e.printStackTrace();
        }
    }
    public static JsonObject getElementConfig(String presetId, String elementId) {
        ElementPreset preset = getPreset(presetId);
        if (preset == null) return null;
        return preset.getConfig(elementId);
    }
    
    public static Set<String> getAllElementTypes() {
        return DefaultConfigRegistry.getAllElementTypes();
    }

    public static Set<String> getAvailableElementTypes(String presetId) {
        Set<String> all = new HashSet<>(getAllElementTypes());
        Set<String> existing = getElementIds(presetId);
        all.removeAll(existing);
        return all;
    }
    
    public static Set<String> getPresetIds() {
        return getActivePresets().keySet();
    }
    
    public static Set<String> getElementIds(String presetId) {
        ElementPreset preset = getActivePresets().get(presetId);
        if (preset == null) return Collections.emptySet();
        return preset.elementConfigs.keySet();
    }
    
    public static Set<String> getConfigKeys(String presetId, String elementId) {
        ElementPreset preset = getActivePresets().get(presetId);
        if (preset == null) return Collections.emptySet();
        JsonObject config = preset.getConfig(elementId);
        if (config == null) return Collections.emptySet();
        Set<String> keys = new HashSet<>(config.keySet());
        keys.addAll(getDefaultElementConfig(elementId).keySet());
        return keys;
    }
    
    
    
    public static void addElementToPreset(String presetId, String elementId) {
        ElementPreset preset = getActivePresets().get(presetId);
        if (preset == null) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.preset_not_found", presetId);
            return;
        }
        if (preset.getConfig(elementId) != null) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.element_exists", elementId);
            return;
        }
        preset.addElementConfig(elementId, getDefaultElementConfig(elementId));
        if (!isEditing) {
            saveConfig();
            ClientMessageLogger.chatSuccess("gd656killicon.client.config.element.element_added", elementId, presetId);
        }
    }

    public static void removeElementFromPreset(String presetId, String elementId) {
        ElementPreset preset = getActivePresets().get(presetId);
        if (preset == null) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.preset_not_found", presetId);
            return;
        }
        if (preset.getConfig(elementId) == null) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.element_not_found", elementId);
            return;
        }
        preset.elementConfigs.remove(elementId);
        if (!isEditing) {
            saveConfig();
            ClientMessageLogger.chatSuccess("gd656killicon.client.config.element.element_removed", elementId, presetId);
        }
    }
    
    public static boolean isOfficialPresetId(String presetId) {
        return DefaultConfigRegistry.isOfficialPreset(presetId);
    }

    

    public static String getPresetDisplayName(String presetId) {
        ElementPreset preset = getActivePresets().get(presetId);
        return preset != null ? preset.getDisplayName() : "";
    }

    public static void setPresetDisplayName(String presetId, String displayName) {
        ElementPreset preset = getActivePresets().get(presetId);
        if (preset != null) {
            preset.setDisplayName(displayName);
            if (!isEditing) {
                saveConfig();
            }
        }
    }

    public static void createPreset(String presetId) {
        if (getActivePresets().containsKey(presetId) || isOfficialPreset(presetId)) {
            return;
        }
        ElementPreset preset = new ElementPreset();
        preset.setDisplayName("新自定义预设");
        getActivePresets().put(presetId, preset);
        ensurePresetAssets(presetId);
        if (!isEditing) {
            saveConfig();
        }
    }

    public static void ensurePresetAssets(String presetId) {
        org.mods.gd656killicon.client.textures.ExternalTextureManager.ensureTextureFilesForPreset(presetId);
        org.mods.gd656killicon.client.sounds.ExternalSoundManager.ensureSoundFilesForPreset(presetId);
    }

    public static void setElementConfig(String presetId, String elementId, JsonObject config) {
        ElementPreset preset = getActivePresets().get(presetId);
        if (preset != null) {
            preset.addElementConfig(elementId, config);
            if (!isEditing) {
                saveConfig();
            }
        }
    }

    public static void updateConfigValue(String presetId, String elementId, String key, String value) {
        ElementPreset preset = getActivePresets().get(presetId);
        if (preset == null) return;
        
        JsonObject config = preset.getConfig(elementId);
        if (config == null) return;

        
        JsonObject defaultConfig = getDefaultElementConfig(presetId, elementId);
        if (defaultConfig != null && defaultConfig.has(key)) {
            com.google.gson.JsonElement defaultVal = defaultConfig.get(key);
            if (defaultVal.isJsonPrimitive()) {
                if (defaultVal.getAsJsonPrimitive().isBoolean()) {
                    config.addProperty(key, Boolean.parseBoolean(value));
                } else if (defaultVal.getAsJsonPrimitive().isNumber()) {
                    try {
                        double d = Double.parseDouble(value);
                        
                        if (defaultVal.getAsNumber().doubleValue() == Math.ceil(defaultVal.getAsNumber().doubleValue())) {
                             
                             
                             
                             config.addProperty(key, d);
                        } else {
                             config.addProperty(key, d);
                        }
                    } catch (NumberFormatException e) {
                        
                    }
                } else {
                    config.addProperty(key, value);
                }
            } else {
                config.addProperty(key, value);
            }
        } else {
            
            
            
            
            
            config.addProperty(key, value);
        }
        
        if (!isEditing) {
            saveConfig();
        }
    }

    public static class ElementPreset {
        private final Map<String, JsonObject> elementConfigs = new HashMap<>();
        private String displayName = "";

        public void addElementConfig(String key, JsonObject config) {
            elementConfigs.put(key, config);
        }

        public Map<String, JsonObject> getElementConfigs() {
            return elementConfigs;
        }

        public JsonObject getConfig(String key) {
            return elementConfigs.get(key);
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ElementPreset that = (ElementPreset) o;
            return java.util.Objects.equals(displayName, that.displayName) &&
                   java.util.Objects.equals(elementConfigs, that.elementConfigs);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(elementConfigs, displayName);
        }
    }
}
