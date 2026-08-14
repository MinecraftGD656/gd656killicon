package org.mods.gd656killicon.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.mods.gd656killicon.client.bridge.ClientBridge;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;

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

    /**
     * 统一清除所有已注册渲染器的显示状态(配置界面关闭时调用)。
     * 每个渲染器通过 IHudRenderer.resetPreview() 清空自身预览/触发残留。
     */
    public static void clearAllPreviews() {
        for (Map<String, IHudRenderer> categoryMap : renderers.values()) {
            for (IHudRenderer renderer : categoryMap.values()) {
                renderer.resetPreview();
            }
        }
    }

    public static void init() {
        // HUD 渲染事件由 FabricClientEvents 通过 ClientGuiEvent.RENDER_HUD 驱动
    }

    public static void onRenderGuiOverlay(GuiGraphics guiGraphics, float partialTick, boolean isMainOverlayPass) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
            return;
        }

        if (!isMainOverlayPass) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        try {
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            String presetId = ConfigManager.getCurrentPresetId();
            for (Map.Entry<String, Map<String, IHudRenderer>> categoryEntry : renderers.entrySet()) {
                String category = categoryEntry.getKey();
                for (Map.Entry<String, IHudRenderer> rendererEntry : categoryEntry.getValue().entrySet()) {
                    String elementId = category + "/" + rendererEntry.getKey();
                    IHudRenderer renderer = rendererEntry.getValue();
                    JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                    float rotationAngle = config != null && config.has("rotation_angle") ? config.get("rotation_angle").getAsFloat() : 0.0f;
                    if (Math.abs(rotationAngle) <= 0.001f) {
                        renderer.render(guiGraphics, partialTick);
                        continue;
                    }
                    String screenAnchor = config != null && config.has("screen_anchor")
                            ? config.get("screen_anchor").getAsString() : org.mods.gd656killicon.client.render.ScreenAnchor.DEFAULT;
                    int xOffset = config != null && config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
                    int yOffset = config != null && config.has("y_offset") ? config.get("y_offset").getAsInt() : 0;
                    float pivotX = org.mods.gd656killicon.client.render.ScreenAnchor.resolveCenterX(screenAnchor, xOffset, screenWidth);
                    float pivotY = org.mods.gd656killicon.client.render.ScreenAnchor.resolveCenterY(screenAnchor, yOffset, screenHeight);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(pivotX, pivotY, 0.0f);
                    guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotationAngle));
                    guiGraphics.pose().translate(-pivotX, -pivotY, 0.0f);
                    renderer.render(guiGraphics, partialTick);
                    guiGraphics.pose().popPose();
                }
            }
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }
}
