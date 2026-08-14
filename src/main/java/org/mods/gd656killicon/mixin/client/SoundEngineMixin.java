package org.mods.gd656killicon.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.mods.gd656killicon.client.ClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void gd656killicon$muteTaczKillSound(SoundInstance soundInstance, CallbackInfo ci) {
        if (ClientEvents.shouldMutePlaySound(soundInstance)) {
            ci.cancel();
        }
    }
}
