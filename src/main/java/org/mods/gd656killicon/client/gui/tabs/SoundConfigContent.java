package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.InfiniteGridWidget;
import org.mods.gd656killicon.client.gui.elements.PromptDialog;
import org.mods.gd656killicon.client.sounds.ExternalSoundManager;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class SoundConfigContent extends ConfigTabContent {
    private final String presetId;
    private final Runnable onClose;
    private int calculatedBottom;
    private GDButton saveButton;
    private boolean isConfirmingReset = false;
    private long resetConfirmTime = 0L;
    private List<String> soundNames = new ArrayList<>();
    private String selectedSoundName;
    private InfiniteGridWidget gridWidget;
    private ExternalSoundManager.SoundData cachedSoundData;
    private String cachedSoundDataName;
    private boolean isCommonExpanded = false;
    private boolean isScrollingExpanded = false;
    private boolean isBattlefield1Expanded = false;
    private boolean isCardExpanded = false;
    private boolean isComboExpanded = false;
    private boolean isValorantExpanded = false;
    private boolean isSelectingSound = false;
    private boolean isSelectOfficialExpanded = true;
    private boolean isSelectCustomExpanded = true;
    private ExternalSoundManager.SoundSlotDefinition selectedSlot = null;
    private boolean lastSelectingSound = false;

    public SoundConfigContent(Minecraft minecraft, String presetId, Runnable onClose) {
        super(minecraft, Component.translatable("gd656killicon.client.gui.config.sound.title"));
        this.presetId = presetId;
        this.onClose = onClose;
        updateSoundRows();
    }

    @Override
    protected void updateSubtitle(int x1, int y1, int x2) {
        if (isSelectingSound) {
            String text = I18n.get("gd656killicon.client.gui.config.sound.select.subtitle");
            if (subtitleRenderer == null) {
                subtitleRenderer = new GDTextRenderer(text, x1, y1, x2, y1 + 9, 1.0f, GuiConstants.COLOR_WHITE, false);
            } else {
                subtitleRenderer.setX1(x1);
                subtitleRenderer.setY1(y1);
                subtitleRenderer.setX2(x2);
                subtitleRenderer.setY2(y1 + 9);
                subtitleRenderer.setText(text);
                subtitleRenderer.setColor(GuiConstants.COLOR_WHITE);
            }
            this.calculatedBottom = y1 + 9;
            return;
        }
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
        if (titleRenderer != null) {
            if (isSelectingSound) {
                titleRenderer.setText(I18n.get("gd656killicon.client.gui.config.sound.select.title"));
            } else {
                titleRenderer.setText(I18n.get("gd656killicon.client.gui.config.sound.title"));
            }
        }

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

        int samples = cachedSoundData.pcmData.length / 2;         if (samples == 0) return;

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
        if (isSelectingSound) {
            return;
        }
        if (!isConfirmingReset || resetButton == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - resetConfirmTime;
        if (elapsed > 3000) {
            isConfirmingReset = false;
            resetButton.setMessage(Component.translatable("gd656killicon.client.gui.config.button.reset_element"));
            resetButton.setTextColor(GuiConstants.COLOR_WHITE);
        } else {
            resetButton.setMessage(Component.translatable("gd656killicon.client.gui.config.button.confirm_reset"));
            resetButton.setTextColor(GuiConstants.COLOR_RED);
        }
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

        if (isSelectingSound) {
            if (resetButton == null) {
                resetButton = new GDButton(x1, row1Y, row1ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.config.sound.reset_current"), (btn) -> {
                    if (selectedSlot != null) {
                        ExternalSoundManager.resetSoundSelectionToDefault(presetId, selectedSlot.slotId());
                        selectedSoundName = ExternalSoundManager.getSelectedSoundBaseName(presetId, selectedSlot.slotId());
                        cachedSoundDataName = null;
                        updateSoundRows();
                    }
                });
            }
            resetButton.setX(x1);
            resetButton.setY(row1Y);
            resetButton.setWidth(row1ButtonWidth);
            resetButton.render(guiGraphics, mouseX, mouseY, partialTick);
            if (cancelButton == null) {
                cancelButton = new GDButton(x1 + row1ButtonWidth + 1, row1Y, row1ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.button.cancel"), (btn) -> {
                    cancelSoundSelection();
                });
            }
            cancelButton.setX(x1 + row1ButtonWidth + 1);
            cancelButton.setY(row1Y);
            cancelButton.setWidth(row1ButtonWidth);
            cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
            if (saveButton == null) {
                saveButton = new GDButton(x1, row2Y, totalWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.config.button.save_and_exit"), (btn) -> {
                    cancelSoundSelection();
                });
            }
            saveButton.setX(x1);
            saveButton.setY(row2Y);
            saveButton.setWidth(totalWidth);
            saveButton.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        if (resetButton == null) {
            resetButton = new GDButton(x1, row1Y, row1ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.config.button.reset_element"), (btn) -> {
                if (isConfirmingReset) {
                    ExternalSoundManager.markPendingSoundReset(presetId);
                    isConfirmingReset = false;
                    btn.setMessage(Component.translatable("gd656killicon.client.gui.config.button.reset_element"));
                    btn.setTextColor(GuiConstants.COLOR_WHITE);
                    if (onClose != null) onClose.run();
                } else {
                    isConfirmingReset = true;
                    resetConfirmTime = System.currentTimeMillis();
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
            if (isSelectingSound) {
                cancelSoundSelection();
            } else if (onClose != null) {
                onClose.run();
            }
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
        if (!isSelectingSound || selectedSlot == null) {
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
        Runnable importAction = () -> {
            String originalName = targetPath.getFileName().toString();
            String baseName = ExternalSoundManager.createCustomSoundFromFile(presetId, targetPath, originalName);
            if (baseName != null) {
                isSelectCustomExpanded = true;
                selectSoundForSlot(baseName);
            } else {
                promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.sound_replace_fail"), PromptDialog.PromptType.ERROR, null);
            }
        };

        importAction.run();
    }

    @Override
    public void onTabOpen() {
        ExternalSoundManager.ensureSoundFilesForPreset(presetId, false);
        updateSoundRows();
        if (ClientConfigManager.shouldShowSoundIntro()) {
            ClientConfigManager.markSoundIntroShown();
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.sound_intro"), PromptDialog.PromptType.INFO, null);
        }
    }

    private void updateSoundRows() {
        configRows.clear();
        if (isSelectingSound != lastSelectingSound) {
            resetButton = null;
            cancelButton = null;
            saveButton = null;
            isConfirmingReset = false;
            lastSelectingSound = isSelectingSound;
        }
        if (isSelectingSound && ClientConfigManager.shouldShowSoundSelectIntro()) {
            ClientConfigManager.markSoundSelectIntroShown();
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.sound_select_intro"), PromptDialog.PromptType.INFO, null);
        }
        if (isSelectingSound && selectedSlot != null) {
            buildSoundSelectionRows();
        } else {
            buildSoundSlotRows();
        }
        
        if (minecraft != null) {
            updateConfigRowsLayout(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
    }

    private void discardSoundChangesAndClose() {
        if (onClose != null) {
            onClose.run();
        }
    }

    private void cancelSoundSelection() {
        isSelectingSound = false;
        updateSoundRows();
    }

    private void addCategoryHeader(String titleKey, boolean expanded, Runnable onToggle) {
        GDRowRenderer header = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_GOLD, 0.75f, true);
        header.addColumn(I18n.get(titleKey), -1, GuiConstants.COLOR_WHITE, true, false, (idx) -> onToggle.run());
        header.addColumn(expanded ? "▼" : "▶", 20, GuiConstants.COLOR_WHITE, true, true, (idx) -> onToggle.run());
        configRows.add(header);
    }

    private void buildSoundSlotRows() {
        Map<ExternalSoundManager.SoundElementGroup, List<ExternalSoundManager.SoundSlotDefinition>> groupedSlots = new LinkedHashMap<>();
        for (ExternalSoundManager.SoundSlotDefinition slot : ExternalSoundManager.getSoundSlotDefinitions()) {
            groupedSlots.computeIfAbsent(slot.group(), key -> new ArrayList<>()).add(slot);
        }

        for (Map.Entry<ExternalSoundManager.SoundElementGroup, List<ExternalSoundManager.SoundSlotDefinition>> entry : groupedSlots.entrySet()) {
            ExternalSoundManager.SoundElementGroup group = entry.getKey();
            boolean expanded = isGroupExpanded(group);
            addCategoryHeader(group.titleKey(), expanded, () -> {
                setGroupExpanded(group, !isGroupExpanded(group));
                updateSoundRows();
            });
            if (!expanded) {
                continue;
            }
            for (ExternalSoundManager.SoundSlotDefinition slot : entry.getValue()) {
                addSoundSlotRow(slot);
                
            }
        }
    }

    private boolean isGroupExpanded(ExternalSoundManager.SoundElementGroup group) {
        return switch (group) {
            case COMMON -> isCommonExpanded;
            case SCROLLING -> isScrollingExpanded;
            case BATTLEFIELD1 -> isBattlefield1Expanded;
            case CARD -> isCardExpanded;
            case COMBO -> isComboExpanded;
            case VALORANT -> isValorantExpanded;
        };
    }

    private void setGroupExpanded(ExternalSoundManager.SoundElementGroup group, boolean expanded) {
        switch (group) {
            case COMMON -> isCommonExpanded = expanded;
            case SCROLLING -> isScrollingExpanded = expanded;
            case BATTLEFIELD1 -> isBattlefield1Expanded = expanded;
            case CARD -> isCardExpanded = expanded;
            case COMBO -> isComboExpanded = expanded;
            case VALORANT -> isValorantExpanded = expanded;
        }
    }

    private void buildSoundSelectionRows() {
        String selectedBaseName = selectedSlot == null ? null : ExternalSoundManager.getSelectedSoundBaseName(presetId, selectedSlot.slotId());
        addCategoryHeader("gd656killicon.client.gui.config.sound.select.group.official", isSelectOfficialExpanded, () -> {
            isSelectOfficialExpanded = !isSelectOfficialExpanded;
            updateSoundRows();
        });
        if (isSelectOfficialExpanded) {
            soundNames = new ArrayList<>(ExternalSoundManager.getOfficialSoundFileNames());
            Collections.sort(soundNames);
            for (String soundName : soundNames) {
                String baseName = soundName.replaceFirst("[.][^.]+$", "");
                addSelectableSoundRow(soundName, baseName, false, selectedBaseName);
            }
        }

        addCategoryHeader("gd656killicon.client.gui.config.sound.select.group.custom", isSelectCustomExpanded, () -> {
            isSelectCustomExpanded = !isSelectCustomExpanded;
            updateSoundRows();
        });
        if (isSelectCustomExpanded) {
            List<String> customNames = ExternalSoundManager.getCustomSoundBaseNames(presetId);
            for (String baseName : customNames) {
                String label = ExternalSoundManager.getSoundDisplayName(presetId, baseName);
                addSelectableSoundRow(label, baseName, true, selectedBaseName);
            }
        }
    }

    private void addImportSoundRow() {
        GDRowRenderer row = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BG, 0.3f, false);
        row.addColumn(I18n.get("gd656killicon.client.gui.config.generic.import_audio_file"), -1, GuiConstants.COLOR_WHITE, false, false, null);
        row.addColumn(I18n.get("gd656killicon.client.gui.action.open"), 60, GuiConstants.COLOR_GREEN, true, true, (idx) -> openSoundImportDialog());
        configRows.add(row);
    }

    private void openSoundImportDialog() {
        getTextInputDialog().show(
            "",
            I18n.get("gd656killicon.client.gui.prompt.sound_import_title"),
            (input) -> {
                Path path = tryParsePath(input);
                if (path != null) {
                    onFilesDrop(List.of(path));
                }
            },
            (input) -> isExistingFileWithExtension(input, "ogg", "wav")
        );
    }

    private Path tryParsePath(String rawInput) {
        if (rawInput == null) {
            return null;
        }
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Path.of(trimmed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isExistingFileWithExtension(String rawInput, String... extensions) {
        Path path = tryParsePath(rawInput);
        if (path == null || !Files.isRegularFile(path) || extensions == null || extensions.length == 0) {
            return false;
        }
        String lower = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (extension != null && !extension.isBlank() && lower.endsWith("." + extension.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void addSoundSlotRow(ExternalSoundManager.SoundSlotDefinition slot) {
        String baseName = ExternalSoundManager.getSelectedSoundBaseName(presetId, slot.slotId());
        String displayName = ExternalSoundManager.getSoundDisplayName(presetId, baseName);
        boolean modified = ExternalSoundManager.isSoundSelectionModified(presetId, slot.slotId());
        String extension = ExternalSoundManager.getSoundExtensionForPreset(presetId, baseName);

        GDRowRenderer row = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0, false);
        row.addColumn(" " + I18n.get(slot.titleKey()), -1, GuiConstants.COLOR_WHITE, false, false, (idx) -> {
            selectedSoundName = baseName;
            cachedSoundDataName = null;
        });
        row.addColumn(extension, 36, GuiConstants.COLOR_GRAY, false, true, null);
        row.addColumn("▶", 20, GuiConstants.COLOR_WHITE, true, true, (idx) -> {
            selectedSoundName = baseName;
            cachedSoundDataName = null;
            playPreviewSlot(slot);
        });
        int nameColor = modified ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
        row.addColumn(displayName, 80, nameColor, false, false, (idx) -> {
            selectedSlot = slot;
            selectedSoundName = baseName;
            isSelectingSound = true;
            updateSoundRows();
        });
        GDRowRenderer.Column hoverCol = new GDRowRenderer.Column();
        hoverCol.text = I18n.get("gd656killicon.client.gui.config.sound.select.hover");
        hoverCol.color = nameColor;
        hoverCol.isCentered = false;
        hoverCol.onClick = (btn) -> {
            selectedSlot = slot;
            selectedSoundName = baseName;
            isSelectingSound = true;
            updateSoundRows();
        };
        row.setColumnHoverReplacement(3, java.util.List.of(hoverCol));
        row.addColumn("↺", GuiConstants.ROW_HEADER_HEIGHT, modified ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY, true, true, (idx) -> {
            if (!modified) {
                return;
            }
            ExternalSoundManager.resetSoundSelectionToDefault(presetId, slot.slotId());
            selectedSoundName = ExternalSoundManager.getSelectedSoundBaseName(presetId, slot.slotId());
            cachedSoundDataName = null;
            updateSoundRows();
        });
        configRows.add(row);
    }

    private void addSelectableSoundRow(String label, String baseName, boolean isCustom, String selectedBaseName) {
        boolean isSelected = baseName != null && baseName.equals(selectedBaseName);
        String extension = ExternalSoundManager.getSoundExtensionForPreset(presetId, baseName);
        GDRowRenderer row = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0, false);
        row.addColumn(label, -1, isSelected ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_WHITE, false, false, (idx) -> {
            selectedSoundName = baseName;
            cachedSoundDataName = null;
        });
        row.addColumn(extension, 36, GuiConstants.COLOR_GRAY, false, true, null);
        row.addColumn("▶", 20, GuiConstants.COLOR_WHITE, true, true, (idx) -> {
            selectedSoundName = baseName;
            cachedSoundDataName = null;
            playPreviewSound(baseName);
        });
        if (isSelected) {
            row.addColumn(I18n.get("gd656killicon.client.gui.config.sound.select.selected"), 36, GuiConstants.COLOR_GRAY, true, true, null);
        } else {
            row.addColumn(I18n.get("gd656killicon.client.gui.config.sound.select"), 36, GuiConstants.COLOR_GOLD, true, true, (idx) -> {
                selectSoundForSlot(baseName);
            });
        }
        if (isCustom) {
            row.addColumn("×", GuiConstants.ROW_HEADER_HEIGHT, GuiConstants.COLOR_RED, true, true, (idx) -> {
                boolean deleted = ExternalSoundManager.deleteCustomSound(presetId, baseName);
                if (!deleted) {
                    promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.sound_delete_fail"), PromptDialog.PromptType.ERROR, null);
                    updateSoundRows();
                    return;
                }
                if (baseName.equals(selectedSoundName)) {
                    selectedSoundName = selectedSlot == null
                        ? null
                        : ExternalSoundManager.getSelectedSoundBaseName(presetId, selectedSlot.slotId());
                    cachedSoundData = null;
                    cachedSoundDataName = null;
                }
                updateSoundRows();
            });
        }
        configRows.add(row);
    }

    private void selectSoundForSlot(String baseName) {
        if (selectedSlot == null) {
            return;
        }
        ExternalSoundManager.setSoundSelection(presetId, selectedSlot.slotId(), baseName);
        selectedSoundName = baseName;
        cachedSoundDataName = null;
        updateSoundRows();
    }

    private void playPreviewSlot(ExternalSoundManager.SoundSlotDefinition slot) {
        if (slot == null) {
            return;
        }
        if (slot.group() == ExternalSoundManager.SoundElementGroup.VALORANT) {
            ExternalSoundManager.playConfiguredSound(presetId, slot.slotId(), false, 1.0f);
            return;
        }
        ExternalSoundManager.playConfiguredSound(presetId, slot.slotId());
    }

    private void playPreviewSound(String baseName) {
        if (baseName == null || baseName.isEmpty()) {
            return;
        }
        ExternalSoundManager.playSound(baseName, false, 1.0f);
    }

}
