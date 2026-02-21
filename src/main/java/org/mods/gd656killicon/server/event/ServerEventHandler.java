package org.mods.gd656killicon.server.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.DamageSoundPacket;
import org.mods.gd656killicon.network.packet.KillIconPacket;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.data.PlayerDataManager;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.util.ServerLog;

import java.util.*;
import java.util.concurrent.*;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID)
public class ServerEventHandler {
    private static final Map<UUID, Float> lastDamage = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastDamageType = new ConcurrentHashMap<>();
    private static final Map<UUID, List<DamageRecord>> damageHistory = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, Long>> killHistory = new ConcurrentHashMap<>(); // victimId -> (attackerId -> timestamp)
    private static final Map<UUID, Map<UUID, CombatState>> activeCombats = new ConcurrentHashMap<>(); // playerUUID -> (targetUUID -> CombatState)
    private static final Map<UUID, Integer> explosionKillCounter = new ConcurrentHashMap<>(); // playerUUID -> count
    private static final Map<UUID, Integer> consecutiveDeaths = new ConcurrentHashMap<>(); // playerUUID -> deathCount
    private static final Map<UUID, List<Long>> playerKillTimestamps = new ConcurrentHashMap<>(); // player/entity UUID -> list of kill timestamps
    private static final Map<UUID, List<Long>> entityKillTimestamps = new ConcurrentHashMap<>(); // monster/mob UUID -> list of kill timestamps
    private static final Map<UUID, Map<String, Long>> teamKillHistory = new ConcurrentHashMap<>(); // attackerId -> (teamName -> timestamp)
    private static final Map<UUID, Integer> lifeKillCount = new ConcurrentHashMap<>(); // playerUUID -> killCount (resets on death)
    private static final Map<UUID, Long> lastItemSwitchTime = new ConcurrentHashMap<>(); // playerUUID -> timestamp
    private static final Map<UUID, Integer> lastSelectedSlot = new ConcurrentHashMap<>(); // playerUUID -> slotIndex
    private static final Map<UUID, Integer> consecutiveAssists = new ConcurrentHashMap<>(); // playerUUID -> assistCount

    private static final List<PendingKill> pendingKills = new ArrayList<>();
    private static ScheduledExecutorService scoreboardRefreshExecutor;

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_EXPLOSION = 1;
    private static final int TYPE_HEADSHOT = 2;
    private static final int TYPE_CRIT = 3;

