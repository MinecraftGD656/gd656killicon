package org.mods.gd656killicon.forge.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.network.PacketContext;

public class ForgePacketContext implements PacketContext {
    private final NetworkEvent.Context context;

    public ForgePacketContext(NetworkEvent.Context context) {
        this.context = context;
    }

    @Override
    public void enqueueWork(Runnable runnable) {
        context.enqueueWork(runnable);
    }

    @Override
    public void setPacketHandled(boolean handled) {
        context.setPacketHandled(handled);
    }

    @Override
    public ServerPlayer getSender() {
        return context.getSender();
    }
}
