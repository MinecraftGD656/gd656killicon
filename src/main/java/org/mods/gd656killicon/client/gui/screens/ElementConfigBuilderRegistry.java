package org.mods.gd656killicon.client.gui.screens;

import org.mods.gd656killicon.client.gui.tabs.ConfigTabContent;
import org.mods.gd656killicon.client.gui.elements.entries.BooleanConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.StringConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.HexColorConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.FloatConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.IntegerConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry;

import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.tabs.ElementConfigContent;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.client.resources.language.I18n;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ElementConfigBuilderRegistry {
    private static final Map<String, ElementConfigBuilder> builders = new HashMap<>();
    private static final ElementConfigBuilder DEFAULT_BUILDER = new DefaultElementConfigBuilder();
    
    // Hex color pattern: # followed by 6 hex digits
    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Set<String> INTEGER_KEYS = Set.of(
        "x_offset",
        "y_offset",
        "line_spacing",
        "max_lines",
        "icon_size",
        "border_size",
        "icon_box_opacity",
        "text_box_opacity",
        "icon_spacing",
        "max_visible_icons",
        "display_interval_ms",
        "max_pending_icons",
        "score_threshold",
        "max_stack_count",
        "texture_animation_total_frames",
        "texture_animation_interval_ms",
        "texture_frame_width_ratio",
        "texture_frame_height_ratio"
    );
    private static final Set<String> TEAM_ELEMENT_IDS = Set.of("kill_icon/card", "kill_icon/card_bar");
    
    public static void register(String elementId, ElementConfigBuilder builder) {
        builders.put(elementId, builder);
    }

    public static ElementConfigBuilder getBuilder(String elementId) {
        return builders.getOrDefault(elementId, DEFAULT_BUILDER);
    }

    // Helper to check if a key should be treated as integer
    private static boolean isIntegerKey(String key) {
        if (INTEGER_KEYS.contains(key)) return true;
        // Check for animation keys with suffixes
        if (key.startsWith("anim_")) {
            for (String intKey : INTEGER_KEYS) {
                if (key.endsWith("_" + intKey)) return true;
            }
        }
        return false;
    }

    private static class DefaultElementConfigBuilder implements ElementConfigBuilder {
        @Override
        public void build(ConfigTabContent content) {
            if (!(content instanceof ElementConfigContent)) return;
            ElementConfigContent elementContent = (ElementConfigContent) content;
            
            String presetId = elementContent.getPresetId();
            String elementId = elementContent.getElementId();
            
            // Get available config keys (current + defaults)
            Set<String> configKeys = ElementConfigManager.getConfigKeys(presetId, elementId);
            JsonObject currentConfig = ElementConfigManager.getElementConfig(presetId, elementId);
            // Use correct default config (preset specific)
            JsonObject defaultConfig = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
            
            if (currentConfig == null) currentConfig = new JsonObject();
            
            // Dependency helper
            java.util.function.Function<String, Boolean> getConfigBoolean = (k) -> {
                JsonObject liveConfig = ElementConfigManager.getElementConfig(presetId, elementId);
                if (liveConfig != null && liveConfig.has(k)) return liveConfig.get(k).getAsBoolean();
                JsonObject liveDefault = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
                return liveDefault != null && liveDefault.has(k) && liveDefault.get(k).getAsBoolean();
            };

            // Dependency Factory
            java.util.function.Function<String, java.util.function.Supplier<Boolean>> getDependency = (k) -> {
                // Global visibility check
                if (!k.equals("visible") && configKeys.contains("visible")) {
                     // We need a supplier that checks both visible AND specific dependency
                     return () -> {
                         if (!getConfigBoolean.apply("visible")) return false;
                         
                         // Specific dependencies
                        if (k.equals("color_flash") && configKeys.contains("enable_flash")) return getConfigBoolean.apply("enable_flash");
                        if (k.equals("glow_intensity") && configKeys.contains("enable_glow_effect")) return getConfigBoolean.apply("enable_glow_effect");
                        if (k.startsWith("ring_effect_") && configKeys.contains("enable_icon_effect")) return getConfigBoolean.apply("enable_icon_effect");
                         
                         if (k.equals("combo_reset_timeout")) {
                             JsonObject liveConfig = ElementConfigManager.getElementConfig(presetId, elementId);
                             JsonObject liveDefault = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
                             
                             String killReset = liveConfig != null && liveConfig.has("reset_kill_combo") ? liveConfig.get("reset_kill_combo").getAsString() 
                                                : (liveDefault != null && liveDefault.has("reset_kill_combo") ? liveDefault.get("reset_kill_combo").getAsString() : "death");
                                                
                             String assistReset = liveConfig != null && liveConfig.has("reset_assist_combo") ? liveConfig.get("reset_assist_combo").getAsString()
                                                  : (liveDefault != null && liveDefault.has("reset_assist_combo") ? liveDefault.get("reset_assist_combo").getAsString() : "death");
                                                  
                             return "time".equals(killReset) || "time".equals(assistReset);
                         }
                         
                         // Battlefield 1 specific
                         if (elementId.equals("kill_icon/battlefield1")) {
                             // Almost everything depends on visible (already checked), but maybe more?
                             // No specific sub-dependencies found in default config.
                         }
                         
                         // Animation dependency logic (inside visible check)
                         if (k.startsWith("anim_")) {
                             String matchingTexture = null;
                             for (String texture : ElementTextureDefinition.getTextures(elementId)) {
                                 String prefix = "anim_" + texture + "_";
                                 if (k.startsWith(prefix)) {
                                     matchingTexture = texture;
                                     break;
                                 }
                             }
                             
                             if (matchingTexture != null) {
                                 String prefix = "anim_" + matchingTexture + "_";
                                 String property = k.substring(prefix.length());
                                 
                                 if (property.equals("enable_texture_animation")) return true;
                                if (property.equals("texture_frame_width_ratio") || property.equals("texture_frame_height_ratio")) {
                                    String enableKey = prefix + "enable_texture_animation";
                                    if (configKeys.contains(enableKey)) {
                                        return !getConfigBoolean.apply(enableKey);
                                    }
                                    return true;
                                }
                                
                                String enableKey = prefix + "enable_texture_animation";
                                 if (configKeys.contains(enableKey)) {
                                     return getConfigBoolean.apply(enableKey);
                                 }
                             }
                         }
                         
                         return true;
                     };
                }
                
                // If visible key doesn't exist (e.g. global settings?), check specific dependencies directly
                return () -> {
                     if (k.equals("color_flash") && configKeys.contains("enable_flash")) return getConfigBoolean.apply("enable_flash");
                     if (k.equals("glow_intensity") && configKeys.contains("enable_glow_effect")) return getConfigBoolean.apply("enable_glow_effect");
                     if (k.startsWith("ring_effect_") && configKeys.contains("enable_icon_effect")) return getConfigBoolean.apply("enable_icon_effect");
                     
                     if (k.equals("combo_reset_timeout")) {
                         JsonObject liveConfig = ElementConfigManager.getElementConfig(presetId, elementId);
                         JsonObject liveDefault = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
                         
                         String killReset = liveConfig != null && liveConfig.has("reset_kill_combo") ? liveConfig.get("reset_kill_combo").getAsString() 
                                            : (liveDefault != null && liveDefault.has("reset_kill_combo") ? liveDefault.get("reset_kill_combo").getAsString() : "death");
                                            
                         String assistReset = liveConfig != null && liveConfig.has("reset_assist_combo") ? liveConfig.get("reset_assist_combo").getAsString()
                                              : (liveDefault != null && liveDefault.has("reset_assist_combo") ? liveDefault.get("reset_assist_combo").getAsString() : "death");
                                              
                         return "time".equals(killReset) || "time".equals(assistReset);
                     }

                     // Animation dependency logic
                     if (k.startsWith("anim_")) {
                         // Robust parsing: Find which texture this key belongs to
                         String matchingTexture = null;
                         for (String texture : ElementTextureDefinition.getTextures(elementId)) {
                             // Check if key starts with "anim_" + texture + "_"
                             String prefix = "anim_" + texture + "_";
                             if (k.startsWith(prefix)) {
                                 matchingTexture = texture;
                                 break;
                             }
                         }
                         
                         if (matchingTexture != null) {
                             String prefix = "anim_" + matchingTexture + "_";
                             String property = k.substring(prefix.length());
                             
                             // Always enabled properties
                             if (property.equals("enable_texture_animation")) return true;
                             if (property.equals("texture_frame_width_ratio") || property.equals("texture_frame_height_ratio")) {
                                 String enableKey = prefix + "enable_texture_animation";
                                 if (configKeys.contains(enableKey)) {
                                     return !getConfigBoolean.apply(enableKey);
                                 }
                                 return true;
                             }
                             
                             // Dependent properties
                             String enableKey = prefix + "enable_texture_animation";
                             if (configKeys.contains(enableKey)) {
                                 return getConfigBoolean.apply(enableKey);
                             }
                         }
                     }
                     
                     return true;
                };
            };

            // Prepare keys list
            List<String> sortedKeys = new java.util.ArrayList<>(configKeys);
            // Sort keys to ensure consistent order (though ElementConfigContent will filter them)
            java.util.Collections.sort(sortedKeys);
            
            // Iterate and add config entries
            for (String key : sortedKeys) {
                // Determine value type from default config
                JsonElement defaultElement = defaultConfig.get(key);
                if (defaultElement == null || !defaultElement.isJsonPrimitive()) {
                    continue; 
                }
                
                com.google.gson.JsonPrimitive primitive = defaultElement.getAsJsonPrimitive();
                String finalPresetId = presetId;
                String finalElementId = elementId;
                String finalKey = key;
                
                // Display Name Logic
                String nameKey = "gd656killicon.client.gui.config.element." + elementId.replace("/", ".") + "." + key;
                String displayName;
                
                if (I18n.exists(nameKey)) {
                    displayName = I18n.get(nameKey);
                } else {
                    // Try generic key
                    String genericKey = "gd656killicon.client.gui.config.generic." + key;
                    if (I18n.exists(genericKey)) {
                        displayName = I18n.get(genericKey);
                    } else if (key.startsWith("anim_")) {
                        // Robust prefix stripping
                        String matchingTexture = null;
                        for (String texture : ElementTextureDefinition.getTextures(elementId)) {
                            if (key.startsWith("anim_" + texture + "_")) {
                                matchingTexture = texture;
                                break;
                            }
                        }
                        
                        if (matchingTexture != null) {
                            String prefix = "anim_" + matchingTexture + "_";
                            String actualProperty = key.substring(prefix.length());
                            String animGenericKey = "gd656killicon.client.gui.config.generic." + actualProperty;
                            if (I18n.exists(animGenericKey)) {
                                displayName = I18n.get(animGenericKey);
                            } else {
                                displayName = key;
                            }
                        } else {
                            displayName = key;
                        }
                    } else {
                        displayName = key;
                    }
                }
                
                java.util.function.Supplier<Boolean> activeCondition = getDependency.apply(key);
                
                if (primitive.isBoolean()) {
                    boolean defaultValue = primitive.getAsBoolean();
                    boolean currentValue = currentConfig.has(key) ? currentConfig.get(key).getAsBoolean() : defaultValue;
                    
                    BooleanConfigEntry entry = new BooleanConfigEntry(
                        0, 0, 0, 0, 
                        GuiConstants.COLOR_BG, 
                        0.3f, 
                        displayName,
                        key,
                        "gd656killicon.config.desc." + key, // Placeholder description key
                        currentValue, 
                        defaultValue, 
                        (newValue) -> {
                            ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, String.valueOf(newValue));
                        },
                        activeCondition
                    );
                    content.getConfigRows().add(entry);
                } else if (primitive.isNumber()) {
                    if (isIntegerKey(key)) {
                        int defaultValue = primitive.getAsInt();
                        int currentValue = currentConfig.has(key) ? currentConfig.get(key).getAsInt() : defaultValue;
                        IntegerConfigEntry entry = new IntegerConfigEntry(
                            0, 0, 0, 0,
                            GuiConstants.COLOR_BG,
                            0.3f,
                            displayName,
                            key,
                            "gd656killicon.config.desc." + key,
                            currentValue,
                            defaultValue,
                            (newValue) -> {
                                ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, String.valueOf(newValue));
                            },
                            content.getTextInputDialog(),
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    } else {
                        float defaultValue = primitive.getAsFloat();
                        float currentValue = currentConfig.has(key) ? currentConfig.get(key).getAsFloat() : defaultValue;
                        FloatConfigEntry entry = new FloatConfigEntry(
                            0, 0, 0, 0,
                            GuiConstants.COLOR_BG,
                            0.3f,
                            displayName,
                            key,
                            "gd656killicon.config.desc." + key,
                            currentValue,
                            defaultValue,
                            (newValue) -> {
                                ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, String.valueOf(newValue));
                            },
                            content.getTextInputDialog(),
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    }
                } else if (primitive.isString()) {
                    String defaultValue = primitive.getAsString();
                    String currentValue = currentConfig.has(key) ? currentConfig.get(key).getAsString() : defaultValue;

                    boolean isColorConfig = key.startsWith("color_") || HEX_PATTERN.matcher(defaultValue).matches();

                    if ("team".equals(key) && TEAM_ELEMENT_IDS.contains(elementId)) {
                        List<FixedChoiceConfigEntry.Choice> choices = List.of(
                            new FixedChoiceConfigEntry.Choice("ct", "CT"),
                            new FixedChoiceConfigEntry.Choice("t", "T")
                        );
                        FixedChoiceConfigEntry entry = new FixedChoiceConfigEntry(
                            0, 0, 0, 0,
                            GuiConstants.COLOR_BG,
                            0.3f,
                            displayName,
                            key,
                            "gd656killicon.config.desc." + key,
                            currentValue,
                            defaultValue,
                            choices,
                            (newValue) -> {
                                ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                            },
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    } else if (key.endsWith("texture_animation_orientation")) {
                         List<FixedChoiceConfigEntry.Choice> choices = List.of(
                             new FixedChoiceConfigEntry.Choice("horizontal", I18n.get("gd656killicon.config.choice.horizontal")),
                             new FixedChoiceConfigEntry.Choice("vertical", I18n.get("gd656killicon.config.choice.vertical"))
                         );
                         FixedChoiceConfigEntry entry = new FixedChoiceConfigEntry(
                             0, 0, 0, 0,
                             GuiConstants.COLOR_BG,
                             0.3f,
                             displayName,
                             key,
                             "gd656killicon.config.desc." + key,
                             currentValue,
                             defaultValue,
                             choices,
                             (newValue) -> {
                                 ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                             },
                             activeCondition
                         );
                         content.getConfigRows().add(entry);
                    } else if (key.endsWith("texture_animation_play_style")) {
                         List<FixedChoiceConfigEntry.Choice> choices = List.of(
                             new FixedChoiceConfigEntry.Choice("sequential", I18n.get("gd656killicon.config.choice.sequential")),
                             new FixedChoiceConfigEntry.Choice("reverse", I18n.get("gd656killicon.config.choice.reverse")),
                             new FixedChoiceConfigEntry.Choice("pingpong", I18n.get("gd656killicon.config.choice.pingpong")),
                             new FixedChoiceConfigEntry.Choice("random", I18n.get("gd656killicon.config.choice.random"))
                         );
                         FixedChoiceConfigEntry entry = new FixedChoiceConfigEntry(
                             0, 0, 0, 0,
                             GuiConstants.COLOR_BG,
                             0.3f,
                             displayName,
                             key,
                             "gd656killicon.config.desc." + key,
                             currentValue,
                             defaultValue,
                             choices,
                             (newValue) -> {
                                 ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                             },
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    } else if (key.equals("reset_kill_combo") || key.equals("reset_assist_combo")) {
                         List<FixedChoiceConfigEntry.Choice> choices = key.equals("reset_assist_combo")
                             ? List.of(
                                 new FixedChoiceConfigEntry.Choice("death", I18n.get("gd656killicon.config.choice.reset_death")),
                                 new FixedChoiceConfigEntry.Choice("time", I18n.get("gd656killicon.config.choice.reset_time")),
                                 new FixedChoiceConfigEntry.Choice("logout", I18n.get("gd656killicon.config.choice.reset_logout")),
                                 new FixedChoiceConfigEntry.Choice("never", I18n.get("gd656killicon.config.choice.reset_never"))
                             )
                             : List.of(
                                 new FixedChoiceConfigEntry.Choice("server", I18n.get("gd656killicon.config.choice.reset_server")),
                                 new FixedChoiceConfigEntry.Choice("death", I18n.get("gd656killicon.config.choice.reset_death")),
                                 new FixedChoiceConfigEntry.Choice("time", I18n.get("gd656killicon.config.choice.reset_time")),
                                 new FixedChoiceConfigEntry.Choice("logout", I18n.get("gd656killicon.config.choice.reset_logout")),
                                 new FixedChoiceConfigEntry.Choice("never", I18n.get("gd656killicon.config.choice.reset_never"))
                             );
                         FixedChoiceConfigEntry entry = new FixedChoiceConfigEntry(
                             0, 0, 0, 0,
                             GuiConstants.COLOR_BG,
                             0.3f,
                             displayName,
                             key,
                             "gd656killicon.config.desc." + key,
                             currentValue,
                             defaultValue,
                             choices,
                             (newValue) -> {
                                 ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                             },
                             activeCondition
                         );
                         content.getConfigRows().add(entry);
                    } else if (isColorConfig) {
                        HexColorConfigEntry entry = new HexColorConfigEntry(
                            0, 0, 0, 0,
                            GuiConstants.COLOR_BG,
                            0.3f,
                            displayName,
                            key,
                            "gd656killicon.config.desc." + key,
                            currentValue,
                            defaultValue,
                            (newValue) -> {
                                ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                            },
                            content.getTextInputDialog(),
                            content.getColorPickerDialog(),
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    } else {
                        StringConfigEntry entry = new StringConfigEntry(
                            0, 0, 0, 0,
                            GuiConstants.COLOR_BG,
                            0.3f,
                            displayName,
                            key,
                            "gd656killicon.config.desc." + key,
                            currentValue,
                            defaultValue,
                            (newValue) -> {
                                ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                            },
                            content.getTextInputDialog(),
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    }
                }
            }
        }
    }
}
