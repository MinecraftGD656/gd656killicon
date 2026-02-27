package org.mods.gd656killicon.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            org.mods.gd656killicon.client.stats.ClientStatsManager.init();
            org.mods.gd656killicon.client.config.ConfigManager.init();
            ExternalTextureManager.init();
            org.mods.gd656killicon.client.sounds.ExternalSoundManager.init();
            org.mods.gd656killicon.client.render.HudElementManager.init();
            org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "scrolling", new org.mods.gd656killicon.client.render.impl.ScrollingIconRenderer());
            org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "combo", new org.mods.gd656killicon.client.render.impl.ComboIconRenderer());
            org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "card_bar", new org.mods.gd656killicon.client.render.impl.CardBarRenderer());
            org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "card", new org.mods.gd656killicon.client.render.impl.CardRenderer());
            org.mods.gd656killicon.client.render.HudElementManager.register("kill_icon", "battlefield1", new org.mods.gd656killicon.client.render.impl.Battlefield1Renderer());
            org.mods.gd656killicon.client.render.HudElementManager.register("subtitle", "kill_feed", new org.mods.gd656killicon.client.render.impl.SubtitleRenderer());
            org.mods.gd656killicon.client.render.HudElementManager.register("subtitle", "score", org.mods.gd656killicon.client.render.impl.ScoreSubtitleRenderer.getInstance());
            org.mods.gd656killicon.client.render.HudElementManager.register("subtitle", "combo", org.mods.gd656killicon.client.render.impl.ComboSubtitleRenderer.getInstance());
            org.mods.gd656killicon.client.render.HudElementManager.register("subtitle", "bonus_list", org.mods.gd656killicon.client.render.impl.BonusListRenderer.getInstance());
            org.mods.gd656killicon.client.render.HudElementManager.register("global", "ace_logo", new org.mods.gd656killicon.client.render.impl.AceLogoRenderer());
            
            registerConfigScreen();
        });
        
        org.mods.gd656killicon.client.command.ClientCommand.init();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_CONFIG);
        event.register(KeyBindings.OPEN_SCOREBOARD);
    }

    public static void registerConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class, 
            () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new org.mods.gd656killicon.client.gui.MainConfigScreen(screen)));
    }
}
