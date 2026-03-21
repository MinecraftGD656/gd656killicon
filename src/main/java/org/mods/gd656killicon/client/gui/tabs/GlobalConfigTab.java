package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.entries.BooleanConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.FixedChoiceConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.HexColorConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.IntegerConfigEntry;
import org.mods.gd656killicon.client.gui.elements.ConfirmDialog;

import org.mods.gd656killicon.client.config.ClientConfigManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GlobalConfigTab extends ConfigTabContent {
    private ConfirmDialog aceLagConfirmDialog;
    private final List<FixedChoiceConfigEntry.Choice> vanillaBlockChoices;
    public GlobalConfigTab(Minecraft minecraft) {
        super(minecraft, "gd656killicon.client.gui.config.tab.global");
        this.aceLagConfirmDialog = new ConfirmDialog(minecraft, null, null);
        this.vanillaBlockChoices = buildVanillaBlockChoices();
        
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
            I18n.get("gd656killicon.client.gui.config.global.enable_icon_antialiasing"),
            "enable_icon_antialiasing",
            I18n.get("gd656killicon.client.gui.config.global.enable_icon_antialiasing.desc"),
            ClientConfigManager.isEnableIconAntialiasing(),
            true,
            ClientConfigManager::setEnableIconAntialiasing
        ));

        this.configRows.add(new FixedChoiceConfigEntry(
            0, 0, 0, 0,
            GuiConstants.COLOR_BG,
            (GuiConstants.COLOR_BG >>> 24) / 255.0f,
            I18n.get("gd656killicon.client.gui.config.global.single_line_subtitle_compression"),
            "single_line_subtitle_compression_mode",
            I18n.get("gd656killicon.client.gui.config.global.single_line_subtitle_compression.desc"),
            ClientConfigManager.getSingleLineSubtitleCompressionMode(),
            "scroll",
            List.of(
                new FixedChoiceConfigEntry.Choice("scroll", I18n.get("gd656killicon.client.gui.config.global.single_line_subtitle_compression.scroll")),
                new FixedChoiceConfigEntry.Choice("ellipsis", I18n.get("gd656killicon.client.gui.config.global.single_line_subtitle_compression.ellipsis"))
            ),
            ClientConfigManager::setSingleLineSubtitleCompressionMode,
            this.getChoiceListDialog()
        ));

        this.configRows.add(new HexColorConfigEntry(
            0, 0, 0, 0,
            GuiConstants.COLOR_BG,
            (GuiConstants.COLOR_BG >>> 24) / 255.0f,
            I18n.get("gd656killicon.client.gui.config.global.gui_theme_color_primary"),
            "gui_theme_color_primary",
            I18n.get("gd656killicon.client.gui.config.global.gui_theme_color_primary.desc"),
            ClientConfigManager.getGuiThemeColorPrimary(),
            "#FFB840",
            (value) -> {
                ClientConfigManager.setGuiThemeColorPrimary(value);
                showGuiReloadPrompt();
            },
            this.getTextInputDialog(),
            this.getColorPickerDialog()
        ));

        this.configRows.add(new HexColorConfigEntry(
            0, 0, 0, 0,
            GuiConstants.COLOR_BG,
            (GuiConstants.COLOR_BG >>> 24) / 255.0f,
            I18n.get("gd656killicon.client.gui.config.global.gui_theme_color_secondary"),
            "gui_theme_color_secondary",
            I18n.get("gd656killicon.client.gui.config.global.gui_theme_color_secondary.desc"),
            ClientConfigManager.getGuiThemeColorSecondary(),
            "#F29B3D",
            (value) -> {
                ClientConfigManager.setGuiThemeColorSecondary(value);
                showGuiReloadPrompt();
            },
            this.getTextInputDialog(),
            this.getColorPickerDialog()
        ));

        this.configRows.add(new HexColorConfigEntry(
            0, 0, 0, 0,
            GuiConstants.COLOR_BG,
            (GuiConstants.COLOR_BG >>> 24) / 255.0f,
            I18n.get("gd656killicon.client.gui.config.global.gui_theme_color_tertiary"),
            "gui_theme_color_tertiary",
            I18n.get("gd656killicon.client.gui.config.global.gui_theme_color_tertiary.desc"),
            ClientConfigManager.getGuiThemeColorTertiary(),
            "#E49A1C",
            (value) -> {
                ClientConfigManager.setGuiThemeColorTertiary(value);
                showGuiReloadPrompt();
            },
            this.getTextInputDialog(),
            this.getColorPickerDialog()
        ));

        this.configRows.add(new FixedChoiceConfigEntry(
            0, 0, 0, 0,
            GuiConstants.COLOR_BG,
            (GuiConstants.COLOR_BG >>> 24) / 255.0f,
            I18n.get("gd656killicon.client.gui.config.global.gui_background_material"),
            "gui_background_material",
            I18n.get("gd656killicon.client.gui.config.global.gui_background_material.desc"),
            ClientConfigManager.getGuiBackgroundMaterial(),
            "minecraft:cut_copper",
            vanillaBlockChoices,
            (value) -> {
                ClientConfigManager.setGuiBackgroundMaterial(value);
                showGuiReloadPrompt();
            },
            this.getChoiceListDialog()
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

    private void showGuiReloadPrompt() {
        promptDialog.show(
            I18n.get("gd656killicon.client.gui.prompt.gui_partial_reload"),
            org.mods.gd656killicon.client.gui.elements.PromptDialog.PromptType.INFO,
            null
        );
        promptDialog.setDismissible(false);
    }

    private List<FixedChoiceConfigEntry.Choice> buildVanillaBlockChoices() {
        List<FixedChoiceConfigEntry.Choice> choices = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key != null && "minecraft".equals(key.getNamespace()) && !"minecraft:air".equals(key.toString())) {
                blocks.add(block);
            }
        }
        blocks.sort(Comparator.comparing(block -> block.getName().getString()));
        for (Block block : blocks) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key == null) {
                continue;
            }
            BlockState state = block.defaultBlockState();
            if (!state.canOcclude() || !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                continue;
            }
            ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "textures/block/" + key.getPath() + ".png");
            if (minecraft.getResourceManager().getResource(textureLocation).isEmpty()) {
                continue;
            }
            String label = block.getName().getString();
            choices.add(new FixedChoiceConfigEntry.Choice(key.toString(), label + " (" + key + ")"));
        }
        return choices;
    }
}
