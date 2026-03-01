package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.entries.BooleanConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.IntegerConfigEntry;
import org.mods.gd656killicon.client.gui.elements.ConfirmDialog;

import org.mods.gd656killicon.client.config.ClientConfigManager;

import net.minecraft.client.resources.language.I18n;

public class GlobalConfigTab extends ConfigTabContent {
    private ConfirmDialog aceLagConfirmDialog;
    public GlobalConfigTab(Minecraft minecraft) {
        super(minecraft, "gd656killicon.client.gui.config.tab.global");
        this.aceLagConfirmDialog = new ConfirmDialog(minecraft, null, null);
        
        this.configRows.add(new BooleanConfigEntry(
            0, 0, 0, 0, 
            GuiConstants.COLOR_BG, 
            (GuiConstants.COLOR_BG >>> 24) / 255.0f, 
            I18n.get("gd656killicon.client.gui.config.global.enable_sound"),
            "enable_sound",
            I18n.get("gd656killicon.client.gui.config.global.enable_sound.desc"),
            ClientConfigManager.isEnableSound(), 
            true,
            ClientConfigManager::setEnableSound
        ));

        this.configRows.add(new IntegerConfigEntry(
            0, 0, 0, 0,
            GuiConstants.COLOR_BG,
            (GuiConstants.COLOR_BG >>> 24) / 255.0f,
            I18n.get("gd656killicon.client.gui.config.global.sound_volume"),
            "sound_volume",
            I18n.get("gd656killicon.client.gui.config.global.sound_volume.desc"),
            ClientConfigManager.getSoundVolume(),
            100,
            ClientConfigManager::setSoundVolume,
            this.getTextInputDialog(),
            ClientConfigManager::isEnableSound,
            (value) -> {
                if (value == null || value.isEmpty()) return false;
                if (!value.matches("^-?\\d+$")) return false;
                try {
                    int parsed = Integer.parseInt(value);
                    return parsed >= 0 && parsed <= 200;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        ));

        this.configRows.add(new BooleanConfigEntry(
            0, 0, 0, 0,
            GuiConstants.COLOR_BG,
            (GuiConstants.COLOR_BG >>> 24) / 255.0f,
            I18n.get("gd656killicon.client.gui.config.global.disable_tacz_kill_sound"),
            "disable_tacz_kill_sound",
            I18n.get("gd656killicon.client.gui.config.global.disable_tacz_kill_sound.desc"),
            ClientConfigManager.isDisableTaczKillSound(),
            false,
            ClientConfigManager::setDisableTaczKillSound,
            () -> ModList.get().isLoaded("tacz")
        ));

        this.configRows.add(new BooleanConfigEntry(
            0, 0, 0, 0, 
            GuiConstants.COLOR_BG, 
            (GuiConstants.COLOR_BG >>> 24) / 255.0f, 
            I18n.get("gd656killicon.client.gui.config.global.enable_bonus_message"),
            "enable_bonus_message",
            I18n.get("gd656killicon.client.gui.config.global.enable_bonus_message.desc"),
            ClientConfigManager.isShowBonusMessage(), 
            false,
            ClientConfigManager::setShowBonusMessage
        ));

        this.configRows.add(new BooleanConfigEntry(
            0, 0, 0, 0, 
            GuiConstants.COLOR_BG, 
            (GuiConstants.COLOR_BG >>> 24) / 255.0f, 
            I18n.get("gd656killicon.client.gui.config.global.enable_ace_lag"),
            "enable_ace_lag",
            I18n.get("gd656killicon.client.gui.config.global.enable_ace_lag.desc"),
            ClientConfigManager.isEnableAceLag(), 
            false,
            ClientConfigManager::setEnableAceLag
        ));

        IntegerConfigEntry[] aceLagEntryRef = new IntegerConfigEntry[1];
        IntegerConfigEntry aceLagEntry = new IntegerConfigEntry(
            0, 0, 0, 0,
            GuiConstants.COLOR_BG,
            (GuiConstants.COLOR_BG >>> 24) / 255.0f,
            I18n.get("gd656killicon.client.gui.config.global.ace_lag_intensity"),
            "ace_lag_intensity",
            I18n.get("gd656killicon.client.gui.config.global.ace_lag_intensity.desc"),
            ClientConfigManager.getAceLagIntensity(),
            5,
            (value) -> {
                if (value > 50) {
                    aceLagConfirmDialog.show(I18n.get("gd656killicon.client.gui.prompt.ace_lag_confirm"), ConfirmDialog.PromptType.WARNING, () -> {
                        ClientConfigManager.setAceLagIntensity(value);
                    }, () -> {
                        if (aceLagEntryRef[0] != null) {
                            aceLagEntryRef[0].setValue(ClientConfigManager.getAceLagIntensity());
                        }
                    });
                } else {
                    ClientConfigManager.setAceLagIntensity(value);
                }
            },
            this.getTextInputDialog(),
            ClientConfigManager::isEnableAceLag,
            (value) -> {
                if (value == null || value.isEmpty()) return false;
                if (!value.matches("^-?\\d+$")) return false;
                try {
                    int parsed = Integer.parseInt(value);
                    return parsed >= 1 && parsed <= 100;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        );
        aceLagEntryRef[0] = aceLagEntry;
        this.configRows.add(aceLagEntry);
        
        sortConfigRows();
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int screenWidth, int screenHeight, int headerHeight) {
        boolean dialogVisible = aceLagConfirmDialog != null && aceLagConfirmDialog.isVisible();
        int effectiveMouseX = dialogVisible ? -1 : mouseX;
        int effectiveMouseY = dialogVisible ? -1 : mouseY;
        super.render(guiGraphics, effectiveMouseX, effectiveMouseY, partialTick, screenWidth, screenHeight, headerHeight);
        if (dialogVisible) {
            aceLagConfirmDialog.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (aceLagConfirmDialog != null && aceLagConfirmDialog.isVisible()) {
            return aceLagConfirmDialog.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (aceLagConfirmDialog != null && aceLagConfirmDialog.isVisible()) {
            return aceLagConfirmDialog.mouseScrolled(mouseX, mouseY, delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (aceLagConfirmDialog != null && aceLagConfirmDialog.isVisible()) {
            return aceLagConfirmDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (aceLagConfirmDialog != null && aceLagConfirmDialog.isVisible()) {
            return aceLagConfirmDialog.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
