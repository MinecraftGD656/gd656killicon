package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import org.mods.gd656killicon.client.render.impl.HitInfoRenderer;
import org.mods.gd656killicon.network.IPacket;
import org.mods.gd656killicon.network.PacketContext;

/**
 * 命中信息数据包(PLAY_TO_CLIENT)。
 * <p>
 * 服务端在玩家对任意生物造成伤害时发送(amount = 本次伤害量, entityId = 受害实体)，
 * 在玩家击杀任意生物时发送(killed = true, entityId = 被击杀实体)。
 * 客户端 HitInfoRenderer 在显示持续窗口内按实体累积伤害量，击杀时对应实体的伤害占位符切换为击杀颜色。
 */
public class HitInfoPacket implements IPacket {
    private final float amount;
    private final boolean killed;
    private final int entityId;

    public HitInfoPacket(float amount, boolean killed) {
        this(amount, killed, -1);
    }

    public HitInfoPacket(float amount, boolean killed, int entityId) {
        this.amount = amount;
        this.killed = killed;
        this.entityId = entityId;
    }

    public HitInfoPacket(FriendlyByteBuf buffer) {
        this.amount = buffer.readFloat();
        this.killed = buffer.readBoolean();
        this.entityId = buffer.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.amount);
        buffer.writeBoolean(this.killed);
        buffer.writeInt(this.entityId);
    }

    public float getAmount() {
        return amount;
    }

    public boolean isKilled() {
        return killed;
    }

    public int getEntityId() {
        return entityId;
    }

    @Override
    public void handle(PacketContext context) {
        context.enqueueWork(() -> {
            if (this.killed) {
                HitInfoRenderer.getInstance().markKill(this.entityId);
            } else if (this.amount > 0.0f) {
                HitInfoRenderer.getInstance().addDamage(this.entityId, this.amount);
            }
        });
        context.setPacketHandled(true);
    }
}
