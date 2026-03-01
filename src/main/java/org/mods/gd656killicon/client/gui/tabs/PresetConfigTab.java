package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.Util;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.gui.ConfigScreenHeader;
import org.mods.gd656killicon.client.gui.screens.ElementConfigBuilder;
import org.mods.gd656killicon.client.gui.screens.ElementConfigBuilderRegistry;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.sounds.ExternalSoundManager;
import org.mods.gd656killicon.client.config.PresetPackManager;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import org.mods.gd656killicon.client.gui.elements.PromptDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.Map;
import java.util.HashMap;
import org.mods.gd656killicon.client.gui.elements.TextInputDialog;
import org.mods.gd656killicon.client.gui.elements.ElementPreview;
import com.google.gson.JsonObject;

public class PresetConfigTab extends ConfigTabContent {
    private enum PanelState {
        HIDDEN,         PEEK,           OPEN        }

    private final Map<String, ElementPreview> previewElements = new HashMap<>();
    private PanelState state = PanelState.HIDDEN;
    private boolean initialized = false;
    private float currentTranslation;
    private float targetTranslation;
    private long lastFrameTime;
    
    private PanelState rightPanelState = PanelState.HIDDEN;
    private float currentRightTranslation;
    private float targetRightTranslation;
    private boolean rightPanelInitialized = false;

    private final List<GDRowRenderer> presetRows = new ArrayList<>();
    private double scrollY = 0;
    private double targetScrollY = 0;
    private static final double SCROLL_SMOOTHING = 15.0;
    private boolean isDragging = false;
    private double lastMouseY = 0;
    private int createRowIndex = -1;

    private static final int TRIGGER_ZONE_WIDTH = GuiConstants.DEFAULT_PADDING; 
    private boolean isEditMode = false;
    private boolean isExportMode = false;
    private Set<String> resetCompletedStates = new HashSet<>();
    private Map<String, Long> resetCompletedTimes = new HashMap<>();
    
    private GDTextRenderer rightTitleRenderer;
    private GDTextRenderer rightInfoRenderer;
    private GDButton rightAddButton;
    private GDButton rightSoundButton;
    
    private boolean isAddElementExpanded = false;
    private List<GDRowRenderer> availableElementRows = new ArrayList<>();
    private double elementListScrollY = 0;
    private double targetElementListScrollY = 0;
    private boolean isElementListDragging = false;
    private double elementListLastMouseY = 0;

    private List<GDRowRenderer> currentElementRows = new ArrayList<>();
    private double contentScrollY = 0;
    private double targetContentScrollY = 0;
    private boolean isContentDragging = false;
    private double contentLastMouseY = 0;
    private boolean leftRequireExitBeforeAutoOpen = false;
    private boolean rightRequireExitBeforeAutoOpen = false;
    private boolean resetPanelPositions = false;
    private int lastScreenWidth = -1;
    private int lastScreenHeight = -1;

    private static class UndoState {
        final String elementId;
        final JsonObject configSnapshot;
        
        UndoState(String elementId, JsonObject config) {
            this.elementId = elementId;
            this.configSnapshot = config.deepCopy();
        }
    }
    
    private final Map<String, java.util.Deque<UndoState>> undoStacks = new HashMap<>();
    private String draggingElementId = null;
    private JsonObject dragStartConfig = null;
    
    private ConfigScreenHeader header;

    public PresetConfigTab(Minecraft minecraft) {
        super(minecraft, "gd656killicon.client.gui.config.tab.preset");
        this.lastFrameTime = Util.getMillis();
        
        rebuildPresetList();
    }

    public void setHeader(ConfigScreenHeader header) {
        this.header = header;
    }

    @Override
    public void onTabOpen() {
        super.onTabOpen();
        state = PanelState.HIDDEN;
        rightPanelState = PanelState.HIDDEN;
        leftRequireExitBeforeAutoOpen = true;
        rightRequireExitBeforeAutoOpen = true;
        resetPanelPositions = true;
        if (ClientConfigManager.shouldShowPresetIntro()) {
            ClientConfigManager.markPresetIntroShown();
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.preset_intro"), PromptDialog.PromptType.INFO, null);
        }
    }

    private void updatePreviews() {
        String currentId = ClientConfigManager.getCurrentPresetId();
        Set<String> elementIds = ElementConfigManager.getElementIds(currentId);
        
        previewElements.keySet().removeIf(id -> !elementIds.contains(id));
        
        for (String elementId : elementIds) {
            ElementPreview preview = previewElements.computeIfAbsent(elementId, ElementPreview::new);
            JsonObject config = ElementConfigManager.getElementConfig(currentId, elementId);
            preview.updateConfig(config);
        }
    }

    private void rebuildPresetList() {
        updatePreviews();
        updateAvailableElementRows();
        updateCurrentElementRows();
        presetRows.clear();
        createRowIndex = -1;
        Set<String> presets = ConfigManager.getPresetIds();
        List<String> sortedPresets = new ArrayList<>(presets);
        Collections.sort(sortedPresets);

        String currentId = ClientConfigManager.getCurrentPresetId();

        for (int i = 0; i < sortedPresets.size(); i++) {
            String id = sortedPresets.get(i);
            GDRowRenderer row = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0, false);
            
            if (i % 2 != 0) row.setBackgroundAlpha(0.15f);
            else row.setBackgroundAlpha(0.3f);

            String displayName = " " + ElementConfigManager.getPresetDisplayName(id);
            int nameColor;
            if (ElementConfigManager.isOfficialPreset(id)) {
                nameColor = isEditMode ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
            } else {
                nameColor = GuiConstants.COLOR_WHITE;
            }
            
            row.addColumn(displayName, -1, nameColor, false, false);
            
            int idWidth = isEditMode ? 60 : 54;
            int idColor = id.equals(currentId) ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_WHITE;
            
            if (isExportMode) {
                 String exportLabel = I18n.get("gd656killicon.client.gui.config.preset.action.export");
                 row.addColumn(exportLabel, idWidth, GuiConstants.COLOR_WHITE, true, true, (btn) -> {
                      openExportDialog(id);
                 });
            } else if (isEditMode) {
                if (ElementConfigManager.isOfficialPreset(id)) {
                    boolean configReset = resetCompletedStates.contains(id + ":reset_all");
                    String label = configReset ? I18n.get("gd656killicon.client.gui.config.preset.action.reset_config.done")
                        : I18n.get("gd656killicon.client.gui.config.preset.action.reset_config");
                    row.addColumn(label, 60, configReset ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_WHITE, true, true, configReset ? null : (btn) -> {
                        ConfigManager.resetPresetConfig(id);
                        ExternalTextureManager.markPendingTextureReset(id);
                        ExternalSoundManager.markPendingSoundReset(id);
                        resetCompletedStates.add(id + ":reset_all");
                        resetCompletedTimes.put(id + ":reset_all", System.currentTimeMillis());
                        rebuildPresetList();
                    });
                } else {
                    row.addColumn(I18n.get("gd656killicon.client.gui.config.preset.action.edit_id"), 30, GuiConstants.COLOR_GRAY, true, true, (btn) -> {
                        openRenameIdDialog(id);
                    });
                    row.addColumn(I18n.get("gd656killicon.client.gui.config.preset.action.delete"), 30, GuiConstants.COLOR_RED, true, true, (btn) -> {
                        if (ElementConfigManager.deletePreset(id)) {
                            if (id.equals(ClientConfigManager.getCurrentPresetId())) {
                                ConfigManager.setCurrentPresetId("00001");
                            }
                            rebuildPresetList();
                        }
                    });
                }
            } else {
                row.addColumn(id, idWidth, idColor, true, true);
                
                if (!isEditMode) {
                    
                    row.getColumn(1).onClick = (btn) -> {
                         if (btn == 0) {
                            ConfigManager.setCurrentPresetId(id);
                            rebuildPresetList();
                         }
                    };
                } else {
                    
                    if (!ElementConfigManager.isOfficialPreset(id)) {
                        List<GDRowRenderer.Column> nameHoverCols = new ArrayList<>();
                        nameHoverCols.add(createActionColumn(I18n.get("gd656killicon.client.gui.config.preset.action.edit_name_tooltip"), GuiConstants.COLOR_GRAY, (btn) -> {
                            openRenameDialog(id);
                        }));
                        row.setColumnHoverReplacement(0, nameHoverCols);
                    }
                }
            }

            presetRows.add(row);
        }
        
