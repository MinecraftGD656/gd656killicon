package org.mods.gd656killicon.client.gui.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * PS-style Color Picker Dialog.
 * Replaces TextInputDialog for color configuration.
 */
public class ColorPickerDialog {
    private final Minecraft minecraft;
    private Consumer<String> onConfirm;
    private Runnable onCancel;
    
    private boolean visible = false;
    private String title = "选择颜色";
    private String initialHex = "#FFFFFF";
    
    // Color State
    private float h = 0.0f; // Hue: 0.0 - 1.0
    private float s = 0.0f; // Saturation: 0.0 - 1.0
    private float v = 1.0f; // Value/Brightness: 0.0 - 1.0
    private int currentRGB = 0xFFFFFFFF;
    
    // UI Layout Constants
    private static final int PANEL_WIDTH = 140;
    private static final int BUTTON_HEIGHT = GuiConstants.ROW_HEADER_HEIGHT;
    private static final int PREVIEW_HEIGHT = GuiConstants.ROW_HEADER_HEIGHT;
    private static final int HUE_BAR_WIDTH = GuiConstants.ROW_HEADER_HEIGHT;
    private static final int PAD = GuiConstants.DEFAULT_PADDING;
    
    // Derived Constants (recalculated in render/show if needed, but constant for fixed panel width)
    private static final int SV_BOX_SIZE = PANEL_WIDTH - HUE_BAR_WIDTH - PAD; // 140 - 17 - 8 = 115
    private static final int PANEL_HEIGHT = SV_BOX_SIZE + PAD + PREVIEW_HEIGHT + PAD + BUTTON_HEIGHT; // 115 + 8 + 17 + 8 + 17 = 165
    
    // Interaction State
    private boolean isDraggingSV = false;
    private boolean isDraggingHue = false;
    
    // Animation State
    private float svHoverProgress = 0.0f;
    private float hueHoverProgress = 0.0f;
    private long lastFrameTime;
    
    private GDButton confirmButton;
    private GDButton cancelButton;
    
    private GDTextRenderer previewTextRenderer;
    private GDTextRenderer titleRenderer;
    
    public ColorPickerDialog(Minecraft minecraft) {
        this.minecraft = minecraft;
    }
    
    public void show(String initialHex, String title, Consumer<String> onConfirm, Runnable onCancel) {
        this.initialHex = initialHex;
        this.title = title;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.visible = true;
        this.lastFrameTime = System.currentTimeMillis();
        this.svHoverProgress = 0.0f;
        this.hueHoverProgress = 0.0f;
        
        parseHex(initialHex);
        
        // Initialize Buttons
        int w1 = (PANEL_WIDTH - 1) / 2;
        int w2 = PANEL_WIDTH - 1 - w1;
        
        this.confirmButton = new GDButton(0, 0, w2, BUTTON_HEIGHT, Component.literal(I18n.get("gd656killicon.client.gui.config.confirm")), (btn) -> confirm());
        this.cancelButton = new GDButton(0, 0, w1, BUTTON_HEIGHT, Component.literal(I18n.get("gd656killicon.client.gui.config.cancel")), (btn) -> cancel());
        
        // Initialize Text Renderer
        this.previewTextRenderer = new GDTextRenderer("\u00A7l" + formatHex(currentRGB), 0, 0, 0, 0, 1.0f, 0xFFFFFFFF, false);
        this.previewTextRenderer.setCentered(true);

        if (this.titleRenderer == null) {
            this.titleRenderer = new GDTextRenderer(title, 0, 0, 0, 0, 1.0f, GuiConstants.COLOR_GOLD, false);
        } else {
            this.titleRenderer.setText(title);
            this.titleRenderer.setColor(GuiConstants.COLOR_GOLD);
        }
    }
    
