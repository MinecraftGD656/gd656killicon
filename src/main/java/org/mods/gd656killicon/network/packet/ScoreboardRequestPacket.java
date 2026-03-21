package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.network.IPacket;

import java.util.function.Supplier;

/**
 * 客户端向服务端请求排行榜数据的数据包
 */
public class ScoreboardRequestPacket implements IPacket {
    private final int offset;
    private final int limit;
    private final long requestId;

    public ScoreboardRequestPacket() {
        this(0, 20, 0L);
    }

    public ScoreboardRequestPacket(int offset, int limit, long requestId) {
        this.offset = Math.max(0, offset);
        this.limit = Math.max(1, limit);
        this.requestId = requestId;
    }

    public ScoreboardRequestPacket(FriendlyByteBuf buffer) {
        this.offset = Math.max(0, buffer.readInt());
        this.limit = Math.max(1, buffer.readInt());
        this.requestId = buffer.readLong();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(offset);
        buffer.writeInt(limit);
        buffer.writeLong(requestId);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null) {
                org.mods.gd656killicon.server.data.PlayerDataManager.get().handleScoreboardRequest(ctx.getSender(), offset, limit, requestId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
