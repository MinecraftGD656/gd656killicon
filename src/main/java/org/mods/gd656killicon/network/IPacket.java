package org.mods.gd656killicon.network;

import net.minecraft.network.FriendlyByteBuf;

public interface IPacket {
    void encode(FriendlyByteBuf buffer);
    void handle(PacketContext context);
}
