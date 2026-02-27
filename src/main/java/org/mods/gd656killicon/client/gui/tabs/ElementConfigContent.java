package org.mods.gd656killicon.client.gui.tabs;

import org.mods.gd656killicon.client.util.GifToSpriteSheetConverter;
import java.io.File;
import java.io.IOException;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.ConfirmDialog;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.InfiniteGridWidget;
import org.mods.gd656killicon.client.gui.elements.PromptDialog;
import org.mods.gd656killicon.client.gui.elements.entries.BooleanConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.FloatConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.HexColorConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.IntegerConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.StringConfigEntry;
import org.mods.gd656killicon.client.gui.screens.ElementConfigBuilder;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.impl.Battlefield1Renderer;
import org.mods.gd656killicon.client.render.impl.CardBarRenderer;
import org.mods.gd656killicon.client.render.impl.CardRenderer;
import org.mods.gd656killicon.client.render.impl.BonusListRenderer;
import org.mods.gd656killicon.client.render.impl.ComboIconRenderer;
import org.mods.gd656killicon.client.render.impl.ScrollingIconRenderer;
import org.mods.gd656killicon.client.render.impl.ScoreSubtitleRenderer;
import org.mods.gd656killicon.client.render.impl.ComboSubtitleRenderer;
import org.mods.gd656killicon.client.render.impl.SubtitleRenderer;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.nio.file.Path;

public class ElementConfigContent extends ConfigTabContent {
    private final String presetId;
    private final String elementId;
    private final ElementConfigBuilder builder;
    private final Runnable onClose;
    private final JsonObject initialConfig;

    private GDTextRenderer elementInfoRenderer;
    private int calculatedBottom;
    private InfiniteGridWidget gridWidget;
    private ScrollingIconRenderer scrollingPreviewRenderer = new ScrollingIconRenderer();
    private long lastPreviewTriggerTime = 0L;
    private int previewKillTypeIndex = 0;
    private ComboIconRenderer comboPreviewRenderer = new ComboIconRenderer();
    private long lastComboPreviewTriggerTime = 0L;
    private int previewComboCount = 1;
    private int previewComboKillTypeIndex = 0;
    private CardRenderer cardPreviewRenderer = new CardRenderer();
    private long lastCardPreviewTriggerTime = 0L;
    private int previewCardComboCount = 1;
    private int previewCardKillTypeIndex = 0;
    private CardBarRenderer cardBarPreviewRenderer = new CardBarRenderer();
    private long lastCardBarPreviewTriggerTime = 0L;
    private int previewCardBarComboCount = 1;
    private int previewCardBarKillTypeIndex = 0;
    private Battlefield1Renderer battlefield1PreviewRenderer = new Battlefield1Renderer();
    private long lastBattlefieldPreviewTriggerTime = 0L;
    private int previewBattlefieldKillTypeIndex = 0;
    private SubtitleRenderer subtitlePreviewRenderer = new SubtitleRenderer();
    private long lastSubtitlePreviewTriggerTime = 0L;
    private ComboSubtitleRenderer comboSubtitlePreviewRenderer = ComboSubtitleRenderer.getInstance();
    private long lastComboSubtitlePreviewTriggerTime = 0L;
    private ScoreSubtitleRenderer scorePreviewRenderer = ScoreSubtitleRenderer.getInstance();
    private long lastScorePreviewTriggerTime = 0L;
    private BonusListRenderer bonusListPreviewRenderer = BonusListRenderer.getInstance();
    private long lastBonusListPreviewTriggerTime = 0L;
    private final List<SecondaryTab> secondaryTabs = new ArrayList<>();
    private SecondaryTab selectedSecondaryTab;
    private final List<GDRowRenderer> allConfigRows = new ArrayList<>();
    private double secondaryScrollX = 0;
    private double secondaryTargetScrollX = 0;
    private double secondaryMaxScroll = 0;
    private boolean secondaryDragging = false;
    private boolean secondaryPressed = false;
    private double secondaryLastMouseX = 0;
    private int secondaryAreaX1 = 0;
    private int secondaryAreaX2 = 0;
    private int secondaryAreaTop = 0;
    private int secondaryAreaBottom = 0;
    private long lastSecondaryRenderTime = 0L;

    private static final long PREVIEW_TRIGGER_INTERVAL_MS = 2000L;
    private static final long PREVIEW_SUBTITLE_TRIGGER_INTERVAL_MS = 2000L;
    private static final int[] PREVIEW_KILL_TYPES = new int[] {
            KillType.NORMAL,
            KillType.HEADSHOT,
            KillType.EXPLOSION,
            KillType.CRIT,
            KillType.ASSIST,
            KillType.DESTROY_VEHICLE
    };
    private static final long PREVIEW_COMBO_TRIGGER_INTERVAL_MS = 1200L;
    private static final int[] PREVIEW_COMBO_KILL_TYPES = new int[] {
            KillType.NORMAL,
            KillType.HEADSHOT,
            KillType.EXPLOSION,
            KillType.CRIT
    };
    private static final long PREVIEW_CARD_TRIGGER_INTERVAL_MS = 1200L;
    private static final long PREVIEW_CARD_BAR_TRIGGER_INTERVAL_MS = 1200L;
    private static final long PREVIEW_BATTLEFIELD_TRIGGER_INTERVAL_MS = 1600L;
    private static final int PREVIEW_BONUS_COUNT = 3;
    private static final int[] PREVIEW_BATTLEFIELD_KILL_TYPES = new int[] {
            KillType.NORMAL,
            KillType.HEADSHOT,
            KillType.EXPLOSION,
            KillType.CRIT,
            KillType.DESTROY_VEHICLE
    };
    private static final RandomSource PREVIEW_RANDOM = RandomSource.create();
    
    private GDButton saveButton;
    private ConfirmDialog textureResetDialog;
    
    private boolean isConfirmingReset = false;
    private long resetConfirmTime = 0;

    public ElementConfigContent(Minecraft minecraft, String presetId, String elementId, ElementConfigBuilder builder, Runnable onClose) {
        super(minecraft, Component.translatable("gd656killicon.client.gui.config.element.title"));
        this.presetId = presetId;
        this.elementId = elementId;
        this.builder = builder;
        this.onClose = onClose;
        this.textureResetDialog = new ConfirmDialog(minecraft, null, null);
        
        JsonObject current = ElementConfigManager.getElementConfig(presetId, elementId);
        this.initialConfig = current != null ? current.deepCopy() : new JsonObject();
        
        rebuildUI();
    }
    
    private void rebuildUI() {
        this.configRows.clear();
        if (builder != null) {
            builder.build(this);
        }
        this.allConfigRows.clear();
        this.allConfigRows.addAll(this.configRows);
        refreshVisibleRows();
    }
    
