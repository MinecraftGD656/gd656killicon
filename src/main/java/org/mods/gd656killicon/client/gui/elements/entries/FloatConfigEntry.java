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

        // 1. 文本显示区域 (自适应)
        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        // 2. 主要控制区 (60px)
        this.addColoredColumn(parseColoredText(this.value), 60, false, false, (btn) -> {
            if (this.textInputDialog != null) {
                // Initial text: format as "%.2f" to show 2 decimal places
                String initialText = String.format(Locale.ROOT, "%.2f", this.value);
                this.textInputDialog.show(initialText, this.configName, (newValue) -> {
                    try {
                        float f = Float.parseFloat(newValue);
                        // Round to 2 decimal places if needed? User input validator already handles it.
                        // But let's ensure consistency.
                        // Actually, user said "precision up to 0.01", so standard float is fine.
                        this.value = f;
                        updateState();
                        if (this.onValueChange != null) {
                            this.onValueChange.accept(this.value);
                        }
                    } catch (NumberFormatException ignored) {
                        // Should be prevented by validator
                    }
                }, this::isValidFloat);
            }
        });

        // 3. 重置按钮
        this.addColumn("R", GuiConstants.ROW_HEADER_HEIGHT, getResetButtonColor(), true, true, (btn) -> {
            if (Math.abs(this.value - this.defaultValue) < 0.0001f) return;

            this.value = this.defaultValue;
            updateState();
            if (this.onValueChange != null) {
                this.onValueChange.accept(this.value);
            }
        });
    }
    
    // Validator: Only allow numbers, optional negative sign, optional decimal point.
    // Max 2 decimal places.
    // Regex: ^-?\d*(\.\d{0,2})?$
    // Also handles empty string or just "-" as intermediate valid states during typing,
    // but on confirm we need a valid number. 
    // Wait, validator in TextInputDialog is used for `renderInputText` color (red/white) and confirm button active state.
    // So we should allow intermediate states? 
    // TextInputDialog logic: confirmButton.active = isValid.
    // So if I type "-", it's not a valid float yet, so button disabled. Good.
    // Regex for valid float with max 2 decimals: ^-?\d+(\.\d{1,2})?$
    // But we need to allow typing "12." which is technically valid float "12.0".
    public String getKey() {
        return key;
    }

    private boolean isValidFloat(String text) {
        if (text == null || text.isEmpty()) return false;
        
        // Handle negative sign
        if (text.equals("-")) return false; // Intermediate state, not valid float yet

        // Allow ".5" or "0.5" or "-.5"
        // Regex: Optional minus, Optional digits, Dot, 0-2 digits
        // OR: Optional minus, One or more digits, Optional (Dot, 0-2 digits)
        
        // Let's break it down:
        // Case 1: Start with dot -> ^-?\.\d{1,2}$ (e.g. .5, -.55)
        // Case 2: Start with digit -> ^-?\d+(\.\d{0,2})?$ (e.g. 5, 5., 5.5, -5.55)
        
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
            resetCol.text = "R";
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
        
        // Format to 2 decimal places always
        String text = String.format(Locale.ROOT, "%.2f", val);
        
        // Find dot
        int dotIndex = text.indexOf('.');
        
        if (dotIndex != -1) {
            // Integer part: White
            String intPart = text.substring(0, dotIndex);
            list.add(new GDTextRenderer.ColoredText(" " + intPart, GuiConstants.COLOR_WHITE)); // Add space padding like StringConfigEntry
            
            // Dot: White
            list.add(new GDTextRenderer.ColoredText(".", GuiConstants.COLOR_WHITE));
            
            // Decimal part: Gray
            String decPart = text.substring(dotIndex + 1);
            list.add(new GDTextRenderer.ColoredText(decPart, GuiConstants.COLOR_GRAY));
        } else {
            // Should not happen with %.2f, but fallback
            list.add(new GDTextRenderer.ColoredText(" " + text, GuiConstants.COLOR_WHITE));
        }

        return list;
    }
}
