package org.mods.gd656killicon.client.gui.tabs;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.InfiniteGridWidget;
import org.mods.gd656killicon.client.gui.elements.PromptDialog;
import org.mods.gd656killicon.client.sounds.ExternalSoundManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SoundConfigContent extends ConfigTabContent {
    private final String presetId;
    private final Runnable onClose;
    private int calculatedBottom;
    private GDButton saveButton;
    private boolean isConfirmingReset = false;
    private List<String> soundNames = new ArrayList<>();
    private String selectedSoundName;
    private InfiniteGridWidget gridWidget;
    private ExternalSoundManager.SoundData cachedSoundData;
    private String cachedSoundDataName;

    public SoundConfigContent(Minecraft minecraft, String presetId, Runnable onClose) {
        super(minecraft, Component.translatable("gd656killicon.client.gui.config.sound.title"));
        this.presetId = presetId;
        this.onClose = onClose;
        updateSoundRows();
    }

    @Override
    protected void updateSubtitle(int x1, int y1, int x2) {
        String presetName = ElementConfigManager.getPresetDisplayName(presetId);

        List<GDTextRenderer.ColoredText> presetTexts = new ArrayList<>();
        presetTexts.add(new GDTextRenderer.ColoredText(I18n.get("gd656killicon.client.gui.config.generic.current_preset"), GuiConstants.COLOR_WHITE));
        presetTexts.add(new GDTextRenderer.ColoredText("[" + presetId + "] ", GuiConstants.COLOR_GRAY));
        presetTexts.add(new GDTextRenderer.ColoredText(presetName, GuiConstants.COLOR_GOLD));

        if (subtitleRenderer == null) {
            subtitleRenderer = new GDTextRenderer(presetTexts, x1, y1, x2, y1 + 9, 1.0f, false);
        } else {
            subtitleRenderer.setX1(x1);
            subtitleRenderer.setY1(y1);
            subtitleRenderer.setX2(x2);
            subtitleRenderer.setY2(y1 + 9);
            subtitleRenderer.setColoredTexts(presetTexts);
        }

        this.calculatedBottom = y1 + 9;
    }

    @Override
    public void updateLayout(int screenWidth, int screenHeight) {
        super.updateLayout(screenWidth, screenHeight);
        this.area1Bottom = this.calculatedBottom;

        int padding = GuiConstants.DEFAULT_PADDING;
        int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int row2Y = screenHeight - padding - buttonHeight;
        int row1Y = row2Y - 1 - buttonHeight;
        int gridX = padding;
        int gridY = this.calculatedBottom + padding;
        int area1Right = (screenWidth - 2 * padding) / 3 + padding;
        int gridWidth = area1Right - padding;
        int gridHeight = row1Y - gridY - padding;

        if (gridHeight > 0 && gridWidth > 0) {
            if (gridWidget == null) {
                gridWidget = new InfiniteGridWidget(gridX, gridY, gridWidth, gridHeight);
            } else {
                gridWidget.setBounds(gridX, gridY, gridWidth, gridHeight);
            }
        }
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        
        if (gridWidget != null) {
            gridWidget.render(guiGraphics, mouseX, mouseY, partialTick, null);
            
            
            if (selectedSoundName != null) {
                renderWaveform(guiGraphics, gridWidget.getX(), gridWidget.getY(), gridWidget.getWidth(), gridWidget.getHeight());
            }
        }
        
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick, screenWidth, screenHeight, headerHeight);
    }

    private void renderWaveform(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        
        if (!selectedSoundName.equals(cachedSoundDataName)) {
            cachedSoundData = ExternalSoundManager.getSoundData(selectedSoundName);
            cachedSoundDataName = selectedSoundName;
            if (cachedSoundData == null) {
                
                String baseName = selectedSoundName.replaceFirst("[.][^.]+$", "");
                cachedSoundData = ExternalSoundManager.getSoundData(baseName);
            }
        }

        if (cachedSoundData == null || cachedSoundData.pcmData == null || cachedSoundData.pcmData.length == 0) {
            return;
        }

        
        String baseName = selectedSoundName.replaceFirst("[.][^.]+$", "");
        boolean isPlaying = ExternalSoundManager.isSoundPlaying(baseName) || ExternalSoundManager.isSoundPlaying(selectedSoundName);
        
        

        int samples = cachedSoundData.pcmData.length / 2; 
        if (samples == 0) return;

        
        float progress = 0.0f;
        if (ExternalSoundManager.isSoundPlaying(baseName) || ExternalSoundManager.isSoundPlaying(selectedSoundName)) {
            progress = ExternalSoundManager.getSoundProgress(baseName);
            if (progress == 0.0f) {
                progress = ExternalSoundManager.getSoundProgress(selectedSoundName);
            }
        }

        guiGraphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        
        int centerY = y + (height + 2) / 2;
        int colorGold = GuiConstants.COLOR_GOLD;
        int colorPlayed = GuiConstants.COLOR_GOLD_ORANGE;
        
        
        guiGraphics.fill(x, centerY, x + width, centerY + 1, (colorGold & 0x00FFFFFF) | 0x40000000);

        
        
        
        double samplesPerPixel = (double) samples / width;
        
        for (int px = 0; px < width; px++) {
            int startSample = (int) (px * samplesPerPixel);
            int endSample = (int) ((px + 1) * samplesPerPixel);
            if (endSample > samples) endSample = samples;
            if (startSample >= endSample) continue;

            short min = Short.MAX_VALUE;
            short max = Short.MIN_VALUE;

            for (int i = startSample; i < endSample; i++) {
                int idx = i * 2;
                if (idx + 1 >= cachedSoundData.pcmData.length) break;
                short sample = (short) ((cachedSoundData.pcmData[idx + 1] << 8) | (cachedSoundData.pcmData[idx] & 0xFF));
                if (sample < min) min = sample;
                if (sample > max) max = sample;
            }

            if (min <= max) {
                float normMin = min / 32768.0f;
                float normMax = max / 32768.0f;
                
                int hMax = (int) (normMax * (height / 2 - 4));
                int hMin = (int) (normMin * (height / 2 - 4));
                
                if (hMax == hMin) hMax++; 
                
                int y1_line = centerY - hMax;
                int y2_line = centerY - hMin;
                
                
                float currentProgressX = (float) px / width;
                int color = currentProgressX <= progress ? colorPlayed : colorGold;
                
                
                guiGraphics.fill(x + px, y1_line, x + px + 1, y2_line, color);
            }
        }
        
        guiGraphics.disableScissor();
    }

    @Override
    protected void updateResetButtonState() {
    }

    @Override
    protected void renderSideButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight) {
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int padding = GuiConstants.DEFAULT_PADDING;

        int row2Y = screenHeight - padding - buttonHeight;
        int row1Y = row2Y - 1 - buttonHeight;

        int totalWidth = area1Right - padding;
        int x1 = padding + (int)getSidebarOffset();
        int row1ButtonWidth = (totalWidth - 1) / 2;

        if (resetButton == null) {
            resetButton = new GDButton(x1, row1Y, row1ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.config.button.reset_element"), (btn) -> {
                if (isConfirmingReset) {
                    ExternalSoundManager.resetSoundsAsync(presetId);
                    isConfirmingReset = false;
                    if (onClose != null) onClose.run();
                } else {
                    isConfirmingReset = true;
                    btn.setMessage(Component.translatable("gd656killicon.client.gui.config.button.confirm_reset"));
                    btn.setTextColor(GuiConstants.COLOR_RED);
                }
            });
        }

        resetButton.setX(x1);
        resetButton.setY(row1Y);
        resetButton.setWidth(row1ButtonWidth);
        resetButton.render(guiGraphics, mouseX, mouseY, partialTick);

        if (cancelButton == null) {
            cancelButton = new GDButton(x1 + row1ButtonWidth + 1, row1Y, row1ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.button.cancel"), (btn) -> {
                discardSoundChangesAndClose();
            });
        }

        cancelButton.setX(x1 + row1ButtonWidth + 1);
        cancelButton.setY(row1Y);
        cancelButton.setWidth(row1ButtonWidth);
        cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);

        int row2ButtonWidth = totalWidth;

        if (saveButton == null) {
            saveButton = new GDButton(x1, row2Y, row2ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.config.button.save_and_exit"), (btn) -> {
                if (onClose != null) onClose.run();
            });
        }

        saveButton.setX(x1);
        saveButton.setY(row2Y);
        saveButton.setWidth(row2ButtonWidth);
        saveButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (promptDialog.isVisible()) {
            return promptDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (textInputDialog.isVisible()) {
            return textInputDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == 256) {
            if (onClose != null) onClose.run();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

        if (resetButton != null && resetButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (cancelButton != null && cancelButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (saveButton != null && saveButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        if (selectedSoundName == null) {
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.sound_replace_select"), PromptDialog.PromptType.INFO, null);
            return;
        }
        Path oggPath = null;
        Path wavPath = null;
        for (Path path : paths) {
            String lower = path.toString().toLowerCase();
            if (lower.endsWith(".ogg")) {
                oggPath = path;
                break;
            } else if (lower.endsWith(".wav")) {
                wavPath = path;
            }
        }
        Path targetPath = oggPath != null ? oggPath : wavPath;
        if (targetPath == null) {
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.sound_replace_invalid"), PromptDialog.PromptType.ERROR, null);
            return;
        }
        boolean replaced = ExternalSoundManager.replaceSoundWithBackup(presetId, selectedSoundName, targetPath);
        if (replaced) {
            
            updateSoundRows();
            
            cachedSoundDataName = null;
            
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.sound_replace_success"), PromptDialog.PromptType.SUCCESS, null);
        } else {
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.sound_replace_fail"), PromptDialog.PromptType.ERROR, null);
        }
    }

    @Override
    public void onTabOpen() {
        ExternalSoundManager.ensureSoundFilesForPreset(presetId, false);
        updateSoundRows();
    }

    private void updateSoundRows() {
        configRows.clear();
        soundNames = new ArrayList<>(ExternalSoundManager.getDefaultSoundNames());
        Collections.sort(soundNames);
        
        for (int i = 0; i < soundNames.size(); i++) {
            String soundName = soundNames.get(i);
            boolean isModified = ExternalSoundManager.isSoundModified(presetId, soundName);
            String baseName = soundName.replaceFirst("[.][^.]+$", "");
            
            
            int bgColor = GuiConstants.COLOR_BLACK;
            if (soundName.equals(selectedSoundName)) {
                bgColor = GuiConstants.COLOR_DARK_GOLD_ORANGE;
            }
            
            GDRowRenderer row = new GDRowRenderer(0, 0, 0, 0, bgColor, 0, false);
            
            
            List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
            String indexLabel = String.format("%02d", i + 1);
            texts.add(new GDTextRenderer.ColoredText("[" + indexLabel + "] ", GuiConstants.COLOR_GRAY));
            texts.add(new GDTextRenderer.ColoredText(baseName, isModified ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_WHITE));
            
            row.addColoredColumn(texts, -1, false, false, (idx) -> {
                selectedSoundName = soundName;
                updateSoundRows(); 
            });
            
            
            String ext = ExternalSoundManager.getSoundExtensionForPreset(presetId, soundName);
            row.addColumn(ext, 36, isModified ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY, false, true, (idx) -> {
                selectedSoundName = soundName;
                updateSoundRows();
            });
            
            
            
            
            
            
            
            
            
            
            row.addColumn("▶", 20, GuiConstants.COLOR_WHITE, true, true, (idx) -> {
                selectedSoundName = soundName;
                ExternalSoundManager.playSound(baseName);
                updateSoundRows();
            });
            
            
            if (isModified) {
                row.addColumn("↺", 20, GuiConstants.COLOR_GOLD, true, true, (idx) -> {
                    boolean reset = ExternalSoundManager.resetSoundWithBackup(presetId, soundName);
                    if (reset) {
                        updateSoundRows();
                        cachedSoundDataName = null;
                    }
                });
            } else {
                 
                 row.addColumn("↺", 20, GuiConstants.COLOR_GRAY, true, true, null);
            }
            
            configRows.add(row);
        }
        
        if (minecraft != null) {
            updateConfigRowsLayout(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
    }

    private void discardSoundChangesAndClose() {
        ExternalSoundManager.revertPendingSoundReplacementsForPreset(presetId);
        if (onClose != null) {
            onClose.run();
        }
    }
}
