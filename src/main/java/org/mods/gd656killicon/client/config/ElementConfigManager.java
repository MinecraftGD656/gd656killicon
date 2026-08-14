package org.mods.gd656killicon.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.mods.gd656killicon.client.bridge.ClientBridge;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import org.mods.gd656killicon.common.bonus.BonusRegistry;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElementConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = ClientBridge.loader().getConfigDir().resolve("gd656killicon").toFile();
    /** 预设存储目录: 每预设一个 json 文件(官方+用户同目录, ID 区分) */
    private static final File PRESETS_DIR = new File(CONFIG_DIR, "presets");
    /** 预设文件格式版本: 数据变更时提升, 触发官方预设重新生成覆盖(玩家同版本修改保留) */
    private static final int FORMAT_VERSION = 1;

    private static final Map<String, ElementPreset> PRESETS = new HashMap<>();
    

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
        JsonObject safeDefaults = getDefaultElementConfig(presetId, elementId);
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
        String defaultName = org.mods.gd656killicon.client.util.I18nCompat.exists(defaultNameKey) 
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

    public static void loadConfig() {
        if (!PRESETS_DIR.exists()) {
            PRESETS_DIR.mkdirs();
        }
        // 强制加载全部动态 format 注册表(懒加载类): normalizePresets 依赖完整注册表判定默认键集合,
        // 必须先于任何配置规范化执行, 否则自定义预设中已注册的动态 format 键会被误判为多余键。
        org.mods.gd656killicon.common.honor.HonorRegistry.getIds();
        org.mods.gd656killicon.common.killtype.KillTypeRegistry.getAll();
        org.mods.gd656killicon.common.bonus.BonusRegistry.getAll();
        File[] existingFiles = PRESETS_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (existingFiles == null || existingFiles.length == 0) {
            migrateLegacySingleFile();
        }
        ensureOfficialPresetFiles();

        PRESETS.clear();
        File[] files = PRESETS_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            createDefaultConfig();
            return;
        }

        for (File file : files) {
            String presetId = file.getName().substring(0, file.getName().length() - 5);
            try {
                JsonObject presetJson = GSON.fromJson(org.mods.gd656killicon.client.util.ConfigFileUtil.readText(file), JsonObject.class);
                PRESETS.put(presetId, parsePresetJson(presetJson, presetId));
            } catch (com.google.gson.JsonSyntaxException e) {
                ClientMessageLogger.error("gd656killicon.client.config.element.load_fail_json");
                ClientMessageLogger.chatWarn("gd656killicon.client.config.element.preset_skipped", presetId, e.getMessage());
            } catch (Exception e) {
                ClientMessageLogger.error("gd656killicon.client.config.element.load_fail", e.getMessage());
            }
        }

        if (PRESETS.isEmpty()) {
            createDefaultConfig();
            return;
        }

        boolean restored = false;
        for (String officialId : DefaultConfigRegistry.getOfficialPresetIds()) {
            if (!PRESETS.containsKey(officialId)) {
                ElementPreset officialPreset = loadOfficialPresetFromJar(officialId);
                if (officialPreset != null) {
                    PRESETS.put(officialId, officialPreset);
                    restored = true;
                    ClientMessageLogger.info("gd656killicon.client.config.element.restored_official", officialId);
                }
            }
        }

        // 加分项系统大清洗升级器：必须在 normalizePresets 之前执行（旧键先搬家，否则会被默认集合删掉）
        if (ClientConfigManager.getBonusFormatMigrated() < BonusFormatMigrator.TARGET_VERSION) {
            BonusFormatMigrator.migrate();
            ClientConfigManager.setBonusFormatMigrated(BonusFormatMigrator.TARGET_VERSION);
        }

        boolean normalized = normalizePresets();
        if (normalized || restored) {
            saveConfig();
        }
        for (String presetId : PRESETS.keySet()) {
            ensurePresetAssets(presetId);
        }
        ClientMessageLogger.info("gd656killicon.client.config.element.load_success");
    }

    /** 解析单个预设文件内容(跳过 format_version / display_name 元数据键)。 */
    private static ElementPreset parsePresetJson(JsonObject presetJson, String presetId) {
        ElementPreset preset = new ElementPreset();
        if (presetJson != null && presetJson.has("display_name")) {
            preset.setDisplayName(presetJson.get("display_name").getAsString());
        } else {
            preset.setDisplayName(DefaultConfigRegistry.getOfficialPresetDisplayName(presetId));
        }
        if (presetJson == null) {
            return preset;
        }
        for (Map.Entry<String, com.google.gson.JsonElement> elementEntry : presetJson.entrySet()) {
            String elementKey = elementEntry.getKey();
            if (elementKey.equals("display_name") || elementKey.equals("format_version")) {
                continue;
            }
            if (elementEntry.getValue().isJsonObject()) {
                preset.addElementConfig(elementKey, elementEntry.getValue().getAsJsonObject());
            }
        }
        return preset;
    }

    /** 保障官方预设文件存在: 缺失/损坏/显式版本落后 → 从 jar 资源解包覆盖(资源缺失则该官方预设不落地)。 */
    private static void ensureOfficialPresetFiles() {
        for (String officialId : DefaultConfigRegistry.getOfficialPresetIds()) {
            File file = new File(PRESETS_DIR, officialId + ".json");
            if (!needsRegenerate(file)) {
                continue;
            }
            String jarContent = readJarResource("/assets/gd656killicon/presets/official/" + officialId + ".json");
            if (jarContent == null) {
                continue;
            }
            try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write(jarContent);
            } catch (IOException e) {
                ClientMessageLogger.error("gd656killicon.client.config.element.save_fail", officialId, e.getMessage());
            }
            ClientMessageLogger.info("gd656killicon.client.config.element.restored_official", officialId);
        }
    }

    /** 是否需要重新生成: 文件缺失、损坏、或显式 format_version 落后(无版本字段的旧文件保留玩家数据)。 */
    private static boolean needsRegenerate(File file) {
        if (!file.exists()) {
            return true;
        }
        int fileVersion = 0;
        try {
            JsonObject json = GSON.fromJson(org.mods.gd656killicon.client.util.ConfigFileUtil.readText(file), JsonObject.class);
            if (json != null && json.has("format_version")) {
                fileVersion = json.get("format_version").getAsInt();
            }
        } catch (Exception ignored) {
            return true; // 损坏
        }
        return fileVersion > 0 && fileVersion < FORMAT_VERSION;
    }

    /** 从 jar 资源读取官方预设(唯一数据源); 资源缺失返回 null。 */
    private static ElementPreset loadOfficialPresetFromJar(String presetId) {
        String content = readJarResource("/assets/gd656killicon/presets/official/" + presetId + ".json");
        if (content == null) {
            return null;
        }
        JsonObject json = GSON.fromJson(content, JsonObject.class);
        return parsePresetJson(json, presetId);
    }

    /** 读取 jar 内资源文本; 不存在或读取失败返回 null。 */
    private static String readJarResource(String path) {
        try (InputStream in = ElementConfigManager.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int n;
            while ((n = in.read(data)) != -1) {
                buffer.write(data, 0, n);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }


    /** 一次性迁移: 旧版单文件 element_config.json → presets/ 目录拆分(仅在 presets/ 为空时触发)。 */
    private static void migrateLegacySingleFile() {
        File legacyFile = new File(CONFIG_DIR, "element_config.json");
        if (!legacyFile.exists()) {
            return;
        }
        try {
            JsonObject json = GSON.fromJson(org.mods.gd656killicon.client.util.ConfigFileUtil.readText(legacyFile), JsonObject.class);
            if (json == null) {
                return;
            }
            for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                writePresetFile(new File(PRESETS_DIR, entry.getKey() + ".json"),
                        parsePresetJson(entry.getValue().getAsJsonObject(), entry.getKey()));
            }
            // 迁移成功后改名留档(不再参与加载; 防误操作, 可手动删除)
            legacyFile.renameTo(new File(CONFIG_DIR, "element_config.json.migrated"));
            ClientMessageLogger.info("gd656killicon.client.config.element.legacy_migrated");
        } catch (Exception e) {
            ClientMessageLogger.error("gd656killicon.client.config.element.legacy_migrate_fail", e.getMessage());
        }
    }

    private static boolean normalizePresets() {
        boolean changed = false;
        for (Map.Entry<String, ElementPreset> presetEntry : PRESETS.entrySet()) {
            String presetId = presetEntry.getKey();
            // 官方预设 = jar 资源唯一数据源(完整快照), 不做默认键修正, 仅处理用户/自定义预设
            if (isOfficialPreset(presetId)) {
                continue;
            }
            ElementPreset preset = presetEntry.getValue();
            for (Map.Entry<String, JsonObject> elementEntry : preset.elementConfigs.entrySet()) {
                String elementId = elementEntry.getKey();
                JsonObject config = elementEntry.getValue();

                if ("kill_icon/valorant".equals(elementId)) {
                    changed |= migrateValorantConfig(presetId, config);
                }
                
                JsonObject safeDefaults = getDefaultElementConfig(presetId, elementId);
                
                if (safeDefaults.entrySet().isEmpty()) {
                    // 未知元素: 保留玩家数据, 不删除(避免静默丢数据)
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
                // 多余键/孤儿键: 保留(玩家数据不静默丢失), 不做删除
            }
        }
        return changed;
    }

    private static boolean migrateValorantConfig(String presetId, JsonObject config) {
        boolean changed = false;
        if (config == null) {
            return false;
        }

        if (config.has("color_gaia_accent")) {
            String legacyAccent = config.get("color_gaia_accent").getAsString();
            if (!config.has("color_accent")) {
                config.addProperty("color_accent", legacyAccent);
                changed = true;
            }
            if (!config.has("enable_accent_tint")) {
                boolean accentCustomized = legacyAccent != null && !"#E2505C".equalsIgnoreCase(legacyAccent);
                config.addProperty("enable_accent_tint", accentCustomized);
                changed = true;
            }
        }

        if (config.has("skin_style")) {
            config.remove("skin_style");
            changed = true;
        }
        if (config.has("base_particle_speed")) {
            config.remove("base_particle_speed");
            changed = true;
        }
        if (config.has("hero_flame_speed")) {
            config.remove("hero_flame_speed");
            changed = true;
        }
        if (config.has("large_sparks_speed")) {
            config.remove("large_sparks_speed");
            changed = true;
        }
        if (config.has("x_sparks_speed")) {
            config.remove("x_sparks_speed");
            changed = true;
        }
        if (config.has("sound_volume")) {
            config.remove("sound_volume");
            changed = true;
        }
        if (config.has("headshot_sound_volume")) {
            config.remove("headshot_sound_volume");
            changed = true;
        }
        if (config.has("enable_icon_antialiasing")) {
            config.remove("enable_icon_antialiasing");
            changed = true;
        }
        if (config.has("display_duration")) {
            config.remove("display_duration");
            changed = true;
        }
        String[] legacyParticleConfigKeys = new String[]{
            "base_particle_scale", "base_particle_x_offset", "base_particle_y_offset", "base_particle_opacity", "base_particle_center_x_offset",
            "hero_flame_scale", "hero_flame_x_offset", "hero_flame_y_offset", "hero_flame_opacity",
            "large_sparks_scale", "large_sparks_x_offset", "large_sparks_y_offset", "large_sparks_opacity",
            "x_sparks_scale", "x_sparks_x_offset", "x_sparks_y_offset", "x_sparks_opacity",
            "enable_custom_color_base_particle", "enable_custom_color_hero_flame", "enable_custom_color_large_sparks", "enable_custom_color_x_sparks",
            "contrast", "icon_entry_curve", "color_headshot_anim_flicker", "headshot_anim_flicker_speed", "headshot_anim_scale_curve",
            "bar_entry_scale_curve", "icon_x_offset", "icon_y_offset", "frame_scale", "enable_ring", "ring_scale", "blade_scale", "bar_scale", "headshot_scale",
            "frame_x_offset", "frame_y_offset", "ring_x_offset", "ring_y_offset", "blade_x_offset", "blade_y_offset", "headshot_x_offset", "headshot_y_offset"
        };
        for (String key : legacyParticleConfigKeys) {
            if (config.has(key)) {
                config.remove(key);
                changed = true;
            }
        }
        if (!config.has("enable_blade_rotation_effect")) {
            config.addProperty("enable_blade_rotation_effect", true);
            changed = true;
        }
        if (!config.has("blade_deceleration_window")) {
            config.addProperty("blade_deceleration_window", 2.0f);
            changed = true;
        }
        if (!config.has("enable_math_particle_effect")) {
            config.addProperty("enable_math_particle_effect", false);
            changed = true;
        }

        String styleId = ValorantStyleCatalog.resolveStyleId(presetId, config);
        if (!config.has("enable_blade_effect")) {
            config.addProperty("enable_blade_effect", ValorantStyleCatalog.usesBlade(styleId));
            changed = true;
        }
        JsonObject valorantDefaults = org.mods.gd656killicon.common.config.ElementConfigRegistry.buildElementDefaults("kill_icon/valorant");
        int expectedBaseParticleYOffset = 45;
        if (valorantDefaults != null && valorantDefaults.has("anim_base_particle_texture_y_offset")) {
            expectedBaseParticleYOffset = valorantDefaults.get("anim_base_particle_texture_y_offset").getAsInt();
        }
        boolean expectedEnableBaseParticle = valorantDefaults == null || !valorantDefaults.has("enable_base_particle") || valorantDefaults.get("enable_base_particle").getAsBoolean();
        boolean expectedEnableHeroFlame = valorantDefaults == null || !valorantDefaults.has("enable_hero_flame") || valorantDefaults.get("enable_hero_flame").getAsBoolean();
        boolean expectedEnableLargeSparks = valorantDefaults == null || !valorantDefaults.has("enable_large_sparks") || valorantDefaults.get("enable_large_sparks").getAsBoolean();
        boolean expectedEnableXSparks = valorantDefaults == null || !valorantDefaults.has("enable_x_sparks") || valorantDefaults.get("enable_x_sparks").getAsBoolean();
        if (!config.has("enable_base_particle")) {
            config.addProperty("enable_base_particle", expectedEnableBaseParticle);
            changed = true;
        }
        if (!config.has("enable_hero_flame")) {
            config.addProperty("enable_hero_flame", expectedEnableHeroFlame);
            changed = true;
        }
        if (!config.has("enable_large_sparks")) {
            config.addProperty("enable_large_sparks", expectedEnableLargeSparks);
            changed = true;
        }
        if (!config.has("enable_x_sparks")) {
            config.addProperty("enable_x_sparks", expectedEnableXSparks);
            changed = true;
        }
        if (!config.has("anim_base_particle_texture_y_offset")) {
            config.addProperty("anim_base_particle_texture_y_offset", expectedBaseParticleYOffset);
            changed = true;
        }
        for (String textureKey : ElementTextureDefinition.getTextures("kill_icon/valorant")) {
            String modeKey = ElementTextureDefinition.getTextureModeKey(textureKey);
            String officialKey = ElementTextureDefinition.getOfficialTextureKey(textureKey);
            String customKey = ElementTextureDefinition.getCustomTextureKey(textureKey);
            String expectedFile = ValorantStyleCatalog.getOfficialTextureFileNameForStyle(styleId, textureKey);
            String mode = config.has(modeKey) ? config.get(modeKey).getAsString() : "official";
            if (!"official".equalsIgnoreCase(mode) && !"custom".equalsIgnoreCase(mode) && !"vanilla".equalsIgnoreCase(mode)) {
                config.addProperty(modeKey, "official");
                changed = true;
                mode = "official";
            }
            if ("custom".equalsIgnoreCase(mode)) {
                String customTexture = config.has(customKey) ? config.get(customKey).getAsString() : "";
                if (customTexture == null || customTexture.isBlank()) {
                    config.addProperty(modeKey, "official");
                    changed = true;
                }
            }
            if (expectedFile == null || expectedFile.isBlank()) {
                continue;
            }
            String officialTexture = config.has(officialKey) ? config.get(officialKey).getAsString() : null;
            if (officialTexture == null || officialTexture.isBlank() || !isValidValorantOfficialTexture(textureKey, officialTexture)) {
                config.addProperty(officialKey, expectedFile);
                changed = true;
            }
        }

        return changed;
    }

    private static boolean isValidValorantOfficialTexture(String textureKey, String fileName) {
        if (!ExternalTextureManager.isOfficialTextureName(fileName)) {
            return false;
        }
        for (ValorantStyleCatalog.StyleSpec definition : ValorantStyleCatalog.getDefinitions()) {
            String allowed = ValorantStyleCatalog.getOfficialTextureFileNameForStyle(definition.styleId(), textureKey);
            if (fileName.equals(allowed)) {
                return true;
            }
        }
        if ("emblem".equals(textureKey)) {
            return "killicon_valorant_icon.png".equals(fileName) || "killicon_valorant_gaia_icon.png".equals(fileName);
        }
        if ("bar".equals(textureKey)) {
            return "killicon_valorant_bar.png".equals(fileName) || "killicon_valorant_gaia_bar.png".equals(fileName);
        }
        return false;
    }

    public static void createDefaultConfig() {
        PRESETS.clear();
        for (String officialId : DefaultConfigRegistry.getOfficialPresetIds()) {
            ElementPreset preset = loadOfficialPresetFromJar(officialId);
            if (preset != null) {
                PRESETS.put(officialId, preset);
            }
        }
        saveConfig();
        // 重新加载以同步文件(无官方预设时跳过, 避免递归)
        if (!PRESETS.isEmpty()) {
            loadConfig();
        }
    }

    public static void resetPresetConfig(String presetId) {
        ElementPreset currentPreset = getActivePresets().get(presetId);
        if (currentPreset == null) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.preset_not_found", presetId);
            return;
        }

        boolean updated = false;

        if (isOfficialPreset(presetId)) {
            // 官方预设唯一数据源 = jar 资源: 重置 = 从 jar 恢复完整内容
            // (官方预设默认 = jar 完整快照, 不用注册表默认)
            ElementPreset jarPreset = loadOfficialPresetFromJar(presetId);
            if (jarPreset != null) {
                currentPreset.elementConfigs.clear();
                currentPreset.elementConfigs.putAll(jarPreset.elementConfigs);
                updated = true;
            }
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
        // 声明式配置注册表: 默认值 = 注册表静态键 + 动态键, format 键覆盖为当前语言(官方预设由 jar json 覆盖, 不经过此处)
        return buildLanguageDefaults(elementId);
    }
    
    /**
     * 获取全局默认配置（第一默认配置），用于非官方预设
     */
    public static JsonObject getDefaultElementConfig(String name) {
        return buildLanguageDefaults(name);
    }

    /** 注册表默认配置 + format 键覆盖为当前语言(formats json 唯一来源): 新增元素/自定义预设重置时写入语言默认文本。 */
    private static JsonObject buildLanguageDefaults(String elementId) {
        JsonObject defaults = org.mods.gd656killicon.common.config.ElementConfigRegistry.buildElementDefaults(elementId);
        for (String key : defaults.keySet()) {
            if (key.startsWith("format_") || key.equals("kill_feed_format") || key.equals("best_text_format")) {
                defaults.addProperty(key, org.mods.gd656killicon.client.config.FormatDefaultsManager.getDefault(elementId, key));
            }
        }
        return defaults;
    }
    
    public static void resetConfig() {
        if (PRESETS_DIR.exists()) {
            File[] files = PRESETS_DIR.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        ClientMessageLogger.chatError("gd656killicon.client.config.element.delete_old_fail_occupied");
                        ClientMessageLogger.error("gd656killicon.client.config.element.delete_old_fail", file.getName());
                    }
                }
            }
        }
        createDefaultConfig();
    }

    public static void resetOfficialPreset(String presetId) {
        if (!isOfficialPresetId(presetId)) {
            ClientMessageLogger.chatError("gd656killicon.client.config.element.reset_unofficial_fail", presetId);
            return;
        }
        ElementPreset preset = loadOfficialPresetFromJar(presetId);
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
        for (Map.Entry<String, ElementPreset> entry : PRESETS.entrySet()) {
            writePresetFile(new File(PRESETS_DIR, entry.getKey() + ".json"), entry.getValue());
        }
        // 清理孤儿文件: presets/ 下不在 PRESETS 中的预设文件(处理 rename/delete)
        File[] files = PRESETS_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 5);
                if (!PRESETS.containsKey(id)) {
                    file.delete();
                }
            }
        }
    }

    /** 写入单个预设文件(带 format_version + display_name + 元素配置)。 */
    private static void writePresetFile(File file, ElementPreset preset) {
        JsonObject presetJson = new JsonObject();
        presetJson.addProperty("format_version", FORMAT_VERSION);
        if (preset.getDisplayName() != null && !preset.getDisplayName().isEmpty()) {
            presetJson.addProperty("display_name", preset.getDisplayName());
        }
        preset.elementConfigs.forEach(presetJson::add);
        try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            GSON.toJson(presetJson, writer);
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.config.element.save_fail", e.getMessage());
        }
    }

    public static JsonObject getElementConfig(String presetId, String elementId) {
        ElementPreset preset = getPreset(presetId);
        if (preset == null) return null;
        return preset.getConfig(elementId);
    }

    /**
     * 配置项默认/重置值的唯一解析源(单行重置按钮、元素级重置、纹理重置/修改判断统一使用)：
     * 官方预设 → jar 资源 json 为唯一默认源(预设里存什么, 默认就是什么);
     *             jar 缺失的元素/键(如玩家在官方预设新增的元素)回落注册表+format json 补齐;
     * 自定义预设 → 统统注册表默认值 + format json 语言默认(buildLanguageDefaults)。
     */
    public static JsonObject getResetDefaultConfig(String presetId, String elementId) {
        JsonObject languageDefaults = getDefaultElementConfig(elementId);
        if (!isOfficialPreset(presetId)) {
            return languageDefaults;
        }
        ElementPreset jarPreset = loadOfficialPresetFromJar(presetId);
        if (jarPreset == null) {
            return languageDefaults;
        }
        JsonObject jarConfig = jarPreset.getConfig(elementId);
        if (jarConfig == null) {
            return languageDefaults;
        }
        JsonObject result = jarConfig.deepCopy();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : languageDefaults.entrySet()) {
            if (!result.has(entry.getKey())) {
                result.add(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
    
    public static Set<String> getAllElementTypes() {
        return org.mods.gd656killicon.common.config.ElementConfigRegistry.getElements();
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
        keys.addAll(org.mods.gd656killicon.common.config.ElementConfigRegistry.getKeys(elementId));
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
        String defaultNameKey = "gd656killicon.client.config.preset.new_preset_default_name";
        preset.setDisplayName(org.mods.gd656killicon.client.util.I18nCompat.exists(defaultNameKey)
                ? net.minecraft.client.resources.language.I18n.get(defaultNameKey)
                : "New Custom Preset");
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
                        config.addProperty(key, Double.parseDouble(value));
                    } catch (NumberFormatException ignored) {
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
