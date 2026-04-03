package org.mods.gd656killicon.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
            Minecraft mc = Minecraft.getInstance();
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
                        renderer.render(event.getGuiGraphics(), event.getPartialTick());
                        continue;
                    }
                    float pivotX = screenWidth / 2.0f + (config != null && config.has("x_offset") ? config.get("x_offset").getAsInt() : 0);
                    float pivotY = screenHeight - (config != null && config.has("y_offset") ? config.get("y_offset").getAsInt() : 0);
                    event.getGuiGraphics().pose().pushPose();
                    event.getGuiGraphics().pose().translate(pivotX, pivotY, 0.0f);
                    event.getGuiGraphics().pose().mulPose(Axis.ZP.rotationDegrees(rotationAngle));
                    event.getGuiGraphics().pose().translate(-pivotX, -pivotY, 0.0f);
                    renderer.render(event.getGuiGraphics(), event.getPartialTick());
                    event.getGuiGraphics().pose().popPose();
                }
            }
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }
}
