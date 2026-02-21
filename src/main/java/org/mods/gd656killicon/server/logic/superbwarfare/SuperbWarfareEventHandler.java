package org.mods.gd656killicon.server.logic.superbwarfare;

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent;
import com.atsuishio.superbwarfare.api.event.ShootEvent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.special.RepairToolItem;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.KillIconPacket;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.LAST_DRIVER_UUID;

public class SuperbWarfareEventHandler implements ISuperbWarfareHandler {
    private final Map<VehicleEntity, VehicleCombatTracker> combatTrackerMap = new WeakHashMap<>();
    private final Map<ServerPlayer, Long> lastRepairBonusTimeMap = new WeakHashMap<>();
    
    private final Map<UUID, Long> headshotVictims = new ConcurrentHashMap<>();
    private final Map<UUID, Long> headshotDamageVictims = new ConcurrentHashMap<>();
    private final Set<UUID> gunKillVictims = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Cache for reflection
    private static Field customExplosionDamageField;
    private static Field explosionRadiusField;
    private static boolean reflectionFailed = false;

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        // We also need to register to SuperbWarfare's event bus if it's separate.
        // In NeoForge it's usually the same or accessible via NeoForge.EVENT_BUS.
        // For compatibility in a Forge mod, we might need to use reflection if SuperbWarfare uses a different bus.
        // But assuming it's standard Forge/NeoForge.
        try {
            Class<?> neoForgeClass = Class.forName("net.neoforged.neoforge.common.NeoForge");
            Object eventBus = neoForgeClass.getField("EVENT_BUS").get(null);
            eventBus.getClass().getMethod("register", Object.class).invoke(eventBus, this);
        } catch (Exception e) {
            // Fallback to standard Forge bus if NeoForge bus is not found
            MinecraftForge.EVENT_BUS.register(this);
        }
        
        // Initialize reflection for CustomExplosion and Explosion
        try {
            Class<?> customExplosionClass = Class.forName("com.atsuishio.superbwarfare.tools.CustomExplosion");
            customExplosionDamageField = customExplosionClass.getDeclaredField("damage");
            customExplosionDamageField.setAccessible(true);
            
            // Explosion radius is private in vanilla Explosion class
            explosionRadiusField = net.minecraft.world.level.Explosion.class.getDeclaredField("radius");
            explosionRadiusField.setAccessible(true);
        } catch (Exception e) {
            ServerLog.error("Failed to find explosion fields via reflection: %s", e.getMessage());
            reflectionFailed = true;
        }
        
