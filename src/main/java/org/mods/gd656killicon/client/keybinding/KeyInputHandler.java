package org.mods.gd656killicon.client.keybinding;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.KeyBindings;
import org.mods.gd656killicon.client.gui.MainConfigScreen;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 1. 处理计分板快捷键 (TAB) - 需要处理按下和松开逻辑
        handleScoreboardKey(mc, event);

        // 2. 处理常规按键 (如 G 键) - 仅在没有打开界面时处理
        if (mc.screen == null) {
            handleConfigKey(mc, event);
        }
    }

    /**
     * 处理计分板快捷键逻辑 (TAB 键)
     */
    private static void handleScoreboardKey(Minecraft mc, InputEvent.Key event) {
        if (!KeyBindings.matches(KeyBindings.OPEN_SCOREBOARD, event.getKey())) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (mc.screen == null) {
                // 打开计分板页签 (索引 3)，启用快捷模式
                mc.setScreen(new MainConfigScreen(null, 3, true));
            }
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (mc.screen instanceof MainConfigScreen screen && screen.isQuickScoreboardMode()) {
                // 仅在快捷模式下，松开按键关闭界面
                mc.setScreen(null);
            }
        }
    }

    /**
     * 处理配置界面快捷键逻辑 (G 键)
     */
    private static void handleConfigKey(Minecraft mc, InputEvent.Key event) {
        if (KeyBindings.OPEN_CONFIG.consumeClick()) {
            mc.setScreen(new MainConfigScreen(null));
        }
    }
}
