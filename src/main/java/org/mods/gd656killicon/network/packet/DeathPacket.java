package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.network.IPacket;

import java.util.function.Supplier;

public class DeathPacket implements IPacket {
    private final String playerName;
    private final String deathCause;
    private final String killerName;

    public DeathPacket(String playerName, String deathCause, String killerName) {
        this.playerName = playerName;
        this.deathCause = deathCause;
        this.killerName = killerName;
    }

    public DeathPacket(FriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.deathCause = buffer.readUtf();
        this.killerName = buffer.readUtf();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeUtf(this.deathCause);
        buffer.writeUtf(this.killerName);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            
            org.mods.gd656killicon.client.stats.ClientStatsManager.recordDeath();
            if (this.killerName != null && !this.killerName.isEmpty()) {
                org.mods.gd656killicon.client.stats.ClientStatsManager.recordDeathByPlayer(this.killerName);
            }
            org.mods.gd656killicon.client.render.impl.ComboSubtitleRenderer.getInstance().onPlayerDeath();
        });
        context.get().setPacketHandled(true);
    }
}