    private void parseHex(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            // Handle 3-digit hex (e.g. #FFF)
            if (hex.length() == 3) {
                StringBuilder sb = new StringBuilder();
                for (char c : hex.toCharArray()) sb.append(c).append(c);
                hex = sb.toString();
            }
            
            int rgb = Integer.parseInt(hex, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            
            float[] hsv = Color.RGBtoHSB(r, g, b, null);
            this.h = hsv[0];
            this.s = hsv[1];
            this.v = hsv[2];
            updateCurrentRGB();
        } catch (Exception e) {
            // Default to white if invalid
            this.h = 0.0f;
            this.s = 0.0f;
            this.v = 1.0f;
            updateCurrentRGB();
        }
    }
    
    private void updateCurrentRGB() {
        this.currentRGB = Color.HSBtoRGB(h, s, v);
        if (previewTextRenderer != null) {
            previewTextRenderer.setText("\u00A7l" + formatHex(currentRGB));
            // Contrast text color
            int textColor = (v > 0.5f) ? 0xFF000000 : 0xFFFFFFFF;
            previewTextRenderer.setColor(textColor);
        }
    }
    
    private String formatHex(int rgb) {
        return String.format("#%06X", (0xFFFFFF & rgb));
    }
    
    private void confirm() {
        if (onConfirm != null) {
            onConfirm.accept(formatHex(currentRGB));
        }
        this.visible = false;
    }
    
    private void cancel() {
        if (onCancel != null) {
            onCancel.run();
        }
        this.visible = false;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500.0f);
        
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // 1. Fullscreen Dim Background
        int dimColor = 0x88444444;
        guiGraphics.fill(0, 0, screenWidth, screenHeight, dimColor);
        
        // Calculate Center Positions
        int centerX = (screenWidth - PANEL_WIDTH) / 2;
        int centerY = (screenHeight - PANEL_HEIGHT) / 2 + 20;
        
        // Calculate Bounds
        int fontHeight = minecraft.font.lineHeight;
        int titleY = centerY - fontHeight - PAD + 6;
        int buttonsY = centerY + PANEL_HEIGHT - BUTTON_HEIGHT;
        
        int containerTop = titleY - PAD;
        int containerLeft = centerX - PAD;
        int containerRight = centerX + PANEL_WIDTH + PAD;
        int containerBottom = buttonsY + BUTTON_HEIGHT + PAD;
        
        // 2. Container Background
        guiGraphics.fill(containerLeft, containerTop, containerRight, containerBottom, GuiConstants.COLOR_BG);
        
        // 3. Title
        if (titleRenderer != null) {
            titleRenderer.setX1(centerX);
            titleRenderer.setY1(titleY);
            titleRenderer.setX2(centerX + PANEL_WIDTH);
            titleRenderer.setY2(titleY + fontHeight);
            titleRenderer.render(guiGraphics, partialTick);
        }
        
        // 4. Layout Calculation
        // Buttons (Bottom)
        int btnLeftX = centerX;
        int btnRightX = centerX + (PANEL_WIDTH - 1) / 2 + 1;
        
        // Preview Box (Above Buttons)
        int previewY = buttonsY - PAD - PREVIEW_HEIGHT;
        int previewX = centerX;
        int previewW = PANEL_WIDTH;
        
        // Hue Slider (Top Right)
        int hueX = centerX + PANEL_WIDTH - HUE_BAR_WIDTH;
        int hueY = previewY - PAD - SV_BOX_SIZE;
        int hueH = SV_BOX_SIZE;
        
        // SV Box (Top Left)
        int svX = centerX;
        int svY = hueY;
        int svW = SV_BOX_SIZE;
        int svH = SV_BOX_SIZE;
        
        // Update Animation State
        long currentTime = System.currentTimeMillis();
        float dt = (currentTime - lastFrameTime) / 1000.0f;
        lastFrameTime = currentTime;
        
        boolean isHoveringSV = mouseX >= svX && mouseX <= svX + svW && mouseY >= svY && mouseY <= svY + svH;
        boolean isHoveringHue = mouseX >= hueX && mouseX <= hueX + HUE_BAR_WIDTH && mouseY >= hueY && mouseY <= hueY + hueH;
        
