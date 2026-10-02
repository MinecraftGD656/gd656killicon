package org.mods.gd656killicon.fabric.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.network.IPacket;

import java.util.HashMap;
import java.util.Map;

public final class FabricNetworkTransport {
    private static final Map<Class<? extends IPacket>, Identifier> IDS = new HashMap<>();
    private static final Map<Class<? extends IPacket>, Boolean> IS_CLIENT_BOUND = new HashMap<>();

    private FabricNetworkTransport() {
    }

    public static void register() {
        registerPacket(org.mods.gd656killicon.network.packet.KillIconPacket.class, true);
        registerPacket(org.mods.gd656killicon.network.packet.DamageSoundPacket.class, true);
        registerPacket(org.mods.gd656killicon.network.packet.HitInfoPacket.class, true);
        registerPacket(org.mods.gd656killicon.network.packet.BonusScorePacket.class, true);
        registerPacket(org.mods.gd656killicon.network.packet.ScoreboardRequestPacket.class, false);
        registerPacket(org.mods.gd656killicon.network.packet.ScoreboardSyncPacket.class, true);
        registerPacket(org.mods.gd656killicon.network.packet.DeathPacket.class, true);
        registerPacket(org.mods.gd656killicon.network.packet.KillDistancePacket.class, true);
        registerPacket(org.mods.gd656killicon.network.packet.HonorPacket.class, true);
    }

    private static void registerPacket(Class<? extends IPacket> clazz, boolean clientBound) {
        String name = clazz.getSimpleName().toLowerCase(java.util.Locale.ROOT);
        Identifier id = Identifier.fromNamespaceAndPath(org.mods.gd656killicon.Gd656killicon.MODID, name);
        IDS.put(clazz, id);
        IS_CLIENT_BOUND.put(clazz, clientBound);
        try {
            var constructor = clazz.getConstructor(RegistryFriendlyByteBuf.class);
            NetworkManager.NetworkReceiver receiver = (buf, ctx) -> {
                IPacket packet;
                try {
                    packet = constructor.newInstance(buf);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decode packet " + clazz.getName(), e);
                }
                packet.handle(new FabricPacketContext(ctx));
            };
            if (clientBound) {
                if (Platform.getEnvironment() == Env.CLIENT) {
                    NetworkManager.registerReceiver(NetworkManager.Side.S2C, id, receiver);
                }
            } else {
                NetworkManager.registerReceiver(NetworkManager.Side.C2S, id, receiver);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Packet " + clazz.getName() + " lacks RegistryFriendlyByteBuf constructor", e);
        }
    }

    public static void sendToServer(IPacket message) {
        Identifier id = IDS.get(message.getClass());
        if (id == null) {
            return;
        }
        RegistryFriendlyByteBuf buf = encode(message);
        NetworkManager.sendToServer(id, buf);
    }

    public static void sendToPlayer(IPacket message, ServerPlayer player) {
        Identifier id = IDS.get(message.getClass());
        if (id == null) {
            return;
        }
        RegistryFriendlyByteBuf buf = encode(message);
        NetworkManager.sendToPlayer(player, id, buf);
    }

    public static void sendToAll(IPacket message) {
        Identifier id = IDS.get(message.getClass());
        if (id == null) {
            return;
        }
        RegistryFriendlyByteBuf buf = encode(message);
        java.util.List<ServerPlayer> players = ServerPlayerAccess.getOnlinePlayers();
        NetworkManager.sendToPlayers(players, id, buf);
    }

    private static RegistryFriendlyByteBuf encode(IPacket message) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        message.encode(buf);
        return buf;
    }

    private static final class ServerPlayerAccess {
        private static java.util.List<ServerPlayer> getOnlinePlayers() {
            net.minecraft.server.MinecraftServer server = org.mods.gd656killicon.server.ServerCore.getServer();
            if (server == null) {
                return java.util.Collections.emptyList();
            }
            return server.getPlayerList().getPlayers();
        }
    }
}
