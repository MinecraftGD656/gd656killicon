package org.mods.gd656killicon.client.gui.elements;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.mods.gd656killicon.client.gui.GuiConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 严格按照需求重构的单行表单渲染器。
 * 1. 颜色计算：锁定 Alpha，仅修改 RGB 使其变深。
 * 2. 金色条动画：非线性拉伸，从左侧起始覆盖底部。
 * 3. 布局：严格 1 像素间隙。
 */
public class GDRowRenderer {
    private int x1, y1, x2, y2;
    private int bgColor;
    private float bgAlpha;
    private final boolean isHeader;
    private final List<Column> columns = new ArrayList<>();
    private int currentColumnIndex = 0;
    private long lastTime = System.currentTimeMillis();

    private boolean separateFirstColumn = false;
    private String hoverTitle;
    private String hoverDescription;
    private Consumer<Boolean> onHover;

    // Clipping bounds (screen coordinates)
    private Integer clipX1, clipY1, clipX2, clipY2;
    private Supplier<Boolean> activeCondition = () -> true;

    public void setActiveCondition(Supplier<Boolean> activeCondition) {
        this.activeCondition = activeCondition;
    }

    public Supplier<Boolean> getActiveCondition() {
        return this.activeCondition;
    }

    public interface CellRenderer {
        void render(GuiGraphics guiGraphics, int x, int y, int width, int height);
    }

    public static class Column {
        public String text;
        public List<GDTextRenderer.ColoredText> coloredTexts;
        public ItemStack icon;
        public int width;
        public int color;
        public boolean isDarker;
        public boolean isCentered;
        public GDTextRenderer textRenderer;
        public Consumer<Integer> onClick;
        public float hoverProgress = 0.0f; // 0.0 -> 1.0
        public List<Column> hoverReplacementColumns;
        public CellRenderer customRenderer;

        public Column() {}
    }

