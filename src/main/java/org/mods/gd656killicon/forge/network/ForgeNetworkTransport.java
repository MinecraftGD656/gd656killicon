package org.mods.gd656killicon.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mods.gd656killicon.Gd656killicon;

public final class ForgeNetworkTransport {
    private static final String PROTOCOL_VERSION = "2";
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private ForgeNetworkTransport() {
    }

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        INSTANCE = net;

        net.messageBuilder(org.mods.gd656killicon.network.packet.KillIconPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.KillIconPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.KillIconPacket::encode)
                .consumerMainThread((msg, ctx) -> msg.handle(new ForgePacketContext(ctx.get())))
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.DamageSoundPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.DamageSoundPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.DamageSoundPacket::encode)
                .consumerMainThread((msg, ctx) -> msg.handle(new ForgePacketContext(ctx.get())))
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.BonusScorePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.BonusScorePacket::new)
                .encoder(org.mods.gd656killicon.network.packet.BonusScorePacket::encode)
                .consumerMainThread((msg, ctx) -> msg.handle(new ForgePacketContext(ctx.get())))
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.ScoreboardRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(org.mods.gd656killicon.network.packet.ScoreboardRequestPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.ScoreboardRequestPacket::encode)
                .consumerMainThread((msg, ctx) -> msg.handle(new ForgePacketContext(ctx.get())))
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.ScoreboardSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.ScoreboardSyncPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.ScoreboardSyncPacket::encode)
                .consumerMainThread((msg, ctx) -> msg.handle(new ForgePacketContext(ctx.get())))
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.DeathPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.DeathPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.DeathPacket::encode)
                .consumerMainThread((msg, ctx) -> msg.handle(new ForgePacketContext(ctx.get())))
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.KillDistancePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.KillDistancePacket::new)
                .encoder(org.mods.gd656killicon.network.packet.KillDistancePacket::encode)
                .consumerMainThread((msg, ctx) -> msg.handle(new ForgePacketContext(ctx.get())))
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
