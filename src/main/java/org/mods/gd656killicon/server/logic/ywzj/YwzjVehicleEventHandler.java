package org.mods.gd656killicon.server.logic.ywzj;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.KillIconPacket;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.util.ServerLog;
import org.ywzj.vehicle.api.event.HitVehicleEvent;
import org.ywzj.vehicle.api.event.VehicleAttackEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.*;

public class YwzjVehicleEventHandler implements IYwzjVehicleHandler {
    // Stores combat history for each vehicle to determine killer and assists
    private final Map<AbstractVehicle, VehicleCombatTracker> combatTrackerMap = new WeakHashMap<>();
    private final Map<ServerPlayer, Long> lastRepairBonusTimeMap = new WeakHashMap<>();

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        ServerLog.info("YWZJ Vehicle event handler registered.");
    }

    @SubscribeEvent
    public void onHitVehicle(HitVehicleEvent event) {
        ServerPlayer player = ServerCore.getServer().getPlayerList().getPlayer(event.shooterUuid);
        if (player == null) return;

        net.minecraft.world.entity.Entity entity = player.level().getEntity(event.entityId);
        if (!(entity instanceof AbstractVehicle vehicle)) return;

        // Prevent interaction with destroyed vehicles
        if (vehicle.isDestroyed()) return;

        if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
            ServerCore.BONUS.add(player, BonusType.HIT_VEHICLE_ARMOR, event.damage, null);
        }

        // Update combat tracker - last attacker is handled in onVehicleAttack
        combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        
        ServerPlayer player = (ServerPlayer) event.player;
        
        // Check if using repair tool
        if (player.isUsingItem()) {
            net.minecraft.world.item.ItemStack stack = player.getUseItem();
            if (stack.getItem() == org.ywzj.vehicle.all.AllItems.REPAIR_TOOL.get()) {
                long now = System.currentTimeMillis();
                Long lastTime = lastRepairBonusTimeMap.getOrDefault(player, 0L);
                
                // 2 seconds interval (2000ms)
                if (now - lastTime >= 2000) {
                    // Raytrace to find vehicle
                    net.minecraft.world.phys.Vec3 viewVector = player.getViewVector(1.0F);
                    net.minecraft.world.phys.Vec3 startPos = player.getEyePosition();
                    net.minecraft.world.phys.Vec3 endPos = startPos.add(viewVector.scale(3.0)); // 3 blocks distance
                    
                    net.minecraft.world.phys.EntityHitResult hitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                            player, startPos, endPos,
                            player.getBoundingBox().expandTowards(viewVector.scale(3.0)).inflate(1.0),
                            e -> e instanceof AbstractVehicle, 9.0 
                    );
                    
                    if (hitResult != null && hitResult.getEntity() instanceof AbstractVehicle vehicle) {
                        // Exclude destroyed and full health vehicles
                        if (!vehicle.isDestroyed() && vehicle.getHealth() < vehicle.getMaxHealth()) {
                            // Award bonus
                            if (ServerData.get().isBonusEnabled(BonusType.VEHICLE_REPAIR)) {
                                ServerCore.BONUS.add(player, BonusType.VEHICLE_REPAIR, 1.0f, null);
                            }
                            
                            // Update time
                            lastRepairBonusTimeMap.put(player, now);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onVehicleAttack(VehicleAttackEvent event) {
        // Prevent interaction with destroyed vehicles
        if (event.getVehicle().isDestroyed()) return;

        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(event.getVehicle(), v -> new VehicleCombatTracker());
        
        net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
        boolean isPlayer = attacker instanceof ServerPlayer;
        UUID attackerUuid = isPlayer ? attacker.getUUID() : null;

        // Update last attacker info for ALL attacks (including projectiles and environmental)
        tracker.lastAttackTime = System.currentTimeMillis();
        tracker.lastAttackerWasPlayer = isPlayer;
        if (attackerUuid != null) {
            tracker.lastAttackerUuid = attackerUuid;
        }

        // If a player is driving the vehicle and attacking something, update the tracker
        if (event.getVehicle().getControllingPassenger() instanceof ServerPlayer player) {
            // No action needed here for damage tracking, handled by LivingDamageEvent
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<AbstractVehicle, VehicleCombatTracker>> iterator = combatTrackerMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AbstractVehicle, VehicleCombatTracker> entry = iterator.next();
            AbstractVehicle vehicle = entry.getKey();
            VehicleCombatTracker tracker = entry.getValue();

            if (vehicle.isDestroyed()) {
                handleVehicleDestruction(vehicle, tracker);
                iterator.remove();
            } else if (vehicle.isRemoved()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public void onVehicleDealDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        
        // Check if the attacker is a vehicle
        if (attacker instanceof AbstractVehicle vehicle) {
            recordVehicleDamage(vehicle, event.getAmount());
        } else if (source.getDirectEntity() instanceof AbstractVehicle vehicle) {
            recordVehicleDamage(vehicle, event.getAmount());
        } else if (attacker instanceof ServerPlayer player && player.getVehicle() instanceof AbstractVehicle vehicle) {
            // Player in a vehicle dealt damage
            recordVehicleDamage(vehicle, event.getAmount());
        }
    }

    private void recordVehicleDamage(AbstractVehicle vehicle, float amount) {
        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
        tracker.accumulatedDamageDealt += amount;
        // ServerLog.info("Vehicle %s dealt %f damage. Total: %f", vehicle.getId(), amount, tracker.accumulatedDamageDealt);
    }

    private void handleVehicleDestruction(AbstractVehicle vehicle, VehicleCombatTracker tracker) {
        long now = System.currentTimeMillis();
        
        // 1. Determine Killer
        ServerPlayer killer = null;
        if (tracker.lastAttackerWasPlayer && tracker.lastAttackerUuid != null) {
            // Only count as kill if the last hit was recent (prevent "stealing" from environment 10 mins later)
            if (now - tracker.lastAttackTime < ServerData.get().getAssistTimeoutMs()) {
                killer = ServerCore.getServer().getPlayerList().getPlayer(tracker.lastAttackerUuid);
            }
        }

        float maxHealth = vehicle.getMaxHealth();
        // Use descriptionId for client-side localization
        String extraInfo = vehicle.getType().getDescriptionId() + "|" + (int)maxHealth;

        if (killer != null) {
            // Award Destroy Bonus to Killer
            double multiplier = ServerData.get().getBonusMultiplier(BonusType.DESTROY_VEHICLE);
            int score = (int) Math.ceil(maxHealth * multiplier);
            if (score > 0) {
                ServerCore.BONUS.add(killer, BonusType.DESTROY_VEHICLE, score, null);
            }
            
            // Award Value Target Destroyed bonus
            if (tracker.accumulatedDamageDealt > 0) {
                // ServerLog.info("Awarding Value Target Destroyed: %f to %s", tracker.accumulatedDamageDealt, killer.getName().getString());
                if (ServerData.get().isBonusEnabled(BonusType.VALUE_TARGET_DESTROYED)) {
                    ServerCore.BONUS.add(killer, BonusType.VALUE_TARGET_DESTROYED, tracker.accumulatedDamageDealt, null);
                }
            } else {
                // ServerLog.info("No accumulated damage for vehicle %s, skipping Value Target Destroyed bonus.", vehicle.getId());
            }
            
            // Increment persistent kill count
            org.mods.gd656killicon.server.data.PlayerDataManager.get().addKill(killer.getUUID(), 1);

            // Trigger Kill Feed for Killer
            sendKillEffects(killer, KillType.DESTROY_VEHICLE, 0, vehicle.getId(), extraInfo);
        }
    }

    private void sendKillEffects(ServerPlayer player, int killType, int combo, int victimId, String victimName) {
        double window = ServerData.get().getComboWindowSeconds();
        boolean hasHelmet = false;
        
        // Icons
        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "scrolling", killType, combo, victimId, window, hasHelmet, victimName), player);
        if (combo > 0) {
            NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "combo", killType, combo, victimId, window, hasHelmet, victimName), player);
        }
        
        if (killType != KillType.DESTROY_VEHICLE) {
            NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "card", killType, combo, victimId, window, hasHelmet, victimName), player);
            NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "card_bar", killType, combo, victimId, window, hasHelmet, victimName), player);
        }
        
        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "battlefield1", killType, combo, victimId, window, hasHelmet, victimName), player);
        
        // Subtitle
        NetworkHandler.sendToPlayer(new KillIconPacket("subtitle", "kill_feed", killType, combo, victimId, window, hasHelmet, victimName), player);
    }

    private static class VehicleCombatTracker {
        UUID lastAttackerUuid;
        long lastAttackTime;
        boolean lastAttackerWasPlayer = false;
        float accumulatedDamageDealt = 0;
    }

}
