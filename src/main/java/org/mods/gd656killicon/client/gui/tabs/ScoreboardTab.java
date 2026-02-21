package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.network.packet.ScoreboardSyncPacket;

import java.util.function.Consumer;

public class ScoreboardTab extends ConfigTabContent {
    private GDRowRenderer headerRenderer;
    private final List<GDRowRenderer> rowRenderers = new ArrayList<>();
    private ItemStack headerIcon = Items.GOLDEN_CARROT.getDefaultInstance();
    private final Random random = new Random();

    // 按钮相关
    private GDButton refreshButton;
    private GDButton toggleOfflineButton;
    private boolean hideOffline = false;

    // 数据相关
    private static List<ScoreboardSyncPacket.Entry> leaderboardData = new ArrayList<>();
    private static long lastRefreshTime = 0;
    private static final long REFRESH_INTERVAL_MS = 1000;
    
    // 排序相关
    private enum SortType {
        NAME, SCORE, KILL, DEATH, ASSIST, PING
    }
    private static SortType currentSortType = SortType.SCORE;
    private static boolean isAscending = false;

    // 滚动相关

    private boolean isDragging = false;
    private double lastMouseY = 0;
    private long lastFrameTime = 0;

    // 区域坐标 (用于判定鼠标是否在区域内)
    private int area2X1, area2Y1, area2X2, area2Y2;
    private int area3X1, area3Y1, area3X2, area3Y2;

    // 提示信息相关
    private GDTextRenderer outOfGameHintRenderer;
    private final List<GDRowRenderer> area3Renderers = new ArrayList<>();
    private final List<GDRowRenderer> weaponDetailRenderers = new ArrayList<>();
    private final List<GDRowRenderer> mobDetailRenderers = new ArrayList<>();
    private final List<GDRowRenderer> playerDetailRenderers = new ArrayList<>();
    private final List<GDRowRenderer> nemesisDetailRenderers = new ArrayList<>();
    private boolean isWeaponExpanded = false;
    private boolean isMobExpanded = false;
    private boolean isPlayerExpanded = false;
    private boolean isNemesisExpanded = false;
    private double scrollY3 = 0;
    private double targetScrollY3 = 0;

    public ScoreboardTab(Minecraft minecraft) {
        super(minecraft, "gd656killicon.client.gui.config.tab.scoreboard");
    }

    public static void updateData(List<ScoreboardSyncPacket.Entry> entries) {
        leaderboardData = entries;
        sortData();
        lastRefreshTime = System.currentTimeMillis();
    }

    private static void sortData() {
        if (leaderboardData == null || leaderboardData.isEmpty()) return;
        
        leaderboardData.sort((a, b) -> {
            int result = 0;
            switch (currentSortType) {
                case NAME:
                    String nameA = a.lastLoginName != null ? a.lastLoginName : a.name;
                    String nameB = b.lastLoginName != null ? b.lastLoginName : b.name;
                    result = nameA.compareToIgnoreCase(nameB);
                    break;
                case SCORE:
                    result = Integer.compare(a.score, b.score);
                    break;
                case KILL:
                    result = Integer.compare(a.kill, b.kill);
                    break;
                case DEATH:
                    result = Integer.compare(a.death, b.death);
                    break;
                case ASSIST:
                    result = Integer.compare(a.assist, b.assist);
                    break;
                case PING:
                    result = Integer.compare(a.ping, b.ping);
                    break;
            }
            
            // 如果主要排序项的值相同，则使用 UUID 作为二级排序键，确保排序结果的唯一性和确定性
            if (result == 0) {
                result = a.uuid.compareTo(b.uuid);
            }
            
            return isAscending ? result : -result;
        });
    }

    private void handleHeaderClick(SortType type, int button) {
        currentSortType = type;
        isAscending = (button != 0); // 左键(0)倒序，右键(1)正序
        sortData();
    }

