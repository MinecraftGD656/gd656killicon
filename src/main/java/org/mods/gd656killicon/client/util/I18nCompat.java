package org.mods.gd656killicon.client.util;

import net.minecraft.client.resources.language.I18n;

public final class I18nCompat {
    private I18nCompat() {
    }

    public static boolean exists(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            return !I18n.get(key).equals(key);
        } catch (Exception ignored) {
            return false;
        }
    }
}
