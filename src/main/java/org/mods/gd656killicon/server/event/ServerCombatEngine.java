package org.mods.gd656killicon.server.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.logic.core.ServerBonusSwitches;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.PlayerDataManager;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.network.ServerPacketDispatcher;
import org.mods.gd656killicon.server.util.ServerLog;

import java.util.*;
import java.util.concurrent.*;

public final class ServerCombatEngine {
    private static final String CONQUEST_SQUAD_BEACON_CLASS = "org.mods.gd656conquest.common.entity.SquadDeployBeaconEntity";
    private static final String CONQUEST_GROUND_SENSOR_CLASS = "org.mods.gd656conquest.common.entity.GroundSensorEntity";
    private static final String CONQUEST_MEDICAL_BOX_CLASS = "org.mods.gd656conquest.common.entity.MedicalBoxEntity";
    private static final String CONQUEST_AMMO_BOX_CLASS = "org.mods.gd656conquest.common.entity.AmmoBoxEntity";
    private static final Map<UUID, Float> lastDamage = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, Integer>> lastDamageType = new ConcurrentHashMap<>();
    private static final Map<UUID, List<DamageRecord>> damageHistory = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, Long>> killHistory = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, CombatState>> activeCombats = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> explosionKillCounter = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> consecutiveDeaths = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Long>> playerKillTimestamps = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Long>> entityKillTimestamps = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, TeamKillRecord>> teamKillHistory = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lifeKillCount = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastItemSwitchTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastSelectedSlot = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> consecutiveAssists = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> lastSprintPositions = new HashMap<>();
    private static final Map<UUID, Double> sprintDistances = new HashMap<>();
    private static final Map<UUID, FireAttribution> fireAttribution = new ConcurrentHashMap<>();

    private static final List<PendingKill> pendingKills = new ArrayList<>();

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_EXPLOSION = 1;
    private static final int TYPE_HEADSHOT = 2;
    private static final int TYPE_CRIT = 3;
    private static final long LOCKED_TARGET_WINDOW_MS = 10000;
    private static final double HOLD_POSITION_MAX_DISTANCE = 1.0;
    private static final long FIRE_ATTRIBUTION_TIMEOUT_MS = 15000;
    private static final long SCOREBOARD_REFRESH_INTERVAL_MS = 60000L;

    private static long nextScoreboardRefreshAt = 0L;

    private ServerCombatEngine() {}

    private static void addBonus(ServerPlayer player, int bonusType, float scale, String extra) {
        if (ServerBonusSwitches.isEnabled(bonusType)) {
            ServerCore.BONUS.add(player, bonusType, scale, extra);
        }
    }

    private static void addBonus(ServerPlayer player, int bonusType, float scale, String extra, int victimId, String victimName) {
        if (ServerBonusSwitches.isEnabled(bonusType)) {
            ServerCore.BONUS.add(player, bonusType, scale, extra, victimId, victimName);
        }
    }

    private static String resolveVictimDisplayName(LivingEntity victim) {
        String baseName = victim instanceof net.minecraft.world.entity.player.Player
            ? victim.getScoreboardName()
            : (victim.hasCustomName() ? victim.getCustomName().getString() : victim.getType().getDescriptionId());
        return ServerCore.CUSTOM_NPCS.resolveVictimDisplayName(victim, baseName);
    }

    public static void onBlockBreak(ServerPlayer player) {
        if (player == null) return;
        addBonus(player, BonusType.DESTROY_BLOCK, 1.0f, "");
    }

    public static void onStarting(MinecraftServer server) {
        if (server == null) return;
        ServerLog.info("Initializing server data...");
        ServerData.get().init(server);
        PlayerDataManager.get().init(server);
        ServerCore.TACZ.init();
        ServerCore.YWZJ_VEHICLE.init();
        ServerCore.SUPERB_WARFARE.init();
        ServerCore.IMMERSIVE_AIRCRAFT.init();
        ServerCore.SPOTTING.init();
        ServerCore.PING_WHEEL.init();
        ServerCore.CUSTOM_NPCS.init();
        ServerCore.CONQUEST_BATTLEFIELD.init();
        ServerCore.CONQUEST_SPOTTING.init();
        nextScoreboardRefreshAt = System.currentTimeMillis() + SCOREBOARD_REFRESH_INTERVAL_MS;
    }

    public static void onStopping(MinecraftServer server) {
        ServerLog.info("Saving server data...");
        ServerData.get().saveAll();
        ServerData.get().shutdown();
        PlayerDataManager.get().shutdown();
        nextScoreboardRefreshAt = 0L;
    }

    public static void onTick() {
        MinecraftServer server = ServerBridge.loader().getCurrentServer();
        ServerCore.BONUS.tick(server);

        ServerCore.TACZ.tick();
        ServerCore.SUPERB_WARFARE.tick();
        ServerCore.YWZJ_VEHICLE.tick();
        ServerCore.IMMERSIVE_AIRCRAFT.tick();
        ServerCore.SPOTTING.tick();
        ServerCore.PING_WHEEL.tick();
        ServerCore.CONQUEST_BATTLEFIELD.tick();

        explosionKillCounter.clear();

        long now = System.currentTimeMillis();
        tickScoreboardRefresh(server, now);
        activeCombats.values().forEach(map -> map.values().removeIf(cs -> now - cs.lastInteractionTime > 30000));
        activeCombats.values().removeIf(Map::isEmpty);

        teamKillHistory.values().forEach(map -> map.values().removeIf(record -> now - record.timestamp() > 60000));
        teamKillHistory.values().removeIf(Map::isEmpty);

        lastItemSwitchTime.values().removeIf(timestamp -> now - timestamp > 10000);

        damageHistory.values().forEach(records -> {
            synchronized (records) {
                records.removeIf(r -> now - r.timestamp() > 120000);
            }
        });
        damageHistory.values().removeIf(List::isEmpty);
        fireAttribution.values().removeIf(record -> now - record.timestamp() > FIRE_ATTRIBUTION_TIMEOUT_MS);

        processPendingKills();
    }

    private static void tickScoreboardRefresh(MinecraftServer server, long now) {
        if (server == null || nextScoreboardRefreshAt == 0L || now < nextScoreboardRefreshAt) {
            return;
        }
        ServerData.get().refreshScoreboard(server);
        nextScoreboardRefreshAt = now + SCOREBOARD_REFRESH_INTERVAL_MS;
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (player == null) return;
        PlayerDataManager.get().updateLastLoginName(player.getUUID(), player.getScoreboardName());
        ServerData.get().syncScoreToPlayer(player);
        lastSelectedSlot.put(player.getUUID(), player.getInventory().selected);
        lastSprintPositions.put(player.getUUID(), player.position());
    }

    public static void onPlayerLogout(ServerPlayer player) {
        if (player == null) return;
        UUID playerId = player.getUUID();
        PlayerDataManager.get().forceSave(playerId);
        lastSprintPositions.remove(playerId);
        sprintDistances.remove(playerId);
    }

