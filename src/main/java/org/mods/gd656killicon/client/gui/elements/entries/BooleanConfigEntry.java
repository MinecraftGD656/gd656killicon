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
        this.setSeparateFirstColumn(true);         this.setHoverInfo(configName, "   " + description);         this.value = initialValue;
        this.defaultValue = defaultValue;
        this.onValueChange = onValueChange;

        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        this.addColumn(getValueText(), 60, getValueColor(), false, true, (btn) -> {
            this.value = !this.value;
            updateState();
            if (this.onValueChange != null) {
                this.onValueChange.accept(this.value);
            }
        });

        this.addColumn("↺", GuiConstants.ROW_HEADER_HEIGHT, getResetButtonColor(), true, true, (btn) -> {
            if (this.value == this.defaultValue) return; 
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
        Column controlCol = getColumn(1);
        if (controlCol != null) {
            controlCol.text = getValueText();
            controlCol.color = getValueColor();
            if (controlCol.textRenderer != null) {
                controlCol.textRenderer.setText(controlCol.text);
                controlCol.textRenderer.setColor(controlCol.color);
            }
        }
        
        Column resetCol = getColumn(2);
        if (resetCol != null) {
            resetCol.color = getResetButtonColor();
            resetCol.text = "↺";
            
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
