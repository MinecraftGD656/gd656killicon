package org.mods.gd656killicon.common.config;

import java.util.ArrayList;

import java.util.List;

import static org.mods.gd656killicon.common.config.ConfigType.*;

/**
 * 声明式配置项定义区(仿 BonusDefinitionsArea)。
 * 静态键在此声明; 动态键(format_<ID> / anim_<texture>_* / texture_*_<texture>)由外部注册。
 */

public final class ElementConfigDefinitionsArea {

    private ElementConfigDefinitionsArea() {}

    public static void register() {

        ElementConfigRegistry.register(subtitleKillFeed());

        ElementConfigRegistry.register(subtitleScore());

        ElementConfigRegistry.register(subtitleBonusList());

        ElementConfigRegistry.register(subtitleCombo());

        ElementConfigRegistry.register(subtitleHitInfo());

        ElementConfigRegistry.register(killIconScrolling());

        ElementConfigRegistry.register(killIconCombo());

        ElementConfigRegistry.register(killIconValorant());

        ElementConfigRegistry.register(killIconCard());

        ElementConfigRegistry.register(killIconCardBar());

        ElementConfigRegistry.register(killIconBattlefield1());

        ElementConfigRegistry.register(killIconHonor());

        // 纹理动画键/纹理选择键(动态, 按纹理清单)

        ElementConfigRegistry.registerTextureKeys("kill_icon/scrolling");

        ElementConfigRegistry.registerTextureKeys("kill_icon/combo");

        ElementConfigRegistry.registerTextureKeys("kill_icon/valorant");
        // valorant 粒子层/刀片动画默认值(覆盖通用纹理模板, 同旧 applyValorantParticleAnimationDefaults)
        ElementConfigRegistry.register(
                ConfigKeyDefinition.builder().key("anim_base_particle_enable_texture_animation").element("kill_icon/valorant").type(BOOLEAN).category(Category.TEXTURE).textureTab("base_particle").defaultValue(true).build(),
                ConfigKeyDefinition.builder().key("anim_base_particle_texture_animation_total_frames").element("kill_icon/valorant").type(INT).category(Category.TEXTURE).textureTab("base_particle").defaultValue(49).build(),
                ConfigKeyDefinition.builder().key("anim_base_particle_texture_animation_interval_ms").element("kill_icon/valorant").type(INT).category(Category.TEXTURE).textureTab("base_particle").defaultValue(25).build(),
                ConfigKeyDefinition.builder().key("anim_hero_flame_enable_texture_animation").element("kill_icon/valorant").type(BOOLEAN).category(Category.TEXTURE).textureTab("hero_flame").defaultValue(true).build(),
                ConfigKeyDefinition.builder().key("anim_hero_flame_texture_animation_total_frames").element("kill_icon/valorant").type(INT).category(Category.TEXTURE).textureTab("hero_flame").defaultValue(20).build(),
                ConfigKeyDefinition.builder().key("anim_hero_flame_texture_animation_interval_ms").element("kill_icon/valorant").type(INT).category(Category.TEXTURE).textureTab("hero_flame").defaultValue(29).build(),
                ConfigKeyDefinition.builder().key("anim_large_sparks_enable_texture_animation").element("kill_icon/valorant").type(BOOLEAN).category(Category.TEXTURE).textureTab("large_sparks").defaultValue(true).build(),
                ConfigKeyDefinition.builder().key("anim_large_sparks_texture_animation_total_frames").element("kill_icon/valorant").type(INT).category(Category.TEXTURE).textureTab("large_sparks").defaultValue(52).build(),
                ConfigKeyDefinition.builder().key("anim_large_sparks_texture_animation_interval_ms").element("kill_icon/valorant").type(INT).category(Category.TEXTURE).textureTab("large_sparks").defaultValue(25).build(),
                ConfigKeyDefinition.builder().key("anim_x_sparks_enable_texture_animation").element("kill_icon/valorant").type(BOOLEAN).category(Category.TEXTURE).textureTab("x_sparks").defaultValue(true).build(),
                ConfigKeyDefinition.builder().key("anim_x_sparks_texture_animation_total_frames").element("kill_icon/valorant").type(INT).category(Category.TEXTURE).textureTab("x_sparks").defaultValue(29).build(),
                ConfigKeyDefinition.builder().key("anim_x_sparks_texture_animation_interval_ms").element("kill_icon/valorant").type(INT).category(Category.TEXTURE).textureTab("x_sparks").defaultValue(25).build(),
                ConfigKeyDefinition.builder().key("anim_base_particle_texture_y_offset").element("kill_icon/valorant").type(INT).category(Category.TEXTURE).textureTab("base_particle").defaultValue(45).build(),
                ConfigKeyDefinition.builder().key("anim_blade_texture_scale").element("kill_icon/valorant").type(FLOAT).category(Category.TEXTURE).textureTab("blade").defaultValue(0.60f).build()
        );

        ElementConfigRegistry.registerTextureKeys("kill_icon/card");

        ElementConfigRegistry.registerTextureKeys("kill_icon/card_bar");

        ElementConfigRegistry.registerTextureKeys("kill_icon/battlefield1");

    }

