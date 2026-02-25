package org.mods.gd656killicon.client.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementTextureDefinition {
    public static final Map<String, List<String>> ELEMENT_TEXTURES;

    static {
        Map<String, List<String>> map = new HashMap<>();
        
        // Scrolling: 6 types
        map.put("kill_icon/scrolling", Arrays.asList(
            "default", 
            "headshot", 
            "explosion", 
            "crit", 
            "destroy_vehicle", 
            "assist"
        ));
        
        // Combo: 6 counts
        map.put("kill_icon/combo", Arrays.asList(
            "combo_1", 
            "combo_2", 
            "combo_3", 
            "combo_4", 
            "combo_5", 
            "combo_6"
        ));
        
        // Card: 10 types
        map.put("kill_icon/card", Arrays.asList(
            "default_ct", "default_t", 
            "headshot_ct", "headshot_t", 
            "explosion_ct", "explosion_t", 
            "crit_ct", "crit_t", 
            "light_ct", "light_t"
        ));
        
        // Card Bar: 2 types
        map.put("kill_icon/card_bar", Arrays.asList(
            "bar_ct", "bar_t"
        ));
        
        // Battlefield 1: 5 types
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
        if (elementId == null || textureKey == null) {
            return null;
        }
        return switch (elementId) {
            case "kill_icon/scrolling" -> "killicon_scrolling_" + normalizeDestroyVehicle(textureKey) + ".png";
            case "kill_icon/combo" -> "killicon_" + textureKey + ".png";
            case "kill_icon/card" -> "killicon_card_" + textureKey + ".png";
            case "kill_icon/card_bar" -> "killicon_card_" + textureKey + ".png";
            case "kill_icon/battlefield1" -> "killicon_battlefield1_" + normalizeDestroyVehicle(textureKey) + ".png";
            default -> null;
        };
    }

    private static String normalizeDestroyVehicle(String textureKey) {
        return "destroy_vehicle".equals(textureKey) ? "destroyvehicle" : textureKey;
    }
}
