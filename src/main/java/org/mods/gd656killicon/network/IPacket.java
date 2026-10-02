package org.mods.gd656killicon.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IPacket {
    void encode(RegistryFriendlyByteBuf buffer);
    void handle(PacketContext context);
}