    // ================================================================================================================
    // Event Handlers
    // ================================================================================================================

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        ServerLog.info("Initializing server data...");
        ServerData.get().init(event.getServer());
        PlayerDataManager.get().init(event.getServer());
        ServerCore.TACZ.init();
        ServerCore.YWZJ_VEHICLE.init();
        ServerCore.SUPERB_WARFARE.init();
        startScoreboardRefreshTask();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        ServerLog.info("Saving server data...");
        ServerData.get().saveAll();
        PlayerDataManager.get().shutdown();
        stopScoreboardRefreshTask();
    }

    private static void startScoreboardRefreshTask() {
        if (scoreboardRefreshExecutor != null) {
            scoreboardRefreshExecutor.shutdownNow();
        }

        scoreboardRefreshExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Scoreboard-Refresh");
            thread.setDaemon(true);
            return thread;
        });

        scoreboardRefreshExecutor.scheduleAtFixedRate(
                () -> {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        ServerData.get().refreshScoreboard(server);
                    }
                },
                1, // Initial delay: 1 minute
                1, // Period: 1 minute
                TimeUnit.MINUTES
        );
    }

    private static void stopScoreboardRefreshTask() {
        if (scoreboardRefreshExecutor != null) {
            scoreboardRefreshExecutor.shutdown();
            try {
                if (!scoreboardRefreshExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scoreboardRefreshExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scoreboardRefreshExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // Bonus Batching
        ServerCore.BONUS.tick(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());
        
        // Integration Tick
        ServerCore.TACZ.tick();
        ServerCore.SUPERB_WARFARE.tick();
        ServerCore.YWZJ_VEHICLE.tick();
        
        // Reset explosion counter every tick
        explosionKillCounter.clear();

        // Combat State Cleanup
        long now = System.currentTimeMillis();
        activeCombats.values().forEach(map -> map.values().removeIf(cs -> now - cs.lastInteractionTime > 30000));
        activeCombats.values().removeIf(Map::isEmpty);

        // Team Kill History Cleanup
        teamKillHistory.values().forEach(map -> map.values().removeIf(timestamp -> now - timestamp > 60000));
        teamKillHistory.values().removeIf(Map::isEmpty);

        // Item Switch History Cleanup
        lastItemSwitchTime.values().removeIf(timestamp -> now - timestamp > 10000);

        // Damage History Cleanup (Prevent memory leak for long-lived entities/players)
        damageHistory.values().forEach(records -> {
            synchronized (records) {
                records.removeIf(r -> now - r.timestamp > 120000); // 2 minutes
            }
        });
        damageHistory.values().removeIf(List::isEmpty);

        // Process delayed kills
        processPendingKills();
        
        // Tacz & SuperbWarfare Integration Cleanup (Must be after processing kills)
        ServerCore.TACZ.tick();
        ServerCore.SUPERB_WARFARE.tick();
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataManager.get().updateLastLoginName(player.getUUID(), player.getScoreboardName());
            ServerData.get().syncScoreToPlayer(player);
            // Initialize selected slot
            lastSelectedSlot.put(player.getUUID(), player.getInventory().selected);
        }
    }

    @SubscribeEvent
    public static void onItemSwitch(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer player && event.getSlot().getType() == net.minecraft.world.entity.EquipmentSlot.Type.HAND) {
            int currentSlot = player.getInventory().selected;
            Integer lastSlot = lastSelectedSlot.get(player.getUUID());
            lastSelectedSlot.put(player.getUUID(), currentSlot);

            // If it's the first time we see this player (or slot is null), just initialize and return
            if (lastSlot == null) {
                return;
            }

            // Check if slot has changed
            if (lastSlot != currentSlot) {
                // Slot changed -> valid switch
                lastItemSwitchTime.put(player.getUUID(), System.currentTimeMillis());
            } else {
                // Slot did NOT change (could be durability change, or swapping same type item)
                // In this case, we ONLY consider it a switch if the item type is different.
                // This filters out durability changes (same item type).
                if (!net.minecraft.world.item.ItemStack.isSameItem(event.getFrom(), event.getTo())) {
                    lastItemSwitchTime.put(player.getUUID(), System.currentTimeMillis());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0) return;

        DamageSource src = event.getSource();
        LivingEntity victim = event.getEntity();
        UUID victimId = victim.getUUID();
        float amt = event.getAmount();

        // Final condition check: Do not award bonuses for self-damaging
        if (src.getEntity() instanceof ServerPlayer player && player.getUUID().equals(victimId)) {
            return;
        }
        
        updateCombatTracking(src, victim, victimId, amt);

        // Record damage for ANY living entity attacker (required for Savior bonus)
        if (src.getEntity() instanceof LivingEntity attacker) {
            float effectiveAmt = Math.min(amt, victim.getHealth());
            int roundedAmt = Math.round(effectiveAmt);
            if (roundedAmt > 0) {
                recordDamage(victimId, attacker.getUUID(), roundedAmt);
            }
        }

        if (!(src.getEntity() instanceof ServerPlayer player)) return;
        
        lastDamage.put(victimId, amt);
        ServerCore.CRIT.recordCrit(player, victimId);

        int type = determineDamageType(player, victimId, src);
        lastDamageType.put(victimId, type);
        
        float effectiveAmt = Math.min(amt, victim.getHealth());
        int roundedAmt = Math.round(effectiveAmt);

        if (roundedAmt > 0) {
            addDamageBonus(player, type, roundedAmt);
        }

        if (amt < victim.getHealth()) {
            NetworkHandler.sendToPlayer(new DamageSoundPacket(), player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        LivingEntity victim = event.getEntity();
        UUID victimId = victim.getUUID();
        
        DamageSource src = event.getSource();
        
        // Track consecutive deaths for players (Brave's Return)
            if (victim instanceof ServerPlayer player) {
                UUID playerId = player.getUUID();
                consecutiveDeaths.merge(playerId, 1, Integer::sum);
                
                // Update death count
                ServerData.get().addDeath(player, 1);
                // 增加助攻统计逻辑
                awardAssists(victimId, victim.getId());
                
                String killerName = "";
                if (src.getEntity() instanceof ServerPlayer killer) {
                    killerName = killer.getScoreboardName();
                }

                // 发送死亡数据包到客户端
                org.mods.gd656killicon.network.NetworkHandler.sendToPlayer(
                    new org.mods.gd656killicon.network.packet.DeathPacket(
                        player.getScoreboardName(), 
                        src.getMsgId(),
                        killerName
                    ), 
                    player
                );
            }
        
        // Record kill for vengeance tracking
        if (src.getEntity() instanceof LivingEntity attacker) {
            killHistory.computeIfAbsent(victimId, k -> new ConcurrentHashMap<>())
                       .put(attacker.getUUID(), System.currentTimeMillis());

            // Track team kill history for "Leave it to me"
            if (victim instanceof ServerPlayer victimPlayer && victimPlayer.getTeam() != null) {
                teamKillHistory.computeIfAbsent(attacker.getUUID(), k -> new ConcurrentHashMap<>())
                              .put(victimPlayer.getTeam().getName(), System.currentTimeMillis());
            }

            // Update attacker's streak if the attacker is a monster/mob (for Interrupted Streak to work on entities)
            if (attacker instanceof Mob mob && !(attacker instanceof ServerPlayer)) {
                long now = System.currentTimeMillis();
                entityKillTimestamps.computeIfAbsent(mob.getUUID(), k -> Collections.synchronizedList(new ArrayList<>())).add(now);
            }
        }
        
        if (src.getEntity() instanceof ServerPlayer player) {
            handlePlayerKill(player, victim, src);
        } else {
            boolean hasHelmet = !victim.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty();
            processAssist(victimId, victim.getId(), hasHelmet, victim instanceof net.minecraft.world.entity.player.Player ? victim.getScoreboardName() : victim.getDisplayName().getString(), victim instanceof net.minecraft.world.entity.player.Player);
        }

        // Clear streak on death (Done after handlePlayerKill to allow capturing streak count)
        playerKillTimestamps.remove(victimId);
        entityKillTimestamps.remove(victimId);
        lifeKillCount.remove(victimId);
        consecutiveAssists.remove(victimId);
        
        cleanupVictimData(victimId);
    }

    // ================================================================================================================
    // Internal Logic
    // ================================================================================================================

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

        // Group by (player, sourceEntityId, tick) to detect simultaneous kills
        Map<String, List<PendingKill>> groups = new HashMap<>();
        for (PendingKill pk : readyKills) {
            if (pk.player == null || pk.player.isRemoved()) continue;
            
            // Generate a grouping key
            String key = pk.player.getUUID().toString() + "_" + pk.sourceEntityId + "_" + pk.tick;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(pk);
        }

        for (List<PendingKill> group : groups.values()) {
            // Process each kill individually for standard bonuses
            for (PendingKill pk : group) {
                processKill(pk);
            }

            // Award "One Bullet Multi Kill" bonus if applicable
            if (group.size() >= 2) {
                PendingKill first = group.get(0);
                
                // Conditions: 
                // 1. Not an explosion (already handled as shockwave/explosion bonus)
                // 2. Either a projectile OR a TACZ gun kill (to exclude sweep attacks)
                boolean hasExplosion = group.stream().anyMatch(pk -> pk.damageType == TYPE_EXPLOSION);
                boolean isProjectile = first.sourceEntityId != -1 && first.sourceEntityId != first.player.getId();
                
                if (!hasExplosion && (isProjectile || first.isGun)) {
                    int count = Math.min(group.size(), 8);
                    ServerCore.BONUS.add(first.player, BonusType.ONE_BULLET_MULTI_KILL, (float) count, String.valueOf(count));
                }
            }
        }
    }

    private static int determineDamageType(ServerPlayer player, UUID victimId, DamageSource src) {
        if (ServerCore.TACZ.isHeadshotDamage(victimId) || ServerCore.SUPERB_WARFARE.isHeadshotDamage(victimId)) return TYPE_HEADSHOT;
        if (src.is(DamageTypeTags.IS_EXPLOSION)) return TYPE_EXPLOSION;
        if (ServerCore.CRIT.isRecentCrit(player.getUUID(), victimId)) return TYPE_CRIT;
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
        ServerCore.BONUS.add(player, bonusType, (float) amount, "");
    }

    private static void handlePlayerKill(ServerPlayer player, LivingEntity victim, DamageSource src) {
        UUID victimId = victim.getUUID();
        int type = lastDamageType.getOrDefault(victimId, TYPE_NORMAL);
        
        ServerData.get().addKill(player, 1);
        
        // Fallback check for non-damage source explosion
        if (type == TYPE_NORMAL && (src.is(DamageTypeTags.IS_EXPLOSION) || src.getMsgId().contains("explosion"))) {
            type = TYPE_EXPLOSION;
        }

        int sourceId = src.getDirectEntity() != null ? src.getDirectEntity().getId() : -1;
        long tick = player.level().getGameTime();
        boolean isGun = ServerCore.TACZ.isGunKill(victimId) || ServerCore.SUPERB_WARFARE.isGunKill(victimId);

        // Reset POTATO_AIM counter on kill
        consecutiveAssists.put(player.getUUID(), 0);

        // Retrieve and remove Flawless state
        boolean isFlawless = false;
        Map<UUID, CombatState> combats = activeCombats.get(player.getUUID());
        if (combats != null) {
            CombatState cs = combats.remove(victimId);
            if (cs != null) {
                isFlawless = cs.flawless;
            }
        }

        double distanceDouble = player.distanceTo(victim);
        float distanceFloat = (float) distanceDouble;
        pendingKills.add(new PendingKill(
            player, 
            victim, 
            victim instanceof net.minecraft.world.entity.player.Player ? victim.getScoreboardName() : victim.getDisplayName().getString(), 
            ServerCore.COMBO.recordKill(player),
            type,
            victim.getMaxHealth(),
            distanceFloat,
            sourceId,
            tick,
            isGun,
            isFlawless
        ));
        
        // Send kill distance packet to client
        org.mods.gd656killicon.network.NetworkHandler.sendToPlayer(
            new org.mods.gd656killicon.network.packet.KillDistancePacket(distanceDouble), 
            player
        );
    }

    private static void cleanupVictimData(UUID victimId) {
        lastDamage.remove(victimId);
        lastDamageType.remove(victimId);
        damageHistory.remove(victimId);
        activeCombats.values().forEach(map -> map.remove(victimId));
        // teamKillHistory should NOT be removed here because the victim might be the target of a "Leave it to me" vengeance kill
        // It will be cleaned up by time-based expiration in onTick or when the bonus is triggered
    }

    private static void awardAssists(UUID victimId, int victimIdInt) {
        List<DamageRecord> records = damageHistory.get(victimId);
        if (records == null || records.isEmpty()) return;

        long now = System.currentTimeMillis();
        Map<UUID, Integer> playerDamages = new HashMap<>();
        
        synchronized (records) {
            long timeout = ServerData.get().getAssistTimeoutMs();
            for (DamageRecord r : records) {
                if (now - r.timestamp <= timeout) {
                    playerDamages.merge(r.attackerId, r.amount, Integer::sum);
                }
            }
        }

        playerDamages.forEach((playerId, totalDamage) -> {
            if (totalDamage > 0) {
                ServerPlayer player = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                if (player != null && !player.getUUID().equals(victimId)) {
                    ServerData.get().addAssist(player, 1);
                }
            }
        });
    }

    private static void processAssist(UUID victimId, int victimIdInt, boolean hasHelmet, String victimName, boolean isVictimPlayer) {
        List<DamageRecord> records = damageHistory.get(victimId);
        if (records == null || records.isEmpty()) return;

        // 如果是生物，尝试使用翻译键作为名称
        String resolvedName = victimName;
        if (!isVictimPlayer) {
             net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
             if (server != null) {
                 for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                     net.minecraft.world.entity.Entity entity = level.getEntity(victimId);
                     if (entity instanceof LivingEntity living) {
                         resolvedName = living.getType().getDescriptionId();
                         break;
                     }
                 }
             }
        }
        final String finalVictimName = resolvedName;

        long now = System.currentTimeMillis();
        Map<UUID, Integer> playerDamages = new HashMap<>();
        
        synchronized (records) {
            long lastTime = records.stream().mapToLong(r -> r.timestamp).max().orElse(0);
            long timeout = ServerData.get().getAssistTimeoutMs();
            if (now - lastTime > timeout) return; // Victim hasn't taken damage within timeout

            for (DamageRecord r : records) {
                if (now - r.timestamp <= timeout) { // Damage within timeout
                    playerDamages.merge(r.attackerId, r.amount, Integer::sum);
                }
            }
        }

        playerDamages.forEach((playerId, totalDamage) -> {
            if (totalDamage > 0) {
                ServerPlayer player = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    ServerCore.BONUS.add(player, BonusType.ASSIST, (float) totalDamage, "");
                    sendKillEffects(player, KillType.ASSIST, 0, victimIdInt, hasHelmet, finalVictimName, isVictimPlayer);
                    
                    // Add assist count
                    ServerData.get().addAssist(player, 1);

                    // POTATO_AIM logic: 3 consecutive assists without any kill
                    int count = consecutiveAssists.merge(playerId, 1, Integer::sum);
                    if (count >= 3) {
                        ServerCore.BONUS.add(player, BonusType.POTATO_AIM, 1.0f, ""); // Multiplier is 1.0, score is defined in ServerData
                        consecutiveAssists.put(playerId, 0);
                    }
                }
            }
        });
    }

    private static void processKill(PendingKill pk) {
        if (pk.player.getUUID().equals(pk.victimId)) return;

        // 如果是生物，使用翻译键作为名称
        String finalVictimName = pk.victimName;
        if (!pk.isVictimPlayer) {
             net.minecraft.server.level.ServerLevel level = pk.player.serverLevel();
             net.minecraft.world.entity.Entity entity = level.getEntity(pk.victimId);
             if (entity instanceof LivingEntity living) {
                 finalVictimName = living.getType().getDescriptionId();
             }
        }

        // 1. Determine and Award Base Kill Bonus
        int killType = determineKillType(pk);
        int bonusType = mapKillTypeToBonus(killType, pk.damageType);
        ServerCore.BONUS.add(pk.player, bonusType, pk.maxHealth, "", pk.victimIdInt);

        // 2. Award Conditional Bonuses
        awardSpecialKills(pk);
        awardPositionalKills(pk);
        awardStatusKills(pk);
        awardStreakKills(pk);

        // 3. Reset and Update States
        updatePostKillStates(pk);

        // 4. Visual/Audio Effects
        sendKillEffects(pk.player, killType, pk.combo, pk.victimIdInt, pk.hasHelmet, finalVictimName, pk.isVictimPlayer);
    }

    private static int determineKillType(PendingKill pk) {
        if (ServerCore.TACZ.isHeadshotKill(pk.victimId) || ServerCore.SUPERB_WARFARE.isHeadshotKill(pk.victimId) || pk.damageType == TYPE_HEADSHOT) return KillType.HEADSHOT;
        if (pk.damageType == TYPE_EXPLOSION) return KillType.EXPLOSION;
        if (ServerCore.CRIT.consumeCrit(pk.player.getUUID(), pk.victimId) || pk.damageType == TYPE_CRIT) return KillType.CRIT;
        return KillType.NORMAL;
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
            if (count == 5) ServerCore.BONUS.add(pk.player, BonusType.SHOCKWAVE, 1.0f, "");
        }
        if (ServerCore.TACZ.isLastBulletKill(pk.victimId)) {
            ServerCore.BONUS.add(pk.player, BonusType.LAST_BULLET_KILL, 1.0f, "");
        }
        if (pk.isVictimThreat && pk.isFlawless) {
            ServerCore.BONUS.add(pk.player, BonusType.EFFORTLESS_KILL, 1.0f, "");
        }
        
        // Quick Switch
        Long switchTime = lastItemSwitchTime.get(pk.player.getUUID());
        if (switchTime != null && System.currentTimeMillis() - switchTime <= 3000) {
            ServerCore.BONUS.add(pk.player, BonusType.QUICK_SWITCH, 1.0f, "");
            lastItemSwitchTime.remove(pk.player.getUUID());
        }
    }

    private static void awardPositionalKills(PendingKill pk) {
        if (pk.distance > 20.0f) {
            ServerCore.BONUS.add(pk.player, BonusType.KILL_LONG_DISTANCE, pk.distance, String.valueOf((int) pk.distance));
        }
        if (isObstructed(pk)) {
            ServerCore.BONUS.add(pk.player, BonusType.KILL_INVISIBLE, 1.0f, "");
        }
        if (pk.isGliding) {
            ServerCore.BONUS.add(pk.player, BonusType.ABSOLUTE_AIR_CONTROL, 1.0f, "");
        } else if (pk.isJusticeFromAbove) {
            ServerCore.BONUS.add(pk.player, BonusType.JUSTICE_FROM_ABOVE, 1.0f, "");
        }
        if (pk.isBackstab) {
            ServerCore.BONUS.add(pk.player, pk.distance < 2.0f ? BonusType.BACKSTAB_MELEE_KILL : BonusType.BACKSTAB_KILL, 1.0f, "");
        }
    }

    private static void awardStatusKills(PendingKill pk) {
        if (pk.player.getHealth() <= 4.0f) {
            ServerCore.BONUS.add(pk.player, BonusType.DESPERATE_COUNTERATTACK, 1.0f, "");
        }
        
        // Blind Kill
        if (checkBlinded(pk.player)) {
            ServerCore.BONUS.add(pk.player, BonusType.BLIND_KILL, 1.0f, "");
        }

        // Seize Opportunity (Victim Blinded)
        if (pk.isVictimBlinded) {
            ServerCore.BONUS.add(pk.player, BonusType.SEIZE_OPPORTUNITY, 1.0f, "");
        }

        // Buff/Debuff
        awardBuffDebuffKills(pk.player);
    }

    private static void awardStreakKills(PendingKill pk) {
        if (pk.combo > 1) {
            ServerCore.BONUS.add(pk.player, BonusType.KILL_COMBO, (float) Math.min(pk.combo, 4), String.valueOf(pk.combo));
        }

        // Brave's Return
        int deathCount = consecutiveDeaths.getOrDefault(pk.player.getUUID(), 0);
        if (deathCount >= 3) ServerCore.BONUS.add(pk.player, BonusType.BRAVE_RETURN, 1.0f, "");
        consecutiveDeaths.put(pk.player.getUUID(), 0);

        // Unified Streak Bonuses (Berserker, Bloodthirsty, etc.)
        int lifeKills = lifeKillCount.merge(pk.player.getUUID(), 1, Integer::sum);
        if (lifeKills == 3) ServerCore.BONUS.add(pk.player, BonusType.BERSERKER, 1.0f, "");
        else if (lifeKills == 5) ServerCore.BONUS.add(pk.player, BonusType.BLOODTHIRSTY, 1.0f, "");
        else if (lifeKills == 10) ServerCore.BONUS.add(pk.player, BonusType.MERCILESS, 1.0f, "");
        else if (lifeKills == 15) ServerCore.BONUS.add(pk.player, BonusType.VALIANT, 1.0f, "");
        else if (lifeKills == 20) ServerCore.BONUS.add(pk.player, BonusType.FIERCE, 1.0f, "");
        else if (lifeKills == 25) ServerCore.BONUS.add(pk.player, BonusType.SAVAGE, 1.0f, "");
        else if (lifeKills == 30) ServerCore.BONUS.add(pk.player, BonusType.PURGE, 1.0f, "");

        // Vengeance
        Map<UUID, Long> history = killHistory.get(pk.player.getUUID());
        if (history != null && history.containsKey(pk.victimId)) {
            ServerCore.BONUS.add(pk.player, BonusType.AVENGE, 1.0f, "");
            history.remove(pk.victimId);
        }

        // Interrupted Streak
        if (pk.streakCount >= 5) {
            ServerCore.BONUS.add(pk.player, BonusType.INTERRUPTED_STREAK, (float) pk.streakCount, String.valueOf(pk.streakCount));
        }

        // Leave it to me (Team Vengeance)
        if (pk.player.getTeam() != null) {
            Map<String, Long> teamHistory = teamKillHistory.get(pk.victimId);
            if (teamHistory != null) {
                Long timestamp = teamHistory.get(pk.player.getTeam().getName());
                if (timestamp != null && System.currentTimeMillis() - timestamp <= 60000) {
                    ServerCore.BONUS.add(pk.player, BonusType.LEAVE_IT_TO_ME, 1.0f, "");
                    teamKillHistory.remove(pk.victimId);
                }
            }
        }

        // Savior (Team Rescue)
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
                                r.attackerId.equals(pk.victimId) && 
                                now - r.timestamp <= 5000 // 5 seconds window
                            );
                            if (saved) {
                                ServerCore.BONUS.add(pk.player, BonusType.SAVIOR, 1.0f, "");
                                break; // Trigger once per kill
                            }
                        }
                    }
                }
            }
        }

        // Slay the Leader
        if (ServerData.get().isTopScorer(pk.victimId)) {
            ServerCore.BONUS.add(pk.player, BonusType.SLAY_THE_LEADER, 1.0f, "");
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
            ServerCore.BONUS.add(player, BonusType.BOTH_BUFF_DEBUFF_KILL, 1.0f, "");
        } else if (hasPositive) {
            ServerCore.BONUS.add(player, BonusType.BUFF_KILL, 1.0f, "");
        } else if (hasNegativeExcludingSpecial) {
            ServerCore.BONUS.add(player, BonusType.DEBUFF_KILL, 1.0f, "");
        }
    }

    private static boolean checkBlinded(LivingEntity entity) {
        if (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.CONFUSION) || entity.hasEffect(MobEffects.DARKNESS)) return true;
        try {
            net.minecraft.world.effect.MobEffect blinded = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lrtactical", "blinded"));
            return blinded != null && entity.hasEffect(blinded);
        } catch (Exception ignored) {}
        return false;
    }

    private static void updatePostKillStates(PendingKill pk) {
        playerKillTimestamps.remove(pk.victimId);
        entityKillTimestamps.remove(pk.victimId);
        
        long now = System.currentTimeMillis();
        playerKillTimestamps.computeIfAbsent(pk.player.getUUID(), k -> Collections.synchronizedList(new ArrayList<>())).add(now);
    }

    private static void updateCombatTracking(DamageSource src, LivingEntity victim, UUID victimId, float amount) {
        long now = System.currentTimeMillis();

        // Victim handling
        if (victim instanceof ServerPlayer playerVictim) {
            Map<UUID, CombatState> playerCombats = activeCombats.get(playerVictim.getUUID());
            if (playerCombats != null) {
                playerCombats.values().forEach(cs -> cs.flawless = false);
            }
            
            if (src.getEntity() instanceof LivingEntity attacker) {
                activeCombats.computeIfAbsent(playerVictim.getUUID(), k -> new ConcurrentHashMap<>())
                             .compute(attacker.getUUID(), (k, v) -> {
                                 if (v == null) {
                                     CombatState cs = new CombatState(now);
                                     cs.flawless = false;
                                     return cs;
                                 }
                                 v.lastInteractionTime = now;
                                 v.flawless = false;
                                 return v;
                             });
            }
        }

        // Attacker handling
        if (src.getEntity() instanceof ServerPlayer player) {
            activeCombats.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                         .compute(victimId, (k, v) -> {
                             if (v == null) return new CombatState(now);
                             v.lastInteractionTime = now;
                             return v;
                         });
        }
    }

    private static void sendKillEffects(ServerPlayer player, int killType, int combo, int victimId, boolean hasHelmet, String victimName, boolean isVictimPlayer) {
        double window = ServerData.get().getComboWindowSeconds();
        
        // 确保使用翻译后的名称（如果传入的是翻译键）
        // 这里不需要做额外处理，因为上层调用已经处理好了 finalVictimName

        // Icons
        // 只有第一个包 (scrolling) 负责记录统计数据，避免重复计数
        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "scrolling", killType, combo, victimId, window, hasHelmet, victimName, isVictimPlayer, true), player);
        
        // 其他视觉效果包不记录统计数据 (recordStats = false)
        if (combo > 0) {
            NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "combo", killType, combo, victimId, window, hasHelmet, victimName, isVictimPlayer, false), player);
        }
        // Card Icon (New)
        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "card", killType, combo, victimId, window, hasHelmet, victimName, isVictimPlayer, false), player);
        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "card_bar", killType, combo, victimId, window, hasHelmet, victimName, isVictimPlayer, false), player);
        NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "battlefield1", killType, combo, victimId, window, hasHelmet, victimName, isVictimPlayer, false), player);
        
        // Subtitle
        NetworkHandler.sendToPlayer(new KillIconPacket("subtitle", "kill_feed", killType, combo, victimId, window, hasHelmet, victimName, isVictimPlayer, false), player);
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

    private record DamageRecord(UUID attackerId, int amount, long timestamp) {}

    private static class CombatState {
        boolean flawless = true;
        long lastInteractionTime;
        CombatState(long time) { this.lastInteractionTime = time; }
    }

    private static class PendingKill {
        ServerPlayer player;
        UUID victimId;
        int victimIdInt;
        Vec3 victimPos;
        String victimName;
        int combo;
        int damageType;
        float maxHealth;
        float distance;
        int delay = 1;
        int sourceEntityId;
        long tick;
        boolean isGun;
        boolean isVictimThreat;
        boolean isBackstab;
        boolean isGliding;
        boolean isJusticeFromAbove;
        boolean isFlawless;
        boolean isVictimBlinded;
        boolean hasHelmet;
        boolean isVictimPlayer;

        long streakCount;

        PendingKill(ServerPlayer p, LivingEntity v, String vname, int c, int type, float hp, float dist, int sourceId, long t, boolean gun, boolean flawless) {
            this.player = p;
            this.victimId = v.getUUID();
            this.victimIdInt = v.getId();
            this.victimPos = v.getBoundingBox().getCenter();
            this.victimName = vname;
            this.combo = c;
            this.damageType = type;
            this.maxHealth = hp;
            this.distance = dist;
            this.sourceEntityId = sourceId;
            this.tick = t;
            this.isGun = gun;
            this.isFlawless = flawless;

            this.streakCount = calculateStreakCount(this.victimId);
            this.isVictimThreat = checkVictimThreat(p, v);
            this.isBackstab = checkBackstab(p, v);
            this.isGliding = p.isFallFlying();
            this.isJusticeFromAbove = checkJusticeFromAbove(p, v, this.isGliding);
            this.isVictimBlinded = checkBlinded(v);
            this.hasHelmet = !v.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty();
            this.isVictimPlayer = v instanceof net.minecraft.world.entity.player.Player;
        }

        private long calculateStreakCount(UUID victimId) {
            List<Long> victimKills = playerKillTimestamps.get(victimId);
            if (victimKills == null) victimKills = entityKillTimestamps.get(victimId);
            if (victimKills == null) return 0;
            
            long now = System.currentTimeMillis();
            return victimKills.stream().filter(time -> now - time <= 360000).count();
        }

        private boolean checkVictimThreat(ServerPlayer p, LivingEntity v) {
            if (v instanceof Monster || v instanceof ServerPlayer) return true;
            if (v instanceof NeutralMob && v instanceof Mob mob) return mob.getTarget() == p;
            return false;
        }

        private boolean checkBackstab(ServerPlayer p, LivingEntity v) {
            Vec3 toAttacker = p.position().subtract(v.position()).normalize();
            Vec3 victimLook = v.getViewVector(1.0F).normalize();
            return victimLook.dot(toAttacker) < -0.2;
        }

        private boolean checkJusticeFromAbove(ServerPlayer p, LivingEntity v, boolean isGliding) {
            boolean isFalling = p.getDeltaMovement().y < -0.1 && !p.onGround() && !p.getAbilities().flying;
            boolean isMovingCleanly = !p.isInWater() && !p.onClimbable();
            double heightDiff = p.getY() - v.getY();
            return !isGliding && isFalling && isMovingCleanly && heightDiff > 2.0;
        }
    }
}
