package org.mods.gd656killicon.client.textures;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class IconTextureAnimationManager {
    private static final long SWITCH_INTERVAL_MS = 1000L;
    private static final Object INIT_LOCK = new Object();
    private static volatile boolean initialized = false;
    private static volatile boolean enabled = true;
    private static List<String> texturePool = new ArrayList<>();

    public static void setEnabled(boolean enabled) {
        IconTextureAnimationManager.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String resolveTexturePath(String originalPath) {
        // Framework retained, but random switching disabled for now as per requirements.
        return originalPath;
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (initialized) {
                return;
            }
            Set<String> pool = new LinkedHashSet<>(ExternalTextureManager.getDefaultTexturePaths());
            pool.add("killicon_scrolling_destroyvehicle.png");
            pool.add("killicon_scrolling_destroyvehicle_assist.png");
            pool.add("killicon_battlefield1_destroyvehicle.png");
            texturePool = new ArrayList<>(pool);
            initialized = true;
        }
    }
}
