package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;

import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.gui.elements.TextInputDialog;
import org.mods.gd656killicon.client.gui.elements.ColorPickerDialog;
import org.mods.gd656killicon.client.gui.elements.PromptDialog;
import java.util.function.Consumer;

import java.util.ArrayList;
import java.util.List;

public abstract class ConfigTabContent {
    protected final Minecraft minecraft;
    protected final Component title;
    protected GDTextRenderer titleRenderer;
    protected GDTextRenderer subtitleRenderer;
    protected int area1Bottom;
    protected final List<GDRowRenderer> configRows = new ArrayList<>();

    // Scroll state
    protected double scrollY = 0;
    protected double targetScrollY = 0;
    protected static final double SCROLL_SMOOTHING = 15.0;
    protected boolean useDefaultScroll = true;
    protected int totalContentHeight = 0;

    // 动态文本渲染器缓存
    private GDTextRenderer dynamicTitleRenderer;
    private GDTextRenderer dynamicSubtitleRenderer;

    // 底部按钮
    protected GDButton resetButton;
    protected GDButton cancelButton;
    protected boolean isResetConfirming = false;
    protected long resetConfirmTime = 0;
    protected static final long RESET_CONFIRM_TIMEOUT = 3000;
    
    protected TextInputDialog textInputDialog;
    protected ColorPickerDialog colorPickerDialog;
    protected PromptDialog promptDialog;

    // 拖拽相关
    protected boolean isDragging = false;
    protected double lastMouseY = 0;
    protected long lastFrameTime = 0;

    public ConfigTabContent(Minecraft minecraft, String titleKey) {
        this.minecraft = minecraft;
        title = Component.translatable(titleKey);
        this.textInputDialog = new TextInputDialog(minecraft, null, null);
        this.colorPickerDialog = new ColorPickerDialog(minecraft);
        this.promptDialog = new PromptDialog(minecraft, null, null);
    }

    public ConfigTabContent(Minecraft minecraft, Component title) {
        this.minecraft = minecraft;
        this.title = title;
        this.textInputDialog = new TextInputDialog(minecraft, null, null);
        this.colorPickerDialog = new ColorPickerDialog(minecraft);
        this.promptDialog = new PromptDialog(minecraft, null, null);
    }

    public TextInputDialog getTextInputDialog() {
        return textInputDialog;
    }
    
    public ColorPickerDialog getColorPickerDialog() {
        return colorPickerDialog;
    }

    public PromptDialog getPromptDialog() {
        return promptDialog;
    }

    public List<GDRowRenderer> getConfigRows() {
        return configRows;
    }

    public void sortConfigRows() {
        if (configRows != null && !configRows.isEmpty()) {
            configRows.sort((a, b) -> {
                String keyA = a.getSortKey();
                String keyB = b.getSortKey();
                return keyA.compareToIgnoreCase(keyB);
            });
        }
    }

