package org.mods.gd656killicon.client.gui.tabs;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.InfiniteGridWidget;
import org.mods.gd656killicon.client.gui.screens.ElementConfigBuilder;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.impl.Battlefield1Renderer;
import org.mods.gd656killicon.client.render.impl.CardBarRenderer;
import org.mods.gd656killicon.client.render.impl.CardRenderer;
import org.mods.gd656killicon.client.render.impl.BonusListRenderer;
import org.mods.gd656killicon.client.render.impl.ComboIconRenderer;
import org.mods.gd656killicon.client.render.impl.ScrollingIconRenderer;
import org.mods.gd656killicon.client.render.impl.ScoreSubtitleRenderer;
import org.mods.gd656killicon.client.render.impl.SubtitleRenderer;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElementConfigContent extends ConfigTabContent {
    private final String presetId;
    private final String elementId;
    private final ElementConfigBuilder builder;
    private final Runnable onClose;
    private final JsonObject initialConfig;

    private GDTextRenderer elementInfoRenderer;
    private int calculatedBottom;
    private InfiniteGridWidget gridWidget;
    private final ScrollingIconRenderer scrollingPreviewRenderer = new ScrollingIconRenderer();
    private long lastPreviewTriggerTime = 0L;
    private int previewKillTypeIndex = 0;
    private final ComboIconRenderer comboPreviewRenderer = new ComboIconRenderer();
    private long lastComboPreviewTriggerTime = 0L;
    private int previewComboCount = 1;
    private int previewComboKillTypeIndex = 0;
    private final CardRenderer cardPreviewRenderer = new CardRenderer();
    private long lastCardPreviewTriggerTime = 0L;
    private int previewCardComboCount = 1;
    private int previewCardKillTypeIndex = 0;
    private final CardBarRenderer cardBarPreviewRenderer = new CardBarRenderer();
    private long lastCardBarPreviewTriggerTime = 0L;
    private int previewCardBarComboCount = 1;
    private int previewCardBarKillTypeIndex = 0;
    private final Battlefield1Renderer battlefield1PreviewRenderer = new Battlefield1Renderer();
    private long lastBattlefieldPreviewTriggerTime = 0L;
    private int previewBattlefieldKillTypeIndex = 0;
    private final SubtitleRenderer subtitlePreviewRenderer = new SubtitleRenderer();
    private long lastSubtitlePreviewTriggerTime = 0L;
    private final ScoreSubtitleRenderer scorePreviewRenderer = ScoreSubtitleRenderer.getInstance();
    private long lastScorePreviewTriggerTime = 0L;
    private final BonusListRenderer bonusListPreviewRenderer = BonusListRenderer.getInstance();
    private long lastBonusListPreviewTriggerTime = 0L;

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
    
    // Use local field to avoid any interference from parent class logic
    private boolean isConfirmingReset = false;
    private long resetConfirmTime = 0;

    public ElementConfigContent(Minecraft minecraft, String presetId, String elementId, ElementConfigBuilder builder, Runnable onClose) {
        super(minecraft, Component.translatable("gd656killicon.client.gui.config.element.title"));
        this.presetId = presetId;
        this.elementId = elementId;
        this.builder = builder;
        this.onClose = onClose;
        
        // Store initial config state for cancellation
        JsonObject current = ElementConfigManager.getElementConfig(presetId, elementId);
        this.initialConfig = current != null ? current.deepCopy() : new JsonObject();
        
        rebuildUI();
    }
    
    private void rebuildUI() {
        this.configRows.clear();
        if (builder != null) {
            builder.build(this);
        }
        sortConfigRows();
    }

    public String getPresetId() {
        return presetId;
    }

    public String getElementId() {
        return elementId;
    }
    
    @Override
    protected void updateSubtitle(int x1, int y1, int x2) {
        // Line 1: "当前预设：" [presetId] presetName
        String presetName = ElementConfigManager.getPresetDisplayName(presetId);
        
        List<GDTextRenderer.ColoredText> presetTexts = new ArrayList<>();
        presetTexts.add(new GDTextRenderer.ColoredText(I18n.get("gd656killicon.client.gui.config.element.current_preset"), GuiConstants.COLOR_WHITE));
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

        // Line 2: "当前元素：" [elementId] elementName
        String nameKey = "gd656killicon.element.name." + elementId.replace("/", ".");
        String elementName = I18n.exists(nameKey) ? I18n.get(nameKey) : elementId;

        List<GDTextRenderer.ColoredText> elementTexts = new ArrayList<>();
        elementTexts.add(new GDTextRenderer.ColoredText(I18n.get("gd656killicon.client.gui.config.element.current_element"), GuiConstants.COLOR_WHITE));
        elementTexts.add(new GDTextRenderer.ColoredText("[" + elementId + "] ", GuiConstants.COLOR_GRAY));
        elementTexts.add(new GDTextRenderer.ColoredText(elementName, GuiConstants.COLOR_GOLD));

        int line2Y = y1 + 9 + 2; // Reduced spacing from 4 to 2

        if (elementInfoRenderer == null) {
            elementInfoRenderer = new GDTextRenderer(elementTexts, x1, line2Y, x2, line2Y + 9, 1.0f, false);
        } else {
            elementInfoRenderer.setX1(x1);
            elementInfoRenderer.setY1(line2Y);
            elementInfoRenderer.setX2(x2);
            elementInfoRenderer.setY2(line2Y + 9);
            elementInfoRenderer.setColoredTexts(elementTexts);
        }

        // Store the calculated bottom
        this.calculatedBottom = line2Y + 9;
    }

    @Override
    public void updateLayout(int screenWidth, int screenHeight) {
        super.updateLayout(screenWidth, screenHeight);
        // Correct area1Bottom after super.updateLayout overwrites it
        this.area1Bottom = this.calculatedBottom;
        
        // Calculate Grid Widget bounds
        int padding = GuiConstants.DEFAULT_PADDING;
        int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
        // Buttons at bottom left
        // row2Y = screenHeight - padding - buttonHeight
        // row1Y = row2Y - 1 - buttonHeight
        int row2Y = screenHeight - padding - buttonHeight;
        int row1Y = row2Y - 1 - buttonHeight;
        
        int gridX = padding;
        int gridY = this.calculatedBottom + padding;
        int area1Right = (screenWidth - 2 * padding) / 3 + padding;
        int gridWidth = area1Right - padding;
        // Leave some padding above buttons
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        // Render Grid Widget first (behind info/dialogs if any overlap, though unlikely)
        if (gridWidget == null) {
            updateLayout(screenWidth, screenHeight);
        }
        if (gridWidget != null) {
            guiGraphics.pose().pushPose();
            gridWidget.render(guiGraphics, mouseX, mouseY, partialTick, Collections.emptyList());
            guiGraphics.pose().popPose();
            renderScrollingPreview(guiGraphics, partialTick);
            renderComboPreview(guiGraphics, partialTick);
            renderCardPreview(guiGraphics, partialTick);
            renderCardBarPreview(guiGraphics, partialTick);
            renderBattlefieldPreview(guiGraphics, partialTick);
            renderKillFeedPreview(guiGraphics, partialTick);
            renderScorePreview(guiGraphics, partialTick);
            renderBonusListPreview(guiGraphics, partialTick);
        }

        // Render the second line of info BEFORE super.render() to ensure it appears under the dialog background (if any)
        // This prevents the text from failing the depth test against the dialog's Z=500 background
        if (elementInfoRenderer != null) {
            elementInfoRenderer.render(guiGraphics, partialTick);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick, screenWidth, screenHeight, headerHeight);
        
        // Handle reset button timer logic locally (Silent timeout)
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
    }

    private void renderScrollingPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"kill_icon/scrolling".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPreviewTriggerTime >= PREVIEW_TRIGGER_INTERVAL_MS) {
            int killType = PREVIEW_KILL_TYPES[previewKillTypeIndex % PREVIEW_KILL_TYPES.length];
            previewKillTypeIndex++;
            scrollingPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1));
            lastPreviewTriggerTime = now;
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
        if (now - lastComboPreviewTriggerTime >= PREVIEW_COMBO_TRIGGER_INTERVAL_MS) {
            int killType = PREVIEW_COMBO_KILL_TYPES[previewComboKillTypeIndex % PREVIEW_COMBO_KILL_TYPES.length];
            int comboCount = previewComboCount;
            previewComboKillTypeIndex++;
            previewComboCount = previewComboCount >= 6 ? 1 : previewComboCount + 1;
            comboPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1, comboCount));
            lastComboPreviewTriggerTime = now;
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
        if (now - lastCardPreviewTriggerTime >= PREVIEW_CARD_TRIGGER_INTERVAL_MS) {
            int killType = PREVIEW_COMBO_KILL_TYPES[previewCardKillTypeIndex % PREVIEW_COMBO_KILL_TYPES.length];
            int comboCount = previewCardComboCount;
            previewCardKillTypeIndex++;
            previewCardComboCount = previewCardComboCount >= 6 ? 1 : previewCardComboCount + 1;
            cardPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1, comboCount));
            lastCardPreviewTriggerTime = now;
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
        JsonObject config = ElementConfigManager.getElementConfig(presetId, elementId);
        cardPreviewRenderer.renderPreviewAt(guiGraphics, partialTick, originX, originY, config);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderCardBarPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"kill_icon/card_bar".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastCardBarPreviewTriggerTime >= PREVIEW_CARD_BAR_TRIGGER_INTERVAL_MS) {
            int killType = PREVIEW_COMBO_KILL_TYPES[previewCardBarKillTypeIndex % PREVIEW_COMBO_KILL_TYPES.length];
            int comboCount = previewCardBarComboCount;
            previewCardBarKillTypeIndex++;
            previewCardBarComboCount = previewCardBarComboCount >= 6 ? 1 : previewCardBarComboCount + 1;
            cardBarPreviewRenderer.trigger(IHudRenderer.TriggerContext.of(killType, -1, comboCount));
            lastCardBarPreviewTriggerTime = now;
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
        cardBarPreviewRenderer.renderAt(guiGraphics, partialTick, originX, originY);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        guiGraphics.disableScissor();
    }

    private void renderBattlefieldPreview(GuiGraphics guiGraphics, float partialTick) {
        if (!"kill_icon/battlefield1".equals(elementId) || gridWidget == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBattlefieldPreviewTriggerTime >= PREVIEW_BATTLEFIELD_TRIGGER_INTERVAL_MS) {
            int killType = PREVIEW_BATTLEFIELD_KILL_TYPES[previewBattlefieldKillTypeIndex % PREVIEW_BATTLEFIELD_KILL_TYPES.length];
            previewBattlefieldKillTypeIndex++;
            String weaponName = resolveRandomItemName();
            battlefield1PreviewRenderer.triggerPreview(killType, weaponName, "Minecraft_GD656", "20");
            lastBattlefieldPreviewTriggerTime = now;
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
        // Let super (TextInputDialog) handle it first
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == 256) { // ESC
            if (onClose != null) onClose.run();
            return true;
        }
        return false;
    }
    
    @Override
    protected void updateResetButtonState() {
        // Disable default timer logic from parent
    }
    
    @Override
    protected void renderSideButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight) {
        // Calculate button area
        int area1Right = (screenWidth - 2 * GuiConstants.DEFAULT_PADDING) / 3 + GuiConstants.DEFAULT_PADDING;
        int buttonHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int padding = GuiConstants.DEFAULT_PADDING;
        
        // Base Y position for the bottom row (Save & Exit)
        int row2Y = screenHeight - padding - buttonHeight;
        // Base Y position for the top row (Reset & Cancel)
        int row1Y = row2Y - 1 - buttonHeight;
        
        // Total width available for buttons
        int totalWidth = area1Right - padding;
        int x1 = padding + (int)getSidebarOffset();

        // Row 1: Reset and Cancel (50% each, 1px spacing)
        int row1ButtonWidth = (totalWidth - 1) / 2;

        // 1. Reset Button (Top Left)
        if (resetButton == null) {
            resetButton = new GDButton(x1, row1Y, row1ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.config.button.reset_element"), (btn) -> {
                if (isConfirmingReset) {
                    // Confirm Reset
                    JsonObject safeDefaults = ElementConfigManager.getDefaultElementConfig(presetId, elementId);
                    if (!safeDefaults.entrySet().isEmpty()) {
                        ElementConfigManager.setElementConfig(presetId, elementId, safeDefaults);
                        // No need to rebuild UI locally since we are exiting
                    }
                    
                    isConfirmingReset = false;
                    // Exit immediately
                    if (onClose != null) onClose.run();
                } else {
                    // Request Confirmation
                    isConfirmingReset = true;
                    resetConfirmTime = System.currentTimeMillis();
                    btn.setMessage(Component.translatable("gd656killicon.client.gui.config.button.confirm_reset")); // No countdown text
                    btn.setTextColor(GuiConstants.COLOR_RED);
                }
            });
        }
        
        // Appearance update is now handled in render() to support countdown
        
        resetButton.setX(x1);
        resetButton.setY(row1Y);
        resetButton.setWidth(row1ButtonWidth);
        resetButton.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 2. Cancel Button (Top Right)
        if (cancelButton == null) {
            cancelButton = new GDButton(x1 + row1ButtonWidth + 1, row1Y, row1ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.button.cancel"), (btn) -> {
                // Revert changes
                ElementConfigManager.setElementConfig(presetId, elementId, initialConfig);
                if (onClose != null) onClose.run();
            });
        }
        
        cancelButton.setX(x1 + row1ButtonWidth + 1);
        cancelButton.setY(row1Y);
        cancelButton.setWidth(row1ButtonWidth);
        cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Row 2: Save & Exit (Full Width)
        int row2ButtonWidth = totalWidth;

        // 3. Save & Exit Button (Bottom Full Width)
        if (saveButton == null) {
            saveButton = new GDButton(x1, row2Y, row2ButtonWidth, buttonHeight, Component.translatable("gd656killicon.client.gui.config.button.save_and_exit"), (btn) -> {
                // Changes are already in memory, just close
                if (onClose != null) onClose.run();
            });
        }
        
        saveButton.setX(x1);
        saveButton.setY(row2Y);
        saveButton.setWidth(row2ButtonWidth);
        saveButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getTextInputDialog().isVisible()) {
            return getTextInputDialog().mouseClicked(mouseX, mouseY, button);
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (gridWidget != null && gridWidget.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (gridWidget != null && gridWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
