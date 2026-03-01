package org.mods.gd656killicon.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ChoiceListDialog {
    private final Minecraft minecraft;
    private Consumer<String> onConfirm;
    private Runnable onCancel;

    private boolean visible = false;
    private String title = "";
    private String filterText = "";
    private int cursorPosition = 0;
    private int displayOffset = 0;
    private String selectedValue = "";

    private final List<FixedChoiceConfigEntry.Choice> choices = new ArrayList<>();
    private final List<FixedChoiceConfigEntry.Choice> filteredChoices = new ArrayList<>();
    private final List<GDRowRenderer> rowRenderers = new ArrayList<>();

    private boolean isDraggingList = false;
    private double lastMouseY = 0;
    private double scrollY = 0;
    private double targetScrollY = 0;

    private float inputHoverProgress = 0.0f;
    private long lastFrameTime;

    private GDButton confirmButton;
    private GDButton cancelButton;
    private GDTextRenderer titleRenderer;

    private static final int PANEL_WIDTH = 140;
    private static final int BUTTON_HEIGHT = GuiConstants.ROW_HEADER_HEIGHT;
    private static final int INPUT_HEIGHT = GuiConstants.ROW_HEADER_HEIGHT;
    private static final int HUE_BAR_WIDTH = GuiConstants.ROW_HEADER_HEIGHT;
    private static final int PAD = GuiConstants.DEFAULT_PADDING;
    private static final int LIST_HEIGHT = PANEL_WIDTH - HUE_BAR_WIDTH - PAD;
    private static final int PANEL_HEIGHT = LIST_HEIGHT + PAD + INPUT_HEIGHT + PAD + BUTTON_HEIGHT;

    public ChoiceListDialog(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void show(String initialValue, String title, List<FixedChoiceConfigEntry.Choice> options, Consumer<String> onConfirm, Runnable onCancel) {
        this.title = title;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.visible = true;
        this.lastFrameTime = System.currentTimeMillis();
        this.inputHoverProgress = 0.0f;
        this.filterText = "";
        this.cursorPosition = 0;
        this.displayOffset = 0;

        this.choices.clear();
        if (options != null) {
            this.choices.addAll(options);
        }

        this.selectedValue = resolveValue(initialValue);
        applyFilter();

        int w1 = (PANEL_WIDTH - 1) / 2;
        int w2 = PANEL_WIDTH - 1 - w1;
        this.confirmButton = new GDButton(0, 0, w2, BUTTON_HEIGHT, net.minecraft.network.chat.Component.literal(I18n.get("gd656killicon.client.gui.config.confirm")), (btn) -> confirm());
        this.cancelButton = new GDButton(0, 0, w1, BUTTON_HEIGHT, net.minecraft.network.chat.Component.literal(I18n.get("gd656killicon.client.gui.config.cancel")), (btn) -> cancel());

        if (this.titleRenderer == null) {
            this.titleRenderer = new GDTextRenderer(title, 0, 0, 0, 0, 1.0f, GuiConstants.COLOR_GOLD, false);
        } else {
            this.titleRenderer.setText(title);
            this.titleRenderer.setColor(GuiConstants.COLOR_GOLD);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void hide() {
        this.visible = false;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500.0f);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int dimColor = 0x88444444;
        guiGraphics.fill(0, 0, screenWidth, screenHeight, dimColor);

        int centerX = (screenWidth - PANEL_WIDTH) / 2;
        int centerY = (screenHeight - PANEL_HEIGHT) / 2 + 20;

        int fontHeight = minecraft.font.lineHeight;
        int titleY = centerY - fontHeight - PAD + 6;
        int buttonsY = centerY + PANEL_HEIGHT - BUTTON_HEIGHT;

        int containerTop = titleY - PAD;
        int containerLeft = centerX - PAD;
        int containerRight = centerX + PANEL_WIDTH + PAD;
        int containerBottom = buttonsY + BUTTON_HEIGHT + PAD;

        guiGraphics.fill(containerLeft, containerTop, containerRight, containerBottom, GuiConstants.COLOR_BG);

        if (titleRenderer != null) {
            titleRenderer.setX1(centerX);
            titleRenderer.setY1(titleY);
            titleRenderer.setX2(centerX + PANEL_WIDTH);
            titleRenderer.setY2(titleY + fontHeight);
            titleRenderer.render(guiGraphics, partialTick);
        }

        int listX = centerX;
        int listY = buttonsY - PAD - INPUT_HEIGHT - PAD - LIST_HEIGHT;
        int listW = PANEL_WIDTH;
        int listH = LIST_HEIGHT;

        int inputX = centerX;
        int inputY = buttonsY - PAD - INPUT_HEIGHT;
        int inputW = PANEL_WIDTH;

        long currentTime = System.currentTimeMillis();
        float dt = (currentTime - lastFrameTime) / 1000.0f;
        lastFrameTime = currentTime;

        boolean isHoveringInput = mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + INPUT_HEIGHT;
        if (isHoveringInput) inputHoverProgress = Math.min(1.0f, inputHoverProgress + dt * 2.0f);
        else inputHoverProgress = Math.max(0.0f, inputHoverProgress - dt * 2.0f);

        if (isDraggingList) {
            double diff = mouseY - lastMouseY;
            targetScrollY -= diff;
            lastMouseY = mouseY;
        }

        updateScroll(dt, listH);

        int listBgColor = (GuiConstants.COLOR_BLACK & 0x00FFFFFF) | (int)(255 * 0.35f) << 24;
        guiGraphics.fill(listX, listY, listX + listW, listY + listH, listBgColor);

        renderList(guiGraphics, mouseX, mouseY, partialTick, listX, listY, listX + listW, listY + listH);

        int inputBgColor = (GuiConstants.COLOR_BLACK & 0x00FFFFFF) | (int)(255 * 0.45f) << 24;
        guiGraphics.fill(inputX, inputY, inputX + inputW, inputY + INPUT_HEIGHT, inputBgColor);

        renderHoverTrail(guiGraphics, inputX, inputY, inputW, INPUT_HEIGHT, inputHoverProgress);
        renderInputText(guiGraphics, inputX, inputY, inputW, INPUT_HEIGHT);

        int btnLeftX = centerX;
        int btnRightX = centerX + (PANEL_WIDTH - 1) / 2 + 1;

        if (cancelButton != null) {
            cancelButton.setX(btnLeftX);
            cancelButton.setY(buttonsY);
            cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (confirmButton != null) {
            confirmButton.setX(btnRightX);
            confirmButton.setY(buttonsY);
            confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        guiGraphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        if (confirmButton != null && confirmButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (cancelButton != null && cancelButton.mouseClicked(mouseX, mouseY, button)) return true;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int centerX = (screenWidth - PANEL_WIDTH) / 2;
        int centerY = (screenHeight - PANEL_HEIGHT) / 2 + 20;

        int buttonsY = centerY + PANEL_HEIGHT - BUTTON_HEIGHT;
        int inputY = buttonsY - PAD - INPUT_HEIGHT;
        int inputX = centerX;
        int listY = inputY - PAD - LIST_HEIGHT;
        int listX = centerX;

        if (mouseX >= inputX && mouseX <= inputX + PANEL_WIDTH && mouseY >= inputY && mouseY <= inputY + INPUT_HEIGHT) {
            setCursorByMouse((int)mouseX, inputX);
            return true;
        }

        if (mouseX >= listX && mouseX <= listX + PANEL_WIDTH && mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
            double adjustedMouseY = mouseY + scrollY;
            int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
            int selectX1 = listX + PANEL_WIDTH - 30;

            for (int i = 0; i < filteredChoices.size(); i++) {
                int rowTop = listY + i * (rowHeight + 1);
                int rowBottom = rowTop + rowHeight;
                if (adjustedMouseY >= rowTop && adjustedMouseY <= rowBottom) {
                    if (button == 0 && mouseX >= selectX1) {
                        selectChoice(filteredChoices.get(i));
                    }
                    break;
                }
            }

            isDraggingList = true;
            lastMouseY = mouseY;
            return true;
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingList = false;
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible) return false;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int centerX = (screenWidth - PANEL_WIDTH) / 2;
        int centerY = (screenHeight - PANEL_HEIGHT) / 2 + 20;
        int buttonsY = centerY + PANEL_HEIGHT - BUTTON_HEIGHT;
        int listY = buttonsY - PAD - INPUT_HEIGHT - PAD - LIST_HEIGHT;
        int listX = centerX;

        if (mouseX >= listX && mouseX <= listX + PANEL_WIDTH && mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
            targetScrollY -= delta * GuiConstants.SCROLL_AMOUNT;
            return true;
        }
        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!visible) return false;

        if (net.minecraft.SharedConstants.isAllowedChatCharacter(codePoint)) {
            filterText = filterText.substring(0, cursorPosition) + codePoint + filterText.substring(cursorPosition);
            cursorPosition++;
            applyFilter();
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursorPosition > 0) {
                filterText = filterText.substring(0, cursorPosition - 1) + filterText.substring(cursorPosition);
                cursorPosition--;
                applyFilter();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorPosition < filterText.length()) {
                filterText = filterText.substring(0, cursorPosition) + filterText.substring(cursorPosition + 1);
                applyFilter();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorPosition > 0) cursorPosition--;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorPosition < filterText.length()) cursorPosition++;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorPosition = 0;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_END) {
            cursorPosition = filterText.length();
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

    private void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int x1, int y1, int x2, int y2) {
        if (y2 <= y1) return;

        guiGraphics.enableScissor(x1, y1, x2, y2);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -scrollY, 0);

        int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
        for (int i = 0; i < filteredChoices.size(); i++) {
            FixedChoiceConfigEntry.Choice choice = filteredChoices.get(i);
            int rowTop = y1 + i * (rowHeight + 1);
            int rowBottom = rowTop + rowHeight;

            float actualTop = rowTop - (float)scrollY;
            float actualBottom = rowBottom - (float)scrollY;
            if (actualBottom > y1 && actualTop < y2) {
                renderChoiceRow(guiGraphics, mouseX, (int)(mouseY + scrollY), partialTick, i, choice, x1, rowTop, x2, rowBottom);
            }
        }

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void renderChoiceRow(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int index, FixedChoiceConfigEntry.Choice choice, int x1, int y1, int x2, int y2) {
        int rowBgColor = GuiConstants.COLOR_BLACK & 0xFFFFFF;
        float alpha = (index % 2 == 1) ? 0.10f : 0.30f;

        while (rowRenderers.size() <= index) {
            rowRenderers.add(new GDRowRenderer(x1, y1, x2, y2, rowBgColor, alpha, false));
        }
        GDRowRenderer renderer = rowRenderers.get(index);
        renderer.setBounds(x1, y1, x2, y2);
        renderer.setBackgroundColor(rowBgColor);
        renderer.setBackgroundAlpha(alpha);
        renderer.resetColumnConfig();

        boolean selected = choice.value().equals(selectedValue);
        int firstColor = selected ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_WHITE;
        int secondColor = selected ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_WHITE;

        renderer.addColumn(" " + choice.label(), -1, firstColor, true, false, null);
        renderer.addColumn(I18n.get("gd656killicon.client.gui.choice.select"), 30, secondColor, false, true, (btn) -> selectChoice(choice));

        renderer.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderInputText(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        guiGraphics.enableScissor(x, y, x + w, y + h);

        int textWidth = minecraft.font.width(filterText);
        int boxInnerWidth = w - 4;
        String textBeforeCursor = filterText.substring(0, cursorPosition);
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

        guiGraphics.drawString(minecraft.font, filterText, drawX, drawY, GuiConstants.COLOR_WHITE, true);

        if (System.currentTimeMillis() / 500 % 2 == 0) {
            int cx = drawX + cursorXRel;
            if (cx >= x && cx <= x + w) {
                guiGraphics.fill(cx, drawY - 1, cx + 1, drawY + minecraft.font.lineHeight + 1, GuiConstants.COLOR_WHITE);
            }
        }

        guiGraphics.disableScissor();
    }

    private void renderHoverTrail(GuiGraphics guiGraphics, int x, int y, int w, int h, float progress) {
        if (progress <= 0.001f) return;

        int color = GuiConstants.COLOR_GOLD;
        float totalLength = w + h + w + h;
        float currentLength = totalLength * easeOut(progress);
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

    private void setCursorByMouse(int mouseX, int inputX) {
        int mouseOffset = mouseX - (inputX + 2) + displayOffset;
        if (mouseOffset <= 0) {
            cursorPosition = 0;
            return;
        }
        int pos = 0;
        for (int i = 1; i <= filterText.length(); i++) {
            int width = minecraft.font.width(filterText.substring(0, i));
            if (width >= mouseOffset) {
                pos = i;
                break;
            }
            pos = i;
        }
        cursorPosition = pos;
    }

    private void updateScroll(float dt, int viewHeight) {
        int contentHeight = filteredChoices.size() * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY));

        double diff = targetScrollY - scrollY;
        if (Math.abs(diff) < 0.01) {
            scrollY = targetScrollY;
        } else {
            scrollY += diff * GuiConstants.SCROLL_SMOOTHING * dt;
        }
    }

    private void applyFilter() {
        String filter = filterText == null ? "" : filterText.toLowerCase(Locale.ROOT);
        filteredChoices.clear();
        for (FixedChoiceConfigEntry.Choice choice : choices) {
            String label = choice.label() == null ? "" : choice.label();
            String value = choice.value() == null ? "" : choice.value();
            if (filter.isEmpty() || label.toLowerCase(Locale.ROOT).contains(filter) || value.toLowerCase(Locale.ROOT).contains(filter)) {
                filteredChoices.add(choice);
            }
        }
        targetScrollY = 0;
        scrollY = 0;
        cursorPosition = Math.min(cursorPosition, filterText.length());
    }

    private String resolveValue(String value) {
        if (value != null) {
            for (FixedChoiceConfigEntry.Choice choice : choices) {
                if (choice.value().equals(value)) {
                    return value;
                }
            }
        }
        if (!choices.isEmpty()) {
            return choices.get(0).value();
        }
        return "";
    }

    private void selectChoice(FixedChoiceConfigEntry.Choice choice) {
        if (choice != null) {
            selectedValue = choice.value();
        }
    }

    private void confirm() {
        if (onConfirm != null) {
            String result = selectedValue;
            if ((result == null || result.isEmpty()) && !choices.isEmpty()) {
                result = choices.get(0).value();
            }
            onConfirm.accept(result);
        }
        this.visible = false;
    }

    private void cancel() {
        if (onCancel != null) {
            onCancel.run();
        }
        this.visible = false;
    }
}
