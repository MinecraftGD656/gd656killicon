package org.mods.gd656killicon.common.config;

import com.google.gson.JsonObject;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 声明式配置项注册表(仿 BonusRegistry):
 * 一处声明, GUI 类型/分类/依赖灰色/默认值四处自动接线。
 * <ul>
 *   <li>静态键: 由 {@link org.mods.gd656killicon.common.config.ElementConfigDefinitionsArea} 声明注册。</li>
 *   <li>动态键: format_&lt;BONUS_ID&gt;(加分项注册时)、format_&lt;KILLTYPE&gt;(击杀类型注册时)、
 *       anim_&lt;texture&gt;_* / texture_*_&lt;texture&gt;(纹理系统注册时)。</li>
 * </ul>
 * 默认值经 {@link #buildElementDefaults(String)} 生成, 取代旧的 DefaultConfigRegistry.GLOBAL_DEFAULTS。
 */
public final class ElementConfigRegistry {
    /** elementId → (key → 定义)。LinkedHashMap 保持声明序。 */
    private static final Map<String, Map<String, ConfigKeyDefinition>> DEFINITIONS = new LinkedHashMap<>();

    private ElementConfigRegistry() {
    }

    static {
        org.mods.gd656killicon.common.config.ElementConfigDefinitionsArea.register();
    }

    // ==================== 注册 ====================

    public static void register(ConfigKeyDefinition... defs) {
        for (ConfigKeyDefinition def : defs) {
            DEFINITIONS.computeIfAbsent(def.elementId(), k -> new LinkedHashMap<>()).put(def.key(), def);
        }
    }

    public static void register(List<ConfigKeyDefinition> defs) {
        for (ConfigKeyDefinition def : defs) {
            register(def);
        }
    }

    /** 是否已注册该元素(静态键或动态键)。 */
    public static boolean hasElement(String elementId) {
        return DEFINITIONS.containsKey(elementId);
    }

    /** 全部元素清单(替代 DefaultConfigRegistry.getAllElementTypes)。 */
    public static Set<String> getElements() {
        return DEFINITIONS.keySet();
    }

    /** 该元素全部键(静态 + 动态, 声明序)。 */
    public static Set<String> getKeys(String elementId) {
        Map<String, ConfigKeyDefinition> defs = DEFINITIONS.get(elementId);
        return defs == null ? java.util.Collections.emptySet() : defs.keySet();
    }

    public static ConfigKeyDefinition getDefinition(String elementId, String key) {
        Map<String, ConfigKeyDefinition> defs = DEFINITIONS.get(elementId);
        return defs == null ? null : defs.get(key);
    }

    public static boolean isRegistered(String elementId, String key) {
        return getDefinition(elementId, key) != null;
    }

    public static ConfigType getType(String elementId, String key) {
        ConfigKeyDefinition def = getDefinition(elementId, key);
        return def == null ? null : def.type();
    }

    public static Category getCategory(String elementId, String key) {
        ConfigKeyDefinition def = getDefinition(elementId, key);
        return def == null ? null : def.category();
    }

    public static String getDependsOn(String elementId, String key) {
        ConfigKeyDefinition def = getDefinition(elementId, key);
        return def == null ? null : def.dependsOn();
    }

    /** TEXTURE 类键的纹理归属(如 "default"); 非纹理键返回 null。 */
    public static String getTextureTab(String elementId, String key) {
        ConfigKeyDefinition def = getDefinition(elementId, key);
        return def == null ? null : def.textureTab();
    }

    /** 生成元素完整默认配置(静态键 + 动态键)。取代 DefaultConfigRegistry.getGlobalDefault。 */
    public static JsonObject buildElementDefaults(String elementId) {
        JsonObject config = new JsonObject();
        Map<String, ConfigKeyDefinition> defs = DEFINITIONS.get(elementId);
        if (defs != null) {
            for (ConfigKeyDefinition def : defs.values()) {
                config.add(def.key(), def.defaultValue());
            }
        }
        return config;
    }

    // ==================== 纹理键动态注册 ====================

    /**
     * 注册某元素全部纹理的动画键(anim_&lt;texture&gt;_*)与纹理选择键(texture_style_/custom_texture_/
     * texture_mode_/vanilla_texture_&lt;texture&gt;), 纹理 tab 归属 = 纹理名。
     * 默认值按原 DefaultConfigRegistry.injectTextureAnimationConfigs / injectTextureSelectionConfigs(00001 基准)。
     */
    public static void registerTextureKeys(String elementId) {
        if (!ElementTextureDefinition.hasTextures(elementId)) {
            return;
        }
        for (String texture : ElementTextureDefinition.getTextures(elementId)) {
            String prefix = "anim_" + texture + "_";
            register(
                    builder(prefix + "enable_texture_animation", elementId).type(ConfigType.BOOLEAN).textureTab(texture).defaultValue(false).build(),
                    builder(prefix + "texture_animation_total_frames", elementId).type(ConfigType.INT).textureTab(texture).defaultValue(1).build(),
                    builder(prefix + "texture_animation_interval_ms", elementId).type(ConfigType.INT).textureTab(texture).defaultValue(100).build(),
                    builder(prefix + "texture_animation_orientation", elementId).type(ConfigType.CHOICE).textureTab(texture).defaultValue("vertical").choices("vertical", "horizontal").build(),
                    builder(prefix + "texture_animation_loop", elementId).type(ConfigType.BOOLEAN).textureTab(texture).defaultValue(false).build(),
                    builder(prefix + "texture_animation_play_style", elementId).type(ConfigType.CHOICE).textureTab(texture).defaultValue("sequential").choices("sequential", "random").build(),
                    builder(prefix + "texture_frame_width_ratio", elementId).type(ConfigType.INT).textureTab(texture).defaultValue(1).build(),
                    builder(prefix + "texture_frame_height_ratio", elementId).type(ConfigType.INT).textureTab(texture).defaultValue(1).build(),
                    builder(prefix + "texture_scale", elementId).type(ConfigType.FLOAT).textureTab(texture).defaultValue(1.0f).build(),
                    builder(prefix + "texture_final_opacity", elementId).type(ConfigType.FLOAT).textureTab(texture).defaultValue(1.0f).build(),
                    builder(prefix + "texture_x_offset", elementId).type(ConfigType.INT).textureTab(texture).defaultValue(0).build(),
                    builder(prefix + "texture_y_offset", elementId).type(ConfigType.INT).textureTab(texture).defaultValue(0).build()
            );

            String officialKey = ElementTextureDefinition.getOfficialTextureKey(texture);
            String customKey = ElementTextureDefinition.getCustomTextureKey(texture);
            String modeKey = ElementTextureDefinition.getTextureModeKey(texture);
            String vanillaKey = ElementTextureDefinition.getVanillaTextureKey(texture);
            String fileName = ElementTextureDefinition.getTextureFileName("00001", elementId, texture);
            String vanillaDefault = resolveVanillaDefaultTexture(elementId, texture);
            List<ConfigKeyDefinition> textureDefs = new ArrayList<>();
            if (fileName != null) {
                textureDefs.add(builder(officialKey, elementId).type(ConfigType.CHOICE).textureTab(texture)
                        .defaultValue(fileName).choices(ElementTextureDefinition.getTextureFileName("00001", elementId, texture)).build());
            }
            textureDefs.add(builder(customKey, elementId).type(ConfigType.STRING).textureTab(texture).defaultValue("").build());
            textureDefs.add(builder(modeKey, elementId).type(ConfigType.CHOICE).textureTab(texture)
                    .defaultValue("official").choices("official", "custom", "vanilla").build());
            if (vanillaDefault != null) {
                textureDefs.add(builder(vanillaKey, elementId).type(ConfigType.CHOICE).textureTab(texture).defaultValue(vanillaDefault).build());
            }
            register(textureDefs);
        }
    }

    private static ConfigKeyDefinition.Builder builder(String key, String elementId) {
        return ConfigKeyDefinition.builder().key(key).element(elementId).category(Category.TEXTURE);
    }

    private static String resolveVanillaDefaultTexture(String elementId, String textureKey) {
        if ("kill_icon/scrolling".equals(elementId)) {
            return switch (textureKey) {
                case "default" -> "minecraft:item/ender_pearl";
                case "headshot" -> "minecraft:item/ender_eye";
                case "explosion" -> "minecraft:item/fire_charge";
                case "crit" -> "minecraft:item/magma_cream";
                case "destroy_vehicle" -> "minecraft:item/blaze_powder";
                case "assist" -> "minecraft:item/slime_ball";
                default -> "minecraft:item/ender_pearl";
            };
        }
        if ("kill_icon/battlefield1".equals(elementId)) {
            return switch (textureKey) {
                case "default" -> "minecraft:item/ender_pearl";
                case "headshot" -> "minecraft:item/ender_eye";
                case "explosion" -> "minecraft:item/fire_charge";
                case "crit" -> "minecraft:item/magma_cream";
                case "destroy_vehicle" -> "minecraft:item/blaze_powder";
                default -> "minecraft:item/ender_pearl";
            };
        }
        if ("kill_icon/combo".equals(elementId)) {
            return switch (textureKey) {
                case "combo_1" -> "minecraft:item/coal";
                case "combo_2" -> "minecraft:item/copper_ingot";
                case "combo_3" -> "minecraft:item/iron_ingot";
                case "combo_4" -> "minecraft:item/gold_ingot";
                case "combo_5" -> "minecraft:item/diamond";
                case "combo_6" -> "minecraft:item/netherite_ingot";
                default -> "minecraft:item/coal";
            };
        }
        if ("kill_icon/valorant".equals(elementId)) {
            return switch (textureKey) {
                case "emblem" -> "minecraft:item/nether_star";
                case "frame" -> "minecraft:item/echo_shard";
                case "bar" -> "minecraft:item/amethyst_shard";
                case "headshot" -> "minecraft:item/firework_star";
                default -> "minecraft:item/nether_star";
            };
        }
        if ("kill_icon/card".equals(elementId)) {
            if (textureKey != null && textureKey.contains("assist")) {
                return "minecraft:item/slime_ball";
            }
            return "minecraft:item/netherite_ingot";
        }
        if ("kill_icon/card_bar".equals(elementId)) {
            if (textureKey != null && textureKey.contains("assist")) {
                return "minecraft:item/slime_ball";
            }
            return "minecraft:item/netherite_ingot";
        }
        return null;
    }

    /** 便捷: 供加分项/击杀类型注册器注入动态 format 键。 */
    public static void registerFormatKey(String elementId, String key, String defaultValue) {
        register(ConfigKeyDefinition.builder()
                .key(key)
                .element(elementId)
                .type(ConfigType.STRING)
                .category(Category.CONTENT)
                .defaultValue(defaultValue)
                .build());
    }
}