    @Override
    public void onTabOpen() {
        currentSortType = SortType.SCORE;
        isAscending = false;
        targetScrollY = 0;
        scrollY = 0;
        sortData();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (headerRenderer != null && headerRenderer.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 检查自定义按钮点击
        if (refreshButton != null && refreshButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (toggleOfflineButton != null && toggleOfflineButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 检查第三区域（左侧统计区域）点击
        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
            double adjustedMouseY = mouseY + scrollY3;
            
            int currentY = area3Y1;
            int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
            
            for (int i = 0; i < 10; i++) { // 10 个主统计项
                // 检查主行点击
                if (mouseX >= area3X1 && mouseX <= area3X2 && adjustedMouseY >= currentY && adjustedMouseY <= currentY + rowHeight) {
                    GDRowRenderer renderer = area3Renderers.get(i);
                    if (renderer.mouseClicked(mouseX, adjustedMouseY, button)) {
                        return true;
                    }
                }
                currentY += (rowHeight + 1);

                // 如果是“宿敌玩家”展开状态
                if (i == 6 && isNemesisExpanded) {
                    java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.PlayerStat> topNemesis = 
                        org.mods.gd656killicon.client.stats.ClientStatsManager.getTopNemesisPlayers(3);
                    
                    for (int j = 0; j < topNemesis.size(); j++) {
                        if (mouseX >= area3X1 + rowHeight && mouseX <= area3X2 && adjustedMouseY >= currentY && adjustedMouseY <= currentY + rowHeight) {
                            if (j < nemesisDetailRenderers.size()) {
                                if (nemesisDetailRenderers.get(j).mouseClicked(mouseX, adjustedMouseY, button)) {
                                    return true;
                                }
                            }
                        }
                        currentY += (rowHeight + 1);
                    }
                }

                // 如果是“最常击杀生物”展开状态
                if (i == 7 && isMobExpanded) {
                    java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.MobStat> topMobs = 
                        org.mods.gd656killicon.client.stats.ClientStatsManager.getTopKilledMobs(3);
                    
                    for (int j = 0; j < topMobs.size(); j++) {
                        if (mouseX >= area3X1 + rowHeight && mouseX <= area3X2 && adjustedMouseY >= currentY && adjustedMouseY <= currentY + rowHeight) {
                            if (j < mobDetailRenderers.size()) {
                                if (mobDetailRenderers.get(j).mouseClicked(mouseX, adjustedMouseY, button)) {
                                    return true;
                                }
                            }
                        }
                        currentY += (rowHeight + 1);
                    }
                }

                // 如果是“最常击杀玩家”展开状态
                if (i == 8 && isPlayerExpanded) {
                    java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.PlayerStat> topPlayers = 
                        org.mods.gd656killicon.client.stats.ClientStatsManager.getTopKilledPlayers(3);
                    
                    for (int j = 0; j < topPlayers.size(); j++) {
                        if (mouseX >= area3X1 + rowHeight && mouseX <= area3X2 && adjustedMouseY >= currentY && adjustedMouseY <= currentY + rowHeight) {
                            if (j < playerDetailRenderers.size()) {
                                if (playerDetailRenderers.get(j).mouseClicked(mouseX, adjustedMouseY, button)) {
                                    return true;
                                }
                            }
                        }
                        currentY += (rowHeight + 1);
                    }
                }

                // 如果是“最常用武器”展开状态
                if (i == 9 && isWeaponExpanded) {
                    java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.WeaponStat> topWeapons = 
                        org.mods.gd656killicon.client.stats.ClientStatsManager.getTopUsedWeapons(3);
                    
                    for (int j = 0; j < topWeapons.size(); j++) {
                        if (mouseX >= area3X1 + rowHeight && mouseX <= area3X2 && adjustedMouseY >= currentY && adjustedMouseY <= currentY + rowHeight) {
                            if (j < weaponDetailRenderers.size()) {
                                if (weaponDetailRenderers.get(j).mouseClicked(mouseX, adjustedMouseY, button)) {
                                    return true;
                                }
                            }
                        }
                        currentY += (rowHeight + 1);
                    }
                }
            }
        }
        
        // 拖拽开始判定（仅限主显示区）
        int area1Right = (minecraft.getWindow().getGuiScaledWidth() - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int x1 = area1Right + GuiConstants.DEFAULT_PADDING;
        int y1 = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
        int x2 = minecraft.getWindow().getGuiScaledWidth() - GuiConstants.DEFAULT_PADDING;
        int y2 = minecraft.getWindow().getGuiScaledHeight() - GuiConstants.DEFAULT_PADDING;
        
        if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
            isDragging = true;
            lastMouseY = mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 仅当鼠标在第二区域（主榜单区域）内时才允许滚动
        if (mouseX >= area2X1 && mouseX <= area2X2 && mouseY >= area2Y1 && mouseY <= area2Y2) {
            targetScrollY -= delta * GuiConstants.SCROLL_AMOUNT;
            return true;
        }
        // 仅当鼠标在第三区域（统计区域）内时才允许滚动
        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
            targetScrollY3 -= delta * GuiConstants.SCROLL_AMOUNT;
            return true;
        }
        return false;
    }

    @Override
    protected void updateScroll(float dt, int screenHeight) {
        // 限制主榜单滚动范围
        int visibleCount = 0;
        if (hideOffline) {
            for (ScoreboardSyncPacket.Entry entry : leaderboardData) {
                if (entry.ping >= 0) visibleCount++;
            }
        } else {
            visibleCount = leaderboardData.size();
        }
        int contentHeight = visibleCount * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        this.totalContentHeight = contentHeight; // 同步给父类

        // 精确计算 View Height
        int topOffset = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
        int listHeaderHeight = GuiConstants.ROW_HEADER_HEIGHT + 1;
        int bottomPadding = GuiConstants.DEFAULT_PADDING;
        int viewHeight = screenHeight - (topOffset + listHeaderHeight + bottomPadding);
        
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY));

        double diff = targetScrollY - scrollY;
        if (Math.abs(diff) < 0.01) {
            scrollY = targetScrollY;
        } else {
            scrollY += diff * SCROLL_SMOOTHING * dt;
        }

        // 限制第三区域滚动范围
        int area3ContentHeight = 10 * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        if (isMobExpanded) {
            java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.MobStat> topMobs = 
                org.mods.gd656killicon.client.stats.ClientStatsManager.getTopKilledMobs(3);
            area3ContentHeight += topMobs.size() * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        }
        if (isPlayerExpanded) {
            java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.PlayerStat> topPlayers = 
                org.mods.gd656killicon.client.stats.ClientStatsManager.getTopKilledPlayers(3);
            area3ContentHeight += topPlayers.size() * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        }
        if (isWeaponExpanded) {
            java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.WeaponStat> topWeapons = 
                org.mods.gd656killicon.client.stats.ClientStatsManager.getTopUsedWeapons(3);
            area3ContentHeight += topWeapons.size() * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        }
        int area3ViewHeight = area3Y2 - area3Y1;
        double maxScroll3 = Math.max(0, area3ContentHeight - area3ViewHeight);
        targetScrollY3 = Math.max(0, Math.min(maxScroll3, targetScrollY3));

        double diff3 = targetScrollY3 - scrollY3;
        if (Math.abs(diff3) < 0.01) {
            scrollY3 = targetScrollY3;
        } else {
            scrollY3 += diff3 * SCROLL_SMOOTHING * dt;
        }
    }

