package org.mods.gd656killicon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.mods.gd656killicon.client.gui.MainConfigScreen;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.gd656killicon";
    public static final String OPEN_CONFIG_KEY = "key.gd656killicon.open_config";
    public static final String OPEN_SCOREBOARD_KEY = "key.gd656killicon.open_scoreboard";

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            OPEN_CONFIG_KEY,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping OPEN_SCOREBOARD = new KeyMapping(
            OPEN_SCOREBOARD_KEY,
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

    public static void onKeyInput(int key, int action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        handleScoreboardKey(mc, key, action);

        if (mc.screen == null) {
            handleConfigKey(mc);
        }
    }

    private static void handleScoreboardKey(Minecraft mc, int key, int action) {
        if (!matches(OPEN_SCOREBOARD, key)) return;

        if (action == GLFW.GLFW_PRESS) {
            if (mc.screen == null) {
                mc.setScreen(new MainConfigScreen(null, 3, true));
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            if (mc.screen instanceof MainConfigScreen screen && screen.isQuickScoreboardMode() && screen.shouldCloseQuickScoreboardOnRelease()) {
                mc.setScreen(null);
            }
        }
    }

    private static void handleConfigKey(Minecraft mc) {
        if (OPEN_CONFIG.consumeClick()) {
            mc.setScreen(new MainConfigScreen(null));
        }
    }
}
