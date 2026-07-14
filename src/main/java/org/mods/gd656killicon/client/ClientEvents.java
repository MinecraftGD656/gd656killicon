package org.mods.gd656killicon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.mods.gd656killicon.client.bridge.ClientBridge;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.gui.MainConfigScreen;
import org.mods.gd656killicon.client.util.AceLagSimulator;
import org.mods.gd656killicon.network.packet.KillIconPacket;

public class ClientEvents {
    private static boolean wasInGame = false;
    private static Class<?> taczGunSoundClass;
    private static boolean taczGunSoundResolved = false;
    private static final ResourceLocation TACZ_KILL_SOUND = ResourceLocation.fromNamespaceAndPath("tacz", "kill");

    public static void onClientTickEnd() {
        ElementConfigManager.tryApplyLocalization();
        while (KeyBindings.OPEN_CONFIG.consumeClick()) {
            Minecraft.getInstance().setScreen(new MainConfigScreen(Minecraft.getInstance().screen));
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            if (!wasInGame) {
                if (org.mods.gd656killicon.client.config.ClientConfigManager.isEnableAceLag()) {
                    org.mods.gd656killicon.client.render.HudElementManager.trigger("global", "ace_logo", org.mods.gd656killicon.client.render.IHudRenderer.TriggerContext.of(0, -1));
                }
                wasInGame = true;
            }
        } else {
            wasInGame = false;
        }
        KillIconPacket.processPendingTriggers();
        AceLagSimulator.onClientTick();
    }


    public static void onClientPlayerLogout() {
        org.mods.gd656killicon.client.render.impl.ComboSubtitleRenderer.getInstance().onPlayerLogout();
    }

    public static void onLivingDeath(Entity deadEntity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && deadEntity == mc.player) {
            org.mods.gd656killicon.client.render.impl.ComboSubtitleRenderer.getInstance().onPlayerDeath();
        }
    }

    public static boolean shouldMutePlaySound(Object sound) {
        if (!ClientConfigManager.isDisableTaczKillSound()) return false;
        if (!ClientBridge.loader().isModLoaded("tacz")) return false;
        if (!taczGunSoundResolved) {
            taczGunSoundResolved = true;
            try {
                taczGunSoundClass = Class.forName("com.tacz.guns.client.sound.GunSoundInstance");
            } catch (Exception ignored) {}
        }
        if (taczGunSoundClass == null) return false;
        if (sound == null || !taczGunSoundClass.isInstance(sound)) return false;
        try {
            var method = taczGunSoundClass.getMethod("getRegistryName");
            Object result = method.invoke(sound);
            if (result instanceof ResourceLocation location && TACZ_KILL_SOUND.equals(location)) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