        ServerLog.info("SuperbWarfare event handler registered.");
    }

    @Override
    public void tick() {
        // Clear old headshot markers (e.g., markers older than 5 seconds)
        long now = System.currentTimeMillis();
        headshotVictims.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
        headshotDamageVictims.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
        gunKillVictims.clear();
    }

    @Override
    public boolean isHeadshotKill(UUID victimId) {
        return headshotVictims.remove(victimId) != null;
    }

    @Override
    public boolean isHeadshotDamage(UUID victimId) {
        return headshotDamageVictims.remove(victimId) != null;
    }

    @Override
    public boolean isGunKill(UUID victimId) {
        return gunKillVictims.remove(victimId);
    }

    @SubscribeEvent
    public void onProjectileHitEntity(ProjectileHitEvent.HitEntity event) {
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living)) return;

        if (event.isHeadshot()) {
            headshotDamageVictims.put(living.getUUID(), System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide) return;

        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        
        // Check if it's gun damage
        if (DamageTypeTool.isGunDamage(source)) {
            UUID victimId = victim.getUUID();
            gunKillVictims.add(victimId);

            // Check for headshot from source
            if (DamageTypeTool.isHeadshotDamage(source)) {
                headshotVictims.put(victimId, System.currentTimeMillis());
            }
        }
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;
        
        Entity attacker = event.getExplosion().getIndirectSourceEntity();
        boolean isPlayer = attacker instanceof ServerPlayer;
        UUID playerUuid = isPlayer ? attacker.getUUID() : null;
        
        // Attempt to get the actual damage and radius
        float baseDamage = 100.0f; // Fallback default
        float finalRadius = 4.0f; // Fallback default
        
        if (!reflectionFailed) {
            try {
                // Get radius from base Explosion class (works for both vanilla and CustomExplosion)
                if (explosionRadiusField != null) {
                    finalRadius = explosionRadiusField.getFloat(event.getExplosion());
                }
                
                // Get damage from CustomExplosion if applicable
                if (customExplosionDamageField != null && event.getExplosion().getClass().getName().equals("com.atsuishio.superbwarfare.tools.CustomExplosion")) {
                    baseDamage = customExplosionDamageField.getFloat(event.getExplosion());
                }
            } catch (Exception e) {
                // Stick to fallbacks
            }
        }

        net.minecraft.world.phys.Vec3 explosionPos = event.getExplosion().getPosition();

        for (Entity entity : event.getAffectedEntities()) {
            if (entity instanceof VehicleEntity vehicle) {
                // Calculate explosion damage using linear falloff formula
                double dist = Math.sqrt(entity.distanceToSqr(explosionPos));
                if (dist > finalRadius) dist = finalRadius;
                
                float damageFactor = (float) (1.0 - (dist / finalRadius));
                if (damageFactor < 0) damageFactor = 0;
                
                float estimatedDamage = baseDamage * damageFactor;
                if (estimatedDamage < 10) estimatedDamage = 10;
                
                VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
                tracker.recordDamage(playerUuid, estimatedDamage, isPlayer);

                if (isPlayer) {
                    if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
                        ServerCore.BONUS.add((ServerPlayer)attacker, BonusType.HIT_VEHICLE_ARMOR, estimatedDamage, null);
                    }
                    // Update vehicle's internal last attacker
                    vehicle.getEntityData().set(VehicleEntity.LAST_ATTACKER_UUID, attacker.getStringUUID());
                }
            }
        }
    }

    @SubscribeEvent
    public void onShoot(ShootEvent event) {
        // Method kept for other potential shoot logic, but last bullet logic removed
    }



    @SubscribeEvent
    public void onRepair(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        // Use Entity to check for VehicleEntity to avoid LivingEntity cast issues
        net.minecraft.world.entity.Entity entity = event.getEntity();
        if (!(entity instanceof VehicleEntity vehicle)) return;
        
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof RepairToolItem) {
                long now = System.currentTimeMillis();
                Long lastBonus = lastRepairBonusTimeMap.get(player);
                if (lastBonus == null || now - lastBonus > 2000) {
                    if (ServerData.get().isBonusEnabled(BonusType.VEHICLE_REPAIR)) {
                        ServerCore.BONUS.add(player, BonusType.VEHICLE_REPAIR, 1.0f, "");
                    }
                    lastRepairBonusTimeMap.put(player, now);
                }
            }
        }
    }

    @SubscribeEvent
    public void onShootRepair(ShootEvent.Post event) {
        Entity shooter = event.getShooter();
        if (!(shooter instanceof ServerPlayer player)) return;

        if (!(event.getData().item() instanceof RepairToolItem)) return;

        // Check if player is looking at a vehicle
        double reachDistance = 5.0;
        net.minecraft.world.phys.Vec3 viewVector = player.getViewVector(1.0F);
        net.minecraft.world.phys.Vec3 startPos = player.getEyePosition();
        net.minecraft.world.phys.Vec3 endPos = startPos.add(viewVector.scale(reachDistance));
        
        net.minecraft.world.phys.EntityHitResult hitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, startPos, endPos,
                player.getBoundingBox().expandTowards(viewVector.scale(reachDistance)).inflate(1.0),
                e -> e instanceof VehicleEntity, reachDistance * reachDistance
        );

        if (hitResult != null && hitResult.getEntity() instanceof VehicleEntity vehicle) {
            // Determine if it's repair or damage using SuperbWarfare's internal logic
            Entity lastDriver = EntityFindUtil.findEntity(player.level(), vehicle.getEntityData().get(LAST_DRIVER_UUID));
            boolean isDamage = (lastDriver != null && !SeekTool.IN_SAME_TEAM.test(shooter, lastDriver) && lastDriver.getTeam() != null) || shooter.isShiftKeyDown();

            if (isDamage) {
                // Award HIT_VEHICLE_ARMOR bonus for repair tool damage
                float damage = 0.5f; // From RepairToolItem.java
                if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
                    ServerCore.BONUS.add(player, BonusType.HIT_VEHICLE_ARMOR, damage, null);
                }
                VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
                tracker.recordDamage(player.getUUID(), damage, true);
                
                // Update vehicle's internal last attacker
                vehicle.getEntityData().set(VehicleEntity.LAST_ATTACKER_UUID, player.getStringUUID());
            } else {
                // Award VEHICLE_REPAIR bonus
                if (vehicle.getHealth() < vehicle.getMaxHealth()) {
                    long now = System.currentTimeMillis();
                    long lastBonusTime = lastRepairBonusTimeMap.getOrDefault(player, 0L);
                    if (now - lastBonusTime > 2000) { // Requirement: every 2 seconds
                        if (ServerData.get().isBonusEnabled(BonusType.VEHICLE_REPAIR)) {
                            ServerCore.BONUS.add(player, BonusType.VEHICLE_REPAIR, 1.0f, null);
                            lastRepairBonusTimeMap.put(player, now);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onProjectileHitVehicle(ProjectileHitEvent.HitEntity event) {
        Entity attacker = event.getOwner();
        if (!(attacker instanceof ServerPlayer player)) return;

        Entity target = event.getTarget();
        if (!(target instanceof VehicleEntity vehicle)) return;

        // ProjectileHitEvent doesn't provide damage amount directly, 
        // but we can estimate or use a default value if needed.
        // However, we can track that this player hit the vehicle.
        float damage = 1.0f; // Default damage for tracking if not specified
        
        if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
            ServerCore.BONUS.add(player, BonusType.HIT_VEHICLE_ARMOR, damage, null);
        }

        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
        tracker.recordDamage(player.getUUID(), damage, true);
        
        // Update vehicle's internal last attacker to ensure synchronization
        vehicle.getEntityData().set(VehicleEntity.LAST_ATTACKER_UUID, player.getStringUUID());
    }

    @SubscribeEvent
    public void onVehicleLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof VehicleEntity vehicle)) return;

        // Check if vehicle was destroyed (health <= 0)
        if (vehicle.getHealth() <= 0) {
            VehicleCombatTracker tracker = combatTrackerMap.get(vehicle);
            if (tracker != null) {
                // Determine the true last attacker from the vehicle's data
                String lastAttackerUuidStr = vehicle.getEntityData().get(VehicleEntity.LAST_ATTACKER_UUID);
                if (lastAttackerUuidStr != null && !lastAttackerUuidStr.isEmpty()) {
                    try {
                        UUID actualLastAttackerUuid = UUID.fromString(lastAttackerUuidStr);
                        tracker.lastAttackerUuid = actualLastAttackerUuid;
                        tracker.lastAttackTime = System.currentTimeMillis();
                        tracker.lastAttackerWasPlayer = true;
                    } catch (IllegalArgumentException ignored) {}
                }
                
                processVehicleDestruction(vehicle, tracker);
                combatTrackerMap.remove(vehicle);
            }
        }
    }

    @SubscribeEvent
    public void onVehicleDealDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        net.minecraft.world.entity.Entity attacker = source.getEntity();

        // Check if the attacker is a vehicle
        if (attacker instanceof VehicleEntity vehicle) {
            recordVehicleDamage(vehicle, event.getAmount());
        } else if (source.getDirectEntity() instanceof VehicleEntity vehicle) {
            recordVehicleDamage(vehicle, event.getAmount());
        } else if (attacker instanceof ServerPlayer player && player.getVehicle() instanceof VehicleEntity vehicle) {
            // Player in a vehicle dealt damage
            recordVehicleDamage(vehicle, event.getAmount());
        }
    }

    private void recordVehicleDamage(VehicleEntity vehicle, float amount) {
        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
        tracker.accumulatedDamageDealt += amount;
    }

    private void processVehicleDestruction(VehicleEntity vehicle, VehicleCombatTracker tracker) {
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
                if (ServerData.get().isBonusEnabled(BonusType.VALUE_TARGET_DESTROYED)) {
                    ServerCore.BONUS.add(killer, BonusType.VALUE_TARGET_DESTROYED, tracker.accumulatedDamageDealt, null);
                }
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

        void recordDamage(UUID attackerUuid, float amount, boolean isPlayer) {
            if (attackerUuid != null) {
                lastAttackerUuid = attackerUuid;
            }
            lastAttackTime = System.currentTimeMillis();
            lastAttackerWasPlayer = isPlayer;
        }
    }
}
