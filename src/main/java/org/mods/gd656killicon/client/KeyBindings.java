package org.mods.gd656killicon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.gd656killicon";
    public static final String OPEN_CONFIG_KEY = "key.gd656killicon.open_config";
    public static final String OPEN_SCOREBOARD_KEY = "key.gd656killicon.open_scoreboard";

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            OPEN_CONFIG_KEY,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping OPEN_SCOREBOARD = new KeyMapping(
            OPEN_SCOREBOARD_KEY,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            CATEGORY
    );

    /**
     * 检查给定的键码是否匹配指定的按键绑定
     */
    public static boolean matches(KeyMapping mapping, int keyCode) {
        return mapping.getKey().getValue() == keyCode;
    }
}
