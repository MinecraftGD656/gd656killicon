package org.mods.gd656killicon.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mods.gd656killicon.Gd656killicon;

public class NetworkHandler {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "main"),
                () -> "1.0",
                s -> true,
                s -> true
        );

        INSTANCE = net;

        net.messageBuilder(org.mods.gd656killicon.network.packet.KillIconPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.KillIconPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.KillIconPacket::encode)
                .consumerMainThread(org.mods.gd656killicon.network.packet.KillIconPacket::handle)
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.DamageSoundPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.DamageSoundPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.DamageSoundPacket::encode)
                .consumerMainThread(org.mods.gd656killicon.network.packet.DamageSoundPacket::handle)
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.BonusScorePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.BonusScorePacket::new)
                .encoder(org.mods.gd656killicon.network.packet.BonusScorePacket::encode)
                .consumerMainThread(org.mods.gd656killicon.network.packet.BonusScorePacket::handle)
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.ScoreboardRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(org.mods.gd656killicon.network.packet.ScoreboardRequestPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.ScoreboardRequestPacket::encode)
                .consumerMainThread(org.mods.gd656killicon.network.packet.ScoreboardRequestPacket::handle)
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.ScoreboardSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.ScoreboardSyncPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.ScoreboardSyncPacket::encode)
                .consumerMainThread(org.mods.gd656killicon.network.packet.ScoreboardSyncPacket::handle)
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.DeathPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.DeathPacket::new)
                .encoder(org.mods.gd656killicon.network.packet.DeathPacket::encode)
                .consumerMainThread(org.mods.gd656killicon.network.packet.DeathPacket::handle)
                .add();
        net.messageBuilder(org.mods.gd656killicon.network.packet.KillDistancePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(org.mods.gd656killicon.network.packet.KillDistancePacket::new)
                .encoder(org.mods.gd656killicon.network.packet.KillDistancePacket::encode)
                .consumerMainThread(org.mods.gd656killicon.network.packet.KillDistancePacket::handle)
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