    public static void onPlayerTick(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return;

        if (player.getAbilities().flying || player.isSpectator()) {
            lastSprintPositions.put(player.getUUID(), player.position());
            return;
        }

        UUID playerId = player.getUUID();
        Vec3 currentPos = player.position();

        Vec3 lastPos = lastSprintPositions.get(playerId);

        if (lastPos == null || !currentPos.equals(lastPos)) {
            lastSprintPositions.put(playerId, currentPos);
        }

        if (lastPos == null) return;
        if (!player.isSprinting()) return;

        double dx = currentPos.x - lastPos.x;
        double dz = currentPos.z - lastPos.z;
        double distSqr = dx * dx + dz * dz;

        if (distSqr < 0.0001 || distSqr > 100.0) return;

        double distance = Math.sqrt(distSqr);
        double total = sprintDistances.getOrDefault(playerId, 0.0) + distance;

        while (total >= 200.0) {
            addBonus(player, BonusType.CHARGE_ASSAULT, 1.0f, "");
            total -= 200.0;
        }
        sprintDistances.put(playerId, total);
    }

    public static void onItemSwitch(ServerPlayer player, net.minecraft.world.entity.EquipmentSlot slot, ItemStack from, ItemStack to) {
        if (player == null || player.level().isClientSide) return;
        if (slot != null && slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HAND) {
            int currentSlot = player.getInventory().selected;
            Integer lastSlot = lastSelectedSlot.get(player.getUUID());
            lastSelectedSlot.put(player.getUUID(), currentSlot);

            if (lastSlot == null) {
                return;
            }

            if (lastSlot != currentSlot) {
                lastItemSwitchTime.put(player.getUUID(), System.currentTimeMillis());
            } else {
                if (!ItemStack.isSameItem(from, to)) {
                    lastItemSwitchTime.put(player.getUUID(), System.currentTimeMillis());
                }
            }
        }
    }

    public static void onEntityInteract(ServerPlayer player, LivingEntity victim, ItemStack itemStack, boolean levelIsClientSide) {
        if (levelIsClientSide) return;
        if (player == null || victim == null || itemStack == null) return;
        if (!itemStack.is(Items.FLINT_AND_STEEL) && !itemStack.is(Items.FIRE_CHARGE)) return;
        ServerCombatAttribution.recordFireAttribution(fireAttribution, victim.getUUID(), player.getUUID());
    }

    public static void onDamage(LivingEntity victim, DamageSource src, float amt) {
        if (victim == null || src == null || victim.level().isClientSide || amt <= 0) return;
        if (isConquestTacticalGadget(victim)) return;

        UUID victimId = victim.getUUID();

        LivingEntity resolvedAttacker = resolveLivingAttacker(src, victim);
        if (resolvedAttacker instanceof ServerPlayer player && player.getUUID().equals(victimId)) {
            return;
        }

        updateCombatTracking(src, victim, victimId, amt);

        if (resolvedAttacker != null) {
            LivingEntity attacker = resolvedAttacker;
            float effectiveAmt = Math.min(amt, victim.getHealth());
            int roundedAmt = Math.round(effectiveAmt);
            if (roundedAmt > 0) {
                recordDamage(victimId, attacker.getUUID(), roundedAmt);
            }
        }

        if (!(resolvedAttacker instanceof ServerPlayer player)) return;

        lastDamage.put(victimId, amt);
        boolean isMeleeCrit = src.is(DamageTypes.PLAYER_ATTACK) && ServerCore.CRIT.isMeleeCrit(player);
        ServerCore.CRIT.updateCrit(player, victimId, isMeleeCrit);

        int type = determineDamageType(player, victimId, src);
        lastDamageType.computeIfAbsent(victimId, k -> new ConcurrentHashMap<>()).put(player.getUUID(), type);

        float effectiveAmt = Math.min(amt, victim.getHealth());
        int roundedAmt = Math.round(effectiveAmt);

        if (roundedAmt > 0) {
            addDamageBonus(player, type, roundedAmt);
            // 命中信息: 伤害量累积由客户端按实体在显示窗口内处理
            ServerPacketDispatcher.sendHitInfo(player, roundedAmt, false, victim.getId());
        }

        if (amt < victim.getHealth()) {
            ServerPacketDispatcher.sendDamageSound(player, type == TYPE_HEADSHOT);
        }
    }

    public static void onDeath(LivingEntity victim, DamageSource src) {
        if (victim == null || src == null || victim.level().isClientSide) return;
        if (isConquestTacticalGadget(victim)) return;

        UUID victimId = victim.getUUID();

        if (victim instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            consecutiveDeaths.merge(playerId, 1, Integer::sum);

            ServerData.get().addDeath(player, 1);

            String killerName = "";
            if (src.getEntity() instanceof ServerPlayer killer) {
                killerName = killer.getScoreboardName();
            }

            ServerPacketDispatcher.sendDeath(player, player.getScoreboardName(), src.getMsgId(), killerName);
        }

        LivingEntity attacker = resolveLivingAttacker(src, victim);
        if (attacker != null) {
            ServerCore.SPOTTING.onLivingDeath(victim, attacker);
        }
        if (attacker != null) {
            killHistory.computeIfAbsent(victimId, k -> new ConcurrentHashMap<>())
                .put(attacker.getUUID(), System.currentTimeMillis());

            if (victim instanceof ServerPlayer victimPlayer && victimPlayer.getTeam() != null) {
                teamKillHistory.computeIfAbsent(attacker.getUUID(), k -> new ConcurrentHashMap<>())
                    .put(victimPlayer.getTeam().getName(), new TeamKillRecord(victimPlayer.getUUID(), System.currentTimeMillis()));
            }

            if (attacker instanceof Mob mob && !(attacker instanceof ServerPlayer)) {
                long now = System.currentTimeMillis();
                entityKillTimestamps.computeIfAbsent(mob.getUUID(), k -> Collections.synchronizedList(new ArrayList<>())).add(now);
            }
        }

        boolean hasHelmet = !victim.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty();
        boolean isVictimPlayer = victim instanceof net.minecraft.world.entity.player.Player;
        String victimName = resolveVictimDisplayName(victim);
        ServerPlayer killer = resolvePlayerAttacker(src, victim);
        // 炮手: 单次存活在载具非主驾驶位击杀(击杀瞬间判定, 玩家仍在载具上)
        if (killer != null && isGunnerSeat(killer)) {
            ServerCore.HONOR.onGunnerKill(killer);
        }
        // 路霸: 载具压死生物(直接伤害源为载具实体, SBW 碾压 direct=载具 / attacker=乘客玩家) → 归给载具驾驶员(可多次)
        net.minecraft.world.entity.Entity vehicleEntity = null;
        if (isVehicleEntity(src.getDirectEntity())) {
            vehicleEntity = src.getDirectEntity();
        } else if (isVehicleEntity(src.getEntity())) {
            vehicleEntity = src.getEntity();
        }
        if (vehicleEntity != null) {
            ServerPlayer driver = resolveVehicleDriver(vehicleEntity);
            if (driver != null) {
                ServerCore.HONOR.onRoadkill(driver);
            }
        }
        if (killer != null) {
            // 命中信息: 击杀任意生物 → 对应实体的伤害占位符切换为击杀颜色
            ServerPacketDispatcher.sendHitInfo(killer, 0.0f, true, victim.getId());
            handlePlayerKill(killer, victim, src);
            processAssist(victimId, victim.getId(), hasHelmet, victimName, isVictimPlayer, killer.getUUID());
        } else {
            processAssist(victimId, victim.getId(), hasHelmet, victimName, isVictimPlayer, null);
        }

        playerKillTimestamps.remove(victimId);
        entityKillTimestamps.remove(victimId);
        lifeKillCount.remove(victimId);
        consecutiveAssists.remove(victimId);

        cleanupVictimData(victimId);
    }

