package org.mods.gd656killicon.server.event;

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

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID)
public class ServerEventHandler {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        ServerCombatEngine.onBlockBreak(event);
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        ServerCombatEngine.onStarting(event);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        ServerCombatEngine.onStopping(event);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        ServerCombatEngine.onTick(event);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerCombatEngine.onPlayerJoin(event);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerCombatEngine.onPlayerLogout(event);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        ServerCombatEngine.onPlayerTick(event);
    }

    @SubscribeEvent
    public static void onItemSwitch(LivingEquipmentChangeEvent event) {
        ServerCombatEngine.onItemSwitch(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ServerCombatEngine.onEntityInteract(event);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        ServerCombatEngine.onDamage(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        ServerCombatEngine.onDeath(event);
    }
}
