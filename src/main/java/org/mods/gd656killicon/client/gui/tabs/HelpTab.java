package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.entries.HelpTextEntry;
import org.mods.gd656killicon.common.BonusType;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.function.Consumer;

public class HelpTab extends ConfigTabContent {

    // --- Area 3 (左侧文本) ---
    private int area3X1, area3Y1, area3X2, area3Y2;
    private double scrollY3 = 0; // 保留滚动变量以防将来需要
    private double targetScrollY3 = 0;
    private GDTextRenderer area3TextRenderer;
    private boolean isDragging3 = false;
    private double lastMouseY3 = 0;

    // --- Area 2 (右侧内容) ---
    private int area2X1, area2Y1, area2X2, area2Y2;
    private final List<GDRowRenderer> contentRenderers = new ArrayList<>();
    // 存储每个分类在 contentRenderers 中的起始索引，用于快速跳转 (不再使用 Area 3 导航，但保留索引逻辑可能有用)
    private int[] categoryStartIndices = new int[4]; 
    // 展开状态 - 默认收拢
    private boolean isCommandExpanded = false;
    private boolean isBonusExpanded = false;
    private boolean isPresetExpanded = false;
    private boolean isUpdateLogExpanded = false;
    
    // 服务端指令子分类展开状态
    private boolean isCommandBonusExpanded = false;
    private boolean isCommandResetExpanded = false;
    private boolean isCommandConfigExpanded = false;
    private boolean isCommandStatisticsExpanded = false;
    private boolean isCommandDebugExpanded = false;

    // 更新日志子分类展开状态
    private boolean isVersion1201F_Expanded = false;

    // 预计算的行高缓存，用于滚动计算
    private final List<Integer> rowHeights = new ArrayList<>();

    // 滚动相关
    private boolean isDragging = false;
    private double lastMouseY = 0;
    private int lastContentWidth = 0;

    public HelpTab(Minecraft minecraft) {
        super(minecraft, "gd656killicon.client.gui.config.tab.help");
    }

    @Override
    public void onTabOpen() {
        super.onTabOpen();
        scrollY = 0;
        targetScrollY = 0;
        scrollY3 = 0;
        targetScrollY3 = 0;
        rebuildContent(); // 构建内容列表
    }

