package org.mods.gd656killicon.client.gui.elements.entries;

import net.minecraft.client.gui.GuiGraphics;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.ChoiceListDialog;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FixedChoiceConfigEntry extends GDRowRenderer {
    public static class Choice {
        private final String value;
        private final String label;

        public Choice(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String value() {
            return value;
        }

        public String label() {
            return label;
        }
    }

    private final List<Choice> choices;
    private int index;
    private final String defaultValue;
    private final Consumer<String> onValueChange;
    private final String key;
    private final ChoiceListDialog choiceListDialog;
    private final String configName;

    public FixedChoiceConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, String initialValue, String defaultValue, List<Choice> choices, Consumer<String> onValueChange, ChoiceListDialog choiceListDialog) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, choices, onValueChange, choiceListDialog, () -> true);
    }

    public FixedChoiceConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, String initialValue, String defaultValue, List<Choice> choices, Consumer<String> onValueChange, ChoiceListDialog choiceListDialog, Supplier<Boolean> activeCondition) {
        super(x1, y1, x2, y2, bgColor, bgAlpha, false);
        this.key = configId;
        this.setActiveCondition(activeCondition);
        this.setSeparateFirstColumn(true);
        this.setHoverInfo(configName, "   " + description);
        this.choices = choices;
        this.defaultValue = defaultValue;
        this.onValueChange = onValueChange;
        this.choiceListDialog = choiceListDialog;
        this.configName = configName;
        this.index = resolveIndex(initialValue, defaultValue);

        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        this.addColumn(getValueText(), 60, GuiConstants.COLOR_GOLD, false, true, (btn) -> {
            openDialog();
        });

        this.addColumn("↺", GuiConstants.ROW_HEADER_HEIGHT, getResetButtonColor(), true, true, (btn) -> {
            if (getCurrentValue().equals(this.defaultValue)) return;
            this.index = resolveIndex(this.defaultValue, this.defaultValue);
            updateState();
            if (this.onValueChange != null) {
                this.onValueChange.accept(getCurrentValue());
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

    private int resolveIndex(String value, String fallback) {
        if (choices != null) {
            for (int i = 0; i < choices.size(); i++) {
                if (choices.get(i).value().equals(value)) {
                    return i;
                }
            }
            for (int i = 0; i < choices.size(); i++) {
                if (choices.get(i).value().equals(fallback)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private String getCurrentValue() {
        return choices.get(index).value();
    }

    private String getValueText() {
        return choices.get(index).label();
    }

    private void updateState() {
        Column controlCol = getColumn(1);
        if (controlCol != null) {
            controlCol.text = getValueText();
            controlCol.color = GuiConstants.COLOR_GOLD;
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

    private void openDialog() {
        if (choiceListDialog != null) {
            choiceListDialog.show(getCurrentValue(), this.configName, this.choices, (newValue) -> {
                if (newValue != null && newValue.equals(getCurrentValue())) {
                    return;
                }
                this.index = resolveIndex(newValue, this.defaultValue);
                updateState();
                if (this.onValueChange != null) {
                    this.onValueChange.accept(getCurrentValue());
                }
            }, null);
        }
    }

    private int getResetButtonColor() {
        return getCurrentValue().equals(this.defaultValue) ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
    }
}
