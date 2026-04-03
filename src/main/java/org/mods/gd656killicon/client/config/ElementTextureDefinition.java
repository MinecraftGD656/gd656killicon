package org.mods.gd656killicon.client.config;

import com.google.gson.JsonObject;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementTextureDefinition {
    public static final String VALORANT_SKIN_PRIME = "prime";
    public static final String VALORANT_SKIN_GLITCHPOP = "glitchpop";
    public static final String VALORANT_SKIN_SINGULARITY_V1 = "singularity_v1";
    public static final String VALORANT_SKIN_SINGULARITY_V2 = "singularity_v2";
    public static final String VALORANT_SKIN_SINGULARITY_V3 = "singularity_v3";
    public static final String VALORANT_SKIN_GAIA = "gaia";
    public static final String VALORANT_SKIN_GAIA_V1 = "gaia_v1";
    public static final String VALORANT_SKIN_GAIA_V2 = "gaia_v2";
    public static final String VALORANT_SKIN_GAIA_V3 = "gaia_v3";
    public static final Map<String, List<String>> ELEMENT_TEXTURES;

    static {
        Map<String, List<String>> map = new HashMap<>();
        
        map.put("kill_icon/scrolling", Arrays.asList(
            "default", 
            "headshot", 
            "explosion", 
            "crit", 
            "destroy_vehicle", 
            "assist"
        ));
        
        map.put("kill_icon/combo", Arrays.asList(
            "combo_1", 
            "combo_2", 
            "combo_3", 
            "combo_4", 
            "combo_5", 
            "combo_6"
        ));

        map.put("kill_icon/valorant", Arrays.asList(
            "emblem",
            "frame",
            "blade",
            "bar",
            "headshot",
            "base_particle",
            "hero_flame",
            "large_sparks",
            "x_sparks"
        ));
        
        map.put("kill_icon/card", Arrays.asList(
            "default_ct", "default_t", 
            "headshot_ct", "headshot_t", 
            "explosion_ct", "explosion_t", 
            "crit_ct", "crit_t", 
            "light_ct", "light_t"
        ));
        
        map.put("kill_icon/card_bar", Arrays.asList(
            "bar_ct", "bar_t"
        ));
        
        map.put("kill_icon/battlefield1", Arrays.asList(
            "default", 
            "headshot", 
            "explosion", 
            "crit", 
            "destroy_vehicle"
        ));
        
        ELEMENT_TEXTURES = Collections.unmodifiableMap(map);
    }
    
    public static List<String> getTextures(String elementId) {
        return ELEMENT_TEXTURES.getOrDefault(elementId, Collections.emptyList());
    }
    
    public static boolean hasTextures(String elementId) {
        return ELEMENT_TEXTURES.containsKey(elementId);
    }

    public static String getTextureFileName(String elementId, String textureKey) {
        return getTextureFileName(ConfigManager.getCurrentPresetId(), elementId, textureKey);
    }

    public static String getTextureFileName(String presetId, String elementId, String textureKey) {
        if (elementId == null || textureKey == null) {
            return null;
        }
        return switch (elementId) {
            case "kill_icon/scrolling" -> resolveScrollingFileName(presetId, textureKey);
            case "kill_icon/combo" -> "killicon_" + textureKey + ".png";
            case "kill_icon/valorant" -> ValorantStyleCatalog.getOfficialTextureFileName(presetId, textureKey);
            case "kill_icon/card" -> "killicon_card_" + textureKey + ".png";
            case "kill_icon/card_bar" -> "killicon_card_" + textureKey + ".png";
            case "kill_icon/battlefield1" -> "killicon_battlefield1_" + normalizeDestroyVehicle(textureKey) + ".png";
            default -> null;
        };
    }

    public static String getSelectedTextureFileName(String presetId, String elementId, String textureKey) {
        JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
        return getSelectedTextureFileName(presetId, elementId, textureKey, config);
    }

    public static String getSelectedTextureFileName(String presetId, String elementId, String textureKey, JsonObject config) {
        String defaultFileName = getDefaultSelectedTextureFileName(presetId, elementId, textureKey, config);
        if (config == null) {
            return defaultFileName;
        }
        String modeKey = getTextureModeKey(textureKey);
        String mode = config.has(modeKey) ? config.get(modeKey).getAsString() : "official";
        if ("vanilla".equalsIgnoreCase(mode)) {
            String vanillaKey = getVanillaTextureKey(textureKey);
            if (config.has(vanillaKey)) {
                String path = config.get(vanillaKey).getAsString();
                if (ExternalTextureManager.isVanillaTexturePath(path)) {
                    return path;
                }
            }
            return defaultFileName;
        }
        if ("custom".equalsIgnoreCase(mode)) {
            String key = getCustomTextureKey(textureKey);
            if (config.has(key)) {
                String fileName = config.get(key).getAsString();
                if (ExternalTextureManager.isCustomTextureName(fileName)) {
                    return fileName;
                }
            }
            String fallbackKey = getOfficialTextureKey(textureKey);
            if (config.has(fallbackKey)) {
                String fileName = config.get(fallbackKey).getAsString();
                if (ExternalTextureManager.isCustomTextureName(fileName)) {
                    return fileName;
                }
            }
            return defaultFileName;
        }
        String officialKey = getOfficialTextureKey(textureKey);
        if (config.has(officialKey)) {
            String fileName = config.get(officialKey).getAsString();
            if (ExternalTextureManager.isOfficialTextureName(fileName)) {
                return fileName;
            }
        }
        return defaultFileName;
    }

    private static String getDefaultSelectedTextureFileName(String presetId, String elementId, String textureKey, JsonObject config) {
        if ("kill_icon/valorant".equals(elementId)) {
            String styleId = ValorantStyleCatalog.resolveStyleId(presetId, config);
            String styleFileName = ValorantStyleCatalog.getOfficialTextureFileNameForStyle(styleId, textureKey);
            if (styleFileName != null) {
                return styleFileName;
            }
        }
        return getTextureFileName(presetId, elementId, textureKey);
    }

    public static String getTextureStyleKey(String textureKey) {
        return "texture_style_" + textureKey;
    }

    public static String getOfficialTextureKey(String textureKey) {
        return "texture_style_" + textureKey;
    }

    public static String getCustomTextureKey(String textureKey) {
        return "custom_texture_" + textureKey;
    }

    public static String getTextureModeKey(String textureKey) {
        return "texture_mode_" + textureKey;
    }

    public static String getVanillaTextureKey(String textureKey) {
        return "vanilla_texture_" + textureKey;
    }

    private static String resolveScrollingFileName(String presetId, String textureKey) {
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
        return "killicon_scrolling_" + normalizeDestroyVehicle(textureKey) + ".png";
    }

    private static String normalizeDestroyVehicle(String textureKey) {
        return "destroy_vehicle".equals(textureKey) ? "destroyvehicle" : textureKey;
    }
}
