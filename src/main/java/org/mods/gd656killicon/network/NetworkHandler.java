package org.mods.gd656killicon.network;

import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.forge.network.ForgeNetworkTransport;

public class NetworkHandler {
    public static void register() {
        ForgeNetworkTransport.register();
    }

    public static <MSG> void sendToServer(MSG message) {
        ForgeNetworkTransport.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        ForgeNetworkTransport.sendToPlayer(message, player);
    }

    public static <MSG> void sendToAll(MSG message) {
        ForgeNetworkTransport.sendToAll(message);
    }
}
