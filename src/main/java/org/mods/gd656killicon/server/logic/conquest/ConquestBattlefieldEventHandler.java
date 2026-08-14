package org.mods.gd656killicon.server.logic.conquest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.network.NetworkHandler;
import org.mods.gd656killicon.network.packet.KillIconPacket;
import org.mods.gd656killicon.server.ServerCore;
import org.mods.gd656killicon.server.bridge.ServerBridge;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.util.ServerLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConquestBattlefieldEventHandler implements IConquestBattlefieldHandler {
    private static final String CONQUEST_MANAGER_CLASS = "org.mods.gd656conquest.server.room.entry.RoomCoreRuntimeManager";
    private static final String CONQUEST_ROOM_DEFINITION_CLASS = "org.mods.gd656conquest.server.room.model.RoomDefinition";
    private static final int CAPTURE_SCORE_VICTIM_ID = -5500;
    private static final long CAPTURE_PROGRESS_INTERVAL_MS = 2000L;
    private static final long MAJOR_EVENT_COOLDOWN_MS = 30000L;

    private final Map<String, Map<String, CaptureSnapshot>> roomSnapshots = new ConcurrentHashMap<>();
    private final Map<String, Long> lastProgressBonusTime = new ConcurrentHashMap<>();
    private final Map<String, Long> lastNeutralizeBonusTime = new ConcurrentHashMap<>();
    private final Map<String, Long> lastControlBonusTime = new ConcurrentHashMap<>();

    private volatile boolean reflectionReady = false;
    private Method managerOfMethod;
    private Method listRoomsMethod;
    private Method buildSnapshotMethod;
    private Method roomIdMethod;

    @Override
    public void init() {
        try {
            Class<?> managerClass = Class.forName(CONQUEST_MANAGER_CLASS);
            Class<?> roomDefinitionClass = Class.forName(CONQUEST_ROOM_DEFINITION_CLASS);
            managerOfMethod = managerClass.getMethod("of", MinecraftServer.class);
            listRoomsMethod = managerClass.getMethod("listRooms");
            buildSnapshotMethod = managerClass.getMethod("buildKilliconConquestSnapshot", String.class);
            roomIdMethod = roomDefinitionClass.getMethod("roomId");
            reflectionReady = true;
            ServerLog.info("GD656Conquest integration initialized.");
        } catch (Exception e) {
            reflectionReady = false;
            ServerLog.info("GD656Conquest integration unavailable: %s", e.getMessage());
        }
    }

    @Override
    public void tick() {
        if (!reflectionReady) {
            return;
        }
        MinecraftServer server = ServerBridge.loader().getCurrentServer();
        if (server == null) {
            return;
        }
        try {
            Object manager = managerOfMethod.invoke(null, server);
            if (manager == null) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<Object> rooms = (List<Object>) listRoomsMethod.invoke(manager);
            if (rooms == null || rooms.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            for (Object roomDefinition : rooms) {
                String roomId = resolveRoomId(roomDefinition);
                if (roomId == null || roomId.isBlank()) {
                    continue;
                }
                Map<String, Object> snapshot = resolveSnapshot(manager, roomId);
                if (snapshot == null || snapshot.isEmpty()) {
                    roomSnapshots.remove(roomId);
                    continue;
                }
                Set<UUID> roomPlayers = castUuidSet(snapshot.get("roomPlayers"));
                Map<UUID, String> playerTeams = castUuidStringMap(snapshot.get("playerTeams"));
                Map<UUID, Set<String>> activeCodesByPlayer = castActiveCodesMap(snapshot.get("activeCodesByPlayer"));
                Map<String, Number> progressByCode = castStringNumberMap(snapshot.get("progressByCode"));
                Map<String, String> ownerByCode = castStringMap(snapshot.get("ownerByCode"));
                Map<String, String> drivingByCode = castStringMap(snapshot.get("drivingByCode"));
                Map<String, String> captureDisplayNames = castStringMap(snapshot.get("captureDisplayNames"));
                if (roomPlayers.isEmpty() || progressByCode.isEmpty()) {
                    roomSnapshots.remove(roomId);
                    continue;
                }
                Map<String, CaptureSnapshot> previousByCode = roomSnapshots.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>());
                Set<String> aliveCodes = new HashSet<>();
                for (Map.Entry<String, Number> codeEntry : progressByCode.entrySet()) {
                    String code = normalizeToken(codeEntry.getKey());
                    if (code.isEmpty()) {
                        continue;
                    }
                    aliveCodes.add(code);
                    float progress = toFloat(codeEntry.getValue());
                    String owner = normalizeToken(ownerByCode.get(code));
                    String driving = normalizeToken(drivingByCode.get(code));
                    CaptureSnapshot previous = previousByCode.put(code, new CaptureSnapshot(progress, owner, driving));
                    if (previous == null) {
                        continue;
                    }
                    for (UUID playerId : roomPlayers) {
                        Set<String> activeCodes = activeCodesByPlayer.get(playerId);
                        if (activeCodes == null || !containsIgnoreCase(activeCodes, code)) {
                            continue;
                        }
                        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                        if (player == null) {
                            continue;
                        }
                        String playerTeam = normalizeToken(playerTeams.get(playerId));
                        if (playerTeam.isEmpty()) {
                            continue;
                        }
                        String cooldownKey = playerId + "|" + code;
                        if (isProgressTowardTeam(previous.progress, progress, playerTeam, driving)) {
                            long last = lastProgressBonusTime.getOrDefault(cooldownKey, 0L);
                            if (now - last >= CAPTURE_PROGRESS_INTERVAL_MS && ServerData.get().isBonusEnabled(BonusType.CONQUEST_CAPTURE_PROGRESS)) {
                                ServerCore.BONUS.add(player, BonusType.CONQUEST_CAPTURE_PROGRESS, 1.0f, "");
                                lastProgressBonusTime.put(cooldownKey, now);
                            }
                        }
                        boolean neutralizedNow = isNeutralizedTowardPlayer(previous.progress, progress, previous.owner, owner, driving, playerTeam);
                        if (neutralizedNow && now - lastNeutralizeBonusTime.getOrDefault(cooldownKey, 0L) >= MAJOR_EVENT_COOLDOWN_MS) {
                            if (ServerData.get().isBonusEnabled(BonusType.CONQUEST_CAPTURE_NEUTRALIZE)) {
                                ServerCore.BONUS.add(player, BonusType.CONQUEST_CAPTURE_NEUTRALIZE, 1.0f, "");
                                lastNeutralizeBonusTime.put(cooldownKey, now);
                            }
                        }
                        boolean controlledNow = !equalsIgnoreCase(owner, previous.owner)
                            && equalsIgnoreCase(owner, playerTeam)
                            && !equalsIgnoreCase(previous.owner, playerTeam);
                        if (controlledNow && now - lastControlBonusTime.getOrDefault(cooldownKey, 0L) >= MAJOR_EVENT_COOLDOWN_MS) {
                            if (ServerData.get().isBonusEnabled(BonusType.CONQUEST_CAPTURE_CONTROL)) {
                                ServerCore.BONUS.add(player, BonusType.CONQUEST_CAPTURE_CONTROL, 1.0f, "", CAPTURE_SCORE_VICTIM_ID);
                                double comboWindow = ServerData.get().getComboWindowSeconds();
                                String capturePayload = buildCaptureSubtitlePayload(captureDisplayNames, code);
                                NetworkHandler.sendToPlayer(new KillIconPacket("kill_icon", "scrolling", KillType.CAPTURE, 0, -1, comboWindow), player);
                                NetworkHandler.sendToPlayer(new KillIconPacket("subtitle", "kill_feed", KillType.CAPTURE, 0, CAPTURE_SCORE_VICTIM_ID, comboWindow, false, capturePayload), player);
                                lastControlBonusTime.put(cooldownKey, now);
                            }
                        }
                    }
                }
                previousByCode.keySet().retainAll(aliveCodes);
            }
        } catch (Exception e) {
            ServerLog.error("GD656Conquest integration runtime error: %s", e.getMessage());
        }
    }

    private String resolveRoomId(Object roomDefinition) {
        if (roomDefinition == null || roomIdMethod == null) {
            return null;
        }
        try {
            Object value = roomIdMethod.invoke(roomDefinition);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveSnapshot(Object manager, String roomId) {
        if (manager == null || buildSnapshotMethod == null || roomId == null || roomId.isBlank()) {
            return Map.of();
        }
        try {
            Object value = buildSnapshotMethod.invoke(manager, roomId);
            if (value instanceof Map<?, ?> raw) {
                return (Map<String, Object>) raw;
            }
        } catch (Exception ignored) {
        }
        return Map.of();
    }

    private String buildCaptureSubtitlePayload(Map<String, String> captureDisplayNames, String captureCode) {
        String normalizedCode = normalizeToken(captureCode);
        String displayName = captureDisplayNames.getOrDefault(normalizedCode, normalizedCode);
        if (displayName == null || displayName.isBlank()) {
            displayName = normalizedCode;
        }
        return normalizedCode + "|" + displayName;
    }

    private float toFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return 0.0f;
    }

    private String normalizeToken(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase(java.util.Locale.ROOT);
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }

    private boolean containsIgnoreCase(Set<String> values, String target) {
        if (values == null || target == null) {
            return false;
        }
        for (String value : values) {
            if (equalsIgnoreCase(value, target)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProgressTowardTeam(float previous, float current, String team, String driving) {
        if (team == null || driving == null) {
            return false;
        }
        if (!equalsIgnoreCase(team, driving)) {
            return false;
        }
        return Math.abs(current - previous) > 0.01f;
    }

    private boolean isNeutralizedTowardPlayer(float previous, float current, String previousOwner, String currentOwner, String driving, String playerTeam) {
        if (playerTeam == null || playerTeam.isBlank()) {
            return false;
        }
        if (equalsIgnoreCase(previousOwner, playerTeam)) {
            return false;
        }
        if (previousOwner == null || previousOwner.isBlank()) {
            return false;
        }
        if (currentOwner != null && !currentOwner.isBlank()) {
            return false;
        }
        if (!equalsIgnoreCase(driving, playerTeam)) {
            return false;
        }
        if (Math.abs(current - previous) <= 0.01f) {
            return false;
        }
        if ("CAMP_A".equalsIgnoreCase(playerTeam)) {
            return current > previous;
        }
        if ("CAMP_B".equalsIgnoreCase(playerTeam)) {
            return current < previous;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Set<UUID> castUuidSet(Object raw) {
        if (!(raw instanceof Set<?> set)) {
            return Set.of();
        }
        Set<UUID> result = new HashSet<>();
        for (Object value : set) {
            if (value instanceof UUID uuid) {
                result.add(uuid);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, String> castUuidStringMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<UUID, String> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof UUID uuid) {
                result.put(uuid, entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private Map<UUID, Set<String>> castActiveCodesMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<UUID, Set<String>> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof UUID uuid) || !(entry.getValue() instanceof Set<?> values)) {
                continue;
            }
            Set<String> normalized = new HashSet<>();
            for (Object value : values) {
                String token = normalizeToken(value);
                if (!token.isEmpty()) {
                    normalized.add(token);
                }
            }
            if (!normalized.isEmpty()) {
                result.put(uuid, normalized);
            }
        }
        return result;
    }

    private Map<String, Number> castStringNumberMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Number> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = normalizeToken(entry.getKey());
            if (!key.isEmpty() && entry.getValue() instanceof Number number) {
                result.put(key, number);
            }
        }
        return result;
    }

    private Map<String, String> castStringMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = normalizeToken(entry.getKey());
            if (!key.isEmpty()) {
                result.put(key, entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private record CaptureSnapshot(float progress, String owner, String driving) {}
}