    public void onFilesDrop(java.util.List<java.nio.file.Path> paths) {
        // Default implementation does nothing
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        updateResetButtonState();

        // If dialog is visible, block hover effects on underlying elements by passing invalid mouse coordinates
        boolean isDialogVisible = textInputDialog.isVisible() || colorPickerDialog.isVisible() || promptDialog.isVisible();
        int effectiveMouseX = isDialogVisible ? -1 : mouseX;
        int effectiveMouseY = isDialogVisible ? -1 : mouseY;

        // 检查是否有配置行被悬停
        GDRowRenderer hoveredRow = null;
        for (GDRowRenderer row : configRows) {
            if (row.isHovered(effectiveMouseX, effectiveMouseY)) {
                hoveredRow = row;
                break;
            }
        }

        // 渲染一号区域的标题和副标题（布局已在 updateLayout 中更新）
        if (titleRenderer != null) titleRenderer.render(guiGraphics, partialTick);
        if (subtitleRenderer != null) subtitleRenderer.render(guiGraphics, partialTick);
        
        // 渲染二号区域内容
        renderContent(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick, screenWidth, screenHeight, headerHeight);
        
        // 渲染侧边按钮（位于 Area 3 下方）
        renderSideButtons(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick, screenWidth, screenHeight);

        // 如果有配置行被悬停，则在 Area 3 (Area 1 下方) 渲染动态描述
        // 仅对 GlobalConfigTab 启用此功能
        if (this instanceof GlobalConfigTab && hoveredRow != null && hoveredRow.getHoverTitle() != null) {
            renderDynamicDescription(guiGraphics, hoveredRow, partialTick);
        }
        
        // Render Dialogs (Top Layer)
        if (textInputDialog.isVisible()) {
            textInputDialog.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        if (colorPickerDialog.isVisible()) {
            colorPickerDialog.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        if (promptDialog.isVisible()) {
            promptDialog.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    public void openTextInput(String title, String initialValue, Consumer<String> onConfirm) {
        textInputDialog.show(title, initialValue, onConfirm);
    }
    
    protected void updateResetButtonState() {
        if (isResetConfirming) {
            long elapsed = System.currentTimeMillis() - resetConfirmTime;
            
            if (elapsed > 6000) {
                 isResetConfirming = false;
                 if (resetButton != null) {
                     resetButton.setMessage(Component.translatable("gd656killicon.client.gui.button.reset"));
                     resetButton.setTextColor(GuiConstants.COLOR_WHITE);
                 }
            } else if (elapsed >= 3000) {
                 if (resetButton != null) {
                     resetButton.setMessage(Component.translatable("gd656killicon.client.gui.config.button.confirm_reset"));
                     resetButton.setTextColor(GuiConstants.COLOR_RED);
                 }
            } else {
                 if (resetButton != null) {
                    long remaining = 3 - (elapsed / 1000);
                    resetButton.setMessage(Component.translatable("gd656killicon.client.gui.config.button.confirm_reset_time", remaining));
                    resetButton.setTextColor(GuiConstants.COLOR_DARK_RED); 
                 }
            }
        }
    }

    protected void renderSideButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight) {
        // 计算按钮区域位置
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int buttonY = screenHeight - GuiConstants.DEFAULT_PADDING - GuiConstants.ROW_HEADER_HEIGHT - 1 - GuiConstants.ROW_HEADER_HEIGHT;
        
        // 2 buttons, 1 space (1px)
        // Total width = area1Right - GuiConstants.DEFAULT_PADDING
        int totalWidth = area1Right - GuiConstants.DEFAULT_PADDING;
        int buttonWidth = (totalWidth - 1) / 2;
        int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int x1 = GuiConstants.DEFAULT_PADDING + (int)getSidebarOffset();

        // 检查重置按钮超时
        // The check is done in render() now
        /*
        if (isResetConfirming && System.currentTimeMillis() - resetConfirmTime > RESET_CONFIRM_TIMEOUT) {
            isResetConfirming = false;
            if (resetButton != null) {
                resetButton.setMessage(Component.translatable("gd656killicon.client.gui.button.reset"));
            }
        }
        */

        if (resetButton == null) {
            resetButton = new GDButton(x1, buttonY, buttonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.button.reset"), (btn) -> {
                if (isResetConfirming) {
                    long elapsed = System.currentTimeMillis() - resetConfirmTime;
                    if (elapsed >= 3000) {
                        // 3秒后点击才有效
                        ConfigManager.resetFull();
                        // 重置后无需保存，直接丢弃当前的临时更改状态
                        ConfigManager.discardChanges();
                        
                        // 退出界面
                        if (minecraft.screen != null) {
                            minecraft.screen.onClose();
                        }

                        isResetConfirming = false;
                        btn.setMessage(Component.translatable("gd656killicon.client.gui.button.reset"));
                        btn.setTextColor(GuiConstants.COLOR_WHITE);
                    }
                } else {
                    // 第一次点击
                    isResetConfirming = true;
                    resetConfirmTime = System.currentTimeMillis();
                    btn.setMessage(Component.translatable("gd656killicon.client.gui.config.button.confirm_reset_time", 3));
                    btn.setTextColor(GuiConstants.COLOR_DARK_RED);
                }
            });
        }
        
        resetButton.setX(x1);
        resetButton.setY(buttonY);
        resetButton.setWidth(buttonWidth);
        resetButton.setHeight(buttonHeight);
        resetButton.render(guiGraphics, mouseX, mouseY, partialTick);

        if (cancelButton == null) {
            cancelButton = new GDButton(x1 + buttonWidth + 1, buttonY, buttonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.button.cancel"), (btn) -> {
                // 取消操作，同 ESC
                if (minecraft.screen != null) {
                    minecraft.screen.onClose();
                }
            });
        }
        
        cancelButton.setX(x1 + buttonWidth + 1);
        cancelButton.setY(buttonY);
        cancelButton.setWidth(buttonWidth);
        cancelButton.setHeight(buttonHeight);
        cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected void renderDynamicDescription(GuiGraphics guiGraphics, GDRowRenderer row, float partialTick) {
        // Area 3 位于 Area 1 下方
        int area1Right = (minecraft.getWindow().getGuiScaledWidth() - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int x1 = GuiConstants.DEFAULT_PADDING;
        
        // Area 3 Top = Area 1 Bottom + Padding
        int y1 = this.area1Bottom + GuiConstants.DEFAULT_PADDING;
        
        int x2 = area1Right;
        
        // 1. 标题 (Config Name)
        // 大小 2.0，白色，不换行，紧贴上边框
        // 标题高度大约是 9 * 2.0 = 18
        int titleHeight = 18;
        if (dynamicTitleRenderer == null) {
            dynamicTitleRenderer = new GDTextRenderer(row.getHoverTitle(), x1, y1, x2, y1 + titleHeight, 2.0f, GuiConstants.COLOR_WHITE, false);
        } else {
            dynamicTitleRenderer.setX1(x1); dynamicTitleRenderer.setY1(y1); dynamicTitleRenderer.setX2(x2); dynamicTitleRenderer.setY2(y1 + titleHeight);
            dynamicTitleRenderer.setText(row.getHoverTitle());
        }
        dynamicTitleRenderer.render(guiGraphics, partialTick);

        // 副标题 (Description)
        // 大小 1.0，白色，自动换行
        int subY1 = y1 + titleHeight + GuiConstants.DEFAULT_PADDING / 2;
        // 高度自适应或者占满剩余空间 (减去底部控制区高度和新增的按钮高度)
        int buttonY = minecraft.getWindow().getGuiScaledHeight() - GuiConstants.DEFAULT_PADDING - GuiConstants.REGION_4_HEIGHT - 1 - GuiConstants.REGION_4_HEIGHT;
        int subY2 = buttonY - GuiConstants.DEFAULT_PADDING;
        
        if (dynamicSubtitleRenderer == null) {
            dynamicSubtitleRenderer = new GDTextRenderer(row.getHoverDescription(), x1, subY1, x2, subY2, 1.0f, GuiConstants.COLOR_WHITE, true);
        } else {
            dynamicSubtitleRenderer.setX1(x1); dynamicSubtitleRenderer.setY1(subY1); dynamicSubtitleRenderer.setX2(x2); dynamicSubtitleRenderer.setY2(subY2);
            dynamicSubtitleRenderer.setText(row.getHoverDescription());
        }
        dynamicSubtitleRenderer.render(guiGraphics, partialTick);
    }

    protected void updateScroll(float dt, int screenHeight) {
        if (!useDefaultScroll) return;

        int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
        int viewHeight = screenHeight - contentY - GuiConstants.DEFAULT_PADDING;

        double maxScroll = Math.max(0, totalContentHeight - viewHeight);
        targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY));

        double diff = targetScrollY - scrollY;
        if (Math.abs(diff) < 0.01) {
            scrollY = targetScrollY;
        } else {
            scrollY += diff * SCROLL_SMOOTHING * dt;
        }
    }

    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        if (!configRows.isEmpty()) {
            if (useDefaultScroll) {
                // 更新时间步进
                long now = System.nanoTime();
                if (lastFrameTime == 0) lastFrameTime = now;
                float dt = (now - lastFrameTime) / 1_000_000_000.0f;
                lastFrameTime = now;
                if (dt > 0.1f) dt = 0.1f;

                // 处理拖拽逻辑 (在调用 updateScroll 之前)
                if (isDragging) {
                    double diff = mouseY - lastMouseY;
                    targetScrollY -= diff;
                    lastMouseY = mouseY;
                }

                updateScroll(dt, screenHeight);

                int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
                int contentX = area1Right + GuiConstants.DEFAULT_PADDING;
                int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
                int contentWidth = screenWidth - contentX - GuiConstants.DEFAULT_PADDING;
                int contentHeight = screenHeight - contentY - GuiConstants.DEFAULT_PADDING;

                // Enable Scissor
                guiGraphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, -scrollY, 0);

                for (GDRowRenderer row : configRows) {
                    row.render(guiGraphics, mouseX, (int)(mouseY + scrollY), partialTick);
                }

                guiGraphics.pose().popPose();
                guiGraphics.disableScissor();
            } else {
                for (GDRowRenderer row : configRows) {
                    row.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }
            return;
        }

        // Default implementation: Show "No Content"
        Component noContent = Component.translatable("gd656killicon.client.gui.config.no_content");
        int textWidth = minecraft.font.width(noContent);
        
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int contentX = area1Right + GuiConstants.DEFAULT_PADDING + (screenWidth - area1Right - 2 * GuiConstants.DEFAULT_PADDING - textWidth) / 2;
        int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING + (screenHeight - (GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING) - 9) / 2;
        
        guiGraphics.drawString(minecraft.font, noContent, contentX, contentY, GuiConstants.COLOR_GRAY, true);
    }
    
