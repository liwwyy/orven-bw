package io.github.liwwyy.orvenbw.feature.esp;

import java.util.*;
import java.util.function.Predicate;

/** One lobby snapshot, a 1.5s transfer deadline, then match-scoped positions. Never persisted to disk. */
public final class LobbyBedScout {
    public static final long TRANSFER_NANOS=1_500_000_000L;
    public record Entry(BedGeometry geometry,BedTeam team) {}
    private final Map<BedGeometry.Pos,Entry> lobby=new LinkedHashMap<>();
    private Map<BedGeometry.Pos,Entry> match=Map.of();
    private boolean scouting,pending;
    private long deadline;
    public void beginLobby() { clear();scouting=true; }
    public void observe(BedGeometry geometry,BedTeam team) {
        if(scouting) lobby.put(geometry.foot(),new Entry(geometry,team));
    }
    public void remove(BedGeometry.Pos foot) { if(scouting) lobby.remove(foot); }
    public void depart(long now) {
        if(!scouting) return;
        scouting=false;pending=!lobby.isEmpty();deadline=now+TRANSFER_NANOS;
        match=Map.of();
    }
    public boolean validate(long now,Predicate<BedGeometry> matches) {
        if(!pending) return !match.isEmpty();
        if(now>=deadline) { lobby.clear();pending=false;return false; }
        if(lobby.values().stream().noneMatch(e->matches.test(e.geometry()))) return false;
        match=Collections.unmodifiableMap(new LinkedHashMap<>(lobby));lobby.clear();pending=false;
        return true;
    }
    public Collection<Entry> matchBeds() { return match.values(); }
    public boolean pending() { return pending; }
    public void expire(long now) { if(pending&&now>=deadline) { pending=false;lobby.clear(); } }
    public void clear() { lobby.clear();match=Map.of();scouting=pending=false; }
}
