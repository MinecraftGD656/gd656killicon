package org.mods.gd656killicon.network;

import net.minecraft.server.level.ServerPlayer;

public interface PacketContext {
    void enqueueWork(Runnable runnable);
    void setPacketHandled(boolean handled);
    ServerPlayer getSender();
}
