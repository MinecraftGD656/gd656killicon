package org.mods.gd656killicon.mixin.superbwarfare;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.mods.gd656killicon.server.logic.superbwarfare.SuperbWarfareEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity", remap = false)
public abstract class VehicleEntityMixin {
    @Unique
    private float gd656killicon$healthBeforeHurt;

    @Shadow
    public abstract float getHealth();

    @Inject(method = "hurt", at = @At("HEAD"))
    private void gd656killicon$capturePreDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        gd656killicon$healthBeforeHurt = this.getHealth();
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void gd656killicon$recordActualVehicleDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if ((Object) this instanceof Entity entity) {
            SuperbWarfareEventHandler.onVehicleDamageApplied(entity, source, gd656killicon$healthBeforeHurt);
        }
    }
}
