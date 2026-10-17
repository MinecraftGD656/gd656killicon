package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import org.mods.gd656killicon.network.IPacket;
import org.mods.gd656killicon.network.PacketContext;

/**
 * 客户端向服务端请求排行榜数据的数据包
 */
public class ScoreboardRequestPacket implements IPacket {
    private final int offset;
    private final int limit;
    private final long requestId;
    private final long snapshotId;

    public ScoreboardRequestPacket() {
        this(0, 20, 0L, 0L);
    }

    public ScoreboardRequestPacket(int offset, int limit, long requestId, long snapshotId) {
        this.offset = Math.max(0, offset);
        this.limit = Math.max(1, limit);
        this.requestId = requestId;
        this.snapshotId = snapshotId;
    }

    public ScoreboardRequestPacket(FriendlyByteBuf buffer) {
        this.offset = Math.max(0, buffer.readInt());
        this.limit = Math.max(1, buffer.readInt());
        this.requestId = buffer.readLong();
        this.snapshotId = buffer.readLong();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(offset);
        buffer.writeInt(limit);
        buffer.writeLong(requestId);
        buffer.writeLong(snapshotId);
    }

    @Override
    public void handle(PacketContext context) {
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                org.mods.gd656killicon.server.data.PlayerDataManager.get().handleScoreboardRequest(context.getSender(), offset, limit, requestId, snapshotId);
            }
        });
        context.setPacketHandled(true);
    }
}