    private static void processPendingKills() {
        if (pendingKills.isEmpty()) return;

        List<PendingKill> readyKills = new ArrayList<>();
        synchronized (pendingKills) {
            Iterator<PendingKill> it = pendingKills.iterator();
            while (it.hasNext()) {
                PendingKill pk = it.next();
                if (pk.delay-- <= 0) {
                    readyKills.add(pk);
                    it.remove();
                }
            }
        }

        if (readyKills.isEmpty()) return;

        Map<String, List<PendingKill>> groups = new HashMap<>();
        for (PendingKill pk : readyKills) {
            if (pk.player == null || pk.player.isRemoved()) continue;

            String key = pk.player.getUUID().toString() + "_" + pk.sourceEntityId + "_" + pk.tick;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(pk);
        }

        for (List<PendingKill> group : groups.values()) {
            for (PendingKill pk : group) {
                processKill(pk);
            }

            if (group.size() >= 2) {
                PendingKill first = group.get(0);

                boolean hasExplosion = group.stream().anyMatch(pk -> pk.damageType == TYPE_EXPLOSION);
                boolean isProjectile = first.sourceEntityId != -1 && first.sourceEntityId != first.player.getId();

                if (!hasExplosion && (isProjectile || first.isGun)) {
                    int count = Math.min(group.size(), 8);
                    addBonus(first.player, BonusType.ONE_BULLET_MULTI_KILL, (float) count, String.valueOf(count));
                }
            }
        }
    }

    private static int determineDamageType(ServerPlayer player, UUID victimId, DamageSource src) {
        UUID attackerId = player.getUUID();
        if (ServerCore.TACZ.isHeadshotDamage(attackerId, victimId) || ServerCore.SUPERB_WARFARE.isHeadshotDamage(attackerId, victimId)) return TYPE_HEADSHOT;
        if (src.is(DamageTypeTags.IS_EXPLOSION)) return TYPE_EXPLOSION;
        if (ServerCore.CRIT.isRecentCrit(attackerId, victimId)) return TYPE_CRIT;
        return TYPE_NORMAL;
    }

    private static void recordDamage(UUID victimId, UUID attackerId, int amount) {
        damageHistory.computeIfAbsent(victimId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(new DamageRecord(attackerId, amount, System.currentTimeMillis()));
    }

    private static void addDamageBonus(ServerPlayer player, int type, int amount) {
        int bonusType = switch (type) {
            case TYPE_HEADSHOT -> BonusType.HEADSHOT;
            case TYPE_EXPLOSION -> BonusType.EXPLOSION;
            case TYPE_CRIT -> BonusType.CRIT;
            default -> BonusType.DAMAGE;
        };
        addBonus(player, bonusType, (float) amount, "");
    }

    private static void handlePlayerKill(ServerPlayer player, LivingEntity victim, DamageSource src) {
        UUID victimId = victim.getUUID();
        if (player.getUUID().equals(victimId)) {
            return;
        }
        int type = getLastDamageType(victimId, player.getUUID());

        ServerData.get().addKill(player, 1);

        if (type == TYPE_NORMAL && (src.is(DamageTypeTags.IS_EXPLOSION) || src.getMsgId().contains("explosion"))) {
            type = TYPE_EXPLOSION;
        }

        int sourceId = src.getDirectEntity() != null ? src.getDirectEntity().getId() : -1;
        long tick = player.level().getGameTime();
        boolean isGun = ServerCore.TACZ.isGunKill(victimId) || ServerCore.SUPERB_WARFARE.isGunKill(victimId);
        boolean isGliding = player.isFallFlying();
        boolean isVictimThreat = checkVictimThreat(player, victim);
        boolean isBackstab = checkBackstab(player, victim);
        boolean isJusticeFromAbove = checkJusticeFromAbove(player, victim, isGliding);
        boolean isVictimBlinded = checkBlinded(victim);
        boolean isLockedTarget = checkLockedTarget(player, victim);
        boolean hasHelmet = !victim.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty();
        boolean isVictimPlayer = victim instanceof net.minecraft.world.entity.player.Player;
        long streakCount = calculateStreakCount(victimId);

        consecutiveAssists.put(player.getUUID(), 0);

        boolean isFlawless = false;
        boolean isHoldPosition = false;
        Map<UUID, CombatState> combats = activeCombats.get(player.getUUID());
        if (combats != null) {
            CombatState cs = combats.get(victimId);
            if (cs != null) {
                isFlawless = cs.flawless;
                if (cs.initialPosition != null) {
                    isHoldPosition = cs.initialPosition.distanceTo(player.position()) <= HOLD_POSITION_MAX_DISTANCE;
                }
            }
        }

        double distanceDouble = player.distanceTo(victim);
        float distanceFloat = (float) distanceDouble;
        // 飞行调度员: 死亡瞬间受害者正搭乘**存活**的空中载具(载具未被摧毁, 否则乘客会脱离载具)
        boolean victimRidingAir = isAliveAirVehicle(victim.getVehicle());
        pendingKills.add(new PendingKill(
            player,
            victim.getUUID(),
            victim.getId(),
            victim.getBoundingBox().getCenter(),
            resolveVictimDisplayName(victim),
            ServerCore.COMBO.recordKill(player),
            type,
            victim.getMaxHealth(),
            distanceFloat,
            1,
            sourceId,
            tick,
            isGun,
            isVictimThreat,
            isBackstab,
            isGliding,
            isJusticeFromAbove,
            isFlawless,
            isVictimBlinded,
            hasHelmet,
            isVictimPlayer,
            isLockedTarget,
            isHoldPosition,
            streakCount,
            victimRidingAir
        ));

        ServerPacketDispatcher.sendKillDistance(player, distanceDouble);
    }

    private static void cleanupVictimData(UUID victimId) {
        lastDamage.remove(victimId);
        lastDamageType.remove(victimId);
        damageHistory.remove(victimId);
        activeCombats.values().forEach(map -> map.remove(victimId));
        fireAttribution.remove(victimId);
    }

    private static void processAssist(UUID victimId, int victimIdInt, boolean hasHelmet, String victimName, boolean isVictimPlayer, UUID excludedPlayerId) {
        List<DamageRecord> records = damageHistory.get(victimId);
        if (records == null || records.isEmpty()) return;

        final String finalVictimName = victimName;

        long now = System.currentTimeMillis();
        Map<UUID, Integer> playerDamages = new HashMap<>();

        synchronized (records) {
            long lastTime = records.stream().mapToLong(r -> r.timestamp()).max().orElse(0);
            long timeout = ServerData.get().getAssistTimeoutMs();
            if (now - lastTime > timeout) return;
            for (DamageRecord r : records) {
                if (now - r.timestamp() <= timeout) {
                    playerDamages.merge(r.attackerId(), r.amount(), Integer::sum);
                }
            }
        }

        playerDamages.forEach((playerId, totalDamage) -> {
            if (excludedPlayerId != null && excludedPlayerId.equals(playerId)) {
                return;
            }
            if (totalDamage > 0) {
                ServerPlayer player = ServerBridge.loader().getCurrentServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    addBonus(player, BonusType.ASSIST, (float) totalDamage, "", victimIdInt, finalVictimName);
                    sendKillEffects(player, KillType.ASSIST, 0, victimIdInt, hasHelmet, finalVictimName, isVictimPlayer, 0.0f,
                            (float) ServerData.get().getBonusMultiplier(BonusType.ASSIST), (float) totalDamage);

                    ServerData.get().addAssist(player, 1);

                    int count = consecutiveAssists.merge(playerId, 1, Integer::sum);
                    if (count >= 3) {
                        addBonus(player, BonusType.POTATO_AIM, 1.0f, "");
                        consecutiveAssists.put(playerId, 0);
                    }
                }
            }
        });
    }

