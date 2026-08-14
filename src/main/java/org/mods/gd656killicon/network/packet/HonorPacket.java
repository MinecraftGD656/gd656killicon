package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import org.mods.gd656killicon.client.render.HudElementManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.network.IPacket;
import org.mods.gd656killicon.network.PacketContext;

/**
 * 荣誉显示包(服务端 → 客户端)。
 * <p>
 * 服务端荣誉达成时下发, 客户端触发 kill_icon/honor 元素的显示。
 * honorId 即荣誉 ID(如 "headhunter"), 客户端据此定位纹理与显示逻辑。
 * </p>
 */
public class HonorPacket implements IPacket {

    private final String honorId;
    private final String extraData;

    public HonorPacket(String honorId) {
        this(honorId, "");
    }

    public HonorPacket(String honorId, String extraData) {
        this.honorId = honorId != null ? honorId : "";
        this.extraData = extraData != null ? extraData : "";
    }

    /** 解码(网络注册用)。 */
    public HonorPacket(FriendlyByteBuf buffer) {
        this.honorId = buffer.readUtf();
        this.extraData = buffer.readUtf();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.honorId);
        buffer.writeUtf(this.extraData);
    }

    @Override
    public void handle(PacketContext context) {
        context.enqueueWork(() -> {
            // 服务端真实下发的荣誉才计入获得次数(预览/调试触发不走此路径)
            org.mods.gd656killicon.client.stats.ClientStatsManager.recordHonor(honorId);
            // 客户端触发荣誉元素显示(渲染当前为占位, 后续在 HonorRenderer 中按 honorId 实现)
            HudElementManager.trigger("kill_icon", "honor",
                    IHudRenderer.TriggerContext.of(0, honorId + (extraData.isEmpty() ? "" : ":" + extraData)));
        });
        context.setPacketHandled(true);
    }
}