        GDRowRenderer createRow = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, false);
        createRow.addColumn(I18n.get("gd656killicon.client.gui.config.preset.create_new"), -1, GuiConstants.COLOR_GREEN, true, true, (btn) -> {
            String newId = ElementConfigManager.createNewPreset();
            ConfigManager.setCurrentPresetId(newId);             rebuildPresetList();
        });
        createRowIndex = presetRows.size();
        presetRows.add(createRow);
        
        GDRowRenderer toggleRow = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, false);
        String toggleText = isEditMode ? I18n.get("gd656killicon.client.gui.config.preset.exit_edit_mode") : I18n.get("gd656killicon.client.gui.config.preset.enter_edit_mode");
        int toggleColor = isEditMode ? GuiConstants.COLOR_WHITE : GuiConstants.COLOR_SKY_BLUE;
        
        toggleRow.addColumn(toggleText, -1, toggleColor, true, true, (btn) -> {
            isEditMode = !isEditMode;
            if (isEditMode) isExportMode = false;
            rebuildPresetList();
        });
        
        presetRows.add(toggleRow);

        GDRowRenderer exportToggleRow = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, false);
        String exportToggleText = isExportMode ? I18n.get("gd656killicon.client.gui.config.preset.exit_export_mode") : I18n.get("gd656killicon.client.gui.config.preset.enter_export_mode");
        int exportToggleColor = isExportMode ? GuiConstants.COLOR_WHITE : GuiConstants.COLOR_SKY_BLUE;
        
        exportToggleRow.addColumn(exportToggleText, -1, exportToggleColor, true, true, (btn) -> {
            isExportMode = !isExportMode;
            if (isExportMode) isEditMode = false;
            rebuildPresetList();
        });
        presetRows.add(exportToggleRow);
    }

    private void openExportDialog(String presetId) {
        String currentName = ElementConfigManager.getPresetDisplayName(presetId);
        textInputDialog.show(currentName, I18n.get("gd656killicon.client.gui.config.export.dialog.title"), (exportName) -> {
             boolean success = PresetPackManager.exportPreset(presetId, exportName);
             if (success) {
                 promptDialog.show(I18n.get("gd656killicon.client.config.export.success.prompt", exportName), PromptDialog.PromptType.SUCCESS, () -> {});
                 openExportFolder();
             }
        });
    }

    private void openExportFolder() {
        try {
            java.io.File dir = PresetPackManager.getExportDir().toFile();
            net.minecraft.Util.getPlatform().openFile(dir);
        } catch (Exception ignored) {
            try {
                new ProcessBuilder("explorer", PresetPackManager.getExportDir().toAbsolutePath().toString()).start();
            } catch (Exception ignoredAgain) {
            }
        }
    }

    private void openImportDuplicateDialog(java.io.File file, String originalId) {
         String randomId = PresetPackManager.generateRandomId();
         String prompt = I18n.get("gd656killicon.client.config.import.duplicate_id", originalId);
         
         textInputDialog.show(randomId, prompt, (newId) -> {
             if (PresetPackManager.importPreset(file, newId)) {
                 rebuildPresetList();
                 promptDialog.show(I18n.get("gd656killicon.client.config.import.success", newId), PromptDialog.PromptType.SUCCESS, () -> {});
             }
         }, createPresetIdValidator(null, false));
    }

    @Override
    public void onFilesDrop(java.util.List<java.nio.file.Path> paths) {
        for (java.nio.file.Path path : paths) {
            java.io.File file = path.toFile();
            String name = file.getName().toLowerCase();
            if (name.endsWith(".gdpack") || name.endsWith(".zip")) {
                 String status = PresetPackManager.checkImport(file);
                 if ("valid".equals(status)) {
                      String originalId = PresetPackManager.getPresetIdFromPack(file);
                      
                      if (originalId != null && ElementConfigManager.getActivePresets().containsKey(originalId)) {
                           openImportDuplicateDialog(file, originalId);
                      } else {
                           String targetId = originalId != null ? originalId : PresetPackManager.generateRandomId();
                           if (ElementConfigManager.getActivePresets().containsKey(targetId)) {
                                openImportDuplicateDialog(file, targetId);
                           } else {
                                if (PresetPackManager.importPreset(file, targetId)) {
                                    rebuildPresetList();
                                    promptDialog.show(I18n.get("gd656killicon.client.config.import.success", targetId), PromptDialog.PromptType.SUCCESS, () -> {});
                                }
                           }
                      }
                 } else {
                      ClientMessageLogger.chatError("gd656killicon.client.config.import.invalid_format");
                 }
            }
        }
    }
    
    private void updateAvailableElementRows() {
        availableElementRows.clear();
        String currentPresetId = ClientConfigManager.getCurrentPresetId();
        Set<String> availableTypes = ElementConfigManager.getAvailableElementTypes(currentPresetId);
        
        List<String> sortedTypes = new ArrayList<>(availableTypes);
        Collections.sort(sortedTypes);

        for (int i = 0; i < sortedTypes.size(); i++) {
            String type = sortedTypes.get(i);
            GDRowRenderer row = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0, false);
            
            if (i % 2 != 0) row.setBackgroundAlpha(0.15f);
            else row.setBackgroundAlpha(0.3f);

            List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
            texts.add(new GDTextRenderer.ColoredText(" [" + type + "] ", GuiConstants.COLOR_GRAY));
            
            String nameKey = "gd656killicon.element.name." + type.replace("/", ".");
            String name = I18n.exists(nameKey) ? I18n.get(nameKey) : type;
            texts.add(new GDTextRenderer.ColoredText(name, GuiConstants.COLOR_GOLD));
            
            row.addColoredColumn(texts, -1, false, false, null);
            
            row.addColumn(I18n.get("gd656killicon.client.gui.config.preset.add_button"), 40, GuiConstants.COLOR_WHITE, true, true, (btn) -> {
                ElementConfigManager.addElement(currentPresetId, type);
                updateAvailableElementRows();
                updateCurrentElementRows();
                updatePreviews();
            });
            
            availableElementRows.add(row);
        }
    }

    private GDRowRenderer.Column createActionColumn(String text, int color, java.util.function.Consumer<Integer> onClick) {
        GDRowRenderer.Column col = new GDRowRenderer.Column();
        col.text = text;
        col.color = color;
        col.isCentered = true;
        col.onClick = onClick;
        col.isDarker = true;         return col;
    }

    private GDRowRenderer.Column createSpacerColumn() {
        GDRowRenderer.Column col = new GDRowRenderer.Column();
        col.text = "";
        col.color = GuiConstants.COLOR_GRAY;
        col.isCentered = true;
        col.onClick = null;
        col.isDarker = true;
        return col;
    }

    private void openRenameDialog(String presetId) {
        String currentName = ElementConfigManager.getPresetDisplayName(presetId);
        textInputDialog.show(currentName, I18n.get("gd656killicon.client.gui.config.dialog.enter_text"), (newName) -> {
            ElementConfigManager.setPresetDisplayName(presetId, newName);
            rebuildPresetList();
        });
    }

    private void openRenameIdDialog(String currentId) {
        textInputDialog.show(currentId, I18n.get("gd656killicon.client.gui.config.dialog.enter_text"), (newId) -> {
            if (ElementConfigManager.renamePresetId(currentId, newId)) {
                if (currentId.equals(ClientConfigManager.getCurrentPresetId())) {
                     ConfigManager.setCurrentPresetId(newId);
                }
                rebuildPresetList();
            }
        }, createPresetIdValidator(currentId, true));
    }

    private java.util.function.Predicate<String> createPresetIdValidator(String currentId, boolean allowSame) {
        return (input) -> {
            if (input == null || !input.matches("^\\d{5}$")) return false;
            if (allowSame && currentId != null && input.equals(currentId)) return true;
            return !ElementConfigManager.presetExists(input);
        };
    }

    @Override
    public float getSidebarOffset() {
        return currentTranslation;
    }

    @Override
    public boolean isSidebarFloating() {
        return true;
    }

    @Override
    protected void updateConfigRowsLayout(int screenWidth, int screenHeight) {
        int contentX = TRIGGER_ZONE_WIDTH;
        
        int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
        int contentWidth = screenWidth - contentX - GuiConstants.DEFAULT_PADDING;
        
        int currentY = contentY;
        for (int i = 0; i < configRows.size(); i++) {
            GDRowRenderer row = configRows.get(i);
            int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
            
            row.setBounds(contentX, currentY, contentX + contentWidth, currentY + rowHeight);
            
            if (i % 2 != 0) {
                row.setBackgroundAlpha(0.15f);
            } else {
                row.setBackgroundAlpha(0.3f);
            }
            
            currentY += rowHeight + 1;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int panelWidth = area1Right + GuiConstants.DEFAULT_PADDING;
        
        updateResetButtonState();
        updatePresetResetState();

        boolean isDialogVisible = textInputDialog.isVisible() || promptDialog.isVisible();
        boolean isPanelOpen = (state == PanelState.OPEN);
        boolean isRightPanelOpen = (rightPanelState == PanelState.OPEN);

        int panelMouseX = isDialogVisible ? -10000 : mouseX;
        int panelMouseY = isDialogVisible ? -10000 : mouseY;

        int bgMouseX = (isDialogVisible || isPanelOpen || isRightPanelOpen) ? -10000 : mouseX;
        int bgMouseY = (isDialogVisible || isPanelOpen || isRightPanelOpen) ? -10000 : mouseY;

        long currentTime = Util.getMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000.0f;         lastFrameTime = currentTime;
        
        boolean sizeChanged = screenWidth != lastScreenWidth || screenHeight != lastScreenHeight;
        if (sizeChanged) {
            lastScreenWidth = screenWidth;
            lastScreenHeight = screenHeight;
            resetPanelPositions = true;
            leftRequireExitBeforeAutoOpen = true;
            rightRequireExitBeforeAutoOpen = true;
        }

        boolean rightPanelVisible = rightPanelState != PanelState.HIDDEN || currentRightTranslation < panelWidth - 0.5f;
        boolean leftPanelVisible = state != PanelState.HIDDEN || currentTranslation > -panelWidth + 0.5f;

        if (state == PanelState.HIDDEN) {
            targetTranslation = -panelWidth;
            if (panelMouseX > TRIGGER_ZONE_WIDTH) {
                leftRequireExitBeforeAutoOpen = false;
            }
            if (!isRightPanelOpen && panelMouseX >= 0 && panelMouseX <= TRIGGER_ZONE_WIDTH && !leftRequireExitBeforeAutoOpen) {
                state = PanelState.OPEN;
            }
        } else if (state == PanelState.PEEK) {
            targetTranslation = TRIGGER_ZONE_WIDTH - panelWidth;
            if (panelMouseX <= TRIGGER_ZONE_WIDTH) {
                state = PanelState.OPEN;
            } else if (panelMouseX > TRIGGER_ZONE_WIDTH) {
                state = PanelState.HIDDEN;
            }
        } else if (state == PanelState.OPEN) {
            targetTranslation = 0;
            if (!rightPanelVisible && panelMouseX > panelWidth) {
                state = PanelState.HIDDEN;
                isEditMode = false;
                isExportMode = false;
            }
        }
        
        if (!initialized) {
             currentTranslation = -panelWidth;
             initialized = true;
        }

        if (resetPanelPositions) {
            if (state == PanelState.OPEN) {
                currentTranslation = 0;
                targetTranslation = 0;
            } else if (state == PanelState.PEEK) {
                currentTranslation = TRIGGER_ZONE_WIDTH - panelWidth;
                targetTranslation = TRIGGER_ZONE_WIDTH - panelWidth;
            } else {
                currentTranslation = -panelWidth;
                targetTranslation = -panelWidth;
            }
        }

        float speed = 15.0f;
        if (Math.abs(targetTranslation - currentTranslation) > 0.1f) {
            currentTranslation += (targetTranslation - currentTranslation) * speed * deltaTime;
        } else {
            currentTranslation = targetTranslation;
        }

        int rightPanelWidth = panelWidth;
        int rightTriggerZoneStart = screenWidth - TRIGGER_ZONE_WIDTH;

        if (rightPanelState == PanelState.HIDDEN) {
            targetRightTranslation = rightPanelWidth;
            if (panelMouseX < rightTriggerZoneStart) {
                rightRequireExitBeforeAutoOpen = false;
            }
            if (!isPanelOpen && panelMouseX >= rightTriggerZoneStart && panelMouseX <= screenWidth && !rightRequireExitBeforeAutoOpen) {
                rightPanelState = PanelState.OPEN;
            }
        } else if (rightPanelState == PanelState.PEEK) {
            targetRightTranslation = rightPanelWidth - TRIGGER_ZONE_WIDTH;
            if (panelMouseX >= rightTriggerZoneStart) {
                rightPanelState = PanelState.OPEN;
            } else if (panelMouseX < rightTriggerZoneStart) {
                rightPanelState = PanelState.HIDDEN;
            }
        } else if (rightPanelState == PanelState.OPEN) {
            targetRightTranslation = 0;
            int rightPanelX = screenWidth - panelWidth;
            if (!leftPanelVisible && panelMouseX < rightPanelX) {
                rightPanelState = PanelState.HIDDEN;
            }
        }

        if (!rightPanelInitialized) {
             currentRightTranslation = rightPanelWidth;
             rightPanelInitialized = true;
        }

        if (resetPanelPositions) {
            if (rightPanelState == PanelState.OPEN) {
                currentRightTranslation = 0;
                targetRightTranslation = 0;
            } else if (rightPanelState == PanelState.PEEK) {
                currentRightTranslation = rightPanelWidth - TRIGGER_ZONE_WIDTH;
                targetRightTranslation = rightPanelWidth - TRIGGER_ZONE_WIDTH;
            } else {
                currentRightTranslation = rightPanelWidth;
                targetRightTranslation = rightPanelWidth;
            }
            resetPanelPositions = false;
        }

        if (Math.abs(targetRightTranslation - currentRightTranslation) > 0.1f) {
            currentRightTranslation += (targetRightTranslation - currentRightTranslation) * speed * deltaTime;
        } else {
            currentRightTranslation = targetRightTranslation;
        }

        if (!configRows.isEmpty()) {
            for (GDRowRenderer row : configRows) {
                row.render(guiGraphics, panelMouseX, panelMouseY, partialTick);
            }
        }

        List<ElementPreview> renderList = new ArrayList<>(previewElements.values());
        
        for (ElementPreview preview : renderList) {
            preview.updatePosition(screenWidth, screenHeight);
        }
        
        renderList.sort((a, b) -> {
            boolean aIsDragged = draggingElementId != null && a.getElementId().equals(draggingElementId);
            boolean bIsDragged = draggingElementId != null && b.getElementId().equals(draggingElementId);
            
            if (aIsDragged && !bIsDragged) return 1;
            if (!aIsDragged && bIsDragged) return -1;
            
            return Integer.compare(a.getZIndex(), b.getZIndex());
        });
        
        ElementPreview hoveredElement = null;
        
        if (draggingElementId != null && previewElements.containsKey(draggingElementId)) {
            hoveredElement = previewElements.get(draggingElementId);
        } else {
            for (int i = renderList.size() - 1; i >= 0; i--) {
                ElementPreview preview = renderList.get(i);
                if (preview.isMouseOver(bgMouseX, bgMouseY)) {
                    hoveredElement = preview;
                    break;                 }
            }
        }
        
        for (ElementPreview preview : renderList) {
            preview.render(guiGraphics, partialTick, screenWidth, preview == hoveredElement, bgMouseX, bgMouseY);
        }

        if (currentElementRows.isEmpty() && !isDialogVisible) {
            net.minecraft.client.gui.Font font = minecraft.font;
            String hintText = I18n.get("gd656killicon.client.gui.config.preset.hint_add_element");
            int goldBarBottom = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT;
            float leftVisibleWidth = Math.max(0f, currentTranslation + panelWidth);
            float rightVisibleWidth = Math.max(0f, panelWidth - currentRightTranslation);
            float area2X1 = Math.max((float)GuiConstants.DEFAULT_PADDING, leftVisibleWidth);
            float area2Y1 = (float)goldBarBottom + GuiConstants.DEFAULT_PADDING;
            float area2X2 = Math.min((float)screenWidth - GuiConstants.DEFAULT_PADDING, screenWidth - rightVisibleWidth);
            float area2Y2 = (float)screenHeight - GuiConstants.DEFAULT_PADDING;
            float padding = (float)GuiConstants.DEFAULT_PADDING;
            float maxWidthFloat = Math.max(0f, area2X2 - area2X1 - 2 * padding);
            int maxWidth = (int)maxWidthFloat;
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(hintText), maxWidth);
            float totalHeight = lines.size() * font.lineHeight;
            float startY = area2Y1 + (area2Y2 - area2Y1 - totalHeight) / 2.0f;
            float centerX = (area2X1 + area2X2) / 2.0f;
            
            float currentY = startY;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(centerX, currentY, 0);
                guiGraphics.drawCenteredString(font, line, 0, 0, GuiConstants.COLOR_GRAY);
                guiGraphics.pose().popPose();
                currentY += font.lineHeight;
            }
        }

        int top = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT;
        
        float visibleWidth = currentTranslation + panelWidth;
        
        
        if (visibleWidth > 0) {
            guiGraphics.enableScissor(0, top, (int)Math.ceil(visibleWidth), screenHeight);
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(currentTranslation, 0, 0);
            
            guiGraphics.fill(0, top, panelWidth, screenHeight, GuiConstants.COLOR_BG);

            if (titleRenderer != null) titleRenderer.render(guiGraphics, partialTick);
            if (subtitleRenderer != null) subtitleRenderer.render(guiGraphics, partialTick);
            
            GDRowRenderer hoveredRow = null;
            for (GDRowRenderer row : configRows) {
                if (row.isHovered(panelMouseX, panelMouseY)) {
                    hoveredRow = row;
                    break;
                }
            }
            if (hoveredRow != null && hoveredRow.getHoverTitle() != null) {
                renderDynamicDescription(guiGraphics, hoveredRow, partialTick);
            }
            
            guiGraphics.pose().popPose();
            guiGraphics.disableScissor(); 
            renderPresetList(guiGraphics, panelMouseX, panelMouseY, partialTick, panelWidth, screenHeight, deltaTime);

            int buttonsScissorWidth = (int)Math.ceil(visibleWidth);
            guiGraphics.enableScissor(0, top, buttonsScissorWidth, screenHeight);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(currentTranslation, 0, 0);
            int translatedMouseX = (int)(panelMouseX - currentTranslation);
            renderSideButtonsInPanel(guiGraphics, translatedMouseX, panelMouseY, partialTick, screenWidth, screenHeight);
            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();
        }

        float rightVisibleWidth = rightPanelWidth - currentRightTranslation;
        int rightPanelX = screenWidth - rightPanelWidth;
        
        if (rightVisibleWidth > 0) {
            int scissorX = (int)(screenWidth - rightVisibleWidth);
            guiGraphics.enableScissor(scissorX, top, screenWidth, screenHeight);
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(currentRightTranslation, 0, 0);
            
            guiGraphics.fill(rightPanelX, top, rightPanelX + rightPanelWidth, screenHeight, GuiConstants.COLOR_BG);
            
            guiGraphics.pose().popPose(); 
            int contentX = (int)(rightPanelX + currentRightTranslation + GuiConstants.DEFAULT_PADDING);
            int contentY = top + GuiConstants.DEFAULT_PADDING;
            int contentWidth = rightPanelWidth - 2 * GuiConstants.DEFAULT_PADDING;
            
            int lineHeight = 9;             int textSpacing = 4;
            int borderPadding = 1; 
            int titleBoxHeight = borderPadding + lineHeight + textSpacing + lineHeight + borderPadding;
            
            int borderColor = (GuiConstants.COLOR_GRAY & 0x00FFFFFF) | (0x80 << 24);             
            guiGraphics.fill(contentX, contentY, contentX + contentWidth, contentY + 1, borderColor);
            guiGraphics.fill(contentX, contentY + titleBoxHeight - 1, contentX + contentWidth, contentY + titleBoxHeight, borderColor);
            guiGraphics.fill(contentX, contentY + 1, contentX + 1, contentY + titleBoxHeight - 1, borderColor);
            guiGraphics.fill(contentX + contentWidth - 1, contentY + 1, contentX + contentWidth, contentY + titleBoxHeight - 1, borderColor);
            
            if (rightTitleRenderer == null) {
                rightTitleRenderer = new GDTextRenderer(I18n.get("gd656killicon.client.gui.config.preset.element_control_title"), 
                    contentX + borderPadding, 
                    contentY + borderPadding, 
                    contentX + contentWidth - borderPadding, 
                    contentY + borderPadding + lineHeight, 
                    1.0f, GuiConstants.COLOR_GOLD, false);
            } else {
                rightTitleRenderer.setX1(contentX + borderPadding);
                rightTitleRenderer.setY1(contentY + borderPadding);
                rightTitleRenderer.setX2(contentX + contentWidth - borderPadding);
                rightTitleRenderer.setY2(contentY + borderPadding + lineHeight);
            }
            rightTitleRenderer.renderInternal(guiGraphics, partialTick, true, deltaTime);
            
            String currentId = ClientConfigManager.getCurrentPresetId();
            String currentName = ElementConfigManager.getPresetDisplayName(currentId);
            
            List<GDTextRenderer.ColoredText> infoTexts = new ArrayList<>();
            infoTexts.add(new GDTextRenderer.ColoredText(I18n.get("gd656killicon.client.gui.config.element.current_preset"), GuiConstants.COLOR_WHITE));
            infoTexts.add(new GDTextRenderer.ColoredText("[" + currentId + "]", GuiConstants.COLOR_GRAY));
            infoTexts.add(new GDTextRenderer.ColoredText(currentName, GuiConstants.COLOR_GOLD));
            
            int infoY = contentY + borderPadding + lineHeight + textSpacing;
            if (rightInfoRenderer == null) {
                rightInfoRenderer = new GDTextRenderer(infoTexts,
                    contentX + borderPadding,
                    infoY,
                    contentX + contentWidth - borderPadding,
                    infoY + lineHeight,
                    1.0f, false);
            } else {
                rightInfoRenderer.setX1(contentX + borderPadding);
                rightInfoRenderer.setY1(infoY);
                rightInfoRenderer.setX2(contentX + contentWidth - borderPadding);
                rightInfoRenderer.setY2(infoY + lineHeight);
                rightInfoRenderer.setColoredTexts(infoTexts);             }
            rightInfoRenderer.renderInternal(guiGraphics, partialTick, true, deltaTime);
            
            int buttonHeight = 17;
            int buttonPadding = 1;
            int buttonSpacing = 1;
            int totalButtonsHeight = buttonHeight * 2 + buttonSpacing;
            int bottomBoxHeight;
            
            if (isAddElementExpanded) {
                bottomBoxHeight = totalButtonsHeight + 3 * GuiConstants.ROW_HEADER_HEIGHT + 4;
            } else {
                bottomBoxHeight = buttonPadding + totalButtonsHeight + buttonPadding;
            }
            
            int bottomBoxY = screenHeight - GuiConstants.DEFAULT_PADDING - bottomBoxHeight;
            
            guiGraphics.fill(contentX, bottomBoxY, contentX + contentWidth, bottomBoxY + 1, borderColor);
            guiGraphics.fill(contentX, bottomBoxY + bottomBoxHeight - 1, contentX + contentWidth, bottomBoxY + bottomBoxHeight, borderColor);
            guiGraphics.fill(contentX, bottomBoxY + 1, contentX + 1, bottomBoxY + bottomBoxHeight - 1, borderColor);
            guiGraphics.fill(contentX + contentWidth - 1, bottomBoxY + 1, contentX + contentWidth, bottomBoxY + bottomBoxHeight - 1, borderColor);
            
            if (isAddElementExpanded) {
                int listTop = bottomBoxY + 1;
                int soundButtonY = bottomBoxY + bottomBoxHeight - buttonPadding - buttonHeight;
                int addButtonY = soundButtonY - buttonSpacing - buttonHeight;
                int listBottom = addButtonY - 2;
                int listHeight = listBottom - listTop;
                
                updateElementListScroll(deltaTime, mouseY, listHeight);
                
                guiGraphics.enableScissor(contentX + 1, listTop, contentX + contentWidth - 1, listBottom);
                
                if (availableElementRows.isEmpty()) {
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, I18n.get("gd656killicon.client.gui.config.preset.all_elements_added"), contentX + contentWidth / 2, listTop + (listBottom - listTop) / 2 - 4, GuiConstants.COLOR_GRAY);
                } else {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, (float)-elementListScrollY, 0);
    
                    int currentY = listTop;
                    for (GDRowRenderer row : availableElementRows) {
                        row.setX1(contentX + 1);
                        row.setY1(currentY);
                        row.setX2(contentX + contentWidth - 1);
                        row.setY2(currentY + GuiConstants.ROW_HEADER_HEIGHT);
                        
                        double scrolledY = currentY - elementListScrollY;
                        if (scrolledY + GuiConstants.ROW_HEADER_HEIGHT > listTop && scrolledY < listBottom) {
                            row.render(guiGraphics, mouseX, (int)(mouseY + elementListScrollY), partialTick);
                        }
                        currentY += GuiConstants.ROW_HEADER_HEIGHT + 1;                     }
                    
                    guiGraphics.pose().popPose();
                }
                guiGraphics.disableScissor();
                
                guiGraphics.enableScissor(scissorX, top, screenWidth, screenHeight);
            }
            
            int soundButtonY = bottomBoxY + bottomBoxHeight - buttonPadding - buttonHeight;
            int addButtonY = soundButtonY - buttonSpacing - buttonHeight;
            
            if (rightAddButton == null) {
                rightAddButton = new GDButton(
                    contentX + buttonPadding, 
                    addButtonY, 
                    contentWidth - 2 * buttonPadding, 
                    buttonHeight, 
                    Component.translatable("gd656killicon.client.gui.config.preset.add_element"), 
                    (btn) -> { 
                        isAddElementExpanded = !isAddElementExpanded;
                        if (isAddElementExpanded) {
                            btn.setMessage(Component.translatable("gd656killicon.client.gui.config.preset.close_selection"));
                            updateAvailableElementRows();
                        } else {
                            btn.setMessage(Component.translatable("gd656killicon.client.gui.config.preset.add_element"));
                        }
                    }
                );
            } else {
                rightAddButton.setX(contentX + buttonPadding);
                rightAddButton.setY(addButtonY);
                rightAddButton.setWidth(contentWidth - 2 * buttonPadding);
            }
            rightAddButton.render(guiGraphics, mouseX, mouseY, partialTick);
            
            if (rightSoundButton == null) {
                rightSoundButton = new GDButton(
                    contentX + buttonPadding,
                    soundButtonY,
                    contentWidth - 2 * buttonPadding,
                    buttonHeight,
                    Component.translatable("gd656killicon.client.gui.config.preset.configure_sound"),
                    (btn) -> {
                        if (header != null) {
                            String currentPresetId = ClientConfigManager.getCurrentPresetId();
                            header.setOverrideContent(new SoundConfigContent(minecraft, currentPresetId, () -> {
                                header.setOverrideContent(null);
                            }));
                        }
                    }
                );
            } else {
                rightSoundButton.setX(contentX + buttonPadding);
                rightSoundButton.setY(soundButtonY);
                rightSoundButton.setWidth(contentWidth - 2 * buttonPadding);
            }
            rightSoundButton.render(guiGraphics, mouseX, mouseY, partialTick);
            
            int middleBoxY = contentY + titleBoxHeight + GuiConstants.DEFAULT_PADDING;
            int middleBoxBottom = bottomBoxY - GuiConstants.DEFAULT_PADDING;
            int middleBoxHeight = middleBoxBottom - middleBoxY;
            
            if (middleBoxHeight > 0) {
                guiGraphics.fill(contentX, middleBoxY, contentX + contentWidth, middleBoxY + 1, borderColor);
                guiGraphics.fill(contentX, middleBoxBottom - 1, contentX + contentWidth, middleBoxBottom, borderColor);
                guiGraphics.fill(contentX, middleBoxY + 1, contentX + 1, middleBoxBottom - 1, borderColor);
                guiGraphics.fill(contentX + contentWidth - 1, middleBoxY + 1, contentX + contentWidth, middleBoxBottom - 1, borderColor);

                int contentListTop = middleBoxY + 1;
                int contentListBottom = middleBoxBottom - 1;
                int contentListHeight = contentListBottom - contentListTop;
                
                if (!currentElementRows.isEmpty()) {
                    updateContentScroll(deltaTime, mouseY, contentListHeight);
                    
                    guiGraphics.enableScissor(contentX + 1, contentListTop, contentX + contentWidth - 1, contentListBottom);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, (float)-contentScrollY, 0);
                    
                    int currentY = contentListTop;
                    for (GDRowRenderer row : currentElementRows) {
                        row.setX1(contentX + 1);
                        row.setY1(currentY);
                        row.setX2(contentX + contentWidth - 1);
                        row.setY2(currentY + GuiConstants.ROW_HEADER_HEIGHT);
                        
                        double scrolledY = currentY - contentScrollY;
                        if (scrolledY + GuiConstants.ROW_HEADER_HEIGHT > contentListTop && scrolledY < contentListBottom) {
                            row.render(guiGraphics, mouseX, (int)(mouseY + contentScrollY), partialTick);
                        }
                        currentY += GuiConstants.ROW_HEADER_HEIGHT + 1;
                    }
                    
                    guiGraphics.pose().popPose();
                    guiGraphics.disableScissor();
                    
                    guiGraphics.enableScissor(scissorX, top, screenWidth, screenHeight);
                } else {
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gd656killicon.client.gui.config.preset.no_elements"), contentX + contentWidth / 2, middleBoxY + middleBoxHeight / 2 - 4, GuiConstants.COLOR_GRAY);
                }
            }

            guiGraphics.disableScissor();
        }

        textInputDialog.render(guiGraphics, mouseX, mouseY, partialTick);
        promptDialog.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderSideButtonsInPanel(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight) {
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int buttonY = screenHeight - GuiConstants.DEFAULT_PADDING - GuiConstants.ROW_HEADER_HEIGHT - 1 - GuiConstants.ROW_HEADER_HEIGHT;
        
        int totalWidth = area1Right - GuiConstants.DEFAULT_PADDING;
        int buttonWidth = (totalWidth - 1) / 2;
        int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int x1 = GuiConstants.DEFAULT_PADDING;

        if (resetButton == null) {
            resetButton = new GDButton(x1, buttonY, buttonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.button.reset"), (btn) -> {
                if (isResetConfirming) {
                    long elapsed = System.currentTimeMillis() - resetConfirmTime;
                    if (elapsed >= 3000) {
                        ConfigManager.resetFull();
                        ConfigManager.discardChanges();
                        
                        if (minecraft.screen != null) {
                            minecraft.screen.onClose();
                        }

                        isResetConfirming = false;
                        btn.setMessage(Component.translatable("gd656killicon.client.gui.button.reset"));
                        btn.setTextColor(GuiConstants.COLOR_WHITE);
                    }
                } else {
                    isResetConfirming = true;
                    resetConfirmTime = System.currentTimeMillis();
                    btn.setMessage(Component.translatable("gd656killicon.client.gui.config.button.confirm_reset_time", 3));
                    btn.setTextColor(GuiConstants.COLOR_DARK_RED);
                }
            });
        }
        
        resetButton.setX(x1);
        resetButton.setY(buttonY);
        resetButton.setWidth(buttonWidth);
        resetButton.setHeight(buttonHeight);
        resetButton.render(guiGraphics, mouseX, mouseY, partialTick);

        if (cancelButton == null) {
            cancelButton = new GDButton(x1 + buttonWidth + 1, buttonY, buttonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.button.cancel"), (btn) -> {
                if (minecraft.screen != null) {
                    minecraft.screen.onClose();
                }
            });
        }
        
        cancelButton.setX(x1 + buttonWidth + 1);
        cancelButton.setY(buttonY);
        cancelButton.setWidth(buttonWidth);
        cancelButton.setHeight(buttonHeight);
        cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updatePresetResetState() {
        if (resetCompletedStates.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, Long> entry : resetCompletedTimes.entrySet()) {
            if (now - entry.getValue() > 3000) {
                expired.add(entry.getKey());
            }
        }
        if (!expired.isEmpty()) {
            for (String key : expired) {
                resetCompletedTimes.remove(key);
                resetCompletedStates.remove(key);
            }
            rebuildPresetList();
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (promptDialog.isVisible()) {
            return promptDialog.charTyped(codePoint, modifiers);
        }
        if (textInputDialog.isVisible()) {
            return textInputDialog.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (promptDialog.isVisible()) {
            return promptDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (textInputDialog.isVisible()) {
            return textInputDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (Screen.hasControlDown() && (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_Z)) {
            if (tryUndo()) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (promptDialog.isVisible()) {
            return promptDialog.mouseClicked(mouseX, mouseY, button);
        }
        if (textInputDialog.isVisible()) {
            if (textInputDialog.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int panelWidth = area1Right + GuiConstants.DEFAULT_PADDING;
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        if (state == PanelState.HIDDEN) {
            if (mouseX <= TRIGGER_ZONE_WIDTH) {
                state = PanelState.OPEN;
                leftRequireExitBeforeAutoOpen = false;
                return true;
            }
        } else if (state == PanelState.PEEK) {
            if (mouseX <= TRIGGER_ZONE_WIDTH) {
                state = PanelState.OPEN;
                return true;
            }
        } else if (state == PanelState.OPEN) {
            if (mouseX <= TRIGGER_ZONE_WIDTH) {
                state = PanelState.HIDDEN;
                leftRequireExitBeforeAutoOpen = true;
                isEditMode = false;
                isExportMode = false;
                return true;
            }
            if (mouseX > panelWidth) {
                state = PanelState.HIDDEN;
                isEditMode = false;
                isExportMode = false;
                return true;
            }
            if (resetButton != null && resetButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (cancelButton != null && cancelButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            
            int startY = this.area1Bottom + GuiConstants.DEFAULT_PADDING;
            int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
            int endY = screenHeight - GuiConstants.DEFAULT_PADDING - buttonHeight - 1 - buttonHeight - GuiConstants.DEFAULT_PADDING;
            
            if (mouseY >= startY && mouseY <= endY && mouseX <= panelWidth) {
                float translatedMouseX = (float)mouseX - currentTranslation;
                double adjustedMouseY = mouseY + scrollY;
                
                for (GDRowRenderer row : presetRows) {
                    if (row.mouseClicked(translatedMouseX, adjustedMouseY, button)) {
                        return true;
                    }
                }

                isDragging = true;
                lastMouseY = mouseY;
                return true;
            }

            if (mouseX <= panelWidth) {
                return true;
            }
        }

        if (rightPanelState == PanelState.HIDDEN) {
            int rightTriggerZoneStart = screenWidth - TRIGGER_ZONE_WIDTH;
            if (mouseX >= rightTriggerZoneStart) {
                rightPanelState = PanelState.OPEN;
                rightRequireExitBeforeAutoOpen = false;
                return true;
            }
        } else if (rightPanelState == PanelState.PEEK) {
            int rightTriggerZoneStart = screenWidth - TRIGGER_ZONE_WIDTH;
            if (mouseX >= rightTriggerZoneStart) {
                rightPanelState = PanelState.OPEN;
                return true;
            }
        } else if (rightPanelState == PanelState.OPEN) {
            int rightPanelX = screenWidth - panelWidth;
            int rightTriggerZoneStart = screenWidth - TRIGGER_ZONE_WIDTH;
            if (mouseX >= rightTriggerZoneStart) {
                rightPanelState = PanelState.HIDDEN;
                rightRequireExitBeforeAutoOpen = true;
                return true;
            }
            
            if (mouseX < rightPanelX) {
                rightPanelState = PanelState.HIDDEN;
                return true;
            }
            
            int contentX = rightPanelX + GuiConstants.DEFAULT_PADDING;

            int buttonHeight = 17;
            int buttonPadding = 1;
            int buttonSpacing = 1;
            int totalButtonsHeight = buttonHeight * 2 + buttonSpacing;
            int bottomBoxHeight;
            if (isAddElementExpanded) {
                bottomBoxHeight = totalButtonsHeight + 3 * GuiConstants.ROW_HEADER_HEIGHT + 4;
            } else {
                bottomBoxHeight = buttonPadding + totalButtonsHeight + buttonPadding;
            }
            int bottomBoxY = screenHeight - GuiConstants.DEFAULT_PADDING - bottomBoxHeight;
            int soundButtonY = bottomBoxY + bottomBoxHeight - buttonPadding - buttonHeight;
            int addButtonY = soundButtonY - buttonSpacing - buttonHeight;

            if (rightAddButton != null && rightAddButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (rightSoundButton != null && rightSoundButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            if (isAddElementExpanded) {
                int listTop = bottomBoxY + 1;
                int listBottom = addButtonY - 2;
                
                if (mouseY >= listTop && mouseY <= listBottom && mouseX >= contentX && mouseX <= contentX + (panelWidth - 2 * GuiConstants.DEFAULT_PADDING)) {
                     double adjustedMouseY = mouseY + elementListScrollY;
                     for (GDRowRenderer row : availableElementRows) {
                         if (row.mouseClicked(mouseX, adjustedMouseY, button)) {
                             return true;
                         }
                     }
                     isElementListDragging = true;
                     elementListLastMouseY = mouseY;
                     return true;
                }
            }
            
            int top = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT;
            int contentY = top + GuiConstants.DEFAULT_PADDING;
            int titleBoxHeight = 24;             int middleBoxY = contentY + titleBoxHeight + GuiConstants.DEFAULT_PADDING;
            int middleBoxBottom = bottomBoxY - GuiConstants.DEFAULT_PADDING;
            
            if (mouseY >= middleBoxY && mouseY <= middleBoxBottom && mouseX >= contentX && mouseX <= contentX + (panelWidth - 2 * GuiConstants.DEFAULT_PADDING)) {
                 double adjustedMouseY = mouseY + contentScrollY;
                 for (GDRowRenderer row : currentElementRows) {
                     if (row.mouseClicked(mouseX, adjustedMouseY, button)) {
                         return true;
                     }
                 }
                 isContentDragging = true;
                 contentLastMouseY = mouseY;
                 return true;
            }
            
            return true;         }
        
        List<ElementPreview> clickList = new ArrayList<>(previewElements.values());
        for (ElementPreview preview : clickList) {
             preview.updatePosition(screenWidth, screenHeight);
        }
        clickList.sort(Comparator.comparingInt(ElementPreview::getZIndex).reversed());
        
        for (ElementPreview preview : clickList) {
            ElementPreview.PreviewInteractionResult result = preview.mouseClicked(mouseX, mouseY, button);
            if (result != ElementPreview.PreviewInteractionResult.PASS) {
                if (result == ElementPreview.PreviewInteractionResult.DOUBLE_CLICK) {
                     String id = preview.getElementId();
                     String currentId = ClientConfigManager.getCurrentPresetId();
                     if (header != null) {
                        ElementConfigBuilder builder = ElementConfigBuilderRegistry.getBuilder(id);
                        header.setOverrideContent(new ElementConfigContent(minecraft, currentId, id, builder, () -> {
                            header.setOverrideContent(null);
                            preview.resetVisualState();                             updatePreviews();
                            updateCurrentElementRows();
                        }));
                     }
                     return true;
                } else if (result == ElementPreview.PreviewInteractionResult.RIGHT_CLICK) {
                     String id = preview.getElementId();
                     String currentId = ClientConfigManager.getCurrentPresetId();
                     JsonObject config = ElementConfigManager.getElementConfig(currentId, id);
                     boolean isVisible = config != null && (config.has("visible") ? config.get("visible").getAsBoolean() : true);
                     ElementConfigManager.updateConfigValue(currentId, id, "visible", String.valueOf(!isVisible));
                     updateCurrentElementRows();
                     updatePreviews();
                     
                     minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                     return true;
                }

                draggingElementId = preview.getElementId();
                String currentPresetId = ClientConfigManager.getCurrentPresetId();
                JsonObject config = ElementConfigManager.getElementConfig(currentPresetId, draggingElementId);
                if (config != null) {
                    dragStartConfig = config.deepCopy();
                }
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        if (!configRows.isEmpty()) {
            for (GDRowRenderer row : configRows) {
                row.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }
    
    private void renderPresetList(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int panelWidth, int screenHeight, float dt) {
        int startY = this.area1Bottom + GuiConstants.DEFAULT_PADDING;
        
        int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int endY = screenHeight - GuiConstants.DEFAULT_PADDING - buttonHeight - 1 - buttonHeight - GuiConstants.DEFAULT_PADDING;
        
        int listHeight = endY - startY;
        if (listHeight <= 0) return;

        updateScroll(dt, mouseY, listHeight);

        float translatedMouseX = mouseX - currentTranslation;
        
        int visibleWidth = (int)Math.ceil(currentTranslation + panelWidth);
        if (visibleWidth <= 0) return;

        if (visibleWidth > 0 && endY > startY) {
            guiGraphics.enableScissor(0, startY, visibleWidth, endY);
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(currentTranslation, -scrollY, 0);

            int currentY = startY;
            
            for (int i = 0; i < presetRows.size(); i++) {
                GDRowRenderer row = presetRows.get(i);
                int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
                
                if (i == createRowIndex && i > 0) {
                    currentY += 1;
                }

                int rowX1 = GuiConstants.DEFAULT_PADDING;
                int rowX2 = panelWidth - GuiConstants.DEFAULT_PADDING;
                
                row.setBounds(rowX1, currentY, rowX2, currentY + rowHeight);
                
                row.render(guiGraphics, (int)translatedMouseX, (int)(mouseY + scrollY), partialTick);
                
                currentY += rowHeight + 1;             }
            
            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();
        }
    }

    private void updateScroll(float dt, double currentMouseY, int viewHeight) {
        int contentHeight = getPresetListContentHeight();
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        
        if (isDragging) {
            double diff = currentMouseY - lastMouseY;
            targetScrollY -= diff;
            lastMouseY = currentMouseY;
        }
        
        targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY));
        
        double diff = targetScrollY - scrollY;
        if (Math.abs(diff) < 0.01) {
            scrollY = targetScrollY;
        } else {
            scrollY += diff * SCROLL_SMOOTHING * dt;
        }
    }

    private int getPresetListContentHeight() {
        int contentHeight = presetRows.size() * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        if (createRowIndex > 0) {
            contentHeight += 1;
        }
        return contentHeight;
    }

    private boolean tryUndo() {
        String presetId = ClientConfigManager.getCurrentPresetId();
        if (undoStacks.containsKey(presetId) && !undoStacks.get(presetId).isEmpty()) {
            UndoState state = undoStacks.get(presetId).pop();
            ElementConfigManager.setElementConfig(presetId, state.elementId, state.configSnapshot);
            updatePreviews();
            updateCurrentElementRows();
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return false;
    }

    private void updateElementListScroll(float dt, double currentMouseY, int viewHeight) {
        int contentHeight = availableElementRows.size() * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        
        if (isElementListDragging) {
            double diff = currentMouseY - elementListLastMouseY;
            targetElementListScrollY -= diff;
            elementListLastMouseY = currentMouseY;
        }
        
        targetElementListScrollY = Math.max(0, Math.min(maxScroll, targetElementListScrollY));
        
        double diff = targetElementListScrollY - elementListScrollY;
        if (Math.abs(diff) < 0.01) {
            elementListScrollY = targetElementListScrollY;
        } else {
            elementListScrollY += diff * SCROLL_SMOOTHING * dt;
        }
    }

    private void updateCurrentElementRows() {
        currentElementRows.clear();
        String currentId = ClientConfigManager.getCurrentPresetId();
        Set<String> elementIds = ElementConfigManager.getElementIds(currentId);
        List<String> sortedIds = new ArrayList<>(elementIds);
        Collections.sort(sortedIds);

        for (int i = 0; i < sortedIds.size(); i++) {
            String id = sortedIds.get(i);
            GDRowRenderer row = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0, false);
            
            if (i % 2 != 0) row.setBackgroundAlpha(0.15f);
            else row.setBackgroundAlpha(0.3f);

            JsonObject config = ElementConfigManager.getElementConfig(currentId, id);
            boolean isVisible = config != null && (config.has("visible") ? config.get("visible").getAsBoolean() : true);

            List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
            texts.add(new GDTextRenderer.ColoredText(" ", GuiConstants.COLOR_WHITE));
            texts.add(new GDTextRenderer.ColoredText("[" + id + "] ", isVisible ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_DARK_GRAY));
            
            String nameKey = "gd656killicon.element.name." + id.replace("/", ".");
            String name = I18n.exists(nameKey) ? I18n.get(nameKey) : id;
            texts.add(new GDTextRenderer.ColoredText(name, isVisible ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY));
            
            row.addColoredColumn(texts, -1, false, false, null);

            row.addColumn(I18n.get("gd656killicon.client.gui.config.preset.configure_button"), 30, GuiConstants.COLOR_WHITE, true, true, (btn) -> {
                if (header != null) {
                    ElementConfigBuilder builder = ElementConfigBuilderRegistry.getBuilder(id);
                    header.setOverrideContent(new ElementConfigContent(minecraft, currentId, id, builder, () -> {
                        header.setOverrideContent(null);
                        updatePreviews();
                        updateCurrentElementRows();
                    }));
                }
            });
            row.getColumn(1).isDarker = true;

            int toggleWidth = GuiConstants.ROW_HEADER_HEIGHT;
            String toggleText = isVisible ? "👁" : "×";             int toggleColor = GuiConstants.COLOR_WHITE; 
            row.addColumn(toggleText, toggleWidth, toggleColor, false, true, (btn) -> {
                ElementConfigManager.updateConfigValue(currentId, id, "visible", String.valueOf(!isVisible));
                updateCurrentElementRows();
                updatePreviews();
            });

            int deleteWidth = GuiConstants.ROW_HEADER_HEIGHT;
            row.addColumn("×", deleteWidth, GuiConstants.COLOR_RED, true, true, (btn) -> {
                ElementConfigManager.removeElementFromPreset(currentId, id);
                updateCurrentElementRows();
                updateAvailableElementRows();
                updatePreviews();
            });
            row.getColumn(3).isDarker = true;

            row.setOnHover((hovered) -> {
                ElementPreview preview = previewElements.get(id);
                if (preview != null) {
                    preview.setExternalHover(hovered);
                }
            });

            currentElementRows.add(row);
        }
    }

    private void updateContentScroll(float dt, double currentMouseY, int viewHeight) {
        int contentHeight = currentElementRows.size() * (GuiConstants.ROW_HEADER_HEIGHT + 1);
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        
        if (isContentDragging) {
            double diff = currentMouseY - contentLastMouseY;
            targetContentScrollY -= diff;
            contentLastMouseY = currentMouseY;
        }
        
        targetContentScrollY = Math.max(0, Math.min(maxScroll, targetContentScrollY));
        
        double diff = targetContentScrollY - contentScrollY;
        if (Math.abs(diff) < 0.01) {
            contentScrollY = targetContentScrollY;
        } else {
            contentScrollY += diff * SCROLL_SMOOTHING * dt;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (promptDialog.isVisible()) {
            return promptDialog.mouseScrolled(mouseX, mouseY, delta);
        }
        if (textInputDialog.isVisible()) {
            return true;         }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int panelWidth = area1Right + GuiConstants.DEFAULT_PADDING;
        
        if (state == PanelState.OPEN && mouseX <= panelWidth) {
             targetScrollY -= delta * GuiConstants.SCROLL_AMOUNT;
             return true;
        }
        
        int rightPanelX = screenWidth - panelWidth;
        if (rightPanelState == PanelState.OPEN && mouseX >= rightPanelX) {
             if (isAddElementExpanded) {
                 int bottomBoxHeight = 17 + 3 * GuiConstants.ROW_HEADER_HEIGHT + 4;
                 int bottomBoxY = minecraft.getWindow().getGuiScaledHeight() - GuiConstants.DEFAULT_PADDING - bottomBoxHeight;
                 
                 if (mouseY >= bottomBoxY) {
                     targetElementListScrollY -= delta * GuiConstants.SCROLL_AMOUNT;
                     return true;
                 }
             }
             
             targetContentScrollY -= delta * GuiConstants.SCROLL_AMOUNT;
             return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (promptDialog.isVisible()) {
            return true;
        }
        if (textInputDialog.isVisible()) {
            return true;
        }

        isDragging = false;
        isElementListDragging = false;
        isContentDragging = false;

        if (draggingElementId != null && dragStartConfig != null) {
             String currentPresetId = ClientConfigManager.getCurrentPresetId();
             JsonObject currentConfig = ElementConfigManager.getElementConfig(currentPresetId, draggingElementId);
             
             if (currentConfig != null && !currentConfig.equals(dragStartConfig)) {
                 undoStacks.computeIfAbsent(currentPresetId, k -> new java.util.ArrayDeque<>()).push(new UndoState(draggingElementId, dragStartConfig));
             }
             
             draggingElementId = null;
             dragStartConfig = null;
        }

        if (state == PanelState.OPEN) {
            return true;
        }
        
        for (ElementPreview preview : previewElements.values()) {
            if (preview.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (promptDialog.isVisible()) {
            return true;
        }
        if (textInputDialog.isVisible()) {
            return true;
        }

        if (state == PanelState.OPEN) {
            return true;
        }

        for (ElementPreview preview : previewElements.values()) {
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            if (preview.mouseDragged(mouseX, mouseY, button, dragX, dragY, screenWidth, screenHeight)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
