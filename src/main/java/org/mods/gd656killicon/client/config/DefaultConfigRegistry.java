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
        OFFICIAL_PRESET_NAMES.put("00006", "PUBG淘汰字幕模式");
        OFFICIAL_PRESET_NAMES.put("00007", "Battlefield 5模式");
        OFFICIAL_PRESET_NAMES.put("00008", "三角洲行动：全面战场模式");
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
    }

    public static JsonObject getDefaultConfig(String presetId, String elementId) {
        JsonObject config;
        
        if (PRESET_OVERRIDES.containsKey(presetId) && PRESET_OVERRIDES.get(presetId).containsKey(elementId)) {
            config = PRESET_OVERRIDES.get(presetId).get(elementId).deepCopy();
        } else if (GLOBAL_DEFAULTS.containsKey(elementId)) {
            
            config = GLOBAL_DEFAULTS.get(elementId).deepCopy();
        } else {
            
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

    public static boolean isTranslatableKey(String key) {
        return key.startsWith("format_") || key.equals("kill_feed_format");
    }

    private static void registerDefaults() {
        
        

        
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
        killFeed.addProperty("color_normal_text", "#FFFFFF");
        killFeed.addProperty("enable_placeholder_bold", false);
        
        
        killFeed.addProperty("enable_normal_kill", true);
        killFeed.addProperty("enable_headshot_kill", true);
        killFeed.addProperty("enable_explosion_kill", true);
        killFeed.addProperty("enable_crit_kill", true);
        killFeed.addProperty("enable_assist_kill", true);
        killFeed.addProperty("enable_destroy_vehicle_kill", true);
        killFeed.addProperty("enable_scale_animation", true);

        
        killFeed.addProperty("color_normal_emphasis", "#FFFFFF");
        killFeed.addProperty("color_headshot_emphasis", "#FFFFFF");
        killFeed.addProperty("color_explosion_emphasis", "#FFFFFF");
        killFeed.addProperty("color_crit_emphasis", "#FFFFFF");
        killFeed.addProperty("color_assist_emphasis", "#FFFFFF");
        killFeed.addProperty("color_destroy_vehicle_emphasis", "#FFFFFF");

        
        killFeed.addProperty("enable_stacking", false);
        killFeed.addProperty("max_lines", 3);
        killFeed.addProperty("line_spacing", 12);

        registerGlobal("subtitle/kill_feed", killFeed);

        
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
        score.addProperty("color_normal_text", "#FFFFFF");
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
        bonusList.addProperty("format_locked_target", "gd656killicon.client.format.bonus_locked_target");
        bonusList.addProperty("format_hold_position", "gd656killicon.client.format.bonus_hold_position");
        bonusList.addProperty("format_charge_assault", "gd656killicon.client.format.bonus_charge_assault");
        bonusList.addProperty("format_fire_suppression", "gd656killicon.client.format.bonus_fire_suppression");
        bonusList.addProperty("format_destroy_block", "gd656killicon.client.format.bonus_destroy_block");
        bonusList.addProperty("format_spotting", "gd656killicon.client.format.bonus_spotting");
        bonusList.addProperty("format_spotting_kill", "gd656killicon.client.format.bonus_spotting_kill");
        bonusList.addProperty("format_spotting_team_assist", "gd656killicon.client.format.bonus_spotting_team_assist");
        bonusList.addProperty("format_kill_combo", "gd656killicon.client.format.bonus_combo");
        bonusList.addProperty("enable_special_streak_subtitles", false);
        bonusList.addProperty("enable_text_scrolling", false);
        bonusList.addProperty("text_scrolling_duration_multiplier", 1.2f);
        bonusList.addProperty("text_scrolling_refresh_rate", 0.02f);
        bonusList.addProperty("color_special_placeholder", "#D4B800");
        bonusList.addProperty("color_normal_text", "#FFFFFF");
        bonusList.addProperty("animation_duration", 0.5f);
        bonusList.addProperty("animation_refresh_rate", 0.01f);
        bonusList.addProperty("align_left", false);
        bonusList.addProperty("align_right", false);
        bonusList.addProperty("merge_window_duration", 0.5f);
        bonusList.addProperty("animation_speed", 7.0f); 
        
        
        
        
        
        
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

        
        JsonObject comboSubtitle = new JsonObject();
        comboSubtitle.addProperty("visible", true);
        comboSubtitle.addProperty("scale", 1.5);
        comboSubtitle.addProperty("x_offset", 0.0);
        comboSubtitle.addProperty("y_offset", 70.0);
        comboSubtitle.addProperty("color_kill_combo", "#FF3500");
        comboSubtitle.addProperty("color_assist_combo", "#FFD700");
        comboSubtitle.addProperty("format_kill_single", "\u003ccombo\u003e 淘汰");
        comboSubtitle.addProperty("format_kill_multi", "\u003ccombo\u003e 淘汰数");
        comboSubtitle.addProperty("format_assist_single", "\u003ccombo\u003e 助攻");
        comboSubtitle.addProperty("format_assist_multi", "\u003ccombo\u003e 助攻数");
        comboSubtitle.addProperty("enable_animation", true);
        comboSubtitle.addProperty("enable_light_effect", true);
        comboSubtitle.addProperty("enable_bold", true);
        comboSubtitle.addProperty("light_height", 10.0);
        comboSubtitle.addProperty("light_hold_duration", 0.0);
        comboSubtitle.addProperty("enable_scale_animation", false);
        comboSubtitle.addProperty("display_duration", 5.0);
        comboSubtitle.addProperty("reset_kill_combo", "death");
        comboSubtitle.addProperty("reset_assist_combo", "death");
        comboSubtitle.addProperty("combo_reset_timeout", 10.0);
        registerGlobal("subtitle/combo", comboSubtitle);

        
        JsonObject scrolling = new JsonObject();
        scrolling.addProperty("visible", true);
        scrolling.addProperty("scale", 0.4f);
        scrolling.addProperty("x_offset", 0);
        scrolling.addProperty("y_offset", 120);
        scrolling.addProperty("display_duration", 3.25f);
        scrolling.addProperty("enable_ring_effect_crit", true);
        scrolling.addProperty("enable_ring_effect_headshot", true);
        scrolling.addProperty("enable_ring_effect_explosion", true);
        scrolling.addProperty("animation_duration", 0.3f);
        scrolling.addProperty("fade_out_duration", 0.1f);
        scrolling.addProperty("position_animation_duration", 0.3f);
        scrolling.addProperty("start_scale", 2.0f);
        scrolling.addProperty("icon_spacing", 4);
        scrolling.addProperty("max_visible_icons", 7);
        scrolling.addProperty("display_interval_ms", 100);
        scrolling.addProperty("max_pending_icons", 30);
        scrolling.addProperty("ring_effect_crit_color", "#9CCC65");
        scrolling.addProperty("ring_effect_crit_radius", 42.0f);
        scrolling.addProperty("ring_effect_crit_thickness", 1.8f);
        scrolling.addProperty("ring_effect_headshot_color", "#D4B800");
        scrolling.addProperty("ring_effect_headshot_radius", 42.0f);
        scrolling.addProperty("ring_effect_headshot_thickness", 3.0f);
        scrolling.addProperty("ring_effect_explosion_color", "#F77F00");
        scrolling.addProperty("ring_effect_explosion_radius", 42.0f);
        scrolling.addProperty("ring_effect_explosion_thickness", 5.4f);
        injectTextureAnimationConfigs("kill_icon/scrolling", scrolling);
        injectTextureStyleConfigs("00001", "kill_icon/scrolling", scrolling);
        registerGlobal("kill_icon/scrolling", scrolling);

        
        JsonObject combo = new JsonObject();
        combo.addProperty("visible", true);
        combo.addProperty("scale", 0.6f);
        combo.addProperty("x_offset", 0);
        combo.addProperty("y_offset", 120);
        combo.addProperty("enable_ring_effect_crit", true);
        combo.addProperty("enable_ring_effect_headshot", true);
        combo.addProperty("enable_ring_effect_explosion", true);
        combo.addProperty("ring_effect_crit_color", "#9CCC65");
        combo.addProperty("ring_effect_crit_radius", 42.0f);
        combo.addProperty("ring_effect_crit_thickness", 1.8f);
        combo.addProperty("ring_effect_headshot_color", "#D4B800");
        combo.addProperty("ring_effect_headshot_radius", 42.0f);
        combo.addProperty("ring_effect_headshot_thickness", 3.0f);
        combo.addProperty("ring_effect_explosion_color", "#F77F00");
        combo.addProperty("ring_effect_explosion_radius", 42.0f);
        combo.addProperty("ring_effect_explosion_thickness", 5.4f);
        injectTextureAnimationConfigs("kill_icon/combo", combo);
        injectTextureStyleConfigs("00001", "kill_icon/combo", combo);
        registerGlobal("kill_icon/combo", combo);

        
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
        cardBar.addProperty("dynamic_card_style", true);
        cardBar.addProperty("animation_duration", 0.2f);
        injectTextureAnimationConfigs("kill_icon/card_bar", cardBar);
        injectTextureStyleConfigs("00001", "kill_icon/card_bar", cardBar);
        registerGlobal("kill_icon/card_bar", cardBar);

        
        JsonObject card = new JsonObject();
        card.addProperty("visible", true);
        card.addProperty("scale", 0.15f);
        card.addProperty("x_offset", 0);
        card.addProperty("y_offset", 35);
        card.addProperty("team", "ct");
        card.addProperty("dynamic_card_style", true);
        card.addProperty("animation_duration", 0.2f);
        card.addProperty("color_text_ct", "#9cc1eb");
        card.addProperty("color_text_t", "#d9ac5b");
        card.addProperty("text_scale", 10.0f);
        card.addProperty("max_stack_count", 6);
        injectTextureAnimationConfigs("kill_icon/card", card);
        injectTextureStyleConfigs("00001", "kill_icon/card", card);
        card.addProperty("anim_light_ct_texture_frame_width_ratio", 1);
        card.addProperty("anim_light_ct_texture_frame_height_ratio", 5);
        card.addProperty("anim_light_t_texture_frame_width_ratio", 1);
        card.addProperty("anim_light_t_texture_frame_height_ratio", 5);
        registerGlobal("kill_icon/card", card);

        
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
        injectTextureStyleConfigs("00001", "kill_icon/battlefield1", bf1);
        registerGlobal("kill_icon/battlefield1", bf1);

        

        
        JsonObject score00004 = score.deepCopy();
        score00004.addProperty("x_offset", 35);
        score00004.addProperty("y_offset", 86);
        score00004.addProperty("format_score", "+\u003cscore\u003e"); 
        score00004.addProperty("color_high_score", "#FFFFFF");
        score00004.addProperty("align_left", true);
        registerOverride("00004", "subtitle/score", score00004);

        JsonObject bonusList00004 = bonusList.deepCopy();
        bonusList00004.addProperty("x_offset", 30);
        bonusList00004.addProperty("y_offset", 90);
        bonusList00004.addProperty("align_right", true);
        bonusList00004.addProperty("animation_speed", 15.0f);
        registerOverride("00004", "subtitle/bonus_list", bonusList00004);

        
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

        
        JsonObject comboSubtitle00006 = comboSubtitle.deepCopy();
        comboSubtitle00006.addProperty("format_kill_single", "gd656killicon.client.format.preset_00006.combo.kill_single");
        comboSubtitle00006.addProperty("format_kill_multi", "gd656killicon.client.format.preset_00006.combo.kill_multi");
        comboSubtitle00006.addProperty("format_assist_single", "gd656killicon.client.format.preset_00006.combo.assist_single");
        comboSubtitle00006.addProperty("format_assist_multi", "gd656killicon.client.format.preset_00006.combo.assist_multi");
        comboSubtitle00006.addProperty("reset_kill_combo", "death");
        comboSubtitle00006.addProperty("reset_assist_combo", "death");
        registerOverride("00006", "subtitle/combo", comboSubtitle00006);

        JsonObject killFeed00006 = killFeed.deepCopy();
        killFeed00006.addProperty("y_offset", 88.0);
        killFeed00006.addProperty("display_duration", 5.0);
        killFeed00006.addProperty("format_normal", "gd656killicon.client.format.preset_00006.kill_feed.normal");
        killFeed00006.addProperty("color_normal_placeholder", "#FFFFFF");
        killFeed00006.addProperty("format_headshot", "gd656killicon.client.format.preset_00006.kill_feed.headshot");
        killFeed00006.addProperty("color_headshot_placeholder", "#FFFFFF");
        killFeed00006.addProperty("format_explosion", "gd656killicon.client.format.preset_00006.kill_feed.explosion");
        killFeed00006.addProperty("color_explosion_placeholder", "#FFFFFF");
        killFeed00006.addProperty("format_crit", "gd656killicon.client.format.preset_00006.kill_feed.crit");
        killFeed00006.addProperty("color_crit_placeholder", "#FFFFFF");
        killFeed00006.addProperty("format_assist", "gd656killicon.client.format.preset_00006.kill_feed.assist");
        killFeed00006.addProperty("color_assist_placeholder", "#FFFFFF");
        killFeed00006.addProperty("format_destroy_vehicle", "gd656killicon.client.format.preset_00006.kill_feed.destroy_vehicle");
        killFeed00006.addProperty("color_destroy_vehicle_placeholder", "#D4B800");
        killFeed00006.addProperty("enable_placeholder_bold", false);
        killFeed00006.addProperty("enable_scale_animation", false);
        killFeed00006.addProperty("enable_destroy_vehicle_kill", false);
        killFeed00006.addProperty("color_normal_emphasis", "#FF3500");
        killFeed00006.addProperty("color_headshot_emphasis", "#FF3500");
        killFeed00006.addProperty("color_explosion_emphasis", "#FF3500");
        killFeed00006.addProperty("color_crit_emphasis", "#FF3500");
        killFeed00006.addProperty("color_assist_emphasis", "#FFD700");
        killFeed00006.addProperty("color_destroy_vehicle_emphasis", "#FFFFFF");
        killFeed00006.addProperty("enable_stacking", true);
        killFeed00006.addProperty("max_lines", 5);
        registerOverride("00006", "subtitle/kill_feed", killFeed00006);

        JsonObject killFeed00007 = killFeed.deepCopy();
        killFeed00007.addProperty("x_offset", -1.0);
        killFeed00007.addProperty("y_offset", 103.0);
        killFeed00007.addProperty("format_normal", "<target> [<weapon>]+<score>");
        killFeed00007.addProperty("color_normal_placeholder", "#FFFFFF");
        killFeed00007.addProperty("format_headshot", "<target> [<weapon>]+<score>");
        killFeed00007.addProperty("color_headshot_placeholder", "#FFFFFF");
        killFeed00007.addProperty("format_explosion", "<target> [<weapon>]+<score>");
        killFeed00007.addProperty("color_explosion_placeholder", "#FFFFFF");
        killFeed00007.addProperty("format_crit", "<target> [<weapon>]+<score>");
        killFeed00007.addProperty("color_crit_placeholder", "#FFFFFF");
        killFeed00007.addProperty("format_assist", "助攻击杀 +<score>");
        killFeed00007.addProperty("color_assist_placeholder", "#FFFFFF");
        killFeed00007.addProperty("format_destroy_vehicle", "载具已摧毁 +<score>");
        killFeed00007.addProperty("color_destroy_vehicle_placeholder", "#FFFFFF");
        killFeed00007.addProperty("color_normal_emphasis", "#FFFFFF");
        killFeed00007.addProperty("color_headshot_emphasis", "#FFFFFF");
        killFeed00007.addProperty("color_explosion_emphasis", "#FFFFFF");
        killFeed00007.addProperty("color_crit_emphasis", "#FFFFFF");
        killFeed00007.addProperty("color_assist_emphasis", "#FFFFFF");
        killFeed00007.addProperty("color_destroy_vehicle_emphasis", "#FFFFFF");
        registerOverride("00007", "subtitle/kill_feed", killFeed00007);

        JsonObject score00007 = score.deepCopy();
        score00007.addProperty("y_offset", 90.0);
        score00007.addProperty("color_high_score", "#FFFFFF");
        score00007.addProperty("color_flash", "#FFFFFF");
        score00007.addProperty("enable_number_segmentation", true);
        score00007.addProperty("enable_flash", false);
        registerOverride("00007", "subtitle/score", score00007);

        JsonObject bonusList00007 = bonusList.deepCopy();
        bonusList00007.addProperty("y_offset", 76.0);
        bonusList00007.addProperty("line_spacing", 10);
        bonusList00007.addProperty("format_damage", "造成伤害 +<score>");
        bonusList00007.addProperty("format_kill", "击杀 +<score>");
        bonusList00007.addProperty("format_explosion_damage", "爆炸伤害 +<score>");
        bonusList00007.addProperty("format_headshot_damage", "爆头伤害 +<score>");
        bonusList00007.addProperty("format_crit_damage", "暴击伤害 +<score>");
        bonusList00007.addProperty("format_kill_explosion", "爆炸击杀 +<score>");
        bonusList00007.addProperty("format_kill_headshot", "精确击败 +<score>");
        bonusList00007.addProperty("format_kill_crit", "暴击击败 +<score>");
        bonusList00007.addProperty("format_combo", "<combo> 连续击败 +<score>");
        bonusList00007.addProperty("format_kill_long_distance", "远距离击败 <distance> +<score>");
        bonusList00007.addProperty("format_kill_invisible", "不见其人 +<score>");
        bonusList00007.addProperty("format_assist", "助攻 +<score>");
        bonusList00007.addProperty("format_desperate_counterattack", "绝境反击 +<score>");
        bonusList00007.addProperty("format_avenge", "一雪前耻 +<score>");
        bonusList00007.addProperty("format_shockwave", "冲击波 +<score>");
        bonusList00007.addProperty("format_blind_kill", "无睹而中 +<score>");
        bonusList00007.addProperty("format_buff_kill", "凭效诛敌 +<score>");
        bonusList00007.addProperty("format_debuff_kill", "逆效制敌 +<score>");
        bonusList00007.addProperty("format_both_buff_debuff_kill", "损益同斩 +<score>");
        bonusList00007.addProperty("format_last_bullet_kill", "末弹酬勇 +<score>");
        bonusList00007.addProperty("format_one_bullet_multi_kill", "一箭<multi_kill>雕 +<score>");
        bonusList00007.addProperty("format_seven_in_seven_out", "七进七出 +<score>");
        bonusList00007.addProperty("format_berserker", "狂战士 +<score>");
        bonusList00007.addProperty("format_interrupted_streak", "已中止敌方 <streak> 连杀 +<score>");
        bonusList00007.addProperty("format_leave_it_to_me", "交给我 +<score>");
        bonusList00007.addProperty("format_savior", "救星 +<score>");
        bonusList00007.addProperty("format_slay_the_leader", "枪打出头鸟 +<score>");
        bonusList00007.addProperty("format_purge", "肃清 +<score>");
        bonusList00007.addProperty("format_quick_switch", "切枪制人 +<score>");
        bonusList00007.addProperty("format_seize_opportunity", "机不可失 +<score>");
        bonusList00007.addProperty("format_bloodthirsty", "嗜血 +<score>");
        bonusList00007.addProperty("format_merciless", "无情 +<score>");
        bonusList00007.addProperty("format_valiant", "勇猛 +<score>");
        bonusList00007.addProperty("format_fierce", "凶狠 +<score>");
        bonusList00007.addProperty("format_savage", "野蛮 +<score>");
        bonusList00007.addProperty("format_potato_aim", "马枪怪 +<score>");
        bonusList00007.addProperty("format_locked_target", "锁定目标 +<score>");
        bonusList00007.addProperty("format_hold_position", "坚守阵地 +<score>");
        bonusList00007.addProperty("format_charge_assault", "冲锋陷阵 +<score>");
        bonusList00007.addProperty("format_fire_suppression", "火力压制 +<score>");
        bonusList00007.addProperty("format_destroy_block", "摧毁道具 +<score>");
        bonusList00007.addProperty("format_kill_combo", "<combo> 连续击败 +<score>");
        registerOverride("00007", "subtitle/bonus_list", bonusList00007);

        JsonObject scrolling00007 = scrolling.deepCopy();
        scrolling00007.addProperty("scale", 0.35f);
        scrolling00007.addProperty("y_offset", 118.0f);
        scrolling00007.addProperty("start_scale", 5.0f);
        scrolling00007.addProperty("icon_spacing", 1.0f);
        scrolling00007.addProperty("enable_ring_effect_crit", false);
        scrolling00007.addProperty("enable_ring_effect_explosion", false);
        scrolling00007.addProperty("ring_effect_headshot_color", "#F77F00");
        scrolling00007.addProperty("ring_effect_headshot_thickness", 5.0f);
        scrolling00007.addProperty("ring_effect_explosion_color", "#F77F00");
        scrolling00007.addProperty("ring_effect_explosion_thickness", 5.4f);
        scrolling00007.addProperty("ring_effect_crit_color", "#9CCC65");
        scrolling00007.addProperty("ring_effect_crit_thickness", 1.8f);
        scrolling00007.addProperty("anim_default_texture_frame_width_ratio", 1);
        scrolling00007.addProperty("anim_default_texture_frame_height_ratio", 1);
        scrolling00007.addProperty("anim_headshot_texture_frame_width_ratio", 1);
        scrolling00007.addProperty("anim_headshot_texture_frame_height_ratio", 1);
        scrolling00007.addProperty("anim_explosion_texture_frame_width_ratio", 1);
        scrolling00007.addProperty("anim_explosion_texture_frame_height_ratio", 1);
        scrolling00007.addProperty("anim_crit_texture_frame_width_ratio", 1);
        scrolling00007.addProperty("anim_crit_texture_frame_height_ratio", 1);
        scrolling00007.addProperty("anim_destroy_vehicle_texture_frame_width_ratio", 1);
        scrolling00007.addProperty("anim_destroy_vehicle_texture_frame_height_ratio", 1);
        scrolling00007.addProperty("anim_assist_texture_frame_width_ratio", 1);
        scrolling00007.addProperty("anim_assist_texture_frame_height_ratio", 1);
        injectTextureStyleConfigs("00007", "kill_icon/scrolling", scrolling00007);
        registerOverride("00007", "kill_icon/scrolling", scrolling00007);

        JsonObject score00008 = score00007.deepCopy();
        score00008.addProperty("y_offset", 92.0);
        score00008.addProperty("color_high_score", "#FFAE4B");
        score00008.addProperty("color_flash", "#D0D0D0");
        score00008.addProperty("enable_flash", false);
        score00008.addProperty("enable_number_segmentation", false);
        score00008.addProperty("enable_score_scaling_effect", false);
        score00008.addProperty("enable_digital_scroll", true);
        score00008.addProperty("enable_glow_effect", false);
        score00008.addProperty("glow_intensity", 0.5f);
        registerOverride("00008", "subtitle/score", score00008);

        JsonObject bonusList00008 = bonusList00007.deepCopy();
        bonusList00008.addProperty("y_offset", 75.0);
        bonusList00008.addProperty("line_spacing", 10);
        bonusList00008.addProperty("max_lines", 4);
        bonusList00008.addProperty("display_duration", 3.0f);
        bonusList00008.addProperty("fade_out_interval", 0.2f);
        bonusList00008.addProperty("format_damage", "造成伤害 +<score>");
        bonusList00008.addProperty("format_kill", "击杀 +<score>");
        bonusList00008.addProperty("format_explosion_damage", "爆炸伤害 +<score>");
        bonusList00008.addProperty("format_headshot_damage", "爆头伤害 +<score>");
        bonusList00008.addProperty("format_crit_damage", "暴击伤害 +<score>");
        bonusList00008.addProperty("format_kill_explosion", "爆炸击杀 +<score>");
        bonusList00008.addProperty("format_kill_headshot", "精确击败 +<score>");
        bonusList00008.addProperty("format_kill_crit", "暴击击败 +<score>");
        bonusList00008.addProperty("format_combo", "<combo> 连续击败 +<score>");
        bonusList00008.addProperty("format_kill_long_distance", "远距离击败 <distance> +<score>");
        bonusList00008.addProperty("format_kill_invisible", "不见其人 +<score>");
        bonusList00008.addProperty("format_assist", "助攻 +<score>");
        bonusList00008.addProperty("format_desperate_counterattack", "绝境反击 +<score>");
        bonusList00008.addProperty("format_avenge", "一雪前耻 +<score>");
        bonusList00008.addProperty("format_shockwave", "冲击波 +<score>");
        bonusList00008.addProperty("format_blind_kill", "无睹而中 +<score>");
        bonusList00008.addProperty("format_buff_kill", "凭效诛敌 +<score>");
        bonusList00008.addProperty("format_debuff_kill", "逆效制敌 +<score>");
        bonusList00008.addProperty("format_both_buff_debuff_kill", "损益同斩 +<score>");
        bonusList00008.addProperty("format_last_bullet_kill", "末弹酬勇 +<score>");
        bonusList00008.addProperty("format_one_bullet_multi_kill", "一箭<multi_kill>雕 +<score>");
        bonusList00008.addProperty("format_seven_in_seven_out", "七进七出 +<score>");
        bonusList00008.addProperty("format_berserker", "狂战士 +<score>");
        bonusList00008.addProperty("format_interrupted_streak", "已中止敌方 <streak> 连杀 +<score>");
        bonusList00008.addProperty("format_leave_it_to_me", "交给我 +<score>");
        bonusList00008.addProperty("format_savior", "救星 +<score>");
        bonusList00008.addProperty("format_slay_the_leader", "枪打出头鸟 +<score>");
        bonusList00008.addProperty("format_purge", "肃清 +<score>");
        bonusList00008.addProperty("format_quick_switch", "切枪制人 +<score>");
        bonusList00008.addProperty("format_seize_opportunity", "机不可失 +<score>");
        bonusList00008.addProperty("format_bloodthirsty", "嗜血 +<score>");
        bonusList00008.addProperty("format_merciless", "无情 +<score>");
        bonusList00008.addProperty("format_valiant", "勇猛 +<score>");
        bonusList00008.addProperty("format_fierce", "凶狠 +<score>");
        bonusList00008.addProperty("format_savage", "野蛮 +<score>");
        bonusList00008.addProperty("format_potato_aim", "马枪怪 +<score>");
        bonusList00008.addProperty("format_locked_target", "锁定目标 +<score>");
        bonusList00008.addProperty("format_hold_position", "坚守阵地 +<score>");
        bonusList00008.addProperty("format_charge_assault", "冲锋陷阵 +<score>");
        bonusList00008.addProperty("format_fire_suppression", "火力压制 +<score>");
        bonusList00008.addProperty("format_destroy_block", "摧毁道具 +<score>");
        bonusList00008.addProperty("format_spotting", "索敌 +<score>");
        bonusList00008.addProperty("format_spotting_kill", "标记击杀 +<score>");
        bonusList00008.addProperty("format_spotting_team_assist", "标记小队助攻 +<score>");
        bonusList00008.addProperty("format_kill_combo", "<combo> 连续击败 +<score>");
        bonusList00008.addProperty("enable_special_streak_subtitles", false);
        bonusList00008.addProperty("enable_text_scrolling", false);
        bonusList00008.addProperty("text_scrolling_duration_multiplier", 1.2f);
        bonusList00008.addProperty("text_scrolling_refresh_rate", 0.02f);
        bonusList00008.addProperty("color_special_placeholder", "#D4B800");
        bonusList00008.addProperty("animation_duration", 0.5f);
        bonusList00008.addProperty("animation_refresh_rate", 0.01f);
        bonusList00008.addProperty("align_left", false);
        bonusList00008.addProperty("align_right", false);
        bonusList00008.addProperty("merge_window_duration", 1.0f);
        bonusList00008.addProperty("animation_speed", 8.0f);
        bonusList00008.addProperty("enable_text_sweep_animation", false);
        bonusList00008.addProperty("enter_animation_duration", 0.2f);
        bonusList00008.addProperty("kill_bonus_scale", 1.0f);
        bonusList00008.addProperty("enable_kill_feed", false);
        bonusList00008.addProperty("kill_feed_format", "[<weapon>] <target> +<score>");
        bonusList00008.addProperty("kill_feed_victim_color", "#FF0000");
        bonusList00008.addProperty("enable_digital_scroll", true);
        bonusList00008.addProperty("enable_glow_effect", false);
        bonusList00008.addProperty("glow_intensity", 0.5f);
        registerOverride("00008", "subtitle/bonus_list", bonusList00008);

        JsonObject scrolling00008 = scrolling00007.deepCopy();
        scrolling00008.addProperty("scale", 0.32f);
        scrolling00008.addProperty("y_offset", 107.0f);
        scrolling00008.addProperty("display_duration", 3.25f);
        scrolling00008.addProperty("enable_ring_effect_crit", false);
        scrolling00008.addProperty("enable_ring_effect_headshot", true);
        scrolling00008.addProperty("enable_ring_effect_explosion", false);
        scrolling00008.addProperty("animation_duration", 0.3f);
        scrolling00008.addProperty("fade_out_duration", 0.1f);
        scrolling00008.addProperty("position_animation_duration", 0.3f);
        scrolling00008.addProperty("start_scale", 4.0f);
        scrolling00008.addProperty("icon_spacing", 1.0f);
        scrolling00008.addProperty("max_visible_icons", 7);
        scrolling00008.addProperty("display_interval_ms", 100);
        scrolling00008.addProperty("max_pending_icons", 30);
        scrolling00008.addProperty("ring_effect_crit_color", "#9CCC65");
        scrolling00008.addProperty("ring_effect_crit_radius", 42.0f);
        scrolling00008.addProperty("ring_effect_crit_thickness", 1.8f);
        scrolling00008.addProperty("ring_effect_headshot_color", "#FFAE4B");
        scrolling00008.addProperty("ring_effect_headshot_radius", 42.0f);
        scrolling00008.addProperty("ring_effect_headshot_thickness", 3.0f);
        scrolling00008.addProperty("ring_effect_explosion_color", "#FFFFFF");
        scrolling00008.addProperty("ring_effect_explosion_radius", 42.0f);
        scrolling00008.addProperty("ring_effect_explosion_thickness", 5.4f);
        injectTextureStyleConfigs("00008", "kill_icon/scrolling", scrolling00008);
        registerOverride("00008", "kill_icon/scrolling", scrolling00008);
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
            config.addProperty(prefix + "texture_animation_orientation", "vertical");
            config.addProperty(prefix + "texture_animation_loop", false);
            config.addProperty(prefix + "texture_animation_play_style", "sequential");
            config.addProperty(prefix + "texture_frame_width_ratio", 1);
            config.addProperty(prefix + "texture_frame_height_ratio", 1);
        }
    }

    private static void injectTextureStyleConfigs(String presetId, String elementId, JsonObject config) {
        if (!ElementTextureDefinition.hasTextures(elementId)) return;

        for (String texture : ElementTextureDefinition.getTextures(elementId)) {
            String key = ElementTextureDefinition.getTextureStyleKey(texture);
            String fileName = "kill_icon/scrolling".equals(elementId)
                ? resolveScrollingStyleFileName(presetId, texture)
                : ElementTextureDefinition.getTextureFileName(presetId, elementId, texture);
            if (fileName != null) {
                config.addProperty(key, fileName);
            }
        }
    }

    private static String resolveScrollingStyleFileName(String presetId, String textureKey) {
        if ("00007".equals(presetId)) {
            return switch (textureKey) {
                case "headshot" -> "killicon_battlefield5_headshot.png";
                case "assist" -> "killicon_battlefield5_assist.png";
                case "destroy_vehicle" -> "killicon_battlefield5_destroyvehicle.png";
                case "explosion", "crit", "default" -> "killicon_battlefield5_default.png";
                default -> "killicon_battlefield5_default.png";
            };
        }
        if ("00008".equals(presetId)) {
            return switch (textureKey) {
                case "headshot" -> "killicon_df_headshot.png";
                case "destroy_vehicle" -> "killicon_df_destroyvehicle.png";
                case "assist" -> "killicon_scrolling_assist.png";
                case "explosion", "crit", "default" -> "killicon_df_default.png";
                default -> "killicon_df_default.png";
            };
        }
        return ElementTextureDefinition.getTextureFileName("00001", "kill_icon/scrolling", textureKey);
    }
}
