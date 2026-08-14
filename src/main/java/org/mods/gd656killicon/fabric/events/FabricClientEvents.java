package org.mods.gd656killicon.fabric.events;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.client.gui.GuiGraphics;
import org.mods.gd656killicon.client.ClientEvents;
import org.mods.gd656killicon.client.KeyBindings;
import org.mods.gd656killicon.client.render.HudElementManager;

public final class FabricClientEvents {
    private static boolean initialized = false;

    private FabricClientEvents() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientTickEvent.CLIENT_POST.register(FabricClientEvents::onClientTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(FabricClientEvents::onClientPlayerLogout);
        EntityEvent.LIVING_DEATH.register(FabricClientEvents::onLivingDeath);
        ClientGuiEvent.RENDER_HUD.register(FabricClientEvents::onRenderHud);
    }

    private static void onClientTick(net.minecraft.client.Minecraft client) {
        KeyBindings.onClientTick();
        ClientEvents.onClientTickEnd();
    }

    private static void onClientPlayerLogout(net.minecraft.client.player.LocalPlayer player) {
        ClientEvents.onClientPlayerLogout();
    }

    private static EventResult onLivingDeath(net.minecraft.world.entity.LivingEntity victim,
                                             net.minecraft.world.damagesource.DamageSource source) {
        if (victim.level().isClientSide) {
            ClientEvents.onLivingDeath(victim);
        }
        return EventResult.pass();
    }

    private static void onRenderHud(GuiGraphics guiGraphics, float partialTick) {
        HudElementManager.onRenderGuiOverlay(guiGraphics, partialTick, true);
    }
}
