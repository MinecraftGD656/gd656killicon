package org.mods.gd656killicon.client.gui.elements.entries;

import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;

import java.util.function.Supplier;

public class ActionConfigEntry extends GDRowRenderer {
    private final String key;

    public ActionConfigEntry(
        int x1,
        int y1,
        int x2,
        int y2,
        int bgColor,
        float bgAlpha,
        String configName,
        String configId,
        String description,
        String actionLabel,
        int actionColor,
        Runnable onAction,
        Supplier<Boolean> activeCondition
    ) {
        super(x1, y1, x2, y2, bgColor, bgAlpha, false);
        this.key = configId;
        this.setActiveCondition(activeCondition);
        this.setSeparateFirstColumn(true);
        this.setHoverInfo(configName, "   " + description);
        this.addNameColumn(configName, "", GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);
        this.addColumn(actionLabel, 60, actionColor, true, true, (btn) -> {
            if (onAction != null) {
                onAction.run();
            }
        });
    }

    public String getKey() {
        return key;
    }
}
