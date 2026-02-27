package org.mods.gd656killicon.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.gui.elements.GDButton;
import org.mods.gd656killicon.client.gui.elements.PromptDialog;
import org.mods.gd656killicon.client.gui.tabs.ConfigTabContent;
import org.mods.gd656killicon.client.render.HudElementManager;
import org.mods.gd656killicon.client.render.IHudRenderer;

public class MainConfigScreen extends Screen {
    private final Screen parent;
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gilded_blackstone.png");
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
        if (ClientConfigManager.checkLanguageChangedAndUpdate(languageCode)) {
            ConfigTabContent activeTab = header.getSelectedTabContent();
            if (activeTab != null) {
                activeTab.getPromptDialog().show(I18n.get("gd656killicon.client.gui.prompt.language_changed"), PromptDialog.PromptType.INFO, null);
            }
        }
    }

    @Override
    public void onClose() {
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
    public boolean shouldCloseOnEsc() {
        if (showExitConfirmation) return false;
        return !quickScoreboardMode;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (showExitConfirmation) {
            if (minecraft.level == null) {
                renderGildedBlackstoneBackground(guiGraphics);
            } else {
                renderBackground(guiGraphics);
            }
            
            int textHeight = font.lineHeight;
            int gap = 5;
            int groupHeight = textHeight + gap + GuiConstants.ROW_HEADER_HEIGHT;
            int groupY = (height - groupHeight) / 2;
            guiGraphics.drawCenteredString(font, Component.translatable("gd656killicon.client.gui.config.exit_dialog.title"), width / 2, groupY, GuiConstants.COLOR_WHITE);
            
            if (btnExitNoSave != null) btnExitNoSave.render(guiGraphics, mouseX, mouseY, partialTick);
            if (btnCancel != null) btnCancel.render(guiGraphics, mouseX, mouseY, partialTick);
            if (btnSaveExit != null) btnSaveExit.render(guiGraphics, mouseX, mouseY, partialTick);
            
            return;
        }

        if (minecraft.level == null) {
            renderGildedBlackstoneBackground(guiGraphics);
        } else {
            renderBackground(guiGraphics);
        }
        
        header.render(guiGraphics, width, mouseX, mouseY, partialTick);
        
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null) {
            activeTab.render(guiGraphics, mouseX, mouseY, partialTick, width, height, GuiConstants.HEADER_HEIGHT);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showExitConfirmation) {
            if (btnExitNoSave != null && btnExitNoSave.mouseClicked(mouseX, mouseY, button)) return true;
            if (btnCancel != null && btnCancel.mouseClicked(mouseX, mouseY, button)) return true;
            if (btnSaveExit != null && btnSaveExit.mouseClicked(mouseX, mouseY, button)) return true;
            return true;
        }
        if (header.mouseClicked(mouseX, mouseY, button)) {
            this.quickScoreboardMode = false;
            return true;
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (showExitConfirmation) {
            if (pendingExitAction != null && button == 0) {
                Runnable action = pendingExitAction;
                pendingExitAction = null;
                action.run();
                return true;
            }
            return true;
        }
        if (header.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (showExitConfirmation) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (header.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showExitConfirmation) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab instanceof org.mods.gd656killicon.client.gui.tabs.ElementConfigContent elementContent
            && elementContent.isMouseInSecondaryTabArea(mouseX, mouseY)) {
            if (activeTab.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        if (header.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (activeTab != null && activeTab.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (showExitConfirmation) {
            return super.charTyped(codePoint, modifiers);
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showExitConfirmation) {
            if (keyCode == 256) {                 showExitConfirmation = false;
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        ConfigTabContent activeTab = header.getSelectedTabContent();
        if (activeTab != null && activeTab.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderGildedBlackstoneBackground(GuiGraphics guiGraphics) {
        RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
        RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);         
        int size = 32;         int cols = width / size + 1;
        int rows = height / size + 1;

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                guiGraphics.blit(BACKGROUND_TEXTURE, x * size, y * size, 0, 0, size, size, size, size);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