    public Component getTitle() {
        return title;
    }

    /**
     * 当该页签被点击/打开时触发
     */
    public void onTabOpen() {}

    /**
     * 获取侧边栏的当前平移偏移量（X轴）
     * 仅供 ConfigScreenHeader 使用，用于同步底部按钮位置
     */
    public float getSidebarOffset() {
        return 0.0f;
    }

    /**
     * 侧边栏是否为浮动/滑动模式
     * 如果为真，Area 2 (主内容区) 的左边框将延伸到最左侧 (DEFAULT_PADDING)
     */
    public boolean isSidebarFloating() {
        return false;
    }

    /**
     * 在渲染边框前更新布局参数，防止1帧延迟
     */
    public void updateLayout(int screenWidth, int screenHeight) {
        int goldBarBottom = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT;
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        
        // 1. 标题布局
        int x1 = GuiConstants.DEFAULT_PADDING;
        int y1 = goldBarBottom + GuiConstants.DEFAULT_PADDING;
        int x2 = area1Right;
        int titleHeight = (int)(9 * 3.0f);
        int y2 = y1 + titleHeight;

        if (titleRenderer == null) {
            onTabOpen(); // 首次初始化布局时也触发一次打开事件
            titleRenderer = new GDTextRenderer(title.getString(), x1, y1, x2, y2, 3.0f, GuiConstants.COLOR_WHITE, false);
        } else {
            titleRenderer.setX1(x1); titleRenderer.setY1(y1); titleRenderer.setX2(x2); titleRenderer.setY2(y2);
        }

        // 2. 副标题布局
        int subY1 = y2 + GuiConstants.DEFAULT_PADDING / 2;
        int subX1 = GuiConstants.DEFAULT_PADDING;
        int subX2 = area1Right;
        
        updateSubtitle(subX1, subY1, subX2);
        
        if (subtitleRenderer != null) {
            this.area1Bottom = subY1 + subtitleRenderer.getFinalHeight();
        } else {
            this.area1Bottom = y2;
        }

        updateConfigRowsLayout(screenWidth, screenHeight);
    }

