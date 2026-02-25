package org.mods.gd656killicon.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.tabs.*;

import java.util.ArrayList;
import java.util.List;

import static org.mods.gd656killicon.client.gui.GuiConstants.*;

/**
 * Handles the rendering and logic for the configuration screen header.
 * Designed to be reusable and distinct from the main screen logic.
 */
public class ConfigScreenHeader {
    
    private final Minecraft minecraft;
    private final Component title;
    private final Component subtitle;
    
    private long openTime;
    private long lastRenderTime;
    
    // Layout & Scroll
    private int splitPoint; // X coordinate where Part 1 ends and Part 2 begins
    private double scrollX;
    private double targetScrollX;
    private double maxScroll;
    private boolean isDragging;
    private boolean isPressed;
    private double lastMouseX;
    
    // Tab System
    private final List<Tab> tabs = new ArrayList<>();
    private Tab selectedTab;

    // Footer Elements
    private GDButton saveExitButton;

    // Override Content (for sub-screens like ElementConfigScreen)
    private ConfigTabContent overrideContent;

    public ConfigScreenHeader() {
        this.minecraft = Minecraft.getInstance();
        this.title = Component.translatable("gd656killicon.client.gui.config.title");
        this.subtitle = Component.translatable("gd656killicon.client.gui.config.subtitle");
        this.openTime = System.currentTimeMillis();
        this.lastRenderTime = System.currentTimeMillis();
        
        // Initialize Tabs
        initTabs();
    }

    private void initTabs() {
        tabs.add(new Tab(new HomeTab(minecraft)));
        tabs.add(new Tab(new GlobalConfigTab(minecraft)));
        
        PresetConfigTab presetTab = new PresetConfigTab(minecraft);
        presetTab.setHeader(this);
        tabs.add(new Tab(presetTab));
        
        tabs.add(new Tab(new ScoreboardTab(minecraft)));
        tabs.add(new Tab(new HelpTab(minecraft)));
        
        if (!tabs.isEmpty()) {
            selectedTab = tabs.get(0);
            selectedTab.isSelected = true;
        }
    }

    // Input Handling
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check Footer Buttons (Area 4)
        if (saveExitButton != null && overrideContent == null && saveExitButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && mouseX > splitPoint && mouseY <= HEADER_HEIGHT + HEADER_CLICK_ZONE) {
            double localMouseX = mouseX - splitPoint + scrollX;
            
            // Check Tabs
            for (Tab tab : tabs) {
                if (localMouseX >= tab.x && localMouseX <= tab.x + tab.width && mouseY >= tab.finalY && mouseY <= tab.finalY + tab.height) {
                    if (selectedTab != tab) {
                        selectedTab.isSelected = false;
                        selectedTab = tab;
                        selectedTab.isSelected = true;
                        selectedTab.content.onTabOpen();
                        overrideContent = null; // Clear override when switching tabs
                    }
                    isDragging = false;
                    isPressed = true;
                    lastMouseX = mouseX;
                    return true;
                }
            }
            
            // Start drag prep
            isDragging = false;
            isPressed = true;
            lastMouseX = mouseX;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        isPressed = false;
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            targetScrollX -= dragX;
            scrollX = targetScrollX;
            clampScroll();
            return true;
        }

