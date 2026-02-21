package org.mods.gd656killicon.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.mods.gd656killicon.client.gui.GuiConstants;

public class PromptDialog {
    public enum PromptType {
        WARNING("gd656killicon.client.gui.prompt.warning", GuiConstants.COLOR_GOLD_ORANGE),
        INFO("gd656killicon.client.gui.prompt.info", GuiConstants.COLOR_SKY_BLUE),
        ERROR("gd656killicon.client.gui.prompt.error", GuiConstants.COLOR_RED),
        SUCCESS("gd656killicon.client.gui.prompt.success", GuiConstants.COLOR_GREEN);

        private final String titleKey;
        private final int color;

        PromptType(String titleKey, int color) {
            this.titleKey = titleKey;
            this.color = color;
        }

        public String getTitle() {
            return I18n.get(titleKey);
        }

        public int getColor() {
            return color;
        }
    }

    private final Minecraft minecraft;
    private Runnable onConfirm;
    private Runnable onCancel;
    private Runnable currentConfirmAction;

    private boolean visible = false;
    private String title = "";
    private String message = "";
    private PromptType type = PromptType.INFO;

    private GDTextRenderer titleRenderer;
    private GDTextRenderer messageRenderer;
    private GDButton confirmButton;

    private static final int INPUT_WIDTH = 140;
    private static final int INPUT_HEIGHT = GuiConstants.ROW_HEADER_HEIGHT;
    private static final int BUTTON_HEIGHT = GuiConstants.ROW_HEADER_HEIGHT;

    public PromptDialog(Minecraft minecraft, Runnable onConfirm, Runnable onCancel) {
        this.minecraft = minecraft;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public void show(String message, PromptType type, Runnable onConfirmAction) {
        show(message, type, onConfirmAction, null);
    }

    public void show(String message, PromptType type, Runnable onConfirmAction, Runnable onCancelAction) {
        this.message = message;
        this.type = type == null ? PromptType.INFO : type;
        this.title = this.type.getTitle();
        this.visible = true;
        this.currentConfirmAction = onConfirmAction;
        this.onCancel = onCancelAction;

        if (this.titleRenderer == null) {
            this.titleRenderer = new GDTextRenderer(title, 0, 0, 0, 0, 1.0f, this.type.getColor(), false);
            this.titleRenderer.setCentered(true);
        } else {
            this.titleRenderer.setText(title);
            this.titleRenderer.setColor(this.type.getColor());
            this.titleRenderer.setCentered(true);
        }

        if (this.messageRenderer == null) {
            this.messageRenderer = new GDTextRenderer(message, 0, 0, 0, 0, 1.0f, GuiConstants.COLOR_WHITE, true);
            this.messageRenderer.setCentered(false);
        } else {
            this.messageRenderer.setText(message);
            this.messageRenderer.setColor(GuiConstants.COLOR_WHITE);
            this.messageRenderer.setCentered(false);
        }

        this.confirmButton = new GDButton(0, 0, INPUT_WIDTH, BUTTON_HEIGHT, Component.literal(I18n.get("gd656killicon.client.gui.config.confirm")), (btn) -> confirm());
    }

    private void confirm() {
        if (currentConfirmAction != null) {
            currentConfirmAction.run();
        } else if (onConfirm != null) {
            onConfirm.run();
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

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (confirmButton != null && confirmButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return visible;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return visible;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
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

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500.0f);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int dimColor = 0x88444444;
        guiGraphics.fill(0, 0, screenWidth, screenHeight, dimColor);

        int fontHeight = minecraft.font.lineHeight;
        int maxWidth = INPUT_WIDTH;
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = minecraft.font.split(Component.literal(message), maxWidth);
        int messageHeight = Math.max(INPUT_HEIGHT, lines.size() * fontHeight);
        int inputX = (screenWidth - INPUT_WIDTH) / 2;
        int inputY = (screenHeight - messageHeight) / 2;
        int titleY = inputY - fontHeight - GuiConstants.DEFAULT_PADDING + 6;
        int buttonsY = inputY + messageHeight + 1;

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

        if (messageRenderer != null) {
            messageRenderer.setX1(inputX);
            messageRenderer.setY1(inputY);
            messageRenderer.setX2(inputX + INPUT_WIDTH);
            messageRenderer.setY2(inputY + messageHeight);
            messageRenderer.render(guiGraphics, partialTick);
        }

        int btnY = inputY + messageHeight + 1;
        if (confirmButton != null) {
            confirmButton.setX(inputX);
            confirmButton.setY(btnY);
            confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        guiGraphics.pose().popPose();
    }
}