    public GDRowRenderer(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, boolean isHeader) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.bgColor = bgColor;
        this.bgAlpha = bgAlpha;
        this.isHeader = isHeader;
    }

    public void setX1(int x1) { this.x1 = x1; }
    public void setY1(int y1) { this.y1 = y1; }
    public void setX2(int x2) { this.x2 = x2; }
    public void setY2(int y2) { this.y2 = y2; }

    public void setBackgroundColor(int bgColor) {
        this.bgColor = bgColor;
    }

    public void setBackgroundAlpha(float bgAlpha) {
        this.bgAlpha = bgAlpha;
    }

    public void addColumn(String text, int width, int color, boolean isDarker, boolean isCentered, Consumer<Integer> onClick) {
        Column col = getOrCreateColumn(currentColumnIndex++);
        col.text = text;
        col.coloredTexts = null;
        col.icon = null;
        col.width = width;
        col.color = color;
        col.isDarker = isDarker;
        col.isCentered = isCentered;
        col.onClick = onClick;
        
        // 如果 textRenderer 已经存在，强制更新其颜色以防缓存
        if (col.textRenderer != null) {
            col.textRenderer.setColor(color);
        }
    }

    public void addColumn(String text, int width, int color, boolean isDarker, boolean isCentered) {
        addColumn(text, width, color, isDarker, isCentered, null);
    }

    public void addNameColumn(String name, String id, int nameColor, int idColor, boolean isDarker, boolean isCentered) {
        List<GDTextRenderer.ColoredText> parts = new ArrayList<>();
        String resolvedName = name == null ? "" : name;
        parts.add(new GDTextRenderer.ColoredText(" " + resolvedName + (id == null || id.isEmpty() ? "" : " "), nameColor));
        if (id != null && !id.isEmpty()) {
            parts.add(new GDTextRenderer.ColoredText(id, idColor));
        }
        addColoredColumn(parts, -1, isDarker, isCentered);
    }

    public void addColoredColumn(List<GDTextRenderer.ColoredText> coloredTexts, int width, boolean isDarker, boolean isCentered, Consumer<Integer> onClick) {
        Column col = getOrCreateColumn(currentColumnIndex++);
        col.coloredTexts = coloredTexts;
        col.text = null;
        col.icon = null;
        col.width = width;
        col.isDarker = isDarker;
        col.isCentered = isCentered;
        col.onClick = onClick;
    }

    public String getSortKey() {
        if (!columns.isEmpty()) {
            String text = columns.get(0).text;
            if (text != null) {
                return text.trim();
            }
            List<GDTextRenderer.ColoredText> colored = columns.get(0).coloredTexts;
            if (colored != null && !colored.isEmpty()) {
                StringBuilder combined = new StringBuilder();
                for (GDTextRenderer.ColoredText part : colored) {
                    if (part != null && part.text != null) {
                        combined.append(part.text);
                    }
                }
                return combined.toString().trim();
            }
        }
        return "";
    }

    public void addColoredColumn(List<GDTextRenderer.ColoredText> coloredTexts, int width, boolean isDarker, boolean isCentered) {
        addColoredColumn(coloredTexts, width, isDarker, isCentered, null);
    }

    public void addIconColumn(ItemStack icon, int width, boolean isDarker, boolean isCentered, Consumer<Integer> onClick) {
        Column col = getOrCreateColumn(currentColumnIndex++);
        col.icon = icon;
        col.text = null;
        col.coloredTexts = null;
        col.width = width;
        col.isDarker = isDarker;
        col.isCentered = isCentered;
        col.onClick = onClick;
    }

    public void addCustomColumn(int width, Consumer<Integer> onClick, CellRenderer renderer) {
        Column col = getOrCreateColumn(currentColumnIndex++);
        col.width = width;
        col.onClick = onClick;
        col.customRenderer = renderer;
        col.text = null;
        col.coloredTexts = null;
        col.icon = null;
    }

    private Column getOrCreateColumn(int index) {
        while (columns.size() <= index) {
            columns.add(new Column());
        }
        return columns.get(index);
    }

    public void resetColumnConfig() {
        currentColumnIndex = 0;
    }

    public void setHoverInfo(String title, String description) {
        this.hoverTitle = title;
        this.hoverDescription = description;
    }

    public String getHoverTitle() {
        return hoverTitle;
    }

    public String getHoverDescription() {
        return hoverDescription;
    }
    
    public void setOnHover(Consumer<Boolean> onHover) {
        this.onHover = onHover;
    }

    public void setSeparateFirstColumn(boolean separate) {
        this.separateFirstColumn = separate;
    }

    public void setBounds(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public Column getColumn(int index) {
        if (index >= 0 && index < columns.size()) {
            return columns.get(index);
        }
        return null;
    }

    public void setColumnHoverReplacement(int index, List<Column> replacementColumns) {
        Column col = getColumn(index);
        if (col != null) {
            col.hoverReplacementColumns = replacementColumns;
        }
    }

    public void setClip(int x1, int y1, int x2, int y2) {
        this.clipX1 = x1;
        this.clipY1 = y1;
        this.clipX2 = x2;
        this.clipY2 = y2;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (onHover != null) {
            onHover.accept(isHovered(mouseX, mouseY));
        }
        
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000.0f;
        lastTime = now;

        int rowHeight = y2 - y1;
        int totalWidth = x2 - x1;

        // 1. 计算列宽 (移动到背景填充之前)
        int fixedW = 0; int flexC = 0;
        for (int i = 0; i < currentColumnIndex; i++) {
            if (columns.get(i).width == -1) flexC++;
            else fixedW += columns.get(i).width;
        }
        int flexW = flexC > 0 ? Math.max(0, (totalWidth - fixedW) / flexC) : 0;

        // 2. 基础颜色计算 (锁定 Alpha)
        int alphaBits = (int)(bgAlpha * 255) << 24;
        int baseR = (bgColor >> 16) & 0xFF;
        int baseG = (bgColor >> 8) & 0xFF;
        int baseB = bgColor & 0xFF;

        // 3. 基础背景填充 (考虑 1 像素间隙)
        // 兼容旧逻辑：如果第一列宽度等于行高，默认开启间隔 (Rank方块)
        boolean doGap = (!isHeader && currentColumnIndex > 1 && columns.get(0).width == rowHeight) || separateFirstColumn;
        
        if (doGap && currentColumnIndex > 0) {
            int col0W = (columns.get(0).width == -1) ? flexW : columns.get(0).width;
            guiGraphics.fill(x1, y1, x1 + col0W, y2, alphaBits | (baseR << 16) | (baseG << 8) | baseB);
            guiGraphics.fill(x1 + col0W + 1, y1, x2, y2, alphaBits | (baseR << 16) | (baseG << 8) | baseB);
        } else {
            guiGraphics.fill(x1, y1, x2, y2, alphaBits | (baseR << 16) | (baseG << 8) | baseB);
        }

        // 4. 渲染每一列
        int currentX = x1;
        for (int i = 0; i < currentColumnIndex; i++) {
            Column col = columns.get(i);
            int colW = (col.width == -1) ? flexW : col.width;
            if (colW <= 0) continue;

            // 动画更新
            boolean isHovered = mouseX >= currentX && mouseX < currentX + colW && mouseY >= y1 && mouseY < y2;
            
            // 如果悬停且有替换列，渲染替换列
            if (isHovered && col.hoverReplacementColumns != null && !col.hoverReplacementColumns.isEmpty()) {
                renderReplacementColumns(guiGraphics, col.hoverReplacementColumns, currentX, y1, colW, rowHeight, mouseX, mouseY, partialTick, dt);
                currentX += colW;
                continue;
            }

            float animSpeed = 8.0f;
            if (isHovered) col.hoverProgress = Math.min(1.0f, col.hoverProgress + dt * animSpeed);
            else col.hoverProgress = Math.max(0.0f, col.hoverProgress - dt * animSpeed);

            // Scissor 补偿 (使用 m31() 方法获取 y 轴偏移量，并使用 Math.round 解决滚动抖动)
            float translateX = guiGraphics.pose().last().pose().m30();
            float translateY = guiGraphics.pose().last().pose().m31();
            
            int sX1 = (int)Math.round(Math.max(x1, currentX) + translateX);
            int sX2 = (int)Math.round(Math.min(x2, currentX + colW) + translateX);
            int sY1 = (int)Math.round(y1 + translateY);
            int sY2 = (int)Math.round(y2 + translateY);

            // Apply custom clipping if set
            if (clipX1 != null) sX1 = Math.max(sX1, clipX1);
            if (clipY1 != null) sY1 = Math.max(sY1, clipY1);
            if (clipX2 != null) sX2 = Math.min(sX2, clipX2);
            if (clipY2 != null) sY2 = Math.min(sY2, clipY2);

            if (sX2 > sX1 && sY2 > sY1) {
                guiGraphics.enableScissor(sX1, sY1, sX2, sY2);

                // 计算实际绘制区域 (考虑排名方块间隙)
                int drawX = currentX;
                int drawW = colW;
                if (doGap && i == 1) {
                    drawX += 1;
                    drawW -= 1;
                }

                // 填充变深背景 (仅当是深色列或有悬停进度时)
                if (col.isDarker || col.hoverProgress > 0) {
                    float darken = 0.0f;
                    if (col.isDarker) darken += 0.25f; // 深色列基础加深 25%
                    
                    // 悬停加深效果：对于可点击列或表头，悬停时加深
                    if (col.hoverProgress > 0 && (isHeader || col.onClick != null)) {
                         darken += col.hoverProgress * 0.2f; // 悬停额外加深 20%
                    }

                    int overlayAlpha = (int)(255 * darken);
                    int overlayColor = (overlayAlpha << 24) | 0x000000; // 纯黑叠加层
                    // 关键修复：使用 guiGraphics.fill 的变体，或者确保坐标计算与基础背景一致
                    guiGraphics.fill(drawX, y1, drawX + drawW, y2, overlayColor);
                }

                // --- 金色条动画 (表头或可点击列) ---
                if ((isHeader || col.onClick != null) && col.hoverProgress > 0.001f) {
                    float t = col.hoverProgress;
                    float ease = 1.0f - (float) Math.pow(1.0f - t, 3); // Ease-out cubic
                    float barWidth = drawW * ease;

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(drawX, y2 - 1.0f, 0);
                    guiGraphics.pose().scale(barWidth, 1.0f, 1.0f);
                    guiGraphics.fill(0, 0, 1, 1, GuiConstants.COLOR_GOLD);
                    guiGraphics.pose().popPose();
                }

                // --- 内容渲染 ---
                renderContent(guiGraphics, col, drawX, drawW, rowHeight, partialTick, dt, i);

                guiGraphics.disableScissor();
            }
            currentX += colW;
        }
    }

    private void renderReplacementColumns(GuiGraphics guiGraphics, List<Column> subColumns, int x, int y, int w, int h, int mouseX, int mouseY, float pt, float dt) {
        int count = subColumns.size();
        int subW = w / count;
        int currentX = x;

        for (int i = 0; i < count; i++) {
            Column subCol = subColumns.get(i);
            // 最后一列填补剩余宽度
            int thisW = (i == count - 1) ? (x + w - currentX) : subW;
            
            // 渲染子列背景 (加深)
            boolean subHovered = mouseX >= currentX && mouseX < currentX + thisW && mouseY >= y && mouseY < y + h;
            if (subHovered) {
                subCol.hoverProgress = Math.min(1.0f, subCol.hoverProgress + dt * 8.0f);
            } else {
                subCol.hoverProgress = Math.max(0.0f, subCol.hoverProgress - dt * 8.0f);
            }

            // Scissor 补偿 (与主渲染逻辑一致)
            float translateX = guiGraphics.pose().last().pose().m30();
            float translateY = guiGraphics.pose().last().pose().m31();
            
            int sX1 = (int)Math.round(Math.max(x1, currentX) + translateX);
            int sX2 = (int)Math.round(Math.min(x2, currentX + thisW) + translateX);
            int sY1 = (int)Math.round(y1 + translateY);
            int sY2 = (int)Math.round(y2 + translateY);

            if (clipX1 != null) sX1 = Math.max(sX1, clipX1);
            if (clipY1 != null) sY1 = Math.max(sY1, clipY1);
            if (clipX2 != null) sX2 = Math.min(sX2, clipX2);
            if (clipY2 != null) sY2 = Math.min(sY2, clipY2);

            if (sX2 > sX1 && sY2 > sY1) {
                guiGraphics.enableScissor(sX1, sY1, sX2, sY2);
                
                // 绘制背景 (稍微加深以区分)
                int alphaBits = (int)(bgAlpha * 255) << 24;
                int bgCol = (bgColor & 0xFFFFFF) | alphaBits;
                guiGraphics.fill(currentX, y, currentX + thisW, y + h, bgCol);

                // 悬停加深
                if (subHovered || subCol.isDarker) {
                    float darken = subCol.isDarker ? 0.25f : 0.0f;
                    if (subHovered) darken += 0.2f;
                    int overlayAlpha = (int)(255 * darken);
                    guiGraphics.fill(currentX, y, currentX + thisW, y + h, (overlayAlpha << 24));
                }
                
                // 渲染内容
                renderContent(guiGraphics, subCol, currentX, thisW, h, pt, dt, -1);
                
                // --- 金色条动画 (表头或可点击列) ---
                if (subCol.onClick != null && subCol.hoverProgress > 0.001f) {
                    float t = subCol.hoverProgress;
                    float ease = 1.0f - (float) Math.pow(1.0f - t, 3); // Ease-out cubic
                    float barWidth = thisW * ease;

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(currentX, y + h - 1.0f, 0);
                    guiGraphics.pose().scale(barWidth, 1.0f, 1.0f);
                    guiGraphics.fill(0, 0, 1, 1, GuiConstants.COLOR_GOLD);
                    guiGraphics.pose().popPose();
                }

                guiGraphics.disableScissor();
            }
            
            // 绘制分割线 (如果不是最后一列)
            if (i < count - 1) {
                guiGraphics.fill(currentX + thisW - 1, y, currentX + thisW, y + h, 0x40000000);
            }

            currentX += thisW;
        }
    }

    protected void renderContent(GuiGraphics guiGraphics, Column col, int x, int w, int h, float pt, float dt, int colIndex) {
        if (col.customRenderer != null) {
            col.customRenderer.render(guiGraphics, x, y1, w, h);
            return;
        }

        if (col.icon != null) {
            float scale = h / 16.0f * 0.8f;
            int size = (int)(16 * scale);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + (w - size) / 2.0, y1 + (h - size) / 2.0, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            try { guiGraphics.renderItem(col.icon, 0, 0); } 
            catch (Exception e) { guiGraphics.renderItem(Items.BARRIER.getDefaultInstance(), 0, 0); }
            guiGraphics.pose().popPose();
        } else {
            int textY = y1 + (h - 9) / 2;
            int tx1 = col.isCentered ? x : x + 2;
            int tx2 = x + w;

            if (col.textRenderer == null) {
                if (col.coloredTexts != null) col.textRenderer = new GDTextRenderer(col.coloredTexts, tx1, textY, tx2, textY + 9, 1.0f, false);
                else col.textRenderer = new GDTextRenderer(col.text, tx1, textY, tx2, textY + 9, 1.0f, col.color, false);
            } else {
                col.textRenderer.setX1(tx1); col.textRenderer.setY1(textY); col.textRenderer.setX2(tx2); col.textRenderer.setY2(textY + 9);
                if (col.coloredTexts != null) col.textRenderer.setColoredTexts(col.coloredTexts);
                else col.textRenderer.setText(col.text);
            }
            col.textRenderer.setCentered(col.isCentered);
            
            // Check active condition
            boolean isActive = activeCondition.get();
            if (!isActive) {
                col.textRenderer.setOverrideColor(GuiConstants.COLOR_GRAY);
            } else {
                col.textRenderer.setOverrideColor(null);
            }
            
            col.textRenderer.renderInternal(guiGraphics, pt, false, dt);
        }
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return false;
        if (!activeCondition.get()) return false;
        
        int currentX = x1;
        int totalW = x2 - x1;
        int fixedW = 0; int flexC = 0;
        for (int i = 0; i < currentColumnIndex; i++) {
            if (columns.get(i).width == -1) flexC++; else fixedW += columns.get(i).width;
        }
        int flexW = flexC > 0 ? (totalW - fixedW) / flexC : 0;

        for (int i = 0; i < currentColumnIndex; i++) {
            int colW = (columns.get(i).width == -1) ? flexW : columns.get(i).width;
            if (mouseX >= currentX && mouseX < currentX + colW) {
                Column col = columns.get(i);
                
                // 检查是否有替换列
                if (col.hoverReplacementColumns != null && !col.hoverReplacementColumns.isEmpty()) {
                    int subCount = col.hoverReplacementColumns.size();
                    int subW = colW / subCount;
                    int subX = currentX;
                    for (int j = 0; j < subCount; j++) {
                        int thisW = (j == subCount - 1) ? (currentX + colW - subX) : subW;
                        if (mouseX >= subX && mouseX < subX + thisW) {
                            if (col.hoverReplacementColumns.get(j).onClick != null) {
                                col.hoverReplacementColumns.get(j).onClick.accept(button);
                                return true;
                            }
                            return false;
                        }
                        subX += thisW;
                    }
                }
                
                if (col.onClick != null) {
                    col.onClick.accept(button);
                    return true; // 触发了回调，返回 true 表示捕获了事件
                }
                // 如果没有回调，返回 false 允许外部处理（如拖拽）
                return false;
            }
            currentX += colW;
        }
        return false;
    }
}
