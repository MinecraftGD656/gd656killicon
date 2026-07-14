package org.mods.gd656killicon.server.logic.superbwarfare;

import com.google.gson.JsonObject;
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent;
import com.atsuishio.superbwarfare.api.event.ShootEvent;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.gun.special.RepairToolItem;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.KillIconPacket;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.util.ServerLog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.LAST_DRIVER_UUID;

public class SuperbWarfareEventHandler implements ISuperbWarfareHandler {
    private static final float MIN_EFFECTIVE_DAMAGE = 0.01f;
    private static final long OBSERVED_DAMAGE_WINDOW_MS = 1500L;
    private static volatile SuperbWarfareEventHandler ACTIVE_INSTANCE;
    private static final HttpClient DEBUG_HTTP = HttpClient.newHttpClient();
    private static final String DEBUG_URL = "http://127.0.0.1:7777/event";
    private static final String DEBUG_SESSION = "vehicle-bonus-missing";
    private static final String DEBUG_RUN = "post-fix";

    private final Map<VehicleEntity, VehicleCombatTracker> combatTrackerMap = new WeakHashMap<>();
    private final Map<VehicleEntity, VehicleStateSnapshot> vehicleStateMap = new WeakHashMap<>();
    private final Map<ServerPlayer, Long> lastRepairBonusTimeMap = new WeakHashMap<>();
    private final Map<UUID, Long> headshotVictims = new ConcurrentHashMap<>();
    private final Map<UUID, Long> headshotDamageVictims = new ConcurrentHashMap<>();
    private final Set<UUID> gunKillVictims = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void init() {
        ACTIVE_INSTANCE = this;
        ServerBridge.loader().registerForgeEventBusSubscriber(this);
        try {
            Class<?> neoForgeClass = Class.forName("net.neoforged.neoforge.common.NeoForge");
            Object eventBus = neoForgeClass.getField("EVENT_BUS").get(null);
            eventBus.getClass().getMethod("register", Object.class).invoke(eventBus, this);
        } catch (Exception ignored) {
            ServerBridge.loader().registerForgeEventBusSubscriber(this);
        }
        // #region debug-point B:handler-init
        JsonObject debugData = new JsonObject();
        debugData.addProperty("activeInstance", ACTIVE_INSTANCE != null);
        debugData.addProperty("loaderHasSuperbWarfare", ServerBridge.loader().isModLoaded("superbwarfare"));
        reportDebug("B", "SuperbWarfareEventHandler.init", "[DEBUG] superb warfare handler initialized", debugData);
        // #endregion
        ServerLog.info("SuperbWarfare event handler registered.");
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        headshotVictims.entrySet().removeIf(entry -> now - entry.getValue() > 5000L);
        headshotDamageVictims.entrySet().removeIf(entry -> now - entry.getValue() > 5000L);
        gunKillVictims.clear();
        processObservedVehicles();
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

    public static void onVehicleDamageApplied(Entity vehicleEntity, DamageSource source, float healthBefore) {
        SuperbWarfareEventHandler handler = ACTIVE_INSTANCE;
        if (handler == null || !(vehicleEntity instanceof VehicleEntity vehicle) || source == null) {
            return;
        }
        handler.observeVehicle(vehicle);
        ServerPlayer attacker = handler.resolveResponsiblePlayer(vehicle, source);
        if (attacker != null) {
            handler.markRecentPlayerAttack(vehicle, attacker);
        }
    }

    private void handleVehicleDamageApplied(VehicleEntity vehicle, DamageSource source, float actualDamage) {
        if (vehicle.level().isClientSide()) {
            return;
        }

        // #region debug-point C:damage-applied
        JsonObject damageData = new JsonObject();
        damageData.addProperty("vehicleType", vehicle.getType().toString());
        damageData.addProperty("actualDamage", actualDamage);
        damageData.addProperty("sourceType", source == null ? "null" : String.valueOf(source.typeHolder().unwrapKey().map(Object::toString).orElse(null)));
        damageData.addProperty("sourceEntity", source == null || source.getEntity() == null ? "null" : source.getEntity().getType().toString());
        damageData.addProperty("directEntity", source == null || source.getDirectEntity() == null ? "null" : source.getDirectEntity().getType().toString());
        reportDebug("C", "SuperbWarfareEventHandler.handleVehicleDamageApplied", "[DEBUG] vehicle damage callback reached", damageData);
        // #endregion
        if (actualDamage <= MIN_EFFECTIVE_DAMAGE) {
            return;
        }

        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, ignored -> new VehicleCombatTracker());
        ServerPlayer attacker = resolveResponsiblePlayer(vehicle, source);
        // #region debug-point D:attacker-resolution
        JsonObject attackerData = new JsonObject();
        attackerData.addProperty("resolved", attacker != null);
        attackerData.addProperty("attacker", attacker == null ? "null" : attacker.getGameProfile().getName());
        reportDebug("D", "SuperbWarfareEventHandler.handleVehicleDamageApplied", "[DEBUG] vehicle attacker resolved", attackerData);
        // #endregion
        if (attacker == null) {
            return;
        }

        tracker.recordDamage(attacker.getUUID(), actualDamage, true);
        vehicle.getEntityData().set(VehicleEntity.LAST_ATTACKER_UUID, attacker.getStringUUID());

        if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
            // #region debug-point E:bonus-hit-vehicle
            JsonObject bonusData = new JsonObject();
            bonusData.addProperty("bonusType", "HIT_VEHICLE_ARMOR");
            bonusData.addProperty("amount", actualDamage);
            bonusData.addProperty("player", attacker.getGameProfile().getName());
            reportDebug("E", "SuperbWarfareEventHandler.handleVehicleDamageApplied", "[DEBUG] issuing hit vehicle armor bonus", bonusData);
            // #endregion
            ServerCore.BONUS.add(attacker, BonusType.HIT_VEHICLE_ARMOR, actualDamage, null);
        }
    }

    private ServerPlayer resolveResponsiblePlayer(VehicleEntity victim, DamageSource source) {
        if (source == null) {
            return resolvePlayerByUuid(victim.level(), parseUuid(victim.getEntityData().get(VehicleEntity.LAST_ATTACKER_UUID)));
        }
        ServerPlayer player = resolvePlayerFromEntity(victim, source.getEntity());
        if (player != null) {
            return player;
        }
        player = resolvePlayerFromEntity(victim, source.getDirectEntity());
        if (player != null) {
            return player;
        }
        return resolvePlayerByUuid(victim.level(), parseUuid(victim.getEntityData().get(VehicleEntity.LAST_ATTACKER_UUID)));
    }

    private ServerPlayer resolvePlayerFromEntity(VehicleEntity victim, Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        if (entity instanceof Projectile projectile) {
            return resolvePlayerFromEntity(victim, projectile.getOwner());
        }
        if (entity instanceof VehicleEntity vehicle) {
            Entity lastDriver = EntityFindUtil.findEntity(victim.level(), vehicle.getEntityData().get(LAST_DRIVER_UUID));
            if (lastDriver instanceof ServerPlayer player) {
                return player;
            }
        }
        return null;
    }

    private ServerPlayer resolvePlayerByUuid(net.minecraft.world.level.Level level, UUID uuid) {
        if (uuid == null || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(uuid);
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        if (entity instanceof VehicleEntity attackerVehicle) {
            Entity lastDriver = EntityFindUtil.findEntity(level, attackerVehicle.getEntityData().get(LAST_DRIVER_UUID));
            if (lastDriver instanceof ServerPlayer player) {
                return player;
            }
        }
        return ServerCore.getServer() == null ? null : ServerCore.getServer().getPlayerList().getPlayer(uuid);
    }

    private VehicleStateSnapshot observeVehicle(VehicleEntity vehicle) {
        return vehicleStateMap.computeIfAbsent(vehicle, ignored -> VehicleStateSnapshot.capture(vehicle));
    }

    private void markRecentPlayerAttack(VehicleEntity vehicle, ServerPlayer attacker) {
        VehicleStateSnapshot snapshot = observeVehicle(vehicle);
        snapshot.pendingAttackerUuid = attacker.getUUID();
        snapshot.pendingAttackTime = System.currentTimeMillis();
    }

    private ServerPlayer resolveObservedAttacker(VehicleEntity vehicle, VehicleStateSnapshot snapshot, long now) {
        if (snapshot.pendingAttackerUuid == null) {
            return null;
        }
        if (now - snapshot.pendingAttackTime > OBSERVED_DAMAGE_WINDOW_MS) {
            snapshot.pendingAttackerUuid = null;
            return null;
        }
        return resolvePlayerByUuid(vehicle.level(), snapshot.pendingAttackerUuid);
    }

    private void processObservedVehicles() {
        var iterator = vehicleStateMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<VehicleEntity, VehicleStateSnapshot> entry = iterator.next();
            VehicleEntity vehicle = entry.getKey();
            VehicleStateSnapshot snapshot = entry.getValue();

            if (vehicle == null || vehicle.isRemoved()) {
                iterator.remove();
                combatTrackerMap.remove(vehicle);
                continue;
            }

            float currentHealth = vehicle.getHealth();
            boolean currentWreck = readVehicleWreckState(vehicle);
            float actualDamage = Math.max(0.0f, snapshot.lastHealth - currentHealth);
            long now = System.currentTimeMillis();

            if (actualDamage > MIN_EFFECTIVE_DAMAGE && !snapshot.wasWreck) {
                ServerPlayer observedAttacker = resolveObservedAttacker(vehicle, snapshot, now);
                if (observedAttacker != null) {
                    handleVehicleDamageApplied(vehicle, makeObservedDamageSource(observedAttacker), actualDamage);
                }
            }

            if (!snapshot.destructionHandled && !snapshot.wasWreck && (currentWreck || currentHealth <= 0.0f)) {
                VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, ignored -> new VehicleCombatTracker());
                UUID actualLastAttackerUuid = parseUuid(vehicle.getEntityData().get(VehicleEntity.LAST_ATTACKER_UUID));
                UUID lastDriverUuid = parseUuid(vehicle.getEntityData().get(LAST_DRIVER_UUID));
                if (actualLastAttackerUuid != null && !actualLastAttackerUuid.equals(lastDriverUuid)) {
                    tracker.lastAttackerUuid = actualLastAttackerUuid;
                    tracker.lastAttackTime = System.currentTimeMillis();
                    tracker.lastAttackerWasPlayer = true;
                }
                processVehicleDestruction(vehicle, tracker);
                snapshot.destructionHandled = true;
            }

            snapshot.lastHealth = currentHealth;
            snapshot.wasWreck = currentWreck;
            if (snapshot.pendingAttackerUuid != null && now - snapshot.pendingAttackTime > OBSERVED_DAMAGE_WINDOW_MS) {
                snapshot.pendingAttackerUuid = null;
            }
        }
    }

    @SubscribeEvent
    public void onProjectileHitEntity(ProjectileHitEvent.HitEntity event) {
        Entity target = event.getTarget();
        if (target instanceof VehicleEntity vehicle) {
            observeVehicle(vehicle);
            if (event.getOwner() instanceof ServerPlayer player) {
                markRecentPlayerAttack(vehicle, player);
            }
        }
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        if (event.isHeadshot()) {
            headshotDamageVictims.put(living.getUUID(), System.currentTimeMillis());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onProjectileHitBlock(ProjectileHitEvent.HitBlock event) {
        if (event.getProjectile().level().isClientSide()) {
            return;
        }
        if (!(event.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getState().isAir()) {
            return;
        }
        if (!event.getProjectile().level().getBlockState(event.getPos()).isAir()) {
            return;
        }
        ServerCore.BONUS.add(player, BonusType.DESTROY_BLOCK, 1.0f, "");
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide) {
            return;
        }

        DamageSource source = event.getSource();
        if (!DamageTypeTool.isGunDamage(source)) {
            return;
        }

        UUID victimId = victim.getUUID();
        gunKillVictims.add(victimId);
        if (DamageTypeTool.isHeadshotDamage(source)) {
            headshotVictims.put(victimId, System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public void onShootRepair(ShootEvent.Post event) {
        if (!(event.getShooter() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getData().item() instanceof RepairToolItem)) {
            return;
        }

        double reachDistance = 5.0;
        var viewVector = player.getViewVector(1.0F);
        var startPos = player.getEyePosition();
        var endPos = startPos.add(viewVector.scale(reachDistance));
        var hitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player,
                startPos,
                endPos,
                player.getBoundingBox().expandTowards(viewVector.scale(reachDistance)).inflate(1.0),
                entity -> entity instanceof VehicleEntity,
                reachDistance * reachDistance
        );

        if (!(hitResult != null && hitResult.getEntity() instanceof VehicleEntity vehicle)) {
            return;
        }

        observeVehicle(vehicle);

        Entity lastDriver = EntityFindUtil.findEntity(player.level(), vehicle.getEntityData().get(LAST_DRIVER_UUID));
        boolean isDamage = (lastDriver != null && !SeekTool.IN_SAME_TEAM.test(player, lastDriver) && lastDriver.getTeam() != null)
                || player.isShiftKeyDown();
        if (isDamage) {
            markRecentPlayerAttack(vehicle, player);
            return;
        }
        if (vehicle.getHealth() >= vehicle.getMaxHealth()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastBonusTime = lastRepairBonusTimeMap.getOrDefault(player, 0L);
        if (now - lastBonusTime <= 2000L) {
            return;
        }
        if (ServerData.get().isBonusEnabled(BonusType.VEHICLE_REPAIR)) {
            // #region debug-point E:bonus-vehicle-repair
            JsonObject repairData = new JsonObject();
            repairData.addProperty("player", player.getGameProfile().getName());
            repairData.addProperty("vehicleType", vehicle.getType().toString());
            repairData.addProperty("health", vehicle.getHealth());
            repairData.addProperty("maxHealth", vehicle.getMaxHealth());
            reportDebug("E", "SuperbWarfareEventHandler.onShootRepair", "[DEBUG] issuing vehicle repair bonus", repairData);
            // #endregion
            ServerCore.BONUS.add(player, BonusType.VEHICLE_REPAIR, 1.0f, null);
        }
        lastRepairBonusTimeMap.put(player, now);
    }

    @SubscribeEvent
    public void onVehicleJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof VehicleEntity vehicle) {
            observeVehicle(vehicle);
        }
    }

    @SubscribeEvent
    public void onVehicleLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof VehicleEntity vehicle)) {
            return;
        }

        VehicleStateSnapshot snapshot = vehicleStateMap.get(vehicle);
        if (snapshot != null && !snapshot.destructionHandled && vehicle.getHealth() <= 0.0f) {
            VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, ignored -> new VehicleCombatTracker());
            UUID lastDriverUuid = parseUuid(vehicle.getEntityData().get(LAST_DRIVER_UUID));
            UUID actualLastAttackerUuid = parseUuid(vehicle.getEntityData().get(VehicleEntity.LAST_ATTACKER_UUID));
            if (actualLastAttackerUuid != null && !actualLastAttackerUuid.equals(lastDriverUuid)) {
                tracker.lastAttackerUuid = actualLastAttackerUuid;
                tracker.lastAttackTime = System.currentTimeMillis();
                tracker.lastAttackerWasPlayer = true;
            }
            processVehicleDestruction(vehicle, tracker);
        }

        vehicleStateMap.remove(vehicle);
        combatTrackerMap.remove(vehicle);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @SubscribeEvent
    public void onVehicleDealDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker instanceof VehicleEntity vehicle) {
            observeVehicle(vehicle);
            recordVehicleDamage(vehicle, event.getAmount());
            return;
        }
        if (source.getDirectEntity() instanceof VehicleEntity vehicle) {
            observeVehicle(vehicle);
            recordVehicleDamage(vehicle, event.getAmount());
            return;
        }
        if (attacker instanceof ServerPlayer player && player.getVehicle() instanceof VehicleEntity vehicle) {
            observeVehicle(vehicle);
            recordVehicleDamage(vehicle, event.getAmount());
        }
    }

    private void recordVehicleDamage(VehicleEntity vehicle, float amount) {
        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, ignored -> new VehicleCombatTracker());
        tracker.accumulatedDamageDealt += amount;
    }

    private void processVehicleDestruction(VehicleEntity vehicle, VehicleCombatTracker tracker) {
        long now = System.currentTimeMillis();
        ServerPlayer killer = null;
        if (tracker.lastAttackerWasPlayer && tracker.lastAttackerUuid != null
                && now - tracker.lastAttackTime < ServerData.get().getAssistTimeoutMs()) {
            killer = ServerCore.getServer().getPlayerList().getPlayer(tracker.lastAttackerUuid);
        }

        float maxHealth = vehicle.getMaxHealth();
        String vehicleNameKey = vehicle.getType().getDescriptionId();
        String extraInfo = vehicleNameKey + "|" + (int) maxHealth;
        if (killer != null) {
            UUID lastDriverUuid = parseUuid(vehicle.getEntityData().get(LAST_DRIVER_UUID));
            if (lastDriverUuid != null && lastDriverUuid.equals(killer.getUUID())) {
                killer = null;
            }
        }

        if (killer == null) {
            return;
        }

        double multiplier = ServerData.get().getBonusMultiplier(BonusType.DESTROY_VEHICLE);
        int score = (int) Math.ceil(maxHealth * multiplier);
        if (score > 0) {
            // #region debug-point E:bonus-destroy-vehicle
            JsonObject destroyData = new JsonObject();
            destroyData.addProperty("killer", killer.getGameProfile().getName());
            destroyData.addProperty("vehicleType", vehicleNameKey);
            destroyData.addProperty("score", score);
            destroyData.addProperty("accumulatedDamageDealt", tracker.accumulatedDamageDealt);
            reportDebug("E", "SuperbWarfareEventHandler.processVehicleDestruction", "[DEBUG] issuing destroy vehicle bonus", destroyData);
            // #endregion
            ServerCore.BONUS.add(killer, BonusType.DESTROY_VEHICLE, score, null, vehicle.getId(), vehicleNameKey);
        }
        if (tracker.accumulatedDamageDealt > 0 && ServerData.get().isBonusEnabled(BonusType.VALUE_TARGET_DESTROYED)) {
            // #region debug-point E:bonus-value-target-destroyed
            JsonObject valueData = new JsonObject();
            valueData.addProperty("killer", killer.getGameProfile().getName());
            valueData.addProperty("vehicleType", vehicleNameKey);
            valueData.addProperty("amount", tracker.accumulatedDamageDealt);
            reportDebug("E", "SuperbWarfareEventHandler.processVehicleDestruction", "[DEBUG] issuing value target destroyed bonus", valueData);
            // #endregion
            ServerCore.BONUS.add(killer, BonusType.VALUE_TARGET_DESTROYED, tracker.accumulatedDamageDealt, null);
        }
        awardVehicleDestroyAssistBonuses(vehicle, tracker, killer);

        ServerData.get().addKill(killer, 1);
        sendKillEffects(killer, KillType.DESTROY_VEHICLE, 0, vehicle.getId(), extraInfo);
    }

    private void awardVehicleDestroyAssistBonuses(VehicleEntity vehicle, VehicleCombatTracker tracker, ServerPlayer killer) {
        if (!ServerData.get().isBonusEnabled(BonusType.VEHICLE_DESTROY_ASSIST)) {
            return;
        }
        UUID killerUuid = killer.getUUID();
        UUID lastDriverUuid = parseUuid(vehicle.getEntityData().get(LAST_DRIVER_UUID));
        long now = System.currentTimeMillis();
        tracker.damageByAttacker.forEach((attackerUuid, damage) -> {
            if (attackerUuid == null || attackerUuid.equals(killerUuid) || damage <= MIN_EFFECTIVE_DAMAGE) {
                return;
            }
            Long lastContributionTime = tracker.lastContributionTimeByAttacker.get(attackerUuid);
            if (lastContributionTime == null || now - lastContributionTime > ServerData.get().getAssistTimeoutMs()) {
                return;
            }
            if (lastDriverUuid != null && lastDriverUuid.equals(attackerUuid)) {
                return;
            }
            ServerPlayer assister = ServerCore.getServer().getPlayerList().getPlayer(attackerUuid);
            if (assister == null) {
                return;
            }
            ServerCore.BONUS.add(assister, BonusType.VEHICLE_DESTROY_ASSIST, damage, null, vehicle.getId(), vehicle.getType().getDescriptionId());
        });
    }

    private void sendKillEffects(ServerPlayer player, int killType, int combo, int victimId, String victimName) {
        double window = ServerData.get().getComboWindowSeconds();
        boolean hasHelmet = false;

        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "scrolling", killType, combo, victimId, window, hasHelmet, victimName), player);
        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "valorant", killType, combo, victimId, window, hasHelmet, victimName), player);
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

    private static class VehicleCombatTracker {
        UUID lastAttackerUuid;
        long lastAttackTime;
        boolean lastAttackerWasPlayer;
        float accumulatedDamageDealt;
        Map<UUID, Float> damageByAttacker = new java.util.HashMap<>();
        Map<UUID, Long> lastContributionTimeByAttacker = new java.util.HashMap<>();

        void recordDamage(UUID attackerUuid, float damage, boolean isPlayer) {
            if (attackerUuid != null) {
                lastAttackerUuid = attackerUuid;
                if (damage > 0.0f) {
                    damageByAttacker.merge(attackerUuid, damage, Float::sum);
                    lastContributionTimeByAttacker.put(attackerUuid, System.currentTimeMillis());
                }
            }
            lastAttackTime = System.currentTimeMillis();
            lastAttackerWasPlayer = isPlayer;
        }
    }

    private static class VehicleStateSnapshot {
        float lastHealth;
        boolean wasWreck;
        boolean destructionHandled;
        UUID pendingAttackerUuid;
        long pendingAttackTime;

        static VehicleStateSnapshot capture(VehicleEntity vehicle) {
            VehicleStateSnapshot snapshot = new VehicleStateSnapshot();
            snapshot.lastHealth = vehicle.getHealth();
            snapshot.wasWreck = readVehicleWreckState(vehicle);
            snapshot.destructionHandled = snapshot.wasWreck || vehicle.getHealth() <= 0.0f;
            return snapshot;
        }
    }

    private static boolean readVehicleWreckState(VehicleEntity vehicle) {
        try {
            Object value = VehicleEntity.class.getMethod("isWreck").invoke(vehicle);
            if (value instanceof Boolean bool) {
                return bool;
            }
        } catch (Exception ignored) {
        }

        try {
            Object value = VehicleEntity.class.getMethod("getIsWreck").invoke(vehicle);
            if (value instanceof Boolean bool) {
                return bool;
            }
        } catch (Exception ignored) {
        }

        try {
            Object accessor = VehicleEntity.class.getField("IS_WRECK").get(null);
            Object value = vehicle.getEntityData().get((net.minecraft.network.syncher.EntityDataAccessor<Boolean>) accessor);
            if (value instanceof Boolean bool) {
                return bool;
            }
        } catch (Exception ignored) {
        }

        return vehicle.getHealth() <= 0.0f;
    }

    private static DamageSource makeObservedDamageSource(ServerPlayer attacker) {
        return attacker.damageSources().playerAttack(attacker);
    }

    public static void reportDebug(String hypothesisId, String location, String msg, JsonObject data) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("sessionId", DEBUG_SESSION);
            payload.addProperty("runId", DEBUG_RUN);
            payload.addProperty("hypothesisId", hypothesisId);
            payload.addProperty("location", location);
            payload.addProperty("msg", msg);
            payload.add("data", data == null ? new JsonObject() : data);
            payload.addProperty("ts", System.currentTimeMillis());
            HttpRequest request = HttpRequest.newBuilder(URI.create(DEBUG_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
            DEBUG_HTTP.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }
}