    private void refreshVisibleRows() {
        this.configRows.clear();
        
        updateRowActiveConditions();

        if (!isKillIconElement() || !ElementTextureDefinition.hasTextures(elementId)) {
            this.configRows.addAll(this.allConfigRows);
            sortConfigRows();
            return;
        }

        if (selectedSecondaryTab == null) {
            ensureSecondaryTabs();
        }
        
        if (selectedSecondaryTab == null) return;         
        boolean isGeneral = "general".equals(selectedSecondaryTab.elementId);
        String texturePrefix = "anim_" + selectedSecondaryTab.elementId + "_";
        String textureStyleKey = ElementTextureDefinition.getTextureStyleKey(selectedSecondaryTab.elementId);
        
        for (GDRowRenderer row : allConfigRows) {
            String key = getConfigKey(row);
            if (key == null) continue;
            
            boolean isAnimKey = key.startsWith("anim_");
            boolean isTextureStyleKey = key.startsWith("texture_style_");
            
            if (isGeneral) {
                if (!isAnimKey && !isTextureStyleKey) {
                    this.configRows.add(row);
                }
            } else {
                if (key.startsWith(texturePrefix) || key.equals(textureStyleKey)) {
                    this.configRows.add(row);
                }
            }
        }
        
        sortConfigRows();
    }
    
    private void updateRowActiveConditions() {
        for (GDRowRenderer row : allConfigRows) {
            String key = getConfigKey(row);
            if (key == null) continue;

            if ("subtitle/kill_feed".equals(elementId)) {
                if (key.equals("max_lines") || key.equals("line_spacing")) {
                    row.setActiveCondition(() -> {
                        JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                        return config != null && config.has("enable_stacking") && config.get("enable_stacking").getAsBoolean();
                    });
                }
                
                else if (key.contains("headshot")) {
                    if (!key.equals("enable_headshot_kill")) {
                        row.setActiveCondition(() -> {
                            JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                            return config == null || !config.has("enable_headshot_kill") || config.get("enable_headshot_kill").getAsBoolean();
                        });
                    }
                }
                
                else if (key.contains("explosion")) {
                    if (!key.equals("enable_explosion_kill")) {
                        row.setActiveCondition(() -> {
                            JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                            return config == null || !config.has("enable_explosion_kill") || config.get("enable_explosion_kill").getAsBoolean();
                        });
                    }
                }
                
                else if (key.contains("crit")) {
                    if (!key.equals("enable_crit_kill")) {
                        row.setActiveCondition(() -> {
                            JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                            return config == null || !config.has("enable_crit_kill") || config.get("enable_crit_kill").getAsBoolean();
                        });
                    }
                }
                
                else if (key.contains("assist")) {
                    if (!key.equals("enable_assist_kill")) {
                        row.setActiveCondition(() -> {
                            JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                            return config == null || !config.has("enable_assist_kill") || config.get("enable_assist_kill").getAsBoolean();
                        });
                    }
                }
                
                else if (key.contains("destroy_vehicle")) {
                    if (!key.equals("enable_destroy_vehicle_kill")) {
                        row.setActiveCondition(() -> {
                            JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                            return config == null || !config.has("enable_destroy_vehicle_kill") || config.get("enable_destroy_vehicle_kill").getAsBoolean();
                        });
                    }
                }
                
                 else if (key.equals("format_normal") || key.equals("color_normal_placeholder") || key.equals("color_normal_emphasis")) {
                      row.setActiveCondition(() -> {
                         JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                         return config == null || !config.has("enable_normal_kill") || config.get("enable_normal_kill").getAsBoolean();
                     });
                 }
             }
         }
         
         for (GDRowRenderer row : allConfigRows) {
             String key = getConfigKey(row);
             if (key == null || key.equals("visible")) continue;
             
             Supplier<Boolean> specificCondition = row.getActiveCondition();
             row.setActiveCondition(() -> {
                 JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                 boolean isVisible = config == null || !config.has("visible") || config.get("visible").getAsBoolean();
                 if (!isVisible) return false;
                 return specificCondition.get();
             });
         }
     }

    private String getConfigKey(GDRowRenderer row) {
        if (row instanceof BooleanConfigEntry) return ((BooleanConfigEntry) row).getKey();
        if (row instanceof IntegerConfigEntry) return ((IntegerConfigEntry) row).getKey();
        if (row instanceof FloatConfigEntry) return ((FloatConfigEntry) row).getKey();
        if (row instanceof StringConfigEntry) return ((StringConfigEntry) row).getKey();
        if (row instanceof HexColorConfigEntry) return ((HexColorConfigEntry) row).getKey();
        if (row instanceof FixedChoiceConfigEntry) return ((FixedChoiceConfigEntry) row).getKey();
        return null;
    }

    public String getPresetId() {
        return presetId;
    }