    private static void processKill(PendingKill pk) {
        if (pk.player.getUUID().equals(pk.victimId)) return;

        String finalVictimName = pk.victimName;

        int killType = determineKillType(pk);
        int bonusType = mapKillTypeToBonus(killType, pk.damageType);

        // 复仇判定(awardStreakKills 会移除 history, 需在加分项判定前计算, 荣誉与加分项共用)
        Map<UUID, Long> avengeHistory = killHistory.get(pk.player.getUUID());
        boolean isAvenge = avengeHistory != null && avengeHistory.containsKey(pk.victimId);

        addBonus(pk.player, bonusType, pk.maxHealth, "", pk.victimIdInt, finalVictimName);
        awardSpecialKills(pk);
        awardPositionalKills(pk);
        awardHoldPosition(pk);
        awardStatusKills(pk);
        awardLockedTarget(pk);
        awardStreakKills(pk);

        updatePostKillStates(pk);

        // 荣誉判定(击杀证据; 受害者实体可能已移除, 布尔属性仍参与判定)
        net.minecraft.world.entity.LivingEntity victimEntity =
                pk.player.level().getEntity(pk.victimIdInt) instanceof net.minecraft.world.entity.LivingEntity le ? le : null;
        // 爆头判定 = killType == HEADSHOT(TACZ/SBW 枪械爆头或原版爆头伤害类型, 对生物同样有效);
        // 注意不能用 pk.hasHelmet(那是"受害者戴头盔", 语义不同)
        // 击杀物品名称(军械库等荣誉用; 按物品显示名判定, 改名/不同名即使 id 相同也算不同武器; 空手不算)
        net.minecraft.world.item.ItemStack handItem = pk.player.getMainHandItem();
        String weaponId = handItem.isEmpty() ? "" : handItem.getHoverName().getString();
        // 最高得分者判定(与加分项 SLAY_THE_LEADER 同条件)
        boolean victimTopScorer = ServerData.get().isTopScorer(pk.victimId);
        // 刽子手: 背刺(与加分项同判定) && 距离 < 2 米(近战背刺) && 手持原版近战武器或 LR 战术工坊武器
        boolean isExecutionerKill = pk.isBackstab && pk.distance < 2.0f && isMeleeWeapon(pk.player.getMainHandItem());
        // 侧袭: 击杀身上带 LR 致盲效果的生物(可一命多次)
        if (victimEntity != null && isLrBlinded(victimEntity)) {
            ServerCore.HONOR.onFlankKill(pk.player);
        }
        // 戍卫: Conquest 据点内击杀敌军且据点内有敌军(单次存活累计 3 次)
        if (isInGarrisonKillSituation(pk.player)) {
            ServerCore.HONOR.onGarrisonKill(pk.player);
        }
        // 急先锋: Conquest 对局中该阵营(CAMP_A/CAMP_B)第一个击杀者
        String firstKillKey = resolveFirstKillTeamKey(pk.player);
        if (firstKillKey != null) {
            ServerCore.HONOR.onFirstKill(pk.player, firstKillKey);
        }
        // 战士: 行走或疾跑 = 水平移动中(非静止; 速度或移动输入, 防止击杀瞬间速度归零)
        boolean isMoving = isPlayerWalkingOrSprinting(pk.player);
        ServerCore.HONOR.onKill(pk.player, victimEntity, killType == org.mods.gd656killicon.common.KillType.HEADSHOT,
                killType == org.mods.gd656killicon.common.KillType.DESTROY_VEHICLE,
                false, // TODO: 受害者空中载具判定(接入集成层后填充)
                false, // TODO: 击杀者搭乘空中载具判定(接入集成层后填充)
                killType, isAvenge, weaponId, victimTopScorer, isExecutionerKill, pk.distance, isMoving,
                pk.victimRidingAir);

        // <score> = 附加数据(伤害) × 加分项表达式(倍率)
        sendKillEffects(pk.player, killType, pk.combo, pk.victimIdInt, pk.hasHelmet, finalVictimName, pk.isVictimPlayer, pk.distance,
                (float) ServerData.get().getBonusMultiplier(bonusType), pk.maxHealth);
    }

    private static int determineKillType(PendingKill pk) {
        UUID attackerId = pk.player.getUUID();
        if (ServerCore.TACZ.isHeadshotKill(attackerId, pk.victimId) || ServerCore.SUPERB_WARFARE.isHeadshotKill(attackerId, pk.victimId) || pk.damageType == TYPE_HEADSHOT) return KillType.HEADSHOT;
        if (pk.damageType == TYPE_EXPLOSION) return KillType.EXPLOSION;
        if (ServerCore.CRIT.consumeCrit(attackerId, pk.victimId) || pk.damageType == TYPE_CRIT) return KillType.CRIT;
        return KillType.NORMAL;
    }

    private static int getLastDamageType(UUID victimId, UUID attackerId) {
        Map<UUID, Integer> victimMap = lastDamageType.get(victimId);
        return victimMap != null ? victimMap.getOrDefault(attackerId, TYPE_NORMAL) : TYPE_NORMAL;
    }

