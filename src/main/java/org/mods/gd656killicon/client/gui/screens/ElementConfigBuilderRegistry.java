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
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.gui.elements.PromptDialog;

public class ElementConfigBuilderRegistry {
    private static final Map<String, ElementConfigBuilder> builders = new HashMap<>();
    private static final ElementConfigBuilder DEFAULT_BUILDER = new DefaultElementConfigBuilder();
    
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
        "max_visible_icons",
        "display_interval_ms",
        "max_pending_icons",
        "score_threshold",
        "max_stack_count",
        "texture_animation_total_frames",
        "texture_animation_interval_ms"
    );
    private static final Set<String> TEAM_ELEMENT_IDS = Set.of("kill_icon/card", "kill_icon/card_bar");
    private static List<FixedChoiceConfigEntry.Choice> cachedVanillaItemChoices = null;
    private static String cachedVanillaItemLanguage = null;
    
    public static void register(String elementId, ElementConfigBuilder builder) {
        builders.put(elementId, builder);
    }

    public static ElementConfigBuilder getBuilder(String elementId) {
        return builders.getOrDefault(elementId, DEFAULT_BUILDER);
    }

    private static boolean isIntegerKey(String key) {
        if (INTEGER_KEYS.contains(key)) return true;
        if (key.startsWith("anim_")) {
            for (String intKey : INTEGER_KEYS) {
                if (key.endsWith("_" + intKey)) return true;
            }
        }
        return false;
    }

    private static List<FixedChoiceConfigEntry.Choice> getCachedVanillaItemChoices() {
        String languageCode = Minecraft.getInstance().options.languageCode;
        if (cachedVanillaItemChoices != null && languageCode.equals(cachedVanillaItemLanguage)) {
            return cachedVanillaItemChoices;
        }
        List<FixedChoiceConfigEntry.Choice> choices = new ArrayList<>();
        List<net.minecraft.resources.ResourceLocation> itemIds = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && "minecraft".equals(id.getNamespace())) {
                itemIds.add(id);
            }
        }
        itemIds.sort((a, b) -> a.getPath().compareToIgnoreCase(b.getPath()));
        for (net.minecraft.resources.ResourceLocation id : itemIds) {
            Item item = BuiltInRegistries.ITEM.get(id);
            String value = "minecraft:item/" + id.getPath();
            if (ExternalTextureManager.isVanillaTextureAvailable(value)) {
                String label = I18n.get(item.getDescriptionId());
                choices.add(new FixedChoiceConfigEntry.Choice(value, label));
            }
        }
        cachedVanillaItemChoices = choices;
        cachedVanillaItemLanguage = languageCode;
        return cachedVanillaItemChoices;
    }

    private static class DefaultElementConfigBuilder implements ElementConfigBuilder {
        @Override
        public void build(ConfigTabContent content) {
            if (!(content instanceof ElementConfigContent)) return;
            ElementConfigContent elementContent = (ElementConfigContent) content;
            
            String presetId = elementContent.getPresetId();
            String elementId = elementContent.getElementId();
            
            Set<String> configKeys = ElementConfigManager.getConfigKeys(presetId, elementId);
            JsonObject currentConfig = ElementConfigManager.getElementConfig(presetId, elementId);
            JsonObject defaultConfig = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
            
            if (currentConfig == null) currentConfig = new JsonObject();
            
            java.util.function.Function<String, Boolean> getConfigBoolean = (k) -> {
                JsonObject liveConfig = ElementConfigManager.getElementConfig(presetId, elementId);
                if (liveConfig != null && liveConfig.has(k)) return liveConfig.get(k).getAsBoolean();
                JsonObject liveDefault = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
                return liveDefault != null && liveDefault.has(k) && liveDefault.get(k).getAsBoolean();
            };

            java.util.function.Function<String, java.util.function.Supplier<Boolean>> getDependency = (k) -> {
                if (!k.equals("visible") && configKeys.contains("visible")) {
                     return () -> {
                         if (!getConfigBoolean.apply("visible")) return false;
                         
                        if (k.equals("color_flash") && configKeys.contains("enable_flash")) return getConfigBoolean.apply("enable_flash");
                        if (k.equals("glow_intensity") && configKeys.contains("enable_glow_effect")) return getConfigBoolean.apply("enable_glow_effect");
                        if (k.startsWith("ring_effect_")) {
                            if (k.startsWith("ring_effect_crit_") || k.startsWith("ring_effect_normal_")) {
                                return !configKeys.contains("enable_ring_effect_crit") || getConfigBoolean.apply("enable_ring_effect_crit");
                            }
                            if (k.startsWith("ring_effect_headshot_")) {
                                return !configKeys.contains("enable_ring_effect_headshot") || getConfigBoolean.apply("enable_ring_effect_headshot");
                            }
                            if (k.startsWith("ring_effect_explosion_")) {
                                return !configKeys.contains("enable_ring_effect_explosion") || getConfigBoolean.apply("enable_ring_effect_explosion");
                            }
                        }
                         
                         if (k.equals("combo_reset_timeout")) {
                             JsonObject liveConfig = ElementConfigManager.getElementConfig(presetId, elementId);
                             JsonObject liveDefault = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
                             
                             String killReset = liveConfig != null && liveConfig.has("reset_kill_combo") ? liveConfig.get("reset_kill_combo").getAsString() 
                                                : (liveDefault != null && liveDefault.has("reset_kill_combo") ? liveDefault.get("reset_kill_combo").getAsString() : "death");
                                                
                             String assistReset = liveConfig != null && liveConfig.has("reset_assist_combo") ? liveConfig.get("reset_assist_combo").getAsString()
                                                  : (liveDefault != null && liveDefault.has("reset_assist_combo") ? liveDefault.get("reset_assist_combo").getAsString() : "death");
                                                  
                             return "time".equals(killReset) || "time".equals(assistReset);
                         }
                         
                         if (elementId.equals("kill_icon/battlefield1")) {
                         }
                         
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
                
                return () -> {
                     if (k.equals("color_flash") && configKeys.contains("enable_flash")) return getConfigBoolean.apply("enable_flash");
                     if (k.equals("glow_intensity") && configKeys.contains("enable_glow_effect")) return getConfigBoolean.apply("enable_glow_effect");
                     if (k.startsWith("ring_effect_")) {
                         if (k.startsWith("ring_effect_crit_") || k.startsWith("ring_effect_normal_")) {
                             return !configKeys.contains("enable_ring_effect_crit") || getConfigBoolean.apply("enable_ring_effect_crit");
                         }
                         if (k.startsWith("ring_effect_headshot_")) {
                             return !configKeys.contains("enable_ring_effect_headshot") || getConfigBoolean.apply("enable_ring_effect_headshot");
                         }
                         if (k.startsWith("ring_effect_explosion_")) {
                             return !configKeys.contains("enable_ring_effect_explosion") || getConfigBoolean.apply("enable_ring_effect_explosion");
                         }
                     }
                     
                     if (k.equals("combo_reset_timeout")) {
                         JsonObject liveConfig = ElementConfigManager.getElementConfig(presetId, elementId);
                         JsonObject liveDefault = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
                         
                         String killReset = liveConfig != null && liveConfig.has("reset_kill_combo") ? liveConfig.get("reset_kill_combo").getAsString() 
                                            : (liveDefault != null && liveDefault.has("reset_kill_combo") ? liveDefault.get("reset_kill_combo").getAsString() : "death");
                                            
                         String assistReset = liveConfig != null && liveConfig.has("reset_assist_combo") ? liveConfig.get("reset_assist_combo").getAsString()
                                              : (liveDefault != null && liveDefault.has("reset_assist_combo") ? liveDefault.get("reset_assist_combo").getAsString() : "death");
                                              
                         return "time".equals(killReset) || "time".equals(assistReset);
                     }

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
            };

            List<String> sortedKeys = new java.util.ArrayList<>(configKeys);
            java.util.Collections.sort(sortedKeys);
            
            for (String key : sortedKeys) {
                if (key.equals("enable_icon_effect")) {
                    continue;
                }
                if (key.startsWith("ring_effect_normal_") && configKeys.contains("ring_effect_crit_color")) {
                    continue;
                }
                JsonElement defaultElement = defaultConfig.get(key);
                if (defaultElement == null || !defaultElement.isJsonPrimitive()) {
                    continue; 
                }
                
                com.google.gson.JsonPrimitive primitive = defaultElement.getAsJsonPrimitive();
                String finalPresetId = presetId;
                String finalElementId = elementId;
                String finalKey = key;
                
                String nameKey = "gd656killicon.client.gui.config.element." + elementId.replace("/", ".") + "." + key;
                String displayName;
                
                if (I18n.exists(nameKey)) {
                    displayName = I18n.get(nameKey);
                } else {
                    String genericKey = "gd656killicon.client.gui.config.generic." + key;
                    if (I18n.exists(genericKey)) {
                        displayName = I18n.get(genericKey);
                    } else if (key.startsWith("anim_")) {
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
                
                if (key.startsWith("texture_style_")) {
                    String styleKey = "gd656killicon.client.gui.config.generic.official_texture_select";
                    if (I18n.exists(styleKey)) {
                        displayName = I18n.get(styleKey);
                    }
                }
                if (key.startsWith("custom_texture_")) {
                    String customKey = "gd656killicon.client.gui.config.generic.custom_texture_select";
                    if (I18n.exists(customKey)) {
                        displayName = I18n.get(customKey);
                    }
                }
                if (key.startsWith("texture_mode_")) {
                    String modeKey = "gd656killicon.client.gui.config.generic.texture_select_mode";
                    if (I18n.exists(modeKey)) {
                        displayName = I18n.get(modeKey);
                    }
                }
                if (key.startsWith("vanilla_texture_")) {
                    String vanillaKey = "gd656killicon.client.gui.config.generic.vanilla_texture_select";
                    if (I18n.exists(vanillaKey)) {
                        displayName = I18n.get(vanillaKey);
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
                        "gd656killicon.config.desc." + key,                         currentValue, 
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

                    if (key.startsWith("texture_mode_")) {
                        List<FixedChoiceConfigEntry.Choice> choices = List.of(
                            new FixedChoiceConfigEntry.Choice("custom", I18n.get("gd656killicon.config.choice.texture_mode.custom")),
                            new FixedChoiceConfigEntry.Choice("official", I18n.get("gd656killicon.config.choice.texture_mode.official")),
                            new FixedChoiceConfigEntry.Choice("vanilla", I18n.get("gd656killicon.config.choice.texture_mode.vanilla"))
                        );
                        String textureKey = key.substring("texture_mode_".length());
                        FixedChoiceConfigEntry entry = new FixedChoiceConfigEntry(
                            0, 0, 0, 0,
                            GuiConstants.COLOR_BG,
                            0.3f,
                            displayName,
                            key,
                            "gd656killicon.config.desc.texture_select_mode",
                            currentValue,
                            defaultValue,
                            choices,
                            (newValue) -> {
                                if (newValue != null && newValue.equals(currentValue)) {
                                    return;
                                }
                                ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                                if ("vanilla".equalsIgnoreCase(newValue)) {
                                    JsonObject updated = ElementConfigManager.getElementConfig(finalPresetId, finalElementId);
                                    String vanillaKey = ElementTextureDefinition.getVanillaTextureKey(textureKey);
                                    String vanillaValue = updated != null && updated.has(vanillaKey) ? updated.get(vanillaKey).getAsString() : null;
                                    if (!ExternalTextureManager.isVanillaTextureAvailable(vanillaValue)) {
                                        elementContent.getChoiceListDialog().hide();
                                        elementContent.getPromptDialog().show(I18n.get("gd656killicon.client.gui.prompt.texture_unavailable"), PromptDialog.PromptType.ERROR, null);
                                        ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, "official");
                                        elementContent.rebuildUIFromConfig();
                                        return;
                                    }
                                }
                                String fileName = ElementTextureDefinition.getSelectedTextureFileName(finalPresetId, finalElementId, textureKey);
                                boolean gifDerived = "custom".equalsIgnoreCase(newValue) && ExternalTextureManager.isGifDerivedCustomTexture(finalPresetId, fileName);
                                elementContent.handleTextureBindingChanged(textureKey, fileName, gifDerived);
                            },
                            content.getChoiceListDialog(),
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    } else if (key.startsWith("vanilla_texture_")) {
                        List<FixedChoiceConfigEntry.Choice> choices = getCachedVanillaItemChoices();
                        String textureKey = key.substring("vanilla_texture_".length());
                        FixedChoiceConfigEntry entry = new FixedChoiceConfigEntry(
                            0, 0, 0, 0,
                            GuiConstants.COLOR_BG,
                            0.3f,
                            displayName,
                            key,
                            "gd656killicon.config.desc.vanilla_texture_select",
                            currentValue,
                            defaultValue,
                            choices,
                            (newValue) -> {
                                if (newValue != null && newValue.equals(currentValue)) {
                                    return;
                                }
                                if (!ExternalTextureManager.isVanillaTextureAvailable(newValue)) {
                                    elementContent.getChoiceListDialog().hide();
                                    elementContent.getPromptDialog().show(I18n.get("gd656killicon.client.gui.prompt.texture_unavailable"), PromptDialog.PromptType.ERROR, null);
                                    ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, currentValue);
                                    elementContent.rebuildUIFromConfig();
                                    return;
                                }
                                ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                                elementContent.handleTextureBindingChanged(textureKey, newValue, false);
                            },
                            content.getChoiceListDialog(),
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    } else if (key.startsWith("texture_style_")) {
                        List<FixedChoiceConfigEntry.Choice> choices = new ArrayList<>();
                        for (String fileName : ExternalTextureManager.getAllTextureFileNames()) {
                            String baseName = fileName.endsWith(".png") ? fileName.substring(0, fileName.length() - 4) : fileName;
                            String labelKey = "gd656killicon.client.gui.texture.file." + baseName;
                            String label = I18n.exists(labelKey) ? I18n.get(labelKey) : baseName;
                            choices.add(new FixedChoiceConfigEntry.Choice(fileName, label));
                        }
                        String textureKey = key.substring("texture_style_".length());
                        FixedChoiceConfigEntry entry = new FixedChoiceConfigEntry(
                            0, 0, 0, 0,
                            GuiConstants.COLOR_BG,
                            0.3f,
                            displayName,
                            key,
                            "gd656killicon.config.desc.official_texture_select",
                            currentValue,
                            defaultValue,
                            choices,
                            (newValue) -> {
                                if (newValue != null && newValue.equals(currentValue)) {
                                    return;
                                }
                                ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                                elementContent.handleTextureBindingChanged(textureKey, newValue, false);
                            },
                            content.getChoiceListDialog(),
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    } else if (key.startsWith("custom_texture_")) {
                        List<FixedChoiceConfigEntry.Choice> choices = new ArrayList<>();
                        Map<String, String> customLabels = ExternalTextureManager.getCustomTextureLabels(presetId);
                        for (String fileName : ExternalTextureManager.getCustomTextureFileNames(presetId)) {
                            String label = customLabels.getOrDefault(fileName, fileName);
                            choices.add(new FixedChoiceConfigEntry.Choice(fileName, label));
                        }
                        if (choices.isEmpty()) {
                            choices.add(new FixedChoiceConfigEntry.Choice("", I18n.get("gd656killicon.client.gui.config.choice.custom_texture_none")));
                        }
                        String textureKey = key.substring("custom_texture_".length());
                        FixedChoiceConfigEntry entry = new FixedChoiceConfigEntry(
                            0, 0, 0, 0,
                            GuiConstants.COLOR_BG,
                            0.3f,
                            displayName,
                            key,
                            "gd656killicon.config.desc.custom_texture_select",
                            currentValue,
                            defaultValue,
                            choices,
                            (newValue) -> {
                                if (newValue != null && newValue.equals(currentValue)) {
                                    return;
                                }
                                ElementConfigManager.updateConfigValue(finalPresetId, finalElementId, finalKey, newValue);
                                if (newValue != null && !newValue.isEmpty()) {
                                    boolean gifDerived = ExternalTextureManager.isGifDerivedCustomTexture(finalPresetId, newValue);
                                    elementContent.handleTextureBindingChanged(textureKey, newValue, gifDerived);
                                }
                            },
                            content.getChoiceListDialog(),
                            activeCondition
                        );
                        content.getConfigRows().add(entry);
                    } else if ("team".equals(key) && TEAM_ELEMENT_IDS.contains(elementId)) {
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
                            content.getChoiceListDialog(),
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
                             content.getChoiceListDialog(),
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
                             content.getChoiceListDialog(),
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
                             content.getChoiceListDialog(),
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
