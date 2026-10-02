package org.mods.gd656killicon.network;

import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.fabric.network.FabricNetworkTransport;
import org.mods.gd656killicon.network.IPacket;

public class NetworkHandler {
    public static void register() {
        FabricNetworkTransport.register();
    }

    public static void sendToServer(IPacket message) {
        FabricNetworkTransport.sendToServer(message);
    }

    public static void sendToPlayer(IPacket message, ServerPlayer player) {
        FabricNetworkTransport.sendToPlayer(message, player);
    }

    public static void sendToAll(IPacket message) {
        FabricNetworkTransport.sendToAll(message);
    }
}
