package org.mods.gd656killicon.client.gui.elements.entries;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;

/**
 * 布尔类型配置行渲染器
 */
public class BooleanConfigEntry extends GDRowRenderer {
    private boolean value;
    private final boolean defaultValue;
    private final String key;

    private final Consumer<Boolean> onValueChange;

    public BooleanConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, boolean initialValue, boolean defaultValue, Consumer<Boolean> onValueChange) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, () -> true);
    }

    public BooleanConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, boolean initialValue, boolean defaultValue, Consumer<Boolean> onValueChange, Supplier<Boolean> activeCondition) {
        super(x1, y1, x2, y2, bgColor, bgAlpha, false);
        this.key = configId;
        this.setActiveCondition(activeCondition);
        this.setSeparateFirstColumn(true); // 第一列与后续列之间增加1像素间隔
        this.setHoverInfo(configName, "   " + description); // 设置悬停显示信息，简介前加两个空格
        this.value = initialValue;
        this.defaultValue = defaultValue;
        this.onValueChange = onValueChange;

        // 1. 文本显示区域 (自适应)
        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        // 2. 主要控制区 (80px)
        this.addColumn(getValueText(), 60, getValueColor(), false, true, (btn) -> {
            this.value = !this.value;
            updateState();
            if (this.onValueChange != null) {
                this.onValueChange.accept(this.value);
            }
        });

        // 3. 重置按钮 (GuiConstants.ROW_HEADER_HEIGHT, 深色)
        this.addColumn("R", GuiConstants.ROW_HEADER_HEIGHT, getResetButtonColor(), true, true, (btn) -> {
            if (this.value == this.defaultValue) return; // Already default, do nothing

            // Reset immediately without confirmation
            this.value = this.defaultValue;
            updateState();
            if (this.onValueChange != null) {
                this.onValueChange.accept(this.value);
            }
        });
    }

    public String getKey() {
        return key;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateState() {
        // 更新主要控制区
        Column controlCol = getColumn(1);
        if (controlCol != null) {
            controlCol.text = getValueText();
            controlCol.color = getValueColor();
            // 强制刷新 textRenderer
            if (controlCol.textRenderer != null) {
                controlCol.textRenderer.setText(controlCol.text);
                controlCol.textRenderer.setColor(controlCol.color);
            }
        }
        
        // 更新重置按钮颜色和文本
        Column resetCol = getColumn(2);
        if (resetCol != null) {
            resetCol.color = getResetButtonColor();
            resetCol.text = "R";
            
            if (resetCol.textRenderer != null) {
                resetCol.textRenderer.setColor(resetCol.color);
                resetCol.textRenderer.setText(resetCol.text);
            }
        }
    }

    private String getValueText() {
        return value ? "ON" : "OFF";
    }

    private int getValueColor() {
        return value ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
    }
    
    private int getResetButtonColor() {
        return value == defaultValue ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
    }
}