    protected void updateConfigRowsLayout(int screenWidth, int screenHeight) {
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int contentX = area1Right + GuiConstants.DEFAULT_PADDING;
        int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
        int contentWidth = screenWidth - contentX - GuiConstants.DEFAULT_PADDING;
        
        int currentY = contentY;
        for (int i = 0; i < configRows.size(); i++) {
            GDRowRenderer row = configRows.get(i);
            int rowHeight = GuiConstants.ROW_HEADER_HEIGHT; // 默认为表头高度17，也可以自定义
            // 这里假设所有配置行高度都是17，如果需要不同高度可以在 Row 中获取
            
            row.setBounds(contentX, currentY, contentX + contentWidth, currentY + rowHeight);
            
            // 双数行透明度降低效果 (索引从0开始，0是单数行视觉上? 不，0是第一行)
            // 榜单透明度通常是 0x4D (77/255 -> 0.3)
            // ScoreboardTab 中使用的是 0.15f 和 0.3f
            if (i % 2 != 0) {
                row.setBackgroundAlpha(0.15f); // 双数行降低透明度
            } else {
                row.setBackgroundAlpha(0.3f); // 单数行标准透明度
            }
            
            currentY += rowHeight + 1; // 1像素间隔
        }
        
        this.totalContentHeight = currentY - contentY;
    }

