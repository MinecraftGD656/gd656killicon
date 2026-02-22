package org.mods.gd656killicon.client.config;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.resources.language.I18n;

public class DefaultConfigRegistry {
    private static final Map<String, JsonObject> GLOBAL_DEFAULTS = new HashMap<>();
    private static final Map<String, Map<String, JsonObject>> PRESET_OVERRIDES = new HashMap<>();

    private static final Map<String, java.util.Set<String>> OFFICIAL_PRESET_STRUCTURE = new HashMap<>();
    private static final Map<String, String> OFFICIAL_PRESET_NAMES = new HashMap<>();

    static {
        registerDefaults();
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
        OFFICIAL_PRESET_NAMES.put("00003", "CS2卡牌模式");
        OFFICIAL_PRESET_NAMES.put("00004", "Battlefield 1模式");
        OFFICIAL_PRESET_NAMES.put("00005", "Battlefield 4模式");
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

    public static java.util.Set<String> getAllElementTypes() {
        return new java.util.HashSet<>(GLOBAL_DEFAULTS.keySet());
    }

    private static void registerOfficialStructures() {
        // 00001
        java.util.Set<String> p00001 = new java.util.HashSet<>();
        p00001.add("subtitle/kill_feed");
        p00001.add("subtitle/score");
        p00001.add("subtitle/bonus_list");
        p00001.add("kill_icon/scrolling");
        OFFICIAL_PRESET_STRUCTURE.put("00001", p00001);

        // 00002
        java.util.Set<String> p00002 = new java.util.HashSet<>();
        p00002.add("subtitle/kill_feed");
        p00002.add("subtitle/score");
        p00002.add("subtitle/bonus_list");
        p00002.add("kill_icon/combo");
        OFFICIAL_PRESET_STRUCTURE.put("00002", p00002);

        // 00003
        java.util.Set<String> p00003 = new java.util.HashSet<>();
        p00003.add("kill_icon/card_bar");
        p00003.add("kill_icon/card");
        OFFICIAL_PRESET_STRUCTURE.put("00003", p00003);

        // 00004
        java.util.Set<String> p00004 = new java.util.HashSet<>();
        p00004.add("subtitle/score");
        p00004.add("subtitle/bonus_list");
        p00004.add("kill_icon/battlefield1");
        OFFICIAL_PRESET_STRUCTURE.put("00004", p00004);

        // 00005
        java.util.Set<String> p00005 = new java.util.HashSet<>();
        p00005.add("subtitle/score");
        p00005.add("subtitle/bonus_list");
        OFFICIAL_PRESET_STRUCTURE.put("00005", p00005);
    }

    public static JsonObject getDefaultConfig(String presetId, String elementId) {
        JsonObject config;
        // 1. Check for preset-specific override
        if (PRESET_OVERRIDES.containsKey(presetId) && PRESET_OVERRIDES.get(presetId).containsKey(elementId)) {
            config = PRESET_OVERRIDES.get(presetId).get(elementId).deepCopy();
        } else if (GLOBAL_DEFAULTS.containsKey(elementId)) {
            // 2. Return global default
            config = GLOBAL_DEFAULTS.get(elementId).deepCopy();
        } else {
            // 3. Fallback (Empty)
            config = new JsonObject();
        }
        
        localizeConfig(config);
        return config;
    }
    
    public static JsonObject getGlobalDefault(String elementId) {
        JsonObject config;
        if (GLOBAL_DEFAULTS.containsKey(elementId)) {
            config = GLOBAL_DEFAULTS.get(elementId).deepCopy();
        } else {
            config = new JsonObject();
        }
        
        localizeConfig(config);
        return config;
    }

    private static void localizeConfig(JsonObject config) {
        for (String key : config.keySet()) {
            if (isTranslatableKey(key)) {
                com.google.gson.JsonElement element = config.get(key);
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    String value = element.getAsString();
                    if (net.minecraft.client.resources.language.I18n.exists(value)) {
                         config.addProperty(key, net.minecraft.client.resources.language.I18n.get(value));
                    }
                }
            }
        }
    }

    private static boolean isTranslatableKey(String key) {
        return key.startsWith("format_") || key.equals("kill_feed_format");
    }

