package org.mods.gd656killicon.server.logic.immersiveaircraft;

import immersive_aircraft.config.Config;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.KillIconPacket;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.PlayerDataManager;
import org.mods.gd656killicon.server.data.ServerData;
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
        MinecraftForge.EVENT_BUS.register(this);
        ServerLog.info("ImmersiveAircraft event handler registered.");
        
        // Register TACZ listener if mod is present
        if (ModList.get().isLoaded("tacz")) {
            try {
                // Use reflection to register listener to avoid ClassNotFoundException
                Class<?> listenerClass = Class.forName("org.mods.gd656killicon.server.logic.immersiveaircraft.ImmersiveAircraftEventHandler$TaczListener");
                Object listener = listenerClass.getDeclaredConstructor(ImmersiveAircraftEventHandler.class).newInstance(this);
                MinecraftForge.EVENT_BUS.register(listener);
                ServerLog.info("ImmersiveAircraft TACZ listener registered.");
            } catch (Exception e) {
                ServerLog.error("Failed to register TACZ listener: " + e.getMessage());
            }
        }
    }
    
    // Internal listener for TACZ events to avoid class loading issues if TACZ is missing
    public class TaczListener {
        @SubscribeEvent
        public void onEntityHurtByGun(com.tacz.guns.api.event.common.EntityHurtByGunEvent event) {
            Entity target = event.getHurtEntity();
            if (target == null || target.level().isClientSide) return;
            if (!(target instanceof VehicleEntity vehicle)) return;
            
            Entity attacker = event.getAttacker();
            if (!(attacker instanceof ServerPlayer player)) return;
            
            float amount = event.getAmount();
            
            // Record damage
            VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
            tracker.recordDamage(player.getUUID(), amount, true);
            
            // Award HIT_VEHICLE_ARMOR bonus if applicable
            if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
                if (amount > 0) {
                    ServerCore.BONUS.add(player, BonusType.HIT_VEHICLE_ARMOR, amount, null);
                }
            }
        }
    }

    @Override
    public void tick() {
        // Use iterator for safe removal
        var iterator = combatTrackerMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<VehicleEntity, VehicleCombatTracker> entry = iterator.next();
            VehicleEntity vehicle = entry.getKey();
            VehicleCombatTracker tracker = entry.getValue();
            
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

        // Check if vehicle was destroyed (health <= 0)
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
            // Check for Creative Instabuild Kill
            if (player.getAbilities().instabuild) {
                // Creative kill immediately destroys the vehicle
                // We trigger destruction logic manually here
                VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
                tracker.recordDamage(player.getUUID(), vehicle.getHealth(), true);
                
                processVehicleDestruction(vehicle, tracker);
                combatTrackerMap.remove(vehicle);
                return;
            }

            // Estimate damage for tracking
            VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
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
        
        long now = System.currentTimeMillis();
        
        ServerPlayer killer = null;
        if (tracker.lastAttackerWasPlayer && tracker.lastAttackerUuid != null) {
            if (now - tracker.lastAttackTime < ServerData.get().getAssistTimeoutMs()) {
                killer = ServerCore.getServer().getPlayerList().getPlayer(tracker.lastAttackerUuid);
            }
        }

        if (killer != null) {
            double multiplier = ServerData.get().getBonusMultiplier(BonusType.DESTROY_VEHICLE);
            int score = (int) (DEFAULT_SCORE_BASE * multiplier);
            
            if (score > 0) {
                ServerCore.BONUS.add(killer, BonusType.DESTROY_VEHICLE, score, null);
            }
            
            PlayerDataManager.get().addKill(killer.getUUID(), 1);
            
            String entityId = vehicle.getType().getDescriptionId();
            String extraInfo = entityId + "|" + DEFAULT_SCORE_BASE;

            sendKillEffects(killer, KillType.DESTROY_VEHICLE, 0, vehicle.getId(), extraInfo);
        }
    }

    private void sendKillEffects(ServerPlayer player, int killType, int combo, int victimId, String victimName) {
        double window = ServerData.get().getComboWindowSeconds();
        boolean hasHelmet = false;
        
        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "scrolling", killType, combo, victimId, window, hasHelmet, victimName), player);
        if (combo > 0) {
            NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "combo", killType, combo, victimId, window, hasHelmet, victimName), player);
        }
        
        if (killType != KillType.DESTROY_VEHICLE) {
            NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "card", killType, combo, victimId, window, hasHelmet, victimName), player);
            NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "card_bar", killType, combo, victimId, window, hasHelmet, victimName), player);
        }
        
        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "battlefield1", killType, combo, victimId, window, hasHelmet, victimName), player);
        
        NetworkHandler.sendToPlayer(new KillIconPacket("subtitle", "kill_feed", killType, combo, victimId, window, hasHelmet, victimName), player);
        NetworkHandler.sendToPlayer(new KillIconPacket("subtitle", "combo", killType, combo, victimId, window, hasHelmet, victimName), player);
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

    private static class VehicleCombatTracker {
        UUID lastAttackerUuid;
        long lastAttackTime;
        boolean lastAttackerWasPlayer = false;

        void recordDamage(UUID attackerUuid, float amount, boolean isPlayer) {
            if (attackerUuid != null) {
                lastAttackerUuid = attackerUuid;
            }
            lastAttackTime = System.currentTimeMillis();
            lastAttackerWasPlayer = isPlayer;
        }
    }
}
