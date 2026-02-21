package org.mods.gd656killicon.client.gui.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    
    // Grid line color: Same as gray border (assumed COLOR_GRAY) but lower opacity
    private static final int GRID_COLOR = (GuiConstants.COLOR_GRAY & 0x00FFFFFF) | (0x40 << 24); // ~25% opacity
    private static final int CROSS_COLOR = (GuiConstants.COLOR_GRAY & 0x00FFFFFF) | (0x90 << 24);
    private static final int TEXT_COLOR = GuiConstants.COLOR_GRAY;
    private static final int BORDER_COLOR = GuiConstants.COLOR_GRAY;
    private static final int ICON_SIZE = 64;

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
        
        // Center the view initially (0,0 at center of widget)
        this.viewX = x + width / 2.0;
        this.viewY = y + height / 2.0;
    }

    public void setBounds(int x, int y, int width, int height) {
        double dx = x - this.x;
        double dy = y - this.y;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.viewX += dx;
        this.viewY += dy;
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
        
        // Enable Scissor Test
        guiGraphics.enableScissor(x, y, x + width, y + height);
        
        // Calculate visible grid range
        // Grid lines are drawn at: viewX + n * gridSize
        // We want: x <= viewX + n * gridSize <= x + width
        // (x - viewX) / gridSize <= n <= (x + width - viewX) / gridSize
        
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

        // Draw Coordinates
        guiGraphics.pose().pushPose();
        float scale = 0.5f;
        guiGraphics.pose().translate(subPixelX, subPixelY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        
        Minecraft mc = Minecraft.getInstance();
        
        for (int i = startCol; i <= endCol; i++) {
            for (int j = startRow; j <= endRow; j++) {
                int px = (int)(baseViewX + i * gridSize);
                int py = (int)(baseViewY + j * gridSize);
                
                // Only draw if intersection is visible
                if (px >= x && px <= x + width && py >= y && py <= y + height) {
                    String coordText = (i * gridSize) + "," + (j * gridSize);
                    int textWidth = mc.font.width(coordText);
                    
                    // Draw text slightly offset from intersection
                    // Scale coordinates back because we scaled the pose
                    // Target pos: px + 2, py + 2
                    // Scaled pos: (px + 2) / scale, (py + 2) / scale
                    
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
        return switch (killType) {
            case KillType.HEADSHOT -> "killicon_scrolling_headshot.png";
            case KillType.EXPLOSION -> "killicon_scrolling_explosion.png";
            case KillType.CRIT -> "killicon_scrolling_crit.png";
            case KillType.ASSIST -> "killicon_scrolling_assist.png";
            case KillType.DESTROY_VEHICLE -> "killicon_scrolling_destroyvehicle.png";
            case KillType.NORMAL -> "killicon_scrolling_default.png";
            default -> "killicon_scrolling_default.png";
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
