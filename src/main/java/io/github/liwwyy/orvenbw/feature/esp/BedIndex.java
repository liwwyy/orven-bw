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
        // A mismatching orientation invalidates that cached position rather than relabeling a different bed.
        if (!bed.geometry.equals(geometry)) { beds.remove(geometry.foot()); bed = new Bed(geometry, true); beds.put(geometry.foot(), bed); }
        bed.confirmed = true;
        return bed;
    }
    public boolean isDestroyed(BedGeometry geometry, BedTeam team) {
        return destroyed.contains(geometry.foot()) || team != BedTeam.UNKNOWN && destroyedTeams.contains(team);
    }
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
    public void assignScouted(BedGeometry geometry,BedTeam team) {
        if(isDestroyed(geometry,team)) return;
        Bed bed=beds.computeIfAbsent(geometry.foot(),p->new Bed(geometry,false));
        if(!bed.geometry.equals(geometry)) return;
        bed.team=team;bed.teamObserved=team!=BedTeam.UNKNOWN;
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
    public void clear() { beds.clear(); destroyed.clear(); destroyedTeams.clear(); }
}
