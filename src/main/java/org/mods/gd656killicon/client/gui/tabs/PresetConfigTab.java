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
        HIDDEN, // Completely hidden (off-screen)
        PEEK,   // Partially visible (17px trigger zone)
        OPEN    // Fully visible
    }

    private final Map<String, ElementPreview> previewElements = new HashMap<>();
    private PanelState state = PanelState.HIDDEN;
    private boolean initialized = false;
    private float currentTranslation;
    private float targetTranslation;
    private long lastFrameTime;
    
    // Right Panel State
    private PanelState rightPanelState = PanelState.HIDDEN;
    private float currentRightTranslation;
    private float targetRightTranslation;
    private boolean rightPanelInitialized = false;

    // Preset List
    private final List<GDRowRenderer> presetRows = new ArrayList<>();
    private double scrollY = 0;
    private double targetScrollY = 0;
    private static final double SCROLL_SMOOTHING = 15.0;
    private boolean isDragging = false;
    private double lastMouseY = 0;
    private int createRowIndex = -1;

    // Config Rows Layout Constants
    private static final int TRIGGER_ZONE_WIDTH = GuiConstants.DEFAULT_PADDING; // 8

    // Edit Mode State
    private boolean isEditMode = false;
    private boolean isExportMode = false;
    private Set<String> resetCompletedStates = new HashSet<>();
    
    // Right Panel Content Renderers
    private GDTextRenderer rightTitleRenderer;
    private GDTextRenderer rightInfoRenderer;
    private GDButton rightAddButton;
    private GDButton rightSoundButton;
    
    // Add Element List State
    private boolean isAddElementExpanded = false;
    private List<GDRowRenderer> availableElementRows = new ArrayList<>();
    private double elementListScrollY = 0;
    private double targetElementListScrollY = 0;
    private boolean isElementListDragging = false;
    private double elementListLastMouseY = 0;

    // Middle Content List State
    private List<GDRowRenderer> currentElementRows = new ArrayList<>();
    private double contentScrollY = 0;
    private double targetContentScrollY = 0;
    private boolean isContentDragging = false;
    private double contentLastMouseY = 0;

    // Undo System
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
        // this.textInputDialog = new TextInputDialog(minecraft, null, null); // Removed, handled by super
        this.lastFrameTime = Util.getMillis();
        
        rebuildPresetList();
    }

    public void setHeader(ConfigScreenHeader header) {
        this.header = header;
    }

    private void updatePreviews() {
        String currentId = ClientConfigManager.getCurrentPresetId();
        Set<String> elementIds = ElementConfigManager.getElementIds(currentId);
        
        // Remove previews that are no longer in the current preset
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
            // x1, y1, x2, y2 will be set in render/layout
            GDRowRenderer row = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0, false);
            
            // Alternating alpha
            if (i % 2 != 0) row.setBackgroundAlpha(0.15f);
            else row.setBackgroundAlpha(0.3f);

            // Col 1: Light, Flex, Gold text (display name)
            // Add space before display name as requested
            String displayName = " " + ElementConfigManager.getPresetDisplayName(id);
            // 官方预设使用金色，非官方预设使用白色
            int nameColor;
            if (ElementConfigManager.isOfficialPreset(id)) {
                nameColor = isEditMode ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
            } else {
                nameColor = GuiConstants.COLOR_WHITE;
            }
            
            // Name Column (Index 0)
            // Default: Non-clickable
            row.addColumn(displayName, -1, nameColor, false, false);
            
            // ID Column (Index 1)
            // Default: Non-clickable
            // In Edit Mode, expand width to accommodate action buttons (110px)
            int idWidth = isEditMode ? 110 : 60;
            int idColor = id.equals(currentId) ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_WHITE;
            
            if (isExportMode) {
                 // Export Mode: ID Column becomes Export Button
                 String exportLabel = I18n.get("gd656killicon.client.gui.config.preset.action.export");
                 row.addColumn(exportLabel, idWidth, GuiConstants.COLOR_WHITE, true, true, (btn) -> {
                      openExportDialog(id);
                 });
            } else {
                row.addColumn(id, idWidth, idColor, true, true);
                
                // If NOT in Edit Mode, add basic selection logic
                if (!isEditMode) {
                    // Name Column: No action
                    
                    // ID Column: Select Preset
                    row.getColumn(1).onClick = (btn) -> {
                         if (btn == 0) {
                            ConfigManager.setCurrentPresetId(id);
                            rebuildPresetList();
                         }
                    };
                } else {
                    // Edit Mode Logic
                    
                    // 1. Name Column Hover Logic (Custom Presets Only)
                    if (!ElementConfigManager.isOfficialPreset(id)) {
                        List<GDRowRenderer.Column> nameHoverCols = new ArrayList<>();
                        nameHoverCols.add(createActionColumn(I18n.get("gd656killicon.client.gui.config.preset.action.edit_name_tooltip"), GuiConstants.COLOR_GRAY, (btn) -> {
                            openRenameDialog(id);
                        }));
                        row.setColumnHoverReplacement(0, nameHoverCols);
                    }
    
                    // 2. ID Column Hover Logic
                    List<GDRowRenderer.Column> idHoverCols = new ArrayList<>();
                    if (ElementConfigManager.isOfficialPreset(id)) {
                        // Official Preset: Reset Buttons
                        // Config Reset
                        boolean configReset = resetCompletedStates.contains(id + ":config");
                        idHoverCols.add(createActionColumn(
                            configReset ? I18n.get("gd656killicon.client.gui.config.preset.action.reset_config.done") : I18n.get("gd656killicon.client.gui.config.preset.action.reset_config"),
                            configReset ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_WHITE,
                            configReset ? null : (btn) -> {
                                ConfigManager.resetPresetConfig(id);
                                resetCompletedStates.add(id + ":config");
                                rebuildPresetList();
                            }
                        ));
                        
                        // Texture Reset
                        boolean textureReset = resetCompletedStates.contains(id + ":textures");
                        idHoverCols.add(createActionColumn(
                            textureReset ? I18n.get("gd656killicon.client.gui.config.preset.action.reset_textures.done") : I18n.get("gd656killicon.client.gui.config.preset.action.reset_textures"),
                            textureReset ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_WHITE,
                            textureReset ? null : (btn) -> {
                                ExternalTextureManager.resetTexturesAsync(id);
                                resetCompletedStates.add(id + ":textures");
                                rebuildPresetList();
                            }
                        ));
    
                        // Sound Reset
                        boolean soundReset = resetCompletedStates.contains(id + ":sounds");
                        idHoverCols.add(createActionColumn(
                            soundReset ? I18n.get("gd656killicon.client.gui.config.preset.action.reset_sounds.done") : I18n.get("gd656killicon.client.gui.config.preset.action.reset_sounds"),
                            soundReset ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_WHITE,
                            soundReset ? null : (btn) -> {
                                ExternalSoundManager.resetSoundsAsync(id);
                                resetCompletedStates.add(id + ":sounds");
                                rebuildPresetList();
                            }
                        ));
                    } else {
                        // Custom Preset: Edit ID / Delete
                        // Edit ID
                        idHoverCols.add(createActionColumn(I18n.get("gd656killicon.client.gui.config.preset.action.edit_id"), GuiConstants.COLOR_GRAY, (btn) -> {
                            openRenameIdDialog(id);
                        }));
                        
                        // Delete
                        idHoverCols.add(createActionColumn(I18n.get("gd656killicon.client.gui.config.preset.action.delete"), GuiConstants.COLOR_RED, (btn) -> {
                            if (ElementConfigManager.deletePreset(id)) {
                                // If deleted preset was current, switch to default
                                if (id.equals(ClientConfigManager.getCurrentPresetId())) {
                                    ConfigManager.setCurrentPresetId("00001");
                                }
                                rebuildPresetList();
                            }
                        }));
                    }
                    row.setColumnHoverReplacement(1, idHoverCols);
                }
            }

            presetRows.add(row);
        }
        
        // Add "Create New Preset" Row
        GDRowRenderer createRow = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, false);
        createRow.addColumn(I18n.get("gd656killicon.client.gui.config.preset.create_new"), -1, GuiConstants.COLOR_GREEN, true, true, (btn) -> {
            String newId = ElementConfigManager.createNewPreset();
            ConfigManager.setCurrentPresetId(newId); // Select the new preset
            rebuildPresetList();
        });
        createRowIndex = presetRows.size();
        presetRows.add(createRow);
        
        // Add Toggle Edit Mode Row
        GDRowRenderer toggleRow = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0.3f, false);
        String toggleText = isEditMode ? I18n.get("gd656killicon.client.gui.config.preset.exit_edit_mode") : I18n.get("gd656killicon.client.gui.config.preset.enter_edit_mode");
        int toggleColor = isEditMode ? GuiConstants.COLOR_WHITE : GuiConstants.COLOR_SKY_BLUE;
        
        toggleRow.addColumn(toggleText, -1, toggleColor, true, true, (btn) -> {
            isEditMode = !isEditMode;
            if (isEditMode) isExportMode = false;
            rebuildPresetList();
        });
        
        presetRows.add(toggleRow);

        // Add Toggle Export Mode Row
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
                           // Double check collision
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
            // Create Row
            // x, y, width, height will be set in render loop
            GDRowRenderer row = new GDRowRenderer(0, 0, 0, 0, GuiConstants.COLOR_BLACK, 0, false);
            
            // Alternating transparency (matching left panel style)
            if (i % 2 != 0) row.setBackgroundAlpha(0.15f);
            else row.setBackgroundAlpha(0.3f);

            // Col 1: Text "[ID] Name"
            List<GDTextRenderer.ColoredText> texts = new ArrayList<>();
            texts.add(new GDTextRenderer.ColoredText(" [" + type + "] ", GuiConstants.COLOR_GRAY));
            
            // Try to find display name
            String nameKey = "gd656killicon.element.name." + type.replace("/", ".");
            String name = I18n.exists(nameKey) ? I18n.get(nameKey) : type;
            texts.add(new GDTextRenderer.ColoredText(name, GuiConstants.COLOR_GOLD));
            
            // Add flex column (width -1)
            row.addColoredColumn(texts, -1, false, false, null);
            
            // Col 2: "添加" button
            // Fixed width, 40px
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
        col.isDarker = true; // Make action buttons slightly darker background
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
                // If the renamed preset was the current one, update current ID reference
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
        // Main content starts at 8px from left (TRIGGER_ZONE_WIDTH)
        int contentX = TRIGGER_ZONE_WIDTH;
        
        int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING;
        int contentWidth = screenWidth - contentX - GuiConstants.DEFAULT_PADDING;
        
        int currentY = contentY;
        for (int i = 0; i < configRows.size(); i++) {
            GDRowRenderer row = configRows.get(i);
            int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
            
            row.setBounds(contentX, currentY, contentX + contentWidth, currentY + rowHeight);
            
            // Apply alternating row transparency
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
        // 1. Calculate Panel Dimensions
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int panelWidth = area1Right + GuiConstants.DEFAULT_PADDING;
        
        // Update Reset Button State
        updateResetButtonState();

        // Check if Text Input Dialog is visible
        boolean isDialogVisible = textInputDialog.isVisible() || promptDialog.isVisible();
        boolean isPanelOpen = (state == PanelState.OPEN);
        boolean isRightPanelOpen = (rightPanelState == PanelState.OPEN);

        // Coordinates for Panel Elements (Config Rows, Side Buttons)
        // Blocked only by Dialog
        int panelMouseX = isDialogVisible ? -10000 : mouseX;
        int panelMouseY = isDialogVisible ? -10000 : mouseY;

        // Coordinates for Background Elements (Element Previews)
        // Blocked by Dialog OR Panel (Left OR Right)
        int bgMouseX = (isDialogVisible || isPanelOpen || isRightPanelOpen) ? -10000 : mouseX;
        int bgMouseY = (isDialogVisible || isPanelOpen || isRightPanelOpen) ? -10000 : mouseY;

        // Update Logic (State & Animation)
        long currentTime = Util.getMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000.0f; // Seconds
        lastFrameTime = currentTime;
        
        // --- Left Panel Logic ---
        // State Transitions
        if (state == PanelState.HIDDEN) {
            targetTranslation = -panelWidth;
            // Trigger Peek - use panelMouseX to allow triggering
            // Only trigger left panel if right panel is not open
            if (!isRightPanelOpen && panelMouseX >= 0 && panelMouseX <= TRIGGER_ZONE_WIDTH) {
                state = PanelState.PEEK;
            }
        } else if (state == PanelState.PEEK) {
            targetTranslation = TRIGGER_ZONE_WIDTH - panelWidth;
            // Auto-retract if mouse leaves trigger zone and not clicking
            if (panelMouseX > TRIGGER_ZONE_WIDTH) {
                state = PanelState.HIDDEN;
            }
        } else if (state == PanelState.OPEN) {
            targetTranslation = 0;
            // Close logic handled in mouseClicked
        }
        
        // Handle initial state or resize
        if (!initialized) {
             currentTranslation = -panelWidth;
             initialized = true;
        }

        // Animation: Smooth non-linear interpolation
        float speed = 15.0f;
        if (Math.abs(targetTranslation - currentTranslation) > 0.1f) {
            currentTranslation += (targetTranslation - currentTranslation) * speed * deltaTime;
        } else {
            currentTranslation = targetTranslation;
        }

        // --- Right Panel Logic ---
        // Right panel width is same as left panel width
        int rightPanelWidth = panelWidth;
        int rightTriggerZoneStart = screenWidth - TRIGGER_ZONE_WIDTH;

        // State Transitions
        if (rightPanelState == PanelState.HIDDEN) {
            targetRightTranslation = rightPanelWidth; // Hidden off-screen to the right
            // Trigger Peek
            // Only trigger right panel if left panel is not open
            if (!isPanelOpen && panelMouseX >= rightTriggerZoneStart && panelMouseX <= screenWidth) {
                rightPanelState = PanelState.PEEK;
            }
        } else if (rightPanelState == PanelState.PEEK) {
            targetRightTranslation = rightPanelWidth - TRIGGER_ZONE_WIDTH;
            // Auto-retract if mouse leaves trigger zone
            if (panelMouseX < rightTriggerZoneStart) {
                rightPanelState = PanelState.HIDDEN;
            }
        } else if (rightPanelState == PanelState.OPEN) {
            targetRightTranslation = 0;
            // Close logic handled in mouseClicked
        }

        // Handle initial state or resize
        if (!rightPanelInitialized) {
             currentRightTranslation = rightPanelWidth;
             rightPanelInitialized = true;
        }

        // Animation
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

        // Render Element Previews with Z-Order and Exclusive Hover
        List<ElementPreview> renderList = new ArrayList<>(previewElements.values());
        
        // 1. Update Positions first (needed for hover check)
        for (ElementPreview preview : renderList) {
            preview.updatePosition(screenWidth, screenHeight);
        }
        
        // 2. Sort by Z-Index (Lower Z rendered first, Higher Z rendered last/on top)
        // Optimization: Always render the dragged element last (on top)
        renderList.sort((a, b) -> {
            boolean aIsDragged = draggingElementId != null && a.getElementId().equals(draggingElementId);
            boolean bIsDragged = draggingElementId != null && b.getElementId().equals(draggingElementId);
            
            if (aIsDragged && !bIsDragged) return 1;
            if (!aIsDragged && bIsDragged) return -1;
            
            return Integer.compare(a.getZIndex(), b.getZIndex());
        });
        
        // 3. Determine Hovered Element
        ElementPreview hoveredElement = null;
        
        // Priority: If dragging, the dragged element is always the hovered element
        if (draggingElementId != null && previewElements.containsKey(draggingElementId)) {
            hoveredElement = previewElements.get(draggingElementId);
        } else {
            // Iterate backwards (Top -> Bottom)
            for (int i = renderList.size() - 1; i >= 0; i--) {
                ElementPreview preview = renderList.get(i);
                if (preview.isMouseOver(bgMouseX, bgMouseY)) {
                    hoveredElement = preview;
                    break; // Found the top-most element
                }
            }
        }
        
        // 4. Render (Bottom to Top)
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

        // 4. Render Sliding Panel
        int top = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT;
        
        // --- Render Left Panel ---
        float visibleWidth = currentTranslation + panelWidth;
        
        // Draw Background (Black Rect) - No scissor needed for background if we handle translation
        // But to clip it properly when sliding in, we need scissor OR manual width calculation.
        // Let's use a simpler approach: Render background with scissor, then disable it before calling sub-renderers that need their own scissor.
        
        if (visibleWidth > 0) {
            // A. Render Background & Header Elements with Panel Scissor
            guiGraphics.enableScissor(0, top, (int)Math.ceil(visibleWidth), screenHeight);
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(currentTranslation, 0, 0);
            
            // Draw Background
            guiGraphics.fill(0, top, panelWidth, screenHeight, GuiConstants.COLOR_BG);

            // Draw Panel Elements (Title, Subtitle)
            if (titleRenderer != null) titleRenderer.render(guiGraphics, partialTick);
            if (subtitleRenderer != null) subtitleRenderer.render(guiGraphics, partialTick);
            
            // Draw Dynamic Description
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
            guiGraphics.disableScissor(); // Disable Panel Scissor before list rendering

            // B. Render Preset List (Handles its own scissor)
            renderPresetList(guiGraphics, panelMouseX, panelMouseY, partialTick, panelWidth, screenHeight, deltaTime);
        }

        // --- Render Right Panel ---
        // rightPanelWidth is already defined above in "Right Panel Logic" section
        float rightVisibleWidth = rightPanelWidth - currentRightTranslation;
        int rightPanelX = screenWidth - rightPanelWidth;
        
        // If visible (even partially)
        if (rightVisibleWidth > 0) {
            // Enable scissor from right edge
            // Scissor rect: [screenWidth - rightVisibleWidth, top, screenWidth, screenHeight]
            int scissorX = (int)(screenWidth - rightVisibleWidth);
            guiGraphics.enableScissor(scissorX, top, screenWidth, screenHeight);
            
            guiGraphics.pose().pushPose();
            // Translate to simulate sliding from right
            // When hidden: currentRightTranslation = panelWidth -> X + panelWidth (off screen)
            // When open: currentRightTranslation = 0 -> X + 0 (fully visible)
            guiGraphics.pose().translate(currentRightTranslation, 0, 0);
            
            // Draw Background (at rightPanelX)
            guiGraphics.fill(rightPanelX, top, rightPanelX + rightPanelWidth, screenHeight, GuiConstants.COLOR_BG);
            
            guiGraphics.pose().popPose(); // 结束平移，后续使用绝对坐标以配合 Scissor

            // --- Right Panel Content ---
            // 1. Title Area
            // Padding from panel edges: 8px
            int contentX = (int)(rightPanelX + currentRightTranslation + GuiConstants.DEFAULT_PADDING);
            int contentY = top + GuiConstants.DEFAULT_PADDING;
            int contentWidth = rightPanelWidth - 2 * GuiConstants.DEFAULT_PADDING;
            
            // Calculate height based on 2 lines of text (approx 9px each) + spacing
            int lineHeight = 9; // Standard font height
            int textSpacing = 4;
            // User requested "tightly adhere" to the border (hollow rectangle).
            // We use 1px padding so the text touches the inner edge of the 1px border without overlapping.
            int borderPadding = 1; 
            int titleBoxHeight = borderPadding + lineHeight + textSpacing + lineHeight + borderPadding;
            
            // Draw Hollow Gray Border
            // User noted "Gray border actually has transparency" and "like the left one".
            // Using logic similar to ConfigScreenHeader or general hollow rect.
            int borderColor = (GuiConstants.COLOR_GRAY & 0x00FFFFFF) | (0x80 << 24); // Semi-transparent gray
            
            // Top
            guiGraphics.fill(contentX, contentY, contentX + contentWidth, contentY + 1, borderColor);
            // Bottom
            guiGraphics.fill(contentX, contentY + titleBoxHeight - 1, contentX + contentWidth, contentY + titleBoxHeight, borderColor);
            // Left
            guiGraphics.fill(contentX, contentY + 1, contentX + 1, contentY + titleBoxHeight - 1, borderColor);
            // Right
            guiGraphics.fill(contentX + contentWidth - 1, contentY + 1, contentX + contentWidth, contentY + titleBoxHeight - 1, borderColor);
            
            // Render Title Text
            // Line 1: "预设内元素控制页", Gold, Scale 1.0
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
                // Text and color are static
            }
            rightTitleRenderer.renderInternal(guiGraphics, partialTick, true, deltaTime);
            
            // Render Info Text
            // Line 2: "当前预设：" (White) + [<ID>] (Gray) + Name (Gold)
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
                rightInfoRenderer.setColoredTexts(infoTexts); // Update dynamic content
            }
            rightInfoRenderer.renderInternal(guiGraphics, partialTick, true, deltaTime);
            
            // 2. Bottom Button Area
            // Padding: 1px around the button inside the gray border
            int buttonHeight = 17;
            int buttonPadding = 1;
            int buttonSpacing = 1;
            int totalButtonsHeight = buttonHeight * 2 + buttonSpacing;
            int bottomBoxHeight;
            
            if (isAddElementExpanded) {
                // Expanded height: Button + 3 Rows + 4px
                bottomBoxHeight = totalButtonsHeight + 3 * GuiConstants.ROW_HEADER_HEIGHT + 4;
            } else {
                bottomBoxHeight = buttonPadding + totalButtonsHeight + buttonPadding;
            }
            
            // Y Position: Bottom of screen - DEFAULT_PADDING - Box Height
            int bottomBoxY = screenHeight - GuiConstants.DEFAULT_PADDING - bottomBoxHeight;
            
            // Draw Hollow Gray Border for Bottom Area
            // Top
            guiGraphics.fill(contentX, bottomBoxY, contentX + contentWidth, bottomBoxY + 1, borderColor);
            // Bottom
            guiGraphics.fill(contentX, bottomBoxY + bottomBoxHeight - 1, contentX + contentWidth, bottomBoxY + bottomBoxHeight, borderColor);
            // Left
            guiGraphics.fill(contentX, bottomBoxY + 1, contentX + 1, bottomBoxY + bottomBoxHeight - 1, borderColor);
            // Right
            guiGraphics.fill(contentX + contentWidth - 1, bottomBoxY + 1, contentX + contentWidth, bottomBoxY + bottomBoxHeight - 1, borderColor);
            
            // Render Element List (if expanded)
            if (isAddElementExpanded) {
                // List Area Bounds
                // Top: bottomBoxY + 1 (border)
                // Bottom: Button Top - 2px
                // Button Top = bottomBoxY + bottomBoxHeight - buttonPadding - buttonHeight
                int listTop = bottomBoxY + 1;
                int soundButtonY = bottomBoxY + bottomBoxHeight - buttonPadding - buttonHeight;
                int addButtonY = soundButtonY - buttonSpacing - buttonHeight;
                int listBottom = addButtonY - 2;
                int listHeight = listBottom - listTop;
                
                // Update Scroll
                updateElementListScroll(deltaTime, mouseY, listHeight);
                
                // Scissor for list
                guiGraphics.enableScissor(contentX + 1, listTop, contentX + contentWidth - 1, listBottom);
                
                if (availableElementRows.isEmpty()) {
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, I18n.get("gd656killicon.client.gui.config.preset.all_elements_added"), contentX + contentWidth / 2, listTop + (listBottom - listTop) / 2 - 4, GuiConstants.COLOR_GRAY);
                } else {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, (float)-elementListScrollY, 0);
    
                    // Render rows
                    int currentY = listTop;
                    for (GDRowRenderer row : availableElementRows) {
                        row.setX1(contentX + 1);
                        row.setY1(currentY);
                        row.setX2(contentX + contentWidth - 1);
                        row.setY2(currentY + GuiConstants.ROW_HEADER_HEIGHT);
                        
                        // Only render if visible (Check logical Y against visible range adjusted by scroll)
                        // Visible range in logical coords: [listTop + scrollY, listBottom + scrollY]
                        double scrolledY = currentY - elementListScrollY;
                        if (scrolledY + GuiConstants.ROW_HEADER_HEIGHT > listTop && scrolledY < listBottom) {
                            row.render(guiGraphics, mouseX, (int)(mouseY + elementListScrollY), partialTick);
                        }
                        currentY += GuiConstants.ROW_HEADER_HEIGHT + 1; // 1px spacing
                    }
                    
                    guiGraphics.pose().popPose();
                }
                guiGraphics.disableScissor();
                
                // Re-enable main scissor for right panel if needed?
                // The main scissor covers the whole right panel.
                // Disabling inner scissor usually restores the previous scissor state in modern GL, but Minecraft's `enableScissor` usually overwrites.
                // We should restore the main scissor.
                // Main scissor: (scissorX, top, screenWidth, screenHeight)
                // scissorX was calculated earlier at line 545.
                guiGraphics.enableScissor(scissorX, top, screenWidth, screenHeight);
            }
            
            // Render Button
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
            
            // 3. Middle Area (Dynamic Height)
            // Between Title Box and Bottom Box, separated by DEFAULT_PADDING
            int middleBoxY = contentY + titleBoxHeight + GuiConstants.DEFAULT_PADDING;
            int middleBoxBottom = bottomBoxY - GuiConstants.DEFAULT_PADDING;
            int middleBoxHeight = middleBoxBottom - middleBoxY;
            
            if (middleBoxHeight > 0) {
                // Draw Hollow Gray Border for Middle Area
                // Top
                guiGraphics.fill(contentX, middleBoxY, contentX + contentWidth, middleBoxY + 1, borderColor);
                // Bottom
                guiGraphics.fill(contentX, middleBoxBottom - 1, contentX + contentWidth, middleBoxBottom, borderColor);
                // Left
                guiGraphics.fill(contentX, middleBoxY + 1, contentX + 1, middleBoxBottom - 1, borderColor);
                // Right
                guiGraphics.fill(contentX + contentWidth - 1, middleBoxY + 1, contentX + contentWidth, middleBoxBottom - 1, borderColor);

                // Content Rendering
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
                        
                        // Visibility Check
                        double scrolledY = currentY - contentScrollY;
                        if (scrolledY + GuiConstants.ROW_HEADER_HEIGHT > contentListTop && scrolledY < contentListBottom) {
                            row.render(guiGraphics, mouseX, (int)(mouseY + contentScrollY), partialTick);
                        }
                        currentY += GuiConstants.ROW_HEADER_HEIGHT + 1;
                    }
                    
                    guiGraphics.pose().popPose();
                    guiGraphics.disableScissor();
                    
                    // Restore main scissor
                    guiGraphics.enableScissor(scissorX, top, screenWidth, screenHeight);
                } else {
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gd656killicon.client.gui.config.preset.no_elements"), contentX + contentWidth / 2, middleBoxY + middleBoxHeight / 2 - 4, GuiConstants.COLOR_GRAY);
                }
            }

            guiGraphics.disableScissor();
        }

        // 5. Render Side Buttons (Reset & Cancel)
        renderSideButtons(guiGraphics, panelMouseX, panelMouseY, partialTick, screenWidth, screenHeight);
        
        // 6. Render Dialogs (Top Layer)
        textInputDialog.render(guiGraphics, mouseX, mouseY, partialTick);
        promptDialog.render(guiGraphics, mouseX, mouseY, partialTick);
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

        // --- Left Panel Click Logic ---
        if (state == PanelState.PEEK) {
            // Trigger zone is strictly 0 to ROW_HEADER_HEIGHT (TRIGGER_ZONE_WIDTH)
            if (mouseX <= TRIGGER_ZONE_WIDTH) {
                state = PanelState.OPEN;
                return true;
            }
        } else if (state == PanelState.OPEN) {
            // If clicking outside the panel
            if (mouseX > panelWidth) {
                state = PanelState.HIDDEN; // Retract to invisible
                return true; // Consume click
            }
            // If clicking inside, check buttons first
            if (resetButton != null && resetButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (cancelButton != null && cancelButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            
            // Dragging & Row Clicks (check bounds of list area)
            int startY = this.area1Bottom + GuiConstants.DEFAULT_PADDING;
            int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
            int endY = screenHeight - GuiConstants.DEFAULT_PADDING - buttonHeight - 1 - buttonHeight - GuiConstants.DEFAULT_PADDING;
            
            if (mouseY >= startY && mouseY <= endY && mouseX <= panelWidth) {
                // Check for row clicks first
                float translatedMouseX = (float)mouseX - currentTranslation;
                double adjustedMouseY = mouseY + scrollY;
                
                for (GDRowRenderer row : presetRows) {
                    if (row.mouseClicked(translatedMouseX, adjustedMouseY, button)) {
                        return true;
                    }
                }

                // Enable Dragging for the list
                isDragging = true;
                lastMouseY = mouseY;
                return true;
            }

            // Block interaction with underlying rows
            if (mouseX <= panelWidth) {
                return true;
            }
        }

        // --- Right Panel Click Logic ---
        if (rightPanelState == PanelState.PEEK) {
            int rightTriggerZoneStart = screenWidth - TRIGGER_ZONE_WIDTH;
            if (mouseX >= rightTriggerZoneStart) {
                rightPanelState = PanelState.OPEN;
                return true;
            }
        } else if (rightPanelState == PanelState.OPEN) {
            int rightPanelX = screenWidth - panelWidth;
            
            // If clicking outside the right panel (to the left of it)
            if (mouseX < rightPanelX) {
                rightPanelState = PanelState.HIDDEN;
                return true;
            }
            
            // If clicking inside right panel
            int contentX = rightPanelX + GuiConstants.DEFAULT_PADDING;
            // int contentWidth = panelWidth - 2 * GuiConstants.DEFAULT_PADDING; // Defined implicitly by usage

            // Calculate Bottom Box Y
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

            // Check Button
            if (rightAddButton != null && rightAddButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (rightSoundButton != null && rightSoundButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            // Check Element List (if expanded)
            if (isAddElementExpanded) {
                int listTop = bottomBoxY + 1;
                int listBottom = addButtonY - 2;
                
                // Check if click is within list vertical bounds and horizontal bounds (right panel content area)
                if (mouseY >= listTop && mouseY <= listBottom && mouseX >= contentX && mouseX <= contentX + (panelWidth - 2 * GuiConstants.DEFAULT_PADDING)) {
                     double adjustedMouseY = mouseY + elementListScrollY;
                     for (GDRowRenderer row : availableElementRows) {
                         // Row positions are updated in render, so we can trust them
                         // NOTE: render() sets row positions to LOGICAL coordinates (unscrolled),
                         // so we must pass adjustedMouseY (LOGICAL mouse position) to match.
                         if (row.mouseClicked(mouseX, adjustedMouseY, button)) {
                             return true;
                         }
                     }
                     // Enable Dragging
                     isElementListDragging = true;
                     elementListLastMouseY = mouseY;
                     return true;
                }
            }
            
            // Middle Area Click Logic
            int top = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT;
            int contentY = top + GuiConstants.DEFAULT_PADDING;
            int titleBoxHeight = 24; // 1 + 9 + 4 + 9 + 1
            int middleBoxY = contentY + titleBoxHeight + GuiConstants.DEFAULT_PADDING;
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
            
            return true; // Consume click
        }
        
        // Sort for click detection (Top to Bottom)
        List<ElementPreview> clickList = new ArrayList<>(previewElements.values());
        // Ensure positions are up to date
        for (ElementPreview preview : clickList) {
             preview.updatePosition(screenWidth, screenHeight);
        }
        clickList.sort(Comparator.comparingInt(ElementPreview::getZIndex).reversed());
        
        for (ElementPreview preview : clickList) {
            ElementPreview.PreviewInteractionResult result = preview.mouseClicked(mouseX, mouseY, button);
            if (result != ElementPreview.PreviewInteractionResult.PASS) {
                if (result == ElementPreview.PreviewInteractionResult.DOUBLE_CLICK) {
                     // Double Click: Open Config
                     String id = preview.getElementId();
                     String currentId = ClientConfigManager.getCurrentPresetId();
                     if (header != null) {
                        ElementConfigBuilder builder = ElementConfigBuilderRegistry.getBuilder(id);
                        header.setOverrideContent(new ElementConfigContent(minecraft, currentId, id, builder, () -> {
                            header.setOverrideContent(null);
                            preview.resetVisualState(); // Reset visual state to clear clicked color
                            updatePreviews();
                            updateCurrentElementRows();
                        }));
                     }
                     return true;
                } else if (result == ElementPreview.PreviewInteractionResult.RIGHT_CLICK) {
                     // Right Click: Toggle Visibility
                     String id = preview.getElementId();
                     String currentId = ClientConfigManager.getCurrentPresetId();
                     JsonObject config = ElementConfigManager.getElementConfig(currentId, id);
                     boolean isVisible = config != null && (config.has("visible") ? config.get("visible").getAsBoolean() : true);
                     ElementConfigManager.updateConfigValue(currentId, id, "visible", String.valueOf(!isVisible));
                     updateCurrentElementRows();
                     updatePreviews();
                     
                     // Play click sound
                     minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                     return true;
                }

                // Start Tracking for Undo (HANDLED case)
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
        // Calculate Layout
        // Use area1Bottom which includes Title + Subtitle height
        int startY = this.area1Bottom + GuiConstants.DEFAULT_PADDING;
        
        int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int endY = screenHeight - GuiConstants.DEFAULT_PADDING - buttonHeight - 1 - buttonHeight - GuiConstants.DEFAULT_PADDING;
        
        int listHeight = endY - startY;
        if (listHeight <= 0) return;

        // Update Scroll
        updateScroll(dt, mouseY, listHeight);

        // Render Rows
        // Translate mouse coordinates for hover check since we are inside a translated pose (panel slide)
        float translatedMouseX = mouseX - currentTranslation;
        
        // Calculate visible width for scissor
        int visibleWidth = (int)Math.ceil(currentTranslation + panelWidth);
        if (visibleWidth <= 0) return;

        // Enable Scissor for the list area to clip background and content
        // This overrides the panel's scissor with a stricter vertical range
        if (visibleWidth > 0 && endY > startY) {
            guiGraphics.enableScissor(0, startY, visibleWidth, endY);
            
            guiGraphics.pose().pushPose();
            // Need to apply panel translation AND scroll translation
            // In render(), we already popped the panel translation pose.
            // So we need to re-apply it here.
            guiGraphics.pose().translate(currentTranslation, -scrollY, 0);

            int currentY = startY;
            
            for (int i = 0; i < presetRows.size(); i++) {
                GDRowRenderer row = presetRows.get(i);
                int rowHeight = GuiConstants.ROW_HEADER_HEIGHT;
                
                if (i == createRowIndex && i > 0) {
                    currentY += 1;
                }

                // Layout Row
                // Width: Panel Width - 2 * Padding (Left/Right)
                int rowX1 = GuiConstants.DEFAULT_PADDING;
                int rowX2 = panelWidth - GuiConstants.DEFAULT_PADDING;
                
                row.setBounds(rowX1, currentY, rowX2, currentY + rowHeight);
                
                // Adjust mouse Y for scroll translation
                row.render(guiGraphics, (int)translatedMouseX, (int)(mouseY + scrollY), partialTick);
                
                currentY += rowHeight + 1; // 1px spacing
            }
            
            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();
        }
    }

    private void updateScroll(float dt, double currentMouseY, int viewHeight) {
        // Content Height
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
        // Content Height: Rows * (Height + 1px spacing)
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

            // Get Visibility Status
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

            // Toggle Visibility Column
            int toggleWidth = GuiConstants.ROW_HEADER_HEIGHT;
            String toggleText = isVisible ? "👁" : "×"; // Eye or Cross
            // Use a light color for the column/button as requested
            int toggleColor = GuiConstants.COLOR_WHITE; 
            row.addColumn(toggleText, toggleWidth, toggleColor, false, true, (btn) -> {
                ElementConfigManager.updateConfigValue(currentId, id, "visible", String.valueOf(!isVisible));
                updateCurrentElementRows();
                updatePreviews();
            });
            // "Light colored column" - We interpret this as a lighter background or distinct look. 
            // Since GDRowRenderer.Column doesn't support custom bg color easily without modification,
            // we rely on the button rendering. 
            // Let's NOT set isDarker = true (which makes it darker).
            // row.getColumn(2).isDarker = false; // Default is false

            int deleteWidth = GuiConstants.ROW_HEADER_HEIGHT;
            row.addColumn("×", deleteWidth, GuiConstants.COLOR_RED, true, true, (btn) -> {
                ElementConfigManager.removeElementFromPreset(currentId, id);
                updateCurrentElementRows();
                updateAvailableElementRows();
                updatePreviews();
            });
            row.getColumn(3).isDarker = true;

            // Set Row Hover Listener to trigger Element Preview Hover
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
            return true; // Consume scroll to block background scrolling
        }

        // Check if mouse is over the panel
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int panelWidth = area1Right + GuiConstants.DEFAULT_PADDING;
        
        if (state == PanelState.OPEN && mouseX <= panelWidth) {
             targetScrollY -= delta * GuiConstants.SCROLL_AMOUNT;
             return true;
        }
        
        // Check Right Panel
        int rightPanelX = screenWidth - panelWidth;
        if (rightPanelState == PanelState.OPEN && mouseX >= rightPanelX) {
             // Check if over Element List (Bottom Box)
             if (isAddElementExpanded) {
                 int bottomBoxHeight = 17 + 3 * GuiConstants.ROW_HEADER_HEIGHT + 4;
                 int bottomBoxY = minecraft.getWindow().getGuiScaledHeight() - GuiConstants.DEFAULT_PADDING - bottomBoxHeight;
                 
                 if (mouseY >= bottomBoxY) {
                     targetElementListScrollY -= delta * GuiConstants.SCROLL_AMOUNT;
                     return true;
                 }
             }
             
             // Otherwise scroll Content List (Middle Area)
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

        // Undo Logic: Commit Drag Step
        if (draggingElementId != null && dragStartConfig != null) {
             String currentPresetId = ClientConfigManager.getCurrentPresetId();
             JsonObject currentConfig = ElementConfigManager.getElementConfig(currentPresetId, draggingElementId);
             
             // Only record if config has changed (position changed)
             if (currentConfig != null && !currentConfig.equals(dragStartConfig)) {
                 undoStacks.computeIfAbsent(currentPresetId, k -> new java.util.ArrayDeque<>()).push(new UndoState(draggingElementId, dragStartConfig));
             }
             
             draggingElementId = null;
             dragStartConfig = null;
        }

        // If panel is open, consume release event to prevent background interaction
        // BUT we need to ensure isDragging was cleared (done above)
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

        // If panel is open, block drag propagation to background elements
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
