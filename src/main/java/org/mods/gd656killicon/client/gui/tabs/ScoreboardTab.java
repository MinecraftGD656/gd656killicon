package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.resources.language.I18n;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ScoreboardLoadoutConfigManager;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.PromptDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.network.packet.ScoreboardSyncPacket;
import net.minecraftforge.fml.ModList;

import java.util.function.Consumer;

public class ScoreboardTab extends ConfigTabContent {
    private GDRowRenderer headerRenderer;
    private final List<GDRowRenderer> rowRenderers = new ArrayList<>();
    private ItemStack headerIcon = Items.GOLDEN_CARROT.getDefaultInstance();
    private final List<GDRowRenderer> panelHeaderRenderers = new ArrayList<>();

    private GDButton refreshButton;
    private GDButton columnModeButton;
    private GDButton toggleOfflineButton;
    private static boolean hideOffline = false;

    private static List<ScoreboardSyncPacket.Entry> leaderboardData = new ArrayList<>();
    private static long lastRefreshTime = 0;
    private static int serverTotalCount = 0;
    private static int lastPacketOffset = -1;
    private static long lastPacketRequestId = -1L;
    private static Integer lastKnownGlobalScore = null;

    private static final int PAGE_SIZE = 20;
    private static final String GLOBAL_SCORE_OBJECTIVE = "gd656killicon.score";
    private static long requestIdSeed = 0L;

    private boolean pageRequestInFlight = false;
    private int pendingPageOffset = -1;

    private boolean refreshChecking = false;
    private long refreshCheckStartAt = 0L;
    private long refreshRequestId = -1L;
    private boolean refreshReplyReceived = false;
    private long refreshStatusUntil = 0L;
    private boolean refreshStatusLocked = false;
    
    private enum SortType {
        NAME, SCORE, KILL, DEATH, ASSIST, REVIVE, PING
    }
    private final SortType[] panelSortType = new SortType[]{SortType.SCORE, SortType.SCORE, SortType.SCORE, SortType.SCORE};
    private final boolean[] panelSortAscending = new boolean[]{false, false, false, false};
    private final double[] panelScrollY = new double[]{0, 0, 0, 0};
    private final double[] panelTargetScrollY = new double[]{0, 0, 0, 0};
    private final int[] panelX1 = new int[]{0, 0, 0, 0};
    private final int[] panelY1 = new int[]{0, 0, 0, 0};
    private final int[] panelX2 = new int[]{0, 0, 0, 0};
    private final int[] panelY2 = new int[]{0, 0, 0, 0};
    private final int[] panelContentHeight = new int[]{0, 0, 0, 0};
    private final int[] panelViewHeight = new int[]{0, 0, 0, 0};
    private int draggingPanelIndex = -1;

    private boolean isDraggingArea3 = false;
    private double lastMouseY = 0;
    private long lastFrameTime = 0;

    private int area2X1, area2Y1, area2X2, area2Y2;
    private int area3X1, area3Y1, area3X2, area3Y2;

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

    public static void updateData(List<ScoreboardSyncPacket.Entry> entries, int offset, int totalCount, long requestId, int serverLayoutColumns, String[] serverPanelTeams) {
        ScoreboardLoadoutConfigManager.clearServerSuggestions();
        if (offset <= 0) {
            leaderboardData = new ArrayList<>(entries);
        } else {
            java.util.Map<java.util.UUID, Integer> indexByUuid = new java.util.HashMap<>();
            for (int i = 0; i < leaderboardData.size(); i++) {
                indexByUuid.put(leaderboardData.get(i).uuid, i);
            }
            for (ScoreboardSyncPacket.Entry entry : entries) {
                Integer index = indexByUuid.get(entry.uuid);
                if (index != null) {
                    leaderboardData.set(index, entry);
                } else {
                    leaderboardData.add(entry);
                }
            }
        }
        serverTotalCount = Math.max(totalCount, leaderboardData.size());
        ScoreboardLoadoutConfigManager.setServerSuggestedColumns(serverLayoutColumns);
        ScoreboardLoadoutConfigManager.setServerSuggestedPanelTeams(serverPanelTeams);
        lastPacketOffset = offset;
        lastPacketRequestId = requestId;
        lastRefreshTime = System.currentTimeMillis();
    }

    private void handleHeaderClick(int panelIndex, SortType type, int button) {
        if (panelIndex < 0 || panelIndex >= 4) {
            return;
        }
        panelSortType[panelIndex] = type;
        panelSortAscending[panelIndex] = (button != 0);
    }

    @Override
    public void onTabOpen() {
        targetScrollY = 0;
        scrollY = 0;
        for (int i = 0; i < 4; i++) {
            panelSortType[i] = SortType.SCORE;
            panelSortAscending[i] = false;
            panelScrollY[i] = 0;
            panelTargetScrollY[i] = 0;
            panelContentHeight[i] = 0;
            panelViewHeight[i] = 0;
        }
        draggingPanelIndex = -1;
        if (ClientConfigManager.shouldShowScoreboardIntro()) {
            ClientConfigManager.markScoreboardIntroShown();
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.scoreboard_intro"), PromptDialog.PromptType.INFO, null);
        }
        refreshButton = null;
        columnModeButton = null;
        toggleOfflineButton = null;
        pageRequestInFlight = false;
        pendingPageOffset = -1;
        refreshChecking = false;
        refreshStatusLocked = false;
        ScoreboardLoadoutConfigManager.clearServerSuggestions();
        requestPage(0, nextRequestId());
    }

