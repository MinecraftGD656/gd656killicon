package org.mods.gd656killicon.client.gui.elements.entries;

import net.minecraft.client.gui.GuiGraphics;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.TextInputDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 浮点数类型配置行渲染器 (Parallel to StringConfigEntry)
 */
public class FloatConfigEntry extends GDRowRenderer {
    private float value;
    private final float defaultValue;
    private final Consumer<Float> onValueChange;
    private final String key;

    private final TextInputDialog textInputDialog;
    private final String configName;

    public FloatConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, float initialValue, float defaultValue, Consumer<Float> onValueChange, TextInputDialog textInputDialog) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, () -> true);
    }

    public FloatConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, float initialValue, float defaultValue, Consumer<Float> onValueChange, TextInputDialog textInputDialog, Supplier<Boolean> activeCondition) {
        super(x1, y1, x2, y2, bgColor, bgAlpha, false);
        this.key = configId;
        this.setActiveCondition(activeCondition);
        this.setSeparateFirstColumn(true);
        this.setHoverInfo(configName, "   " + description);
        this.value = initialValue;
        this.defaultValue = defaultValue;
        this.onValueChange = onValueChange;
        this.textInputDialog = textInputDialog;
        this.configName = configName;

        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        this.addColoredColumn(parseColoredText(this.value), 60, false, false, (btn) -> {
            if (this.textInputDialog != null) {
                String initialText = String.format(Locale.ROOT, "%.2f", this.value);
                this.textInputDialog.show(initialText, this.configName, (newValue) -> {
                    try {
                        float f = Float.parseFloat(newValue);
                        this.value = f;
                        updateState();
                        if (this.onValueChange != null) {
                            this.onValueChange.accept(this.value);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }, this::isValidFloat);
            }
        });

        this.addColumn("↺", GuiConstants.ROW_HEADER_HEIGHT, getResetButtonColor(), true, true, (btn) -> {
            if (Math.abs(this.value - this.defaultValue) < 0.0001f) return;

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

    private boolean isValidFloat(String text) {
        if (text == null || text.isEmpty()) return false;
        
        if (text.equals("-")) return false; 
        
        
        return text.matches("^-?\\d+(\\.\\d{0,2})?$") || text.matches("^-?\\.\\d{1,2}$");
    }

    private void updateState() {
        Column controlCol = getColumn(1);
        if (controlCol != null) {
            controlCol.coloredTexts = parseColoredText(this.value);
            controlCol.textRenderer = null;
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

    private int getResetButtonColor() {
        return (Math.abs(value - defaultValue) < 0.0001f) ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
    }

    private List<GDTextRenderer.ColoredText> parseColoredText(float val) {
        List<GDTextRenderer.ColoredText> list = new ArrayList<>();
        
        String text = String.format(Locale.ROOT, "%.2f", val);
        
        int dotIndex = text.indexOf('.');
        
        if (dotIndex != -1) {
            String intPart = text.substring(0, dotIndex);
            list.add(new GDTextRenderer.ColoredText(" " + intPart, GuiConstants.COLOR_WHITE));             
            list.add(new GDTextRenderer.ColoredText(".", GuiConstants.COLOR_WHITE));
            
            String decPart = text.substring(dotIndex + 1);
            list.add(new GDTextRenderer.ColoredText(decPart, GuiConstants.COLOR_GRAY));
        } else {
            list.add(new GDTextRenderer.ColoredText(" " + text, GuiConstants.COLOR_WHITE));
        }

        return list;
    }
}
