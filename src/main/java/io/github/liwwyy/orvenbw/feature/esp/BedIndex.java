package io.github.liwwyy.orvenbw.feature.esp;

import java.util.*;
import java.util.function.ToIntFunction;

/** Match-local observations. Unloaded chunks never mean broken beds. */
public final class BedIndex {
    public static final class Bed {
        public final BedGeometry geometry;
        public boolean confirmed;
        public BedTeam team = BedTeam.UNKNOWN;
        public boolean teamObserved;
        public int obsidian, knownDefence;
        private final Map<BedTeam, Set<String>> voters = new EnumMap<>(BedTeam.class);
        private final Map<String, Integer> repeats = new HashMap<>();
        Bed(BedGeometry geometry, boolean confirmed) { this.geometry = geometry; this.confirmed = confirmed; }
        public String count() { return obsidian + (knownDefence == 8 ? "/8" : "/8?"); }
    }
    private final Map<BedGeometry.Pos, Bed> beds = new LinkedHashMap<>();
    private final Set<BedGeometry.Pos> destroyed = new HashSet<>();
    private final Set<BedTeam> destroyedTeams = EnumSet.noneOf(BedTeam.class);
    public Collection<Bed> beds() { return Collections.unmodifiableCollection(beds.values()); }
    public long confirmedCount() { return beds.values().stream().filter(b -> b.confirmed).count(); }
    public Bed observe(BedGeometry geometry) {
        if (destroyed.contains(geometry.foot())) return null;
        Bed bed = beds.computeIfAbsent(geometry.foot(), p -> new Bed(geometry, true));
        // A mismatching orientation invalidates that prediction rather than relabeling a different bed.
        if (!bed.geometry.equals(geometry)) { beds.remove(geometry.foot()); bed = new Bed(geometry, true); beds.put(geometry.foot(), bed); }
        bed.confirmed = true;
        return bed;
    }
    public boolean isDestroyed(BedGeometry geometry, BedTeam team) {
        return destroyed.contains(geometry.foot()) || team != BedTeam.UNKNOWN && destroyedTeams.contains(team);
    }
    public void predict(BedGeometry geometry, BedTeam team) {
        if (isDestroyed(geometry, team)) return;
        Bed bed = beds.computeIfAbsent(geometry.foot(), p -> new Bed(geometry, false));
        if (!bed.confirmed && bed.geometry.equals(geometry)) bed.team = team;
    }
    public void discardPredictions() { beds.values().removeIf(b -> !b.confirmed); }
    public void broken(BedGeometry.Pos pos) {
        Bed found = beds.values().stream().filter(b -> b.geometry.contains(pos)).findFirst().orElse(null);
        if (found != null) { beds.remove(found.geometry.foot()); destroyed.add(found.geometry.foot()); }
    }
    public void broken(BedTeam team) {
        if (team == BedTeam.UNKNOWN) return;
        destroyedTeams.add(team);
        for (Bed bed : List.copyOf(beds.values())) if (bed.team == team) {
            if (bed.teamObserved) broken(bed.geometry.foot());
            else if (!bed.confirmed) beds.remove(bed.geometry.foot());
        }
    }
    public void vote(Bed bed, BedTeam team, String player) {
        if (!bed.confirmed || team == BedTeam.UNKNOWN || bed.teamObserved) return;
        String key = player + ":" + team;
        bed.repeats.merge(key, 1, Integer::sum);
        if (bed.repeats.get(key) < 4) return;
        bed.voters.computeIfAbsent(team, t -> new HashSet<>()).add(player);
    }
    public void resolveTeams() {
        Map<BedTeam, List<Bed>> candidates = new EnumMap<>(BedTeam.class);
        for (Bed bed : beds.values()) if (!bed.teamObserved && bed.voters.size() == 1) {
            BedTeam team = bed.voters.keySet().iterator().next();
            if (beds.values().stream().noneMatch(b -> b != bed && b.teamObserved && b.team == team))
                candidates.computeIfAbsent(team,t -> new ArrayList<>()).add(bed);
        }
        for (var entry : candidates.entrySet()) if (entry.getValue().size() == 1) {
            Bed bed = entry.getValue().getFirst(); bed.team = entry.getKey(); bed.teamObserved = true;
            if (destroyedTeams.contains(bed.team)) broken(bed.team);
        }
    }
    public void updateDefence(ToIntFunction<BedGeometry.Pos> sample) {
        for (Bed bed : beds.values()) {
            bed.obsidian = bed.knownDefence = 0;
            for (var pos : bed.geometry.defence()) {
                int value = sample.applyAsInt(pos); // -1 unloaded, 0 another block, 1 obsidian.
                if (value >= 0) bed.knownDefence++;
                if (value == 1) bed.obsidian++;
            }
        }
    }
    public boolean complete(int expected) { return destroyed.isEmpty() && (expected == 4 || expected == 8) && confirmedCount() == expected; }
    public void clear() { beds.clear(); destroyed.clear(); destroyedTeams.clear(); }
}
