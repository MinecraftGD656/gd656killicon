package org.mods.gd656killicon.server.logic.conquest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.network.packet.ScoreboardSyncPacket;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ConquestScoreboardAdapter {
    private static final String BRIDGE_CLASS_NAME = "org.mods.gd656conquest.server.integration.KilliconScoreboardBridge";
    private static Method queryMethod;
    private static Method payloadEntriesMethod;
    private static Method payloadColumnsMethod;
    private static Method payloadPanelTeamsMethod;
    private static Method entryUuidMethod;
    private static Method entryNameMethod;
    private static Method entryLastLoginNameMethod;
    private static Method entryTeamNameMethod;
    private static Method entrySquadLabelMethod;
    private static Method entryScoreMethod;
    private static Method entryKillMethod;
    private static Method entryDeathMethod;
    private static Method entryAssistMethod;
    private static Method entryReviveMethod;
    private static Method entryPingMethod;
    private static Method entryOnlineMethod;
    private static Method entrySpectatorMethod;
    private static boolean initialized;

    private ConquestScoreboardAdapter() {
    }

    public static Result resolve(ServerPlayer requester) {
        if (!isConquestScoreboardAvailable(requester)) {
            return null;
        }
        try {
            ensureInitialized();
            Object payload = queryMethod.invoke(null, requester.server, requester.getUUID());
            if (payload == null) {
                return null;
            }
            int columns = (int) payloadColumnsMethod.invoke(payload);
            String[] panelTeams = (String[]) payloadPanelTeamsMethod.invoke(payload);
            List<ScoreboardSyncPacket.Entry> entries = mapPayloadEntries((List<?>) payloadEntriesMethod.invoke(payload));
            entries.sort((a, b) -> {
                int scoreCompare = Integer.compare(b.score, a.score);
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return a.uuid.compareTo(b.uuid);
            });
            int normalizedColumns = columns == 2 || columns == 4 ? columns : 1;
            return new Result(entries, normalizedColumns, normalizePanelTeams(panelTeams));
        } catch (Exception exception) {
            ServerLog.error("Failed to resolve conquest scoreboard payload: %s", exception.getMessage());
            return null;
        }
    }

    private static void ensureInitialized() throws Exception {
        if (initialized) {
            return;
        }
        Class<?> bridgeClass = Class.forName(BRIDGE_CLASS_NAME);
        queryMethod = bridgeClass.getMethod("query", MinecraftServer.class, UUID.class);
        Class<?> payloadClass = Class.forName(BRIDGE_CLASS_NAME + "$Payload");
        payloadEntriesMethod = payloadClass.getMethod("entries");
        payloadColumnsMethod = payloadClass.getMethod("columns");
        payloadPanelTeamsMethod = payloadClass.getMethod("panelTeams");
        Class<?> entryClass = Class.forName(BRIDGE_CLASS_NAME + "$Entry");
        entryUuidMethod = entryClass.getMethod("uuid");
        entryNameMethod = entryClass.getMethod("name");
        entryLastLoginNameMethod = entryClass.getMethod("lastLoginName");
        entryTeamNameMethod = entryClass.getMethod("teamName");
        try {
            entrySquadLabelMethod = entryClass.getMethod("squadLabel");
        } catch (NoSuchMethodException ignored) {
            entrySquadLabelMethod = null;
        }
        entryScoreMethod = entryClass.getMethod("score");
        entryKillMethod = entryClass.getMethod("kill");
        entryDeathMethod = entryClass.getMethod("death");
        entryAssistMethod = entryClass.getMethod("assist");
        try {
            entryReviveMethod = entryClass.getMethod("revive");
        } catch (NoSuchMethodException ignored) {
            entryReviveMethod = null;
        }
        entryPingMethod = entryClass.getMethod("ping");
        entryOnlineMethod = entryClass.getMethod("online");
        entrySpectatorMethod = entryClass.getMethod("spectator");
        initialized = true;
    }

    private static boolean isConquestScoreboardAvailable(ServerPlayer requester) {
        return requester != null
            && requester.server != null
            && ServerBridge.loader().isModLoaded("gd656conquest");
    }

    private static List<ScoreboardSyncPacket.Entry> mapPayloadEntries(List<?> payloadEntries) throws Exception {
        List<ScoreboardSyncPacket.Entry> entries = new ArrayList<>();
        if (payloadEntries == null) {
            return entries;
        }
        for (Object entry : payloadEntries) {
            entries.add(mapPayloadEntry(entry));
        }
        return entries;
    }

    private static ScoreboardSyncPacket.Entry mapPayloadEntry(Object entry) throws Exception {
        UUID uuid = (UUID) entryUuidMethod.invoke(entry);
        String name = (String) entryNameMethod.invoke(entry);
        String lastLoginName = (String) entryLastLoginNameMethod.invoke(entry);
        String teamName = (String) entryTeamNameMethod.invoke(entry);
        String squadLabel = entrySquadLabelMethod == null ? "" : (String) entrySquadLabelMethod.invoke(entry);
        int score = (int) entryScoreMethod.invoke(entry);
        int kill = (int) entryKillMethod.invoke(entry);
        int death = (int) entryDeathMethod.invoke(entry);
        int assist = (int) entryAssistMethod.invoke(entry);
        int revive = entryReviveMethod == null ? 0 : (int) entryReviveMethod.invoke(entry);
        int ping = (int) entryPingMethod.invoke(entry);
        boolean online = (boolean) entryOnlineMethod.invoke(entry);
        boolean spectator = (boolean) entrySpectatorMethod.invoke(entry);
        return new ScoreboardSyncPacket.Entry(uuid, safe(name), safe(lastLoginName), safe(teamName), safe(squadLabel), score, kill, death, assist, revive, ping, online, spectator);
    }

    private static String[] normalizePanelTeams(String[] source) {
        String[] result = new String[]{"", "", "", ""};
        if (source == null) {
            return result;
        }
        for (int i = 0; i < 4; i++) {
            if (i < source.length && source[i] != null) {
                result[i] = source[i];
            }
        }
        return result;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record Result(List<ScoreboardSyncPacket.Entry> entries, int columns, String[] panelTeams) {
    }
}