    protected void renderTabHeader(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
        // 兼容旧调用，实际渲染逻辑已移至 render
        updateLayout(screenWidth, screenHeight);
    }

    protected void updateSubtitle(int x1, int y1, int x2) {
        // 默认显示翻译键对应的简介，开启自动换行
        String key = title.getContents().toString();
        // 提取翻译键
        String translationKey = "";
        if (title.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc) {
            translationKey = tc.getKey();
        }
        
        String subKey = translationKey + ".subtitle";
        Component subtitle = Component.translatable(subKey);
        
        if (subtitleRenderer == null) {
            subtitleRenderer = new GDTextRenderer(subtitle.getString(), x1, y1, x2, y1 + 100, 1.0f, GuiConstants.COLOR_GRAY, true);
        } else {
            subtitleRenderer.setX1(x1); subtitleRenderer.setY1(y1); subtitleRenderer.setX2(x2);
        }
    }

    public int getArea1Bottom() {
        return area1Bottom;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (promptDialog.isVisible()) {
            return promptDialog.mouseClicked(mouseX, mouseY, button);
        }
        if (textInputDialog.isVisible()) {
            return textInputDialog.mouseClicked(mouseX, mouseY, button);
        }
        if (colorPickerDialog.isVisible()) {
            return colorPickerDialog.mouseClicked(mouseX, mouseY, button);
        }

        if (resetButton != null && resetButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (cancelButton != null && cancelButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        if (useDefaultScroll) {
             int area1Right = (minecraft.getWindow().getGuiScaledWidth() - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
             int contentX = area1Right + GuiConstants.DEFAULT_PADDING;
             if (mouseX > contentX) {
                 double adjustedMouseY = mouseY + scrollY;
                 for (GDRowRenderer row : configRows) {
                     if (row.mouseClicked(mouseX, adjustedMouseY, button)) {
                         return true;
                     }
                 }
                 
                 // 如果没有点击到任何行内元素，则开始拖拽
                 isDragging = true;
                 lastMouseY = mouseY;
                 return true;
             }
        } else {
            for (GDRowRenderer row : configRows) {
                if (row.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        if (promptDialog.isVisible()) {
            return true;
        }
        if (colorPickerDialog.isVisible()) {
            return colorPickerDialog.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (promptDialog.isVisible()) {
            return promptDialog.mouseScrolled(mouseX, mouseY, delta);
        }
        if (textInputDialog.isVisible()) {
            return textInputDialog.mouseScrolled(mouseX, mouseY, delta);
        }
        if (colorPickerDialog.isVisible()) {
            return colorPickerDialog.mouseScrolled(mouseX, mouseY, delta);
        }

        if (useDefaultScroll) {
             int area1Right = (minecraft.getWindow().getGuiScaledWidth() - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
             int contentX = area1Right + GuiConstants.DEFAULT_PADDING;
             if (mouseX > contentX) {
                 targetScrollY -= delta * GuiConstants.SCROLL_AMOUNT;
                 return true;
             }
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (promptDialog.isVisible()) {
            return promptDialog.charTyped(codePoint, modifiers);
        }
        if (textInputDialog.isVisible()) {
            return textInputDialog.charTyped(codePoint, modifiers);
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (promptDialog.isVisible()) {
            return promptDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (textInputDialog.isVisible()) {
            return textInputDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (colorPickerDialog.isVisible()) {
            return colorPickerDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
}
