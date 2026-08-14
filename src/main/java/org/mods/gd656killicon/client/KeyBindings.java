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
        return mapping.matches(keyCode, 0);
    }

    private static boolean scoreboardKeyPressed = false;

    /**
     * 客户端 tick 轮询按键状态（替代 Forge InputEvent.Key）
     */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            scoreboardKeyPressed = false;
            return;
        }

        handleScoreboardKey(mc);

        if (mc.screen == null) {
            handleConfigKey(mc);
        }
    }

    private static void handleScoreboardKey(Minecraft mc) {
        // 必须读 GLFW/原始键位：打开 Screen 后 KeyMapping.isDown() 会被清空或不更新，
        // 用 isDown() 会导致按住 TAB 时反复开合 → 整页闪烁。
        boolean down = isRawKeyDown(OPEN_SCOREBOARD);
        if (down && !scoreboardKeyPressed) {
            scoreboardKeyPressed = true;
            if (mc.screen == null) {
                mc.setScreen(new MainConfigScreen(null, 3, true));
            }
        } else if (!down && scoreboardKeyPressed) {
            scoreboardKeyPressed = false;
            if (mc.screen instanceof MainConfigScreen screen && screen.isQuickScoreboardMode() && screen.shouldCloseQuickScoreboardOnRelease()) {
                mc.setScreen(null);
            }
        }
    }

    private static boolean isRawKeyDown(KeyMapping mapping) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return false;
        }
        // 1.21 Mojmap 无公开 getKey()；saveString() 返回当前绑定键名
        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        long windowHandle = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(windowHandle, key.getValue());
        }
        return false;
    }

    private static void handleConfigKey(Minecraft mc) {
        if (OPEN_CONFIG.consumeClick()) {
            mc.setScreen(new MainConfigScreen(null));
        }
    }
}
