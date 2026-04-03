package org.mods.gd656killicon.server.logic.core;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerBonusSwitches {
    private static final Set<Integer> DISABLED_BONUS_TYPES = ConcurrentHashMap.newKeySet();

    private ServerBonusSwitches() {}

    public static boolean isEnabled(int bonusType) {
        return !DISABLED_BONUS_TYPES.contains(bonusType);
    }

    public static void setEnabled(int bonusType, boolean enabled) {
        if (enabled) {
            DISABLED_BONUS_TYPES.remove(bonusType);
        } else {
            DISABLED_BONUS_TYPES.add(bonusType);
        }
    }

    public static Set<Integer> getDisabledBonusTypes() {
        return Collections.unmodifiableSet(DISABLED_BONUS_TYPES);
    }
}
