package org.mods.gd656killicon.client.gui;


import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.mods.gd656killicon.client.KeyBindings;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.client.gui.elements.PromptDialog;
import org.mods.gd656killicon.client.gui.tabs.ConfigTabContent;
import org.mods.gd656killicon.client.render.HudElementManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import java.net.URI;

public class MainConfigScreen extends Screen {
    private final Screen parent;
    private static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/gilded_blackstone.png");
    private final ConfigScreenHeader header;
    private boolean quickScoreboardMode = false;
    
    private GDButton btnExitNoSave;
    private GDButton btnCancel;
    private GDButton btnSaveExit;
    private boolean showExitConfirmation = false;
    private Runnable pendingExitAction;

    public MainConfigScreen(Screen parent) {
        super(Component.translatable("gd656killicon.client.gui.config.title"));
        this.parent = parent;
        header = new ConfigScreenHeader();
        ConfigManager.startEditing();
    }

    public MainConfigScreen(Screen parent, int initialTabIndex) {
        this(parent);
        header.setSelectedTab(initialTabIndex);
    }

    public MainConfigScreen(Screen parent, int initialTabIndex, boolean quickScoreboardMode) {
        this(parent);
        this.quickScoreboardMode = quickScoreboardMode;
        header.setSelectedTab(initialTabIndex);
    }

    public Screen getParentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        super.init();
        
        int btnWidth = 100;
        int btnHeight = GuiConstants.ROW_HEADER_HEIGHT;
        int spacing = 1;
        int totalWidth = btnWidth * 3 + spacing * 2;
        int startX = (width - totalWidth) / 2;
        
        int textHeight = font.lineHeight;
        int gap = 5;
        int groupHeight = textHeight + gap + btnHeight;
        int groupY = (height - groupHeight) / 2;
        int btnY = groupY + textHeight + gap;
        
        btnExitNoSave = new GDButton(startX, btnY, btnWidth, btnHeight, Component.translatable("gd656killicon.client.gui.config.exit_dialog.exit_no_save"), (btn) -> {
            pendingExitAction = () -> {
                ConfigManager.discardChanges();
                minecraft.setScreen(parent);
            };
        });
        
        btnCancel = new GDButton(startX + btnWidth + spacing, btnY, btnWidth, btnHeight, Component.translatable("gd656killicon.client.gui.config.exit_dialog.return"), (btn) -> {
            showExitConfirmation = false;
        });
        
        btnSaveExit = new GDButton(startX + (btnWidth + spacing) * 2, btnY, btnWidth, btnHeight, Component.translatable("gd656killicon.client.gui.config.exit_dialog.save_exit"), (btn) -> {
            pendingExitAction = () -> {
                boolean showAceLogo = ClientConfigManager.isEnableAceLag()
                    && ClientConfigManager.isAceLagConfigChangedInEdit();
                ConfigManager.saveChanges();
                minecraft.setScreen(parent);
                if (showAceLogo) {
                    HudElementManager.trigger("global", "ace_logo", IHudRenderer.TriggerContext.of(0, -1));
                }
            };
        });

