package org.mods.gd656killicon.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.mods.gd656killicon.client.gui.GuiConstants;

import java.util.function.Consumer;

public class TextInputDialog {
    private final Minecraft minecraft;
    private Consumer<String> onConfirm;
    private Runnable onCancel;
    
    
    private Consumer<String> currentConfirmAction;
    private java.util.function.Predicate<String> currentValidator;
    
    private boolean visible = false;
    private String title = "";
    private String text = "";
    private int cursorPosition = 0;
    private int displayOffset = 0; 
    private GDTextRenderer titleRenderer;
    
    
    private float hoverProgress = 0.0f; 
    private long lastFrameTime;
    
    
    private static final int INPUT_WIDTH = 140;
    private static final int INPUT_HEIGHT = GuiConstants.ROW_HEADER_HEIGHT;
    private static final int BUTTON_HEIGHT = GuiConstants.ROW_HEADER_HEIGHT; 
    private static final int BUTTON_WIDTH = (INPUT_WIDTH - 1) / 2;
    
    private GDButton confirmButton;
    private GDButton cancelButton;
    
    public TextInputDialog(Minecraft minecraft, Consumer<String> onConfirm, Runnable onCancel) {
        this.minecraft = minecraft;
        this.onConfirm = onConfirm; 
        this.onCancel = onCancel;   
    }
    
    public void show(String initialText, String title, Consumer<String> onConfirmAction) {
        show(initialText, title, onConfirmAction, null);
    }