    private static List<ConfigKeyDefinition> subtitleKillFeed() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("subtitle/kill_feed").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("subtitle/kill_feed").type(FLOAT).category(Category.POSITION).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("subtitle/kill_feed").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("subtitle/kill_feed").type(INT).category(Category.POSITION).defaultValue(100).build());
        list.add(ConfigKeyDefinition.builder().key("display_duration").element("subtitle/kill_feed").type(FLOAT).category(Category.TIMING).defaultValue(3.0f).build());
        list.add(ConfigKeyDefinition.builder().key("color_normal_placeholder").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_normal_kill").defaultValue("#008B8B").build());
        list.add(ConfigKeyDefinition.builder().key("color_headshot_placeholder").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_headshot_kill").defaultValue("#D4B800").build());
        list.add(ConfigKeyDefinition.builder().key("color_explosion_placeholder").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_explosion_kill").defaultValue("#F77F00").build());
        list.add(ConfigKeyDefinition.builder().key("color_crit_placeholder").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_crit_kill").defaultValue("#9CCC65").build());
        list.add(ConfigKeyDefinition.builder().key("color_assist_placeholder").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_assist_kill").defaultValue("#008B8B").build());
        list.add(ConfigKeyDefinition.builder().key("color_destroy_vehicle_placeholder").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_destroy_vehicle_kill").defaultValue("#D4B800").build());
        list.add(ConfigKeyDefinition.builder().key("format_rush_bomb_planted_capture").element("subtitle/kill_feed").type(STRING).category(Category.CONTENT).defaultValue("安装炸弹 +<score>").build());
        list.add(ConfigKeyDefinition.builder().key("format_rush_bomb_defused_capture").element("subtitle/kill_feed").type(STRING).category(Category.CONTENT).defaultValue("拆除炸弹 +<score>").build());
        list.add(ConfigKeyDefinition.builder().key("format_rush_objective_destroyed_capture").element("subtitle/kill_feed").type(STRING).category(Category.CONTENT).defaultValue("成功炸毁通讯设施 <target>").build());
        list.add(ConfigKeyDefinition.builder().key("color_capture_placeholder").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_capture_kill").defaultValue("#008B8B").build());
        list.add(ConfigKeyDefinition.builder().key("color_normal_text").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("enable_placeholder_bold").element("subtitle/kill_feed").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enable_normal_kill").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_headshot_kill").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_explosion_kill").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_crit_kill").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_assist_kill").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_destroy_vehicle_kill").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_capture_kill").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_spot_assist_kill").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_rescue_kill").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_scale_animation").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("color_normal_emphasis").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_normal_kill").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("color_headshot_emphasis").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_headshot_kill").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("color_explosion_emphasis").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_explosion_kill").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("color_crit_emphasis").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_crit_kill").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("color_assist_emphasis").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_assist_kill").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("color_destroy_vehicle_emphasis").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_destroy_vehicle_kill").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("color_capture_emphasis").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_capture_kill").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("color_spot_assist_placeholder").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_spot_assist_kill").defaultValue("#008B8B").build());
        list.add(ConfigKeyDefinition.builder().key("color_spot_assist_emphasis").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_spot_assist_kill").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("color_rescue_placeholder").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_rescue_kill").defaultValue("#008B8B").build());
        list.add(ConfigKeyDefinition.builder().key("color_rescue_emphasis").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_rescue_kill").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("glow_color_rescue").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_glow_effect").defaultValue("#63D048").build());
        list.add(ConfigKeyDefinition.builder().key("enable_stacking").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("max_lines").element("subtitle/kill_feed").type(INT).category(Category.POSITION).dependsOn("enable_stacking").defaultValue(3).build());
        list.add(ConfigKeyDefinition.builder().key("line_spacing").element("subtitle/kill_feed").type(INT).category(Category.POSITION).dependsOn("enable_stacking").defaultValue(12).build());
        list.add(ConfigKeyDefinition.builder().key("enable_text_shadow").element("subtitle/kill_feed").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("blink_fade_animation").element("subtitle/kill_feed").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enable_glow_effect").element("subtitle/kill_feed").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("glow_intensity").element("subtitle/kill_feed").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(0.5f).build());
        list.add(ConfigKeyDefinition.builder().key("glow_size").element("subtitle/kill_feed").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(0.3f).build());
        list.add(ConfigKeyDefinition.builder().key("glow_color").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("enable_glow_effect").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("glow_alpha").element("subtitle/kill_feed").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("align_left").element("subtitle/kill_feed").type(BOOLEAN).category(Category.POSITION).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("align_right").element("subtitle/kill_feed").type(BOOLEAN).category(Category.POSITION).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enable_flash_in").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("fade_in_duration").element("subtitle/kill_feed").type(FLOAT).category(Category.TIMING).defaultValue(0.0f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background").element("subtitle/kill_feed").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_alpha").element("subtitle/kill_feed").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_fade_in").element("subtitle/kill_feed").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.067f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_sweep_duration").element("subtitle/kill_feed").type(FLOAT).category(Category.TIMING).dependsOn("entrance_background").defaultValue(0.1f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_fade_out").element("subtitle/kill_feed").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.133f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_color").element("subtitle/kill_feed").type(COLOR).category(Category.COLOR).dependsOn("entrance_background").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("enable_queue_linkage").element("subtitle/kill_feed").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("queue_linkage_scroll_speed").element("subtitle/kill_feed").type(FLOAT).category(Category.TIMING).dependsOn("enable_queue_linkage").defaultValue(0.1f).build());
        list.add(ConfigKeyDefinition.builder().key("queue_linkage_icon_y_offset").element("subtitle/kill_feed").type(FLOAT).category(Category.EFFECT).dependsOn("enable_queue_linkage").defaultValue(0.0f).build());
        return list;
    }
    private static List<ConfigKeyDefinition> subtitleScore() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("subtitle/score").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("subtitle/score").type(FLOAT).category(Category.POSITION).defaultValue(2.0f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("subtitle/score").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("subtitle/score").type(INT).category(Category.POSITION).defaultValue(80).build());
        list.add(ConfigKeyDefinition.builder().key("display_duration").element("subtitle/score").type(FLOAT).category(Category.TIMING).defaultValue(4.0f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background").element("subtitle/score").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_alpha").element("subtitle/score").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_fade_in").element("subtitle/score").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.067f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_sweep_duration").element("subtitle/score").type(FLOAT).category(Category.TIMING).dependsOn("entrance_background").defaultValue(0.1f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_fade_out").element("subtitle/score").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.133f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_color").element("subtitle/score").type(COLOR).category(Category.COLOR).dependsOn("entrance_background").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("format_score").element("subtitle/score").type(STRING).category(Category.CONTENT).defaultValue("\u003cscore\u003e").build());
        list.add(ConfigKeyDefinition.builder().key("score_threshold").element("subtitle/score").type(INT).category(Category.BEHAVIOR).defaultValue(1000).build());
        list.add(ConfigKeyDefinition.builder().key("color_high_score").element("subtitle/score").type(COLOR).category(Category.COLOR).defaultValue("#D4B800").build());
        list.add(ConfigKeyDefinition.builder().key("color_flash").element("subtitle/score").type(COLOR).category(Category.COLOR).dependsOn("enable_flash").defaultValue("#D0D0D0").build());
        list.add(ConfigKeyDefinition.builder().key("color_normal_text").element("subtitle/score").type(COLOR).category(Category.COLOR).defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("animation_duration").element("subtitle/score").type(FLOAT).category(Category.TIMING).dependsOn("enable_digital_scroll").defaultValue(1.25f).build());
        list.add(ConfigKeyDefinition.builder().key("animation_refresh_rate").element("subtitle/score").type(FLOAT).category(Category.TIMING).dependsOn("enable_digital_scroll").defaultValue(0.01f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_number_segmentation").element("subtitle/score").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enable_flash").element("subtitle/score").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("align_left").element("subtitle/score").type(BOOLEAN).category(Category.POSITION).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("align_right").element("subtitle/score").type(BOOLEAN).category(Category.POSITION).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enable_score_scaling_effect").element("subtitle/score").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enable_digital_scroll").element("subtitle/score").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_glow_effect").element("subtitle/score").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("glow_intensity").element("subtitle/score").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(0.5f).build());
        list.add(ConfigKeyDefinition.builder().key("glow_size").element("subtitle/score").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(0.3f).build());
        list.add(ConfigKeyDefinition.builder().key("glow_color").element("subtitle/score").type(COLOR).category(Category.COLOR).dependsOn("enable_glow_effect").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("glow_alpha").element("subtitle/score").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_text_shadow").element("subtitle/score").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("blink_fade_animation").element("subtitle/score").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        return list;
    }
    private static List<ConfigKeyDefinition> subtitleBonusList() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("subtitle/bonus_list").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("subtitle/bonus_list").type(FLOAT).category(Category.POSITION).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("subtitle/bonus_list").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("subtitle/bonus_list").type(INT).category(Category.POSITION).defaultValue(65).build());
        list.add(ConfigKeyDefinition.builder().key("line_spacing").element("subtitle/bonus_list").type(INT).category(Category.POSITION).defaultValue(12).build());
        list.add(ConfigKeyDefinition.builder().key("max_lines").element("subtitle/bonus_list").type(INT).category(Category.POSITION).defaultValue(4).build());
        list.add(ConfigKeyDefinition.builder().key("display_duration").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).defaultValue(3.0f).build());
        list.add(ConfigKeyDefinition.builder().key("fade_out_interval").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_special_streak_subtitles").element("subtitle/bonus_list").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enable_text_scrolling").element("subtitle/bonus_list").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("text_scrolling_duration_multiplier").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).dependsOn("enable_text_scrolling").defaultValue(1.2f).build());
        list.add(ConfigKeyDefinition.builder().key("text_scrolling_refresh_rate").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).dependsOn("enable_text_scrolling").defaultValue(0.02f).build());
        list.add(ConfigKeyDefinition.builder().key("color_special_placeholder").element("subtitle/bonus_list").type(COLOR).category(Category.COLOR).defaultValue("#D4B800").build());
        list.add(ConfigKeyDefinition.builder().key("color_normal_text").element("subtitle/bonus_list").type(COLOR).category(Category.COLOR).defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("text_box").element("subtitle/bonus_list").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("text_box_border_width").element("subtitle/bonus_list").type(FLOAT).category(Category.EFFECT).dependsOn("text_box").defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("text_box_color").element("subtitle/bonus_list").type(COLOR).category(Category.COLOR).dependsOn("text_box").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("enable_horizontal_layout").element("subtitle/bonus_list").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("clean_subtitle_content").element("subtitle/bonus_list").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("fade_out_duration").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).defaultValue(0.3f).build());
        list.add(ConfigKeyDefinition.builder().key("fade_start_line_ratio").element("subtitle/bonus_list").type(FLOAT).category(Category.EFFECT).defaultValue(0.0f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_queue_linkage").element("subtitle/bonus_list").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("queue_linkage_scroll_speed").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).dependsOn("enable_queue_linkage").defaultValue(0.1f).build());
        list.add(ConfigKeyDefinition.builder().key("queue_linkage_icon_y_offset").element("subtitle/bonus_list").type(FLOAT).category(Category.EFFECT).dependsOn("enable_queue_linkage").defaultValue(0.0f).build());
        list.add(ConfigKeyDefinition.builder().key("animation_duration").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).dependsOn("enable_digital_scroll").defaultValue(0.5f).build());
        list.add(ConfigKeyDefinition.builder().key("animation_refresh_rate").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).dependsOn("enable_digital_scroll").defaultValue(0.01f).build());
        list.add(ConfigKeyDefinition.builder().key("align_left").element("subtitle/bonus_list").type(BOOLEAN).category(Category.POSITION).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("align_right").element("subtitle/bonus_list").type(BOOLEAN).category(Category.POSITION).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("merge_window_duration").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).defaultValue(0.5f).build());
        list.add(ConfigKeyDefinition.builder().key("animation_speed").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).defaultValue(10.0f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_text_sweep_animation").element("subtitle/bonus_list").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enter_animation_duration").element("subtitle/bonus_list").type(FLOAT).category(Category.TIMING).defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("kill_bonus_scale").element("subtitle/bonus_list").type(FLOAT).category(Category.POSITION).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_kill_feed").element("subtitle/bonus_list").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("kill_feed_format").element("subtitle/bonus_list").type(STRING).category(Category.CONTENT).dependsOn("enable_kill_feed").defaultValue("[\u003cweapon\u003e] \u003ctarget\u003e +\u003cscore\u003e").build());
        list.add(ConfigKeyDefinition.builder().key("kill_feed_victim_color").element("subtitle/bonus_list").type(COLOR).category(Category.COLOR).dependsOn("enable_kill_feed").defaultValue("#FF0000").build());
        list.add(ConfigKeyDefinition.builder().key("enable_digital_scroll").element("subtitle/bonus_list").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_stack_multiplier").element("subtitle/bonus_list").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enable_glow_effect").element("subtitle/bonus_list").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("glow_intensity").element("subtitle/bonus_list").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(0.5f).build());
        list.add(ConfigKeyDefinition.builder().key("glow_size").element("subtitle/bonus_list").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(0.3f).build());
        list.add(ConfigKeyDefinition.builder().key("glow_color").element("subtitle/bonus_list").type(COLOR).category(Category.COLOR).dependsOn("enable_glow_effect").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("glow_alpha").element("subtitle/bonus_list").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_text_shadow").element("subtitle/bonus_list").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("blink_fade_animation").element("subtitle/bonus_list").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        return list;
    }
    private static List<ConfigKeyDefinition> subtitleCombo() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("subtitle/combo").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("subtitle/combo").type(FLOAT).category(Category.POSITION).defaultValue(1.5f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("subtitle/combo").type(FLOAT).category(Category.POSITION).defaultValue(0.0f).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("subtitle/combo").type(FLOAT).category(Category.POSITION).defaultValue(70.0f).build());
        list.add(ConfigKeyDefinition.builder().key("color_kill_combo").element("subtitle/combo").type(COLOR).category(Category.COLOR).defaultValue("#FF3500").build());
        list.add(ConfigKeyDefinition.builder().key("color_assist_combo").element("subtitle/combo").type(COLOR).category(Category.COLOR).defaultValue("#FFD700").build());
        list.add(ConfigKeyDefinition.builder().key("format_kill_single").element("subtitle/combo").type(STRING).category(Category.CONTENT).defaultValue("\u003ccombo\u003e 娣樻卑").build());
        list.add(ConfigKeyDefinition.builder().key("format_kill_multi").element("subtitle/combo").type(STRING).category(Category.CONTENT).defaultValue("\u003ccombo\u003e 淘汰数").build());
        list.add(ConfigKeyDefinition.builder().key("format_assist_single").element("subtitle/combo").type(STRING).category(Category.CONTENT).defaultValue("\u003ccombo\u003e 鍔╂敾").build());
        list.add(ConfigKeyDefinition.builder().key("format_assist_multi").element("subtitle/combo").type(STRING).category(Category.CONTENT).defaultValue("\u003ccombo\u003e 助攻数").build());
        list.add(ConfigKeyDefinition.builder().key("enable_animation").element("subtitle/combo").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_light_effect").element("subtitle/combo").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_bold").element("subtitle/combo").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_text_shadow").element("subtitle/combo").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("blink_fade_animation").element("subtitle/combo").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("light_height").element("subtitle/combo").type(FLOAT).category(Category.EFFECT).defaultValue(10.0f).build());
        list.add(ConfigKeyDefinition.builder().key("light_hold_duration").element("subtitle/combo").type(FLOAT).category(Category.TIMING).defaultValue(0.0f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_scale_animation").element("subtitle/combo").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("display_duration").element("subtitle/combo").type(FLOAT).category(Category.TIMING).defaultValue(5.0f).build());
        list.add(ConfigKeyDefinition.builder().key("reset_kill_combo").element("subtitle/combo").type(STRING).category(Category.BEHAVIOR).defaultValue("death").build());
        list.add(ConfigKeyDefinition.builder().key("reset_assist_combo").element("subtitle/combo").type(STRING).category(Category.BEHAVIOR).defaultValue("death").build());
        list.add(ConfigKeyDefinition.builder().key("combo_reset_timeout").element("subtitle/combo").type(FLOAT).category(Category.TIMING).defaultValue(10.0f).build());
        return list;
    }
    private static List<ConfigKeyDefinition> subtitleHitInfo() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("subtitle/hit_info").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("subtitle/hit_info").type(FLOAT).category(Category.POSITION).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("subtitle/hit_info").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("subtitle/hit_info").type(INT).category(Category.POSITION).defaultValue(80).build());
        list.add(ConfigKeyDefinition.builder().key("display_duration").element("subtitle/hit_info").type(FLOAT).category(Category.TIMING).defaultValue(3.0f).build());
        list.add(ConfigKeyDefinition.builder().key("align_left").element("subtitle/hit_info").type(BOOLEAN).category(Category.POSITION).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("align_right").element("subtitle/hit_info").type(BOOLEAN).category(Category.POSITION).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("format_damage").element("subtitle/hit_info").type(STRING).category(Category.CONTENT).defaultValue("<damage>").build());
        list.add(ConfigKeyDefinition.builder().key("color_damage_default").element("subtitle/hit_info").type(COLOR).category(Category.COLOR).defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("color_damage_kill").element("subtitle/hit_info").type(COLOR).category(Category.COLOR).defaultValue("#D4B800").build());
        list.add(ConfigKeyDefinition.builder().key("color_normal_text").element("subtitle/hit_info").type(COLOR).category(Category.COLOR).defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("fade_in_duration").element("subtitle/hit_info").type(FLOAT).category(Category.TIMING).defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("fade_out_duration").element("subtitle/hit_info").type(FLOAT).category(Category.TIMING).defaultValue(0.3f).build());
        list.add(ConfigKeyDefinition.builder().key("blink_fade_animation").element("subtitle/hit_info").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("enable_damage_scroll").element("subtitle/hit_info").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("damage_scroll_duration").element("subtitle/hit_info").type(FLOAT).category(Category.TIMING).dependsOn("enable_damage_scroll").defaultValue(1.25f).build());
        list.add(ConfigKeyDefinition.builder().key("damage_scroll_refresh_rate").element("subtitle/hit_info").type(FLOAT).category(Category.TIMING).dependsOn("enable_damage_scroll").defaultValue(0.02f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_entity_layers").element("subtitle/hit_info").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("max_layers").element("subtitle/hit_info").type(INT).category(Category.BEHAVIOR).dependsOn("enable_entity_layers").defaultValue(5).build());
        list.add(ConfigKeyDefinition.builder().key("layer_spacing").element("subtitle/hit_info").type(INT).category(Category.BEHAVIOR).dependsOn("enable_entity_layers").defaultValue(12).build());
        list.add(ConfigKeyDefinition.builder().key("layer_move_animation_duration").element("subtitle/hit_info").type(FLOAT).category(Category.TIMING).dependsOn("enable_entity_layers").defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("layer_collapse_animation_duration").element("subtitle/hit_info").type(FLOAT).category(Category.TIMING).dependsOn("enable_entity_layers").defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_glow_effect").element("subtitle/hit_info").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("glow_intensity").element("subtitle/hit_info").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(0.5f).build());
        list.add(ConfigKeyDefinition.builder().key("glow_size").element("subtitle/hit_info").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(0.3f).build());
        list.add(ConfigKeyDefinition.builder().key("glow_color_damage_default").element("subtitle/hit_info").type(COLOR).category(Category.COLOR).dependsOn("enable_glow_effect").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("glow_color_damage_kill").element("subtitle/hit_info").type(COLOR).category(Category.COLOR).dependsOn("enable_glow_effect").defaultValue("#D4B800").build());
        list.add(ConfigKeyDefinition.builder().key("glow_alpha").element("subtitle/hit_info").type(FLOAT).category(Category.EFFECT).dependsOn("enable_glow_effect").defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_text_shadow").element("subtitle/hit_info").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        return list;
    }
    private static List<ConfigKeyDefinition> killIconScrolling() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("kill_icon/scrolling").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("kill_icon/scrolling").type(FLOAT).category(Category.POSITION).defaultValue(0.4f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("kill_icon/scrolling").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("kill_icon/scrolling").type(INT).category(Category.POSITION).defaultValue(120).build());
        list.add(ConfigKeyDefinition.builder().key("display_duration").element("kill_icon/scrolling").type(FLOAT).category(Category.TIMING).defaultValue(3.25f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_ring_effect_crit").element("kill_icon/scrolling").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_ring_effect_headshot").element("kill_icon/scrolling").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_ring_effect_explosion").element("kill_icon/scrolling").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("animation_duration").element("kill_icon/scrolling").type(FLOAT).category(Category.TIMING).defaultValue(0.3f).build());
        list.add(ConfigKeyDefinition.builder().key("fade_out_duration").element("kill_icon/scrolling").type(FLOAT).category(Category.TIMING).defaultValue(0.1f).build());
        list.add(ConfigKeyDefinition.builder().key("position_animation_duration").element("kill_icon/scrolling").type(FLOAT).category(Category.TIMING).defaultValue(0.3f).build());
        list.add(ConfigKeyDefinition.builder().key("start_scale").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).defaultValue(2.0f).build());
        list.add(ConfigKeyDefinition.builder().key("icon_spacing").element("kill_icon/scrolling").type(INT).category(Category.POSITION).defaultValue(4).build());
        list.add(ConfigKeyDefinition.builder().key("max_visible_icons").element("kill_icon/scrolling").type(INT).category(Category.POSITION).defaultValue(7).build());
        list.add(ConfigKeyDefinition.builder().key("display_interval_ms").element("kill_icon/scrolling").type(INT).category(Category.TIMING).defaultValue(100).build());
        list.add(ConfigKeyDefinition.builder().key("max_pending_icons").element("kill_icon/scrolling").type(INT).category(Category.POSITION).defaultValue(30).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_crit_color").element("kill_icon/scrolling").type(COLOR).category(Category.COLOR).dependsOn("enable_ring_effect_crit").defaultValue("#9CCC65").build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_crit_radius").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_crit").defaultValue(42.0f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_crit_thickness").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_crit").defaultValue(1.8f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_headshot_color").element("kill_icon/scrolling").type(COLOR).category(Category.COLOR).dependsOn("enable_ring_effect_headshot").defaultValue("#D4B800").build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_headshot_radius").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_headshot").defaultValue(42.0f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_headshot_thickness").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_headshot").defaultValue(3.0f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_explosion_color").element("kill_icon/scrolling").type(COLOR).category(Category.COLOR).dependsOn("enable_ring_effect_explosion").defaultValue("#F77F00").build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_explosion_radius").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_explosion").defaultValue(42.0f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_explosion_thickness").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_explosion").defaultValue(5.4f).build());
        list.add(ConfigKeyDefinition.builder().key("scroll_direction").element("kill_icon/scrolling").type(STRING).category(Category.EFFECT).defaultValue("left").build());
        list.add(ConfigKeyDefinition.builder().key("pin_newest_icon").element("kill_icon/scrolling").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("blink_fade_animation").element("kill_icon/scrolling").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background").element("kill_icon/scrolling").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("icon_delay").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).defaultValue(0.0f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_size").element("kill_icon/scrolling").type(INT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(64).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_fade_in").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_fade_out").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_color").element("kill_icon/scrolling").type(COLOR).category(Category.COLOR).dependsOn("entrance_background").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_headshot_color").element("kill_icon/scrolling").type(COLOR).category(Category.COLOR).dependsOn("entrance_background").defaultValue("#FF5000").build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_alpha").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_border").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.5f).build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_border_color").element("kill_icon/scrolling").type(COLOR).category(Category.COLOR).dependsOn("entrance_background").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_headshot_border_color").element("kill_icon/scrolling").type(COLOR).category(Category.COLOR).dependsOn("entrance_background").defaultValue("#FF4300").build());
        list.add(ConfigKeyDefinition.builder().key("entrance_background_border_alpha").element("kill_icon/scrolling").type(FLOAT).category(Category.EFFECT).dependsOn("entrance_background").defaultValue(0.2f).build());
        return list;
    }
    private static List<ConfigKeyDefinition> killIconCombo() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("kill_icon/combo").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("kill_icon/combo").type(FLOAT).category(Category.POSITION).defaultValue(0.6f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("kill_icon/combo").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("kill_icon/combo").type(INT).category(Category.POSITION).defaultValue(120).build());
        list.add(ConfigKeyDefinition.builder().key("enable_ring_effect_crit").element("kill_icon/combo").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_ring_effect_headshot").element("kill_icon/combo").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_ring_effect_explosion").element("kill_icon/combo").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_crit_color").element("kill_icon/combo").type(COLOR).category(Category.COLOR).dependsOn("enable_ring_effect_crit").defaultValue("#9CCC65").build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_crit_radius").element("kill_icon/combo").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_crit").defaultValue(42.0f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_crit_thickness").element("kill_icon/combo").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_crit").defaultValue(1.8f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_headshot_color").element("kill_icon/combo").type(COLOR).category(Category.COLOR).dependsOn("enable_ring_effect_headshot").defaultValue("#D4B800").build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_headshot_radius").element("kill_icon/combo").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_headshot").defaultValue(42.0f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_headshot_thickness").element("kill_icon/combo").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_headshot").defaultValue(3.0f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_explosion_color").element("kill_icon/combo").type(COLOR).category(Category.COLOR).dependsOn("enable_ring_effect_explosion").defaultValue("#F77F00").build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_explosion_radius").element("kill_icon/combo").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_explosion").defaultValue(42.0f).build());
        list.add(ConfigKeyDefinition.builder().key("ring_effect_explosion_thickness").element("kill_icon/combo").type(FLOAT).category(Category.EFFECT).dependsOn("enable_ring_effect_explosion").defaultValue(5.4f).build());
        return list;
    }
    private static List<ConfigKeyDefinition> killIconValorant() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("kill_icon/valorant").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("kill_icon/valorant").type(FLOAT).category(Category.POSITION).defaultValue(0.85f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("kill_icon/valorant").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("kill_icon/valorant").type(INT).category(Category.POSITION).defaultValue(80).build());
        list.add(ConfigKeyDefinition.builder().key("enable_accent_tint").element("kill_icon/valorant").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("color_accent").element("kill_icon/valorant").type(COLOR).category(Category.COLOR).defaultValue("#E2505C").build());
        list.add(ConfigKeyDefinition.builder().key("brightness").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_icon_glow").element("kill_icon/valorant").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("color_icon_glow").element("kill_icon/valorant").type(COLOR).category(Category.COLOR).dependsOn("enable_icon_glow").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("icon_glow_intensity").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).dependsOn("enable_icon_glow").defaultValue(0.45f).build());
        list.add(ConfigKeyDefinition.builder().key("icon_glow_size").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).dependsOn("enable_icon_glow").defaultValue(4.0f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_halo_ring").element("kill_icon/valorant").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("halo_ring_radius").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).dependsOn("enable_halo_ring").defaultValue(30.0f).build());
        list.add(ConfigKeyDefinition.builder().key("halo_ring_width").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).dependsOn("enable_halo_ring").defaultValue(1.5f).build());
        list.add(ConfigKeyDefinition.builder().key("color_halo_ring").element("kill_icon/valorant").type(COLOR).category(Category.COLOR).dependsOn("enable_halo_ring").defaultValue("#FFFFFF").build());
        list.add(ConfigKeyDefinition.builder().key("icon_entry_offset_y").element("kill_icon/valorant").type(INT).category(Category.POSITION).defaultValue(-16).build());
        list.add(ConfigKeyDefinition.builder().key("icon_entry_duration").element("kill_icon/valorant").type(FLOAT).category(Category.TIMING).defaultValue(0.1f).build());
        list.add(ConfigKeyDefinition.builder().key("icon_flash_count").element("kill_icon/valorant").type(INT).category(Category.EFFECT).defaultValue(4).build());
        list.add(ConfigKeyDefinition.builder().key("icon_flash_hold_duration").element("kill_icon/valorant").type(FLOAT).category(Category.TIMING).defaultValue(0.08f).build());
        list.add(ConfigKeyDefinition.builder().key("color_icon_flash").element("kill_icon/valorant").type(COLOR).category(Category.COLOR).defaultValue("#FF2A36").build());
        list.add(ConfigKeyDefinition.builder().key("color_headshot_overlay").element("kill_icon/valorant").type(COLOR).category(Category.COLOR).defaultValue("#FF2A36").build());
        list.add(ConfigKeyDefinition.builder().key("headshot_anim_initial_scale").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).defaultValue(1.8f).build());
        list.add(ConfigKeyDefinition.builder().key("headshot_anim_duration").element("kill_icon/valorant").type(FLOAT).category(Category.TIMING).defaultValue(0.25f).build());
        list.add(ConfigKeyDefinition.builder().key("bar_entry_initial_scale").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).defaultValue(1.6f).build());
        list.add(ConfigKeyDefinition.builder().key("bar_entry_duration").element("kill_icon/valorant").type(FLOAT).category(Category.TIMING).defaultValue(0.18f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_math_particle_effect").element("kill_icon/valorant").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).build());
        list.add(ConfigKeyDefinition.builder().key("math_particle_density").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("math_particle_spread").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("math_particle_size").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("icon_scale").element("kill_icon/valorant").type(FLOAT).category(Category.POSITION).defaultValue(0.9f).build());
        list.add(ConfigKeyDefinition.builder().key("enable_blade_effect").element("kill_icon/valorant").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("enable_blade_rotation_effect").element("kill_icon/valorant").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("blade_deceleration_window").element("kill_icon/valorant").type(FLOAT).category(Category.TIMING).defaultValue(2.0f).build());
        list.add(ConfigKeyDefinition.builder().key("bar_x_offset").element("kill_icon/valorant").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("bar_y_offset").element("kill_icon/valorant").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("bar_radius_offset").element("kill_icon/valorant").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("anim_base_particle_texture_y_offset").element("kill_icon/valorant").type(INT).category(Category.EFFECT).defaultValue(45).build());
        list.add(ConfigKeyDefinition.builder().key("anim_blade_texture_scale").element("kill_icon/valorant").type(FLOAT).category(Category.EFFECT).defaultValue(0.6f).build());
        return list;
    }
    private static List<ConfigKeyDefinition> killIconCard() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("kill_icon/card").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("kill_icon/card").type(FLOAT).category(Category.POSITION).defaultValue(0.15f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("kill_icon/card").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("kill_icon/card").type(INT).category(Category.POSITION).defaultValue(35).build());
        list.add(ConfigKeyDefinition.builder().key("team").element("kill_icon/card").type(STRING).category(Category.EFFECT).defaultValue("ct").build());
        list.add(ConfigKeyDefinition.builder().key("dynamic_card_style").element("kill_icon/card").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("animation_duration").element("kill_icon/card").type(FLOAT).category(Category.TIMING).defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("color_text_ct").element("kill_icon/card").type(COLOR).category(Category.COLOR).defaultValue("#9cc1eb").build());
        list.add(ConfigKeyDefinition.builder().key("color_text_t").element("kill_icon/card").type(COLOR).category(Category.COLOR).defaultValue("#d9ac5b").build());
        list.add(ConfigKeyDefinition.builder().key("text_scale").element("kill_icon/card").type(FLOAT).category(Category.POSITION).defaultValue(10.0f).build());
        list.add(ConfigKeyDefinition.builder().key("max_stack_count").element("kill_icon/card").type(INT).category(Category.EFFECT).defaultValue(6).build());
        list.add(ConfigKeyDefinition.builder().key("anim_light_ct_texture_frame_width_ratio").element("kill_icon/card").type(INT).category(Category.EFFECT).defaultValue(1).build());
        list.add(ConfigKeyDefinition.builder().key("anim_light_ct_texture_frame_height_ratio").element("kill_icon/card").type(INT).category(Category.EFFECT).defaultValue(5).build());
        list.add(ConfigKeyDefinition.builder().key("anim_light_t_texture_frame_width_ratio").element("kill_icon/card").type(INT).category(Category.EFFECT).defaultValue(1).build());
        list.add(ConfigKeyDefinition.builder().key("anim_light_t_texture_frame_height_ratio").element("kill_icon/card").type(INT).category(Category.EFFECT).defaultValue(5).build());
        return list;
    }
    private static List<ConfigKeyDefinition> killIconCardBar() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("kill_icon/card_bar").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("kill_icon/card_bar").type(FLOAT).category(Category.POSITION).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("kill_icon/card_bar").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("kill_icon/card_bar").type(INT).category(Category.POSITION).defaultValue(40).build());
        list.add(ConfigKeyDefinition.builder().key("team").element("kill_icon/card_bar").type(STRING).category(Category.EFFECT).defaultValue("ct").build());
        list.add(ConfigKeyDefinition.builder().key("show_light").element("kill_icon/card_bar").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("light_width").element("kill_icon/card_bar").type(FLOAT).category(Category.EFFECT).defaultValue(350.0f).build());
        list.add(ConfigKeyDefinition.builder().key("light_height").element("kill_icon/card_bar").type(FLOAT).category(Category.EFFECT).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("color_light_ct").element("kill_icon/card_bar").type(COLOR).category(Category.COLOR).defaultValue("#9cc1eb").build());
        list.add(ConfigKeyDefinition.builder().key("color_light_t").element("kill_icon/card_bar").type(COLOR).category(Category.COLOR).defaultValue("#d9ac5b").build());
        list.add(ConfigKeyDefinition.builder().key("dynamic_card_style").element("kill_icon/card_bar").type(BOOLEAN).category(Category.BEHAVIOR).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("animation_duration").element("kill_icon/card_bar").type(FLOAT).category(Category.TIMING).defaultValue(0.2f).build());
        return list;
    }
    private static List<ConfigKeyDefinition> killIconBattlefield1() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("kill_icon/battlefield1").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("icon_size").element("kill_icon/battlefield1").type(INT).category(Category.POSITION).defaultValue(25).build());
        list.add(ConfigKeyDefinition.builder().key("border_size").element("kill_icon/battlefield1").type(INT).category(Category.POSITION).defaultValue(3).build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("kill_icon/battlefield1").type(INT).category(Category.POSITION).defaultValue(0).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("kill_icon/battlefield1").type(INT).category(Category.POSITION).defaultValue(100).build());
        list.add(ConfigKeyDefinition.builder().key("background_color").element("kill_icon/battlefield1").type(COLOR).category(Category.COLOR).defaultValue("#000000").build());
        list.add(ConfigKeyDefinition.builder().key("icon_box_opacity").element("kill_icon/battlefield1").type(INT).category(Category.EFFECT).defaultValue(80).build());
        list.add(ConfigKeyDefinition.builder().key("text_box_opacity").element("kill_icon/battlefield1").type(INT).category(Category.EFFECT).defaultValue(90).build());
        list.add(ConfigKeyDefinition.builder().key("scale_weapon").element("kill_icon/battlefield1").type(FLOAT).category(Category.EFFECT).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("scale_victim").element("kill_icon/battlefield1").type(FLOAT).category(Category.EFFECT).defaultValue(1.2f).build());
        list.add(ConfigKeyDefinition.builder().key("scale_health").element("kill_icon/battlefield1").type(FLOAT).category(Category.EFFECT).defaultValue(1.5f).build());
        list.add(ConfigKeyDefinition.builder().key("color_victim").element("kill_icon/battlefield1").type(COLOR).category(Category.COLOR).defaultValue("#FF0000").build());
        list.add(ConfigKeyDefinition.builder().key("animation_duration").element("kill_icon/battlefield1").type(FLOAT).category(Category.TIMING).defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("display_duration").element("kill_icon/battlefield1").type(FLOAT).category(Category.TIMING).defaultValue(4.5f).build());
        return list;
    }

    private static List<ConfigKeyDefinition> killIconHonor() {
        List<ConfigKeyDefinition> list = new ArrayList<>();
        list.add(ConfigKeyDefinition.builder().key("visible").element("kill_icon/honor").type(BOOLEAN).category(Category.VISIBILITY).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("scale").element("kill_icon/honor").type(FLOAT).category(Category.POSITION).defaultValue(0.85f).build());
        list.add(ConfigKeyDefinition.builder().key("direction").element("kill_icon/honor").type(STRING).category(Category.POSITION).defaultValue("right").build());
        list.add(ConfigKeyDefinition.builder().key("x_offset").element("kill_icon/honor").type(INT).category(Category.POSITION).defaultValue(-80).build());
        list.add(ConfigKeyDefinition.builder().key("y_offset").element("kill_icon/honor").type(INT).category(Category.POSITION).defaultValue(-22).build());
        list.add(ConfigKeyDefinition.builder().key("screen_anchor").element("kill_icon/honor").type(STRING).category(Category.POSITION).defaultValue("center").build());
        list.add(ConfigKeyDefinition.builder().key("display_delay").element("kill_icon/honor").type(FLOAT).category(Category.EFFECT).defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("enter_animation_duration").element("kill_icon/honor").type(FLOAT).category(Category.TIMING).defaultValue(0.2f).build());
        list.add(ConfigKeyDefinition.builder().key("enter_start_scale").element("kill_icon/honor").type(FLOAT).category(Category.EFFECT).defaultValue(0.0f).build());
        list.add(ConfigKeyDefinition.builder().key("display_duration").element("kill_icon/honor").type(FLOAT).category(Category.TIMING).defaultValue(2.0f).build());
        list.add(ConfigKeyDefinition.builder().key("min_display_duration").element("kill_icon/honor").type(FLOAT).category(Category.TIMING).defaultValue(1.0f).build());
        list.add(ConfigKeyDefinition.builder().key("fade_out_duration").element("kill_icon/honor").type(FLOAT).category(Category.TIMING).defaultValue(0.3f).build());
        list.add(ConfigKeyDefinition.builder().key("blink_fade_animation").element("kill_icon/honor").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_enabled").element("kill_icon/honor").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_x_offset").element("kill_icon/honor").type(INT).category(Category.POSITION).defaultValue(-70).dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_height").element("kill_icon/honor").type(INT).category(Category.POSITION).defaultValue(10).dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_enter_duration").element("kill_icon/honor").type(FLOAT).category(Category.TIMING).defaultValue(0.2f).dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_color").element("kill_icon/honor").type(COLOR).category(Category.COLOR).defaultValue("#FFFFFF").dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_max_alpha").element("kill_icon/honor").type(FLOAT).category(Category.EFFECT).defaultValue(20.0f).dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_text_color").element("kill_icon/honor").type(COLOR).category(Category.COLOR).defaultValue("#000000").dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_text_padding").element("kill_icon/honor").type(INT).category(Category.POSITION).defaultValue(6).dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_text_gap").element("kill_icon/honor").type(INT).category(Category.POSITION).defaultValue(8).dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_text_scale").element("kill_icon/honor").type(FLOAT).category(Category.EFFECT).defaultValue(0.8f).dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("hint_box_text_shadow").element("kill_icon/honor").type(BOOLEAN).category(Category.EFFECT).defaultValue(false).dependsOn("hint_box_enabled").build());
        list.add(ConfigKeyDefinition.builder().key("main_icon_max_alpha").element("kill_icon/honor").type(FLOAT).category(Category.EFFECT).defaultValue(20.0f).build());
        list.add(ConfigKeyDefinition.builder().key("shake_enabled").element("kill_icon/honor").type(BOOLEAN).category(Category.EFFECT).defaultValue(true).build());
        list.add(ConfigKeyDefinition.builder().key("shake_count").element("kill_icon/honor").type(INT).category(Category.EFFECT).dependsOn("shake_enabled").defaultValue(16).build());
        list.add(ConfigKeyDefinition.builder().key("shake_range").element("kill_icon/honor").type(INT).category(Category.EFFECT).dependsOn("shake_enabled").defaultValue(1).build());
        return list;
    }
}