package org.mods.gd656killicon.client;

import org.mods.gd656killicon.client.bridge.ClientBridge;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;

public class ClientSetup {
    public static void initializeClient() {
        org.mods.gd656killicon.client.stats.ClientStatsManager.init();
        org.mods.gd656killicon.client.config.ConfigManager.init();
        ExternalTextureManager.init();
        org.mods.gd656killicon.client.sounds.ExternalSoundManager.init();
        org.mods.gd656killicon.client.render.HudElementManager.init();
        org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "scrolling", org.mods.gd656killicon.client.render.impl.ScrollingIconRenderer.getInstance());
        org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "combo", new org.mods.gd656killicon.client.render.impl.ComboIconRenderer());
        org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "valorant", new org.mods.gd656killicon.client.render.impl.ValorantIconRenderer());
        org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "card_bar", new org.mods.gd656killicon.client.render.impl.CardBarRenderer());
        org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "card", new org.mods.gd656killicon.client.render.impl.CardRenderer());
        org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "battlefield1", new org.mods.gd656killicon.client.render.impl.Battlefield1Renderer());
        org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "honor", org.mods.gd656killicon.client.render.impl.HonorRenderer.getInstance());
        org.mods.gd656killicon.client.render.HudElementManager.register("subtitle", "kill_feed", org.mods.gd656killicon.client.render.impl.SubtitleRenderer.getInstance());
        org.mods.gd656killicon.client.render.HudElementManager.register("subtitle", "score", org.mods.gd656killicon.client.render.impl.ScoreSubtitleRenderer.getInstance());
        org.mods.gd656killicon.client.render.HudElementManager.register("subtitle", "combo", org.mods.gd656killicon.client.render.impl.ComboSubtitleRenderer.getInstance());
        org.mods.gd656killicon.client.render.HudElementManager.register("subtitle", "bonus_list", org.mods.gd656killicon.client.render.impl.BonusListRenderer.getInstance());
        org.mods.gd656killicon.client.render.HudElementManager.register("subtitle", "hit_info", org.mods.gd656killicon.client.render.impl.HitInfoRenderer.getInstance());
        org.mods.gd656killicon.client.render.HudElementManager.register("global", "ace_logo", new org.mods.gd656killicon.client.render.impl.AceLogoRenderer());
        registerConfigScreen();
        org.mods.gd656killicon.client.command.ClientCommand.init();
    }

    public static void registerConfigScreen() {
        ClientBridge.loader().registerConfigScreen();
    }
}
