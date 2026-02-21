package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.network.IPacket;

import java.util.function.Supplier;

/**
 * 客户端向服务端请求排行榜数据的数据包
 */
public class ScoreboardRequestPacket implements IPacket {

    public ScoreboardRequestPacket() {
    }

    public ScoreboardRequestPacket(FriendlyByteBuf buffer) {
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null) {
                org.mods.gd656killicon.server.data.PlayerDataManager.get().handleScoreboardRequest(ctx.getSender());
            }
        });
        ctx.setPacketHandled(true);
    }
}
