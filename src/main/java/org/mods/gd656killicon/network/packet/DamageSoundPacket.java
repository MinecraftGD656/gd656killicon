package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.client.sounds.SoundTriggerManager;
import org.mods.gd656killicon.network.IPacket;

import java.util.function.Supplier;

public class DamageSoundPacket implements IPacket {
    public DamageSoundPacket() {}

    public DamageSoundPacket(FriendlyByteBuf buffer) {}

    @Override
    public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(SoundTriggerManager::playHitSound);
        context.get().setPacketHandled(true);
    }
}