    @Override
    protected void renderContent(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        // 更新布局坐标
        updateAreaCoordinates(screenWidth, screenHeight);

        // 定时刷新逻辑
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime > REFRESH_INTERVAL_MS) {
            if (minecraft.player != null && minecraft.getConnection() != null) {
                org.mods.gd656killicon.network.NetworkHandler.sendToServer(new org.mods.gd656killicon.network.packet.ScoreboardRequestPacket());
                lastRefreshTime = now;
            }
        }

        // 更新滚动
        // Use real time delta for physics, not render partial ticks
        long currentTime = System.nanoTime();
        if (lastFrameTime == 0) lastFrameTime = currentTime;
        float dt = (currentTime - lastFrameTime) / 1_000_000_000.0f; // Convert ns to seconds
        lastFrameTime = currentTime;
        
        // Cap dt to avoid large jumps on lag spikes
        if (dt > 0.1f) dt = 0.1f;
        
        // 处理拖拽逻辑 (在调用 updateScroll 之前)
        if (isDragging) {
            double diff = mouseY - lastMouseY;
            targetScrollY -= diff;
            lastMouseY = mouseY;
        }

        updateScroll(dt, screenHeight);

        // 1. 渲染固定表头
        renderHeader(guiGraphics, mouseX, mouseY, partialTick, area2X1, area2Y1, area2X2, area2Y1 + GuiConstants.ROW_HEADER_HEIGHT);