    @Override
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
        if (choiceListDialog.isVisible()) {
            return choiceListDialog.mouseClicked(mouseX, mouseY, button);
        }
        for (GDRowRenderer panelHeaderRenderer : panelHeaderRenderers) {
            if (panelHeaderRenderer != null && panelHeaderRenderer.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (refreshButton != null && refreshButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (columnModeButton != null && columnModeButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (toggleOfflineButton != null && toggleOfflineButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
            double adjustedMouseY = mouseY + scrollY3;
            
            int currentY = area3Y1;
            int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
            
            for (int i = 0; i < 10; i++) {                 if (mouseX >= area3X1 && mouseX <= area3X2 && adjustedMouseY >= currentY && adjustedMouseY <= currentY + rowHeight) {
                    GDRowRenderer renderer = area3Renderers.get(i);
                    if (renderer.mouseClicked(mouseX, adjustedMouseY, button)) {
                        return true;
                    }
                }
                currentY += (rowHeight + 1);

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
        
        int panelIndex = findPanelAt(mouseX, mouseY);
        if (panelIndex >= 0) {
            draggingPanelIndex = panelIndex;
            lastMouseY = mouseY;
            return true;
        }

        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
            isDraggingArea3 = true;
            lastMouseY = mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPanelIndex = -1;
        isDraggingArea3 = false;
        if (promptDialog.isVisible()) {
            return true;
        }
        if (choiceListDialog.isVisible()) {
            return choiceListDialog.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
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
        if (choiceListDialog.isVisible()) {
            return choiceListDialog.mouseScrolled(mouseX, mouseY, delta);
        }
        int panelIndex = findPanelAt(mouseX, mouseY);
        if (panelIndex >= 0) {
            panelTargetScrollY[panelIndex] -= delta * GuiConstants.SCROLL_AMOUNT;
            return true;
        }
        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
            targetScrollY3 -= delta * GuiConstants.SCROLL_AMOUNT;
            return true;
        }
        return false;
    }

    @Override
    protected void updateScroll(float dt, int screenHeight) {
        for (int i = 0; i < 4; i++) {
            double maxScroll = Math.max(0, panelContentHeight[i] - panelViewHeight[i]);
            panelTargetScrollY[i] = Math.max(0, Math.min(maxScroll, panelTargetScrollY[i]));
            double diff = panelTargetScrollY[i] - panelScrollY[i];
            if (Math.abs(diff) < 0.01) {
                panelScrollY[i] = panelTargetScrollY[i];
            } else {
                panelScrollY[i] += diff * SCROLL_SMOOTHING * dt;
            }
        }

        int area3ContentHeight = 10 * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        if (isNemesisExpanded) {
            java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.PlayerStat> topNemesis = 
                org.mods.gd656killicon.client.stats.ClientStatsManager.getTopNemesisPlayers(3);
            area3ContentHeight += topNemesis.size() * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        }
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
        updateAreaCoordinates(screenWidth, screenHeight);

        long currentTime = System.nanoTime();
        if (lastFrameTime == 0) lastFrameTime = currentTime;
        float dt = (currentTime - lastFrameTime) / 1_000_000_000.0f;         lastFrameTime = currentTime;
        
        if (dt > 0.1f) dt = 0.1f;
        
        if (draggingPanelIndex >= 0) {
            double diff = mouseY - lastMouseY;
            panelTargetScrollY[draggingPanelIndex] -= diff;
            lastMouseY = mouseY;
        } else if (isDraggingArea3) {
            double diff = mouseY - lastMouseY;
            targetScrollY3 -= diff;
            lastMouseY = mouseY;
        }

        updateScroll(dt, screenHeight);
        updateRefreshState();
        tryLoadNextPage();

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
            renderSingleBoardPanel(guiGraphics, mouseX, mouseY, partialTick, 0, area2X1, area2Y1, area2X2, area2Y1 + GuiConstants.ROW_HEADER_HEIGHT + 1);
        } else {
            renderBoards(guiGraphics, mouseX, mouseY, partialTick, area2X1, area2Y1, area2X2, screenHeight - GuiConstants.DEFAULT_PADDING);
        }

        renderArea3Stats(guiGraphics, mouseX, mouseY, partialTick, screenWidth, screenHeight);

        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int buttonY = screenHeight - GuiConstants.DEFAULT_PADDING - GuiConstants.ROW_HEADER_HEIGHT - 1 - GuiConstants.ROW_HEADER_HEIGHT;
        int buttonWidth = (area1Right - GuiConstants.DEFAULT_PADDING - 2) / 3;

        if (refreshButton == null) {
            refreshButton = new GDButton(area3X1, buttonY, buttonWidth, GuiConstants.ROW_HEADER_HEIGHT, Component.translatable("gd656killicon.client.gui.button.refresh"), (btn) -> {
                startRefreshCheck();
            });
        }
        refreshButton.setX(area3X1);
        refreshButton.setY(buttonY);
        refreshButton.setWidth(buttonWidth);
        refreshButton.setHeight(GuiConstants.ROW_HEADER_HEIGHT);
        refreshButton.active = !refreshChecking && !refreshStatusLocked;
        refreshButton.render(guiGraphics, mouseX, mouseY, partialTick);

        if (columnModeButton == null) {
            columnModeButton = new GDButton(area3X1 + buttonWidth + 1, buttonY, buttonWidth, GuiConstants.ROW_HEADER_HEIGHT, Component.translatable("gd656killicon.client.gui.button.scoreboard_column_mode"), (btn) -> {
                openColumnModeSelector();
            });
        }
        columnModeButton.setX(area3X1 + buttonWidth + 1);
        columnModeButton.setY(buttonY);
        columnModeButton.setWidth(buttonWidth);
        columnModeButton.setHeight(GuiConstants.ROW_HEADER_HEIGHT);
        boolean canSelectColumnMode = (minecraft.player != null && minecraft.level != null)
            && !refreshChecking
            && !refreshStatusLocked
            && !ScoreboardLoadoutConfigManager.isDisplayModeLockedToAuto();
        columnModeButton.active = canSelectColumnMode;
        columnModeButton.setTextColor(canSelectColumnMode ? GuiConstants.COLOR_WHITE : GuiConstants.COLOR_GRAY);
        columnModeButton.render(guiGraphics, mouseX, mouseY, partialTick);

        if (toggleOfflineButton == null) {
            toggleOfflineButton = new GDButton(area3X1 + 2 * (buttonWidth + 1), buttonY, buttonWidth, GuiConstants.ROW_HEADER_HEIGHT, Component.translatable("gd656killicon.client.gui.button.hide_offline"), (btn) -> {
                hideOffline = !hideOffline;
                btn.setMessage(Component.translatable(hideOffline ? "gd656killicon.client.gui.button.show_offline" : "gd656killicon.client.gui.button.hide_offline"));
                for (int i = 0; i < 4; i++) {
                    panelTargetScrollY[i] = 0;
                }
            });
        }
        toggleOfflineButton.setX(area3X1 + 2 * (buttonWidth + 1));
        toggleOfflineButton.setY(buttonY);
        toggleOfflineButton.setWidth(buttonWidth);
        toggleOfflineButton.setHeight(GuiConstants.ROW_HEADER_HEIGHT);
        toggleOfflineButton.active = !refreshChecking && !refreshStatusLocked;
        toggleOfflineButton.setMessage(Component.translatable(hideOffline ? "gd656killicon.client.gui.button.show_offline" : "gd656killicon.client.gui.button.hide_offline"));
        toggleOfflineButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void startRefreshCheck() {
        refreshChecking = true;
        refreshReplyReceived = false;
        refreshCheckStartAt = System.currentTimeMillis();
        refreshRequestId = nextRequestId();
        refreshStatusLocked = false;
        setRefreshButtonStatus(Component.translatable("gd656killicon.client.gui.scoreboard.refresh.checking"), GuiConstants.COLOR_GRAY);
        requestPage(0, refreshRequestId);
    }

    private void updateRefreshState() {
        if (refreshChecking && lastPacketRequestId == refreshRequestId && lastPacketOffset == 0) {
            refreshReplyReceived = true;
        }

        long now = System.currentTimeMillis();
        if (refreshChecking && refreshReplyReceived) {
            refreshChecking = false;
            int selfPing = getSelfPing();
            showRefreshResult(Component.translatable("gd656killicon.client.gui.scoreboard.refresh.success", selfPing), GuiConstants.COLOR_GRAY);
        }

        if (refreshChecking && now - refreshCheckStartAt >= 2000) {
            refreshChecking = false;
            if (!isConnectedToVanillaServer()) {
                showRefreshResult(Component.translatable("gd656killicon.client.gui.scoreboard.refresh.disconnected"), GuiConstants.COLOR_RED);
            } else if (!refreshReplyReceived) {
                showRefreshResult(Component.translatable("gd656killicon.client.gui.scoreboard.refresh.handshake_failed"), GuiConstants.COLOR_RED);
            }
        }

        if (refreshStatusLocked && now >= refreshStatusUntil) {
            refreshStatusLocked = false;
            refreshButton.active = true;
            refreshButton.setMessage(Component.translatable("gd656killicon.client.gui.button.refresh"));
            refreshButton.setTextColor(GuiConstants.COLOR_WHITE);
        }
    }

    private void showRefreshResult(Component message, int color) {
        refreshStatusLocked = true;
        refreshStatusUntil = System.currentTimeMillis() + 3000;
        setRefreshButtonStatus(message, color);
    }

    private void setRefreshButtonStatus(Component text, int color) {
        if (refreshButton != null) {
            refreshButton.active = false;
            refreshButton.setMessage(text);
            refreshButton.setTextColor(color);
        }
    }

    private boolean isConnectedToVanillaServer() {
        return minecraft.getConnection() != null
            && minecraft.getConnection().getConnection() != null
            && minecraft.getConnection().getConnection().isConnected();
    }

    private int getSelfPing() {
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return -1;
        }
        net.minecraft.client.multiplayer.PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        return info != null ? info.getLatency() : -1;
    }

    private static long nextRequestId() {
        requestIdSeed++;
        return requestIdSeed;
    }

    private void requestPage(int offset, long requestId) {
        if (minecraft.player == null || minecraft.getConnection() == null || !isConnectedToVanillaServer()) {
            return;
        }
        int safeOffset = Math.max(0, offset);
        int limit = resolvePreferredRequestLimit();
        pendingPageOffset = safeOffset;
        pageRequestInFlight = true;
        try {
            org.mods.gd656killicon.network.NetworkHandler.sendToServer(new org.mods.gd656killicon.network.packet.ScoreboardRequestPacket(safeOffset, limit, requestId));
        } catch (Exception ignored) {
            pageRequestInFlight = false;
            pendingPageOffset = -1;
        }
    }

    private void tryLoadNextPage() {
        if (pageRequestInFlight && lastPacketOffset == pendingPageOffset) {
            pageRequestInFlight = false;
            pendingPageOffset = -1;
        }
        if (refreshChecking || refreshStatusLocked || pageRequestInFlight) {
            return;
        }
        if (leaderboardData.size() >= serverTotalCount || serverTotalCount <= 0) {
            return;
        }
        boolean nearBottom = false;
        for (int i = 0; i < 4; i++) {
            double maxScroll = Math.max(0, panelContentHeight[i] - panelViewHeight[i]);
            if (maxScroll > 0 && panelTargetScrollY[i] >= maxScroll - 1.0) {
                nearBottom = true;
                break;
            }
        }
        boolean hasVisibleGap = hasAnyPanelVisibleGap();
        if (!nearBottom && !hasVisibleGap) {
            return;
        }
        requestPage(leaderboardData.size(), nextRequestId());
    }

    private int resolvePreferredRequestLimit() {
        int visibleRows = 0;
        int rowHeight = GuiConstants.ROW_HEADER_HEIGHT + 1;
        for (int i = 0; i < 4; i++) {
            int view = panelViewHeight[i];
            if (view > 0) {
                visibleRows += Math.max(1, (int) Math.ceil(view / (double) rowHeight));
            }
        }
        if (visibleRows <= 0) {
            return PAGE_SIZE;
        }
        int desired = Math.max(PAGE_SIZE, visibleRows + (PAGE_SIZE / 2));
        return Math.min(100, desired);
    }

    private boolean hasAnyPanelVisibleGap() {
        int rowHeight = GuiConstants.ROW_HEADER_HEIGHT + 1;
        for (int i = 0; i < 4; i++) {
            int view = panelViewHeight[i];
            if (view <= 0) {
                continue;
            }
            int visibleRows = Math.max(1, (int) Math.ceil(view / (double) rowHeight));
            int loadedRows = panelContentHeight[i] <= 0 ? 0 : (int) Math.ceil(panelContentHeight[i] / (double) rowHeight);
            if (loadedRows < visibleRows) {
                return true;
            }
        }
        return false;
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
        
        int area4Top = screenHeight - GuiConstants.DEFAULT_PADDING - GuiConstants.REGION_4_HEIGHT;
        this.area3Y2 = area4Top - GuiConstants.DEFAULT_PADDING;
    }

    private void renderArea3Stats(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight) {
        int x1 = area3X1;
        int yStart = area3Y1;
        int x2 = area3X2;
        int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;

        List<String[]> stats = new ArrayList<>();
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.total_kills"), String.valueOf(org.mods.gd656killicon.client.stats.ClientStatsManager.getTotalKills())});
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.total_deaths"), String.valueOf(org.mods.gd656killicon.client.stats.ClientStatsManager.getTotalDeaths())});
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.total_assists"), String.valueOf(org.mods.gd656killicon.client.stats.ClientStatsManager.getTotalAssists())});
        if (ModList.get().isLoaded("gd656conquest")) {
            stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.total_revives"), String.valueOf(org.mods.gd656killicon.client.stats.ClientStatsManager.getTotalRevives())});
        }
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.max_streak"), String.valueOf(org.mods.gd656killicon.client.stats.ClientStatsManager.getMaxKillStreak())});
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.max_distance"), String.format("%.1fm", org.mods.gd656killicon.client.stats.ClientStatsManager.getMaxKillDistance())});
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.total_damage"), String.format("%.0f", org.mods.gd656killicon.client.stats.ClientStatsManager.getTotalDamageDealt())});
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.nemesis"), org.mods.gd656killicon.client.stats.ClientStatsManager.getNemesis()});
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.most_killed_mob"), org.mods.gd656killicon.client.stats.ClientStatsManager.getMostKilledMob()});
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.most_killed_player"), org.mods.gd656killicon.client.stats.ClientStatsManager.getMostKilledPlayer()});
        stats.add(new String[]{" " + net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.scoreboard.stat.most_used_weapon"), org.mods.gd656killicon.client.stats.ClientStatsManager.getMostUsedWeapon()});

        guiGraphics.enableScissor(area3X1, area3Y1, area3X2, area3Y2);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -scrollY3, 0);

        int currentY = yStart;
        int visualRowIndex = 0;
        for (int i = 0; i < stats.size(); i++) {
            while (area3Renderers.size() <= i) {
                area3Renderers.add(new GDRowRenderer(x1, currentY, x2, currentY + rowHeight, GuiConstants.COLOR_BLACK, 0.3f, false));
            }
            
            GDRowRenderer renderer = area3Renderers.get(i);
            renderer.setBounds(x1, currentY, x2, currentY + rowHeight);
            
            float alpha = (visualRowIndex % 2 == 1) ? 0.15f : 0.3f;
            renderer.setBackgroundAlpha(alpha);
            
            renderer.resetColumnConfig();

            Consumer<Integer> callback = null;
            int nemesisIndex = ModList.get().isLoaded("gd656conquest") ? 7 : 6;
            int mobIndex = ModList.get().isLoaded("gd656conquest") ? 8 : 7;
            int playerIndex = ModList.get().isLoaded("gd656conquest") ? 9 : 8;
            int weaponIndex = ModList.get().isLoaded("gd656conquest") ? 10 : 9;
            if (i == nemesisIndex) callback = (btn) -> { isNemesisExpanded = !isNemesisExpanded; };
            else if (i == mobIndex) callback = (btn) -> { isMobExpanded = !isMobExpanded; };
            else if (i == playerIndex) callback = (btn) -> { isPlayerExpanded = !isPlayerExpanded; };
            else if (i == weaponIndex) callback = (btn) -> { isWeaponExpanded = !isWeaponExpanded; };

            String[] statRow = stats.get(i);
            if (callback != null) {
                renderer.addColumn(statRow[0], 70, GuiConstants.COLOR_GOLD, false, false, callback);
                renderer.addColumn(statRow[1], -1, GuiConstants.COLOR_WHITE, true, true, callback);
            } else {
                renderer.addColumn(statRow[0], 70, GuiConstants.COLOR_GOLD, false, false);
                renderer.addColumn(statRow[1], -1, GuiConstants.COLOR_WHITE, true, true);
            }
            
            renderer.render(guiGraphics, mouseX, (int)(mouseY + scrollY3), partialTick);
            currentY += (rowHeight + 1);
            visualRowIndex++;

            if (i == nemesisIndex && isNemesisExpanded) {
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

            if (i == mobIndex && isMobExpanded) {
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

            if (i == playerIndex && isPlayerExpanded) {
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

            if (i == weaponIndex && isWeaponExpanded) {
                java.util.List<org.mods.gd656killicon.client.stats.ClientStatsManager.WeaponStat> topWeapons = 
                    org.mods.gd656killicon.client.stats.ClientStatsManager.getTopUsedWeapons(3);
                
                for (int j = 0; j < topWeapons.size(); j++) {
                    org.mods.gd656killicon.client.stats.ClientStatsManager.WeaponStat stat = topWeapons.get(j);
                    
                    while (weaponDetailRenderers.size() <= j) {
                        weaponDetailRenderers.add(new GDRowRenderer(x1 + rowHeight, currentY, x2, currentY + rowHeight, GuiConstants.COLOR_BLACK, 0.15f, false));
                    }
                    
                    GDRowRenderer detailRenderer = weaponDetailRenderers.get(j);
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
        }

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void renderBoards(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int x1, int y1, int x2, int y2) {
        int columns = ScoreboardLoadoutConfigManager.getEffectiveColumns(true);
        if (columns == 2) {
            int width = x2 - x1;
            int panelWidth = (width - GuiConstants.DEFAULT_PADDING) / 2;
            int p1x1 = x1;
            int p1x2 = p1x1 + panelWidth;
            int p2x1 = p1x2 + GuiConstants.DEFAULT_PADDING;
            int p2x2 = x2;
            renderSingleBoardPanel(guiGraphics, mouseX, mouseY, partialTick, 0, p1x1, y1, p1x2, y2);
            renderSingleBoardPanel(guiGraphics, mouseX, mouseY, partialTick, 1, p2x1, y1, p2x2, y2);
            return;
        }
        if (columns == 4) {
            int width = x2 - x1;
            int height = y2 - y1;
            int panelWidth = (width - GuiConstants.DEFAULT_PADDING) / 2;
            int panelHeight = (height - GuiConstants.DEFAULT_PADDING) / 2;
            int p1x1 = x1;
            int p1x2 = p1x1 + panelWidth;
            int p2x1 = p1x2 + GuiConstants.DEFAULT_PADDING;
            int p2x2 = x2;
            int p1y1 = y1;
            int p1y2 = p1y1 + panelHeight;
            int p2y1 = p1y2 + GuiConstants.DEFAULT_PADDING;
            int p2y2 = y2;
            renderSingleBoardPanel(guiGraphics, mouseX, mouseY, partialTick, 0, p1x1, p1y1, p1x2, p1y2);
            renderSingleBoardPanel(guiGraphics, mouseX, mouseY, partialTick, 1, p2x1, p1y1, p2x2, p1y2);
            renderSingleBoardPanel(guiGraphics, mouseX, mouseY, partialTick, 2, p1x1, p2y1, p1x2, p2y2);
            renderSingleBoardPanel(guiGraphics, mouseX, mouseY, partialTick, 3, p2x1, p2y1, p2x2, p2y2);
            return;
        }
        renderSingleBoardPanel(guiGraphics, mouseX, mouseY, partialTick, 0, x1, y1, x2, y2);
    }

    private void renderSingleBoardPanel(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int panelIndex, int x1, int y1, int x2, int y2) {
        panelX1[panelIndex] = x1;
        panelY1[panelIndex] = y1;
        panelX2[panelIndex] = x2;
        panelY2[panelIndex] = y2;
        int headerY2 = y1 + GuiConstants.ROW_HEADER_HEIGHT;
        renderPanelHeader(guiGraphics, mouseX, mouseY, partialTick, panelIndex, x1, y1, x2, headerY2);
        int contentY1 = headerY2 + 1;
        int contentY2 = y2;
        if (contentY2 <= contentY1) {
            return;
        }
        List<ScoreboardSyncPacket.Entry> filtered = getPanelEntries(panelIndex);
        int contentHeight = filtered.size() * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        int viewHeight = contentY2 - contentY1;
        panelContentHeight[panelIndex] = contentHeight;
        panelViewHeight[panelIndex] = viewHeight;
        ColumnLayout layout = computeColumnLayout(x2 - x1, shouldShowDedicatedReviveColumn());
        guiGraphics.enableScissor(x1, contentY1, x2, contentY2);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -panelScrollY[panelIndex], 0);
        int visualIndex = 0;
        for (int i = 0; i < filtered.size(); i++) {
            ScoreboardSyncPacket.Entry entry = filtered.get(i);
            int rowTop = contentY1 + visualIndex * (GuiConstants.ROW_HEADER_HEIGHT + 1);
            int rowBottom = rowTop + GuiConstants.ROW_HEADER_HEIGHT;
            float actualScreenTop = rowTop - (float) panelScrollY[panelIndex];
            float actualScreenBottom = rowBottom - (float) panelScrollY[panelIndex];
            if (actualScreenBottom > contentY1 && actualScreenTop < contentY2) {
                renderRow(guiGraphics, mouseX, (int) (mouseY + panelScrollY[panelIndex]), partialTick, panelIndex, entry, visualIndex, x1, rowTop, x2, rowBottom, layout);
            }
            visualIndex++;
        }
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void renderPanelHeader(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int panelIndex, int x1, int y1, int x2, int y2) {
        while (panelHeaderRenderers.size() <= panelIndex) {
            panelHeaderRenderers.add(new GDRowRenderer(x1, y1, x2, y2, GuiConstants.COLOR_GOLD, 0.75f, true));
        }
        GDRowRenderer renderer = panelHeaderRenderers.get(panelIndex);
        renderer.setBounds(x1, y1, x2, y2);
        renderer.resetColumnConfig();
        String boundTeam = ScoreboardLoadoutConfigManager.getEffectivePanelTeamBinding(panelIndex, true);
        int teamColor = getBoundTeamColor(boundTeam);
        renderer.setBackgroundColor(teamColor == GuiConstants.COLOR_GOLD ? GuiConstants.COLOR_GOLD : (teamColor & 0xFFFFFF));
        renderer.setBackgroundAlpha(0.75f);
        boolean useReviveColumn = shouldUseReviveColumn();
        boolean showDedicatedReviveColumn = shouldShowDedicatedReviveColumn();
        ColumnLayout layout = computeColumnLayout(x2 - x1, showDedicatedReviveColumn);
        ItemStack icon = getPanelIconByTeam(boundTeam);
        renderer.addIconColumn(icon, layout.rankWidth, false, false, null);
        boolean allowTeamFilterClick = !ScoreboardLoadoutConfigManager.isDisplayModeLockedToAuto()
            && ScoreboardLoadoutConfigManager.getDisplayMode() != ScoreboardLoadoutConfigManager.DisplayMode.AUTO;
        renderer.addColoredColumn(getPanelTeamInfo(panelIndex), layout.nameWidth, false, false, allowTeamFilterClick ? (btn) -> openPanelTeamSelector(panelIndex) : null);
        renderer.addColumn(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.header.score"), layout.scoreWidth, GuiConstants.COLOR_WHITE, true, true, (btn) -> handleHeaderClick(panelIndex, SortType.SCORE, btn));
        renderer.addColumn(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.header.kill"), layout.killWidth, GuiConstants.COLOR_WHITE, false, true, (btn) -> handleHeaderClick(panelIndex, SortType.KILL, btn));
        renderer.addColumn(
            net.minecraft.client.resources.language.I18n.get(useReviveColumn
                ? "gd656killicon.client.gui.config.tab.scoreboard.header.revive"
                : "gd656killicon.client.gui.config.tab.scoreboard.header.death"),
            layout.deathWidth,
            GuiConstants.COLOR_WHITE,
            false,
            true,
            (btn) -> handleHeaderClick(panelIndex, useReviveColumn ? SortType.REVIVE : SortType.DEATH, btn)
        );
        renderer.addColumn(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.header.assist"), layout.assistWidth, GuiConstants.COLOR_WHITE, false, true, (btn) -> handleHeaderClick(panelIndex, SortType.ASSIST, btn));
        if (showDedicatedReviveColumn) {
            renderer.addColumn(
                net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.header.revive"),
                layout.reviveWidth,
                GuiConstants.COLOR_WHITE,
                false,
                true,
                (btn) -> handleHeaderClick(panelIndex, SortType.REVIVE, btn)
            );
        }
        renderer.addColoredColumn(getPingInfo(), layout.pingWidth, true, true, (btn) -> handleHeaderClick(panelIndex, SortType.PING, btn));
        renderer.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderRow(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int panelIndex, ScoreboardSyncPacket.Entry entry, int visualIndex, int x1, int y1, int x2, int y2, ColumnLayout layout) {
        int ping = resolvePing(entry);
        boolean oddRow = (visualIndex % 2 == 1);
        int rowBgColor = 0x000000;
        float alpha = oddRow ? 0.12f : 0.22f;
        boolean isTeammate = false;
        boolean isSelf = minecraft.player != null && entry.uuid.equals(minecraft.player.getUUID());
        boolean isConquestMatchRowColor = isConquestMatchScoreboardJoined();
        boolean isSoloMatch = isConquestSoloMatchScoreboardJoined();
        String selfTeamName = getSelfTeamName();
        String selfSquadLabel = getSelfSquadLabel();
        boolean isSameTeamAsSelf = !selfTeamName.isBlank() && selfTeamName.equals(entry.teamName);
        boolean isSameSquadAsSelf = isSameTeamAsSelf
            && !selfSquadLabel.isBlank()
            && selfSquadLabel.equals(entry.squadLabel);
        int themeColor = getBoundTeamColor(ScoreboardLoadoutConfigManager.getEffectivePanelTeamBinding(panelIndex, true));
        if (minecraft.player != null && minecraft.level != null) {
            net.minecraft.world.scores.Team team = minecraft.player.getTeam();
            if (team != null && team.getName().equals(entry.teamName)) {
                isTeammate = true;
            } else if (isSameTeamAsSelf) {
                isTeammate = true;
            }
            if (isSoloMatch) {
                isTeammate = false;
                isSameSquadAsSelf = false;
            }
            if (isSelf) {
                if (isSoloMatch) {
                    rowBgColor = 0xCC6666;
                    alpha = oddRow ? 0.22f : 0.28f;
                } else if (isConquestMatchRowColor) {
                    rowBgColor = 0x9ACD32;
                    alpha = oddRow ? 0.17f : 0.23f;
                } else {
                    rowBgColor = tint(themeColor, 0.20f) & 0xFFFFFF;
                    alpha = 0.30f;
                }
            } else if (isConquestMatchRowColor) {
                if (isSameSquadAsSelf) {
                    rowBgColor = 0x6B8E23;
                    alpha = oddRow ? 0.17f : 0.23f;
                } else if (isTeammate) {
                    rowBgColor = 0x1E3A8A;
                    alpha = oddRow ? 0.17f : 0.23f;
                } else {
                    rowBgColor = 0x7F1D1D;
                    alpha = oddRow ? 0.17f : 0.23f;
                }
            } else if (isTeammate) {
                rowBgColor = tint(themeColor, -0.15f) & 0xFFFFFF;
            }
        }
        if (!isConquestMatchRowColor && isTeammate) {
            alpha = oddRow ? 0.17f : 0.23f;
        }

        while (rowRenderers.size() <= visualIndex) {
            rowRenderers.add(new GDRowRenderer(x1, y1, x2, y2, rowBgColor, alpha, false));
        }
        GDRowRenderer renderer = rowRenderers.get(visualIndex);
        renderer.setBounds(x1, y1, x2, y2);
        renderer.setBackgroundColor(rowBgColor);
        renderer.setBackgroundAlpha(alpha);
        renderer.resetColumnConfig();
        boolean isOffline = !entry.online;
        boolean isSpectator = entry.online && entry.spectator;
        boolean useReviveColumn = shouldUseReviveColumn();
        boolean showDedicatedReviveColumn = shouldShowDedicatedReviveColumn();

        int rowTextColor = GuiConstants.COLOR_WHITE;
        boolean italic = false;
        if (isSelf) {
            rowTextColor = GuiConstants.COLOR_GOLD;
            if (isSpectator) {
                italic = true;
            }
        } else if (isOffline) {
            rowTextColor = GuiConstants.COLOR_DARK_GRAY;
        } else if (isSpectator) {
            rowTextColor = GuiConstants.COLOR_GRAY;
            italic = true;
        }

        renderer.addColumn(toStyledText(String.valueOf(visualIndex + 1), italic), layout.rankWidth, rowTextColor, true, true);
        
        renderer.addColumn(toStyledText(entry.lastLoginName != null ? entry.lastLoginName : entry.name, italic), layout.nameWidth, rowTextColor, false, false);
        
        renderer.addColumn(toStyledText(String.valueOf(entry.score), italic), layout.scoreWidth, rowTextColor, true, true);
        
        renderer.addColumn(toStyledText(String.valueOf(entry.kill), italic), layout.killWidth, rowTextColor, false, true);
        
        renderer.addColumn(toStyledText(String.valueOf(useReviveColumn ? entry.revive : entry.death), italic), layout.deathWidth, rowTextColor, false, true);
        
        renderer.addColumn(toStyledText(String.valueOf(entry.assist), italic), layout.assistWidth, rowTextColor, false, true);

        if (showDedicatedReviveColumn) {
            renderer.addColumn(toStyledText(String.valueOf(entry.revive), italic), layout.reviveWidth, rowTextColor, false, true);
        }
        
        renderer.addColoredColumn(formatPing(ping), layout.pingWidth, true, true);

        renderer.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private boolean isConquestMatchScoreboardJoined() {
        if (isConquestSoloMatchScoreboardJoined()) {
            return true;
        }
        if (minecraft.player == null || ScoreboardLoadoutConfigManager.getEffectiveColumns(true) != 2) {
            return false;
        }
        String leftTeam = resolveBoundTeamName(ScoreboardLoadoutConfigManager.getEffectivePanelTeamBinding(0, true));
        String rightTeam = resolveBoundTeamName(ScoreboardLoadoutConfigManager.getEffectivePanelTeamBinding(1, true));
        if (!isConquestRuntimeTeamName(leftTeam) || !isConquestRuntimeTeamName(rightTeam)) {
            return false;
        }
        String selfTeamName = getSelfTeamName();
        if (selfTeamName.isBlank()) {
            net.minecraft.world.scores.Team selfTeam = minecraft.player.getTeam();
            selfTeamName = selfTeam == null || selfTeam.getName() == null ? "" : selfTeam.getName();
        }
        if (selfTeamName.isBlank()) {
            return false;
        }
        return selfTeamName.equals(leftTeam) || selfTeamName.equals(rightTeam);
    }

    private boolean isTeamAll(String bind) {
        return ScoreboardLoadoutConfigManager.TEAM_ALL.equals(bind) || ScoreboardLoadoutConfigManager.TEAM_CONQUEST_SOLO.equals(bind);
    }

    private boolean isConquestSoloMatchScoreboardJoined() {
        if (ScoreboardLoadoutConfigManager.isServerForcingSoloMode()) {
            return true;
        }
        if (minecraft.player == null || ScoreboardLoadoutConfigManager.getEffectiveColumns(true) != 1) {
            return false;
        }
        String teamBind = ScoreboardLoadoutConfigManager.getEffectivePanelTeamBinding(0, true);
        if (!isTeamAll(teamBind)) {
            return false;
        }
        for (ScoreboardSyncPacket.Entry item : leaderboardData) {
            String normalized = item.teamName == null ? "" : item.teamName.toLowerCase(java.util.Locale.ROOT);
            if (normalized.startsWith("room_") && normalized.contains("_camp_")) {
                return true;
            }
        }
        return false;
    }

    private boolean isConquestRuntimeTeamName(String teamName) {
        String normalized = teamName == null ? "" : teamName.toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("room_") && normalized.contains("_camp_");
    }

    private String getSelfTeamName() {
        if (minecraft.player == null) {
            return "";
        }
        UUID selfUUID = minecraft.player.getUUID();
        for (ScoreboardSyncPacket.Entry item : leaderboardData) {
            if (selfUUID.equals(item.uuid)) {
                return item.teamName == null ? "" : item.teamName;
            }
        }
        return "";
    }

    private String getSelfSquadLabel() {
        if (minecraft.player == null) {
            return "";
        }
        UUID selfUUID = minecraft.player.getUUID();
        for (ScoreboardSyncPacket.Entry item : leaderboardData) {
            if (selfUUID.equals(item.uuid)) {
                return item.squadLabel == null ? "" : item.squadLabel;
            }
        }
        return "";
    }

    private boolean shouldUseReviveColumn() {
        return isConquestMatchScoreboardJoined();
    }

    private boolean shouldShowDedicatedReviveColumn() {
        return ModList.get().isLoaded("gd656conquest") && !shouldUseReviveColumn();
    }

    private int findPanelAt(double mouseX, double mouseY) {
        for (int i = 0; i < 4; i++) {
            if (mouseX >= panelX1[i] && mouseX <= panelX2[i] && mouseY >= panelY1[i] && mouseY <= panelY2[i]) {
                return i;
            }
        }
        return -1;
    }

    private ColumnLayout computeColumnLayout(int panelWidth, boolean showDedicatedReviveColumn) {
        int rankWidth = 17;
        int nameMin = 25;
        int scoreMin = 15;
        int otherMin = 10;
        int statColumnCount = showDedicatedReviveColumn ? 5 : 4;
        int minTotal = rankWidth + nameMin + scoreMin + otherMin * statColumnCount;
        int extra = Math.max(0, panelWidth - minTotal);
        int nameWidth = nameMin + extra / 2;
        int scoreWidth = scoreMin + extra / 4;
        int remaining = extra - (nameWidth - nameMin) - (scoreWidth - scoreMin);
        int statExtraBase = remaining / statColumnCount;
        int statRemainder = remaining - statExtraBase * statColumnCount;
        int killWidth = otherMin + statExtraBase;
        int deathWidth = otherMin + statExtraBase;
        int assistWidth = otherMin + statExtraBase;
        int reviveWidth = showDedicatedReviveColumn ? otherMin + statExtraBase : 0;
        int pingWidth = otherMin + statExtraBase + statRemainder;
        return new ColumnLayout(rankWidth, nameWidth, scoreWidth, killWidth, deathWidth, assistWidth, reviveWidth, pingWidth);
    }

    private int tint(int argbColor, float delta) {
        int a = (argbColor >>> 24) & 0xFF;
        int r = (argbColor >>> 16) & 0xFF;
        int g = (argbColor >>> 8) & 0xFF;
        int b = argbColor & 0xFF;
        r = clampColor((int) (r + 255 * delta));
        g = clampColor((int) (g + 255 * delta));
        b = clampColor((int) (b + 255 * delta));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private record ColumnLayout(int rankWidth, int nameWidth, int scoreWidth, int killWidth, int deathWidth, int assistWidth, int reviveWidth, int pingWidth) {}

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

    private int resolvePing(ScoreboardSyncPacket.Entry entry) {
        if (minecraft.getConnection() != null) {
            net.minecraft.client.multiplayer.PlayerInfo info = minecraft.getConnection().getPlayerInfo(entry.uuid);
            if (info != null) {
                return info.getLatency();
            }
        }
        return entry.online ? entry.ping : -1;
    }

    private String toStyledText(String text, boolean italic) {
        if (!italic || text == null || text.isEmpty()) {
            return text;
        }
        return "§o" + text + "§r";
    }

    private List<ScoreboardSyncPacket.Entry> getPanelEntries(int panelIndex) {
        List<ScoreboardSyncPacket.Entry> list = new ArrayList<>();
        String bind = ScoreboardLoadoutConfigManager.getEffectivePanelTeamBinding(panelIndex, true);
        String boundTeamName = resolveBoundTeamName(bind);
        for (ScoreboardSyncPacket.Entry entry : leaderboardData) {
            if (hideOffline && !entry.online) {
                continue;
            }
            if (!isTeamAll(bind) && !boundTeamName.equals(entry.teamName)) {
                continue;
            }
            list.add(entry);
        }
        SortType sortType = panelSortType[panelIndex];
        boolean ascending = panelSortAscending[panelIndex];
        list.sort((a, b) -> {
            int result = switch (sortType) {
                case NAME -> {
                    String nameA = a.lastLoginName != null ? a.lastLoginName : a.name;
                    String nameB = b.lastLoginName != null ? b.lastLoginName : b.name;
                    yield nameA.compareToIgnoreCase(nameB);
                }
                case SCORE -> Integer.compare(a.score, b.score);
                case KILL -> Integer.compare(a.kill, b.kill);
                case DEATH -> Integer.compare(a.death, b.death);
                case ASSIST -> Integer.compare(a.assist, b.assist);
                case REVIVE -> Integer.compare(a.revive, b.revive);
                case PING -> Integer.compare(resolvePing(a), resolvePing(b));
            };
            if (result == 0) {
                result = a.uuid.compareTo(b.uuid);
            }
            return ascending ? result : -result;
        });
        return list;
    }

    private List<GDTextRenderer.ColoredText> getPanelTeamInfo(int panelIndex) {
        List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
        String bind = ScoreboardLoadoutConfigManager.getEffectivePanelTeamBinding(panelIndex, true);
        String boundTeamName = resolveBoundTeamName(bind);
        if (minecraft.level == null) {
            texts.add(new GDTextRenderer.ColoredText(" " + I18n.get("gd656killicon.client.gui.config.tab.scoreboard.no_team"), GuiConstants.COLOR_GRAY));
            return texts;
        }
        if (isConquestSoloMatchScoreboardJoined()) {
            String title = I18n.exists("gd656conquest.scoreboard.solo_all") ? I18n.get("gd656conquest.scoreboard.solo_all") : "单人枪神竞技";
            texts.add(new GDTextRenderer.ColoredText(" " + title + " ", GuiConstants.COLOR_SOFT_RED));
            int onlineMembers = 0;
            for (ScoreboardSyncPacket.Entry entry : leaderboardData) {
                if (entry.online && (!hideOffline || entry.online)) {
                    onlineMembers++;
                }
            }
            texts.add(new GDTextRenderer.ColoredText(String.valueOf(onlineMembers), GuiConstants.COLOR_GOLD));
            return texts;
        }
        net.minecraft.world.scores.PlayerTeam team;
        if (isTeamAll(bind)) {
            team = minecraft.player == null ? null : minecraft.level.getScoreboard().getPlayersTeam(minecraft.player.getScoreboardName());
        } else {
            team = minecraft.level.getScoreboard().getPlayerTeam(boundTeamName);
        }
        if (team == null) {
            if (isConquestRuntimeTeamName(boundTeamName)) {
                int totalMembers = 0;
                int onlineMembers = 0;
                for (ScoreboardSyncPacket.Entry entry : leaderboardData) {
                    if (!boundTeamName.equals(entry.teamName)) {
                        continue;
                    }
                    totalMembers++;
                    if (entry.online) {
                        onlineMembers++;
                    }
                }
                if (totalMembers > 0) {
                    int configuredMaxPlayers = resolveBoundTeamConfiguredMax(bind);
                    int color = resolveRuntimeTeamFallbackColor(boundTeamName);
                    texts.add(new GDTextRenderer.ColoredText(" " + resolveRuntimeTeamDisplayName(boundTeamName) + " ", color));
                    texts.add(new GDTextRenderer.ColoredText(String.valueOf(onlineMembers), GuiConstants.COLOR_GOLD));
                    texts.add(new GDTextRenderer.ColoredText("/" + (configuredMaxPlayers > 0 ? configuredMaxPlayers : totalMembers), GuiConstants.COLOR_GRAY));
                    return texts;
                }
            }
            texts.add(new GDTextRenderer.ColoredText(" " + I18n.get("gd656killicon.client.gui.config.tab.scoreboard.no_team"), GuiConstants.COLOR_GRAY));
            return texts;
        }
        int color = convertChatFormattingToColor(team.getColor());
        texts.add(new GDTextRenderer.ColoredText(" " + team.getDisplayName().getString() + " ", color));
        int totalMembers = team.getPlayers().size();
        int onlineMembers = 0;
        if (minecraft.getConnection() != null) {
            for (String playerName : team.getPlayers()) {
                if (minecraft.getConnection().getPlayerInfo(playerName) != null) {
                    onlineMembers++;
                }
            }
        }
        int configuredMaxPlayers = resolveBoundTeamConfiguredMax(bind);
        texts.add(new GDTextRenderer.ColoredText(String.valueOf(onlineMembers), GuiConstants.COLOR_GOLD));
        texts.add(new GDTextRenderer.ColoredText("/" + (configuredMaxPlayers > 0 ? configuredMaxPlayers : totalMembers), GuiConstants.COLOR_GRAY));
        return texts;
    }

    private int getBoundTeamColor(String bind) {
        if (minecraft.level == null || bind == null || isTeamAll(bind)) {
            if (isConquestSoloMatchScoreboardJoined()) {
                return GuiConstants.COLOR_SOFT_RED;
            }
            return GuiConstants.COLOR_GOLD;
        }
        String boundTeamName = resolveBoundTeamName(bind);
        net.minecraft.world.scores.PlayerTeam team = minecraft.level.getScoreboard().getPlayerTeam(boundTeamName);
        if (team == null) {
            if (isConquestRuntimeTeamName(boundTeamName)) {
                return resolveRuntimeTeamFallbackColor(boundTeamName);
            }
            return GuiConstants.COLOR_GOLD;
        }
        return convertChatFormattingToColor(team.getColor());
    }

    private String resolveRuntimeTeamDisplayName(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return I18n.get("gd656killicon.client.gui.config.tab.scoreboard.no_team");
        }
        String normalized = teamName.toLowerCase(java.util.Locale.ROOT);
        if (normalized.endsWith("_camp_a")) {
            return "Camp A";
        }
        if (normalized.endsWith("_camp_b")) {
            return "Camp B";
        }
        return teamName;
    }

    private int resolveRuntimeTeamFallbackColor(String teamName) {
        String normalized = teamName == null ? "" : teamName.toLowerCase(java.util.Locale.ROOT);
        if (normalized.endsWith("_camp_a")) {
            return GuiConstants.COLOR_SKY_BLUE;
        }
        if (normalized.endsWith("_camp_b")) {
            return GuiConstants.COLOR_RED;
        }
        return GuiConstants.COLOR_GOLD;
    }

    private String resolveBoundTeamName(String bind) {
        if (bind == null || bind.isBlank() || isTeamAll(bind)) {
            return bind;
        }
        int separator = bind.indexOf('|');
        if (separator <= 0) {
            return bind;
        }
        return bind.substring(0, separator);
    }

    private int resolveBoundTeamConfiguredMax(String bind) {
        if (bind == null || bind.isBlank()) {
            return 0;
        }
        int separator = bind.indexOf("|cap=");
        if (separator < 0 || separator + 5 >= bind.length()) {
            return 0;
        }
        try {
            return Integer.parseInt(bind.substring(separator + 5));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void openColumnModeSelector() {
        if (ScoreboardLoadoutConfigManager.isDisplayModeLockedToAuto()) {
            return;
        }
        List<org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry.Choice> options = new ArrayList<>();
        options.add(new org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry.Choice("auto", I18n.get("gd656killicon.client.gui.scoreboard.column_mode.auto")));
        options.add(new org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry.Choice("single", I18n.get("gd656killicon.client.gui.scoreboard.column_mode.single")));
        options.add(new org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry.Choice("double", I18n.get("gd656killicon.client.gui.scoreboard.column_mode.double")));
        options.add(new org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry.Choice("quad", I18n.get("gd656killicon.client.gui.scoreboard.column_mode.quad")));
        String selected = switch (ScoreboardLoadoutConfigManager.getDisplayMode()) {
            case AUTO -> "auto";
            case SINGLE -> "single";
            case DOUBLE -> "double";
            case QUAD -> "quad";
        };
        choiceListDialog.show(
            selected,
            I18n.get("gd656killicon.client.gui.button.scoreboard_column_mode"),
            options,
            value -> {
                if ("double".equals(value)) {
                    ScoreboardLoadoutConfigManager.setDisplayMode(ScoreboardLoadoutConfigManager.DisplayMode.DOUBLE);
                } else if ("quad".equals(value)) {
                    ScoreboardLoadoutConfigManager.setDisplayMode(ScoreboardLoadoutConfigManager.DisplayMode.QUAD);
                } else if ("single".equals(value)) {
                    ScoreboardLoadoutConfigManager.setDisplayMode(ScoreboardLoadoutConfigManager.DisplayMode.SINGLE);
                } else {
                    ScoreboardLoadoutConfigManager.setDisplayMode(ScoreboardLoadoutConfigManager.DisplayMode.AUTO);
                }
                panelTargetScrollY[0] = 0;
                panelTargetScrollY[1] = 0;
                panelTargetScrollY[2] = 0;
                panelTargetScrollY[3] = 0;
            },
            null
        );
    }

    private void openPanelTeamSelector(int panelIndex) {
        if (minecraft.level == null || ScoreboardLoadoutConfigManager.getDisplayMode() == ScoreboardLoadoutConfigManager.DisplayMode.AUTO) {
            return;
        }
        List<org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry.Choice> options = new ArrayList<>();
        options.add(new org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry.Choice(
            ScoreboardLoadoutConfigManager.TEAM_ALL,
            I18n.get("gd656killicon.client.gui.scoreboard.team_filter.all")
        ));
        for (net.minecraft.world.scores.PlayerTeam team : minecraft.level.getScoreboard().getPlayerTeams()) {
            String label = (team.getColor() != null ? team.getColor().toString() : "") + team.getDisplayName().getString() + "§r";
            options.add(new org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry.Choice(team.getName(), label));
        }
        choiceListDialog.show(
            ScoreboardLoadoutConfigManager.getEffectivePanelTeamBinding(panelIndex, true),
            I18n.get("gd656killicon.client.gui.scoreboard.team_filter.title"),
            options,
            value -> {
                ScoreboardLoadoutConfigManager.setPanelTeamBinding(panelIndex, value);
                panelTargetScrollY[panelIndex] = 0;
            },
            null
        );
    }

    private ItemStack getPanelIconByTeam(String bind) {
        if (minecraft.level == null || bind == null || isTeamAll(bind)) {
            return Items.GOLDEN_CARROT.getDefaultInstance();
        }
        net.minecraft.world.scores.PlayerTeam team = minecraft.level.getScoreboard().getPlayerTeam(bind);
        if (team == null) {
            return Items.GOLDEN_CARROT.getDefaultInstance();
        }
        return switch (team.getColor()) {
            case RED, DARK_RED -> Items.REDSTONE.getDefaultInstance();
            case BLUE, DARK_BLUE, AQUA, DARK_AQUA -> Items.LAPIS_LAZULI.getDefaultInstance();
            case GREEN, DARK_GREEN -> Items.EMERALD.getDefaultInstance();
            case YELLOW, GOLD -> Items.GOLD_INGOT.getDefaultInstance();
            case WHITE, GRAY -> Items.QUARTZ.getDefaultInstance();
            case BLACK -> Items.COAL.getDefaultInstance();
            case LIGHT_PURPLE, DARK_PURPLE -> Items.AMETHYST_SHARD.getDefaultInstance();
            default -> Items.GOLDEN_CARROT.getDefaultInstance();
        };
    }

    private int convertChatFormattingToColor(net.minecraft.ChatFormatting color) {
        if (color == null) {
            return GuiConstants.COLOR_GOLD;
        }
        return switch (color) {
            case BLACK -> 0xFF111111;
            case DARK_BLUE -> 0xFF0000AA;
            case DARK_GREEN -> 0xFF00AA00;
            case DARK_AQUA -> 0xFF00AAAA;
            case DARK_RED -> 0xFFAA0000;
            case DARK_PURPLE -> 0xFFAA00AA;
            case GOLD -> 0xFFFFAA00;
            case GRAY -> 0xFFAAAAAA;
            case DARK_GRAY -> 0xFF555555;
            case BLUE -> 0xFF5555FF;
            case GREEN -> 0xFF55FF55;
            case AQUA -> 0xFF55FFFF;
            case RED -> 0xFFFF5555;
            case LIGHT_PURPLE -> 0xFFFF55FF;
            case YELLOW -> 0xFFFFFF55;
            case WHITE -> 0xFFFFFFFF;
            default -> GuiConstants.COLOR_GOLD;
        };
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

        int r, g, b;
        float factor = Math.min(1.0f, ping / 200.0f);
        if (factor <= 0.5f) {
            r = (int)(factor * 2 * 255);
            g = 200;             b = 0;
        } else {
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
        List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
        texts.add(new GDTextRenderer.ColoredText(net.minecraft.client.resources.language.I18n.get("gd656killicon.client.gui.config.tab.scoreboard.your_score"), GuiConstants.COLOR_GRAY));

        Integer scoreObj = getSelfGlobalScore();
        if (scoreObj == null) {
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

    private Integer getSelfGlobalScore() {
        if (minecraft.player == null) {
            return null;
        }
        net.minecraft.world.scores.Scoreboard scoreboard = minecraft.player.getScoreboard();
        if (scoreboard != null) {
            net.minecraft.world.scores.Objective objective = scoreboard.getObjective(GLOBAL_SCORE_OBJECTIVE);
            if (objective != null) {
                try {
                    Object scoreAccess = scoreboard.getOrCreatePlayerScore(minecraft.player.getScoreboardName(), objective);
                    try {
                        int score = ((Number) scoreAccess.getClass().getMethod("get").invoke(scoreAccess)).intValue();
                        lastKnownGlobalScore = score;
                        return score;
                    } catch (Exception ignored) {
                    }
                    try {
                        int score = ((Number) scoreAccess.getClass().getMethod("getScore").invoke(scoreAccess)).intValue();
                        lastKnownGlobalScore = score;
                        return score;
                    } catch (Exception ignored) {
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (isConquestMatchScoreboardJoined()) {
            return lastKnownGlobalScore;
        }
        UUID selfUUID = minecraft.player.getUUID();
        for (ScoreboardSyncPacket.Entry entry : leaderboardData) {
            if (entry.uuid.equals(selfUUID)) {
                lastKnownGlobalScore = entry.score;
                return entry.score;
            }
        }
        return lastKnownGlobalScore;
    }

    @Override
    protected void renderSideButtons(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight) {
    }
}
