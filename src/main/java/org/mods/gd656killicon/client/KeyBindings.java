package org.mods.gd656killicon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.gui.MainConfigScreen;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        handleScoreboardKey(mc, event);

        if (mc.screen == null) {
            handleConfigKey(mc);
        }
    }

    private static void handleScoreboardKey(Minecraft mc, InputEvent.Key event) {
        if (!matches(OPEN_SCOREBOARD, event.getKey())) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (mc.screen == null) {
                mc.setScreen(new MainConfigScreen(null, 3, true));
            }
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (mc.screen instanceof MainConfigScreen screen && screen.isQuickScoreboardMode()) {
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
