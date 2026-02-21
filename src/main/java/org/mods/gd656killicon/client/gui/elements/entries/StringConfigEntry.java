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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本类型配置行渲染器
 */
public class StringConfigEntry extends GDRowRenderer {
    private String value;
    private final String defaultValue;
    private final Consumer<String> onValueChange;
    private final String key;

    private final TextInputDialog textInputDialog;
    private final String configName;

    public StringConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, String initialValue, String defaultValue, Consumer<String> onValueChange, TextInputDialog textInputDialog) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, () -> true);
    }

    public StringConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, String initialValue, String defaultValue, Consumer<String> onValueChange, TextInputDialog textInputDialog, Supplier<Boolean> activeCondition) {
        super(x1, y1, x2, y2, bgColor, bgAlpha, false);
        this.key = configId;
        this.setActiveCondition(activeCondition);
        this.setSeparateFirstColumn(true); // 第一列与后续列之间增加1像素间隔
        this.setHoverInfo(configName, "   " + description); // 设置悬停显示信息，简介前加两个空格
        this.value = initialValue;
        this.defaultValue = defaultValue;
        this.onValueChange = onValueChange;
        this.textInputDialog = textInputDialog;
        this.configName = configName;

        // 1. 文本显示区域 (自适应)
        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        // 2. 主要控制区 (120px)
        // 使用 ColoredText 实现高亮，文本前加空格
        // 此列为浅色列 (isDarker = false)，文本左对齐 (isCentered = false)
        this.addColoredColumn(parseColoredText(this.value), 120, false, false, (btn) -> {
            if (this.textInputDialog != null) {
                this.textInputDialog.show(this.value, this.configName, (newValue) -> {
                    this.value = newValue;
                    updateState();
                    if (this.onValueChange != null) {
                        this.onValueChange.accept(this.value);
                    }
                });
            }
        });

        // 3. 重置按钮 (GuiConstants.ROW_HEADER_HEIGHT, 深色)
        this.addColumn("R", GuiConstants.ROW_HEADER_HEIGHT, getResetButtonColor(), true, true, (btn) -> {
            if (this.value != null && this.value.equals(this.defaultValue)) return; // Already default, do nothing

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
            controlCol.coloredTexts = parseColoredText(this.value);
            // 强制刷新 textRenderer (设为 null 以便下次渲染时重建)
            controlCol.textRenderer = null;
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

    private int getResetButtonColor() {
        return (value != null && value.equals(defaultValue)) ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
    }

    private List<GDTextRenderer.ColoredText> parseColoredText(String text) {
        List<GDTextRenderer.ColoredText> list = new ArrayList<>();
        // 文本前面需要多一个空格
        String processingText = " " + (text == null ? "" : text);

        Pattern pattern = Pattern.compile("<.*?>");
        Matcher matcher = pattern.matcher(processingText);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                list.add(new GDTextRenderer.ColoredText(processingText.substring(lastEnd, matcher.start()), GuiConstants.COLOR_WHITE));
            }
            // 识别到占位符即被"<>"包裹的部分仅包括"<>"在内为金色的文本
            list.add(new GDTextRenderer.ColoredText(matcher.group(), GuiConstants.COLOR_GOLD));
            lastEnd = matcher.end();
        }
        if (lastEnd < processingText.length()) {
            list.add(new GDTextRenderer.ColoredText(processingText.substring(lastEnd), GuiConstants.COLOR_WHITE));
        }

        return list;
    }
}
