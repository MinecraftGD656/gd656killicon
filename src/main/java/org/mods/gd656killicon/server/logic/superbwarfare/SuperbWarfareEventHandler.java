package org.mods.gd656killicon.server.logic.superbwarfare;

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent;
import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.api.event.ShootEvent;
import com.atsuishio.superbwarfare.event.custom.ProjectileHitCallback;
import com.atsuishio.superbwarfare.event.custom.ReloadCallback;
import com.atsuishio.superbwarfare.event.custom.ShootCallback;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.gun.special.RepairToolItem;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SeekTool;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.logic.core.VehicleRewardHelper;
import org.mods.gd656killicon.server.util.ServerLog;

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

    private final Map<VehicleEntity, VehicleCombatTracker> combatTrackerMap = new WeakHashMap<>();
    private final Map<VehicleEntity, VehicleStateSnapshot> vehicleStateMap = new WeakHashMap<>();
    private final Map<ServerPlayer, Long> lastRepairBonusTimeMap = new WeakHashMap<>();
    private final Map<String, Long> headshotVictims = new ConcurrentHashMap<>();
    private final Map<String, Long> headshotDamageVictims = new ConcurrentHashMap<>();
    private final Set<UUID> gunKillVictims = Collections.newSetFromMap(new ConcurrentHashMap<>());
    /** 布雷者: SBW 反坦克地雷(TM-62)爆炸记录(owner, 爆炸位置, 时间), 载具被毁时匹配。 */
    private final java.util.List<MineExplosion> mineExplosions = new java.util.concurrent.CopyOnWriteArrayList<>();

    private record MineExplosion(java.util.UUID owner, double x, double y, double z, long time) {
    }

    @Override
    public void init() {
        ACTIVE_INSTANCE = this;
        ProjectileHitCallback.HIT_ENTITY.register(this::onProjectileHitEntity);
        ProjectileHitCallback.HIT_BLOCK.register(this::onProjectileHitBlock);
        ReloadCallback.POST.register(this::onReload);
        ShootCallback.POST.register(this::onShootRepair);
        EntityEvent.ADD.register(this::onVehicleJoinLevel);
        ServerEntityEvents.ENTITY_UNLOAD.register(this::onMineEntityLeave);
        ServerEntityEvents.ENTITY_UNLOAD.register(this::onVehicleLeaveLevel);
        EntityEvent.LIVING_HURT.register(this::onVehicleDealDamage);
        EntityEvent.LIVING_DEATH.register(this::onLivingDeath);
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
    public boolean isHeadshotKill(UUID attackerId, UUID victimId) {
        String key = attackerId.toString() + ":" + victimId.toString();
        return headshotVictims.remove(key) != null;
    }

    @Override
    public boolean isHeadshotDamage(UUID attackerId, UUID victimId) {
        String key = attackerId.toString() + ":" + victimId.toString();
        return headshotDamageVictims.remove(key) != null;
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
        if (actualDamage <= MIN_EFFECTIVE_DAMAGE) {
            return;
        }

        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, ignored -> new VehicleCombatTracker());
        ServerPlayer attacker = resolveResponsiblePlayer(vehicle, source);
        if (attacker == null) {
            return;
        }

        tracker.recordDamage(attacker.getUUID(), actualDamage, true);
        vehicle.getEntityData().set(VehicleEntity.LAST_ATTACKER_UUID, attacker.getStringUUID());

        if (ServerData.get().isBonusEnabled(BonusType.HIT_VEHICLE_ARMOR)) {
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
        if (event.isHeadshot() && event.getOwner() != null) {
            UUID attackerId = event.getOwner().getUUID();
            String key = attackerId.toString() + ":" + living.getUUID().toString();
            headshotDamageVictims.put(key, System.currentTimeMillis());
        }
    }

    public void onProjectileHitBlock(ProjectileHitEvent.HitBlock event) {
        if (event.getProjectile().level().isClientSide()) {
            return;
        }
        if (!(event.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        net.minecraft.world.level.block.state.BlockState state = event.getState();
        net.minecraft.core.BlockPos pos = event.getPos();
        if (state == null || pos == null || state.isAir()) {
            return;
        }
        if (!event.getProjectile().level().getBlockState(pos).isAir()) {
            return;
        }
        ServerCore.BONUS.add(player, BonusType.DESTROY_BLOCK, 1.0f, "");
    }

    public EventResult onLivingDeath(LivingEntity victim, DamageSource source) {
        if (victim == null || victim.level().isClientSide) {
            return EventResult.pass();
        }

        if (!DamageTypeTool.isGunDamage(source)) {
            return EventResult.pass();
        }

        UUID victimId = victim.getUUID();
        gunKillVictims.add(victimId);
        if (DamageTypeTool.isHeadshotDamage(source)) {
            Entity attackerEntity = source.getEntity();
            if (attackerEntity != null) {
                UUID attackerId = attackerEntity.getUUID();
                String key = attackerId.toString() + ":" + victimId.toString();
                headshotVictims.put(key, System.currentTimeMillis());
            }
        }
        // 势不可挡: SBW 机枪击杀(非机枪击杀清零)
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer shooter) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onMachineGunKill(shooter, isSbwMachineGun(shooter.getMainHandItem()));
        }
        // 神射手: SBW 狙击枪击杀
        if (attacker instanceof ServerPlayer shooter && isSbwSniper(shooter.getMainHandItem())) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onSniperKill(shooter);
        }
        // 弹头: SBW 火箭筒爆头击杀
        if (DamageTypeTool.isHeadshotDamage(source)
                && attacker instanceof ServerPlayer shooter && isSbwRocket(shooter.getMainHandItem())) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onWarheadKill(shooter);
        }
        // 步枪手: SBW 突击步枪击杀
        if (attacker instanceof ServerPlayer shooter && isSbwRifle(shooter.getMainHandItem())) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onRifleKill(shooter);
        }
        return EventResult.pass();
    }

    /** 换弹完成(服务端): 中断机枪连续击杀(势不可挡)。 */
    public void onReload(ReloadEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            org.mods.gd656killicon.server.ServerCore.HONOR.onReload(player);
        }
    }

    /** 手持是否为 SBW 机枪(直接匹配物品注册名, 5 个机枪: devotion/rpk/m_60/m_2_hb/minigun)。 */
    private boolean isSbwMachineGun(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        net.minecraft.resources.ResourceLocation key =
                BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && SBW_MACHINE_GUN_IDS.contains(key.toString());
    }

    private static final java.util.Set<String> SBW_MACHINE_GUN_IDS = java.util.Set.of(
            "superbwarfare:devotion", "superbwarfare:rpk", "superbwarfare:m_60",
            "superbwarfare:m_2_hb", "superbwarfare:minigun");

    /** 手持是否为 SBW 狙击枪(直接匹配物品注册名)。 */
    private boolean isSbwSniper(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        net.minecraft.resources.ResourceLocation key =
                BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && SBW_SNIPER_IDS.contains(key.toString());
    }

    private static final java.util.Set<String> SBW_SNIPER_IDS = java.util.Set.of(
            "superbwarfare:awm", "superbwarfare:hunting_rifle", "superbwarfare:k_98",
            "superbwarfare:m_98b", "superbwarfare:mosin_nagant", "superbwarfare:ntw_20",
            "superbwarfare:ql_1031", "superbwarfare:sentinel", "superbwarfare:svd");

    /** 手持是否为 SBW 火箭筒(直接匹配物品注册名, DirectLauncher 类)。 */
    private boolean isSbwRocket(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        net.minecraft.resources.ResourceLocation key =
                BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && SBW_ROCKET_IDS.contains(key.toString());
    }

    private static final java.util.Set<String> SBW_ROCKET_IDS = java.util.Set.of(
            "superbwarfare:rpg", "superbwarfare:javelin", "superbwarfare:igla_9k38",
            "superbwarfare:super_star_shooter");

    /** 手持是否为 SBW 突击步枪(直接匹配物品注册名, Rifle 类)。 */
    private boolean isSbwRifle(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        net.minecraft.resources.ResourceLocation key =
                BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && SBW_RIFLE_IDS.contains(key.toString());
    }

    private static final java.util.Set<String> SBW_RIFLE_IDS = java.util.Set.of(
            "superbwarfare:ak_12", "superbwarfare:ak_47", "superbwarfare:hk_416",
            "superbwarfare:insidious", "superbwarfare:m_4", "superbwarfare:marlin",
            "superbwarfare:mk_14", "superbwarfare:qbz_191", "superbwarfare:qbz_95",
            "superbwarfare:sks");

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
            ServerCore.BONUS.add(player, BonusType.VEHICLE_REPAIR, 1.0f, null);
        }
        lastRepairBonusTimeMap.put(player, now);
    }

    public void onMineEntityLeave(Entity entity, Level level) {
        if (level.isClientSide()) {
            return;
        }
        if (!isTm62Mine(entity)) {
            return;
        }
        // TM-62 地雷爆炸(或移除)时记录: owner(放置玩家) + 爆炸位置 + 时间
        java.util.UUID owner = readMineOwner(entity);
        if (owner == null) {
            return;
        }
        mineExplosions.add(new MineExplosion(owner, entity.getX(), entity.getY(), entity.getZ(), System.currentTimeMillis()));
    }

    /** SBW 反坦克地雷(TM-62)实体判定(实体注册名)。 */
    private static boolean isTm62Mine(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return false;
        }
        net.minecraft.resources.ResourceLocation key =
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && "superbwarfare:tm_62".equals(key.toString());
    }

    /** TM-62 地雷 owner UUID(反射 OwnableEntity.getOwnerUUID)。 */
    private static java.util.UUID readMineOwner(net.minecraft.world.entity.Entity entity) {
        try {
            java.lang.reflect.Method m = entity.getClass().getMethod("getOwnerUUID");
            Object v = m.invoke(entity);
            return v instanceof java.util.UUID uuid ? uuid : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 载具摧毁是否由地雷爆炸造成: killer 是地雷 owner, 且 3 秒内有爆炸且位置在 12 格内。 */
    private boolean hasRecentMineExplosion(ServerPlayer killer, net.minecraft.world.phys.Vec3 vehiclePos) {
        long now = System.currentTimeMillis();
        mineExplosions.removeIf(e -> now - e.time > 3000L);
        for (MineExplosion e : mineExplosions) {
            if (!e.owner().equals(killer.getUUID())) {
                continue;
            }
            double dx = e.x() - vehiclePos.x;
            double dy = e.y() - vehiclePos.y;
            double dz = e.z() - vehiclePos.z;
            if (dx * dx + dy * dy + dz * dz <= 144.0) {
                return true;
            }
        }
        return false;
    }

    public EventResult onVehicleJoinLevel(Entity entity, Level level) {
        if (level.isClientSide()) {
            return EventResult.pass();
        }
        if (entity instanceof VehicleEntity vehicle) {
            observeVehicle(vehicle);
        }
        return EventResult.pass();
    }

    public void onVehicleLeaveLevel(Entity entity, Level level) {
        if (level.isClientSide()) {
            return;
        }
        if (!(entity instanceof VehicleEntity vehicle)) {
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

    public EventResult onVehicleDealDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide) {
            return EventResult.pass();
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof VehicleEntity vehicle) {
            observeVehicle(vehicle);
            recordVehicleDamage(vehicle, amount);
            return EventResult.pass();
        }
        if (source.getDirectEntity() instanceof VehicleEntity vehicle) {
            observeVehicle(vehicle);
            recordVehicleDamage(vehicle, amount);
            return EventResult.pass();
        }
        if (attacker instanceof ServerPlayer player && player.getVehicle() instanceof VehicleEntity vehicle) {
            observeVehicle(vehicle);
            recordVehicleDamage(vehicle, amount);
        }
        return EventResult.pass();
    }

    private void recordVehicleDamage(VehicleEntity vehicle, float amount) {
        VehicleCombatTracker tracker = combatTrackerMap.computeIfAbsent(vehicle, ignored -> new VehicleCombatTracker());
        tracker.accumulatedDamageDealt += amount;
    }

    private void processVehicleDestruction(VehicleEntity vehicle, VehicleCombatTracker tracker) {
        float maxHealth = vehicle.getMaxHealth();
        String vehicleNameKey = vehicle.getType().getDescriptionId();
        String extraInfo = vehicleNameKey + "|" + (int) maxHealth;
        UUID lastDriverUuid = parseUuid(vehicle.getEntityData().get(LAST_DRIVER_UUID));
        ServerPlayer killer = VehicleRewardHelper.resolveRecentKiller(
                tracker.lastAttackerUuid,
                tracker.lastAttackerWasPlayer,
                tracker.lastAttackTime,
                lastDriverUuid
        );

        if (killer == null) {
            return;
        }
        if (!VehicleRewardHelper.shouldAwardDestroyVehicleRewards(killer, lastDriverUuid)) {
            return;
        }

        double multiplier = ServerData.get().getBonusMultiplier(BonusType.DESTROY_VEHICLE);
        int score = (int) Math.ceil(maxHealth * multiplier);
        if (score > 0) {
            ServerCore.BONUS.add(killer, BonusType.DESTROY_VEHICLE, score, null, vehicle.getId(), vehicleNameKey);
        }
        // 布雷者: 载具由 SBW 反坦克地雷(TM-62)爆炸摧毁(地雷 owner = 击杀者, 3 秒内且位置在爆炸半径内)
        if (hasRecentMineExplosion(killer, vehicle.position())) {
            ServerCore.HONOR.onMineLayer(killer);
        }
        if (tracker.accumulatedDamageDealt > 0 && ServerData.get().isBonusEnabled(BonusType.VALUE_TARGET_DESTROYED)) {
            ServerCore.BONUS.add(killer, BonusType.VALUE_TARGET_DESTROYED, tracker.accumulatedDamageDealt, null);
        }
        VehicleRewardHelper.awardDestroyAssistBonuses(
                killer.getUUID(),
                lastDriverUuid,
                vehicle.getId(),
                vehicleNameKey,
                MIN_EFFECTIVE_DAMAGE,
                tracker.damageByAttacker,
                tracker.lastContributionTimeByAttacker
        );

        ServerData.get().addKill(killer, 1);
        VehicleRewardHelper.sendDestroyVehicleEffects(killer, vehicle.getId(), extraInfo, (float) org.mods.gd656killicon.server.data.ServerData.get().getBonusMultiplier(org.mods.gd656killicon.common.BonusType.DESTROY_VEHICLE), score);
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
}
