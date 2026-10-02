package org.mods.gd656killicon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import org.mods.gd656killicon.network.IPacket;
import org.mods.gd656killicon.network.PacketContext;

public class KillDistancePacket implements IPacket {
    private final double distance;

    public KillDistancePacket(double distance) {
        this.distance = distance;
    }

    public KillDistancePacket(RegistryFriendlyByteBuf buffer) {
        this.distance = buffer.readDouble();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeDouble(this.distance);
    }

    @Override
    public void handle(PacketContext context) {
        context.enqueueWork(() -> {
            org.mods.gd656killicon.client.stats.ClientStatsManager.recordKillDistance(this.distance);
        });
        context.setPacketHandled(true);
    }
}
