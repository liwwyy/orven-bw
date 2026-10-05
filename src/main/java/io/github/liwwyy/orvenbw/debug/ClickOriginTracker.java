package io.github.liwwyy.orvenbw.debug;

import java.util.ArrayDeque;
import java.util.Deque;

/** Follows binding press counts from queue to vanilla consumption and action dispatch. */
public final class ClickOriginTracker {
    public record Origin(long id, String source, String method, String profile, long intended, long queuedAt) {}
    public record Action(long id, Origin origin, String side) {}
    private record Prepared(String source, String method, String profile, long intended) {}
    private final ThreadLocal<Prepared> prepared = new ThreadLocal<>();
    private final Deque<Origin>[] waiting;
    private final Origin[] consumed = new Origin[2];
    private final ClickDebugLog log;
    private long nextId, nextActionId;
    private Action active;

    @SuppressWarnings("unchecked")
    public ClickOriginTracker(ClickDebugLog log) {
        this.log = log;
        waiting = new Deque[]{new ArrayDeque<Origin>(), new ArrayDeque<Origin>()};
    }

    public void prepare(String source, String method, String profile, long intended) {
        prepared.set(new Prepared(source, method, profile, intended));
    }

    public Origin queued(int side, int keyCode, long now) {
        Prepared source = prepared.get();
        prepared.remove();
        if (side < 0 || side > 1) return null;
        if (source == null) source = new Prepared("unknown", "unattributed", null, -1);
        Origin origin = new Origin(++nextId, source.source(), source.method(), source.profile(), source.intended(), now);
        Deque<Origin> queue = waiting[side];
        while (!queue.isEmpty() && now - queue.peekFirst().queuedAt() > 2_000_000_000L) queue.removeFirst();
        if (queue.size() >= 64) queue.removeFirst();
        queue.addLast(origin);
        String button = side == 0 ? "left" : "right";
        if ("artificial".equals(origin.source()))
            log.generated(origin.method(), button, keyCode, origin.profile(),
                    origin.intended() >= 0 ? origin.intended() : now, now, origin.id());
        else log.queued(origin.source(), origin.method(), button, keyCode, origin.id(), now);
        return origin;
    }

    public void consumed(int side) {
        if (side < 0 || side > 1) return;
        consumed[side] = waiting[side].pollFirst();
    }

    public Action beginAction(int side, long now) {
        if (side < 0 || side > 1) return null;
        Origin origin = consumed[side];
        consumed[side] = null;
        if (origin == null) origin = new Origin(0, "unknown", "unattributed", null, -1, now);
        active = new Action(++nextActionId, origin, side == 0 ? "left" : "right");
        log.action(origin.source(), origin.method(), active.side(), origin.id(), active.id(), now);
        return active;
    }

    public void packet(String side, String kind, long now) {
        Action action = active;
        if (action == null || !action.side().equals(side)) return;
        Origin origin = action.origin();
        log.packet(origin.source(), origin.method(), side, kind, origin.id(), action.id(), now);
    }

    public void endAction() { active = null; }
    public void nextTick() { prepared.remove(); consumed[0] = consumed[1] = null; active = null; }
    public void reset() {
        nextTick(); waiting[0].clear(); waiting[1].clear();
    }
}
