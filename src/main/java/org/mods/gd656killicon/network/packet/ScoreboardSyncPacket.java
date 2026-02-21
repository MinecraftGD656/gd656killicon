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

    public ScoreboardSyncPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public ScoreboardSyncPacket(FriendlyByteBuf buffer) {
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
                buffer.readInt() // ping
            ));
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
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
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 客户端处理逻辑
            org.mods.gd656killicon.client.gui.tabs.ScoreboardTab.updateData(this.entries);
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

        public Entry(UUID uuid, String name, String lastLoginName, int score, int kill, int death, int assist, int ping) {
            this.uuid = uuid;
            this.name = name;
            this.lastLoginName = lastLoginName;
            this.score = score;
            this.kill = kill;
            this.death = death;
            this.assist = assist;
            this.ping = ping;
        }
    }
}
