package org.mods.gd656killicon.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.mods.gd656killicon.client.gui.GuiConstants;

import java.util.function.Consumer;

/**
 * 自定义按钮类，具有悬停动画效果。
 */
public class GDButton {
    private int x, y, width, height;
    private Component message;
    private final Consumer<GDButton> onPress;
    private final Minecraft minecraft;
    private int textColor = GuiConstants.COLOR_WHITE;
    public boolean active = true;

    private float hoverProgress = 0.0f;
    private long lastTime;
    
    
    private GDTextRenderer textRenderer;

    public GDButton(int x, int y, int width, int height, Component message, Consumer<GDButton> onPress) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message;
        this.onPress = onPress;
        this.minecraft = Minecraft.getInstance();
        this.lastTime = System.currentTimeMillis();
        
        
        
        int textY = y + (height - 9) / 2;
        
        this.textRenderer = new GDTextRenderer(message.getString(), x, textY, x + width, textY + 9, 1.0f, textColor, false);
        this.textRenderer.setCentered(true);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / (float) GuiConstants.BUTTON_ANIM_DURATION_MS;
        lastTime = now;

        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        
        if (hovered) {
            hoverProgress = Math.min(1.0f, hoverProgress + dt);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - dt);
        }

        
        guiGraphics.fill(x, y, x + width, y + height, GuiConstants.COLOR_BG);

        
        
        textRenderer.render(guiGraphics, partialTick);

        
        if (hoverProgress > 0.001f) {
            float ease = 1.0f - (float) Math.pow(1.0f - hoverProgress, 3); 
            float floatBarWidth = width * ease;
            
            guiGraphics.pose().pushPose();
            
            guiGraphics.pose().translate(x, y + height - 1.0f, 0);
            
            guiGraphics.pose().scale(floatBarWidth, 1.0f, 1.0f);
            
            guiGraphics.fill(0, 0, 1, 1, GuiConstants.COLOR_GOLD);
            guiGraphics.pose().popPose();
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (active && button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            this.onPress.accept(this);
            return true;
        }
        return false;
    }

    public void setX(int x) { 
        this.x = x; 
        updateTextRendererBounds();
    }
    
    public void setY(int y) { 
        this.y = y; 
        updateTextRendererBounds();
    }
    
    public void setWidth(int width) { 
        this.width = width; 
        updateTextRendererBounds();
    }
    
    public void setHeight(int height) { 
        this.height = height; 
        updateTextRendererBounds();
    }
    
    public void setMessage(Component message) { 
        this.message = message; 
        if (textRenderer != null) {
            textRenderer.setText(message.getString());
        }
    }
    
    public void setTextColor(int color) { 
        this.textColor = color; 
        if (textRenderer != null) {
            textRenderer.setColor(color);
        }
    }
    
    private void updateTextRendererBounds() {
        if (textRenderer != null) {
            int textY = y + (height - 9) / 2;
            textRenderer.setX1(x);
            textRenderer.setY1(textY);
            textRenderer.setX2(x + width);
            textRenderer.setY2(textY + 9);
        }
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Component getMessage() { return message; }
}
