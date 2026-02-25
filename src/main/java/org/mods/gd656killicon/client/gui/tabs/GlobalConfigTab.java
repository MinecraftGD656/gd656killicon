package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.entries.BooleanConfigEntry;
import org.mods.gd656killicon.client.gui.elements.entries.IntegerConfigEntry;

import org.mods.gd656killicon.client.config.ClientConfigManager;

import net.minecraft.client.resources.language.I18n;

public class GlobalConfigTab extends ConfigTabContent {
    public GlobalConfigTab(Minecraft minecraft) {
        super(minecraft, "gd656killicon.client.gui.config.tab.global");
        
        // 添加 "是否启用模组音效" 配置项
        // 初始位置设为0，实际布局由 updateConfigRowsLayout 控制
        // 默认开启
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

        // 添加 "是否启用客户端聊天框加分项提示信息" 配置项
        // 初始位置设为0，实际布局由 updateConfigRowsLayout 控制
        // 默认关闭
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

        // 添加 "是否启用ACE扫盘模拟" 配置项
        // 默认关闭
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

        // 添加 "ACE扫盘模拟强度" 配置项
        // 范围 1-10
        this.configRows.add(new IntegerConfigEntry(
            0, 0, 0, 0,
            GuiConstants.COLOR_BG,
            (GuiConstants.COLOR_BG >>> 24) / 255.0f,
            I18n.get("gd656killicon.client.gui.config.global.ace_lag_intensity"),
            "ace_lag_intensity",
            I18n.get("gd656killicon.client.gui.config.global.ace_lag_intensity.desc"),
            ClientConfigManager.getAceLagIntensity(),
            5,
            ClientConfigManager::setAceLagIntensity,
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
        ));
        
        sortConfigRows();
    }
}
