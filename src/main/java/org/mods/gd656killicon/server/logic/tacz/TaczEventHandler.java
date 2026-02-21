package org.mods.gd656killicon.server.logic.tacz;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TaczEventHandler implements ITaczHandler {
    private final Set<UUID> headshotVictims = new HashSet<>();
    private final Set<UUID> headshotDamageVictims = new HashSet<>();
    private final Set<UUID> lastBulletVictims = new HashSet<>();
    private final Set<UUID> gunKillVictims = new HashSet<>();

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

        if (event.isHeadShot()) {
            headshotDamageVictims.add(victim.getUUID());
        }
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
}
