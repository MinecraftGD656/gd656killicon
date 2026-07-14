package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import org.mods.gd656killicon.client.sounds.SoundTriggerManager;
import org.mods.gd656killicon.network.IPacket;
import org.mods.gd656killicon.network.PacketContext;

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
    public void handle(PacketContext context) {
        context.enqueueWork(() -> {
            if (headshotDamage) {
                SoundTriggerManager.playHeadshotDamageSound();
            } else {
                SoundTriggerManager.playHitSound();
            }
        });
        context.setPacketHandled(true);
    }
}
