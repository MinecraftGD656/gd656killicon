package org.mods.gd656killicon.mixin.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.mods.gd656killicon.server.ServerCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 替代 Forge EntityMountEvent: 监听上下车事件, 转发到荣誉引擎(跳机火箭筒)。
 */
@Mixin(Entity.class)
public abstract class EntityRideMixin {
    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("RETURN"))
    private void gd656killicon$onStartRiding(Entity vehicle, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }
        if (self instanceof ServerPlayer player) {
            ServerCore.HONOR.onVehicleMountBack(player, vehicle);
        }
    }

    @Inject(method = "stopRiding", at = @At("HEAD"))
    private void gd656killicon$onStopRiding(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }
        Entity vehicle = self.getVehicle();
        if (self instanceof ServerPlayer player && vehicle != null) {
            ServerCore.HONOR.onVehicleBailOut(player, vehicle);
        }
    }
}