        if (isHoveringSV) svHoverProgress = Math.min(1.0f, svHoverProgress + dt * 2.0f);
        else svHoverProgress = Math.max(0.0f, svHoverProgress - dt * 2.0f);
        
        if (isHoveringHue) hueHoverProgress = Math.min(1.0f, hueHoverProgress + dt * 2.0f);
        else hueHoverProgress = Math.max(0.0f, hueHoverProgress - dt * 2.0f);
        
        // 5. Render Components
        
        // Render SV Box
        renderSVBox(guiGraphics, svX, svY, svW, svH);
        renderHoverTrail(guiGraphics, svX, svY, svW, svH, svHoverProgress);
        
        // Render Hue Bar
        renderHueBar(guiGraphics, hueX, hueY, HUE_BAR_WIDTH, hueH);
        renderHoverTrail(guiGraphics, hueX, hueY, HUE_BAR_WIDTH, hueH, hueHoverProgress);
        
        // Render Selection Indicators
        // SV Circle
        int selX = svX + (int)(s * svW);
        int selY = svY + (int)((1.0f - v) * svH);
        // Clamp selection to box
        selX = Math.max(svX, Math.min(svX + svW, selX));
        selY = Math.max(svY, Math.min(svY + svH, selY));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1); // Bring to front
        guiGraphics.fill(selX - 2, selY - 2, selX + 2, selY + 2, 0xFFFFFFFF);
        guiGraphics.fill(selX - 1, selY - 1, selX + 1, selY + 1, 0xFF000000);
        guiGraphics.pose().popPose();
        
        // Hue Handle
        int hueSelY = hueY + (int)(h * hueH);
        hueSelY = Math.max(hueY, Math.min(hueY + hueH, hueSelY));
        guiGraphics.fill(hueX - 1, hueSelY - 1, hueX + HUE_BAR_WIDTH + 1, hueSelY + 1, 0xFFFFFFFF);
        guiGraphics.fill(hueX, hueSelY, hueX + HUE_BAR_WIDTH, hueSelY, 0xFF000000);
        
        // Render Preview Box
        guiGraphics.fill(previewX, previewY, previewX + previewW, previewY + PREVIEW_HEIGHT, 0xFF000000 | currentRGB);
        
        // Render Hex Text
        if (previewTextRenderer != null) {
            previewTextRenderer.setX1(previewX);
            previewTextRenderer.setY1(previewY + (PREVIEW_HEIGHT - 9) / 2 + 1); // Center vertically, roughly
            previewTextRenderer.setX2(previewX + previewW);
            previewTextRenderer.setY2(previewY + PREVIEW_HEIGHT);
            previewTextRenderer.render(guiGraphics, partialTick);
        }
        
        // 6. Buttons
        if (cancelButton != null) {
            cancelButton.setX(centerX);
            cancelButton.setY(buttonsY);
            cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        if (confirmButton != null) {
            confirmButton.setX(btnRightX); // Right side
            confirmButton.setY(buttonsY);
            confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        // Handle Dragging
        if (isDraggingSV) {
            float newS = (float)(mouseX - svX) / svW;
            float newV = 1.0f - (float)(mouseY - svY) / svH;
            s = Math.max(0.0f, Math.min(1.0f, newS));
            v = Math.max(0.0f, Math.min(1.0f, newV));
            updateCurrentRGB();
        }
        
        if (isDraggingHue) {
            float newH = (float)(mouseY - hueY) / hueH;
            h = Math.max(0.0f, Math.min(1.0f, newH));
            updateCurrentRGB();
        }
        
        guiGraphics.pose().popPose();
    }
    
    private void renderSVBox(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        int hueColor = Color.HSBtoRGB(this.h, 1.0f, 1.0f);
        int r = (hueColor >> 16) & 0xFF;
        int g = (hueColor >> 8) & 0xFF;
        int b = hueColor & 0xFF;
        
        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        // Bottom-Left (Black)
        bufferbuilder.vertex(matrix, x, y + h, 0).color(0, 0, 0, 255).endVertex();
        
        // Bottom-Right (Black)
        bufferbuilder.vertex(matrix, x + w, y + h, 0).color(0, 0, 0, 255).endVertex();
        
        // Top-Right (Hue Color)
        bufferbuilder.vertex(matrix, x + w, y, 0).color(r, g, b, 255).endVertex();
        
        // Top-Left (White)
        bufferbuilder.vertex(matrix, x, y, 0).color(255, 255, 255, 255).endVertex();
        
        BufferUploader.drawWithShader(bufferbuilder.end());
        RenderSystem.disableBlend();
    }
    
    private void renderHueBar(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        int step = 1;
        // Optimization: Don't draw too many lines if height is large
        // But for h=115, 115 lines is fine.
        for (int i = 0; i < h; i += step) {
            float hue = (float)i / h;
            int color = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            guiGraphics.fill(x, y + i, x + w, y + i + step, 0xFF000000 | color);
        }
    }
    
    private void renderHoverTrail(GuiGraphics guiGraphics, int x, int y, int w, int h, float progress) {
        if (progress <= 0.001f) return;
        
        int color = GuiConstants.COLOR_GOLD;
        float totalLength = w + h + w + h;
        float currentLength = totalLength * easeOut(progress);
        
        float drawn = 0;
        
        // Bottom: (x, y+h) -> (x+w, y+h)
        if (currentLength > 0) {
            float segLen = Math.min(w, currentLength);
            guiGraphics.fill(x, y + h - 1, x + (int)segLen, y + h, color);
            drawn += w;
        }
        
        // Right: (x+w, y+h) -> (x+w, y)
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(h, rem);
            guiGraphics.fill(x + w - 1, y + h - (int)segLen, x + w, y + h, color);
            drawn += h;
        }
        
        // Top: (x+w, y) -> (x, y)
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(w, rem);
            guiGraphics.fill(x + w - (int)segLen, y, x + w, y + 1, color);
            drawn += w;
        }
        
        // Left: (x, y) -> (x, y+h)
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(h, rem);
            guiGraphics.fill(x, y, x + 1, y + (int)segLen, color);
        }
    }
    
    private float easeOut(float t) {
        return 1.0f - (float)Math.pow(1.0f - t, 3);
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        // Check buttons
        if (confirmButton != null && confirmButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (cancelButton != null && cancelButton.mouseClicked(mouseX, mouseY, button)) return true;
        
        // Layout must be recalculated to match render
        // For simplicity, we can duplicate the calculation or store it.
        // Duplicating is safer to avoid state desync if render hasn't run yet (though unlikely)
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int centerX = (screenWidth - PANEL_WIDTH) / 2;
        int centerY = (screenHeight - PANEL_HEIGHT) / 2 + 20;
        
        int buttonsY = centerY + PANEL_HEIGHT - BUTTON_HEIGHT;
        int previewY = buttonsY - PAD - PREVIEW_HEIGHT;
        int hueY = previewY - PAD - SV_BOX_SIZE;
        int hueX = centerX + PANEL_WIDTH - HUE_BAR_WIDTH;
        int svX = centerX;
        
        if (mouseX >= svX && mouseX <= svX + SV_BOX_SIZE && mouseY >= hueY && mouseY <= hueY + SV_BOX_SIZE) {
            isDraggingSV = true;
            return true;
        }
        
        if (mouseX >= hueX && mouseX <= hueX + HUE_BAR_WIDTH && mouseY >= hueY && mouseY <= hueY + SV_BOX_SIZE) {
            isDraggingHue = true;
            return true;
        }
        
        return true; // Consume background clicks
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingSV = false;
        isDraggingHue = false;
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return visible;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return true;
    }
}