    public String getElementId() {
        return elementId;
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

        String nameKey = "gd656killicon.element.name." + elementId.replace("/", ".");
        String elementName = I18n.exists(nameKey) ? I18n.get(nameKey) : elementId;

        List<GDTextRenderer.ColoredText> elementTexts = new ArrayList<>();
        elementTexts.add(new GDTextRenderer.ColoredText(I18n.get("gd656killicon.client.gui.config.generic.current_element"), GuiConstants.COLOR_WHITE));
        elementTexts.add(new GDTextRenderer.ColoredText("[" + elementId + "] ", GuiConstants.COLOR_GRAY));
        elementTexts.add(new GDTextRenderer.ColoredText(elementName, GuiConstants.COLOR_GOLD));

        int line2Y = y1 + 9 + 2; 
        if (elementInfoRenderer == null) {
            elementInfoRenderer = new GDTextRenderer(elementTexts, x1, line2Y, x2, line2Y + 9, 1.0f, false);
        } else {
            elementInfoRenderer.setX1(x1);
            elementInfoRenderer.setY1(line2Y);
            elementInfoRenderer.setX2(x2);
            elementInfoRenderer.setY2(line2Y + 9);
            elementInfoRenderer.setColoredTexts(elementTexts);
        }

        this.calculatedBottom = line2Y + 9;
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

        updateSecondaryTabLayout(screenWidth);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        boolean resetDialogVisible = textureResetDialog != null && textureResetDialog.isVisible();
        boolean promptVisible = promptDialog.isVisible();
        boolean dialogVisible = resetDialogVisible || promptVisible || textInputDialog.isVisible() || colorPickerDialog.isVisible();
        int effectiveMouseX = dialogVisible ? -1 : mouseX;
        int effectiveMouseY = dialogVisible ? -1 : mouseY;
        if (gridWidget == null) {
            updateLayout(screenWidth, screenHeight);
        }
        if (gridWidget != null) {
            guiGraphics.pose().pushPose();
            gridWidget.render(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick, Collections.emptyList());
            guiGraphics.pose().popPose();
            renderScrollingPreview(guiGraphics, partialTick);
            renderComboPreview(guiGraphics, partialTick);
            renderCardPreview(guiGraphics, partialTick);
            renderCardBarPreview(guiGraphics, partialTick);
            renderBattlefieldPreview(guiGraphics, partialTick);
            renderKillFeedPreview(guiGraphics, partialTick);
            renderComboSubtitlePreview(guiGraphics, partialTick);
            renderScorePreview(guiGraphics, partialTick);
            renderBonusListPreview(guiGraphics, partialTick);
        }

        if (elementInfoRenderer != null) {
            elementInfoRenderer.render(guiGraphics, partialTick);
        }

        if (dialogVisible) {
            renderSecondaryTabs(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick);
        }

        if (resetDialogVisible) {
            super.render(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick, screenWidth, screenHeight, headerHeight);
        } else {
            super.render(guiGraphics, mouseX, mouseY, partialTick, screenWidth, screenHeight, headerHeight);
        }
        if (!dialogVisible) {
            renderSecondaryTabs(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick);
        }
        
        if (isConfirmingReset) {
            long elapsed = System.currentTimeMillis() - resetConfirmTime;
            if (elapsed > 3000) {
                isConfirmingReset = false;
                if (resetButton != null) {
                    resetButton.setMessage(Component.translatable("gd656killicon.client.gui.config.button.reset_element"));
                    resetButton.setTextColor(GuiConstants.COLOR_WHITE);
                }
            }
        }
        if (resetDialogVisible) {
            textureResetDialog.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateScroll(float dt, int screenHeight) {
        if (!useDefaultScroll) return;

        int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING + getContentTopOffset();
        int viewHeight = screenHeight - contentY - GuiConstants.DEFAULT_PADDING;

        double maxScroll = Math.max(0, totalContentHeight - viewHeight);
        targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY));

        double diff = targetScrollY - scrollY;
        if (Math.abs(diff) < 0.01) {
            scrollY = targetScrollY;
        } else {
            scrollY += diff * GuiConstants.SCROLL_SMOOTHING * dt;
        }
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        if (!configRows.isEmpty()) {
            if (useDefaultScroll) {
                float dt = minecraft.getDeltaFrameTime() / 20.0f;

                if (isDragging) {
                    double diff = mouseY - lastMouseY;
                    targetScrollY -= diff;
                    lastMouseY = mouseY;
                }

                updateScroll(dt, screenHeight);

                int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
                int contentX = area1Right + GuiConstants.DEFAULT_PADDING;
                int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING + getContentTopOffset();
                int contentWidth = screenWidth - contentX - GuiConstants.DEFAULT_PADDING;
                int contentHeight = screenHeight - contentY - GuiConstants.DEFAULT_PADDING;

                guiGraphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, -scrollY, 0);

                for (GDRowRenderer row : configRows) {
                    row.render(guiGraphics, mouseX, (int)(mouseY + scrollY), partialTick);
                }

                guiGraphics.pose().popPose();
                guiGraphics.disableScissor();
            } else {
                for (GDRowRenderer row : configRows) {
                    row.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }
            return;
        }

        Component noContent = Component.translatable("gd656killicon.client.gui.config.no_content");
        int textWidth = minecraft.font.width(noContent);

        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int contentX = area1Right + GuiConstants.DEFAULT_PADDING + (screenWidth - area1Right - 2 * GuiConstants.DEFAULT_PADDING - textWidth) / 2;
        int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING + getContentTopOffset() + (screenHeight - (GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING + getContentTopOffset()) - 9) / 2;

        guiGraphics.drawString(minecraft.font, noContent, contentX, contentY, GuiConstants.COLOR_GRAY, true);
    }

    @Override
    protected void updateConfigRowsLayout(int screenWidth, int screenHeight) {
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int contentX = area1Right + GuiConstants.DEFAULT_PADDING;
        int contentY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + GuiConstants.DEFAULT_PADDING + getContentTopOffset();
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

        this.totalContentHeight = currentY - contentY;
    }

    private void renderScrollingPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"kill_icon/scrolling".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        
        boolean isSpecific = selectedSecondaryTab != null && !"general".equals(selectedSecondaryTab.elementId);
        
