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

    
    private int area3X1, area3Y1, area3X2, area3Y2;
    private double scrollY3 = 0; 
    private double targetScrollY3 = 0;
    private GDTextRenderer area3TextRenderer;
    private boolean isDragging3 = false;
    private double lastMouseY3 = 0;

    
    private int area2X1, area2Y1, area2X2, area2Y2;
    private final List<GDRowRenderer> contentRenderers = new ArrayList<>();
    private int[] categoryStartIndices = new int[3];
    
    private boolean isCommandExpanded = false;
    private boolean isBonusExpanded = false;
    private boolean isPresetExpanded = false;
    
    private boolean isCommandBonusExpanded = false;
    private boolean isCommandResetExpanded = false;
    private boolean isCommandConfigExpanded = false;
    private boolean isCommandStatisticsExpanded = false;
    private boolean isCommandDebugExpanded = false;

    
    private final List<Integer> rowHeights = new ArrayList<>();

    
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
        rebuildContent(); 
    }

    private void rebuildContent() {
        contentRenderers.clear();
        rowHeights.clear();
        
        
        int area1Right = (minecraft.getWindow().getGuiScaledWidth() - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int x1 = area1Right + GuiConstants.DEFAULT_PADDING;
        int x2 = minecraft.getWindow().getGuiScaledWidth() - GuiConstants.DEFAULT_PADDING;
        int contentWidth = x2 - x1;

        
        addCategoryHeader(0, "gd656killicon.client.gui.help.category.commands", isCommandExpanded, (btn) -> {
            isCommandExpanded = !isCommandExpanded;
            rebuildContent();
        });
        categoryStartIndices[0] = 0; 
        
        if (isCommandExpanded) {
            
            addSubCategoryHeader("gd656killicon.client.gui.help.command.category.bonus", isCommandBonusExpanded, (btn) -> {
                isCommandBonusExpanded = !isCommandBonusExpanded;
                rebuildContent();
            });
            if (isCommandBonusExpanded) {
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.bonus.turnon.title", "gd656killicon.client.gui.help.command.bonus.turnon.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.bonus.turnoff.title", "gd656killicon.client.gui.help.command.bonus.turnoff.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.bonus.edit.title", "gd656killicon.client.gui.help.command.bonus.edit.desc", contentWidth);
            }

            
            addSubCategoryHeader("gd656killicon.client.gui.help.command.category.reset", isCommandResetExpanded, (btn) -> {
                isCommandResetExpanded = !isCommandResetExpanded;
                rebuildContent();
            });
            if (isCommandResetExpanded) {
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.reset.config.title", "gd656killicon.client.gui.help.command.reset.config.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.reset.bonus.title", "gd656killicon.client.gui.help.command.reset.bonus.desc", contentWidth);
            }

            
            addSubCategoryHeader("gd656killicon.client.gui.help.command.category.config", isCommandConfigExpanded, (btn) -> {
                isCommandConfigExpanded = !isCommandConfigExpanded;
                rebuildContent();
            });
            if (isCommandConfigExpanded) {
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.config.combowindow.title", "gd656killicon.client.gui.help.command.config.combowindow.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.config.scoremaxlimit.title", "gd656killicon.client.gui.help.command.config.scoremaxlimit.desc", contentWidth);
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.config.displayname.title", "gd656killicon.client.gui.help.command.config.displayname.desc", contentWidth);
            }

            
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

            
            addSubCategoryHeader("gd656killicon.client.gui.help.command.category.debug", isCommandDebugExpanded, (btn) -> {
                isCommandDebugExpanded = !isCommandDebugExpanded;
                rebuildContent();
            });
            if (isCommandDebugExpanded) {
                addIndentedHelpEntry("gd656killicon.client.gui.help.command.debug.scoreboard.title", "gd656killicon.client.gui.help.command.debug.scoreboard.desc", contentWidth);
            }
        }

        
        int bonusIndex = contentRenderers.size();
        addCategoryHeader(1, "gd656killicon.client.gui.help.category.bonus", isBonusExpanded, (btn) -> {
            isBonusExpanded = !isBonusExpanded;
            rebuildContent();
        });
        categoryStartIndices[1] = bonusIndex;

        if (isBonusExpanded) {
            
            
            
            List<String> bonusNames = new ArrayList<>(BonusType.getAllNames());
            bonusNames.sort(Comparator.comparingInt(BonusType::getTypeByName));
            
            for (String bonusName : bonusNames) {
                 
                 
                 String nameKey = "gd656killicon.bonus." + bonusName + ".name";
                 String descKey = "gd656killicon.bonus." + bonusName + ".desc";
                 
                 if (!I18n.exists(descKey)) {
                     descKey = "gd656killicon.client.gui.help.bonus.default_desc"; 
                 }
                 int bonusType = BonusType.getTypeByName(bonusName);
                 addPrefixedBonusHelpEntry(bonusType, bonusName, nameKey, descKey, contentWidth);
            }
        }

        
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
        
        
        String area3Text = I18n.get("gd656killicon.client.gui.help.area3.desc");
        if (area3TextRenderer == null) {
            area3TextRenderer = new GDTextRenderer(area3Text, area3X1, area3Y1, area3X2, area3Y2, 1.0f, GuiConstants.COLOR_WHITE, true);
        } else {
            area3TextRenderer.setText(area3Text);
        }

        
        this.totalContentHeight = 0;
        for (int h : rowHeights) {
            this.totalContentHeight += h + 1; 
        }
    }

    private void addCategoryHeader(int index, String titleKey, boolean expanded, Consumer<Integer> onClick) {
        
        GDRowRenderer header = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_GOLD, 0.75f, true);
        
        header.addColumn(I18n.get(titleKey), -1, GuiConstants.COLOR_WHITE, true, false, onClick);
        
        header.addColumn(expanded ? "▼" : "▶", 20, GuiConstants.COLOR_WHITE, true, true, onClick);
        
        contentRenderers.add(header);
        rowHeights.add(GuiConstants.ROW_HEADER_HEIGHT);
    }

    private void addSubCategoryHeader(String titleKey, boolean expanded, Consumer<Integer> onClick) {
        
        
        GDRowRenderer header = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, true);
        
        
        String title = "  " + I18n.get(titleKey);
        
        
        header.addColumn(title, -1, GuiConstants.COLOR_WHITE, true, false, onClick);
        
        header.addColumn(expanded ? "▼" : "▶", 20, GuiConstants.COLOR_GRAY, true, true, onClick);
        
        contentRenderers.add(header);
        rowHeights.add(GuiConstants.ROW_HEADER_HEIGHT);
    }

    private void addIndentedHelpEntry(String titleKey, String descKey, int contentWidth) {
        String title = I18n.exists(titleKey) ? I18n.get(titleKey) : titleKey;
        String desc = I18n.exists(descKey) ? I18n.get(descKey) : descKey;
        
        
        title = "    " + title;
        
        HelpTextEntry entry = new HelpTextEntry(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, title, desc);
        int height = entry.getRequiredHeight(contentWidth);
        
        contentRenderers.add(entry);
        rowHeights.add(height);
    }

    private void addHelpEntry(String titleKey, String descKey, int contentWidth) {
        
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
        
        
        renderArea3(guiGraphics, mouseX, mouseY, partialTick);

        
        renderArea2(guiGraphics, mouseX, mouseY, partialTick, screenHeight);
        
        
        long now = System.nanoTime();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = (now - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = now;
        if (dt > 0.1f) dt = 0.1f;
        
        
        int currentContentWidth = area2X2 - area2X1;
        if (currentContentWidth != lastContentWidth && currentContentWidth > 0) {
            lastContentWidth = currentContentWidth;
            recalculateHeights(currentContentWidth);
        }

        
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
        
        
        
        if (contentRenderers.isEmpty()) {
            rebuildContent();
        }

        for (int i = 0; i < contentRenderers.size(); i++) {
            GDRowRenderer renderer = contentRenderers.get(i);
            int height = rowHeights.get(i);
            
            renderer.setBounds(x1, currentY, x2, currentY + height);
            
            
            
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
        
        
        
        
        
        
        if (index == 0) isCommandExpanded = true;
        else if (index == 1) isBonusExpanded = true;
        else if (index == 2) isPresetExpanded = true;
        
        rebuildContent();
        scrollToCategory(index);
    }

    private void scrollToCategory(int index) {
        if (index < 0 || index >= categoryStartIndices.length) return;
        
        int targetIndex = categoryStartIndices[index];
        if (targetIndex >= rowHeights.size()) return;

        
        double targetY = 0;
        for (int i = 0; i < targetIndex; i++) {
            targetY += rowHeights.get(i) + 1;
        }
        
        this.targetScrollY = targetY;
        
        double maxScroll = Math.max(0, totalContentHeight - (area2Y2 - area2Y1));
        this.targetScrollY = Math.max(0, Math.min(maxScroll, this.targetScrollY));
    }
    
    @Override
    protected void updateScroll(float dt, int screenHeight) {
        
        this.totalContentHeight = 0;
        for (int h : rowHeights) {
            this.totalContentHeight += h + 1; 
        }

        
        int viewHeight = area2Y2 - area2Y1;
        double maxScroll = Math.max(0, totalContentHeight - viewHeight);
        targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY));
        
        double diff = targetScrollY - scrollY;
        if (Math.abs(diff) < 0.1) scrollY = targetScrollY;
        else scrollY += diff * SCROLL_SMOOTHING * dt;
        
        
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
        
        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
             isDragging3 = true;
             lastMouseY3 = mouseY;
             return true;
        }
        
        
        if (mouseX >= area2X1 && mouseX <= area2X2 && mouseY >= area2Y1 && mouseY <= area2Y2) {
            double adjustedY = mouseY + scrollY;
            for (GDRowRenderer renderer : contentRenderers) {
                if (renderer.mouseClicked(mouseX, adjustedY, button)) return true;
            }

            
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
