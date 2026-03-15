package org.mods.gd656killicon.server.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ServerCombatAttribution {
    private ServerCombatAttribution() {}

    static LivingEntity resolveLivingAttacker(DamageSource src, LivingEntity victim, Map<UUID, FireAttribution> fireAttribution, long fireAttributionTimeoutMs) {
        Entity source = src.getEntity();
        if (source instanceof LivingEntity living) {
            if (living instanceof ServerPlayer player && src.is(DamageTypeTags.IS_FIRE)) {
                recordFireAttribution(fireAttribution, victim.getUUID(), player.getUUID());
            }
            return living;
        }
        Entity direct = src.getDirectEntity();
        if (direct instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living) {
                if (living instanceof ServerPlayer player && src.is(DamageTypeTags.IS_FIRE)) {
                    recordFireAttribution(fireAttribution, victim.getUUID(), player.getUUID());
                }
                return living;
            }
        }
        ServerPlayer firePlayer = resolveFireAttacker(victim, src, fireAttribution, fireAttributionTimeoutMs);
        if (firePlayer != null) {
            return firePlayer;
        }
        return null;
    }

    static ServerPlayer resolvePlayerAttacker(DamageSource src, LivingEntity victim, Map<UUID, FireAttribution> fireAttribution, long fireAttributionTimeoutMs) {
        LivingEntity attacker = resolveLivingAttacker(src, victim, fireAttribution, fireAttributionTimeoutMs);
        return attacker instanceof ServerPlayer player ? player : null;
    }

    static void recordFireAttribution(Map<UUID, FireAttribution> fireAttribution, UUID victimId, UUID attackerId) {
        fireAttribution.put(victimId, new FireAttribution(attackerId, System.currentTimeMillis()));
    }

    private static ServerPlayer resolveFireAttacker(LivingEntity victim, DamageSource src, Map<UUID, FireAttribution> fireAttribution, long fireAttributionTimeoutMs) {
        if (!src.is(DamageTypeTags.IS_FIRE)) return null;
        if (src.is(DamageTypes.LAVA)) return null;
        long now = System.currentTimeMillis();
        FireAttribution record = fireAttribution.get(victim.getUUID());
        if (record != null) {
            if (now - record.timestamp() <= fireAttributionTimeoutMs) {
                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return null;
                ServerPlayer player = server.getPlayerList().getPlayer(record.attackerId());
                if (player != null) return player;
            } else {
                fireAttribution.remove(victim.getUUID());
            }
        }
        ServerPlayer molotovOwner = resolveMolotovOwner(victim);
        if (molotovOwner != null) {
            recordFireAttribution(fireAttribution, victim.getUUID(), molotovOwner.getUUID());
            return molotovOwner;
        }
        return null;
    }

    private static ServerPlayer resolveMolotovOwner(LivingEntity victim) {
        double searchRadius = 6.0;
        AABB area = victim.getBoundingBox().inflate(searchRadius, 2.0, searchRadius);
        List<Entity> entities = victim.level().getEntities(victim, area, ServerCombatAttribution::isLrTacticalFireCloud);
        for (Entity entity : entities) {
            if (!(entity instanceof AreaEffectCloud cloud)) continue;
            if (!isIgniteCloud(entity)) continue;
            double radius = cloud.getRadius();
            if (radius <= 0.0) continue;
            double maxDist = radius + 1.0;
            if (cloud.position().distanceToSqr(victim.position()) > maxDist * maxDist) continue;
            Entity owner = cloud.getOwner();
            if (owner instanceof ServerPlayer player) {
                return player;
            }
        }
        return null;
    }

    private static boolean isLrTacticalFireCloud(Entity entity) {
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && "lrtactical".equals(key.getNamespace()) && "sp_effect_cloud".equals(key.getPath());
    }

    private static boolean isIgniteCloud(Entity entity) {
        try {
            Method method = entity.getClass().getMethod("isIgnite");
            Object result = method.invoke(entity);
            return result instanceof Boolean b && b;
        } catch (Exception ignored) {
            return true;
        }
    }
}