    private static int mapKillTypeToBonus(int killType, int damageType) {
        return switch (killType) {
            case KillType.HEADSHOT -> BonusType.KILL_HEADSHOT;
            case KillType.EXPLOSION -> BonusType.KILL_EXPLOSION;
            case KillType.CRIT -> BonusType.KILL_CRIT;
            default -> BonusType.KILL;
        };
    }

    private static void awardSpecialKills(PendingKill pk) {
        if (pk.damageType == TYPE_EXPLOSION) {
            int count = explosionKillCounter.merge(pk.player.getUUID(), 1, Integer::sum);
            if (count == 5) addBonus(pk.player, BonusType.SHOCKWAVE, 1.0f, "");
        }
        if (ServerCore.TACZ.isLastBulletKill(pk.victimId)) {
            addBonus(pk.player, BonusType.LAST_BULLET_KILL, 1.0f, "");
        }
        if (pk.isVictimThreat && pk.isFlawless) {
            addBonus(pk.player, BonusType.EFFORTLESS_KILL, 1.0f, "");
        }

        Long switchTime = lastItemSwitchTime.get(pk.player.getUUID());
        if (switchTime != null && System.currentTimeMillis() - switchTime <= 3000) {
            addBonus(pk.player, BonusType.QUICK_SWITCH, 1.0f, "");
            lastItemSwitchTime.remove(pk.player.getUUID());
        }
    }

    private static void awardPositionalKills(PendingKill pk) {
        if (pk.distance > 20.0f) {
            addBonus(pk.player, BonusType.KILL_LONG_DISTANCE, pk.distance, String.valueOf((int) pk.distance));
        }
        if (isObstructed(pk)) {
            addBonus(pk.player, BonusType.KILL_INVISIBLE, 1.0f, "");
        }
        if (pk.isGliding) {
            addBonus(pk.player, BonusType.ABSOLUTE_AIR_CONTROL, 1.0f, "");
        } else if (pk.isJusticeFromAbove) {
            addBonus(pk.player, BonusType.JUSTICE_FROM_ABOVE, 1.0f, "");
        }
        if (pk.isBackstab) {
            addBonus(pk.player, pk.distance < 2.0f ? BonusType.BACKSTAB_MELEE_KILL : BonusType.BACKSTAB_KILL, 1.0f, "");
        }
    }

    private static void awardHoldPosition(PendingKill pk) {
        if (pk.isHoldPosition) {
            addBonus(pk.player, BonusType.HOLD_POSITION, 1.0f, "");
        }
    }

    private static void awardStatusKills(PendingKill pk) {
        if (pk.player.getHealth() <= 4.0f) {
            addBonus(pk.player, BonusType.DESPERATE_COUNTERATTACK, 1.0f, "");
        }

        if (checkBlinded(pk.player)) {
            addBonus(pk.player, BonusType.BLIND_KILL, 1.0f, "");
        }

        if (pk.isVictimBlinded) {
            addBonus(pk.player, BonusType.SEIZE_OPPORTUNITY, 1.0f, "");
        }

        awardBuffDebuffKills(pk.player);
    }

    private static void awardLockedTarget(PendingKill pk) {
        if (pk.isLockedTarget) {
            addBonus(pk.player, BonusType.LOCKED_TARGET, 1.0f, "");
        }
    }

