package org.mods.gd656killicon.client.cache;

/** Tracks the server cursor independently of visible rows and UUID deduplication. */
public final class ScoreboardPageState {
    public static final long REQUEST_TIMEOUT_MILLIS = 5000L;
    private static final int MAX_SNAPSHOT_RESTARTS = 1;

    public enum Reply { IGNORED, ACCEPTED, RESTARTED, FAILED }

    private long requestSequence;
    private long snapshotId;
    private int nextOffset;
    private int totalCount = -1;
    private Request pending;
    private long sentAt;
    private boolean loading;
    private boolean complete;
    private boolean failed;
    private int restarts;

    public void start() {
        reset();
        loading = true;
    }

    public void reset() {
        snapshotId = 0L;
        nextOffset = 0;
        totalCount = -1;
        pending = null;
        loading = false;
        complete = false;
        failed = false;
        restarts = 0;
        // Keep requestSequence across refreshes and reconnects to reject old replies.
    }

    public Request nextRequest(int limit, long now) {
        if (!loading || pending != null) {
            return null;
        }
        pending = new Request(snapshotId, nextOffset, Math.max(1, Math.min(100, limit)), ++requestSequence);
        sentAt = now;
        return pending;
    }

    public Reply accept(long replySnapshotId, int offset, int replyNextOffset, int replyTotal,
                        int entryCount, boolean hasMore, long requestId, boolean expired) {
        if (pending == null || pending.requestId() != requestId || pending.offset() != offset
            || (pending.snapshotId() != 0L && pending.snapshotId() != replySnapshotId)) {
            return Reply.IGNORED;
        }
        if (expired) {
            if (restarts >= MAX_SNAPSHOT_RESTARTS) {
                fail();
                return Reply.FAILED;
            }
            int attempts = restarts + 1;
            start();
            restarts = attempts;
            return Reply.RESTARTED;
        }
        if (replySnapshotId <= 0L || replyTotal < 0 || entryCount < 0
            || replyNextOffset != (long) offset + entryCount || replyNextOffset > replyTotal
            || hasMore != (replyNextOffset < replyTotal) || (hasMore && entryCount == 0)
            || (totalCount >= 0 && totalCount != replyTotal)) {
            fail();
            return Reply.FAILED;
        }
        snapshotId = replySnapshotId;
        nextOffset = replyNextOffset;
        totalCount = replyTotal;
        pending = null;
        complete = !hasMore;
        loading = hasMore;
        return Reply.ACCEPTED;
    }

    public void checkTimeout(long now) {
        if (pending != null && now - sentAt >= REQUEST_TIMEOUT_MILLIS) {
            fail();
        }
    }

    public void fail() {
        pending = null;
        loading = false;
        complete = false;
        failed = true;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean hasFailed() {
        return failed;
    }

    public record Request(long snapshotId, int offset, int limit, long requestId) {
    }
}
