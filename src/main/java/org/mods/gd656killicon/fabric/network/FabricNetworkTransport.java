package org.mods.gd656killicon.fabric.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.mods.gd656killicon.fabric.network.FabricPacketContext;
import org.mods.gd656killicon.network.IPacket;

import java.util.HashMap;
import java.util.Map;

public final class FabricNetworkTransport {
    private static final Map<Class<? extends IPacket>, ResourceLocation> IDS = new HashMap<>();
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
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(org.mods.gd656killicon.Gd656killicon.MODID, name);
        IDS.put(clazz, id);
        IS_CLIENT_BOUND.put(clazz, clientBound);
        try {
            var constructor = clazz.getConstructor(net.minecraft.network.FriendlyByteBuf.class);
            NetworkManager.NetworkReceiver<RegistryFriendlyByteBuf> receiver = (buf, ctx) -> {
                IPacket packet;
                try {
                    packet = constructor.newInstance(buf);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decode packet " + clazz.getName(), e);
                }
                packet.handle(new FabricPacketContext(ctx));
            };
            if (clientBound) {
                if (Platform.getEnv() == net.fabricmc.api.EnvType.CLIENT) {
                    NetworkManager.registerReceiver(NetworkManager.Side.S2C, id, receiver);
                } else {
                    // Architectury 已知问题(architectury-api#680): Fabric 专用服务器上 registerS2C 被
                    // @Environment(CLIENT) 剔除, 服务端注册 S2C 接收器会 AbstractMethodError。
                    // 服务端只需注册 payload 类型(用于协商与发送 S2C 包), 接收器仅存在于客户端。
                    NetworkManager.registerS2CPayloadType(id);
                }
            } else {
                NetworkManager.registerReceiver(NetworkManager.Side.C2S, id, receiver);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Packet " + clazz.getName() + " lacks FriendlyByteBuf constructor", e);
        }
    }

    public static void sendToServer(IPacket message) {
        ResourceLocation id = IDS.get(message.getClass());
        if (id == null) {
            return;
        }
        RegistryFriendlyByteBuf buf = encode(message);
        NetworkManager.sendToServer(id, buf);
    }

    public static void sendToPlayer(IPacket message, ServerPlayer player) {
        ResourceLocation id = IDS.get(message.getClass());
        if (id == null) {
            return;
        }
        RegistryFriendlyByteBuf buf = encode(message);
        NetworkManager.sendToPlayer(player, id, buf);
    }

    public static void sendToAll(IPacket message) {
        ResourceLocation id = IDS.get(message.getClass());
        if (id == null) {
            return;
        }
        RegistryFriendlyByteBuf buf = encode(message);
        java.util.List<ServerPlayer> players = ServerPlayerAccess.getOnlinePlayers();
        NetworkManager.sendToPlayers(players, id, buf);
    }

    private static RegistryFriendlyByteBuf encode(IPacket message) {
        RegistryAccess registryAccess = getRegistryAccess();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registryAccess);
        message.encode(buf);
        return buf;
    }

    private static RegistryAccess getRegistryAccess() {
        MinecraftServer server = org.mods.gd656killicon.server.ServerCore.getServer();
        if (server != null) {
            return server.registryAccess();
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.getConnection() != null) {
            return mc.getConnection().registryAccess();
        }
        return RegistryAccess.EMPTY;
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
