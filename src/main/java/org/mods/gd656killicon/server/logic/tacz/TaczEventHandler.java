package org.mods.gd656killicon.server.logic.tacz;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.ServerCore;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TaczEventHandler implements ITaczHandler {
    private final Set<UUID> headshotVictims = new HashSet<>();
    private final Set<UUID> headshotDamageVictims = new HashSet<>();
    private final Set<UUID> lastBulletVictims = new HashSet<>();
    private final Set<UUID> gunKillVictims = new HashSet<>();
    private final Map<UUID, EntityKineticBullet> trackedBullets = new ConcurrentHashMap<>();
    private final Map<UUID, Vec3> bulletLastPositions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> bulletShooters = new ConcurrentHashMap<>();
    private final Set<UUID> countedBullets = ConcurrentHashMap.newKeySet();
    private final Set<UUID> hitBullets = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> suppressionCounts = new ConcurrentHashMap<>();

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void tick() {
        headshotVictims.clear();
        headshotDamageVictims.clear();
        lastBulletVictims.clear();
        gunKillVictims.clear();
        updateFireSuppressionTracking();
    }

    @Override
    public boolean isHeadshotKill(UUID victimId) {
        return headshotVictims.remove(victimId);
    }

    @Override
    public boolean isHeadshotDamage(UUID victimId) {
        return headshotDamageVictims.remove(victimId);
    }

    @Override
    public boolean isLastBulletKill(UUID victimId) {
        return lastBulletVictims.remove(victimId);
    }

    @Override
    public boolean isGunKill(UUID victimId) {
        return gunKillVictims.remove(victimId);
    }

    @SubscribeEvent
    public void onKill(EntityKillByGunEvent event) {
        LivingEntity victim = event.getKilledEntity();
        if (victim == null) return;
        UUID victimId = victim.getUUID();

        // Mark gun kill
        gunKillVictims.add(victimId);

        // Mark headshot
        if (event.isHeadShot()) {
            headshotVictims.add(victimId);
        }

        // Check last bullet
        checkLastBullet(event);
    }

    @SubscribeEvent
    public void onHurt(EntityHurtByGunEvent event) {
        if (!(event.getHurtEntity() instanceof LivingEntity victim)) return;

        Entity bulletEntity = event.getBullet();
        if (bulletEntity instanceof EntityKineticBullet bullet) {
            hitBullets.add(bullet.getUUID());
        }

        if (event.isHeadShot()) {
            headshotDamageVictims.add(victim.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAmmoHitBlock(AmmoHitBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity owner = event.getAmmo().getOwner();
        if (!(owner instanceof ServerPlayer player)) return;
        if (event.getState().isAir()) return;
        if (!event.getLevel().getBlockState(event.getHitResult().getBlockPos()).isAir()) return;
        ServerCore.BONUS.add(player, BonusType.DESTROY_BLOCK, 1.0f, "");
    }

    @SubscribeEvent
    public void onBulletJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof EntityKineticBullet bullet)) return;
        UUID bulletId = bullet.getUUID();
        trackedBullets.put(bulletId, bullet);
        bulletLastPositions.put(bulletId, bullet.position());
        if (bullet.getOwner() instanceof ServerPlayer player) {
            bulletShooters.put(bulletId, player.getUUID());
        }
    }

    @SubscribeEvent
    public void onBulletLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof EntityKineticBullet bullet)) return;
        UUID bulletId = bullet.getUUID();
        Vec3 lastPos = bulletLastPositions.get(bulletId);
        Vec3 currentPos = lastPos == null ? bullet.position() : lastPos.add(bullet.getDeltaMovement());
        if (lastPos != null && !countedBullets.contains(bulletId)) {
            ServerPlayer shooter = resolveShooter(bullet, bulletId);
            if (shooter != null) {
                processSuppressionHit(shooter, bullet, bulletId, lastPos, currentPos);
            }
        }
        trackedBullets.remove(bulletId);
        bulletLastPositions.remove(bulletId);
        bulletShooters.remove(bulletId);
        countedBullets.remove(bulletId);
        hitBullets.remove(bulletId);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        suppressionCounts.remove(player.getUUID());
    }

    private void checkLastBullet(EntityKillByGunEvent event) {
        if (!(event.getAttacker() instanceof Player player)) return;

        ItemStack stack = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) return;

        int currentAmmo = iGun.getCurrentAmmoCount(stack);
        
        // If there is ammo left, it's not the last bullet
        if (currentAmmo > 0) return;

        // Check if there is a bullet in the barrel (closed bolt mechanics)
        if (iGun.hasBulletInBarrel(stack)) return;

        // Check gun capacity >= 2 to avoid single-shot weapons (like RPGs or single-shot rifles) counting every shot as "last bullet"
        TimelessAPI.getCommonGunIndex(event.getGunId()).ifPresent(index -> {
            int maxAmmo = index.getGunData().getAmmoAmount();
            if (maxAmmo >= 2 && event.getKilledEntity() != null) {
                lastBulletVictims.add(event.getKilledEntity().getUUID());
            }
        });
    }

    private ServerPlayer resolveShooter(EntityKineticBullet bullet, UUID bulletId) {
        UUID shooterId = bulletShooters.get(bulletId);
        if (shooterId == null && bullet.getOwner() instanceof ServerPlayer owner) {
            shooterId = owner.getUUID();
            bulletShooters.put(bulletId, shooterId);
        }
        if (shooterId == null) return null;
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayer(shooterId);
    }

    private void updateFireSuppressionTracking() {
        if (trackedBullets.isEmpty()) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (var entry : trackedBullets.entrySet()) {
            UUID bulletId = entry.getKey();
            EntityKineticBullet bullet = entry.getValue();
            if (bullet == null || bullet.isRemoved()) {
                trackedBullets.remove(bulletId);
                bulletLastPositions.remove(bulletId);
                bulletShooters.remove(bulletId);
                countedBullets.remove(bulletId);
                hitBullets.remove(bulletId);
                continue;
            }

            Vec3 lastPos = bulletLastPositions.get(bulletId);
            Vec3 currentPos;
            if (lastPos == null) {
                currentPos = bullet.position();
                bulletLastPositions.put(bulletId, currentPos);
                continue;
            } else {
                currentPos = lastPos.add(bullet.getDeltaMovement());
                bulletLastPositions.put(bulletId, currentPos);
            }
            if (countedBullets.contains(bulletId)) continue;

            ServerPlayer shooter = resolveShooter(bullet, bulletId);
            if (shooter == null) continue;
            processSuppressionHit(shooter, bullet, bulletId, lastPos, currentPos);
        }
    }

    private void processSuppressionHit(ServerPlayer shooter, EntityKineticBullet bullet, UUID bulletId, Vec3 start, Vec3 end) {
        if (hitBullets.contains(bulletId)) return;
        if (!isBulletSuppressing(shooter, bullet, start, end)) return;
        countedBullets.add(bulletId);
        UUID shooterId = shooter.getUUID();
        int count = suppressionCounts.getOrDefault(shooterId, 0) + 1;
        if (count >= 35) {
            ServerCore.BONUS.add(shooter, BonusType.FIRE_SUPPRESSION, 1.0f, "");
            count -= 35;
        }
        suppressionCounts.put(shooterId, count);
    }

    private boolean isBulletSuppressing(ServerPlayer shooter, EntityKineticBullet bullet, Vec3 start, Vec3 end) {
        AABB area = new AABB(start, end).inflate(2.0);
        var entities = bullet.level().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);
        if (entities.isEmpty()) return false;

        for (LivingEntity target : entities) {
            if (target == shooter) continue;
            if (target.isPassengerOfSameVehicle(shooter)) continue;
            if (target instanceof Player targetPlayer) {
                if (shooter.getTeam() != null && targetPlayer.getTeam() != null && shooter.getTeam() == targetPlayer.getTeam()) {
                    continue;
                }
            }
            AABB targetBox = target.getBoundingBox().inflate(2.0);
            if (targetBox.clip(start, end).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
