package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.network.IPacket;

import java.util.function.Supplier;

public class KillDistancePacket implements IPacket {
    private final double distance;

    public KillDistancePacket(double distance) {
        this.distance = distance;
    }

    public KillDistancePacket(FriendlyByteBuf buffer) {
        this.distance = buffer.readDouble();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.distance);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            
            org.mods.gd656killicon.client.stats.ClientStatsManager.recordKillDistance(this.distance);
        });
        context.get().setPacketHandled(true);
    }
}
