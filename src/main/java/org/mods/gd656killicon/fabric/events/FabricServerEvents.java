package org.mods.gd656killicon.fabric.events;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.fabric.server.FabricServerLoaderBridge;
import org.mods.gd656killicon.server.event.ServerCombatEngine;
import org.mods.gd656killicon.server.ServerCore;

public final class FabricServerEvents {
    private static boolean initialized = false;

    private FabricServerEvents() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        LifecycleEvent.SERVER_STARTING.register(FabricServerEvents::onStarting);
        LifecycleEvent.SERVER_STOPPING.register(FabricServerEvents::onStopping);
        TickEvent.SERVER_POST.register(FabricServerEvents::onTick);
        PlayerEvent.PLAYER_JOIN.register(FabricServerEvents::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(FabricServerEvents::onPlayerLogout);
        TickEvent.PLAYER_POST.register(FabricServerEvents::onPlayerTick);
        InteractionEvent.INTERACT_ENTITY.register(FabricServerEvents::onEntityInteract);
        BlockEvent.BREAK.register(FabricServerEvents::onBlockBreak);
        EntityEvent.LIVING_HURT.register(FabricServerEvents::onDamage);
        EntityEvent.LIVING_DEATH.register(FabricServerEvents::onDeath);
        ServerEntityEvents.EQUIPMENT_CHANGE.register(FabricServerEvents::onEquipmentChange);
    }

    private static void onStarting(MinecraftServer server) {
        FabricServerLoaderBridge.setCurrentServer(server);
        ServerCombatEngine.onStarting(server);
    }

    private static void onStopping(MinecraftServer server) {
        ServerCombatEngine.onStopping(server);
        ServerCore.HONOR.onServerStop();
        FabricServerLoaderBridge.setCurrentServer(null);
    }

    private static void onTick(MinecraftServer server) {
        ServerCombatEngine.onTick();
    }

    private static void onPlayerJoin(ServerPlayer player) {
        ServerCombatEngine.onPlayerJoin(player);
        ServerCore.HONOR.onPlayerJoin(player);
    }

    private static void onPlayerLogout(ServerPlayer player) {
        ServerCombatEngine.onPlayerLogout(player);
        ServerCore.HONOR.onPlayerLogout(player);
    }

    private static void onPlayerTick(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerCombatEngine.onPlayerTick(serverPlayer);
        }
    }

    private static EventResult onEntityInteract(net.minecraft.world.entity.player.Player player,
                                                net.minecraft.world.entity.Entity target,
                                                net.minecraft.world.InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && target instanceof net.minecraft.world.entity.LivingEntity victim) {
            ServerCombatEngine.onEntityInteract(serverPlayer, victim, player.getMainHandItem(), player.level().isClientSide);
        }
        return EventResult.pass();
    }

    private static EventResult onBlockBreak(net.minecraft.world.level.Level level,
                                            net.minecraft.core.BlockPos pos,
                                            net.minecraft.world.level.block.state.BlockState state,
                                            ServerPlayer player,
                                            dev.architectury.utils.value.IntValue experience) {
        if (player != null) {
            ServerCombatEngine.onBlockBreak(player);
        }
        return EventResult.pass();
    }

    private static EventResult onDamage(net.minecraft.world.entity.LivingEntity victim,
                                        net.minecraft.world.damagesource.DamageSource source,
                                        float amount) {
        ServerCombatEngine.onDamage(victim, source, amount);
        return EventResult.pass();
    }

    private static void onEquipmentChange(net.minecraft.world.entity.LivingEntity entity,
                                          net.minecraft.world.entity.EquipmentSlot slot,
                                          net.minecraft.world.item.ItemStack from,
                                          net.minecraft.world.item.ItemStack to) {
        if (entity instanceof ServerPlayer player) {
            ServerCombatEngine.onItemSwitch(player, slot, from, to);
        }
    }

    private static EventResult onDeath(net.minecraft.world.entity.LivingEntity victim,
                                       net.minecraft.world.damagesource.DamageSource source) {
        ServerCombatEngine.onDeath(victim, source);
        if (victim instanceof ServerPlayer player) {
            ServerCore.HONOR.onPlayerDeath(player);
        }
        return EventResult.pass();
    }
}