        // 2. 渲染列表内容或提示
        if (minecraft.player == null || minecraft.level == null) {
            int centerY = (area2Y1 + screenHeight - GuiConstants.DEFAULT_PADDING) / 2 - 5;
            List<GDTextRenderer.ColoredText> hintTexts = new ArrayList<>();
            hintTexts.add(new GDTextRenderer.ColoredText(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.open_in_game"), GuiConstants.COLOR_GRAY));
            
            if (outOfGameHintRenderer == null) {
                outOfGameHintRenderer = new GDTextRenderer(hintTexts, area2X1, centerY, area2X2, centerY + 10, 1.0f, false);
            } else {
                 outOfGameHintRenderer.setX1(area2X1);
                 outOfGameHintRenderer.setY1(centerY);
                 outOfGameHintRenderer.setX2(area2X2);
                 outOfGameHintRenderer.setColoredTexts(hintTexts);
             }
             outOfGameHintRenderer.setCentered(true);
             outOfGameHintRenderer.render(guiGraphics, partialTick);
        } else {
            renderList(guiGraphics, mouseX, mouseY, partialTick, area2X1, area2Y1 + GuiConstants.ROW_HEADER_HEIGHT + 1, area2X2, screenHeight - GuiConstants.DEFAULT_PADDING);
        }

        // 3. 渲染第三区域（左侧第二控制区域）的统计表单（游戏内外均显示）
        renderArea3Stats(guiGraphics, mouseX, mouseY, partialTick, screenWidth, screenHeight);

        // 4. 渲染自定义按钮（位于第三区域下方，保存并退出按钮上方）
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int buttonY = screenHeight - GuiConstants.DEFAULT_PADDING - GuiConstants.ROW_HEADER_HEIGHT - 1 - GuiConstants.ROW_HEADER_HEIGHT;
        int buttonWidth = (area1Right - GuiConstants.DEFAULT_PADDING - 1) / 2;

        if (refreshButton == null) {
            refreshButton = new GDButton(area3X1, buttonY, buttonWidth, GuiConstants.ROW_HEADER_HEIGHT, Component.translatable("gd656killicon.client.gui.button.refresh"), (btn) -> {
                if (minecraft.player != null && minecraft.getConnection() != null) {
                    org.mods.gd656killicon.network.NetworkHandler.sendToServer(new org.mods.gd656killicon.network.packet.ScoreboardRequestPacket());
                    lastRefreshTime = System.currentTimeMillis();
                }
            });
        }
        refreshButton.setX(area3X1);
        refreshButton.setY(buttonY);
        refreshButton.setWidth(buttonWidth);
        refreshButton.setHeight(GuiConstants.ROW_HEADER_HEIGHT);
        refreshButton.render(guiGraphics, mouseX, mouseY, partialTick);

        if (toggleOfflineButton == null) {
            toggleOfflineButton = new GDButton(area3X1 + buttonWidth + 1, buttonY, buttonWidth, GuiConstants.ROW_HEADER_HEIGHT, Component.translatable("gd656killicon.client.gui.button.hide_offline"), (btn) -> {
                hideOffline = !hideOffline;
                btn.setMessage(Component.translatable(hideOffline ? "gd656killicon.client.gui.button.show_offline" : "gd656killicon.client.gui.button.hide_offline"));
                targetScrollY = 0; // 重置滚动位置以防止显示错乱
            });
        }
        toggleOfflineButton.setX(area3X1 + buttonWidth + 1);
        toggleOfflineButton.setY(buttonY);
        toggleOfflineButton.setWidth(buttonWidth);
        toggleOfflineButton.setHeight(GuiConstants.ROW_HEADER_HEIGHT);
        toggleOfflineButton.setMessage(Component.translatable(hideOffline ? "gd656killicon.client.gui.button.show_offline" : "gd656killicon.client.gui.button.hide_offline"));
        toggleOfflineButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateAreaCoordinates(int screenWidth, int screenHeight) {
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        this.area2X1 = area1Right + GuiConstants.DEFAULT_PADDING;
        this.area2Y1 = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
        this.area2X2 = screenWidth - GuiConstants.DEFAULT_PADDING;
        this.area2Y2 = screenHeight - GuiConstants.DEFAULT_PADDING;

        this.area3X1 = GuiConstants.DEFAULT_PADDING;
        this.area3Y1 = this.area1Bottom + GuiConstants.DEFAULT_PADDING;
        this.area3X2 = area1Right;
        
        // 区域4的高度及偏移计算，必须与 ConfigScreenHeader 中的逻辑保持一致
        int area4Top = screenHeight - GuiConstants.DEFAULT_PADDING - GuiConstants.REGION_4_HEIGHT;
        this.area3Y2 = area4Top - GuiConstants.DEFAULT_PADDING;
    }

    private void renderArea3Stats(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight) {
        int x1 = area3X1;
        int yStart = area3Y1;
        int x2 = area3X2;
        int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;

        // 准备统计数据
        String[][] stats = {
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.total_kills"), String.valueOf(org.mods.gd656killicon.client.stats.ClientStatsManager.getTotalKills())},
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.total_deaths"), String.valueOf(org.mods.gd656killicon.client.stats.ClientStatsManager.getTotalDeaths())},
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.total_assists"), String.valueOf(org.mods.gd656killicon.client.stats.ClientStatsManager.getTotalAssists())},
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.max_streak"), String.valueOf(org.mods.gd656killicon.client.stats.ClientStatsManager.getMaxKillStreak())},
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.max_distance"), String.format("%.1fm", org.mods.gd656killicon.client.stats.ClientStatsManager.getMaxKillDistance())},
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.total_damage"), String.format("%.0f", org.mods.gd656killicon.client.stats.ClientStatsManager.getTotalDamageDealt())},
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.nemesis"), org.mods.gd656killicon.client.stats.ClientStatsManager.getNemesis()},
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.most_killed_mob"), org.mods.gd656killicon.client.stats.ClientStatsManager.getMostKilledMob()},
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.most_killed_player"), org.mods.gd656killicon.client.stats.ClientStatsManager.getMostKilledPlayer()},
            {net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.most_used_weapon"), org.mods.gd656killicon.client.stats.ClientStatsManager.getMostUsedWeapon()}
        };

        // 开启裁剪以支持滚动 (使用逻辑坐标，GuiGraphics 会自动处理缩放)
        guiGraphics.enableScissor(area3X1, area3Y1, area3X2, area3Y2);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -scrollY3, 0);

        int currentY = yStart;
        int visualRowIndex = 0;
        for (int i = 0; i < stats.length; i++) {
            // 确保渲染器列表足够长
            while (area3Renderers.size() <= i) {
                area3Renderers.add(new GDRowRenderer(x1, currentY, x2, currentY + rowHeight, GuiConstants.COLOR_BLACK, 0.3f, false));
            }
            
            GDRowRenderer renderer = area3Renderers.get(i);
            // 这里 bounds 设置为相对于 yStart 的偏移，或者是包含滚动的绝对坐标，
            // 但因为我们用了 translate，这里的坐标设置需要小心。
            // 修正：setBounds 应该反映它在 translate 后的逻辑位置，或者我们手动处理偏移。
            renderer.setBounds(x1, currentY, x2, currentY + rowHeight);
            
            // 使用 visualRowIndex 确保透明度交替正确
            float alpha = (visualRowIndex % 2 == 1) ? 0.15f : 0.3f;
            renderer.setBackgroundAlpha(alpha);
            
            renderer.resetColumnConfig();

            // 为特定行添加点击回调以支持展开和金色条动画
            Consumer<Integer> callback = null;
            if (i == 6) callback = (btn) -> { isNemesisExpanded = !isNemesisExpanded; };
            else if (i == 7) callback = (btn) -> { isMobExpanded = !isMobExpanded; };
            else if (i == 8) callback = (btn) -> { isPlayerExpanded = !isPlayerExpanded; };
            else if (i == 9) callback = (btn) -> { isWeaponExpanded = !isWeaponExpanded; };

            if (callback != null) {
                renderer.addColumn(stats[i][0], 70, GuiConstants.COLOR_GOLD, false, false, callback);
                renderer.addColumn(stats[i][1], -1, GuiConstants.COLOR_WHITE, true, true, callback);
            } else {
                renderer.addColumn(stats[i][0], 70, GuiConstants.COLOR_GOLD, false, false);
                renderer.addColumn(stats[i][1], -1, GuiConstants.COLOR_WHITE, true, true);
            }
            
            // 渲染时传入的鼠标 Y 坐标需要加上 scrollY3 以抵消 translate
            renderer.render(guiGraphics, mouseX, (int)(mouseY + scrollY3), partialTick);
            currentY += (rowHeight + 1);
            visualRowIndex++;

            // 如果是“宿敌玩家”行且已展开，则渲染详情行
            if (i == 6 && isNemesisExpanded) {
                java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.PlayerStat> topNemesis = 
                    org.mods.gd656killicon.client.stats.ClientStatsManager.getTopNemesisPlayers(3);
                
                for (int j = 0; j < topNemesis.size(); j++) {
                    org.mods.gd656killicon.client.stats.ClientStatsManager.PlayerStat stat = topNemesis.get(j);
                    
                    while (nemesisDetailRenderers.size() <= j) {
                        nemesisDetailRenderers.add(new GDRowRenderer(x1 + rowHeight, currentY, x2, currentY + rowHeight, GuiConstants.COLOR_BLACK, 0.15f, false));
                    }
                    
                    GDRowRenderer detailRenderer = nemesisDetailRenderers.get(j);
                    detailRenderer.setBounds(x1 + rowHeight, currentY, x2, currentY + rowHeight);
                    
                    float detailAlpha = (visualRowIndex % 2 == 1) ? 0.15f : 0.3f;
                    detailRenderer.setBackgroundAlpha(detailAlpha);
                    
                    detailRenderer.resetColumnConfig();
                    detailRenderer.addColumn(" " + stat.name, -1, GuiConstants.COLOR_WHITE, false, false);
                    detailRenderer.addColumn(String.valueOf(stat.count), 40, GuiConstants.COLOR_GOLD, true, true);
                    
                    detailRenderer.render(guiGraphics, mouseX, (int)(mouseY + scrollY3), partialTick);
                    currentY += (rowHeight + 1);
                    visualRowIndex++;
                }
            }

            // 如果是“最常击杀生物”行且已展开，则渲染详情行
            if (i == 7 && isMobExpanded) {
                java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.MobStat> topMobs = 
                    org.mods.gd656killicon.client.stats.ClientStatsManager.getTopKilledMobs(3);
                
                for (int j = 0; j < topMobs.size(); j++) {
                    org.mods.gd656killicon.client.stats.ClientStatsManager.MobStat stat = topMobs.get(j);
                    
                    while (mobDetailRenderers.size() <= j) {
                        mobDetailRenderers.add(new GDRowRenderer(x1 + rowHeight, currentY, x2, currentY + rowHeight, GuiConstants.COLOR_BLACK, 0.15f, false));
                    }
                    
                    GDRowRenderer detailRenderer = mobDetailRenderers.get(j);
                    detailRenderer.setBounds(x1 + rowHeight, currentY, x2, currentY + rowHeight);
                    
                    float detailAlpha = (visualRowIndex % 2 == 1) ? 0.15f : 0.3f;
                    detailRenderer.setBackgroundAlpha(detailAlpha);
                    
                    detailRenderer.resetColumnConfig();
                    detailRenderer.addColumn(" " + stat.name, -1, GuiConstants.COLOR_WHITE, false, false);
                    detailRenderer.addColumn(String.valueOf(stat.count), 40, GuiConstants.COLOR_GOLD, true, true);
                    
                    detailRenderer.render(guiGraphics, mouseX, (int)(mouseY + scrollY3), partialTick);
                    currentY += (rowHeight + 1);
                    visualRowIndex++;
                }
            }

            // 如果是“最常击杀玩家”行且已展开，则渲染详情行
            if (i == 8 && isPlayerExpanded) {
                java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.PlayerStat> topPlayers = 
                    org.mods.gd656killicon.client.stats.ClientStatsManager.getTopKilledPlayers(3);
                
                for (int j = 0; j < topPlayers.size(); j++) {
                    org.mods.gd656killicon.client.stats.ClientStatsManager.PlayerStat stat = topPlayers.get(j);
                    
                    while (playerDetailRenderers.size() <= j) {
                        playerDetailRenderers.add(new GDRowRenderer(x1 + rowHeight, currentY, x2, currentY + rowHeight, GuiConstants.COLOR_BLACK, 0.15f, false));
                    }
                    
                    GDRowRenderer detailRenderer = playerDetailRenderers.get(j);
                    detailRenderer.setBounds(x1 + rowHeight, currentY, x2, currentY + rowHeight);
                    
                    float detailAlpha = (visualRowIndex % 2 == 1) ? 0.15f : 0.3f;
                    detailRenderer.setBackgroundAlpha(detailAlpha);
                    
                    detailRenderer.resetColumnConfig();
                    detailRenderer.addColumn(" " + stat.name, -1, GuiConstants.COLOR_WHITE, false, false);
                    detailRenderer.addColumn(String.valueOf(stat.count), 40, GuiConstants.COLOR_GOLD, true, true);
                    
                    detailRenderer.render(guiGraphics, mouseX, (int)(mouseY + scrollY3), partialTick);
                    currentY += (rowHeight + 1);
                    visualRowIndex++;
                }
            }

            // 如果是“最常用武器”行且已展开，则渲染详情行
            if (i == 9 && isWeaponExpanded) {
                java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.WeaponStat> topWeapons = 
                    org.mods.gd656killicon.client.stats.ClientStatsManager.getTopUsedWeapons(3);
                
                for (int j = 0; j < topWeapons.size(); j++) {
                    org.mods.gd656killicon.client.stats.ClientStatsManager.WeaponStat stat = topWeapons.get(j);
                    
                    // 确保详情渲染器列表足够长
                    while (weaponDetailRenderers.size() <= j) {
                        weaponDetailRenderers.add(new GDRowRenderer(x1 + rowHeight, currentY, x2, currentY + rowHeight, GuiConstants.COLOR_BLACK, 0.15f, false));
                    }
                    
                    GDRowRenderer detailRenderer = weaponDetailRenderers.get(j);
                    detailRenderer.setBounds(x1 + rowHeight, currentY, x2, currentY + rowHeight);
                    
                    // 详情行也参与透明度交替
                    float detailAlpha = (visualRowIndex % 2 == 1) ? 0.15f : 0.3f;
                    detailRenderer.setBackgroundAlpha(detailAlpha);
                    
                    detailRenderer.resetColumnConfig();
                    detailRenderer.addColumn(" " + stat.name, -1, GuiConstants.COLOR_WHITE, false, false);
                    
                    // 格式化为击杀数
                    detailRenderer.addColumn(String.valueOf(stat.count), 40, GuiConstants.COLOR_GOLD, true, true);
                    
                    detailRenderer.render(guiGraphics, mouseX, (int)(mouseY + scrollY3), partialTick);
                    currentY += (rowHeight + 1);
                    visualRowIndex++;
                }
            }
        }

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void renderList(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int x1, int headerY2, int x2, int screenHeight) {
        // 3. 渲染可滚动内容区域
        int contentY1 = headerY2; 
        int contentY2 = screenHeight;
        
        if (contentY2 > contentY1) {
            // 彻底修复文字消失问题：Scissor 必须在 Pose 变换之外应用，
            // 且 Scissor 的坐标是逻辑坐标，GuiGraphics 会自动处理物理像素转换。
            guiGraphics.enableScissor(x1, contentY1, x2, contentY2);
            
            guiGraphics.pose().pushPose();
            // 使用 translate 实现亚像素级别平滑平移
            guiGraphics.pose().translate(0, -scrollY, 0);
            
            int visualIndex = 0;
            for (int i = 0; i < leaderboardData.size(); i++) {
                ScoreboardSyncPacket.Entry entry = leaderboardData.get(i);
                
                // 如果开启了隐藏离线玩家且该玩家已离线（ping < 0），则跳过渲染
                if (hideOffline && entry.ping < 0) {
                    continue;
                }

                // 这里的 y 坐标传入原始不带偏移的值，偏移由 translate 处理
                int rowTop = contentY1 + visualIndex * (GuiConstants.ROW_HEADER_HEIGHT + 1);
                int rowBottom = rowTop + GuiConstants.ROW_HEADER_HEIGHT;
                
                // 计算实际在屏幕上的渲染位置用于 Scissor 优化判定
                float actualScreenTop = rowTop - (float)scrollY;
                float actualScreenBottom = rowBottom - (float)scrollY;
                
                // 仅渲染可见区域内的行
                if (actualScreenBottom > contentY1 && actualScreenTop < contentY2) {
                    // 注意：由于我们在 PoseStack 中做了 translate，GDTextRenderer 内部如果也开启 Scissor 
                    // 它的坐标会因为 PoseStack 的平移而发生偏移，导致与屏幕实际 Scissor 区域错位。
                    // 因此我们需要确保 renderRow 内部的组件意识到这种变换。
                    renderRow(guiGraphics, mouseX, (int)(mouseY + scrollY), partialTick, i, visualIndex, x1, rowTop, x2, rowBottom);
                }
                visualIndex++;
            }
            
            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();
        }
    }

    private void renderHeader(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int x1, int y1, int x2, int y2) {
        if (headerRenderer == null) {
            headerRenderer = new GDRowRenderer(x1, y1, x2, y2, GuiConstants.COLOR_GOLD, 0.75f, true);
        } else {
            headerRenderer.setBounds(x1, y1, x2, y2);
        }

        headerRenderer.resetColumnConfig();
        
        // 第一列：原版贴图图标 (17x17)
        headerRenderer.addIconColumn(headerIcon, 17, true, true, (btn) -> {
            if (btn == 0) {
                List<net.minecraft.world.item.Item> items = BuiltInRegistries.ITEM.stream()
                    .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
                    .filter(item -> item != Items.AIR)
                    .toList();
                if (!items.isEmpty()) {
                    headerIcon = items.get(random.nextInt(items.size())).getDefaultInstance();
                }
            } else if (btn == 1) {
                headerIcon = Items.GOLDEN_CARROT.getDefaultInstance();
            }
        });

        headerRenderer.addColoredColumn(getTeamInfo(), -1, false, false, (btn) -> handleHeaderClick(SortType.NAME, btn));
        headerRenderer.addColumn(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.header.score"), 50, GuiConstants.COLOR_WHITE, true, true, (btn) -> handleHeaderClick(SortType.SCORE, btn));
        headerRenderer.addColumn(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.header.kill"), 25, GuiConstants.COLOR_WHITE, false, true, (btn) -> handleHeaderClick(SortType.KILL, btn));
        headerRenderer.addColumn(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.header.death"), 25, GuiConstants.COLOR_WHITE, false, true, (btn) -> handleHeaderClick(SortType.DEATH, btn));
        headerRenderer.addColumn(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.header.assist"), 25, GuiConstants.COLOR_WHITE, false, true, (btn) -> handleHeaderClick(SortType.ASSIST, btn));
        headerRenderer.addColoredColumn(getPingInfo(), 40, true, true, (btn) -> handleHeaderClick(SortType.PING, btn));

        headerRenderer.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderRow(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int index, int visualIndex, int x1, int y1, int x2, int y2) {
        ScoreboardSyncPacket.Entry entry = leaderboardData.get(index);
        
        // 确定行背景颜色和透明度
        int rowBgColor = 0x000000; // 默认黑色
        float alpha = (visualIndex % 2 == 1) ? 0.10f : 0.30f; // 双数行（索引为奇数）透明度 0.10，单数行 0.30
        
        if (minecraft.player != null) {
            if (entry.uuid.equals(minecraft.player.getUUID())) {
                rowBgColor = GuiConstants.COLOR_GOLD_ORANGE & 0xFFFFFF; // 提取 RGB，去掉 Alpha
                alpha = 0.30f; // 自己行固定使用 0.30 透明度，不受单双数影响
            } else {
                net.minecraft.world.scores.PlayerTeam team = minecraft.level.getScoreboard().getPlayersTeam(minecraft.player.getScoreboardName());
                if (team != null && team.getPlayers().contains(entry.name)) {
                    rowBgColor = GuiConstants.COLOR_DARK_GOLD_ORANGE & 0xFFFFFF; // 提取 RGB，去掉 Alpha
                }
            }
        }

        // 获取或创建行渲染器
        while (rowRenderers.size() <= index) {
            rowRenderers.add(new GDRowRenderer(x1, y1, x2, y2, rowBgColor, alpha, false));
        }
        GDRowRenderer renderer = rowRenderers.get(index);
        renderer.setBounds(x1, y1, x2, y2);
        renderer.setBackgroundColor(rowBgColor); // 动态更新背景色
        renderer.setBackgroundAlpha(alpha); // 动态更新透明度，确保自己行的透明度正确
        renderer.resetColumnConfig(); // 必须调用 resetColumnConfig 而不是 clearColumns，以保留动画状态并允许重新配置列内容

        boolean isOffline = entry.ping < 0;
        int rowTextColor = isOffline ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_WHITE;

        // 1. 排名 (17x17, 居中)
        renderer.addColumn(String.valueOf(index + 1), 17, rowTextColor, true, true);
        
        // 2. 玩家名 (左对齐)
        renderer.addColumn(entry.lastLoginName != null ? entry.lastLoginName : entry.name, -1, rowTextColor, false, false);
        
        // 3. 分数 (居中)
        renderer.addColumn(String.valueOf(entry.score), 50, rowTextColor, true, true);
        
        // 4. K (居中)
        renderer.addColumn(String.valueOf(entry.kill), 25, rowTextColor, false, true);
        
        // 5. D (居中)
        renderer.addColumn(String.valueOf(entry.death), 25, rowTextColor, false, true);
        
        // 6. A (居中)
        renderer.addColumn(String.valueOf(entry.assist), 25, rowTextColor, false, true);
        
        // 7. 延迟 (居中, 颜色逻辑同表头)
        renderer.addColoredColumn(formatPing(entry.ping), 40, true, true);

        renderer.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private List<GDTextRenderer.ColoredText> formatPing(int ping) {
        List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
        if (ping < 0) {
            texts.add(new GDTextRenderer.ColoredText("--", GuiConstants.COLOR_DARK_GRAY));
            return texts;
        }

        int r, g, b;
        float factor = Math.min(1.0f, ping / 200.0f);
        if (factor <= 0.5f) {
            r = (int)(factor * 2 * 255);
            g = 200;
            b = 0;
        } else {
            r = 255;
            g = (int)((1.0f - factor) * 2 * 200);
            b = 0;
        }
        int pingColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        texts.add(new GDTextRenderer.ColoredText(ping + "ms", pingColor));
        return texts;
    }

    private List<GDTextRenderer.ColoredText> getTeamInfo() {
        List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
        // 表头第二列字幕前加一个空格
        String prefix = " ";
        
        if (minecraft.player == null || minecraft.level == null) {
            texts.add(new GDTextRenderer.ColoredText(prefix + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.no_team"), GuiConstants.COLOR_GRAY));
            return texts;
        }

        net.minecraft.world.scores.PlayerTeam team = minecraft.level.getScoreboard().getPlayersTeam(minecraft.player.getScoreboardName());
        
        if (team == null) {
            texts.add(new GDTextRenderer.ColoredText(prefix + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.no_team"), GuiConstants.COLOR_GRAY));
        } else {
            // 获取队伍名称
            String teamName = team.getDisplayName().getString();
            texts.add(new GDTextRenderer.ColoredText(prefix + teamName + " ", GuiConstants.COLOR_WHITE));
            
            // 计算在线人数和总人数
            int totalMembers = team.getPlayers().size();
            int onlineMembers = 0;
            if (minecraft.getConnection() != null) {
                for (String playerName : team.getPlayers()) {
                    if (minecraft.getConnection().getPlayerInfo(playerName) != null) {
                        onlineMembers++;
                    }
                }
            }
            
            texts.add(new GDTextRenderer.ColoredText(String.valueOf(onlineMembers), GuiConstants.COLOR_GOLD)); // x: 在线人数
            texts.add(new GDTextRenderer.ColoredText("/" + totalMembers, GuiConstants.COLOR_GRAY)); // /y: 总人数
        }
        return texts;
    }

    private List<GDTextRenderer.ColoredText> getPingInfo() {
        List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            texts.add(new GDTextRenderer.ColoredText("--", GuiConstants.COLOR_DARK_GRAY));
            return texts;
        }

        int ping = 0;
        net.minecraft.client.multiplayer.PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        if (info != null) {
            ping = info.getLatency();
        } else {
            texts.add(new GDTextRenderer.ColoredText("--", GuiConstants.COLOR_DARK_GRAY));
            return texts;
        }

        // 动态计算延迟颜色：0(深绿) -> 200(深红)
        // 深绿: 0xFF006400, 深红: 0xFF8B0000
        int r, g, b;
        float factor = Math.min(1.0f, ping / 200.0f);
        if (factor <= 0.5f) {
            // 绿 -> 黄 (0-100)
            r = (int)(factor * 2 * 255);
            g = 200; // 保持较深的绿色调
            b = 0;
        } else {
            // 黄 -> 红 (100-200)
            r = 255;
            g = (int)((1.0f - factor) * 2 * 200);
            b = 0;
        }
        int pingColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        
        texts.add(new GDTextRenderer.ColoredText(ping + "ms", pingColor));
        return texts;
    }

    @Override
    protected void updateSubtitle(int x1, int y1, int x2) {
        // 分数榜单特有逻辑：显示分数，关闭自动换行
        List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
        texts.add(new GDTextRenderer.ColoredText(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.your_score"), GuiConstants.COLOR_GRAY));
        
        Integer scoreObj = null;
        if (minecraft.player != null) {
            UUID selfUUID = minecraft.player.getUUID();
            // 优先从已同步的排行榜数据中查找自己的分数
            for (ScoreboardSyncPacket.Entry entry : leaderboardData) {
                if (entry.uuid.equals(selfUUID)) {
                    scoreObj = entry.score;
                    break;
                }
            }
        }
        
        if (scoreObj == null) {
            // 对于所有种类的网络获取数据失败都需要显示深灰色的"--"
            texts.add(new GDTextRenderer.ColoredText("--", GuiConstants.COLOR_DARK_GRAY));
        } else {
            texts.add(new GDTextRenderer.ColoredText(String.valueOf(scoreObj), GuiConstants.COLOR_GOLD));
        }

        if (subtitleRenderer == null) {
            subtitleRenderer = new GDTextRenderer(texts, x1, y1, x2, y1 + 10, 1.0f, false);
        } else {
            subtitleRenderer.setX1(x1);
            subtitleRenderer.setY1(y1);
            subtitleRenderer.setX2(x2);
            subtitleRenderer.setColoredTexts(texts);
        }
    }

    @Override
    protected void renderSideButtons(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight) {
        // Override to do nothing, effectively hiding Reset and Cancel buttons for this tab
    }
}
