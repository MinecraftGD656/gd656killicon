package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.client.render.HudElementManager;
import org.mods.gd656killicon.client.render.impl.ComboIconRenderer;
import org.mods.gd656killicon.client.sounds.SoundTriggerManager;
import org.mods.gd656killicon.network.IPacket;

import java.util.function.Supplier;

public class KillIconPacket implements IPacket {
    private final String category;
    private final String name;
    private final int killType;
    private final int comboCount;
    private final int victimId;
    private final double comboWindowSeconds;
    private final boolean hasHelmet;
    private final String customVictimName;
    private final boolean isVictimPlayer;
    private final boolean shouldRecordStats;

    public KillIconPacket(String category, String name, int killType, int victimId) {
        this(category, name, killType, 0, victimId, -1.0, false, "", false, false);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId) {
        this(category, name, killType, comboCount, victimId, -1.0, false, "", false, false);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds) {
        this(category, name, killType, comboCount, victimId, comboWindowSeconds, false, "", false, false);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds, boolean hasHelmet) {
        this(category, name, killType, comboCount, victimId, comboWindowSeconds, hasHelmet, "", false, false);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds, boolean hasHelmet, String customVictimName) {
        this(category, name, killType, comboCount, victimId, comboWindowSeconds, hasHelmet, customVictimName, false, false);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds, boolean hasHelmet, String customVictimName, boolean isVictimPlayer) {
        this(category, name, killType, comboCount, victimId, comboWindowSeconds, hasHelmet, customVictimName, isVictimPlayer, false);
    }

    public KillIconPacket(String category, String name, int killType, int comboCount, int victimId, double comboWindowSeconds, boolean hasHelmet, String customVictimName, boolean isVictimPlayer, boolean shouldRecordStats) {
        this.category = category;
        this.name = name;
        this.killType = killType;
        this.comboCount = comboCount;
        this.victimId = victimId;
        this.comboWindowSeconds = comboWindowSeconds;
        this.hasHelmet = hasHelmet;
        this.customVictimName = customVictimName == null ? "" : customVictimName;
        this.isVictimPlayer = isVictimPlayer;
        this.shouldRecordStats = shouldRecordStats;
    }

    public KillIconPacket(FriendlyByteBuf buffer) {
        this.category = buffer.readUtf();
        this.name = buffer.readUtf();
        this.killType = buffer.readInt();
        this.comboCount = buffer.readInt();
        this.victimId = buffer.readInt();
        this.comboWindowSeconds = buffer.readDouble();
        this.hasHelmet = buffer.readBoolean();
        this.customVictimName = buffer.readUtf();
        this.isVictimPlayer = buffer.readBoolean();
        this.shouldRecordStats = buffer.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.category);
        buffer.writeUtf(this.name);
        buffer.writeInt(this.killType);
        buffer.writeInt(this.comboCount);
        buffer.writeInt(this.victimId);
        buffer.writeDouble(this.comboWindowSeconds);
        buffer.writeBoolean(this.hasHelmet);
        buffer.writeUtf(this.customVictimName);
        buffer.writeBoolean(this.isVictimPlayer);
        buffer.writeBoolean(this.shouldRecordStats);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ComboIconRenderer.updateServerComboWindowSeconds(this.comboWindowSeconds);
            // 收到数据包直接尝试播放音效，不依赖渲染器
            SoundTriggerManager.tryPlaySound(this.category, this.name, this.killType, this.comboCount, this.hasHelmet);
            
            // 尝试翻译生物名称（如果不是玩家且有自定义名称）
            String displayName = this.customVictimName;
            if (this.customVictimName != null && !this.customVictimName.isEmpty() && !this.isVictimPlayer) {
                try {
                    displayName = net.minecraft.client.resources.language.I18n.get(this.customVictimName);
                } catch (Exception e) {
                    // 忽略翻译错误，使用原始 Key
                }
            }
            
            HudElementManager.trigger(this.category, this.name, 
                new org.mods.gd656killicon.client.render.IHudRenderer.TriggerContext(
                    this.killType, this.victimId, this.comboCount, displayName
                )
            );

            // 统计击杀数据 (仅当 shouldRecordStats 为 true 时)
            if (this.shouldRecordStats && displayName != null && !displayName.isEmpty()) {
                org.mods.gd656killicon.client.stats.ClientStatsManager.recordGeneralKillStats(displayName, this.isVictimPlayer);
            }
        });
        context.get().setPacketHandled(true);
    }
}
