package org.mods.gd656killicon.server.logic.immersiveaircraft;

import immersive_aircraft.config.Config;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.data.PlayerDataManager;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.logic.core.VehicleRewardHelper;
import org.mods.gd656killicon.server.logic.immersiveaircraft.IImmersiveAircraftHandler;
import org.mods.gd656killicon.server.util.ServerLog;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class ImmersiveAircraftEventHandler implements IImmersiveAircraftHandler {
    private static final int DEFAULT_SCORE_BASE = 100;
    private static final float DEFAULT_TRACKING_DAMAGE = 0.1f;
    private final Map<VehicleEntity, VehicleCombatTracker> combatTrackerMap = new WeakHashMap<>();

    @Override
    public void init() {
        ServerBridge.loader().registerForgeEventBusSubscriber(this);
        ServerLog.info("ImmersiveAircraft event handler registered.");
        
        if (ServerBridge.loader().isModLoaded("tacz")) {
            try {
                Class<?> listenerClass = Class.forName("org.mods.gd656killicon.server.logic.immersiveaircraft.ImmersiveAircraftEventHandler$TaczListener");
                Object listener = listenerClass.getDeclaredConstructor(ImmersiveAircraftEventHandler.class).newInstance(this);
                ServerBridge.loader().registerForgeEventBusSubscriber(listener);
                ServerLog.info("ImmersiveAircraft TACZ listener registered.");
            } catch (Exception e) {
                ServerLog.error("Failed to register TACZ listener: " + e.getMessage());
            }
        }
    }
    
    public class TaczListener {
        @SubscribeEvent
        public void onEntityHurtByGun(com.tacz.guns.api.event.common.EntityHurtByGunEvent event) {
            Entity target = event.getHurtEntity();
            if (target == null || target.level().isClientSide) return;
            if (!(target instanceof VehicleEntity vehicle)) return;
            
            Entity attacker = event.getAttacker();
            if (!(attacker instanceof ServerPlayer player)) return;
            
            float amount = event.getAmount();
            
            VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
            updateLastDriver(vehicle, tracker);
            tracker.recordDamage(player.getUUID(), amount, true);
            
            if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
                if (amount > 0) {
                    ServerCore.BONUS.add(player, BonusType.HIT_VEHICLE_ARMOR, amount, null);
                }
            }
        }
    }

    @Override
    public void tick() {
        var iterator = combatTrackerMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<VehicleEntity, VehicleCombatTracker> entry = iterator.next();
            VehicleEntity vehicle = entry.getKey();
            VehicleCombatTracker tracker = entry.getValue();
            updateLastDriver(vehicle, tracker);
            
            if (vehicle.isRemoved() || vehicle.getHealth() <= 0) {
                processVehicleDestruction(vehicle, tracker);
                iterator.remove();
            }
        }
    }
    
    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof VehicleEntity vehicle)) return;

        if (vehicle.getHealth() <= 0) {
            VehicleCombatTracker tracker = combatTrackerMap.get(vehicle);
            if (tracker != null) {
                processVehicleDestruction(vehicle, tracker);
                combatTrackerMap.remove(vehicle);
            }
        }
    }
    
    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getTarget() instanceof VehicleEntity vehicle)) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getAbilities().instabuild) {
                VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
                updateLastDriver(vehicle, tracker);
                tracker.recordDamage(player.getUUID(), vehicle.getHealth(), true);
                
                processVehicleDestruction(vehicle, tracker);
                combatTrackerMap.remove(vehicle);
                return;
            }

            VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
            updateLastDriver(vehicle, tracker);
            tracker.recordDamage(player.getUUID(), DEFAULT_TRACKING_DAMAGE, true);
            
            if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
                ServerCore.BONUS.add(player, BonusType.HIT_VEHICLE_ARMOR, 1.0f, null);
            }
        }
    }

    @SubscribeEvent
    public void onProjectileHit(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != HitResult.Type.ENTITY) return;
        
        EntityHitResult hitResult = (EntityHitResult) event.getRayTraceResult();
        Entity target = hitResult.getEntity();
        
        if (target == null || target.level().isClientSide) return;
        if (!(target instanceof VehicleEntity vehicle)) return;
        
        Projectile projectile = event.getProjectile();
        Entity owner = projectile.getOwner();
        
        if (owner instanceof ServerPlayer player) {
            VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
            updateLastDriver(vehicle, tracker);
            tracker.recordDamage(player.getUUID(), DEFAULT_TRACKING_DAMAGE, true);
            
            if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
                ServerCore.BONUS.add(player, BonusType.HIT_VEHICLE_ARMOR, 1.0f, null);
            }
        }
    }
    
    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;
        
        Entity attacker = event.getExplosion().getIndirectSourceEntity();
        boolean isPlayer = attacker instanceof ServerPlayer;
        UUID attackerUuid = isPlayer ? attacker.getUUID() : null;
        
        for (Entity entity : event.getAffectedEntities()) {
            if (entity instanceof VehicleEntity vehicle) {
                VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
                updateLastDriver(vehicle, tracker);
                tracker.recordDamage(attackerUuid, 0.5f, isPlayer);
                
                if (isPlayer) {
                     if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
                        ServerCore.BONUS.add((ServerPlayer)attacker, BonusType.HIT_VEHICLE_ARMOR, 5.0f, null);
                    }
                }
            }
        }
    }

    private void processVehicleDestruction(VehicleEntity vehicle, VehicleCombatTracker tracker) {
        if (tracker == null) return;
        updateLastDriver(vehicle, tracker);
        UUID driverUuid = tracker.lastDriverUuid;
        ServerPlayer killer = VehicleRewardHelper.resolveRecentKiller(
                tracker.lastAttackerUuid,
                tracker.lastAttackerWasPlayer,
                tracker.lastAttackTime,
                driverUuid
        );

        if (killer != null) {
            if (!VehicleRewardHelper.shouldAwardDestroyVehicleRewards(killer, driverUuid)) {
                return;
            }
            double multiplier = ServerData.get().getBonusMultiplier(BonusType.DESTROY_VEHICLE);
            int score = (int) (DEFAULT_SCORE_BASE * multiplier);
            
            String vehicleNameKey = vehicle.getType().getDescriptionId();
            if (score > 0) {
                ServerCore.BONUS.add(killer, BonusType.DESTROY_VEHICLE, score, null, vehicle.getId(), vehicleNameKey);
            }

            VehicleRewardHelper.awardDestroyAssistBonuses(
                    killer.getUUID(),
                    driverUuid,
                    vehicle.getId(),
                    vehicleNameKey,
                    0.0f,
                    tracker.damageByAttacker,
                    tracker.lastContributionTimeByAttacker
            );
            
            ServerData.get().addKill(killer, 1);
            
            String extraInfo = vehicleNameKey + "|" + DEFAULT_SCORE_BASE;
            VehicleRewardHelper.sendDestroyVehicleEffects(killer, vehicle.getId(), extraInfo, (float) org.mods.gd656killicon.server.data.ServerData.get().getBonusMultiplier(org.mods.gd656killicon.common.BonusType.DESTROY_VEHICLE), score);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        
        Entity target = event.getTarget();
        if (!(target instanceof VehicleEntity vehicle)) return;
        
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!ServerData.get().isBonusEnabled(BonusType.VEHICLE_REPAIR)) return;

        if (vehicle.getHealth() >= 1.0f) return;
        
        boolean requireShift = Config.getInstance().requireShiftForRepair;
        if (requireShift && !player.isShiftKeyDown()) return;
        
        if (vehicle.hasPassenger(player)) return;
        
        if (!vehicle.isValidDimension()) return;

        if (player.isSecondaryUseActive()) return;

        float repairAmount = Config.getInstance().repairSpeed;
        float score = repairAmount * 10.0f;
        
        if (score > 0) {
            ServerCore.BONUS.add(player, BonusType.VEHICLE_REPAIR, score, null);
        }
    }

    private void updateLastDriver(VehicleEntity vehicle, VehicleCombatTracker tracker) {
        if (vehicle != null && tracker != null && vehicle.getControllingPassenger() instanceof ServerPlayer driver) {
            tracker.lastDriverUuid = driver.getUUID();
        }
    }

    private static class VehicleCombatTracker {
        UUID lastAttackerUuid;
        UUID lastDriverUuid;
        long lastAttackTime;
        boolean lastAttackerWasPlayer = false;
        Map<UUID, Float> damageByAttacker = new java.util.HashMap<>();
        Map<UUID, Long> lastContributionTimeByAttacker = new java.util.HashMap<>();

        void recordDamage(UUID attackerUuid, float amount, boolean isPlayer) {
            if (attackerUuid != null) {
                lastAttackerUuid = attackerUuid;
                if (amount > 0.0f) {
                    damageByAttacker.merge(attackerUuid, amount, Float::sum);
                    lastContributionTimeByAttacker.put(attackerUuid, System.currentTimeMillis());
                }
            }
            lastAttackTime = System.currentTimeMillis();
            lastAttackerWasPlayer = isPlayer;
        }
    }
}
