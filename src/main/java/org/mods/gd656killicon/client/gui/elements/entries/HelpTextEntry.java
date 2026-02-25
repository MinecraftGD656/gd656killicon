package org.mods.gd656killicon.client.gui.elements.entries;

import net.minecraft.client.gui.GuiGraphics;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * 帮助文本显示条目，用于显示标题和长文本描述。
 * 标题显示在左侧，描述显示在右侧（或下方，取决于实现）。
 * 为了保持与 Area 3 的风格一致，我们使用 GDRowRenderer 的列布局。
 * 左侧显示小标题（如加分项名称），右侧显示详细描述（自动换行）。
 * 这里的描述部分需要自定义渲染器来支持多行文本，因为标准 Column 只支持单行。
 */
public class HelpTextEntry extends GDRowRenderer {
    private final String title;
    private final String description;
    private GDTextRenderer descriptionRenderer;

    public HelpTextEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String title, String description) {
        super(x1, y1, x2, y2, bgColor, bgAlpha, false);
        this.title = title;
        this.description = description;

        // 1. 标题列 (自适应宽度或固定宽度)
        // 使用 addColumn 添加标题，白色，不加深
        this.addColumn(title, 120, GuiConstants.COLOR_GOLD, false, false, null);

        // 2. 描述列 (剩余空间)
        // 使用自定义渲染器来实现多行文本
        this.addCustomColumn(-1, null, (guiGraphics, x, y, width, height) -> {
            if (descriptionRenderer == null) {
                // 初始化多行文本渲染器，字体大小 1.0，白色，自动换行
                descriptionRenderer = new GDTextRenderer(description, x + 4, y + 4, x + width - 4, y + height - 4, 1.0f, GuiConstants.COLOR_WHITE, true);
            } else {
                descriptionRenderer.setX1(x + 4);
                descriptionRenderer.setY1(y + 4);
                descriptionRenderer.setX2(x + width - 4);
                descriptionRenderer.setY2(y + height - 4);
            }
            descriptionRenderer.render(guiGraphics, 0, false); // partialTick 传 0 即可，禁用内部 Scissor 以兼容外部滚动
        });
    }

    public HelpTextEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, List<GDTextRenderer.ColoredText> titleParts, String description) {
        super(x1, y1, x2, y2, bgColor, bgAlpha, false);
        this.title = titleParts == null ? "" : titleParts.stream().map(part -> part.text).reduce("", String::concat);
        this.description = description;

        this.addColoredColumn(titleParts, 120, false, false);

        this.addCustomColumn(-1, null, (guiGraphics, x, y, width, height) -> {
            if (descriptionRenderer == null) {
                descriptionRenderer = new GDTextRenderer(description, x + 4, y + 4, x + width - 4, y + height - 4, 1.0f, GuiConstants.COLOR_WHITE, true);
            } else {
                descriptionRenderer.setX1(x + 4);
                descriptionRenderer.setY1(y + 4);
                descriptionRenderer.setX2(x + width - 4);
                descriptionRenderer.setY2(y + height - 4);
            }
            descriptionRenderer.render(guiGraphics, 0, false);
        });
    }

    /**
     * 计算该条目需要的总高度。
     * 基于描述文本的长度和宽度进行计算。
     */
    public int getRequiredHeight(int width) {
        // 假设第一列宽度 120，第二列宽度 width - 120
        int descWidth = width - 120 - 8; // 减去 padding
        if (descWidth <= 0) return GuiConstants.ROW_HEADER_HEIGHT;

        if (descriptionRenderer == null) {
            // 临时创建一个来计算高度
            descriptionRenderer = new GDTextRenderer(description, 0, 0, descWidth, 100, 1.0f, GuiConstants.COLOR_WHITE, true);
        } else {
            // 更新宽度以便重新计算
            descriptionRenderer.setX1(0);
            descriptionRenderer.setX2(descWidth);
        }
        
        int textHeight = descriptionRenderer.getFinalHeight();
        return Math.max(GuiConstants.ROW_HEADER_HEIGHT, textHeight + 8); // 最小高度为标准行高，加上 padding
    }
}
