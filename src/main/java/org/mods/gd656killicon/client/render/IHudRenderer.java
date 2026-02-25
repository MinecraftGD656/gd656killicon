package org.mods.gd656killicon.client.render;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 客户端 HUD 渲染器接口。
 * 定义了统一的渲染与触发规范。
 */
public interface IHudRenderer {
    /**
     * 渲染逻辑实现。
     *
     * @param guiGraphics 渲染画布
     * @param partialTick 渲染间隔偏移
     */
    void render(GuiGraphics guiGraphics, float partialTick);

    /**
     * 触发 HUD 元素的显示。
     * 所有的名称解析、本地化逻辑应在实现类中通过 ID 自行处理。
     *
     * @param context 触发上下文，包含触发所需的所有原始数据
     */
    void trigger(TriggerContext context);

    /**
     * 触发上下文，用于封装触发事件所需的原始数据。
     */
    record TriggerContext(int type, int entityId, int comboCount, String extraData, float distance) {
        public static TriggerContext of(int type, int entityId) {
            return new TriggerContext(type, entityId, 0, "", 0.0f);
        }

        public static TriggerContext of(int type, int entityId, int comboCount) {
            return new TriggerContext(type, entityId, comboCount, "", 0.0f);
        }
        
        public static TriggerContext of(int type, int entityId, int comboCount, String extraData) {
            return new TriggerContext(type, entityId, comboCount, extraData, 0.0f);
        }

        public static TriggerContext of(int type, int entityId, int comboCount, String extraData, float distance) {
            return new TriggerContext(type, entityId, comboCount, extraData, distance);
        }

        public static TriggerContext of(int type, String extraData) {
            return new TriggerContext(type, -1, 0, extraData, 0.0f);
        }
    }
}
