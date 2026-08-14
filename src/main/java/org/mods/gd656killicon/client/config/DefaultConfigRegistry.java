package org.mods.gd656killicon.client.config;

import net.minecraft.client.resources.language.I18n;

import java.util.HashMap;
import java.util.Map;

/**
 * 官方预设结构注册表(元素组成 + 显示名)。
 * 注意: 元素配置项默认值已迁移至 {@link org.mods.gd656killicon.common.config.ElementConfigRegistry}
 * (声明式注册表), 本类只负责"哪些预设存在、含哪些元素、显示名"。
 */
public class DefaultConfigRegistry {
    private static final Map<String, java.util.Set<String>> OFFICIAL_PRESET_STRUCTURE = new HashMap<>();
    private static final Map<String, String> OFFICIAL_PRESET_NAMES = new HashMap<>();

    static {
        registerOfficialStructures();
        registerOfficialNames();
    }

    public static String getOfficialPresetDisplayName(String presetId) {
        String key = "gd656killicon.client.gui.config.preset.official." + presetId;
        if (I18n.exists(key)) {
            return I18n.get(key);
        }
        return OFFICIAL_PRESET_NAMES.getOrDefault(presetId, "");
    }

    private static void registerOfficialNames() {
        OFFICIAL_PRESET_NAMES.put("00001", "六五六自制预设");
        OFFICIAL_PRESET_NAMES.put("00002", "CF连杀图标模式");
        OFFICIAL_PRESET_NAMES.put("00003", "CS2卡片模式");
        OFFICIAL_PRESET_NAMES.put("00004", "Battlefield 1模式");
        OFFICIAL_PRESET_NAMES.put("00005", "Battlefield 4模式");
        OFFICIAL_PRESET_NAMES.put("00006", "PUBG淘汰字幕模式");
        OFFICIAL_PRESET_NAMES.put("00007", "Battlefield 5模式");
        OFFICIAL_PRESET_NAMES.put("00008", "三角洲行动：全面战场模式");
        OFFICIAL_PRESET_NAMES.put("00035", "Custom Preset (EN)");
        OFFICIAL_PRESET_NAMES.put("00036", "Battlefield 5 (EN)");
        OFFICIAL_PRESET_NAMES.put("00037", "Battlefield 6模式");
        for (ValorantStyleCatalog.StyleSpec definition : ValorantStyleCatalog.getDefinitions()) {
            OFFICIAL_PRESET_NAMES.put(definition.presetId(), "VALORANT " + definition.displayName());
        }
    }

    public static java.util.Set<String> getOfficialPresetElements(String presetId) {
        return OFFICIAL_PRESET_STRUCTURE.getOrDefault(presetId, java.util.Collections.emptySet());
    }
    
    public static boolean isOfficialPreset(String presetId) {
        return OFFICIAL_PRESET_STRUCTURE.containsKey(presetId);
    }
    
    public static java.util.Set<String> getOfficialPresetIds() {
        return OFFICIAL_PRESET_STRUCTURE.keySet();
    }

    private static void registerOfficialStructures() {
        java.util.Set<String> p00001 = new java.util.HashSet<>();
        p00001.add("subtitle/kill_feed");
        p00001.add("subtitle/score");
        p00001.add("subtitle/bonus_list");
        p00001.add("kill_icon/scrolling");
        OFFICIAL_PRESET_STRUCTURE.put("00001", p00001);

        java.util.Set<String> p00002 = new java.util.HashSet<>();
        p00002.add("subtitle/kill_feed");
        p00002.add("subtitle/score");
        p00002.add("subtitle/bonus_list");
        p00002.add("kill_icon/combo");
        OFFICIAL_PRESET_STRUCTURE.put("00002", p00002);

        java.util.Set<String> p00003 = new java.util.HashSet<>();
        p00003.add("kill_icon/card_bar");
        p00003.add("kill_icon/card");
        OFFICIAL_PRESET_STRUCTURE.put("00003", p00003);

        java.util.Set<String> p00004 = new java.util.HashSet<>();
        p00004.add("subtitle/score");
        p00004.add("subtitle/bonus_list");
        p00004.add("kill_icon/battlefield1");
        OFFICIAL_PRESET_STRUCTURE.put("00004", p00004);

        java.util.Set<String> p00005 = new java.util.HashSet<>();
        p00005.add("subtitle/score");
        p00005.add("subtitle/bonus_list");
        OFFICIAL_PRESET_STRUCTURE.put("00005", p00005);

        java.util.Set<String> p00006 = new java.util.HashSet<>();
        p00006.add("subtitle/combo");
        p00006.add("subtitle/kill_feed");
        OFFICIAL_PRESET_STRUCTURE.put("00006", p00006);

        java.util.Set<String> p00007 = new java.util.HashSet<>();
        p00007.add("subtitle/kill_feed");
        p00007.add("subtitle/score");
        p00007.add("subtitle/bonus_list");
        p00007.add("kill_icon/scrolling");
        OFFICIAL_PRESET_STRUCTURE.put("00007", p00007);

        java.util.Set<String> p00008 = new java.util.HashSet<>();
        p00008.add("subtitle/score");
        p00008.add("subtitle/bonus_list");
        p00008.add("kill_icon/scrolling");
        OFFICIAL_PRESET_STRUCTURE.put("00008", p00008);

        java.util.Set<String> p00035 = new java.util.HashSet<>();
        p00035.add("subtitle/kill_feed");
        p00035.add("subtitle/score");
        p00035.add("subtitle/bonus_list");
        p00035.add("kill_icon/scrolling");
        OFFICIAL_PRESET_STRUCTURE.put("00035", p00035);

        java.util.Set<String> p00036 = new java.util.HashSet<>();
        p00036.add("subtitle/kill_feed");
        p00036.add("subtitle/score");
        p00036.add("subtitle/bonus_list");
        p00036.add("kill_icon/scrolling");
        OFFICIAL_PRESET_STRUCTURE.put("00036", p00036);
        java.util.Set<String> p00037 = new java.util.HashSet<>();
        p00037.add("subtitle/kill_feed");
        p00037.add("subtitle/score");
        p00037.add("subtitle/bonus_list");
        p00037.add("kill_icon/scrolling");
        p00037.add("subtitle/hit_info");   // 命中信息仅属 BF6(00037)
        OFFICIAL_PRESET_STRUCTURE.put("00037", p00037);

        for (ValorantStyleCatalog.StyleSpec definition : ValorantStyleCatalog.getDefinitions()) {
            java.util.Set<String> valorantPreset = new java.util.HashSet<>();
            valorantPreset.add("kill_icon/valorant");
            OFFICIAL_PRESET_STRUCTURE.put(definition.presetId(), valorantPreset);
        }
    }
}
