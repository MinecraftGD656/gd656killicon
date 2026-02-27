package org.mods.gd656killicon.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * HUD 元素管理器，负责渲染器的注册、事件分发与渲染调度。
 */
public class HudElementManager {
    private static final Map<String, Map<String, IHudRenderer>> renderers = new HashMap<>();

    /**
     * 注册渲染器。
     */
    public static void register(String category, String name, IHudRenderer renderer) {
        renderers.computeIfAbsent(category, k -> new HashMap<>()).put(name, renderer);
    }

    /**
     * 触发指定渲染器的显示。
     *
     * @param category 类别
     * @param name     名称
     * @param context  触发上下文
     */
    public static void trigger(String category, String name, IHudRenderer.TriggerContext context) {
        Map<String, IHudRenderer> categoryMap = renderers.get(category);
        if (categoryMap != null) {
            IHudRenderer renderer = categoryMap.get(name);
            if (renderer != null) {
                renderer.trigger(context);
            }
        }
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(HudElementManager.class);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        
        
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        try {
            for (Map<String, IHudRenderer> categoryMap : renderers.values()) {
                for (IHudRenderer renderer : categoryMap.values()) {
                    renderer.render(event.getGuiGraphics(), event.getPartialTick());
                }
            }
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }
}
