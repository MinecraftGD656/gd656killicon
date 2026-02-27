package org.mods.gd656killicon.client.gui.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.textures.ModTextures;
import org.mods.gd656killicon.common.KillType;

import java.util.List;

public class InfiniteGridWidget {
    private int x, y, width, height;
    private double viewX = 0;
    private double viewY = 0;
    private int gridSize = 20;
    
    private boolean isDragging = false;
    private double lastMouseX, lastMouseY;
    
    private static final int GRID_COLOR = (GuiConstants.COLOR_GRAY & 0x00FFFFFF) | (0x40 << 24);     private static final int CROSS_COLOR = (GuiConstants.COLOR_GRAY & 0x00FFFFFF) | (0x90 << 24);
    private static final int TEXT_COLOR = GuiConstants.COLOR_GRAY;
    private static final int BORDER_COLOR = (GuiConstants.COLOR_GRAY & 0x00FFFFFF) | (0x80 << 24);     private static final int ICON_SIZE = 64;

    public static final class ScrollingIcon {
        private final int killType;
        private final double gridX;
        private final double gridY;

        public ScrollingIcon(int killType) {
            this(killType, 0, 0);
        }

        public ScrollingIcon(int killType, double gridX, double gridY) {
            this.killType = killType;
            this.gridX = gridX;
            this.gridY = gridY;
        }
    }

    public InfiniteGridWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        
        this.viewX = x + width / 2.0;
        this.viewY = y + height / 2.0;
    }

    public void setBounds(int x, int y, int width, int height) {
        double dx = x - this.x;
        double dy = y - this.y;
        double dw = width - this.width;
        double dh = height - this.height;

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        
        this.viewX += dx + dw / 2.0;
        this.viewY += dy + dh / 2.0;
    }

    public float getOriginX() {
        return (float) viewX;
    }

    public float getOriginY() {
        return (float) viewY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, List<ScrollingIcon> icons) {
        guiGraphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        guiGraphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);
        
        guiGraphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        
        
        int startCol = (int) Math.floor((x - viewX) / gridSize);
        int endCol = (int) Math.ceil((x + width - viewX) / gridSize);
        
        int startRow = (int) Math.floor((y - viewY) / gridSize);
        int endRow = (int) Math.ceil((y + height - viewY) / gridSize);
        
        double baseViewX = Math.floor(viewX);
        double baseViewY = Math.floor(viewY);
        float subPixelX = (float) (viewX - baseViewX);
        float subPixelY = (float) (viewY - baseViewY);
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(subPixelX, subPixelY, 0);
        
        for (int i = startCol; i <= endCol; i++) {
            int lineX = (int)(baseViewX + i * gridSize);
            int lineColor = i == 0 ? CROSS_COLOR : GRID_COLOR;
            guiGraphics.fill(lineX, y, lineX + 1, y + height, lineColor);
        }
        
        for (int j = startRow; j <= endRow; j++) {
            int lineY = (int)(baseViewY + j * gridSize);
            int lineColor = j == 0 ? CROSS_COLOR : GRID_COLOR;
            guiGraphics.fill(x, lineY, x + width, lineY + 1, lineColor);
        }
        
        guiGraphics.pose().popPose();
        
        if (icons != null && !icons.isEmpty()) {
            for (ScrollingIcon icon : icons) {
                float iconX = (float) (viewX + icon.gridX);
                float iconY = (float) (viewY + icon.gridY);
                String texturePath = getTexturePath(icon.killType);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(iconX, iconY, 0);
                guiGraphics.pose().translate(-ICON_SIZE / 2f, -ICON_SIZE / 2f, 0);
                guiGraphics.blit(ModTextures.get(texturePath), 0, 0, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
                guiGraphics.pose().popPose();
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        guiGraphics.pose().pushPose();
        float scale = 0.5f;
        guiGraphics.pose().translate(subPixelX, subPixelY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        
        Minecraft mc = Minecraft.getInstance();
        
        for (int i = startCol; i <= endCol; i++) {
            for (int j = startRow; j <= endRow; j++) {
                int px = (int)(baseViewX + i * gridSize);
                int py = (int)(baseViewY + j * gridSize);
                
                if (px >= x && px <= x + width && py >= y && py <= y + height) {
                    String coordText = (i * gridSize) + "," + (j * gridSize);
                    int textWidth = mc.font.width(coordText);
                    
                    
                    float drawX = (px + 2) / scale;
                    float drawY = (py + 2) / scale;
                    
                    guiGraphics.drawString(mc.font, coordText, (int)drawX, (int)drawY, TEXT_COLOR, false);
                }
            }
        }
        
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private static String getTexturePath(int killType) {
        String presetId = org.mods.gd656killicon.client.config.ConfigManager.getCurrentPresetId();
        String textureKey = getTextureKey(killType);
        return ElementTextureDefinition.getSelectedTextureFileName(
            presetId,
            "kill_icon/scrolling",
            textureKey,
            ElementConfigManager.getElementConfig(presetId, "kill_icon/scrolling")
        );
    }

    private static String getTextureKey(int killType) {
        return switch (killType) {
            case KillType.HEADSHOT -> "headshot";
            case KillType.EXPLOSION -> "explosion";
            case KillType.CRIT -> "crit";
            case KillType.ASSIST -> "assist";
            case KillType.DESTROY_VEHICLE -> "destroy_vehicle";
            case KillType.NORMAL -> "default";
            default -> "default";
        };
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean wasDragging = isDragging;
            isDragging = false;
            return wasDragging;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            double dx = mouseX - lastMouseX;
            double dy = mouseY - lastMouseY;
            
            viewX += dx;
            viewY += dy;
            
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
