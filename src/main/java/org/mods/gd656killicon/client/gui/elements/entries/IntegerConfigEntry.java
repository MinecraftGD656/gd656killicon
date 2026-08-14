package org.mods.gd656killicon.client.gui.elements.entries;

import net.minecraft.client.gui.GuiGraphics;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.TextInputDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntegerConfigEntry extends GDRowRenderer {
    private int value;
    private final int defaultValue;
    private final Consumer<Integer> onValueChange;
    private final TextInputDialog textInputDialog;
    private final String configName;
    private final String key;
    private final Predicate<String> validator;

    public IntegerConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, int initialValue, int defaultValue, Consumer<Integer> onValueChange, TextInputDialog textInputDialog) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, () -> true);
    }

    public IntegerConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, int initialValue, int defaultValue, Consumer<Integer> onValueChange, TextInputDialog textInputDialog, Supplier<Boolean> activeCondition) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, activeCondition, null);
    }

    public IntegerConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, int initialValue, int defaultValue, Consumer<Integer> onValueChange, TextInputDialog textInputDialog, Supplier<Boolean> activeCondition, Predicate<String> validator) {
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
        this.validator = validator == null ? this::isValidInteger : validator;

        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        this.addColoredColumn(parseColoredText(String.valueOf(this.value)), 60, false, false, (btn) -> {
            if (this.textInputDialog != null) {
                this.textInputDialog.show(String.valueOf(this.value), this.configName, (newValue) -> {
                    try {
                        if (this.validator != null && !this.validator.test(newValue)) {
                            return;
                        }
                        int parsed = Integer.parseInt(newValue);
                        this.value = parsed;
                        updateState();
                        if (this.onValueChange != null) {
                            this.onValueChange.accept(this.value);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }, this.validator);
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

    public void setValue(int value) {
        this.value = value;
        updateState();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private boolean isValidInteger(String text) {
        if (text == null || text.isEmpty()) return false;
        if (text.equals("-")) return false;
        return text.matches("^-?\\d+$");
    }

    private void updateState() {
        Column controlCol = getColumn(1);
        if (controlCol != null) {
            controlCol.coloredTexts = parseColoredText(String.valueOf(this.value));
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
        return this.value == this.defaultValue ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
    }

    private List<GDTextRenderer.ColoredText> parseColoredText(String text) {
        List<GDTextRenderer.ColoredText> list = new ArrayList<>();
        String processingText = " " + (text == null ? "" : text);

        Pattern pattern = Pattern.compile("<.*?>");
        Matcher matcher = pattern.matcher(processingText);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                list.add(new GDTextRenderer.ColoredText(processingText.substring(lastEnd, matcher.start()), GuiConstants.COLOR_WHITE));
            }
            list.add(new GDTextRenderer.ColoredText(matcher.group(), GuiConstants.COLOR_GOLD));
            lastEnd = matcher.end();
        }
        if (lastEnd < processingText.length()) {
            list.add(new GDTextRenderer.ColoredText(processingText.substring(lastEnd), GuiConstants.COLOR_WHITE));
        }

        return list;
    }
}