        if (isSpecific) {
            String tex = selectedSecondaryTab.elementId;
            int killType = -1;
            if ("default".equals(tex)) killType = KillType.NORMAL;
            else if ("headshot".equals(tex)) killType = KillType.HEADSHOT;
            else if ("explosion".equals(tex)) killType = KillType.EXPLOSION;
            else if ("crit".equals(tex)) killType = KillType.CRIT;
            else if ("destroy_vehicle".equals(tex)) killType = KillType.DESTROY_VEHICLE;
            else if ("assist".equals(tex)) killType = KillType.ASSIST;
            
            if (killType != -1) {
                if (now - lastPreviewTriggerTime >= PREVIEW_TRIGGER_INTERVAL_MS) {
                    scrollingPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1));
                    lastPreviewTriggerTime = now;
                }
            }
        } else {
            if (now - lastPreviewTriggerTime >= PREVIEW_TRIGGER_INTERVAL_MS) {
                int killType = PREVIEW_KILL_TYPES[previewKillTypeIndex % PREVIEW_KILL_TYPES.length];
                previewKillTypeIndex++;
                scrollingPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1));
                lastPreviewTriggerTime = now;
            }
        }
        
        float originX = gridWidget.getOriginX();
        float originY = gridWidget.getOriginY();
        int scissorX1 = gridWidget.getX();
        int scissorY1 = gridWidget.getY();
        int scissorX2 = scissorX1 + gridWidget.getWidth();
        int scissorY2 = scissorY1 + gridWidget.getHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        scrollingPreviewRenderer.renderAt(guiGraphics, partialTick, originX, originY);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderComboPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"kill_icon/combo".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        
        boolean isSpecific = selectedSecondaryTab != null && !"general".equals(selectedSecondaryTab.elementId);
        
        if (isSpecific) {
            String tex = selectedSecondaryTab.elementId;
            int combo = 1;
            try {
                if (tex.startsWith("combo_")) {
                    combo = Integer.parseInt(tex.substring(6));
                }
            } catch (Exception e) {}
            
            if (now - lastComboPreviewTriggerTime >= PREVIEW_COMBO_TRIGGER_INTERVAL_MS) {
                comboPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(KillType.NORMAL, -1, combo));
                lastComboPreviewTriggerTime = now;
            }
        } else {
            if (now - lastComboPreviewTriggerTime >= PREVIEW_COMBO_TRIGGER_INTERVAL_MS) {
                int killType = PREVIEW_COMBO_KILL_TYPES[previewComboKillTypeIndex % PREVIEW_COMBO_KILL_TYPES.length];
                int comboCount = previewComboCount;
                previewComboKillTypeIndex++;
                previewComboCount = previewComboCount >= 6 ? 1 : previewComboCount + 1;
                comboPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1, comboCount));
                lastComboPreviewTriggerTime = now;
            }
        }
        
        float originX = gridWidget.getOriginX();
        float originY = gridWidget.getOriginY();
        int scissorX1 = gridWidget.getX();
        int scissorY1 = gridWidget.getY();
        int scissorX2 = scissorX1 + gridWidget.getWidth();
        int scissorY2 = scissorY1 + gridWidget.getHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        comboPreviewRenderer.renderAt(guiGraphics, partialTick, originX, originY);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderCardPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"kill_icon/card".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        
        JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId).deepCopy();
        boolean isSpecific = selectedSecondaryTab != null && !"general".equals(selectedSecondaryTab.elementId);
        
        if (isSpecific) {
            String tex = selectedSecondaryTab.elementId;
            String team = "ct";
            if (tex.contains("_ct")) team = "ct";
            else if (tex.contains("_t")) team = "t";
            config.addProperty("team", team);
            config.addProperty("dynamic_card_style", false);
            
            int killType = KillType.NORMAL;
            if (tex.startsWith("headshot")) killType = KillType.HEADSHOT;
            else if (tex.startsWith("explosion")) killType = KillType.EXPLOSION;
            else if (tex.startsWith("crit")) killType = KillType.CRIT;
            
            if (tex.startsWith("light")) {
                config.addProperty("show_light", true);
            } else {
                config.addProperty("show_light", false);
            }
            
            if (now - lastCardPreviewTriggerTime >= PREVIEW_CARD_TRIGGER_INTERVAL_MS) {
                cardPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1, 1));
                lastCardPreviewTriggerTime = now;
            }
        } else {
            if (now - lastCardPreviewTriggerTime >= PREVIEW_CARD_TRIGGER_INTERVAL_MS) {
                int killType = PREVIEW_COMBO_KILL_TYPES[previewCardKillTypeIndex % PREVIEW_COMBO_KILL_TYPES.length];
                int comboCount = previewCardComboCount;
                previewCardKillTypeIndex++;
                previewCardComboCount = previewCardComboCount >= 6 ? 1 : previewCardComboCount + 1;
                cardPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1, comboCount));
                lastCardPreviewTriggerTime = now;
            }
        }

        float originX = gridWidget.getOriginX();
        float originY = gridWidget.getOriginY();
        int scissorX1 = gridWidget.getX();
        int scissorY1 = gridWidget.getY();
        int scissorX2 = scissorX1 + gridWidget.getWidth();
        int scissorY2 = scissorY1 + gridWidget.getHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        cardPreviewRenderer.renderPreviewAt(guiGraphics, partialTick, originX, originY, config);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderCardBarPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"kill_icon/card_bar".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        
        JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId).deepCopy();
        boolean isSpecific = selectedSecondaryTab != null && !"general".equals(selectedSecondaryTab.elementId);
        
        if (isSpecific) {
            String tex = selectedSecondaryTab.elementId;
            String team = "ct";
            if (tex.contains("_ct")) team = "ct";
            else if (tex.contains("_t")) team = "t";
            config.addProperty("team", team);
            config.addProperty("dynamic_card_style", false);
            
            if (now - lastCardBarPreviewTriggerTime >= PREVIEW_CARD_BAR_TRIGGER_INTERVAL_MS) {
                cardBarPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(KillType.NORMAL, -1, 1));
                lastCardBarPreviewTriggerTime = now;
            }
        } else {
            if (now - lastCardBarPreviewTriggerTime >= PREVIEW_CARD_BAR_TRIGGER_INTERVAL_MS) {
                int killType = PREVIEW_COMBO_KILL_TYPES[previewCardBarKillTypeIndex % PREVIEW_COMBO_KILL_TYPES.length];
                int comboCount = previewCardBarComboCount;
                previewCardBarKillTypeIndex++;
                previewCardBarComboCount = previewCardBarComboCount >= 6 ? 1 : previewCardBarComboCount + 1;
                cardBarPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1, comboCount));
                lastCardBarPreviewTriggerTime = now;
            }
        }
        
        float originX = gridWidget.getOriginX();
        float originY = gridWidget.getOriginY();
        int scissorX1 = gridWidget.getX();
        int scissorY1 = gridWidget.getY();
        int scissorX2 = scissorX1 + gridWidget.getWidth();
        int scissorY2 = scissorY1 + gridWidget.getHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        cardBarPreviewRenderer.renderPreviewAt(guiGraphics, partialTick, originX, originY, config);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderBattlefieldPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"kill_icon/battlefield1".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        
        boolean isSpecific = selectedSecondaryTab != null && !"general".equals(selectedSecondaryTab.elementId);
        
        if (isSpecific) {
            String tex = selectedSecondaryTab.elementId;
            int killType = -1;
            if ("default".equals(tex)) killType = KillType.NORMAL;
            else if ("headshot".equals(tex)) killType = KillType.HEADSHOT;
            else if ("explosion".equals(tex)) killType = KillType.EXPLOSION;
            else if ("crit".equals(tex)) killType = KillType.CRIT;
            else if ("destroy_vehicle".equals(tex)) killType = KillType.DESTROY_VEHICLE;
            
            if (killType != -1) {
                if (now - lastBattlefieldPreviewTriggerTime >= PREVIEW_BATTLEFIELD_TRIGGER_INTERVAL_MS) {
                    String weaponName = resolveRandomItemName();
                    battlefield1PreviewRenderer.triggerPreview(killType, weaponName, "Minecraft_GD656", "20");
                    lastBattlefieldPreviewTriggerTime = now;
                }
            }
        } else {
            if (now - lastBattlefieldPreviewTriggerTime >= PREVIEW_BATTLEFIELD_TRIGGER_INTERVAL_MS) {
                int killType = PREVIEW_BATTLEFIELD_KILL_TYPES[previewBattlefieldKillTypeIndex % PREVIEW_BATTLEFIELD_KILL_TYPES.length];
                previewBattlefieldKillTypeIndex++;
                String weaponName = resolveRandomItemName();
                battlefield1PreviewRenderer.triggerPreview(killType, weaponName, "Minecraft_GD656", "20");
                lastBattlefieldPreviewTriggerTime = now;
            }
        }
        
        float originX = gridWidget.getOriginX();
        float originY = gridWidget.getOriginY();
        int scissorX1 = gridWidget.getX();
        int scissorY1 = gridWidget.getY();
        int scissorX2 = scissorX1 + gridWidget.getWidth();
        int scissorY2 = scissorY1 + gridWidget.getHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        battlefield1PreviewRenderer.renderAt(guiGraphics, partialTick, originX, originY);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderKillFeedPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"subtitle/kill_feed".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSubtitlePreviewTriggerTime >= PREVIEW_SUBTITLE_TRIGGER_INTERVAL_MS) {
            int killType = PREVIEW_KILL_TYPES[previewKillTypeIndex % PREVIEW_KILL_TYPES.length];
            previewKillTypeIndex++;
            String weaponName = resolveRandomItemName();
            subtitlePreviewRenderer.triggerPreview(killType, weaponName, "Minecraft_GD656");
            lastSubtitlePreviewTriggerTime = now;
        }
        float originX = gridWidget.getOriginX();
        float originY = gridWidget.getOriginY();
        int scissorX1 = gridWidget.getX();
        int scissorY1 = gridWidget.getY();
        int scissorX2 = scissorX1 + gridWidget.getWidth();
        int scissorY2 = scissorY1 + gridWidget.getHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        subtitlePreviewRenderer.renderAt(guiGraphics, partialTick, originX, originY);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderComboSubtitlePreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"subtitle/combo".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastComboSubtitlePreviewTriggerTime >= PREVIEW_SUBTITLE_TRIGGER_INTERVAL_MS) {
            int combo = 1 + PREVIEW_RANDOM.nextInt(8);
            boolean isAssist = PREVIEW_RANDOM.nextBoolean();
            comboSubtitlePreviewRenderer.triggerPreview(isAssist, combo);
            lastComboSubtitlePreviewTriggerTime = now;
        }
        float originX = gridWidget.getOriginX();
        float originY = gridWidget.getOriginY();
        int scissorX1 = gridWidget.getX();
        int scissorY1 = gridWidget.getY();
        int scissorX2 = scissorX1 + gridWidget.getWidth();
        int scissorY2 = scissorY1 + gridWidget.getHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        
        
        
        comboSubtitlePreviewRenderer.renderAt(guiGraphics, partialTick, originX, originY);
        
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderScorePreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"subtitle/score".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastScorePreviewTriggerTime >= PREVIEW_SUBTITLE_TRIGGER_INTERVAL_MS) {
            float score = 0.1f + PREVIEW_RANDOM.nextFloat() * (100.0f - 0.1f);
            scorePreviewRenderer.addScore(score);
            lastScorePreviewTriggerTime = now;
        }
        float originX = gridWidget.getOriginX();
        float originY = gridWidget.getOriginY();
        int scissorX1 = gridWidget.getX();
        int scissorY1 = gridWidget.getY();
        int scissorX2 = scissorX1 + gridWidget.getWidth();
        int scissorY2 = scissorY1 + gridWidget.getHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        scorePreviewRenderer.renderAt(guiGraphics, partialTick, originX, originY);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderBonusListPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"subtitle/bonus_list".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBonusListPreviewTriggerTime >= PREVIEW_SUBTITLE_TRIGGER_INTERVAL_MS) {
            JsonObject config = ElementConfigManager.getElementConfig(presetId, "subtitle/bonus_list");
            if (config == null) {
                return;
            }
            boolean enableKillFeed = config.has("enable_kill_feed") && config.get("enable_kill_feed").getAsBoolean();
            String weaponName = enableKillFeed ? resolveRandomItemName() : "";
            String victimName = "Minecraft_GD656";
            List<Integer> bonusTypes = resolveRandomBonusTypes(PREVIEW_BONUS_COUNT);
            for (int type : bonusTypes) {
                float score = 0.1f + PREVIEW_RANDOM.nextFloat() * (100.0f - 0.1f);
                String extraData = resolveBonusPreviewExtraData(type, config);
                bonusListPreviewRenderer.triggerPreview(type, score, extraData, weaponName, victimName, config);
            }
            lastBonusListPreviewTriggerTime = now;
        }
        float originX = gridWidget.getOriginX();
        float originY = gridWidget.getOriginY();
        int scissorX1 = gridWidget.getX();
        int scissorY1 = gridWidget.getY();
        int scissorX2 = scissorX1 + gridWidget.getWidth();
        int scissorY2 = scissorY1 + gridWidget.getHeight();
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        bonusListPreviewRenderer.renderAt(guiGraphics, partialTick, originX, originY);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private String resolveRandomItemName() {
        for (int i = 0; i < 10; i++) {
            var holder = BuiltInRegistries.ITEM.getRandom(PREVIEW_RANDOM);
            if (holder.isPresent()) {
                Item item = holder.get().value();
                if (item != Items.AIR) {
                    return new ItemStack(item).getHoverName().getString();
                }
            }
        }
        return new ItemStack(Items.IRON_SWORD).getHoverName().getString();
    }

    private List<Integer> resolveRandomBonusTypes(int count) {
        List<Integer> types = new ArrayList<>();
        for (String name : BonusType.getAllNames()) {
            int type = BonusType.getTypeByName(name);
            if (type >= 0) {
                types.add(type);
            }
        }
        for (int i = types.size() - 1; i > 0; i--) {
            int j = PREVIEW_RANDOM.nextInt(i + 1);
            Collections.swap(types, i, j);
        }
        if (types.size() <= count) {
            return types;
        }
        return new ArrayList<>(types.subList(0, count));
    }

    private String resolveBonusPreviewExtraData(int type, JsonObject config) {
        int defaultValue = PREVIEW_RANDOM.nextInt(100) + 1;
        int oneBulletValue = PREVIEW_RANDOM.nextInt(9) + 2;
        String candidate = type == BonusType.ONE_BULLET_MULTI_KILL ? String.valueOf(oneBulletValue) : String.valueOf(defaultValue);
        String format = BonusListRenderer.getEffectiveFormat(type, candidate, config);
        if (format.contains("<combo>") || format.contains("<multi_kill>") || format.contains("<distance>") || format.contains("<streak>")) {
            return candidate;
        }
        return "";
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textureResetDialog != null && textureResetDialog.isVisible()) {
            return textureResetDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == 256) {             closeContent();
            return true;
        }
        return false;
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
                    JsonObject safeDefaults = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
                    if (!safeDefaults.entrySet().isEmpty()) {
                        ElementConfigManager.setElementConfig(presetId, elementId, safeDefaults);
                        ExternalTextureManager.resetTexturesForElement(presetId, elementId);
                    }
                    
                    isConfirmingReset = false;
                    closeContent();
                } else {
                    isConfirmingReset = true;
                    resetConfirmTime = System.currentTimeMillis();
                    btn.setMessage(Component.translatable("gd656killicon.client.gui.config.button.confirm_reset"));                     btn.setTextColor(GuiConstants.COLOR_RED);
                }
            });
        }
        
        
        resetButton.setX(x1);
        resetButton.setY(row1Y);
        resetButton.setWidth(row1ButtonWidth);
        resetButton.render(guiGraphics, mouseX, mouseY, partialTick);
        
        if (cancelButton == null) {
            cancelButton = new GDButton(x1 + row1ButtonWidth + 1, row1Y, row1ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.button.cancel"), (btn) -> {
                discardElementChangesAndClose();
            });
        }
        
        cancelButton.setX(x1 + row1ButtonWidth + 1);
        cancelButton.setY(row1Y);
        cancelButton.setWidth(row1ButtonWidth);
        cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int row2ButtonWidth = totalWidth;

        if (saveButton == null) {
            saveButton = new GDButton(x1, row2Y, row2ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.config.button.save_and_exit"), (btn) -> {
                closeContent();
            });
        }
        
        saveButton.setX(x1);
        saveButton.setY(row2Y);
        saveButton.setWidth(row2ButtonWidth);
        saveButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (textureResetDialog != null && textureResetDialog.isVisible()) {
            return textureResetDialog.mouseClicked(mouseX, mouseY, button);
        }
        if (promptDialog.isVisible()) {
            return promptDialog.mouseClicked(mouseX, mouseY, button);
        }
        if (getTextInputDialog().isVisible()) {
            return getTextInputDialog().mouseClicked(mouseX, mouseY, button);
        }
        if (getColorPickerDialog().isVisible()) {
            return getColorPickerDialog().mouseClicked(mouseX, mouseY, button);
        }

        if (button == 1 && "subtitle/score".equals(elementId) && gridWidget != null && gridWidget.isMouseOver(mouseX, mouseY)) {
            scorePreviewRenderer.resetPreview();
            return true;
        }
        
        if (gridWidget != null && gridWidget.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (saveButton != null && saveButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (handleSecondaryTabClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (handleSecondaryTabRelease()) {
            return true;
        }
        if (gridWidget != null && gridWidget.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (handleSecondaryTabDrag(mouseX, mouseY, dragX)) {
            return true;
        }
        if (gridWidget != null && gridWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (textureResetDialog != null && textureResetDialog.isVisible()) {
            return textureResetDialog.mouseScrolled(mouseX, mouseY, delta);
        }
        if (promptDialog.isVisible()) {
            return promptDialog.mouseScrolled(mouseX, mouseY, delta);
        }
        if (handleSecondaryTabScroll(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (textureResetDialog != null && textureResetDialog.isVisible()) {
            return textureResetDialog.charTyped(codePoint, modifiers);
        }
        if (promptDialog.isVisible()) {
            return promptDialog.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        if (!isKillIconElement() || !ElementTextureDefinition.hasTextures(elementId)) {
            return;
        }
        if (selectedSecondaryTab == null || "general".equals(selectedSecondaryTab.elementId)) {
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.texture_drop_wrong_tab"), PromptDialog.PromptType.WARNING, null);
            return;
        }
        
        Path pngPath = null;
        Path gifPath = null;
        
        for (Path path : paths) {
            String lower = path.toString().toLowerCase();
            if (lower.endsWith(".png")) {
                pngPath = path;
                break;
            } else if (lower.endsWith(".gif")) {
                gifPath = path;
                break;
            }
        }
        
        if (pngPath == null && gifPath == null) {
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.texture_drop_invalid"), PromptDialog.PromptType.ERROR, null);
            return;
        }
        
        String targetFileName = ElementTextureDefinition.getSelectedTextureFileName(presetId, elementId, selectedSecondaryTab.elementId, ElementConfigManager.getElementConfig(presetId, elementId));
        if (targetFileName == null) {
            return;
        }

        if (gifPath != null) {
            handleGifDrop(gifPath, targetFileName);
        } else if (pngPath != null) {
            boolean replaced = ExternalTextureManager.replaceTextureWithBackup(presetId, targetFileName, pngPath);
            if (replaced) {
                promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.texture_replace_success"), PromptDialog.PromptType.SUCCESS, null);
            }
        }
    }

    private void handleGifDrop(Path gifPath, String targetFileName) {
        try {
            File tempFile = File.createTempFile("gd656_gif_convert_", ".png");
            tempFile.deleteOnExit();
            
            GifToSpriteSheetConverter.ConversionResult result = GifToSpriteSheetConverter.convertGifToSpriteSheet(gifPath, tempFile.toPath());
            
            if (result.success && result.outputPng != null) {
                boolean replaced = ExternalTextureManager.replaceTextureWithBackup(presetId, targetFileName, result.outputPng.toPath());
                
                if (replaced) {
                    applyGifAnimationToSharedTextures(targetFileName, result);
                    
                    rebuildUI();
                    
                    promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.gif_import_success"), PromptDialog.PromptType.SUCCESS, null);
                } else {
                     promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.texture_replace_fail"), PromptDialog.PromptType.ERROR, null);
                }
            } else {
                promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.gif_convert_fail"), PromptDialog.PromptType.ERROR, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            promptDialog.show(I18n.get("gd656killicon.client.gui.prompt.gif_convert_fail") + ": " + e.getMessage(), PromptDialog.PromptType.ERROR, null);
        }
    }

    private void applyGifAnimationToSharedTextures(String targetFileName, GifToSpriteSheetConverter.ConversionResult result) {
        JsonObject current = ElementConfigManager.getElementConfig(presetId, elementId);
        if (current == null) current = new JsonObject();
        for (String textureKey : ElementTextureDefinition.getTextures(elementId)) {
            String selectedFile = ElementTextureDefinition.getSelectedTextureFileName(presetId, elementId, textureKey, current);
            if (targetFileName.equals(selectedFile)) {
                String prefix = "anim_" + textureKey + "_";
                current.addProperty(prefix + "enable_texture_animation", true);
                current.addProperty(prefix + "texture_animation_total_frames", result.frameCount);
                current.addProperty(prefix + "texture_animation_interval_ms", result.intervalMs);
                current.addProperty(prefix + "texture_animation_orientation", "vertical");
                current.addProperty(prefix + "texture_animation_loop", true);
            }
        }
        ElementConfigManager.setElementConfig(presetId, elementId, current);
    }

    private void resetTextureRelatedConfigs(String textureKey) {
        if (textureKey == null) {
            return;
        }
        JsonObject defaults = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
        if (defaults == null) {
            return;
        }
        JsonObject current = ElementConfigManager.getElementConfig(presetId, elementId);
        JsonObject target = current != null ? current : defaults.deepCopy();
        String prefix = "anim_" + textureKey + "_";
        String textureStyleKey = ElementTextureDefinition.getTextureStyleKey(textureKey);
        String[] keys = new String[] {
            prefix + "enable_texture_animation",
            prefix + "texture_animation_total_frames",
            prefix + "texture_animation_interval_ms",
            prefix + "texture_animation_orientation",
            prefix + "texture_animation_loop",
            prefix + "texture_animation_play_style",
            prefix + "texture_frame_width_ratio",
            prefix + "texture_frame_height_ratio",
            textureStyleKey
        };
        for (String key : keys) {
            if (defaults.has(key)) {
                target.add(key, defaults.get(key));
            }
        }
        ElementConfigManager.setElementConfig(presetId, elementId, target);
    }

    public boolean isKillIconElement() {
        return elementId != null && elementId.startsWith("kill_icon");
    }

    public boolean isMouseInSecondaryTabArea(double mouseX, double mouseY) {
        if (!isKillIconElement()) return false;
        updateSecondaryTabLayout(minecraft.getWindow().getGuiScaledWidth());
        return mouseX >= secondaryAreaX1 && mouseX <= secondaryAreaX2 && mouseY >= secondaryAreaTop && mouseY <= secondaryAreaBottom;
    }

    public int getSecondaryTabHeight() {
        return GuiConstants.ROW_HEADER_HEIGHT;
    }

    private int getContentTopOffset() {
        return isKillIconElement() ? getSecondaryTabHeight() + 1 : 0;
    }

    private void updateSecondaryTabLayout(int screenWidth) {
        if (!isKillIconElement()) {
            secondaryTabs.clear();
            selectedSecondaryTab = null;
            return;
        }
        ensureSecondaryTabs();

        int padding = GuiConstants.DEFAULT_PADDING;
        int area1Right = (screenWidth - 2 * padding) / 3 + padding;
        secondaryAreaX1 = area1Right + padding;
        secondaryAreaX2 = screenWidth - padding;
        int baseTop = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + padding;
        secondaryAreaTop = baseTop;
        secondaryAreaBottom = baseTop + getSecondaryTabHeight();

        int currentX = 0;
        for (SecondaryTab tab : secondaryTabs) {
            tab.width = minecraft.font.width(tab.label) + 10;
            tab.x = currentX;
            tab.height = getSecondaryTabHeight();
            currentX += tab.width + GuiConstants.TAB_SPACING;
        }
        int totalWidth = Math.max(0, currentX - GuiConstants.TAB_SPACING);
        int visibleWidth = secondaryAreaX2 - secondaryAreaX1;
        secondaryMaxScroll = Math.max(0, totalWidth - visibleWidth);
        secondaryTargetScrollX = Mth.clamp(secondaryTargetScrollX, 0, secondaryMaxScroll);
        if (secondaryScrollX > secondaryMaxScroll) secondaryScrollX = secondaryMaxScroll;
    }

    private void ensureSecondaryTabs() {
        if (!secondaryTabs.isEmpty()) return;
        
        if (!ElementTextureDefinition.hasTextures(elementId)) {
            return;
        }

        String generalKey = "gd656killicon.client.gui.config.tab.general";
        String generalLabel = I18n.exists(generalKey) ? I18n.get(generalKey) : "General";
        secondaryTabs.add(new SecondaryTab("general", generalLabel));
        
        List<String> textures = ElementTextureDefinition.getTextures(elementId);
        for (String texture : textures) {
            String key = "gd656killicon.client.gui.config.tab.texture." + texture;
            String label = I18n.exists(key) ? I18n.get(key) : texture;
            secondaryTabs.add(new SecondaryTab(texture, label));
        }

        if (selectedSecondaryTab == null && !secondaryTabs.isEmpty()) {
            selectedSecondaryTab = secondaryTabs.get(0);
        }
    }

    private String getElementDisplayName(String id) {
        String nameKey = "gd656killicon.element.name." + id.replace("/", ".");
        return I18n.exists(nameKey) ? I18n.get(nameKey) : id;
    }

    private void renderSecondaryTabs(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!isKillIconElement() || secondaryTabs.isEmpty()) {
            return;
        }
        float dt = 0.0f;
        long now = System.currentTimeMillis();
        if (lastSecondaryRenderTime > 0) {
            dt = (now - lastSecondaryRenderTime) / 1000.0f;
        }
        lastSecondaryRenderTime = now;

        if (!secondaryDragging) {
            double diff = secondaryTargetScrollX - secondaryScrollX;
            if (Math.abs(diff) < 0.1) {
                secondaryScrollX = secondaryTargetScrollX;
            } else {
                secondaryScrollX += diff * GuiConstants.SCROLL_SMOOTHING * dt;
            }
        }

        int localMouseX = (int)(mouseX - secondaryAreaX1 + secondaryScrollX);
        boolean inArea = mouseX >= secondaryAreaX1 && mouseX <= secondaryAreaX2 && mouseY >= secondaryAreaTop && mouseY <= secondaryAreaBottom;
        int borderColor = getRegionBorderColor();

        guiGraphics.enableScissor(secondaryAreaX1, secondaryAreaTop, secondaryAreaX2, secondaryAreaBottom);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float)(secondaryAreaX1 - secondaryScrollX), 0, 0);

        renderSecondaryBaseLine(guiGraphics, borderColor);
        JsonObject currentConfig = ElementConfigManager.getElementConfig(presetId, elementId);
        for (SecondaryTab tab : secondaryTabs) {
            boolean isSelected = tab == selectedSecondaryTab;
            boolean hovered = inArea && localMouseX >= tab.x && localMouseX <= tab.x + tab.width;
            boolean isModified = false;
            if (!"general".equals(tab.elementId)) {
                String fileName = ElementTextureDefinition.getSelectedTextureFileName(presetId, elementId, tab.elementId, currentConfig);
                boolean textureModified = fileName != null && ExternalTextureManager.isTextureModified(presetId, fileName);
                boolean styleModified = isTextureStyleModified(tab.elementId, currentConfig);
                isModified = textureModified || styleModified;
            }
            int baseColor = isSelected ? GuiConstants.COLOR_WHITE : (hovered ? GuiConstants.COLOR_DARK_GRAY : GuiConstants.COLOR_GRAY);
            if (isModified) {
                baseColor = isSelected ? GuiConstants.COLOR_GOLD : (hovered ? GuiConstants.COLOR_DARK_GOLD_ORANGE : GuiConstants.COLOR_GOLD_ORANGE);
            }
            int textX = tab.x + (tab.width - minecraft.font.width(tab.label)) / 2;
            int textY = secondaryAreaTop + (getSecondaryTabHeight() - 9) / 2;
            guiGraphics.drawString(minecraft.font, tab.label, textX, textY, baseColor, true);

            if (isSelected) {
                int borderY = secondaryAreaTop;
                guiGraphics.fill(tab.x, borderY, tab.x + tab.width, borderY + 1, borderColor);
                guiGraphics.fill(tab.x, borderY, tab.x + 1, secondaryAreaBottom, borderColor);
                guiGraphics.fill(tab.x + tab.width - 1, borderY, tab.x + tab.width, secondaryAreaBottom, borderColor);
            }
        }

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void resetPreviews() {
        scrollingPreviewRenderer = new ScrollingIconRenderer();
        comboPreviewRenderer = new ComboIconRenderer();
        cardPreviewRenderer = new CardRenderer();
        cardBarPreviewRenderer = new CardBarRenderer();
        battlefield1PreviewRenderer = new Battlefield1Renderer();
        subtitlePreviewRenderer = new SubtitleRenderer();
        comboSubtitlePreviewRenderer.resetPreview();
        scorePreviewRenderer.resetPreview();
        bonusListPreviewRenderer.resetPreview();
        
        lastPreviewTriggerTime = 0;
        lastComboPreviewTriggerTime = 0;
        lastCardPreviewTriggerTime = 0;
        lastCardBarPreviewTriggerTime = 0;
        lastBattlefieldPreviewTriggerTime = 0;
        lastSubtitlePreviewTriggerTime = 0;
        lastScorePreviewTriggerTime = 0;
        lastBonusListPreviewTriggerTime = 0;
    }

    private void closeContent() {
        resetPreviews();
        if (onClose != null) onClose.run();
    }

    private boolean handleSecondaryTabClick(double mouseX, double mouseY, int button) {
        if (!isKillIconElement()) return false;
        if (button != 0 && button != 1) return false;
        updateSecondaryTabLayout(minecraft.getWindow().getGuiScaledWidth());
        if (mouseX < secondaryAreaX1 || mouseX > secondaryAreaX2 || mouseY < secondaryAreaTop || mouseY > secondaryAreaBottom) return false;

        int localMouseX = (int)(mouseX - secondaryAreaX1 + secondaryScrollX);
        for (SecondaryTab tab : secondaryTabs) {
            if (localMouseX >= tab.x && localMouseX <= tab.x + tab.width) {
                if (button == 1) {
                    if ("general".equals(tab.elementId)) {
                        return false;
                    }
                    JsonObject currentConfig = ElementConfigManager.getElementConfig(presetId, elementId);
                    String fileName = ElementTextureDefinition.getSelectedTextureFileName(presetId, elementId, tab.elementId, currentConfig);
                    boolean textureModified = fileName != null && ExternalTextureManager.isTextureModified(presetId, fileName);
                    boolean styleModified = isTextureStyleModified(tab.elementId, currentConfig);
                    if (!textureModified && !styleModified) {
                        return false;
                    }
                    if (textureResetDialog != null) {
                        String textureKey = tab.elementId;
                        textureResetDialog.show("是否重置此材质", ConfirmDialog.PromptType.WARNING, () -> {
                            JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
                            List<String> affectedKeys = getTextureKeysBySelectedFile(config, fileName);
                            if (affectedKeys.isEmpty()) {
                                affectedKeys = new ArrayList<>();
                                affectedKeys.add(textureKey);
                            }
                            if (textureModified && fileName != null) {
                                ExternalTextureManager.resetTextureWithBackup(presetId, fileName);
                            }
                            for (String key : affectedKeys) {
                                resetTextureRelatedConfigs(key);
                            }
                            rebuildUI();
                        });
                    }
                    return true;
                }
                if (selectedSecondaryTab != tab) {
                    selectedSecondaryTab = tab;
                    refreshVisibleRows();
                    resetPreviews();
                }
                secondaryPressed = true;
                secondaryDragging = false;
                secondaryLastMouseX = mouseX;
                return true;
            }
        }
        if (button == 0) {
            secondaryPressed = true;
            secondaryDragging = false;
            secondaryLastMouseX = mouseX;
            return true;
        }
        return false;
    }

    private boolean isTextureStyleModified(String textureKey, JsonObject currentConfig) {
        if (textureKey == null) {
            return false;
        }
        JsonObject defaults = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
        if (defaults == null) {
            return false;
        }
        String textureStyleKey = ElementTextureDefinition.getTextureStyleKey(textureKey);
        if (textureStyleKey == null) {
            return false;
        }
        com.google.gson.JsonElement defaultValue = defaults.get(textureStyleKey);
        com.google.gson.JsonElement currentValue = currentConfig != null ? currentConfig.get(textureStyleKey) : null;
        if (defaultValue == null && currentValue == null) {
            return false;
        }
        return defaultValue == null || !defaultValue.equals(currentValue);
    }

    private List<String> getTextureKeysBySelectedFile(JsonObject currentConfig, String fileName) {
        if (fileName == null) {
            return new ArrayList<>();
        }
        List<String> keys = new ArrayList<>();
        for (String textureKey : ElementTextureDefinition.getTextures(elementId)) {
            String selectedFile = ElementTextureDefinition.getSelectedTextureFileName(presetId, elementId, textureKey, currentConfig);
            if (fileName.equals(selectedFile)) {
                keys.add(textureKey);
            }
        }
        return keys;
    }

    private void discardElementChangesAndClose() {
        ElementConfigManager.setElementConfig(presetId, elementId, initialConfig);
        ExternalTextureManager.revertPendingTextureReplacementsForElement(presetId, elementId);
        closeContent();
    }

    private boolean handleSecondaryTabRelease() {
        if (!isKillIconElement()) return false;
        if (secondaryDragging || secondaryPressed) {
            secondaryDragging = false;
            secondaryPressed = false;
            return true;
        }
        return false;
    }

    private boolean handleSecondaryTabDrag(double mouseX, double mouseY, double dragX) {
        if (!isKillIconElement()) return false;
        updateSecondaryTabLayout(minecraft.getWindow().getGuiScaledWidth());
        if (secondaryDragging) {
            secondaryTargetScrollX -= dragX;
            secondaryScrollX = secondaryTargetScrollX;
            clampSecondaryScroll();
            return true;
        }
        if (secondaryPressed && mouseY <= secondaryAreaTop + GuiConstants.HEADER_SCROLL_ZONE && mouseX >= secondaryAreaX1 && mouseX <= secondaryAreaX2) {
            secondaryDragging = true;
            secondaryTargetScrollX -= dragX;
            secondaryScrollX = secondaryTargetScrollX;
            clampSecondaryScroll();
            return true;
        }
        return false;
    }

    private boolean handleSecondaryTabScroll(double mouseX, double mouseY, double delta) {
        if (!isKillIconElement()) return false;
        updateSecondaryTabLayout(minecraft.getWindow().getGuiScaledWidth());
        if (mouseX >= secondaryAreaX1 && mouseX <= secondaryAreaX2 && mouseY <= secondaryAreaTop + GuiConstants.HEADER_SCROLL_ZONE) {
            secondaryTargetScrollX -= delta * GuiConstants.SCROLL_AMOUNT;
            clampSecondaryScroll();
            return true;
        }
        return false;
    }

    private void clampSecondaryScroll() {
        secondaryTargetScrollX = Mth.clamp(secondaryTargetScrollX, 0, secondaryMaxScroll);
        if (secondaryDragging) {
            secondaryScrollX = secondaryTargetScrollX;
        }
    }

    private int getRegionBorderColor() {
        return (0x80 << 24) | (GuiConstants.COLOR_GRAY & 0x00FFFFFF);
    }

    private void renderSecondaryBaseLine(GuiGraphics guiGraphics, int color) {
        int lineY = secondaryAreaBottom - 1;
        int drawEnd = Math.max((secondaryAreaX2 - secondaryAreaX1) + (int)secondaryMaxScroll, 5000);
        if (selectedSecondaryTab == null) {
            guiGraphics.fill(0, lineY, drawEnd, lineY + 1, color);
            return;
        }
        if (selectedSecondaryTab.x > 0) {
            guiGraphics.fill(0, lineY, selectedSecondaryTab.x, lineY + 1, color);
        }
        int tabRight = selectedSecondaryTab.x + selectedSecondaryTab.width;
        if (tabRight < drawEnd) {
            guiGraphics.fill(tabRight, lineY, drawEnd, lineY + 1, color);
        }
    }

    private static class SecondaryTab {
        String elementId;
        String label;
        int x;
        int width;
        int height;

        SecondaryTab(String elementId, String label) {
            this.elementId = elementId;
            this.label = label;
        }
    }
}