    private static void registerDefaults() {
        // --- 1. Global Defaults (Base/First Default) ---
        // Derived from 00001 (or the primary preset for that element)

        // subtitle/kill_feed (Base: 00001)
        JsonObject killFeed = new JsonObject();
        killFeed.addProperty("visible", true);
        killFeed.addProperty("scale", 1.0);
        killFeed.addProperty("x_offset", 0);
        killFeed.addProperty("y_offset", 100);
        killFeed.addProperty("display_duration", 3.0);
        killFeed.addProperty("format_normal", "gd656killicon.client.format.normal");
        killFeed.addProperty("color_normal_placeholder", "#008B8B");
        killFeed.addProperty("format_headshot", "gd656killicon.client.format.headshot");
        killFeed.addProperty("color_headshot_placeholder", "#D4B800");
        killFeed.addProperty("format_explosion", "gd656killicon.client.format.explosion");
        killFeed.addProperty("color_explosion_placeholder", "#F77F00");
        killFeed.addProperty("format_crit", "gd656killicon.client.format.crit");
        killFeed.addProperty("color_crit_placeholder", "#9CCC65");
        killFeed.addProperty("format_assist", "gd656killicon.client.format.assist");
        killFeed.addProperty("color_assist_placeholder", "#008B8B");
        killFeed.addProperty("format_destroy_vehicle", "gd656killicon.client.format.destroy_vehicle");
        killFeed.addProperty("color_destroy_vehicle_placeholder", "#D4B800");
        killFeed.addProperty("enable_placeholder_bold", false);
        registerGlobal("subtitle/kill_feed", killFeed);

        // subtitle/score (Base: 00001)
        JsonObject score = new JsonObject();
        score.addProperty("visible", true);
        score.addProperty("scale", 2.0);
        score.addProperty("x_offset", 0);
        score.addProperty("y_offset", 80);
        score.addProperty("display_duration", 4.0);
        score.addProperty("format_score", "\u003cscore\u003e");
        score.addProperty("score_threshold", 1000);
        score.addProperty("color_high_score", "#D4B800");
        score.addProperty("color_flash", "#D0D0D0");
        score.addProperty("animation_duration", 1.25);
        score.addProperty("animation_refresh_rate", 0.01);
        score.addProperty("enable_number_segmentation", false);
        score.addProperty("enable_flash", true);
        score.addProperty("align_left", false);
        score.addProperty("align_right", false);
        score.addProperty("enable_score_scaling_effect", false);
        score.addProperty("enable_digital_scroll", true);
        score.addProperty("enable_glow_effect", false);
        score.addProperty("glow_intensity", 0.5);
        registerGlobal("subtitle/score", score);

        // subtitle/bonus_list (Base: 00001)
        JsonObject bonusList = new JsonObject();
        bonusList.addProperty("visible", true);
        bonusList.addProperty("scale", 1.0);
        bonusList.addProperty("x_offset", 0);
        bonusList.addProperty("y_offset", 65);
        bonusList.addProperty("line_spacing", 12);
        bonusList.addProperty("max_lines", 4);
        bonusList.addProperty("display_duration", 3.0);
        bonusList.addProperty("fade_out_interval", 0.2);
        bonusList.addProperty("format_damage", "gd656killicon.client.format.bonus_damage");
        bonusList.addProperty("format_kill", "gd656killicon.client.format.bonus_kill");
        bonusList.addProperty("format_explosion_damage", "gd656killicon.client.format.bonus_explosion");
        bonusList.addProperty("format_headshot_damage", "gd656killicon.client.format.bonus_headshot");
        bonusList.addProperty("format_crit_damage", "gd656killicon.client.format.bonus_crit");
        bonusList.addProperty("format_kill_explosion", "gd656killicon.client.format.bonus_kill_explosion");
        bonusList.addProperty("format_kill_headshot", "gd656killicon.client.format.bonus_kill_headshot");
        bonusList.addProperty("format_kill_crit", "gd656killicon.client.format.bonus_kill_crit");
        bonusList.addProperty("format_combo", "gd656killicon.client.format.bonus_combo");
        bonusList.addProperty("format_kill_long_distance", "gd656killicon.client.format.bonus_kill_long_distance");
        bonusList.addProperty("format_kill_invisible", "gd656killicon.client.format.bonus_kill_invisible");
        bonusList.addProperty("format_assist", "gd656killicon.client.format.bonus_assist");
        bonusList.addProperty("format_desperate_counterattack", "gd656killicon.client.format.bonus_desperate_counterattack");
        bonusList.addProperty("format_avenge", "gd656killicon.client.format.bonus_avenge");
        bonusList.addProperty("format_shockwave", "gd656killicon.client.format.bonus_shockwave");
        bonusList.addProperty("format_blind_kill", "gd656killicon.client.format.bonus_blind_kill");
        bonusList.addProperty("format_buff_kill", "gd656killicon.client.format.bonus_buff_kill");
        bonusList.addProperty("format_debuff_kill", "gd656killicon.client.format.bonus_debuff_kill");
        bonusList.addProperty("format_both_buff_debuff_kill", "gd656killicon.client.format.bonus_both_buff_debuff_kill");
        bonusList.addProperty("format_last_bullet_kill", "gd656killicon.client.format.bonus_last_bullet_kill");
        bonusList.addProperty("format_one_bullet_multi_kill", "gd656killicon.client.format.bonus_one_bullet_multi_kill");
        bonusList.addProperty("format_seven_in_seven_out", "gd656killicon.client.format.bonus_seven_in_seven_out");
        bonusList.addProperty("format_berserker", "gd656killicon.client.format.bonus_berserker");
        bonusList.addProperty("format_interrupted_streak", "gd656killicon.client.format.bonus_interrupted_streak");
        bonusList.addProperty("format_leave_it_to_me", "gd656killicon.client.format.bonus_leave_it_to_me");
        bonusList.addProperty("format_savior", "gd656killicon.client.format.bonus_savior");
        bonusList.addProperty("format_slay_the_leader", "gd656killicon.client.format.bonus_slay_the_leader");
        bonusList.addProperty("format_purge", "gd656killicon.client.format.bonus_purge");
        bonusList.addProperty("format_quick_switch", "gd656killicon.client.format.bonus_quick_switch");
        bonusList.addProperty("format_seize_opportunity", "gd656killicon.client.format.bonus_seize_opportunity");
        bonusList.addProperty("format_bloodthirsty", "gd656killicon.client.format.bonus_bloodthirsty");
        bonusList.addProperty("format_merciless", "gd656killicon.client.format.bonus_merciless");
        bonusList.addProperty("format_valiant", "gd656killicon.client.format.bonus_valiant");
        bonusList.addProperty("format_fierce", "gd656killicon.client.format.bonus_fierce");
        bonusList.addProperty("format_savage", "gd656killicon.client.format.bonus_savage");
        bonusList.addProperty("format_potato_aim", "gd656killicon.client.format.bonus_potato_aim");
        bonusList.addProperty("format_kill_combo", "gd656killicon.client.format.bonus_combo");
        bonusList.addProperty("enable_special_streak_subtitles", false);
        bonusList.addProperty("enable_text_scrolling", false);
        bonusList.addProperty("text_scrolling_duration_multiplier", 1.2f);
        bonusList.addProperty("text_scrolling_refresh_rate", 0.02f);
        bonusList.addProperty("color_special_placeholder", "#D4B800");
        bonusList.addProperty("animation_duration", 0.5f);
        bonusList.addProperty("animation_refresh_rate", 0.01f);
        bonusList.addProperty("align_left", false);
        bonusList.addProperty("align_right", false);
        bonusList.addProperty("merge_window_duration", 0.5f);
        bonusList.addProperty("animation_speed", 7.0f); // Note: JSON said 10.0 in 00002/00001? Wait.
        // 00001 JSON snippet: "animation_speed": 10.0 ??
        // Let me recheck the JSON content I read.
        // 00002 bonus_list: "animation_speed": 10.0
        // 00001 bonus_list: "animation_speed": 10.0
        // But ElementConfigManager.java had 7.0f in createBonusListDefaults().
        // I should use the JSON value (10.0) as the new source of truth for the global default if it matches 00001.
        bonusList.addProperty("animation_speed", 10.0f);
        
        bonusList.addProperty("enable_text_sweep_animation", false);
        bonusList.addProperty("enter_animation_duration", 0.2f);
        bonusList.addProperty("kill_bonus_scale", 1.0f);
        bonusList.addProperty("enable_kill_feed", false);
        bonusList.addProperty("kill_feed_format", "[\u003cweapon\u003e] \u003ctarget\u003e +\u003cscore\u003e");
        bonusList.addProperty("kill_feed_victim_color", "#FF0000");
        bonusList.addProperty("enable_digital_scroll", true);
        bonusList.addProperty("enable_glow_effect", false);
        bonusList.addProperty("glow_intensity", 0.5f);
        registerGlobal("subtitle/bonus_list", bonusList);

        // kill_icon/scrolling (Base: 00001)
        JsonObject scrolling = new JsonObject();
        scrolling.addProperty("visible", true);
        scrolling.addProperty("scale", 0.4f);
        scrolling.addProperty("x_offset", 0);
        scrolling.addProperty("y_offset", 120);
        scrolling.addProperty("display_duration", 3.25f);
        scrolling.addProperty("enable_icon_effect", true);
        scrolling.addProperty("animation_duration", 0.3f);
        scrolling.addProperty("fade_out_duration", 0.1f);
        scrolling.addProperty("position_animation_duration", 0.3f);
        scrolling.addProperty("start_scale", 2.0f);
        scrolling.addProperty("icon_spacing", 4);
        scrolling.addProperty("max_visible_icons", 7);
        scrolling.addProperty("display_interval_ms", 100);
        scrolling.addProperty("max_pending_icons", 30);
        injectTextureAnimationConfigs("kill_icon/scrolling", scrolling);
        registerGlobal("kill_icon/scrolling", scrolling);

        // kill_icon/combo (Base: 00002)
        JsonObject combo = new JsonObject();
        combo.addProperty("visible", true);
        combo.addProperty("scale", 0.6f);
        combo.addProperty("x_offset", 0);
        combo.addProperty("y_offset", 120);
        combo.addProperty("enable_icon_effect", true);
        injectTextureAnimationConfigs("kill_icon/combo", combo);
        registerGlobal("kill_icon/combo", combo);

        // kill_icon/card_bar (Base: 00003)
        JsonObject cardBar = new JsonObject();
        cardBar.addProperty("visible", true);
        cardBar.addProperty("scale", 1.0f);
        cardBar.addProperty("x_offset", 0);
        cardBar.addProperty("y_offset", 40);
        cardBar.addProperty("team", "ct");
        cardBar.addProperty("show_light", true);
        cardBar.addProperty("light_width", 350.0f);
        cardBar.addProperty("light_height", 1.0f);
        cardBar.addProperty("color_light_ct", "#9cc1eb");
        cardBar.addProperty("color_light_t", "#d9ac5b");
        cardBar.addProperty("dynamic_card_style", false);
        cardBar.addProperty("animation_duration", 0.2f);
        injectTextureAnimationConfigs("kill_icon/card_bar", cardBar);
        registerGlobal("kill_icon/card_bar", cardBar);

        // kill_icon/card (Base: 00003)
        JsonObject card = new JsonObject();
        card.addProperty("visible", true);
        card.addProperty("scale", 0.15f);
        card.addProperty("x_offset", 0);
        card.addProperty("y_offset", 35);
        card.addProperty("team", "ct");
        card.addProperty("dynamic_card_style", false);
        card.addProperty("animation_duration", 0.2f);
        card.addProperty("color_text_ct", "#9cc1eb");
        card.addProperty("color_text_t", "#d9ac5b");
        card.addProperty("text_scale", 10.0f);
        card.addProperty("max_stack_count", 6);
        injectTextureAnimationConfigs("kill_icon/card", card);
        card.addProperty("anim_light_ct_texture_frame_width_ratio", 1);
        card.addProperty("anim_light_ct_texture_frame_height_ratio", 5);
        card.addProperty("anim_light_t_texture_frame_width_ratio", 1);
        card.addProperty("anim_light_t_texture_frame_height_ratio", 5);
        registerGlobal("kill_icon/card", card);

        // kill_icon/battlefield1 (Base: 00004)
        JsonObject bf1 = new JsonObject();
        bf1.addProperty("visible", true);
        bf1.addProperty("icon_size", 25);
        bf1.addProperty("border_size", 3);
        bf1.addProperty("x_offset", 0);
        bf1.addProperty("y_offset", 100);
        bf1.addProperty("background_color", "#000000");
        bf1.addProperty("icon_box_opacity", 80);
        bf1.addProperty("text_box_opacity", 90);
        bf1.addProperty("scale_weapon", 1.0f);
        bf1.addProperty("scale_victim", 1.2f);
        bf1.addProperty("scale_health", 1.5f);
        bf1.addProperty("color_victim", "#FF0000");
        bf1.addProperty("animation_duration", 0.2f);
        bf1.addProperty("display_duration", 4.5f);
        injectTextureAnimationConfigs("kill_icon/battlefield1", bf1);
        registerGlobal("kill_icon/battlefield1", bf1);

        // --- 2. Preset Overrides (Second/Third Defaults) ---

        // 00004 Overrides
        JsonObject score00004 = score.deepCopy();
        score00004.addProperty("x_offset", 35);
        score00004.addProperty("y_offset", 86);
        score00004.addProperty("format_score", "+\u003cscore\u003e"); // +<score>
        score00004.addProperty("color_high_score", "#FFFFFF");
        score00004.addProperty("align_left", true);
        registerOverride("00004", "subtitle/score", score00004);

        JsonObject bonusList00004 = bonusList.deepCopy();
        bonusList00004.addProperty("x_offset", 30);
        bonusList00004.addProperty("y_offset", 90);
        bonusList00004.addProperty("align_right", true);
        bonusList00004.addProperty("animation_speed", 15.0f);
        registerOverride("00004", "subtitle/bonus_list", bonusList00004);

        // 00005 Overrides
        JsonObject score00005 = score.deepCopy();
        score00005.addProperty("x_offset", 30);
        score00005.addProperty("y_offset", 80);
        score00005.addProperty("color_high_score", "#FFFFFF");
        score00005.addProperty("enable_flash", false);
        score00005.addProperty("align_left", true);
        score00005.addProperty("enable_score_scaling_effect", true);
        score00005.addProperty("enable_digital_scroll", false);
        score00005.addProperty("enable_glow_effect", true);
        score00005.addProperty("glow_intensity", 0.3f);
        score00005.addProperty("display_duration", 4.5f);
        registerOverride("00005", "subtitle/score", score00005);

        JsonObject bonusList00005 = bonusList.deepCopy();
        bonusList00005.addProperty("x_offset", 20);
        bonusList00005.addProperty("y_offset", 80);
        bonusList00005.addProperty("max_lines", 5);
        bonusList00005.addProperty("align_right", true);
        bonusList00005.addProperty("enable_text_sweep_animation", true);
        bonusList00005.addProperty("animation_speed", 40.0f);
        bonusList00005.addProperty("enable_kill_feed", true);
        bonusList00005.addProperty("enable_digital_scroll", false);
        bonusList00005.addProperty("enable_glow_effect", true);
        bonusList00005.addProperty("glow_intensity", 0.3f);
        bonusList00005.addProperty("kill_bonus_scale", 1.2f);
        registerOverride("00005", "subtitle/bonus_list", bonusList00005);
    }

    private static void registerGlobal(String elementId, JsonObject config) {
        GLOBAL_DEFAULTS.put(elementId, config);
    }

    private static void registerOverride(String presetId, String elementId, JsonObject config) {
        PRESET_OVERRIDES.computeIfAbsent(presetId, k -> new HashMap<>()).put(elementId, config);
    }

    private static void injectTextureAnimationConfigs(String elementId, JsonObject config) {
        if (!ElementTextureDefinition.hasTextures(elementId)) return;
        
        for (String texture : ElementTextureDefinition.getTextures(elementId)) {
            String prefix = "anim_" + texture + "_";
            
            config.addProperty(prefix + "enable_texture_animation", false);
            config.addProperty(prefix + "texture_animation_total_frames", 1);
            config.addProperty(prefix + "texture_animation_interval_ms", 100);
            config.addProperty(prefix + "texture_animation_orientation", "horizontal");
            config.addProperty(prefix + "texture_animation_loop", false);
            config.addProperty(prefix + "texture_animation_play_style", "sequential");
            config.addProperty(prefix + "texture_frame_width_ratio", 1);
            config.addProperty(prefix + "texture_frame_height_ratio", 1);
        }
    }
}
