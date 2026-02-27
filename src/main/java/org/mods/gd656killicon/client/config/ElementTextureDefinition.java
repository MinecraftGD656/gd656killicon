package org.mods.gd656killicon.client.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonObject;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;

public class ElementTextureDefinition {
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
        String defaultFileName = getTextureFileName(presetId, elementId, textureKey);
        if (config == null) {
            return defaultFileName;
        }
        String key = getTextureStyleKey(textureKey);
        if (config.has(key)) {
            String fileName = config.get(key).getAsString();
            if (ExternalTextureManager.isValidTextureName(fileName)) {
                return fileName;
            }
        }
        return defaultFileName;
    }

    public static String getTextureStyleKey(String textureKey) {
        return "texture_style_" + textureKey;
    }

    private static String resolveScrollingFileName(String presetId, String textureKey) {
        return "killicon_scrolling_" + normalizeDestroyVehicle(textureKey) + ".png";
    }

    private static String normalizeDestroyVehicle(String textureKey) {
        return "destroy_vehicle".equals(textureKey) ? "destroyvehicle" : textureKey;
    }
}
