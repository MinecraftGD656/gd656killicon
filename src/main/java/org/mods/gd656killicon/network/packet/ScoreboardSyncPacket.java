package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.network.IPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端向客户端同步排行榜数据的数据包
 */
public class ScoreboardSyncPacket implements IPacket {
    private final List<Entry> entries;
    private final int offset;
    private final int totalCount;
    private final long requestId;

    public ScoreboardSyncPacket(List<Entry> entries, int offset, int totalCount, long requestId) {
        this.entries = entries;
        this.offset = Math.max(0, offset);
        this.totalCount = Math.max(0, totalCount);
        this.requestId = requestId;
    }

    public ScoreboardSyncPacket(FriendlyByteBuf buffer) {
        this.offset = Math.max(0, buffer.readInt());
        this.totalCount = Math.max(0, buffer.readInt());
        this.requestId = buffer.readLong();
        int size = buffer.readInt();
        this.entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.entries.add(new Entry(
                buffer.readUUID(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readBoolean()             ));
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(offset);
        buffer.writeInt(totalCount);
        buffer.writeLong(requestId);
        buffer.writeInt(entries.size());
        for (Entry entry : entries) {
            buffer.writeUUID(entry.uuid);
            buffer.writeUtf(entry.name);
            buffer.writeUtf(entry.lastLoginName);
            buffer.writeInt(entry.score);
            buffer.writeInt(entry.kill);
            buffer.writeInt(entry.death);
            buffer.writeInt(entry.assist);
            buffer.writeInt(entry.ping);
            buffer.writeBoolean(entry.online);
            buffer.writeBoolean(entry.spectator);
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            org.mods.gd656killicon.client.gui.tabs.ScoreboardTab.updateData(this.entries, this.offset, this.totalCount, this.requestId);
        });
        context.get().setPacketHandled(true);
    }

    public static class Entry {
        public final UUID uuid;
        public final String name;
        public final String lastLoginName;
        public final int score;
        public final int kill;
        public final int death;
        public final int assist;
        public final int ping;
        public final boolean online;
        public final boolean spectator;

        public Entry(UUID uuid, String name, String lastLoginName, int score, int kill, int death, int assist, int ping, boolean online, boolean spectator) {
            this.uuid = uuid;
            this.name = name;
            this.lastLoginName = lastLoginName;
            this.score = score;
            this.kill = kill;
            this.death = death;
            this.assist = assist;
            this.ping = ping;
            this.online = online;
            this.spectator = spectator;
        }
    }
}
