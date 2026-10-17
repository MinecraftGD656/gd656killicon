package org.mods.gd656killicon.server.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Bounded, idle-expiring snapshots; accessed only on the server thread. */
public final class ScoreboardSnapshotStore<T> {
    private final Map<UUID, Snapshot<T>> snapshots = new HashMap<>();
    private final long idleTimeoutMillis;
    private final int maxSnapshots;
    private long nextId;

    public ScoreboardSnapshotStore(long idleTimeoutMillis) {
        this(idleTimeoutMillis, 64);
    }

    public ScoreboardSnapshotStore(long idleTimeoutMillis, int maxSnapshots) {
        if (idleTimeoutMillis <= 0L || maxSnapshots <= 0) {
            throw new IllegalArgumentException("Snapshot timeout and capacity must be positive");
        }
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.maxSnapshots = maxSnapshots;
    }

    /** Returns null when full, preserving players who are already paging through a snapshot. */
    public Snapshot<T> create(UUID playerId, Supplier<? extends T> dataFactory, long now) {
        cleanupExpired(now);
        if (!snapshots.containsKey(playerId) && snapshots.size() >= maxSnapshots) {
            return null;
        }
        // Resolve the full board only after admission, so rejected requests do not allocate it.
        Snapshot<T> snapshot = new Snapshot<>(++nextId, dataFactory.get(), now + idleTimeoutMillis);
        snapshots.put(playerId, snapshot);
        return snapshot;
    }

    public void cleanupExpired(long now) {
        snapshots.values().removeIf(snapshot -> now >= snapshot.expiresAt());
    }

    public Snapshot<T> get(UUID playerId, long snapshotId, long now) {
        Snapshot<T> snapshot = snapshots.get(playerId);
        if (snapshot == null || snapshot.id() != snapshotId) {
            return null;
        }
        if (now >= snapshot.expiresAt()) {
            snapshots.remove(playerId);
            return null;
        }
        Snapshot<T> renewed = new Snapshot<>(snapshot.id(), snapshot.data(), now + idleTimeoutMillis);
        snapshots.put(playerId, renewed);
        return renewed;
    }

    public void remove(UUID playerId) {
        snapshots.remove(playerId);
    }

    public void clear() {
        snapshots.clear();
    }

    public record Snapshot<T>(long id, T data, long expiresAt) {
    }
}
