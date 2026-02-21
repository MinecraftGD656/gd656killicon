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
        "icon_animation_frames",
        "icon_frame_duration"
    );
    private static final Set<String> TEAM_ELEMENT_IDS = Set.of("kill_icon/card", "kill_icon/card_bar");
    
    // Animation Config Keys
    private static final List<String> ICON_ANIMATION_KEYS = List.of(
        "enable_icon_animation",
        "icon_animation_frames",
        "icon_frame_height_ratio",
        "icon_frame_width_ratio",
        "icon_animation_orientation",
        "icon_frame_duration",
        "icon_animation_loop",
        "icon_animation_style"
    );

    public static void register(String elementId, ElementConfigBuilder builder) {
        builders.put(elementId, builder);
    }

    public static ElementConfigBuilder getBuilder(String elementId) {
        return builders.getOrDefault(elementId, DEFAULT_BUILDER);
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
                         
                         // Animation dependencies
                         if (ICON_ANIMATION_KEYS.contains(k) && !k.equals("enable_icon_animation")) {
                             return getConfigBoolean.apply("enable_icon_animation");
                         }
                         
                         // Battlefield 1 specific
                         if (elementId.equals("kill_icon/battlefield1")) {
                             // Almost everything depends on visible (already checked), but maybe more?
                             // No specific sub-dependencies found in default config.
                         }
                         
                         return true;
                     };
                }
                
                // If visible key doesn't exist (e.g. global settings?), check specific dependencies directly
                return () -> {
                     if (k.equals("color_flash") && configKeys.contains("enable_flash")) return getConfigBoolean.apply("enable_flash");
                     if (k.equals("glow_intensity") && configKeys.contains("enable_glow_effect")) return getConfigBoolean.apply("enable_glow_effect");
                     if (ICON_ANIMATION_KEYS.contains(k) && !k.equals("enable_icon_animation")) {
                         return getConfigBoolean.apply("enable_icon_animation");
                     }
                     return true;
                };
            };

            // Prepare keys list (Normal keys first, then Animation keys if applicable)
            List<String> sortedKeys = new java.util.ArrayList<>();
            List<String> animKeys = new java.util.ArrayList<>();
            
            for (String key : configKeys) {
                if (ICON_ANIMATION_KEYS.contains(key)) {
                    animKeys.add(key);
                } else {
                    sortedKeys.add(key);
                }
            }
            
            // Add animation keys at the end in specific order
            if (!animKeys.isEmpty()) {
                for (String animKey : ICON_ANIMATION_KEYS) {
                    if (animKeys.contains(animKey)) {
                        sortedKeys.add(animKey);
                    }
                }
            }
            
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
                String nameKey = "gd656killicon.client.gui.config.element." + elementId.replace("/", ".") + "." + key;
                // Fallback to generic key if specific key doesn't exist
                String displayName = I18n.exists(nameKey) ? I18n.get(nameKey) : 
                                     (I18n.exists("gd656killicon.client.gui.config.generic." + key) ? I18n.get("gd656killicon.client.gui.config.generic." + key) : key);
                
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
                    if (INTEGER_KEYS.contains(key)) {
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
                    } else if ("icon_animation_orientation".equals(key)) {
                        List<FixedChoiceConfigEntry.Choice> choices = List.of(
                            new FixedChoiceConfigEntry.Choice("vertical", "Vertical"),
                            new FixedChoiceConfigEntry.Choice("horizontal", "Horizontal")
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
                    } else if ("icon_animation_style".equals(key)) {
                        List<FixedChoiceConfigEntry.Choice> choices = List.of(
                            new FixedChoiceConfigEntry.Choice("sequential", "Sequential"),
                            new FixedChoiceConfigEntry.Choice("reverse", "Reverse"),
                            new FixedChoiceConfigEntry.Choice("pingpong", "Ping Pong"),
                            new FixedChoiceConfigEntry.Choice("random", "Random")
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