    private void rebuildContent() {
        contentRenderers.clear();
        rowHeights.clear();
        
        // 确保使用最新的布局参数
        int area1Right = (minecraft.getWindow().getGuiScaledWidth() - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int x1 = area1Right + GuiConstants.DEFAULT_PADDING;
        int x2 = minecraft.getWindow().getGuiScaledWidth() - GuiConstants.DEFAULT_PADDING;
        int contentWidth = x2 - x1;

        // 1. 服务端指令介绍
        addCategoryHeader(0, "gd656killicon.client.gui.help.category.commands", isCommandExpanded, (btn) -> {
            isCommandExpanded = !isCommandExpanded;
            rebuildContent();
        });
        categoryStartIndices[0] = 0; // 记录位置
        
        if (isCommandExpanded) {
            // 子分类：加分项指令
            addSubCategoryHeader("gd656killicon.client.gui.help.command.category.bonus", isCommandBonusExpanded, (btn) -> {
                isCommandBonusExpanded = !isCommandBonusExpanded;
                rebuildContent();
            });
            if (isCommandBonusExpanded) {
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.bonus.turnon.title", "gd656killicon.client.gui.help.command.bonus.turnon.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.bonus.turnoff.title", "gd656killicon.client.gui.help.command.bonus.turnoff.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.bonus.edit.title", "gd656killicon.client.gui.help.command.bonus.edit.desc", contentWidth);
            }

            // 子分类：重置指令
            addSubCategoryHeader("gd656killicon.client.gui.help.command.category.reset", isCommandResetExpanded, (btn) -> {
                isCommandResetExpanded = !isCommandResetExpanded;
                rebuildContent();
            });
            if (isCommandResetExpanded) {
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.reset.config.title", "gd656killicon.client.gui.help.command.reset.config.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.reset.bonus.title", "gd656killicon.client.gui.help.command.reset.bonus.desc", contentWidth);
            }

            // 子分类：配置指令
            addSubCategoryHeader("gd656killicon.client.gui.help.command.category.config", isCommandConfigExpanded, (btn) -> {
                isCommandConfigExpanded = !isCommandConfigExpanded;
                rebuildContent();
            });
            if (isCommandConfigExpanded) {
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.config.combowindow.title", "gd656killicon.client.gui.help.command.config.combowindow.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.config.scoremaxlimit.title", "gd656killicon.client.gui.help.command.config.scoremaxlimit.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.config.displayname.title", "gd656killicon.client.gui.help.command.config.displayname.desc", contentWidth);
            }

            // 子分类：统计指令
            addSubCategoryHeader("gd656killicon.client.gui.help.command.category.statistics", isCommandStatisticsExpanded, (btn) -> {
                isCommandStatisticsExpanded = !isCommandStatisticsExpanded;
                rebuildContent();
            });
            if (isCommandStatisticsExpanded) {
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.statistics.get.title", "gd656killicon.client.gui.help.command.statistics.get.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.statistics.list.title", "gd656killicon.client.gui.help.command.statistics.list.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.statistics.modify.title", "gd656killicon.client.gui.help.command.statistics.modify.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.statistics.reset.title", "gd656killicon.client.gui.help.command.statistics.reset.desc", contentWidth);
            }

            // 子分类：调试指令
            addSubCategoryHeader("gd656killicon.client.gui.help.command.category.debug", isCommandDebugExpanded, (btn) -> {
                isCommandDebugExpanded = !isCommandDebugExpanded;
                rebuildContent();
            });
            if (isCommandDebugExpanded) {
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.debug.scoreboard.title", "gd656killicon.client.gui.help.command.debug.scoreboard.desc", contentWidth);
            }
        }

        // 2. 加分项介绍
        int bonusIndex = contentRenderers.size();
        addCategoryHeader(1, "gd656killicon.client.gui.help.category.bonus", isBonusExpanded, (btn) -> {
            isBonusExpanded = !isBonusExpanded;
            rebuildContent();
        });
        categoryStartIndices[1] = bonusIndex;

        if (isBonusExpanded) {
            // 遍历所有 BonusType 并生成条目
            // BonusType 是 final class，包含 int 常量，不能使用 values()
            // 使用 getAllNames() 获取所有名称，并按 BonusType ID 排序
            List<String> bonusNames = new ArrayList<>(BonusType.getAllNames());
            bonusNames.sort(Comparator.comparingInt(BonusType::getTypeByName));
            
            for (String bonusName : bonusNames) {
                 // 使用 BonusType 的 name 作为 key 的一部分
                 // 例如: gd656killicon.bonus.HEADSHOT.name 和 gd656killicon.bonus.HEADSHOT.desc
                 String nameKey = "gd656killicon.bonus." + bonusName + ".name";
                 String descKey = "gd656killicon.bonus." + bonusName + ".desc";
                 // 如果 lang 文件中没有 desc，可以使用默认文本
                 if (!I18n.exists(descKey)) {
                     descKey = "gd656killicon.client.gui.help.bonus.default_desc"; 
                 }
                 int bonusType = BonusType.getTypeByName(bonusName);
                 addPrefixedBonusHelpEntry(bonusType, bonusName, nameKey, descKey, contentWidth);
            }
        }

        // 3. 预设系统操作
        int presetIndex = contentRenderers.size();
        addCategoryHeader(2, "gd656killicon.client.gui.help.category.preset", isPresetExpanded, (btn) -> {
            isPresetExpanded = !isPresetExpanded;
            rebuildContent();
        });
        categoryStartIndices[2] = presetIndex;

        if (isPresetExpanded) {
            addHelpEntry("gd656killicon.client.gui.help.preset.intro", "gd656killicon.client.gui.help.preset.intro.desc", contentWidth);
            addHelpEntry("gd656killicon.client.gui.help.preset.select", "gd656killicon.client.gui.help.preset.select.desc", contentWidth);
            addHelpEntry("gd656killicon.client.gui.help.preset.create", "gd656killicon.client.gui.help.preset.create.desc", contentWidth);
            addHelpEntry("gd656killicon.client.gui.help.preset.edit", "gd656killicon.client.gui.help.preset.edit.desc", contentWidth);
            addHelpEntry("gd656killicon.client.gui.help.preset.export", "gd656killicon.client.gui.help.preset.export.desc", contentWidth);
            addHelpEntry("gd656killicon.client.gui.help.preset.structure", "gd656killicon.client.gui.help.preset.structure.desc", contentWidth);
        }
        
        // 4. 近期更新日志
        int updateLogIndex = contentRenderers.size();
        addCategoryHeader(3, "gd656killicon.client.gui.help.category.update_log", isUpdateLogExpanded, (btn) -> {
            isUpdateLogExpanded = !isUpdateLogExpanded;
            rebuildContent();
        });
        categoryStartIndices[3] = updateLogIndex;

        if (isUpdateLogExpanded) {
            // 子分类：版本号 (硬编码)
            addSubCategoryHeader("GD656KilliconForge1.20.1", isVersion1201F_Expanded, (btn) -> {
                isVersion1201F_Expanded = !isVersion1201F_Expanded;
                rebuildContent();
            });
            
            if (isVersion1201F_Expanded) {
                // 硬编码的更新日志内容
                String logContent = "1. [Bonus] 新增锁定目标、坚守阵地、冲锋陷阵与火力压制加分项；\n2. [Fix] 修复锁定目标在长耗时击杀不触发的问题；\n3. [Config] 补齐加分项格式与本地化；\n4. [Bonus] 新增摧毁道具加分项（默认关闭）；\n5. [Adjust] 火力压制加分项调整为15分；\n6. [Fix] 修复TACZ子弹类型匹配错误；\n7. [Bonus] 新增Spotting联动与索敌/标记击杀/标记小队助攻加分项；\n8. [Fix] 修复BonusList击杀受害者名称显示未翻译键名的问题；\n9. [Config] 音效音量支持0~200数值（含增益）";
                
                addIndentedHelpEntry("1.1.0.006Alpha", logContent, contentWidth);
            }
        }
        
        // 初始化 Area 3 渲染器
        String area3Text = I18n.get("gd656killicon.client.gui.help.area3.desc");
        if (area3TextRenderer == null) {
            area3TextRenderer = new GDTextRenderer(area3Text, area3X1, area3Y1, area3X2, area3Y2, 1.0f, GuiConstants.COLOR_WHITE, true);
        } else {
            area3TextRenderer.setText(area3Text);
        }

        // 更新总内容高度
        this.totalContentHeight = 0;
        for (int h : rowHeights) {
            this.totalContentHeight += h + 1; // 1px gap
        }
    }

    private void addCategoryHeader(int index, String titleKey, boolean expanded, Consumer<Integer> onClick) {
        // 创建表头行
        GDRowRenderer header = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_GOLD, 0.75f, true);
        // 使用 I18n.get 确保显示翻译后的文本
        header.addColumn(I18n.get(titleKey), -1, GuiConstants.COLOR_WHITE, true, false, onClick);
        // 添加展开/折叠图标
        header.addColumn(expanded ? "▼" : "▶", 20, GuiConstants.COLOR_WHITE, true, true, onClick);
        
        contentRenderers.add(header);
        rowHeights.add(GuiConstants.ROW_HEADER_HEIGHT);
    }

