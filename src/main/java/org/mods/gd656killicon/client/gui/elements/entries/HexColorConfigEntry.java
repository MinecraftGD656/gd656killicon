package org.mods.gd656killicon.client.gui.elements.entries;

import net.minecraft.client.gui.GuiGraphics;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.TextInputDialog;
import org.mods.gd656killicon.client.gui.elements.ColorPickerDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 十六进制颜色配置行渲染器
 */
public class HexColorConfigEntry extends GDRowRenderer {
    private String value;
    private final String defaultValue;
    private final Consumer<String> onValueChange;
    private final String key;

    private final TextInputDialog textInputDialog;
    private final ColorPickerDialog colorPickerDialog;
    private final String configName;
    
    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public HexColorConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, String initialValue, String defaultValue, Consumer<String> onValueChange, TextInputDialog textInputDialog, ColorPickerDialog colorPickerDialog) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, colorPickerDialog, () -> true);
    }

    public HexColorConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, String initialValue, String defaultValue, Consumer<String> onValueChange, TextInputDialog textInputDialog, ColorPickerDialog colorPickerDialog, Supplier<Boolean> activeCondition) {
        super(x1, y1, x2, y2, bgColor, bgAlpha, false);
        this.key = configId;
        this.setActiveCondition(activeCondition);
        this.setSeparateFirstColumn(true);
        this.setHoverInfo(configName, "   " + description);
        this.value = initialValue;
        this.defaultValue = defaultValue;
        this.onValueChange = onValueChange;
        this.textInputDialog = textInputDialog;
        this.colorPickerDialog = colorPickerDialog;
        this.configName = configName;

        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        this.addColoredColumn(parseColoredText(this.value), 47, false, false, (btn) -> {
            openDialog();
        });
        
        this.addCustomColumn(17, (btn) -> {
            openColorPicker();
        }, (guiGraphics, x, y, w, h) -> {
             int size = 13;
             int bx = x + (w - size) / 2;
             int by = y + (h - size) / 2;
             
             int color = 0xFFFFFFFF;              try {
                 if (this.value != null && this.value.startsWith("#") && this.value.length() == 7) {
                     int rgb = Integer.parseInt(this.value.substring(1), 16);
                     color = 0xFF000000 | rgb;                  }
             } catch (Exception e) {
             }
             
             guiGraphics.fill(bx, by, bx + size, by + size, color);
        });

        this.addColumn("↺", GuiConstants.ROW_HEADER_HEIGHT, getResetButtonColor(), true, true, (btn) -> {
             if (this.value != null && this.value.equals(this.defaultValue)) return;
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

    private void openDialog() {
        if (this.textInputDialog != null) {
            this.textInputDialog.show(this.value, this.configName, (newValue) -> {
                this.value = newValue;
                updateState();
                if (this.onValueChange != null) {
                    this.onValueChange.accept(this.value);
                }
            }, (input) -> {
                 if (input == null || input.length() != 7) return false;
                 return HEX_PATTERN.matcher(input).matches();
            });
        }
    }
    
    private void openColorPicker() {
        if (this.colorPickerDialog != null) {
            this.colorPickerDialog.show(this.value, this.configName, (newValue) -> {
                this.value = newValue;
                updateState();
                if (this.onValueChange != null) {
                    this.onValueChange.accept(this.value);
                }
            }, null);
        } else {
            openDialog();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateState() {
        Column controlCol = getColumn(1);
        if (controlCol != null) {
            controlCol.coloredTexts = parseColoredText(this.value);
            controlCol.textRenderer = null;
        }

        Column resetCol = getColumn(3);
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
        return (value != null && value.equals(defaultValue)) ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
    }

    private List<GDTextRenderer.ColoredText> parseColoredText(String text) {
        List<GDTextRenderer.ColoredText> list = new ArrayList<>();
        String processingText = " " + (text == null ? "" : text);
        list.add(new GDTextRenderer.ColoredText(processingText, GuiConstants.COLOR_GOLD));
        return list;
    }
}
