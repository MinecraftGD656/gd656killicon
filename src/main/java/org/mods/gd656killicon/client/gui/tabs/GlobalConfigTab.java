package org.mods.gd656killicon.client.gui.tabs;

import net.minecraft.client.Minecraft;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.entries.BooleanConfigEntry;

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
        
        sortConfigRows();
    }
}
