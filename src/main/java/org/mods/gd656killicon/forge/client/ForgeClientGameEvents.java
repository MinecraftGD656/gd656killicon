package org.mods.gd656killicon.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.ClientEvents;
import org.mods.gd656killicon.client.KeyBindings;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID, value = Dist.CLIENT)
public final class ForgeClientGameEvents {
    private ForgeClientGameEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientEvents.onClientTickEnd();
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientEvents.onClientPlayerLogout();
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        ClientEvents.onLivingDeath(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (ClientEvents.shouldMutePlaySound(event.getSound())) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        KeyBindings.onKeyInput(event.getKey(), event.getAction());
    }
}