    private static void awardStreakKills(PendingKill pk) {
        if (pk.combo > 1) {
            addBonus(pk.player, BonusType.KILL_COMBO, (float) Math.min(pk.combo, 4), String.valueOf(pk.combo));
            // 4 连杀(4 连续击败加分项触发)后开始计算掠夺者 8 秒窗口
            if (pk.combo == 4) {
                ServerCore.HONOR.recordCombo4(pk.player);
            }
        }

        int deathCount = consecutiveDeaths.getOrDefault(pk.player.getUUID(), 0);
        if (deathCount >= 3) addBonus(pk.player, BonusType.BRAVE_RETURN, 1.0f, "");
        consecutiveDeaths.put(pk.player.getUUID(), 0);

        int lifeKills = lifeKillCount.merge(pk.player.getUUID(), 1, Integer::sum);
        awardLifeKillMilestone(pk.player, lifeKills);

        Map<UUID, Long> history = killHistory.get(pk.player.getUUID());
        if (history != null && history.containsKey(pk.victimId)) {
            addBonus(pk.player, BonusType.AVENGE, 1.0f, "");
            history.remove(pk.victimId);
        }

        if (pk.streakCount >= 5) {
            addBonus(pk.player, BonusType.INTERRUPTED_STREAK, (float) pk.streakCount, String.valueOf(pk.streakCount));
        }

        if (pk.player.getTeam() != null) {
            Map<String, TeamKillRecord> teamHistory = teamKillHistory.get(pk.victimId);
            if (teamHistory != null) {
                TeamKillRecord record = teamHistory.get(pk.player.getTeam().getName());
                if (record != null && System.currentTimeMillis() - record.timestamp() <= 60000 && !record.victimId().equals(pk.player.getUUID())) {
                    addBonus(pk.player, BonusType.LEAVE_IT_TO_ME, 1.0f, "");
                }
                teamKillHistory.remove(pk.victimId);
            }
        }

        if (pk.player.getTeam() != null) {
            long now = System.currentTimeMillis();
            Collection<String> teamMembers = pk.player.getTeam().getPlayers();
            for (String memberName : teamMembers) {
                ServerPlayer member = pk.player.getServer().getPlayerList().getPlayerByName(memberName);
                if (member != null && !member.getUUID().equals(pk.player.getUUID()) && member.isAlive()) {
                    List<DamageRecord> records = damageHistory.get(member.getUUID());
                    if (records != null) {
                        synchronized (records) {
                            boolean saved = records.stream().anyMatch(r ->
                                r.attackerId().equals(pk.victimId) &&
                                    now - r.timestamp() <= 5000);
                            if (saved) {
                                addBonus(pk.player, BonusType.SAVIOR, 1.0f, "");
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (ServerData.get().isTopScorer(pk.victimId)) {
            addBonus(pk.player, BonusType.SLAY_THE_LEADER, 1.0f, "");
        }
    }

    private static void awardBuffDebuffKills(ServerPlayer player) {
        boolean hasPositive = player.getActiveEffects().stream().anyMatch(e -> e.getEffect().isBeneficial());
        boolean hasNegativeExcludingSpecial = player.getActiveEffects().stream().anyMatch(e -> {
            net.minecraft.world.effect.MobEffect effect = e.getEffect();
            if (effect.isBeneficial()) return false;
            if (effect == MobEffects.BLINDNESS || effect == MobEffects.CONFUSION) return false;
            var key = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect);
            return key == null || !key.toString().equals("lrtactical:blinded");
        });

        if (hasPositive && hasNegativeExcludingSpecial) {
            addBonus(player, BonusType.BOTH_BUFF_DEBUFF_KILL, 1.0f, "");
        } else if (hasPositive) {
            addBonus(player, BonusType.BUFF_KILL, 1.0f, "");
        } else if (hasNegativeExcludingSpecial) {
            addBonus(player, BonusType.DEBUFF_KILL, 1.0f, "");
        }
    }

    private static void awardLifeKillMilestone(ServerPlayer player, int lifeKills) {
        if (lifeKills == 3) addBonus(player, BonusType.BERSERKER, 1.0f, "");
        else if (lifeKills == 5) addBonus(player, BonusType.BLOODTHIRSTY, 1.0f, "");
        else if (lifeKills == 10) addBonus(player, BonusType.MERCILESS, 1.0f, "");
        else if (lifeKills == 15) addBonus(player, BonusType.VALIANT, 1.0f, "");
        else if (lifeKills == 20) addBonus(player, BonusType.FIERCE, 1.0f, "");
        else if (lifeKills == 25) addBonus(player, BonusType.SAVAGE, 1.0f, "");
        else if (lifeKills == 30) addBonus(player, BonusType.PURGE, 1.0f, "");
    }

    private static boolean checkBlinded(LivingEntity entity) {
        if (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.CONFUSION) || entity.hasEffect(MobEffects.DARKNESS)) return true;
        try {
            net.minecraft.world.effect.MobEffect blinded = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lrtactical", "blinded"));
            return blinded != null && entity.hasEffect(blinded);
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void updatePostKillStates(PendingKill pk) {
        playerKillTimestamps.remove(pk.victimId);
        entityKillTimestamps.remove(pk.victimId);

        long now = System.currentTimeMillis();
        playerKillTimestamps.computeIfAbsent(pk.player.getUUID(), k -> Collections.synchronizedList(new ArrayList<>())).add(now);
        Map<UUID, CombatState> combats = activeCombats.get(pk.player.getUUID());
        if (combats != null) {
            combats.remove(pk.victimId);
        }
    }

    private static void updateCombatTracking(DamageSource src, LivingEntity victim, UUID victimId, float amount) {
        long now = System.currentTimeMillis();

        if (victim instanceof ServerPlayer playerVictim) {
            Map<UUID, CombatState> playerCombats = activeCombats.get(playerVictim.getUUID());
            if (playerCombats != null) {
                playerCombats.values().forEach(cs -> cs.flawless = false);
            }

            LivingEntity attacker = resolveLivingAttacker(src, victim);
            if (attacker != null) {
                activeCombats.computeIfAbsent(playerVictim.getUUID(), k -> new ConcurrentHashMap<>())
                    .compute(attacker.getUUID(), (k, v) -> {
                        if (v == null) {
                            CombatState cs = new CombatState(now, playerVictim.position());
                            cs.flawless = false;
                            return cs;
                        }
                        v.lastInteractionTime = now;
                        v.flawless = false;
                        return v;
                    });
            }
        }

        ServerPlayer player = resolvePlayerAttacker(src, victim);
        if (player != null) {
            activeCombats.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                .compute(victimId, (k, v) -> {
                    if (v == null) return new CombatState(now, player.position());
                    v.lastInteractionTime = now;
                    return v;
                });
        }
    }

    private static boolean checkLockedTarget(ServerPlayer player, LivingEntity victim) {
        Map<UUID, CombatState> playerCombats = activeCombats.get(player.getUUID());
        if (playerCombats == null) return false;
        CombatState state = playerCombats.get(victim.getUUID());
        if (state == null) return false;
        long now = System.currentTimeMillis();
        return now - state.firstInteractionTime >= LOCKED_TARGET_WINDOW_MS;
    }

    private static LivingEntity resolveLivingAttacker(DamageSource src, LivingEntity victim) {
        return ServerCombatAttribution.resolveLivingAttacker(src, victim, fireAttribution, FIRE_ATTRIBUTION_TIMEOUT_MS);
    }

    private static ServerPlayer resolvePlayerAttacker(DamageSource src, LivingEntity victim) {
        return ServerCombatAttribution.resolvePlayerAttacker(src, victim, fireAttribution, FIRE_ATTRIBUTION_TIMEOUT_MS);
    }

    private static void sendKillEffects(ServerPlayer player, int killType, int combo, int victimId, boolean hasHelmet, String victimName, boolean isVictimPlayer, float distance, float bonusMultiplier, float bonusScale) {
        double window = ServerData.get().getComboWindowSeconds();
        ServerPacketDispatcher.sendKillEffects(player, killType, combo, victimId, window, hasHelmet, victimName, isVictimPlayer, distance, bonusMultiplier, bonusScale);
    }

    private static boolean isObstructed(PendingKill pk) {
        if (pk.player == null || pk.victimPos == null) return false;

        Vec3 start = pk.player.getEyePosition();
        Vec3 end = pk.victimPos;

        BlockHitResult blockHit = pk.player.level().clip(new ClipContext(
            start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, pk.player
        ));

        return blockHit.getType() != HitResult.Type.MISS;
    }

    private static long calculateStreakCount(UUID victimId) {
        List<Long> victimKills = playerKillTimestamps.get(victimId);
        if (victimKills == null) victimKills = entityKillTimestamps.get(victimId);
        if (victimKills == null) return 0;

        long now = System.currentTimeMillis();
        return victimKills.stream().filter(time -> now - time <= 360000).count();
    }

    private static boolean checkVictimThreat(ServerPlayer player, LivingEntity victim) {
        if (victim instanceof Monster || victim instanceof ServerPlayer) return true;
        if (victim instanceof NeutralMob && victim instanceof Mob mob) return mob.getTarget() == player;
        return false;
    }

    private static boolean checkBackstab(ServerPlayer player, LivingEntity victim) {
        Vec3 toAttacker = player.position().subtract(victim.position()).normalize();
        Vec3 victimLook = victim.getViewVector(1.0F).normalize();
        return victimLook.dot(toAttacker) < -0.2;
    }

    /**
     * 战士判定: 玩家是否处于行走或疾跑状态(水平速度非零, 或按住移动键 zza/xxa 非零)。
     * 结合速度与移动输入, 避免击杀瞬间速度归零导致误判。
     */
    private static boolean isPlayerWalkingOrSprinting(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        net.minecraft.world.phys.Vec3 vel = player.getDeltaMovement();
        if (vel.x * vel.x + vel.z * vel.z > 1.0E-7) {
            return true;
        }
        try {
            if (entityZzaField == null) {
                entityZzaField = net.minecraft.world.entity.Entity.class.getDeclaredField("zza");
                entityXxaField = net.minecraft.world.entity.Entity.class.getDeclaredField("xxa");
                entityZzaField.setAccessible(true);
                entityXxaField.setAccessible(true);
            }
            float zza = entityZzaField.getFloat(player);
            float xxa = entityXxaField.getFloat(player);
            return zza != 0.0F || xxa != 0.0F;
        } catch (Exception e) {
            return false;
        }
    }

    private static java.lang.reflect.Field entityZzaField;
    private static java.lang.reflect.Field entityXxaField;

    /**
     * 戍卫判定: 击杀者是否处于 Conquest 据点内且该据点内有敌军(不同队伍玩家同在据点)。
     * Conquest 为可选模组, 反射调用 RoomCoreRuntimeManager.of(server).coreService().buildKilliconConquestSnapshot(roomId),
     * 用快照中的 activeCodesByPlayer(玩家→据点 codes)与 playerTeams(玩家→队伍)判定。
     */
    private static boolean isInGarrisonKillSituation(ServerPlayer killer) {
        if (killer == null || killer.server == null) {
            return false;
        }
        try {
            if (garrisonMgrOfMethod == null) {
                Class<?> mgrClass = Class.forName("org.mods.gd656conquest.server.room.entry.RoomCoreRuntimeManager");
                garrisonMgrOfMethod = mgrClass.getMethod("of", net.minecraft.server.MinecraftServer.class);
                garrisonCoreServiceMethod = mgrClass.getMethod("coreService");
                Class<?> coreServiceClass = Class.forName("org.mods.gd656conquest.server.room.core.RoomCoreService");
                garrisonFindRoomMethod = coreServiceClass.getMethod("findPlayerRoomId", java.util.UUID.class);
                garrisonSnapshotMethod = coreServiceClass.getMethod("buildKilliconConquestSnapshot", String.class);
                garrisonReady = true;
            }
            if (!garrisonReady) {
                return false;
            }
            Object mgr = garrisonMgrOfMethod.invoke(null, killer.server);
            if (mgr == null) {
                return false;
            }
            Object coreService = garrisonCoreServiceMethod.invoke(mgr);
            Object roomOpt = garrisonFindRoomMethod.invoke(coreService, killer.getUUID());
            if (!(roomOpt instanceof java.util.Optional<?> optional) || optional.isEmpty()) {
                return false;
            }
            Object snapshot = garrisonSnapshotMethod.invoke(coreService, optional.get());
            if (!(snapshot instanceof java.util.Map<?, ?> snapshotMap)) {
                return false;
            }
            Object activeByPlayer = snapshotMap.get("activeCodesByPlayer");
            Object teams = snapshotMap.get("playerTeams");
            if (!(activeByPlayer instanceof java.util.Map<?, ?> activeMap)
                    || !(teams instanceof java.util.Map<?, ?> teamMap)) {
                return false;
            }
            Object killerCodes = activeMap.get(killer.getUUID());
            if (!(killerCodes instanceof java.util.Set<?> codeSet) || codeSet.isEmpty()) {
                return false;
            }
            Object killerTeam = teamMap.get(killer.getUUID());
            if (killerTeam == null) {
                return false;
            }
            for (Object entry : activeMap.entrySet()) {
                java.util.Map.Entry<?, ?> e = (java.util.Map.Entry<?, ?>) entry;
                if (e.getKey() == null || e.getKey().equals(killer.getUUID())) {
                    continue;
                }
                Object otherTeam = teamMap.get(e.getKey());
                if (otherTeam == null || otherTeam.equals(killerTeam)) {
                    continue;
                }
                if (e.getValue() instanceof java.util.Set<?> otherCodes) {
                    for (Object c : otherCodes) {
                        if (codeSet.contains(c)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean garrisonReady = false;
    private static java.lang.reflect.Method garrisonMgrOfMethod;
    private static java.lang.reflect.Method garrisonCoreServiceMethod;
    private static java.lang.reflect.Method garrisonFindRoomMethod;
    private static java.lang.reflect.Method garrisonSnapshotMethod;
    private static java.lang.reflect.Method garrisonFindRunningMethod;

    /**
     * 急先锋判定: 玩家在 Conquest RUNNING 对局中的**小队**(阵营内 squad, 非整个阵营), 返回 roomId:squadLabel;
     * 不在对局/无小队返回 null。复用戍卫反射链, 用 buildKilliconConquestSnapshot 非空验证对局已开始(RUNNING)。
     */
    private static String resolveFirstKillTeamKey(ServerPlayer killer) {
        if (killer == null || killer.server == null) {
            return null;
        }
        try {
            if (garrisonMgrOfMethod == null) {
                Class<?> mgrClass = Class.forName("org.mods.gd656conquest.server.room.entry.RoomCoreRuntimeManager");
                garrisonMgrOfMethod = mgrClass.getMethod("of", net.minecraft.server.MinecraftServer.class);
                garrisonCoreServiceMethod = mgrClass.getMethod("coreService");
                Class<?> coreServiceClass = Class.forName("org.mods.gd656conquest.server.room.core.RoomCoreService");
                garrisonFindRoomMethod = coreServiceClass.getMethod("findPlayerRoomId", java.util.UUID.class);
                garrisonSnapshotMethod = coreServiceClass.getMethod("buildKilliconConquestSnapshot", String.class);
                garrisonFindRunningMethod = coreServiceClass.getMethod("findRunningState", String.class);
                garrisonReady = true;
            }
            if (!garrisonReady) {
                return null;
            }
            Object mgr = garrisonMgrOfMethod.invoke(null, killer.server);
            Object coreService = garrisonCoreServiceMethod.invoke(mgr);
            Object roomOpt = garrisonFindRoomMethod.invoke(coreService, killer.getUUID());
            if (!(roomOpt instanceof java.util.Optional<?> optional) || optional.isEmpty()) {
                return null;
            }
            String roomId = (String) optional.get();
            // 对局未开始(RUNNING 之外)时快照为空
            Object snapshot = garrisonSnapshotMethod.invoke(coreService, roomId);
            if (!(snapshot instanceof java.util.Map<?, ?> snapshotMap) || snapshotMap.isEmpty()) {
                return null;
            }
            // 玩家小队: findRunningState(roomId).getSquadLabelsByPlayer()
            Object runningOpt = garrisonFindRunningMethod.invoke(coreService, roomId);
            if (!(runningOpt instanceof java.util.Optional<?> runningOptional) || runningOptional.isEmpty()) {
                return null;
            }
            Object runningState = runningOptional.get();
            Object squadMap = runningState.getClass().getMethod("getSquadLabelsByPlayer").invoke(runningState);
            if (squadMap instanceof java.util.Map<?, ?> squadLabels) {
                Object label = squadLabels.get(killer.getUUID());
                if (label instanceof String squadLabel && !squadLabel.isBlank()) {
                    return roomId + ":" + squadLabel;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 炮手判定: 玩家在载具内且**不在主驾驶位**(基于 SBW/YWZJ 数据):
     * - SBW: `VehicleEntity.getSeatIndex(player)`(座位 0 = 主驾驶) != 0
     * - YWZJ: `player != vehicle.getDriver()`(getDriver = 主驾驶)
     */
    private static boolean isGunnerSeat(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        net.minecraft.world.entity.Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return false;
        }
        // SBW: getSeatIndex(player) != 0
        try {
            if (sbwSeatIndexMethod == null) {
                Class<?> sbwVehicleClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
                sbwSeatIndexMethod = sbwVehicleClass.getMethod("getSeatIndex", net.minecraft.world.entity.Entity.class);
            }
            if (sbwSeatIndexMethod != null && sbwSeatIndexMethod.getDeclaringClass().isInstance(vehicle)) {
                Object idx = sbwSeatIndexMethod.invoke(vehicle, player);
                if (idx instanceof Integer seatIndex) {
                    return seatIndex != 0;
                }
            }
        } catch (Exception ignored) {
        }
        // YWZJ: 玩家 != getDriver()(主驾驶)
        try {
            if (ywzjVehicleClass == null) {
                ywzjVehicleClass = Class.forName("org.ywzj.vehicle.entity.vehicle.AbstractVehicle");
                ywzjGetDriverMethod = ywzjVehicleClass.getMethod("getDriver");
            }
            if (ywzjVehicleClass != null && ywzjVehicleClass.isInstance(vehicle)) {
                Object driver = ywzjGetDriverMethod.invoke(vehicle);
                return driver != player;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static java.lang.reflect.Method sbwSeatIndexMethod;
    private static java.lang.reflect.Method ywzjGetDriverMethod;

    /**
     * 载具判定(路霸): SBW VehicleEntity 或 YWZJ AbstractVehicle 子类(反射 instanceof, 可选模组)。
     */
    private static boolean isVehicleEntity(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return false;
        }
        try {
            if (sbwVehicleClass == null) {
                sbwVehicleClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
                ywzjVehicleClass = Class.forName("org.ywzj.vehicle.entity.vehicle.AbstractVehicle");
            }
            return (sbwVehicleClass != null && sbwVehicleClass.isInstance(entity))
                    || (ywzjVehicleClass != null && ywzjVehicleClass.isInstance(entity));
        } catch (Exception e) {
            return false;
        }
    }

    /** 载具驾驶员(路霸): 优先控制者, 否则遍历乘客找 ServerPlayer。 */
    private static ServerPlayer resolveVehicleDriver(net.minecraft.world.entity.Entity vehicle) {
        if (vehicle == null) {
            return null;
        }
        net.minecraft.world.entity.Entity controlling = vehicle.getControllingPassenger();
        if (controlling instanceof ServerPlayer player) {
            return player;
        }
        for (net.minecraft.world.entity.Entity passenger : vehicle.getIndirectPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                return player;
            }
        }
        return null;
    }

    /**
     * 飞行调度员: 受害者死亡瞬间正搭乘**存活**的空中载具。
     * 载具必须同时满足: 是空中载具(SBW/YWZJ 类型判定)且未被摧毁。
     * 摧毁判定(SBW health≤0 / isWreck, YWZJ isDestroyed): 载具与乘客同时被毁时,
     * 死亡瞬间乘客尚未脱离载具, 仅靠 getVehicle() 会误判, 需排除已摧毁载具。
     */
    private static boolean isAliveAirVehicle(net.minecraft.world.entity.Entity vehicle) {
        if (vehicle == null
                || !org.mods.gd656killicon.server.logic.core.BonusEngine.isAircraftEntity(vehicle)) {
            return false;
        }
        return !isDestroyedVehicle(vehicle);
    }

    /** 载具是否已摧毁: SBW health≤0 或 isWreck() 为 true; YWZJ isDestroyed() 为 true。 */
    private static boolean isDestroyedVehicle(net.minecraft.world.entity.Entity vehicle) {
        try {
            if (sbwVehicleClass == null) {
                sbwVehicleClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            }
            if (sbwVehicleClass != null && sbwVehicleClass.isInstance(vehicle)) {
                // SBW: getHealth() ≤ 0
                try {
                    java.lang.reflect.Method healthMethod = vehicle.getClass().getMethod("getHealth");
                    Object health = healthMethod.invoke(vehicle);
                    if (health instanceof Number n && n.floatValue() <= 0.0f) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
                // SBW: isWreck()/getIsWreck() 为 true(残骸; Kotlin 属性 getter 名可能不同)
                for (String wreckMethodName : new String[]{"isWreck", "getIsWreck"}) {
                    try {
                        java.lang.reflect.Method wreckMethod = vehicle.getClass().getMethod(wreckMethodName);
                        Object wreck = wreckMethod.invoke(vehicle);
                        if (wreck instanceof Boolean b && b) {
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            if (ywzjVehicleClass == null) {
                ywzjVehicleClass = Class.forName("org.ywzj.vehicle.entity.vehicle.AbstractVehicle");
            }
            if (ywzjVehicleClass != null && ywzjVehicleClass.isInstance(vehicle)) {
                java.lang.reflect.Method destroyedMethod = vehicle.getClass().getMethod("isDestroyed");
                Object destroyed = destroyedMethod.invoke(vehicle);
                if (destroyed instanceof Boolean b && b) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static Class<?> sbwVehicleClass;
    private static Class<?> ywzjVehicleClass;

    /**
     * 受害者是否带有 LR 战术工坊的致盲效果(lrtactical:blinded, 闪光弹致盲)。
     * LR 为可选模组, 用 MobEffect 注册名判断, 避免编译期依赖。
     */
    private static boolean isLrBlinded(net.minecraft.world.entity.LivingEntity victim) {
        try {
            net.minecraft.world.effect.MobEffect blinded =
                    net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lrtactical", "blinded"));
            return blinded != null && victim.hasEffect(blinded);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 刽子手武器判定: 原版近战武器(剑/斧)或 LR 战术工坊(lrtactical)武器。
     * LR 为可选模组, 用注册名命名空间判断, 避免编译期依赖。
     */
    private static boolean isMeleeWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof net.minecraft.world.item.SwordItem || item instanceof net.minecraft.world.item.AxeItem) {
            return true;
        }
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        return key != null && "lrtactical".equals(key.getNamespace());
    }

    private static boolean checkJusticeFromAbove(ServerPlayer player, LivingEntity victim, boolean isGliding) {
        boolean isFalling = player.getDeltaMovement().y < -0.1 && !player.onGround() && !player.getAbilities().flying;
        boolean isMovingCleanly = !player.isInWater() && !player.onClimbable();
        double heightDiff = player.getY() - victim.getY();
        return !isGliding && isFalling && isMovingCleanly && heightDiff > 2.0;
    }

    private static boolean isConquestTacticalGadget(LivingEntity entity) {
        if (entity == null) return false;
        String className = entity.getClass().getName();
        return CONQUEST_SQUAD_BEACON_CLASS.equals(className)
                || CONQUEST_GROUND_SENSOR_CLASS.equals(className)
                || CONQUEST_MEDICAL_BOX_CLASS.equals(className)
                || CONQUEST_AMMO_BOX_CLASS.equals(className);
    }
}
