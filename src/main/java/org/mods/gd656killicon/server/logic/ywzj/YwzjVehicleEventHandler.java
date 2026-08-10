package org.mods.gd656killicon.server.logic.ywzj;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.logic.core.VehicleRewardHelper;
import org.mods.gd656killicon.server.logic.ywzj.IYwzjVehicleHandler;
import org.mods.gd656killicon.server.util.ServerLog;
import org.ywzj.vehicle.api.event.HitVehicleEvent;
import org.ywzj.vehicle.api.event.VehicleAttackEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.*;

public class YwzjVehicleEventHandler implements IYwzjVehicleHandler {
    private final Map<AbstractVehicle, VehicleCombatTracker> combatTrackerMap = new WeakHashMap<>();
    private final Map<ServerPlayer, Long> lastRepairBonusTimeMap = new WeakHashMap<>();

    @Override
    public void init() {
        ServerBridge.loader().registerForgeEventBusSubscriber(this);
        ServerLog.info("YWZJ Vehicle event handler registered.");
    }

    @SubscribeEvent
    public void onHitVehicle(HitVehicleEvent event) {
        ServerPlayer player = ServerCore.getServer().getPlayerList().getPlayer(event.shooterUuid);
        if (player == null) return;

        net.minecraft.world.entity.Entity entity = player.level().getEntity(event.entityId);
        if (!(entity instanceof AbstractVehicle vehicle)) return;

        if (vehicle.isDestroyed()) return;

        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
        updateLastDriver(vehicle, tracker);
        tracker.recordDamage(player.getUUID(), event.damage, true);

        if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
            ServerCore.BONUS.add(player, BonusType.HIT_VEHICLE_ARMOR, event.damage, null);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        
        ServerPlayer player = (ServerPlayer) event.player;
        
        if (player.isUsingItem()) {
            net.minecraft.world.item.ItemStack stack = player.getUseItem();
            if (stack.getItem() == org.ywzj.vehicle.all.AllItems.REPAIR_TOOL.get()) {
                long now = System.currentTimeMillis();
                Long lastTime = lastRepairBonusTimeMap.getOrDefault(player, 0L);
                
                if (now - lastTime >= 2000) {
                    net.minecraft.world.phys.Vec3 viewVector = player.getViewVector(1.0F);
                    net.minecraft.world.phys.Vec3 startPos = player.getEyePosition();
                    net.minecraft.world.phys.Vec3 endPos = startPos.add(viewVector.scale(3.0));                     
                    net.minecraft.world.phys.EntityHitResult hitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                            player, startPos, endPos,
                            player.getBoundingBox().expandTowards(viewVector.scale(3.0)).inflate(1.0),
                            e -> e instanceof AbstractVehicle, 9.0 
                    );
                    
                    if (hitResult != null && hitResult.getEntity() instanceof AbstractVehicle vehicle) {
                        if (!vehicle.isDestroyed() && vehicle.getHealth() < vehicle.getMaxHealth()) {
                            if (ServerData.get().isBonusEnabled(BonusType.VEHICLE_REPAIR)) {
                                ServerCore.BONUS.add(player, BonusType.VEHICLE_REPAIR, 1.0f, null);
                            }
                            
                            lastRepairBonusTimeMap.put(player, now);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onVehicleAttack(VehicleAttackEvent event) {
        if (event.getVehicle().isDestroyed()) return;

        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(event.getVehicle(), v -> new VehicleCombatTracker());
        updateLastDriver(event.getVehicle(), tracker);
        
        net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
        boolean isPlayer = attacker instanceof ServerPlayer;
        UUID attackerUuid = isPlayer ? attacker.getUUID() : null;

        tracker.recordDamage(attackerUuid, 0.0f, isPlayer);

        if (event.getVehicle().getControllingPassenger() instanceof ServerPlayer player) {
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
            updateLastDriver(vehicle, tracker);

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
        
        if (attacker instanceof AbstractVehicle vehicle) {
            recordVehicleDamage(vehicle, event.getAmount());
        } else if (source.getDirectEntity() instanceof AbstractVehicle vehicle) {
            recordVehicleDamage(vehicle, event.getAmount());
        } else if (attacker instanceof ServerPlayer player && player.getVehicle() instanceof AbstractVehicle vehicle) {
            recordVehicleDamage(vehicle, event.getAmount());
        }
    }

    private void recordVehicleDamage(AbstractVehicle vehicle, float amount) {
        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, v -> new VehicleCombatTracker());
        tracker.accumulatedDamageDealt += amount;
        if (vehicle.getControllingPassenger() instanceof ServerPlayer driver) {
            tracker.lastDriverUuid = driver.getUUID();
        }
    }

    private void updateLastDriver(AbstractVehicle vehicle, VehicleCombatTracker tracker) {
        if (vehicle != null && tracker != null && vehicle.getControllingPassenger() instanceof ServerPlayer driver) {
            tracker.lastDriverUuid = driver.getUUID();
        }
    }

    private void handleVehicleDestruction(AbstractVehicle vehicle, VehicleCombatTracker tracker) {
        float maxHealth = vehicle.getMaxHealth();
        String vehicleNameKey = vehicle.getType().getDescriptionId();
        String extraInfo = vehicleNameKey + "|" + (int) maxHealth;
        ServerPlayer killer = VehicleRewardHelper.resolveRecentKiller(
                tracker.lastAttackerUuid,
                tracker.lastAttackerWasPlayer,
                tracker.lastAttackTime,
                tracker.lastDriverUuid
        );

        if (killer != null) {
            if (!VehicleRewardHelper.shouldAwardDestroyVehicleRewards(killer, tracker.lastDriverUuid)) {
                return;
            }
            double multiplier = ServerData.get().getBonusMultiplier(BonusType.DESTROY_VEHICLE);
            int score = (int) Math.ceil(maxHealth * multiplier);
            if (score > 0) {
                ServerCore.BONUS.add(killer, BonusType.DESTROY_VEHICLE, score, null, vehicle.getId(), vehicleNameKey);
            }
            
            if (tracker.accumulatedDamageDealt > 0) {
                if (ServerData.get().isBonusEnabled(BonusType.VALUE_TARGET_DESTROYED)) {
                    ServerCore.BONUS.add(killer, BonusType.VALUE_TARGET_DESTROYED, tracker.accumulatedDamageDealt, null);
                }
            }

            VehicleRewardHelper.awardDestroyAssistBonuses(
                    killer.getUUID(),
                    tracker.lastDriverUuid,
                    vehicle.getId(),
                    vehicleNameKey,
                    0.0f,
                    tracker.damageByAttacker,
                    tracker.lastContributionTimeByAttacker
            );
            
            ServerData.get().addKill(killer, 1);
            VehicleRewardHelper.sendDestroyVehicleEffects(killer, vehicle.getId(), extraInfo, org.mods.gd656killicon.server.logic.core.BonusEngine.resolveScore(org.mods.gd656killicon.common.BonusType.DESTROY_VEHICLE, score));
        }
    }

    private static class VehicleCombatTracker {
        UUID lastAttackerUuid;
        UUID lastDriverUuid;
        long lastAttackTime;
        boolean lastAttackerWasPlayer = false;
        float accumulatedDamageDealt = 0;
        Map<UUID, Float> damageByAttacker = new HashMap<>();
        Map<UUID, Long> lastContributionTimeByAttacker = new HashMap<>();

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
