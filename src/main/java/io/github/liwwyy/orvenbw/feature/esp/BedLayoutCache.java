package io.github.liwwyy.orvenbw.feature.esp;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

/** Only stores observed full layouts; no universal colour/geometry formula is assumed. */
public final class BedLayoutCache {
    public record Entry(BedGeometry geometry, BedTeam team) {}
    public record Layout(String server, String map, int teams, List<Entry> beds) {}
    private final List<Layout> layouts = new ArrayList<>();
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public BedLayoutCache(Path file) {
        this.file = file;
        if (file == null || !Files.isRegularFile(file)) return;
        try {
            Layout[] saved = gson.fromJson(Files.readString(file), Layout[].class);
            if (saved != null) for (Layout layout : saved) if (valid(layout)) layouts.add(layout);
        } catch (IOException | RuntimeException error) { io.github.liwwyy.orvenbw.OrvenBw.LOGGER.warn("Ignoring invalid bed layout cache", error); }
    }
    private static boolean valid(Layout l) {
        if (l == null || l.server == null || l.map == null || l.map.isBlank() || l.beds == null
                || (l.teams != 4 && l.teams != 8) || l.beds.size() != l.teams) return false;
        Set<BedGeometry.Pos> positions = new HashSet<>();
        for (Entry e : l.beds) if (e == null || e.geometry == null || e.geometry.foot() == null || e.team == null || !positions.add(e.geometry.foot())) return false;
        return true;
    }
    public void learn(String server, String map, int teams, BedIndex index) {
        if (map == null || map.isBlank() || !index.complete(teams)) return;
        var observed = index.beds().stream().filter(b -> b.confirmed).map(b -> new Entry(b.geometry, b.teamObserved ? b.team : BedTeam.UNKNOWN)).toList();
        Set<BedGeometry> geometry = new HashSet<>(observed.stream().map(Entry::geometry).toList());
        var prior = layouts.stream().filter(l -> l.server.equals(server) && l.map.equals(map) && l.teams == teams
                && new HashSet<>(l.beds.stream().map(Entry::geometry).toList()).equals(geometry)).findFirst();
        // A new match's still-unassigned players must not erase earlier observed colour evidence.
        var entries = observed.stream().map(e -> e.team != BedTeam.UNKNOWN ? e : prior
                .flatMap(l -> l.beds.stream().filter(p -> p.geometry.equals(e.geometry)).findFirst()).orElse(e)).toList();
        Layout layout = new Layout(server, map, teams, entries);
        if (layouts.contains(layout)) return;
        layouts.removeIf(l -> l.server.equals(server) && l.map.equals(map) && l.teams == teams && new HashSet<>(l.beds.stream().map(Entry::geometry).toList()).equals(geometry));
        layouts.add(layout);
        if (layouts.size() > 128) layouts.removeFirst();
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, gson.toJson(layouts), StandardCharsets.UTF_8);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException error) { io.github.liwwyy.orvenbw.OrvenBw.LOGGER.warn("Could not save bed layouts", error); }
    }
    public Optional<Layout> match(String server, String map, int teams, Collection<BedIndex.Bed> observations) {
        List<BedIndex.Bed> observed = observations.stream().filter(b -> b.confirmed).toList();
        if (observed.isEmpty() || map == null || map.isBlank()) return Optional.empty();
        var matches = layouts.stream().filter(l -> l.server.equals(server) && l.map.equals(map) && (teams == 0 || l.teams == teams))
                .filter(l -> observed.stream().allMatch(b -> l.beds.stream().anyMatch(e -> e.geometry.equals(b.geometry)
                        && (b.team == BedTeam.UNKNOWN || e.team == BedTeam.UNKNOWN || b.team == e.team)))).toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }
}
