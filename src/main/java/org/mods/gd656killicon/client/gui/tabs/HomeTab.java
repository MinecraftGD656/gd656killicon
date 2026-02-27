package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraftforge.fml.ModList;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;

import net.minecraft.resources.ResourceLocation;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HomeTab extends ConfigTabContent {
    private final List<ModStatus> modStatuses = new ArrayList<>();
    private final List<GDRowRenderer> area3Renderers = new ArrayList<>();

    
    private int area3X1, area3Y1, area3X2, area3Y2;
    
    
    private double scrollY3 = 0;
    private double targetScrollY3 = 0;
    private boolean isDragging3 = false;
    private double lastMouseY3 = 0;
    
    
    private long lastFrameTime = 0;

    
    private static final ResourceLocation ICON_NORMAL = new ResourceLocation("gd656killicon", "icon/gd656killicon_icon.png");
    private static final ResourceLocation ICON_RARE = new ResourceLocation("gd656killicon", "icon/gd656killicon_656de_shuai_zhao.png");
    private ResourceLocation currentIcon = ICON_NORMAL;
    private int versionColor = GuiConstants.COLOR_WHITE;
    private GDRowRenderer linksRowRenderer; 
    private final List<GDRowRenderer.Column> linkHoverColumns = new ArrayList<>();
    
    

    public HomeTab(Minecraft minecraft) {
        super(minecraft, "gd656killicon.client.gui.config.tab.home");
        initModStatuses();
    }

    private void initModStatuses() {
        modStatuses.add(new ModStatus("gd656killicon.client.gui.hometab.mod.tacz.name", "tacz", "https://www.mcmod.cn/class/14980.html", "gd656killicon.client.gui.hometab.mod.tacz.desc"));
        modStatuses.add(new ModStatus("gd656killicon.client.gui.hometab.mod.sbw.name", "superbwarfare", "https://www.mcmod.cn/class/18845.html", "gd656killicon.client.gui.hometab.mod.sbw.desc"));
        modStatuses.add(new ModStatus("gd656killicon.client.gui.hometab.mod.ywzj.name", "ywzj_vehicle", "https://www.mcmod.cn/class/24495.html", "gd656killicon.client.gui.hometab.mod.ywzj.desc"));
        modStatuses.add(new ModStatus("gd656killicon.client.gui.hometab.mod.ia.name", "immersive_aircraft", "https://www.mcmod.cn/class/8527.html", "gd656killicon.client.gui.hometab.mod.ia.desc"));
        modStatuses.add(new ModStatus("gd656killicon.client.gui.hometab.mod.spotting.name", "spotting", "https://www.curseforge.com/minecraft/mc-mods/spotting", "gd656killicon.client.gui.hometab.mod.spotting.desc"));
        modStatuses.add(new ModStatus("gd656killicon.client.gui.hometab.mod.lr.name", "lrtactical", "https://curseforge.com/minecraft/mc-mods/tacz-lesraisins-tactical-equipements", "gd656killicon.client.gui.hometab.mod.lr.desc"));
    }

    @Override
    public void onTabOpen() {
        super.onTabOpen();
        
        
        area3Renderers.clear();
        scrollY3 = 0;
        targetScrollY3 = 0;
        
        
        if (Math.random() < 0.01) {
            currentIcon = ICON_RARE;
        } else {
            currentIcon = ICON_NORMAL;
        }
        
        
        String version = GuiConstants.MOD_VERSION;
        if (version.endsWith("Alpha")) {
            versionColor = GuiConstants.COLOR_RED;
        } else if (version.endsWith("Beta")) {
            versionColor = GuiConstants.COLOR_GOLD_ORANGE;
        } else if (version.endsWith("Release")) {
            versionColor = GuiConstants.COLOR_GREEN;
        } else {
            versionColor = GuiConstants.COLOR_WHITE;
        }
        
        
        
        
        
        
        
        linksRowRenderer = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.0f, false);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        
        updateAreaCoordinates(screenWidth, screenHeight);
        
        long now = System.nanoTime();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = (now - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = now;
        if (dt > 0.1f) dt = 0.1f;
        
        
        if (isDragging3) {
            double diff = mouseY - lastMouseY3;
            targetScrollY3 -= diff;
            lastMouseY3 = mouseY;
        }
        
        
        if (isDragging) {
            double diff = mouseY - lastMouseY;
            targetScrollY -= diff;
            lastMouseY = mouseY;
        }

        updateScroll(dt, screenHeight);

        
        renderArea3(guiGraphics, mouseX, mouseY, partialTick);

        
        
        
        
        
        
        
        
        if (configRows.isEmpty()) {
            
            int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
            int contentX = area1Right + GuiConstants.DEFAULT_PADDING;
            int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
            int contentWidth = screenWidth - contentX - GuiConstants.DEFAULT_PADDING;
            int contentHeight = screenHeight - contentY - GuiConstants.DEFAULT_PADDING;
            
            
            int centerX = contentX + contentWidth / 2;
            int centerY = contentY + contentHeight / 2;
            
            
            guiGraphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

            
            
            int linksRowHeight = 2 * GuiConstants.ROW_HEADER_HEIGHT + 1;
            int linksY2 = contentY + contentHeight;
            int linksY1 = linksY2 - linksRowHeight;
            
            
            if (linksRowRenderer == null) linksRowRenderer = new GDRowRenderer(contentX, linksY1, contentX + contentWidth, linksY2, GuiConstants.COLOR_BLACK, 0.0f, false);
            linksRowRenderer.setX1(contentX);
            linksRowRenderer.setY1(linksY1);
            linksRowRenderer.setX2(contentX + contentWidth);
            linksRowRenderer.setY2(linksY2);
            
            linksRowRenderer.resetColumnConfig();
            
            String bilibiliText = Component.translatable("gd656killicon.client.gui.hometab.link.bilibili").getString();
            Consumer<Integer> bilibiliClick = (btn) -> Util.getPlatform().openUri(URI.create("https://space.bilibili.com/516946949"));
            linksRowRenderer.addColumn(bilibiliText, -1, GuiConstants.COLOR_GOLD, true, true, bilibiliClick);
            linksRowRenderer.setColumnHoverReplacement(0, List.of(prepareHoverColumn(0, bilibiliText, 0xFFFB7299, true, true, bilibiliClick)));
            
            String modrinthText = Component.translatable("gd656killicon.client.gui.hometab.link.modrinth").getString();
            Consumer<Integer> modrinthClick = (btn) -> Util.getPlatform().openUri(URI.create("https://modrinth.com/project/dWe4hPBb"));
            linksRowRenderer.addColumn(modrinthText, -1, GuiConstants.COLOR_GOLD, false, true, modrinthClick);
            linksRowRenderer.setColumnHoverReplacement(1, List.of(prepareHoverColumn(1, modrinthText, 0xFF1bd96a, false, true, modrinthClick)));
            
            String curseforgeText = Component.translatable("gd656killicon.client.gui.hometab.link.curseforge").getString();
            Consumer<Integer> curseforgeClick = (btn) -> Util.getPlatform().openUri(URI.create("https://www.curseforge.com/minecraft/mc-mods/gd656killicon"));
            linksRowRenderer.addColumn(curseforgeText, -1, GuiConstants.COLOR_GOLD, true, true, curseforgeClick);
            linksRowRenderer.setColumnHoverReplacement(2, List.of(prepareHoverColumn(2, curseforgeText, 0xFFeb622b, true, true, curseforgeClick)));

            String mcmodText = Component.translatable("gd656killicon.client.gui.hometab.link.mcmod").getString();
            Consumer<Integer> mcmodClick = (btn) -> Util.getPlatform().openUri(URI.create("https://www.mcmod.cn/class/21672.html"));
            linksRowRenderer.addColumn(mcmodText, -1, GuiConstants.COLOR_GOLD, false, true, mcmodClick);
            linksRowRenderer.setColumnHoverReplacement(3, List.of(prepareHoverColumn(3, mcmodText, 0xFF86c155, false, true, mcmodClick)));

            String websiteText = Component.translatable("gd656killicon.client.gui.hometab.link.website").getString();
            Consumer<Integer> websiteClick = (btn) -> Util.getPlatform().openUri(URI.create("https://flna.top/"));
            linksRowRenderer.addColumn(websiteText, -1, GuiConstants.COLOR_GOLD, true, true, websiteClick);
            linksRowRenderer.setColumnHoverReplacement(4, List.of(prepareHoverColumn(4, websiteText, GuiConstants.COLOR_GOLD, true, true, websiteClick)));

            String qqgroupText = Component.translatable("gd656killicon.client.gui.hometab.link.qqgroup").getString();
            Consumer<Integer> qqgroupClick = (btn) -> Util.getPlatform().openUri(URI.create("https://qm.qq.com/cgi-bin/qm/qr?k=eRC17xsb4HOIgEf53befoTLrWTlVVe0_&jump_from=webapi&authKey=8ruGOkg+eFZt3Y10+in17XkFovA/yTeY4edwAvm4B073f/uMru1qlNeFZl6oFyiv"));
            linksRowRenderer.addColumn(qqgroupText, -1, GuiConstants.COLOR_GOLD, false, true, qqgroupClick);
            linksRowRenderer.setColumnHoverReplacement(5, List.of(prepareHoverColumn(5, qqgroupText, GuiConstants.COLOR_WHITE, false, true, qqgroupClick)));
            
            linksRowRenderer.render(guiGraphics, mouseX, mouseY, partialTick);

            
            
            int remainingHeight = contentHeight - linksRowHeight;
            int upperCenterY = contentY + remainingHeight / 2;
            
            
            String titleText = Component.translatable("gd656killicon.client.gui.hometab.title").getString();
            float titleScale = 2.0f;
            int titleWidth = (int)(minecraft.font.width(titleText) * titleScale);
            int titleHeight = (int)(9 * titleScale); 
            
            String versionText = GuiConstants.MOD_VERSION;
            float versionScale = 1.0f;
            int versionWidth = minecraft.font.width(versionText);
            int versionHeight = 9;
            
            
            
            
            int textGap = 2;
            int totalTextHeight = titleHeight + textGap + versionHeight;
            
            int iconSize = totalTextHeight;
            
            
            
            
            
            
            
            int maxTextWidth = Math.max(titleWidth, versionWidth);
            int totalGroupWidth = iconSize + maxTextWidth; 
            
            
            int groupX = centerX - totalGroupWidth / 2;
            int groupY = upperCenterY - totalTextHeight / 2;
            
            
            int iconX = groupX;
            int iconY = groupY;
            
            
            if (currentIcon == null) currentIcon = ICON_NORMAL; 
            
            guiGraphics.blit(currentIcon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            
            
            
            int textStartX = iconX + iconSize;
            
            
            int textVisibleRight = contentX + contentWidth;
            
            
            int titleY = iconY; 
            
            
            GDTextRenderer titleRenderer = new GDTextRenderer(titleText, textStartX, titleY, textVisibleRight, titleY + titleHeight, titleScale, GuiConstants.COLOR_WHITE, false);
            titleRenderer.render(guiGraphics, partialTick, false); 
            
            
            int versionY = titleY + titleHeight + textGap; 
            GDTextRenderer versionRenderer = new GDTextRenderer(versionText, textStartX, versionY, textVisibleRight, versionY + versionHeight, versionScale, versionColor, false);
            versionRenderer.render(guiGraphics, partialTick, false); 
            
            guiGraphics.disableScissor();
        } else {
             
             
             int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
             int contentX = area1Right + GuiConstants.DEFAULT_PADDING;
             int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
             int contentWidth = screenWidth - contentX - GuiConstants.DEFAULT_PADDING;
             int contentHeight = screenHeight - contentY - GuiConstants.DEFAULT_PADDING;
             
             guiGraphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
             guiGraphics.pose().pushPose();
             guiGraphics.pose().translate(0, -scrollY, 0);
             
             for (GDRowRenderer row : configRows) {
                 row.render(guiGraphics, mouseX, (int)(mouseY + scrollY), partialTick);
             }
             
             guiGraphics.pose().popPose();
             guiGraphics.disableScissor();
        }
    }
    
    @Override
    protected void updateScroll(float dt, int screenHeight) {
        
        if (!configRows.isEmpty()) {
            int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
            int viewHeight = screenHeight - contentY - GuiConstants.DEFAULT_PADDING;
            double maxScroll = Math.max(0, totalContentHeight - viewHeight);
            targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY));
            
            double diff = targetScrollY - scrollY;
            if (Math.abs(diff) < 0.1) scrollY = targetScrollY;
            else scrollY += diff * GuiConstants.SCROLL_SMOOTHING * dt;
        }

        
        int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int gap = 1;
        int totalContentHeight3 = 0;
        
        for (ModStatus status : modStatuses) {
            totalContentHeight3 += (rowHeight + gap);
            if (status.expanded) {
                totalContentHeight3 += (rowHeight + gap);
            }
        }
        
        int viewHeight3 = area3Y2 - area3Y1;
        
        double maxScroll3 = Math.max(0, totalContentHeight3 - viewHeight3);
        targetScrollY3 = Math.max(0, Math.min(maxScroll3, targetScrollY3));
        
        double diff3 = targetScrollY3 - scrollY3;
        if (Math.abs(diff3) < 0.1) scrollY3 = targetScrollY3;
        else scrollY3 += diff3 * GuiConstants.SCROLL_SMOOTHING * dt;
    }

    private void updateAreaCoordinates(int screenWidth, int screenHeight) {
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;

        this.area3X1 = GuiConstants.DEFAULT_PADDING;
        
        this.area3Y1 = this.area1Bottom + GuiConstants.DEFAULT_PADDING;
        this.area3X2 = area1Right;
        
        this.area3Y2 = screenHeight - GuiConstants.REGION_4_HEIGHT - 2 * GuiConstants.DEFAULT_PADDING;
    }

    private void renderArea3(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x1 = area3X1;
        int yStart = area3Y1;
        int x2 = area3X2;
        int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int gap = 1;

        int currentY = yStart;

        
        if (area3Renderers.isEmpty()) {
            for (ModStatus status : modStatuses) {
                
                GDRowRenderer renderer = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, false);
                area3Renderers.add(renderer);
            }
        }

        
        guiGraphics.enableScissor(area3X1, area3Y1, area3X2, area3Y2);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -scrollY3, 0);

        for (int i = 0; i < modStatuses.size(); i++) {
            ModStatus status = modStatuses.get(i);
            GDRowRenderer renderer = area3Renderers.get(i);

            
            renderer.setBounds(x1, currentY, x2, currentY + rowHeight);

            
            renderer.resetColumnConfig();

            boolean isInstalled = ModList.get().isLoaded(status.modId);

            
            int nameColor = isInstalled ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
            renderer.addColumn(Component.translatable(status.nameKey).getString(), -1, nameColor, false, false, (btn) -> {
                status.expanded = !status.expanded;
            });

            
            String statusText = isInstalled ? Component.translatable("gd656killicon.client.gui.hometab.status.installed").getString() : Component.translatable("gd656killicon.client.gui.hometab.status.not_installed").getString();
            int statusColor = isInstalled ? GuiConstants.COLOR_WHITE : GuiConstants.COLOR_DARK_GRAY;
            renderer.addColumn(statusText, 40, statusColor, true, true);

            
            renderer.addColumn(Component.translatable("gd656killicon.client.gui.hometab.action.get").getString(), 30, GuiConstants.COLOR_WHITE, true, true, (btn) -> {
                Util.getPlatform().openUri(URI.create(status.url));
            });

            
            
            
            float actualTop = currentY - (float)scrollY3;
            float actualBottom = currentY + rowHeight - (float)scrollY3;
            
            if (actualBottom > area3Y1 && actualTop < area3Y2) {
                renderer.render(guiGraphics, mouseX, (int)(mouseY + scrollY3), partialTick);
            }

            currentY += (rowHeight + gap);

            
            if (status.expanded) {
                if (status.descriptionRenderer == null) {
                    
                    status.descriptionRenderer = new GDRowRenderer(x1 + rowHeight, 0, x2, 0, GuiConstants.COLOR_BLACK, 0.15f, false);
                }
                
                GDRowRenderer descRenderer = status.descriptionRenderer;
                descRenderer.setBounds(x1 + rowHeight, currentY, x2, currentY + rowHeight);
                descRenderer.resetColumnConfig();
                
                
                descRenderer.addColumn(Component.translatable(status.descriptionKey).getString(), -1, GuiConstants.COLOR_WHITE, false, false);
                
                float descTop = currentY - (float)scrollY3;
                float descBottom = currentY + rowHeight - (float)scrollY3;
                
                if (descBottom > area3Y1 && descTop < area3Y2) {
                    descRenderer.render(guiGraphics, mouseX, (int)(mouseY + scrollY3), partialTick);
                }
                
                currentY += (rowHeight + gap);
            }
        }
        
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        
        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
            
            double adjustedMouseY = mouseY + scrollY3;
            
            
            
            
            
            int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
            int gap = 1;
            int currentY = area3Y1;
            
            for (int i = 0; i < modStatuses.size(); i++) {
                ModStatus status = modStatuses.get(i);
                GDRowRenderer renderer = area3Renderers.get(i);
                
                
                
                
                
                
                
                
                
                
                if (renderer.mouseClicked(mouseX, adjustedMouseY, button)) {
                    return true;
                }
                
                currentY += (rowHeight + gap);
                
                
                if (status.expanded && status.descriptionRenderer != null) {
                    if (status.descriptionRenderer.mouseClicked(mouseX, adjustedMouseY, button)) {
                        return true;
                    }
                    currentY += (rowHeight + gap);
                }
            }
            
            
            isDragging3 = true;
            lastMouseY3 = mouseY;
            return true; 
        }
        
        
        if (linksRowRenderer != null) {
            
            
            
            
            
            
            
            if (linksRowRenderer.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        
        
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging3 = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= area3X1 && mouseX <= area3X2 && mouseY >= area3Y1 && mouseY <= area3Y2) {
            targetScrollY3 -= delta * GuiConstants.SCROLL_AMOUNT;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static class ModStatus {
        final String nameKey;
        final String modId;
        final String url;
        final String descriptionKey;
        boolean expanded = false;
        GDRowRenderer descriptionRenderer;

        ModStatus(String nameKey, String modId, String url, String descriptionKey) {
            this.nameKey = nameKey;
            this.modId = modId;
            this.url = url;
            this.descriptionKey = descriptionKey;
        }
    }

    private GDRowRenderer.Column prepareHoverColumn(int index, String text, int color, boolean isDarker, boolean isCentered, Consumer<Integer> onClick) {
        while (linkHoverColumns.size() <= index) {
            linkHoverColumns.add(new GDRowRenderer.Column());
        }
        GDRowRenderer.Column col = linkHoverColumns.get(index);
        col.text = text;
        col.color = color;
        col.isDarker = isDarker;
        col.isCentered = isCentered;
        col.onClick = onClick;
        return col;
    }
}