        String languageCode = minecraft.options.languageCode;
        boolean languageChanged = ClientConfigManager.checkLanguageChangedAndUpdate(languageCode);
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null) {
            PromptDialog dialog = activeTab.getPromptDialog();
            boolean versionChanged = ClientConfigManager.checkModVersionChangedAndUpdate(GuiConstants.MOD_VERSION);
            Runnable showVersionPrompt = null;
            if (versionChanged) {
                String message = I18n.get("gd656killicon.client.gui.prompt.version_updated", getVersionColorText(GuiConstants.MOD_VERSION));
                showVersionPrompt = () -> dialog.showWithActionsCentered(
                    message,
                    PromptDialog.PromptType.INFO,
                    I18n.get("gd656killicon.client.gui.prompt.version_confirm"),
                    I18n.get("gd656killicon.client.gui.prompt.version_view"),
                    null,
                    () -> Util.getPlatform().openUri(URI.create(resolveOnlineVersionUrl()))
                );
            }
            Runnable showVersionPromptFinal = showVersionPrompt;
            if (ClientConfigManager.shouldShowConfigIntro()) {
                ClientConfigManager.markConfigIntroShown();
                Runnable showThird = () -> dialog.show(I18n.get("gd656killicon.client.gui.prompt.config_intro_3"), PromptDialog.PromptType.INFO, showVersionPromptFinal);
                Runnable showSecond = () -> dialog.show(I18n.get("gd656killicon.client.gui.prompt.config_intro_2"), PromptDialog.PromptType.INFO, showThird);
                Runnable showFirst = () -> dialog.show(I18n.get("gd656killicon.client.gui.prompt.config_intro_1"), PromptDialog.PromptType.INFO, showSecond);
                if (languageChanged) {
                    dialog.show(I18n.get("gd656killicon.client.gui.prompt.language_changed"), PromptDialog.PromptType.INFO, showFirst);
                } else {
                    showFirst.run();
                }
            } else if (languageChanged) {
                dialog.show(I18n.get("gd656killicon.client.gui.prompt.language_changed"), PromptDialog.PromptType.INFO, showVersionPromptFinal);
            } else if (showVersionPromptFinal != null) {
                showVersionPromptFinal.run();
            }
        }
    }

    private String resolveOnlineVersionUrl() {
        return GuiConstants.MOD_ONLINE_VERSION;
    }

    private String getVersionColorText(String version) {
        if (version == null) {
            return "";
        }
        if (version.endsWith("Alpha")) {
            return "§c" + version;
        }
        if (version.endsWith("Beta")) {
            return "§6" + version;
        }
        if (version.endsWith("Release")) {
            return "§a" + version;
        }
        return "§f" + version;
    }

    @Override
    public void onClose() {
        // 关闭配置界面时统一清除所有 HUD 渲染器的显示状态(预览/触发残留)
        org.mods.gd656killicon.client.render.HudElementManager.clearAllPreviews();
        if (ConfigManager.hasUnsavedChanges()) {
            showExitConfirmation = true;
        } else {
            ConfigManager.discardChanges();
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return !quickScoreboardMode;
    }

    public boolean isQuickScoreboardMode() {
        return quickScoreboardMode;
    }

    @Override
    public void tick() {
        super.tick();
    }

    public boolean shouldCloseQuickScoreboardOnRelease() {
        if (!quickScoreboardMode || showExitConfirmation) {
            return false;
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab == null) {
            return true;
        }
        if (!(activeTab instanceof org.mods.gd656killicon.client.gui.tabs.ScoreboardTab)) {
            return false;
        }
        if (activeTab.getChoiceListDialog().isVisible()) {
            return false;
        }
        if (activeTab.getTextInputDialog().isVisible()) {
            return false;
        }
        if (activeTab.getColorPickerDialog().isVisible()) {
            return false;
        }
        if (activeTab.getPromptDialog().isVisible()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (showExitConfirmation) return false;
        return !quickScoreboardMode;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (minecraft != null && minecraft.level != null) {
            guiGraphics.fillGradient(0, 0, this.width, this.height, GuiConstants.COLOR_SCREEN_DIM_TOP, GuiConstants.COLOR_SCREEN_DIM_BOTTOM);
        } else {
            renderGildedBlackstoneBackground(guiGraphics);
        }

        if (showExitConfirmation) {
            int textHeight = font.lineHeight;
            int gap = 5;
            int groupHeight = textHeight + gap + GuiConstants.ROW_HEADER_HEIGHT;
            int groupY = (height - groupHeight) / 2;
            guiGraphics.centeredText(font, Component.translatable("gd656killicon.client.gui.config.exit_dialog.title"), width / 2, groupY, GuiConstants.COLOR_WHITE);
            
            if (btnExitNoSave != null) btnExitNoSave.render(guiGraphics, mouseX, mouseY, delta);
            if (btnCancel != null) btnCancel.render(guiGraphics, mouseX, mouseY, delta);
            if (btnSaveExit != null) btnSaveExit.render(guiGraphics, mouseX, mouseY, delta);
            
            return;
        }

        header.render(guiGraphics, width, mouseX, mouseY, delta);
        
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null) {
            activeTab.render(guiGraphics, mouseX, mouseY, delta, width, height, GuiConstants.HEADER_HEIGHT);
        }
        
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (showExitConfirmation) {
            if (btnExitNoSave != null && btnExitNoSave.mouseClicked(event.x(), event.y(), event.button())) return true;
            if (btnCancel != null && btnCancel.mouseClicked(event.x(), event.y(), event.button())) return true;
            if (btnSaveExit != null && btnSaveExit.mouseClicked(event.x(), event.y(), event.button())) return true;
            return true;
        }
        if (header.mouseClicked(event.x(), event.y(), event.button())) {
            this.quickScoreboardMode = false;
            return true;
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (showExitConfirmation) {
            if (pendingExitAction != null && event.button() == 0) {
                Runnable action = pendingExitAction;
                pendingExitAction = null;
                action.run();
                return true;
            }
            return true;
        }
        if (header.mouseReleased(event.x(), event.y(), event.button())) {
            return true;
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.mouseReleased(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (showExitConfirmation) {
            return super.mouseDragged(event, dragX, dragY);
        }
        if (header.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY)) {
            return true;
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public void onFilesDrop(java.util.List<java.nio.file.Path> paths) {
        if (showExitConfirmation) return;
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null) {
            activeTab.onFilesDrop(paths);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        if (showExitConfirmation) {
            return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab instanceof org.mods.gd656killicon.client.gui.tabs.ElementConfigContent elementContent
            && elementContent.isMouseInSecondaryTabArea(mouseX, mouseY)) {
            if (activeTab.mouseScrolled(mouseX, mouseY, amountY)) {
                return true;
            }
        }
        if (header.mouseScrolled(mouseX, mouseY, amountY)) {
            return true;
        }
        if (activeTab != null && activeTab.mouseScrolled(mouseX, mouseY, amountY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
    }
    @Override
    public boolean charTyped(CharacterEvent event) {
        if (showExitConfirmation) {
            return super.charTyped(event);
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.charTyped((char) event.codepoint(), 0)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        if (showExitConfirmation) {
            if (keyCode == 256) {                 showExitConfirmation = false;
                return true;
            }
            return super.keyPressed(event);
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.keyPressed(event)) {
            return true;
        }
        if (quickScoreboardMode && keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void renderGildedBlackstoneBackground(GuiGraphicsExtractor guiGraphics) {
        int bgTint = 0xFF3F3F3F;
        int size = 32;         int cols = width / size + 1;
        int rows = height / size + 1;

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resolveBackgroundTexture(), x * size, y * size, 0, 0, size, size, size, size, bgTint);
            }
        }
    }

    private Identifier resolveBackgroundTexture() {
        String material = ClientConfigManager.getGuiBackgroundMaterial();
        if (material == null || !material.contains(":")) {
            return BACKGROUND_TEXTURE;
        }
        String[] split = material.split(":", 2);
        if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty()) {
            return BACKGROUND_TEXTURE;
        }
        Identifier texture = Identifier.fromNamespaceAndPath(split[0], "textures/block/" + split[1] + ".png");
        if (minecraft == null || minecraft.getResourceManager().getResource(texture).isEmpty()) {
            return BACKGROUND_TEXTURE;
        }
        return texture;
    }
}