    private void addSubCategoryHeader(String titleKey, boolean expanded, Consumer<Integer> onClick) {
        // 创建子分类表头行 (稍微缩进，颜色区分)
        // 使用透明背景
        GDRowRenderer header = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, true);
        
        // 缩进：通过在标题前加空格实现视觉缩进
        String title = "  " + I18n.get(titleKey);
        
        // 使用白色文字
        header.addColumn(title, -1, GuiConstants.COLOR_WHITE, true, false, onClick);
        // 添加展开/折叠图标
        header.addColumn(expanded ? "▼" : "▶", 20, GuiConstants.COLOR_GRAY, true, true, onClick);
        
        contentRenderers.add(header);
        rowHeights.add(GuiConstants.ROW_HEADER_HEIGHT);
    }

    private void addIndentedHelpEntry(String titleKey, String descKey, int contentWidth) {
        String title = I18n.exists(titleKey) ? I18n.get(titleKey) : titleKey;
        String desc = I18n.exists(descKey) ? I18n.get(descKey) : descKey;
        
        // Indent title with spaces
        title = "    " + title;
        
        HelpTextEntry entry = new HelpTextEntry(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, title, desc);
        int height = entry.getRequiredHeight(contentWidth);
        
        contentRenderers.add(entry);
        rowHeights.add(height);
    }

    private void addHelpEntry(String titleKey, String descKey, int contentWidth) {
        // 使用 I18n.get 确保显示翻译后的文本
        String title = I18n.exists(titleKey) ? I18n.get(titleKey) : titleKey;
        String desc = I18n.exists(descKey) ? I18n.get(descKey) : descKey;
        
        HelpTextEntry entry = new HelpTextEntry(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, title, desc);
        int height = entry.getRequiredHeight(contentWidth);
        
        contentRenderers.add(entry);
        rowHeights.add(height);
    }

    private void addPrefixedBonusHelpEntry(int bonusType, String bonusName, String titleKey, String descKey, int contentWidth) {
        String title = I18n.exists(titleKey) ? I18n.get(titleKey) : titleKey;
        String desc = I18n.exists(descKey) ? I18n.get(descKey) : descKey;
        List<GDTextRenderer.ColoredText> titleParts = new ArrayList<>();
        String prefix = "[" + bonusType + "] ";
        titleParts.add(new GDTextRenderer.ColoredText(prefix, GuiConstants.COLOR_GRAY));
        titleParts.add(new GDTextRenderer.ColoredText(bonusName, GuiConstants.COLOR_DARK_GRAY));
        titleParts.add(new GDTextRenderer.ColoredText(" " + title, GuiConstants.COLOR_GOLD));
        HelpTextEntry entry = new HelpTextEntry(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, titleParts, desc);
        int height = entry.getRequiredHeight(contentWidth);
        contentRenderers.add(entry);
        rowHeights.add(height);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        updateAreaCoordinates(screenWidth, screenHeight);
        
        // --- 渲染 Area 3 (导航) ---
        renderArea3(guiGraphics, mouseX, mouseY, partialTick);

        // --- 渲染 Area 2 (内容) ---
        renderArea2(guiGraphics, mouseX, mouseY, partialTick, screenHeight);
        
        // 滚动更新
        long now = System.nanoTime();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = (now - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = now;
        if (dt > 0.1f) dt = 0.1f;
        
        // 检查宽度变化并重新计算高度
        int currentContentWidth = area2X2 - area2X1;
        if (currentContentWidth != lastContentWidth && currentContentWidth > 0) {
            lastContentWidth = currentContentWidth;
            recalculateHeights(currentContentWidth);
        }

        // 处理拖拽逻辑 (在调用 updateScroll 之前)
        if (isDragging) {
            double diff = mouseY - lastMouseY;
            targetScrollY -= diff;
            lastMouseY = mouseY;
        }
        
        if (isDragging3) {
            double diff = mouseY - lastMouseY3;
            targetScrollY3 -= diff;
            lastMouseY3 = mouseY;
        }

        updateScroll(dt, screenHeight);
    }
    
    private long lastFrameTime = 0;

    private void updateAreaCoordinates(int screenWidth, int screenHeight) {
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        
        this.area2X1 = area1Right + GuiConstants.DEFAULT_PADDING;
        this.area2Y1 = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
        this.area2X2 = screenWidth - GuiConstants.DEFAULT_PADDING;
        this.area2Y2 = screenHeight - GuiConstants.DEFAULT_PADDING;

        this.area3X1 = GuiConstants.DEFAULT_PADDING;
        this.area3Y1 = this.area1Bottom + GuiConstants.DEFAULT_PADDING;
        this.area3X2 = area1Right;
        // Area 3 占满剩余高度 (减去 Area 4 高度和 padding)
        this.area3Y2 = screenHeight - GuiConstants.REGION_4_HEIGHT - 2 * GuiConstants.DEFAULT_PADDING;
        
        if (area3TextRenderer != null) {
            area3TextRenderer.setX1(area3X1);
            area3TextRenderer.setY1(area3Y1);
            area3TextRenderer.setX2(area3X2);
            area3TextRenderer.setY2(area3Y2);
        }
    }

    private void renderArea3(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (area3TextRenderer != null) {
            // 使用 GDTextRenderer 渲染，支持自动滚动
            area3TextRenderer.render(guiGraphics, partialTick);
        }
    }

    private void recalculateHeights(int contentWidth) {
        rowHeights.clear();
        totalContentHeight = 0;
        for (GDRowRenderer renderer : contentRenderers) {
            int height;
            if (renderer instanceof HelpTextEntry) {
                height = ((HelpTextEntry)renderer).getRequiredHeight(contentWidth);
            } else {
                height = GuiConstants.ROW_HEADER_HEIGHT;
            }
            rowHeights.add(height);
            totalContentHeight += height + 1;
        }
    }

    private void renderArea2(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenHeight) {
        int x1 = area2X1;
        int x2 = area2X2;
        
        guiGraphics.enableScissor(area2X1, area2Y1, area2X2, area2Y2);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -scrollY, 0);

        int currentY = area2Y1;
        
        // 仅当宽度变化时才需要重新计算高度，这里简化为每帧更新 bounds
        // 如果 contentRenderers 为空，尝试重建
        if (contentRenderers.isEmpty()) {
            rebuildContent();
        }

        for (int i = 0; i < contentRenderers.size(); i++) {
            GDRowRenderer renderer = contentRenderers.get(i);
            int height = rowHeights.get(i);
            
            renderer.setBounds(x1, currentY, x2, currentY + height);
            
            // 渲染
            // 计算屏幕可见性
            float actualTop = currentY - (float)scrollY;
            float actualBottom = currentY + height - (float)scrollY;
            
            if (actualBottom > area2Y1 && actualTop < area2Y2) {
                renderer.render(guiGraphics, mouseX, (int)(mouseY + scrollY), partialTick);
            }
            
            currentY += height + 1;
        }

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void expandAndScrollToCategory(int index) {
        // 此方法原用于 Area 3 点击跳转，现 Area 3 已改为纯文本显示，不再需要此逻辑。
        // 保留方法体为空或直接移除。为保持代码整洁，建议移除。
        // 但由于 contentRenderers 初始化时可能依赖 categoryStartIndices，
        // 且 scrollToCategory 可能被其他地方调用（目前没有），这里暂且保留空实现或简单的展开逻辑
        // 以防万一。
        
        if (index == 0) isCommandExpanded = true;
        else if (index == 1) isBonusExpanded = true;
        else if (index == 2) isPresetExpanded = true;
        else if (index == 3) isUpdateLogExpanded = true;
        
        rebuildContent();
        scrollToCategory(index);
    }

    private void scrollToCategory(int index) {
        if (index < 0 || index >= categoryStartIndices.length) return;
        
        int targetIndex = categoryStartIndices[index];
        if (targetIndex >= rowHeights.size()) return;

        // 计算目标 Y
        double targetY = 0;
        for (int i = 0; i < targetIndex; i++) {
            targetY += rowHeights.get(i) + 1;
        }
        
        this.targetScrollY = targetY;
        // 限制滚动范围
        double maxScroll = Math.max(0, totalContentHeight - (area2Y2 - area2Y1));
        this.targetScrollY = Math.max(0, Math.min(maxScroll, this.targetScrollY));
    }
    
    @Override
    protected void updateScroll(float dt, int screenHeight) {
        // Recalculate content height to be safe/sync with parent
        this.totalContentHeight = 0;
        for (int h : rowHeights) {
            this.totalContentHeight += h + 1; // 1px gap
        }

        // Area 2 滚动
        int viewHeight = area2Y2 - area2Y1;
        double maxScroll = Math.max(0, totalContentHeight - viewHeight);
        targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY));
        
        double diff = targetScrollY - scrollY;
        if (Math.abs(diff) < 0.1) scrollY = targetScrollY;
        else scrollY += diff * SCROLL_SMOOTHING * dt;
        
        // Area 3 滚动
        if (area3TextRenderer != null) {
            float maxScroll3 = area3TextRenderer.getMaxScrollY();
            targetScrollY3 = Math.max(0, Math.min(maxScroll3, targetScrollY3));
            
            double diff3 = targetScrollY3 - scrollY3;
            if (Math.abs(diff3) < 0.1) scrollY3 = targetScrollY3;
            else scrollY3 += diff3 * SCROLL_SMOOTHING * dt;
            
            area3TextRenderer.setScrollY((float)scrollY3);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= area2X1 && mouseX <= area2X2 && mouseY >= area2Y1 && mouseY <= area2Y2) {
            targetScrollY -= delta * GuiConstants.SCROLL_AMOUNT;
            return true;
        }
        
        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
            targetScrollY3 -= delta * GuiConstants.SCROLL_AMOUNT;
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Area 3 点击 (处理拖拽)
        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
             isDragging3 = true;
             lastMouseY3 = mouseY;
             return true;
        }
        
        // Area 2 点击
        if (mouseX >= area2X1 && mouseX <= area2X2 && mouseY >= area2Y1 && mouseY <= area2Y2) {
            double adjustedY = mouseY + scrollY;
            for (GDRowRenderer renderer : contentRenderers) {
                if (renderer.mouseClicked(mouseX, adjustedY, button)) return true;
            }

            // 如果没有点击到任何行内元素，则开始拖拽
            isDragging = true;
            lastMouseY = mouseY;
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        isDragging3 = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
