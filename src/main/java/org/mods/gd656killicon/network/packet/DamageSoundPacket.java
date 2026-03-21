package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.client.sounds.SoundTriggerManager;
import org.mods.gd656killicon.network.IPacket;

import java.util.function.Supplier;

public class DamageSoundPacket implements IPacket {
    private final boolean headshotDamage;

    public DamageSoundPacket() {
        this(false);
    }

    public DamageSoundPacket(boolean headshotDamage) {
        this.headshotDamage = headshotDamage;
    }

    public DamageSoundPacket(FriendlyByteBuf buffer) {
        this.headshotDamage = buffer.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(headshotDamage);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (headshotDamage) {
                SoundTriggerManager.playHeadshotDamageSound();
            } else {
                SoundTriggerManager.playHitSound();
            }
        });
        context.get().setPacketHandled(true);
    }
}