    public void show(String initialText, String title, Consumer<String> onConfirmAction, java.util.function.Predicate<String> validator) {
        this.text = initialText;
        this.title = title;
        this.visible = true;
        this.cursorPosition = text.length();
        this.displayOffset = 0;
        this.hoverProgress = 0.0f;
        this.lastFrameTime = System.currentTimeMillis();
        
        this.currentConfirmAction = onConfirmAction;
        this.currentValidator = validator;

        if (this.titleRenderer == null) {
            this.titleRenderer = new GDTextRenderer(title, 0, 0, 0, 0, 1.0f, GuiConstants.COLOR_GOLD, false);
        } else {
            this.titleRenderer.setText(title);
            this.titleRenderer.setColor(GuiConstants.COLOR_GOLD);
        }

        
        int w1 = (INPUT_WIDTH - 1) / 2;
        int w2 = INPUT_WIDTH - 1 - w1;
        
        this.confirmButton = new GDButton(0, 0, w2, BUTTON_HEIGHT, Component.literal(I18n.get("gd656killicon.client.gui.config.confirm")), (btn) -> confirm());
        this.cancelButton = new GDButton(0, 0, w1, BUTTON_HEIGHT, Component.literal(I18n.get("gd656killicon.client.gui.config.cancel")), (btn) -> cancel());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        if (confirmButton != null && confirmButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (cancelButton != null && cancelButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        
        return true;
    }

    /**
     * Consumes scroll events to prevent background scrolling
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return visible;
    }

    private void confirm() {
        if (currentConfirmAction != null) {
            currentConfirmAction.accept(this.text);
        } else if (onConfirm != null) {
            onConfirm.accept(this.text);
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
        
        
        int dimColor = 0x88444444;
        guiGraphics.fill(0, 0, screenWidth, screenHeight, dimColor);
        
        
        int inputX = (screenWidth - INPUT_WIDTH) / 2;
        int inputY = (screenHeight - INPUT_HEIGHT) / 2;
        
        
        int fontHeight = minecraft.font.lineHeight;
        
        int titleY = inputY - fontHeight - GuiConstants.DEFAULT_PADDING + 6; 
        int buttonsY = inputY + INPUT_HEIGHT + 1; 
        
        
        
        int containerTop = titleY - GuiConstants.DEFAULT_PADDING;
        int containerLeft = inputX - GuiConstants.DEFAULT_PADDING;
        int containerRight = inputX + INPUT_WIDTH + GuiConstants.DEFAULT_PADDING;
        int containerBottom = buttonsY + BUTTON_HEIGHT + GuiConstants.DEFAULT_PADDING;
        
        
        guiGraphics.fill(containerLeft, containerTop, containerRight, containerBottom, GuiConstants.COLOR_BG); 
        
        
        if (titleRenderer != null) {
            titleRenderer.setX1(inputX);
            titleRenderer.setY1(titleY);
            titleRenderer.setX2(inputX + INPUT_WIDTH);
            titleRenderer.setY2(titleY + fontHeight);
            titleRenderer.render(guiGraphics, partialTick);
        }
        
        
        int inputBgColor = (GuiConstants.COLOR_BLACK & 0x00FFFFFF) | (int)(255 * 0.45f) << 24;
        guiGraphics.fill(inputX, inputY, inputX + INPUT_WIDTH, inputY + INPUT_HEIGHT, inputBgColor);
        
        
        boolean isHovered = mouseX >= inputX && mouseX <= inputX + INPUT_WIDTH && 
                            mouseY >= inputY && mouseY <= inputY + INPUT_HEIGHT;
        
        long currentTime = System.currentTimeMillis();
        float dt = (currentTime - lastFrameTime) / 1000.0f;
        lastFrameTime = currentTime;
        
        if (isHovered) {
            hoverProgress = Math.min(1.0f, hoverProgress + dt * 2.0f);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - dt * 2.0f);
        }
        
        
        renderHoverTrail(guiGraphics, inputX, inputY, INPUT_WIDTH, INPUT_HEIGHT);
        
        
        boolean isValid = currentValidator == null || currentValidator.test(text);
        renderInputText(guiGraphics, inputX, inputY, INPUT_WIDTH, INPUT_HEIGHT, isValid);
        
        
        int btnY = inputY + INPUT_HEIGHT + 1;
        
        
        if (cancelButton != null) {
            cancelButton.setX(inputX);
            cancelButton.setY(btnY);
            
            cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        if (confirmButton != null) {
            int w1 = (INPUT_WIDTH - 1) / 2;
            confirmButton.setX(inputX + w1 + 1);
            confirmButton.setY(btnY);
            
            
            confirmButton.active = isValid;
            if (!isValid) {
                 confirmButton.setTextColor(GuiConstants.COLOR_GRAY);
            } else {
                 confirmButton.setTextColor(GuiConstants.COLOR_WHITE);
            }

            
            confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        guiGraphics.pose().popPose();
    }
    
    private void renderHoverTrail(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        if (hoverProgress <= 0.001f) return;
        
        int color = GuiConstants.COLOR_GOLD;
        
        float totalLength = w + h + w + h;
        float currentLength = totalLength * easeOut(hoverProgress);
        
        
        
        
        
        
        
        float drawn = 0;
        
        
        if (currentLength > 0) {
            float segLen = Math.min(w, currentLength);
            guiGraphics.fill(x, y + h - 1, x + (int)segLen, y + h, color);
            drawn += w;
        }
        
        
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(h, rem);
            guiGraphics.fill(x + w - 1, y + h - (int)segLen, x + w, y + h, color);
            drawn += h;
        }
        
        
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(w, rem);
            guiGraphics.fill(x + w - (int)segLen, y, x + w, y + 1, color);
            drawn += w;
        }
        
        
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(h, rem);
            guiGraphics.fill(x, y, x + 1, y + (int)segLen, color);
        }
    }
    
    private float easeOut(float t) {
        
        return 1.0f - (float)Math.pow(1.0f - t, 3);
    }
    
    private void renderInputText(GuiGraphics guiGraphics, int x, int y, int w, int h, boolean isValid) {
        
        
        double scale = minecraft.getWindow().getGuiScale();
        int sx = (int)(x * scale);
        int sy = (int)(minecraft.getWindow().getHeight() - (y + h) * scale);
        int sw = (int)(w * scale);
        int sh = (int)(h * scale);
        
        guiGraphics.enableScissor(x, y, x + w, y + h);
        
        
        int textWidth = minecraft.font.width(text);
        int boxInnerWidth = w - 4; 
        
        
        
        String textBeforeCursor = text.substring(0, cursorPosition);
        int cursorXRel = minecraft.font.width(textBeforeCursor);
        
        
        
        
        
        if (cursorXRel < displayOffset) {
            displayOffset = cursorXRel;
        } else if (cursorXRel > displayOffset + boxInnerWidth) {
            displayOffset = cursorXRel - boxInnerWidth;
        }
        
        
        if (textWidth < boxInnerWidth) {
            displayOffset = 0;
        }
        
        int drawX = x + 2 - displayOffset;
        int drawY = y + (h - minecraft.font.lineHeight) / 2;
        
        int textColor = isValid ? GuiConstants.COLOR_WHITE : GuiConstants.COLOR_RED;
        guiGraphics.drawString(minecraft.font, text, drawX, drawY, textColor, true);
        
        
        if (System.currentTimeMillis() / 500 % 2 == 0) {
            int cx = drawX + cursorXRel;
            if (cx >= x && cx <= x + w) { 
                guiGraphics.fill(cx, drawY - 1, cx + 1, drawY + minecraft.font.lineHeight + 1, GuiConstants.COLOR_WHITE);
            }
        }
        
        guiGraphics.disableScissor();
    }
    
    public boolean charTyped(char codePoint, int modifiers) {
        if (!visible) return false;
        
        if (net.minecraft.SharedConstants.isAllowedChatCharacter(codePoint)) {
            text = text.substring(0, cursorPosition) + codePoint + text.substring(cursorPosition);
            cursorPosition++;
            return true;
        }
        return false; 
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursorPosition > 0) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                cursorPosition--;
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorPosition < text.length()) {
                text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorPosition > 0) cursorPosition--;
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorPosition < text.length()) cursorPosition++;
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorPosition = 0;
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_END) {
            cursorPosition = text.length();
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        
        return true; 
    }
}