        if (isPressed && mouseY <= HEADER_HEIGHT + HEADER_SCROLL_ZONE && mouseX > splitPoint) {
            isDragging = true;
            targetScrollX -= dragX;
            scrollX = targetScrollX;
            clampScroll();
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX > splitPoint && mouseY <= HEADER_HEIGHT + HEADER_SCROLL_ZONE) {
            targetScrollX -= delta * SCROLL_AMOUNT;
            clampScroll();
            return true;
        }
        return false;
    }

    private void clampScroll() {
        targetScrollX = Mth.clamp(targetScrollX, 0, maxScroll);
        if (isDragging) {
            scrollX = targetScrollX;
        }
    }

    // Rendering
    public void render(GuiGraphics guiGraphics, int screenWidth, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float dt = (now - lastRenderTime) / 1000.0f;
        lastRenderTime = now;
        
        // 先更新当前选中的标签布局，确保 area1Bottom 是最新的
        ConfigTabContent activeTab = getSelectedTabContent();
        int screenHeight = guiGraphics.guiHeight();
        if (activeTab != null) {
            activeTab.updateLayout(screenWidth, screenHeight);
        }

        // Calculate Layout & Physics
        calculateLayout(screenWidth);
        updateScroll(dt);

        // Update Area 4 Button
        updateFooterButtons(screenWidth, screenHeight);
        
        // Render Part 2 (Scrollable Tabs)
        renderPart2(guiGraphics, screenWidth, mouseX, mouseY, now, dt);
        
        // Render Part 1 (Static Title/Subtitle)
        renderPart1(guiGraphics, now);
        
        // Render Intro Slice (Global Overlay)
        renderIntroSlice(guiGraphics, screenWidth, now);

        // Render Footer Buttons
        if (saveExitButton != null && overrideContent == null) {
            saveExitButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Render Region Borders
        renderRegionBorders(guiGraphics, screenWidth, screenHeight);
    }

    private void updateFooterButtons(int screenWidth, int screenHeight) {
        int area1Right = (screenWidth - 2 * DEFAULT_PADDING) / 3 + DEFAULT_PADDING;
        int x = DEFAULT_PADDING;
        int y = screenHeight - DEFAULT_PADDING - ROW_HEADER_HEIGHT;
        int width = area1Right - DEFAULT_PADDING;
        int height = ROW_HEADER_HEIGHT;

        // Apply sidebar offset from active tab (e.g., PresetConfigTab sliding panel)
        ConfigTabContent activeTab = getSelectedTabContent();
        if (activeTab != null) {
            float offset = activeTab.getSidebarOffset();
            x += (int)offset;
        }

        if (saveExitButton == null) {
            saveExitButton = new GDButton(x, y, width, height, Component.translatable("gd656killicon.client.gui.config.button.save_and_exit"), (btn) -> {
                ConfigManager.saveChanges();
                if (minecraft.screen != null) {
                    minecraft.screen.onClose();
                }
            });
        }
        
        saveExitButton.setX(x);
        saveExitButton.setY(y);
        saveExitButton.setWidth(width);
        saveExitButton.setHeight(height);
    }
    
    private void renderRegionBorders(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        int goldBarBottom = HEADER_HEIGHT + GOLD_BAR_HEIGHT;
        
        // Calculate dynamic dimensions
        int area1Right = (screenWidth - 2 * DEFAULT_PADDING) / 3 + DEFAULT_PADDING;
        
        // 获取当前选中的标签内容以确定动态下边框和侧边栏状态
        int area1Bottom;
        float sidebarOffset = 0;
        boolean isSidebarFloating = false;
        
        ConfigTabContent activeTab = getSelectedTabContent();
        if (activeTab != null) {
            // 一号区域的下底边与灰色字幕的最下边完全对齐，不加额外的 Padding
            area1Bottom = activeTab.getArea1Bottom();
            sidebarOffset = activeTab.getSidebarOffset();
            isSidebarFloating = activeTab.isSidebarFloating();
        } else {
            area1Bottom = goldBarBottom + DEFAULT_PADDING + AREA1_BOTTOM_OFFSET;
        }
        
        int area4Top = screenHeight - DEFAULT_PADDING - REGION_4_HEIGHT;
        
        // 使用半透明灰色 (例如 0x80505050)
        int translucentGray = (0x80 << 24) | (COLOR_GRAY & 0xFFFFFF);
        
        // Side Areas (1, 3, 4) with potential offset
        int sideX1 = DEFAULT_PADDING + (int)sidebarOffset;
        int sideX2 = area1Right + (int)sidebarOffset;
        
        // Area 1 (Big Title + Subtitle)
        drawBorderRect(guiGraphics, sideX1, goldBarBottom + DEFAULT_PADDING, sideX2, area1Bottom, translucentGray);
        
        // Area 2 (List)
        // If sidebar is floating, Area 2 extends to the left (DEFAULT_PADDING)
        // Otherwise, it starts after Area 1
        int area2X1 = isSidebarFloating ? DEFAULT_PADDING : (area1Right + DEFAULT_PADDING);
        int area2Top = goldBarBottom + DEFAULT_PADDING;
        if (activeTab instanceof ElementConfigContent elementContent && elementContent.isKillIconElement()) {
            area2Top += elementContent.getSecondaryTabHeight();
            drawBorderRectNoTop(guiGraphics, area2X1, area2Top, screenWidth - DEFAULT_PADDING, screenHeight - DEFAULT_PADDING, translucentGray);
        } else {
            drawBorderRect(guiGraphics, area2X1, area2Top, screenWidth - DEFAULT_PADDING, screenHeight - DEFAULT_PADDING, translucentGray);
        }
        
        // Area 3 (Sub Control) - 位于 Area 1 下方，Area 4 上方
        drawBorderRect(guiGraphics, sideX1, area1Bottom + DEFAULT_PADDING, sideX2, area4Top - DEFAULT_PADDING, translucentGray);
        
        // Area 4 (Total Control) - 位于最下方
        drawBorderRect(guiGraphics, sideX1, area4Top, sideX2, screenHeight - DEFAULT_PADDING, translucentGray);
    }
    
    private void drawBorderRect(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y1 + 1, color); // Top
        guiGraphics.fill(x1, y2 - 1, x2, y2, color); // Bottom
        guiGraphics.fill(x1, y1, x1 + 1, y2, color); // Left
        guiGraphics.fill(x2 - 1, y1, x2, y2, color); // Right
    }

    private void drawBorderRectNoTop(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y2 - 1, x2, y2, color);
        guiGraphics.fill(x1, y1, x1 + 1, y2, color);
        guiGraphics.fill(x2 - 1, y1, x2, y2, color);
    }


    private void updateScroll(float dt) {
        if (!isDragging) {
            double diff = targetScrollX - scrollX;
            if (Math.abs(diff) < 0.1) {
                scrollX = targetScrollX;
            } else {
                scrollX += diff * SCROLL_SMOOTHING * dt;
            }
        }
    }

    private void calculateLayout(int screenWidth) {
        int titleWidth = minecraft.font.width(title);
        int subtitleWidth = minecraft.font.width(subtitle);
        
        // Split Point: Where Part 1 ends
        splitPoint = titleWidth + (int)(subtitleWidth * 0.5f) + DEFAULT_PADDING + SPLIT_POINT_OFFSET;
        
        // Calculate Tabs Layout
        float currentX = 0;
        for (Tab tab : tabs) {
            tab.width = minecraft.font.width(tab.content.getTitle()) + 10;
            tab.x = (int)currentX;
            tab.finalY = TAB_Y_OFFSET;
            tab.height = HEADER_HEIGHT + HEADER_CLICK_ZONE;
            currentX += tab.width + TAB_SPACING;
        }
        
        // Calculate scroll bounds
        float totalContentWidth = currentX;
        float visibleWidth = screenWidth - splitPoint;
        maxScroll = Math.max(0, totalContentWidth - visibleWidth);
        
        // Safety clamp if window resized
        if (targetScrollX > maxScroll) targetScrollX = maxScroll;
        if (scrollX > maxScroll) scrollX = maxScroll;
    }

    private void renderPart1(GuiGraphics guiGraphics, long now) {
        // Background and gold bar
        guiGraphics.fill(0, 0, splitPoint, HEADER_HEIGHT, COLOR_BG);
        guiGraphics.fill(0, HEADER_HEIGHT, splitPoint, HEADER_HEIGHT + GOLD_BAR_HEIGHT, COLOR_GOLD);
        
        // Glow effect
        guiGraphics.fillGradient(0, HEADER_HEIGHT - 5, splitPoint, HEADER_HEIGHT, 0x00000000, (0x99 << 24) | (COLOR_GOLD & 0x00FFFFFF));
        
        // Title animation
        long elapsed = now - openTime;
        float ease = getEaseOutCubic(Math.min(1.0f, elapsed / (float)INTRO_DURATION_MS));
        float animOffsetX = -TITLE_ANIM_OFFSET * (1.0f - ease);
        
        // Render title
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(animOffsetX + DEFAULT_PADDING, (float)DEFAULT_PADDING, 0);
        guiGraphics.drawString(minecraft.font, title, 0, 0, COLOR_GOLD, true);
        guiGraphics.pose().popPose();

        // Render subtitle
        float subtitleX = DEFAULT_PADDING + minecraft.font.width(title) + 5;
        float subtitleY = DEFAULT_PADDING + 9 - 11 * 0.5f;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(animOffsetX + subtitleX, subtitleY, 0);
        guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
        guiGraphics.drawString(minecraft.font, subtitle, 0, 0, COLOR_GRAY, true);
        guiGraphics.pose().popPose();
    }

    private void renderPart2(GuiGraphics guiGraphics, int screenWidth, int mouseX, int mouseY, long now, float dt) {
        // Scissor Area
        guiGraphics.enableScissor(splitPoint, 0, screenWidth, HEADER_HEIGHT + 10);
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float)(splitPoint - scrollX), 0, 0);
        
        double localMouseX = mouseX - splitPoint + scrollX;
        
        // Render Background
        renderPart2Background(guiGraphics, screenWidth);

        // Render Tabs
        renderTabs(guiGraphics, localMouseX, mouseY, now, dt);
        
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }
    
    private void renderPart2Background(GuiGraphics guiGraphics, int screenWidth) {
        float drawEnd = Math.max(screenWidth + (float)maxScroll, 5000);
        
        if (selectedTab == null) {
            guiGraphics.fill(0, 0, (int)drawEnd, HEADER_HEIGHT, COLOR_BG);
            guiGraphics.fill(0, HEADER_HEIGHT, (int)drawEnd, HEADER_HEIGHT + GOLD_BAR_HEIGHT, COLOR_GOLD);
            renderPart2Glow(guiGraphics, 0, (int)drawEnd);
            return;
        }
        
        // Segment 1: 0 to SelectedTab.x
        if (selectedTab.x > 0) {
            guiGraphics.fill(0, 0, selectedTab.x, HEADER_HEIGHT, COLOR_BG);
            guiGraphics.fill(0, HEADER_HEIGHT, selectedTab.x, HEADER_HEIGHT + GOLD_BAR_HEIGHT, COLOR_GOLD);
            renderPart2Glow(guiGraphics, 0, selectedTab.x);
        }
        
        // Segment 2: SelectedTab.right to End
        int tabRight = selectedTab.x + selectedTab.width;
        if (tabRight < drawEnd) {
            guiGraphics.fill(tabRight, 0, (int)drawEnd, HEADER_HEIGHT, COLOR_BG);
            guiGraphics.fill(tabRight, HEADER_HEIGHT, (int)drawEnd, HEADER_HEIGHT + GOLD_BAR_HEIGHT, COLOR_GOLD);
            renderPart2Glow(guiGraphics, tabRight, (int)drawEnd);
        }
    }
    
    private void renderPart2Glow(GuiGraphics guiGraphics, int startX, int endX) {
        int gradientStartY = HEADER_HEIGHT;
        int gradientEndY = HEADER_HEIGHT - 5;
        guiGraphics.fillGradient(startX, gradientEndY, endX, gradientStartY, 0x00000000, (0x99 << 24) | (COLOR_GOLD & 0x00FFFFFF));
    }

    private void renderTabs(GuiGraphics guiGraphics, double localMouseX, int mouseY, long now, float dt) {
        int selectedIndex = tabs.indexOf(selectedTab);

        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean isSelected = (tab == selectedTab);
            boolean hovered = localMouseX >= tab.x && localMouseX <= tab.x + tab.width && mouseY >= tab.finalY && mouseY <= tab.finalY + tab.height && mouseY <= HEADER_HEIGHT + HEADER_CLICK_ZONE;

            // Update Animations
            if (hovered) {
                tab.hoverProgress = Math.min(1.0f, tab.hoverProgress + ANIMATION_SPEED * dt);
            } else {
                tab.hoverProgress = Math.max(0.0f, tab.hoverProgress - ANIMATION_SPEED * dt);
            }
            
            if (isSelected) {
                tab.selectionProgress = Math.min(1.0f, tab.selectionProgress + ANIMATION_SPEED * dt);
            } else {
                tab.selectionProgress = Math.max(0.0f, tab.selectionProgress - ANIMATION_SPEED * dt);
            }
            
            // Intro Animation
            int distance = Math.abs(i - selectedIndex);
            long delay = distance * (long)TAB_DELAY_MS;
            long tabIntroElapsed = now - (openTime + delay);
            float introEase = getEaseOutCubic(tabIntroElapsed > 0 ? Math.min(1.0f, tabIntroElapsed / (float)INTRO_DURATION_MS) : 0.0f);
            
            // Text Rendering
            float textCurrentY = HEADER_HEIGHT + (tab.finalY - HEADER_HEIGHT) * introEase;
            int alpha = Math.max(5, (int)(255 * introEase));
            
            int baseColor = isSelected ? COLOR_WHITE : (tab.hoverProgress > 0 ? COLOR_DARK_GRAY : COLOR_GRAY);
            int colorWithAlpha = (baseColor & 0x00FFFFFF) | (alpha << 24);
            
            int textX = tab.x + (tab.width - minecraft.font.width(tab.content.getTitle())) / 2;
            
            // 使用亚像素级平移实现平滑动画
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(textX, textCurrentY + 8.0f, 0);
            guiGraphics.drawString(minecraft.font, tab.content.getTitle(), 0, 0, colorWithAlpha, true);
            guiGraphics.pose().popPose();
            
            // Selection Border
            if (tab.selectionProgress > 0.01f) {
                float borderEase = getEaseOutCubic(tab.selectionProgress);
                int borderCurrentY = (int)(HEADER_HEIGHT + (tab.finalY - HEADER_HEIGHT) * borderEase);
                
                guiGraphics.fill(tab.x, borderCurrentY, tab.x + tab.width, borderCurrentY + 1, COLOR_GOLD);
                guiGraphics.fill(tab.x, borderCurrentY, tab.x + 1, HEADER_HEIGHT + GOLD_BAR_HEIGHT, COLOR_GOLD);
                guiGraphics.fill(tab.x + tab.width - 1, borderCurrentY, tab.x + tab.width, HEADER_HEIGHT + GOLD_BAR_HEIGHT, COLOR_GOLD);
            }
            
            // Hover Border
            if (tab.hoverProgress > 0.01f) {
                float hoverEase = getEaseOutCubic(tab.hoverProgress);
                int hoverCurrentY = (int)(HEADER_HEIGHT + (tab.finalY - HEADER_HEIGHT) * hoverEase);
                
                RenderSystem.enableBlend();
                guiGraphics.fill(tab.x, hoverCurrentY, tab.x + tab.width, hoverCurrentY + 1, COLOR_HOVER_BORDER);
                guiGraphics.fill(tab.x, hoverCurrentY, tab.x + 1, HEADER_HEIGHT, COLOR_HOVER_BORDER);
                guiGraphics.fill(tab.x + tab.width - 1, hoverCurrentY, tab.x + tab.width, HEADER_HEIGHT, COLOR_HOVER_BORDER);
                RenderSystem.disableBlend();
            }
        }
    }

    private void renderIntroSlice(GuiGraphics guiGraphics, int screenWidth, long now) {
        long elapsed = now - openTime;
        if (elapsed > SLICE_DURATION_MS) return;
        
        float t = elapsed / (float)SLICE_DURATION_MS;
        float ease = getEaseOutCubic(t);
        
        float sliceWidth = HEADER_HEIGHT / 3.0f;
        float currentX = screenWidth * ease;
        
        float alpha = 1.0f - Math.min(1.0f, t * 3.0f);
        if (alpha <= 0.01f) return;
        
        int color = ((int)(alpha * 255) << 24) | (COLOR_GRAY & 0x00FFFFFF);
        
        RenderSystem.enableBlend();
        guiGraphics.fill((int)currentX, 0, (int)(currentX + sliceWidth), HEADER_HEIGHT, color);
        RenderSystem.disableBlend();
    }
    
    private float getEaseOutCubic(float t) {
        return 1 - (float)Math.pow(1 - t, 3);
    }
    
    public void setSelectedTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            Tab tab = tabs.get(index);
            if (selectedTab != tab) {
                if (selectedTab != null) selectedTab.isSelected = false;
                selectedTab = tab;
                selectedTab.isSelected = true;
                selectedTab.content.onTabOpen();
            }
        }
    }

    public int getTabCount() {
        return tabs.size();
    }

    public ConfigTabContent getTabContent(int index) {
        return (index >= 0 && index < tabs.size()) ? tabs.get(index).content : null;
    }

    public ConfigTabContent getSelectedTabContent() {
        if (overrideContent != null) return overrideContent;
        return selectedTab != null ? selectedTab.content : null;
    }

    public void setOverrideContent(ConfigTabContent content) {
        this.overrideContent = content;
    }
    
    public void resetAnimation() {
        openTime = System.currentTimeMillis();
        lastRenderTime = System.currentTimeMillis();
        for (Tab tab : tabs) {
            tab.selectionProgress = 0.0f;
            tab.hoverProgress = 0.0f;
        }
    }

    private static class Tab {
        ConfigTabContent content;
        int x, width, height;
        int finalY;
        boolean isSelected = false;
        float selectionProgress = 0.0f;
        float hoverProgress = 0.0f;
        
        public Tab(ConfigTabContent content) {
            this.content = content;
        }
    }
}
