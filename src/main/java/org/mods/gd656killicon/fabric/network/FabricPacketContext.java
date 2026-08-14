package org.mods.gd656killicon.fabric.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.network.PacketContext;

public class FabricPacketContext implements PacketContext {
    private final NetworkManager.PacketContext context;

    public FabricPacketContext(NetworkManager.PacketContext context) {
        this.context = context;
    }

    @Override
    public void enqueueWork(Runnable runnable) {
        context.queue(runnable);
    }

    @Override
    public void setPacketHandled(boolean handled) {
        // Architectury 网络层已自动处理 packet handled 状态
    }

    @Override
    public ServerPlayer getSender() {
        if (context.getPlayer() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }
}
