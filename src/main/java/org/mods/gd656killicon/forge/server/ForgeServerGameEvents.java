package org.mods.gd656killicon.forge.server;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.server.event.ServerCombatEngine;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID)
public final class ForgeServerGameEvents {
    private ForgeServerGameEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
            ServerCombatEngine.onBlockBreak(player);
        }
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        ServerCombatEngine.onStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        ServerCombatEngine.onStopping(event.getServer());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerCombatEngine.onTick();
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            ServerCombatEngine.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            ServerCombatEngine.onPlayerLogout(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof net.minecraft.server.level.ServerPlayer player) {
            ServerCombatEngine.onPlayerTick(player);
        }
    }

    @SubscribeEvent
    public static void onItemSwitch(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            ServerCombatEngine.onItemSwitch(player, event.getSlot(), event.getFrom(), event.getTo());
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && event.getTarget() instanceof net.minecraft.world.entity.LivingEntity victim) {
            ServerCombatEngine.onEntityInteract(player, victim, event.getItemStack(), event.getLevel().isClientSide);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        ServerCombatEngine.onDamage(event.getEntity(), event.getSource(), event.getAmount());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        ServerCombatEngine.onDeath(event.getEntity(), event.getSource());
    }
}
