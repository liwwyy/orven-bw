package io.github.liwwyy.orvenbw.feature.esp;

import io.github.liwwyy.orvenbw.OrvenBw;
import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.*;
import io.github.liwwyy.orvenbw.mixin.ClientChunksAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.entity.living.mob.IronGolemEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerUseC2SPacket;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.scoreboard.team.Team;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.*;
import java.util.*;

/** One match-local bed index serves waypoints and alerts, even when a menu is open. */
public final class EspFeature implements ClientFeature {
    private final OrvenConfig config;
    private final EspRenderer renderer;
    private final BedIndex index = new BedIndex();
    private final AlertTracker alerts = new AlertTracker();
    private final Set<Integer> flyingAlerts = new HashSet<>();
    private final EspDiagnostics diagnostics;
    private final BedLayoutCache cache;
    private final Map<Long, WorldChunk> scanned = new HashMap<>();
    private final ArrayDeque<Scan> scans = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    private final ArrayDeque<Egg> eggs = new ArrayDeque<>();
    private final Set<Integer> ownGolems = new HashSet<>();
    private Object world;
    private BedwarsSidebar.Phase phase = BedwarsSidebar.Phase.NONE;
    private String map = "", server = "";
    private int ticks, matchStart, teamCount;
    private boolean allowed, wasAlerts;
    private static final class Scan {
        final WorldChunk chunk; int section, offset;
        Scan(WorldChunk chunk) { this.chunk = chunk; }
    }
    private record Egg(double x, double y, double z, int tick) {}
    public EspFeature(OrvenConfig config) {
        this.config = config;
        diagnostics=new EspDiagnostics(FabricLoader.getInstance().getConfigDir().resolve("orven-bw/esp-debug.jsonl"),()->config.espDebug);
        cache = new BedLayoutCache(FabricLoader.getInstance().getConfigDir().resolve("orven-bw/bed-layouts.json"));
        renderer = new EspRenderer(config);
    }
    public BedIndex index() { return index; }
    public EspRenderer renderer() { return renderer; }
    public boolean inMatch() { return phase == BedwarsSidebar.Phase.MATCH; }
    @Override public void tick(Minecraft mc) {
        ticks++;
        if (world != mc.world) { reset(); world = mc.world; }
        allowed = mc.world != null && mc.player != null && ScoreboardGate.allows(mc, config);
        if (!allowed) { if (phase != BedwarsSidebar.Phase.NONE) clearMatch(); renderer.clearPlayers(); return; }
        if (!config.bedAlertsEnabled && wasAlerts) { alerts.clear(); flyingAlerts.clear(); }
        wasAlerts = config.bedAlertsEnabled;
        if (!config.bedWaypointsEnabled && !config.bedAlertsEnabled) { clearMatch(); return; }
        var sidebar = BedwarsSidebar.parse(sidebar(mc));
        diagnostics.record("scoreboard","phase="+sidebar.phase()+" lines="+sidebar(mc));
        if (sidebar.phase() == BedwarsSidebar.Phase.NONE) { clearMatch(); return; }
        if ((phase == BedwarsSidebar.Phase.MATCH && sidebar.phase() == BedwarsSidebar.Phase.LOBBY)
                || !sidebar.map().isEmpty() && !map.isEmpty() && !map.equals(sidebar.map())) clearMatch();
        if (phase != BedwarsSidebar.Phase.MATCH && sidebar.phase() == BedwarsSidebar.Phase.MATCH) matchStart = ticks;
        phase = sidebar.phase();
        if (!sidebar.map().isEmpty()) map = sidebar.map();
        server = mc.getCurrentServerEntry() == null ? "local" : mc.getCurrentServerEntry().ip.toLowerCase(Locale.ROOT);
        if (sidebar.teams() != 0) teamCount = sidebar.teams();
        if(inMatch()) for (BedTeam team : sidebar.destroyed()) index.broken(team);
        if (ticks % 10 == 0) discover(mc);
        scan(mc, 16384); // At most four non-empty sections per tick, never full-world scans per frame.
        if (ticks % 4 == 0) {
            validateBeds(mc);
            if (phase == BedwarsSidebar.Phase.MATCH && ticks - matchStart <= 300) associateTeams(mc);
            if (teamCount == 0 && index.confirmedCount() == 8) teamCount = 8;
            cache.learn(server, map, teamCount, index);
            if (config.bedPredict) {
                var layout = cache.match(server, map, teamCount, index.beds()).filter(l -> l.beds().stream().allMatch(e ->
                        index.isDestroyed(e.geometry(), e.team()) || compatible(mc,e.geometry())));
                if(layout.isEmpty()) diagnostics.record("layout-unmatched","No unique compatible cached layout: server="+server+" map="+map+" teamCount="+teamCount+" confirmedBeds="+index.confirmedCount()+" loadedChunks="+scanned.size());
                index.discardPredictions();
                layout.ifPresent(l -> { for (var entry : l.beds()) {
                    var observed = index.beds().stream().filter(b -> b.geometry.equals(entry.geometry())).findFirst();
                    if (observed.isPresent()) {
                        if (!observed.get().teamObserved) observed.get().team = entry.team();
                    } else index.predict(entry.geometry(), entry.team());
                }});
                validateBeds(mc);
            } else index.discardPredictions();
            index.updateDefence(p -> sample(mc,p));
            for(var bed:index.beds()) {
                if(!bed.teamObserved) diagnostics.record("team:"+bed.geometry.foot(),"No confirmed spawn/team association: team="+bed.team+" confirmed="+bed.confirmed+" votes require slow tab-listed players near the bed during the first 15 seconds");
                diagnostics.record("defence:"+bed.geometry.foot(),"bed="+bed.geometry+" obsidian="+bed.count()+" samples="+bed.geometry.defence().stream().map(p->p+":"+sample(mc,p)).toList());
            }
            if (config.bedAlertsEnabled && inMatch()) {
                playerAlerts(mc);
                projectileAlerts(mc);
                if (config.bedAlertPlacedObsidian) for (var bed : index.beds())
                    if (bed.confirmed && alerts.defence(bed)) {
                        BedTeam team = bed.teamObserved ? bed.team : BedTeam.UNKNOWN;
                        alert(mc, "§" + team.code + team.label + "§r bed defence: §5Obsidian §r(" + bed.count() + ").");
                    }
            }
            trackGolems(mc);
        }
    }
    private static List<String> sidebar(Minecraft mc) {
        var board = mc.world.getScoreboard();
        var team = board.getTeamOfMember(mc.player.getName());
        var objective = team == null || team.getColor() == null || team.getColor().getId() < 0 ? null : board.getDisplayObjective(3 + team.getColor().getId());
        if (objective == null) objective = board.getDisplayObjective(1);
        if (objective == null) return List.of();
        List<String> result = new ArrayList<>(); result.add(objective.getDisplayName());
        List<ScoreboardScore> scores = new ArrayList<>();
        for (var score : board.getScores(objective)) if (score.getOwner() != null && !score.getOwner().startsWith("#")) scores.add(score);
        for (int i = Math.max(0, scores.size() - 15); i < scores.size(); i++) {
            String owner = scores.get(i).getOwner(); result.add(Team.getMemberDisplayName(board.getTeamOfMember(owner), owner));
        }
        return result;
    }
    private void discover(Minecraft mc) {
        if (!(mc.world.getChunkSource() instanceof ClientChunksAccessor source)) return;
        Set<Long> loaded = new HashSet<>();
        for (WorldChunk chunk : source.orven$loadedChunks()) {
            long key = chunkKey(chunk.chunkX, chunk.chunkZ); loaded.add(key);
            if (scanned.get(key) != chunk && queued.add(key)) scans.addLast(new Scan(chunk));
        }
        scanned.keySet().retainAll(loaded);
    }
    public void chunkChanged(Minecraft mc, int x, int z) {
        long key = chunkKey(x, z); scanned.remove(key);
        if (!allowed || mc.world == null || !mc.world.getChunkSource().hasChunk(x, z)) return;
        if (queued.add(key)) scans.addLast(new Scan(mc.world.getChunkAt(x, z)));
        else for (Scan scan : scans) if (chunkKey(scan.chunk.chunkX,scan.chunk.chunkZ) == key) { scan.section = 0; scan.offset = 0; break; }
    }
    private void scan(Minecraft mc, int budget) {
        while (budget > 0 && !scans.isEmpty()) {
            Scan scan = scans.peekFirst(); WorldChunk chunk = scan.chunk;
            long key = chunkKey(chunk.chunkX, chunk.chunkZ);
            if (!mc.world.getChunkSource().hasChunk(chunk.chunkX, chunk.chunkZ) || mc.world.getChunkAt(chunk.chunkX, chunk.chunkZ) != chunk) {
                scans.removeFirst(); queued.remove(key); continue;
            }
            var sections = chunk.getSections();
            while (scan.section < sections.length && (sections[scan.section] == null || sections[scan.section].isEmpty())) { scan.section++; scan.offset = 0; }
            if (scan.section >= sections.length) { scanned.put(key, chunk); scans.removeFirst(); queued.remove(key); continue; }
            var section = sections[scan.section];
            int end = Math.min(4096, scan.offset + budget); budget -= end - scan.offset;
            for (; scan.offset < end; scan.offset++) {
                int x = scan.offset & 15, z = (scan.offset >> 4) & 15, y = scan.offset >> 8;
                if (section.getBlock(x, y, z) instanceof BedBlock) observe(mc, new BlockPos(chunk.chunkX * 16 + x, section.getOffsetY() + y, chunk.chunkZ * 16 + z));
            }
            if (scan.offset == 4096) { scan.section++; scan.offset = 0; }
        }
    }
    private void observe(Minecraft mc, BlockPos pos) {
        var state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return;
        var facing = state.get(HorizontalFacingBlock.FACING);
        var foot = state.get(BedBlock.PART) == BedBlock.Part.HEAD ? pos.offset(facing.getOpposite()) : pos;
        var head = foot.offset(facing);
        // A single half can arrive first at a chunk boundary. Recheck on later discovery/update.
        if (!mc.world.isChunkLoaded(foot) || !mc.world.isChunkLoaded(head)
                || !(mc.world.getBlockState(foot).getBlock() instanceof BedBlock)
                || !(mc.world.getBlockState(head).getBlock() instanceof BedBlock)) return;
        index.observe(new BedGeometry(pos(foot), head.getX() - foot.getX(), head.getZ() - foot.getZ()));
    }
    public void blockChanged(Minecraft mc, BlockPos pos) {
        if (!allowed || phase == BedwarsSidebar.Phase.NONE || mc.world == null) return;
        if (mc.world.getBlockState(pos).getBlock() instanceof BedBlock) observe(mc, pos);
        else index.broken(pos(pos));
        // Do not delay confirmation/removal until the next periodic scan.
        index.updateDefence(p -> sample(mc, p));
    }
    private static boolean compatible(Minecraft mc, BedGeometry geometry) {
        for (var p : List.of(geometry.foot(),geometry.head())) if (mc.world.isChunkLoaded(block(p))) {
            var state = mc.world.getBlockState(block(p));
            if (!(state.getBlock() instanceof BedBlock)) return false;
            var facing = state.get(HorizontalFacingBlock.FACING);
            var expected = block(geometry.foot()).offset(facing);
            if (!expected.equals(block(geometry.head()))) return false;
        }
        return true;
    }
    private void validateBeds(Minecraft mc) {
        for (var bed : List.copyOf(index.beds())) {
            var geometry = bed.geometry;
            for (var p : List.of(geometry.foot(), geometry.head())) if (mc.world.isChunkLoaded(block(p))) {
                if (!(mc.world.getBlockState(block(p)).getBlock() instanceof BedBlock)) { index.broken(p); break; }
                if (mc.world.isChunkLoaded(block(geometry.foot())) && mc.world.isChunkLoaded(block(geometry.head()))) observe(mc, block(p));
            }
        }
    }
    private void associateTeams(Minecraft mc) {
        for (PlayerEntity player : mc.world.players) {
            if (!player.isAlive() || player.isSpectator() || !EspPlayers.listed(mc, player)) continue;
            BedTeam team = BedTeam.fromColor(EspPlayers.color(player));
            if (team == BedTeam.UNKNOWN) { diagnostics.record("team-colour:"+player.getName(),"No usable team/nametag colour; player="+player.getName()+" team="+player.getScoreboardTeam()); continue; }
            // Only slow, early spawn observations near a base. A rushing opponent cannot relabel a bed.
            if (Math.hypot(player.x - player.prevX, player.z - player.prevZ) > .35) continue;
            var nearby = index.beds().stream().filter(b -> b.confirmed && Math.abs(player.y - b.geometry.y()) < 5)
                    .sorted(Comparator.comparingDouble(b -> b.geometry.distanceSquared(player.x, player.y, player.z))).toList();
            if (nearby.isEmpty()) continue;
            var bed = nearby.getFirst(); double distance = bed.geometry.distanceSquared(player.x, player.y, player.z);
            if (distance > 144 || nearby.size() > 1 && nearby.get(1).geometry.distanceSquared(player.x, player.y, player.z) < distance * 2) continue;
            index.vote(bed, team, player.getName());
        }
        index.resolveTeams();
    }
    private static int sample(Minecraft mc, BedGeometry.Pos p) {
        return !mc.world.isChunkLoaded(block(p)) ? -1 : mc.world.getBlockState(block(p)).getBlock() == Blocks.OBSIDIAN ? 1 : 0;
    }
    private void playerAlerts(Minecraft mc) {
        for (PlayerEntity player : mc.world.players) {
            if (player == mc.player || !player.isAlive() || config.bedAlertIgnoreNpcs && !EspPlayers.listed(mc, player)) continue;
            String name = player.getName(), display = player.getDisplayName().getFormattedString();
            ItemStack leggings = player.getArmor(1);
            if (config.bedAlertArmor && leggings != null && leggings.getItem() == Items.DIAMOND_LEGGINGS && alerts.armor(name)) alert(mc, display + "§r has purchased §bDiamond Armour§r.");
            ItemStack hand = player.getItemInHand();
            Item item = hand == null ? null : hand.getItem();
            String type = item == Items.ENDER_PEARL ? "Ender Pearl" : item == Items.FIRE_CHARGE ? "Fireball"
                    : item == Items.BOW ? "Bow" : item == Items.STICK ? "Stick" : item == Item.byBlock(Blocks.OBSIDIAN) ? "Obsidian" : null;
            boolean selected = type != null && switch (type) { case "Ender Pearl" -> config.bedAlertPearl; case "Fireball" -> config.bedAlertFireball && (!config.bedAlertFireballVisible || PlayerVisibility.visible(mc,player)); case "Bow" -> config.bedAlertBow; case "Stick" -> config.bedAlertStick; default -> config.bedAlertHeldObsidian; };
            if (alerts.held(name, selected ? type : null)) alert(mc, display + "§r is holding §e" + type + "§r (" + Math.round(player.distanceTo(mc.player)) + "m).");
        }
    }
    private void projectileAlerts(Minecraft mc) {
        Set<Integer> alive=new HashSet<>();
        for(var entity:mc.world.getEntities()) {
            boolean fireball=entity instanceof net.minecraft.entity.projectile.FireballEntity || entity instanceof net.minecraft.entity.projectile.SmallFireballEntity;
            boolean arrow=entity instanceof net.minecraft.entity.projectile.ArrowEntity && !((io.github.liwwyy.orvenbw.mixin.ArrowEntityAccessor)entity).orven$inGround();
            if(!fireball&&!arrow) continue;
            alive.add(entity.getNetworkId());
            if((fireball?config.bedAlertFlyingFireball:config.bedAlertArrow) && flyingAlerts.add(entity.getNetworkId())) alert(mc,"§e"+(fireball?"Fireball":"Arrow")+"§r in flight ("+Math.round(entity.distanceTo(mc.player))+"m).");
        }
        flyingAlerts.retainAll(alive);
    }
    private void alert(Minecraft mc, String message) {
        mc.gui.getChat().addMessage(new LiteralText("§8[§borven-bw§8] §r" + message));
        if (config.bedAlertSound) mc.getSoundManager().play(net.minecraft.client.sound.instance.SimpleSoundInstance.of(new net.minecraft.resource.Identifier("orvenbw:warning"+(config.bedWarningSound==1?2:1))));
    }
    public void chat(String message) {
        if (allowed && inMatch()) index.broken(BedDestructionMessage.parse(message));
    }
    public void outgoing(PlayerUseC2SPacket packet) {
        if (!allowed || !config.bedAlertsEnabled || !inMatch() || packet.getFace() == 255 || packet.getItemInHand() == null) return;
        var stack = packet.getItemInHand();
        if (stack.getItem() == Items.SPAWN_EGG && net.minecraft.entity.Entities.getType(stack.getMetadata()) == IronGolemEntity.class) {
            BlockPos p = packet.getPos(); eggs.addLast(new Egg(p.getX() + .5, p.getY() + 1, p.getZ() + .5, ticks));
        }
    }
    private void trackGolems(Minecraft mc) {
        eggs.removeIf(e -> ticks - e.tick > 60);
        if (eggs.isEmpty()) return;
        for (var entity : mc.world.getEntities()) if (entity instanceof IronGolemEntity && !ownGolems.contains(entity.getNetworkId())) {
            var match = eggs.stream().filter(e -> Math.pow(entity.x - e.x, 2) + Math.pow(entity.y - e.y, 2) + Math.pow(entity.z - e.z, 2) <= 9).findFirst();
            match.ifPresent(e -> { eggs.remove(e); ownGolems.add(entity.getNetworkId()); });
        }
    }
    public boolean isOwnGolem(int networkId) { return ownGolems.contains(networkId); }
    public void captureTerrain(Minecraft mc) {
        if(allowed && mc.world==world && ScoreboardGate.allows(mc,config) && mc.screen==null && !mc.options.hideGui && !mc.isPaused()) renderer.captureTerrain(mc);
    }
    public void renderWorld(Minecraft mc, float delta) {
        if (allowed && mc.world == world && ScoreboardGate.allows(mc,config) && mc.screen == null && !mc.options.hideGui && !mc.isPaused()) renderer.renderWorld(mc, delta, index, phase != BedwarsSidebar.Phase.NONE);
        else renderer.clearFrame();
    }
    public void renderHud(Minecraft mc) {
        if (allowed && mc.world == world && ScoreboardGate.allows(mc,config) && mc.screen == null && !mc.options.hideGui && !mc.isPaused()) renderer.renderHud(mc, index, phase != BedwarsSidebar.Phase.NONE);
    }
    private void clearMatch() { index.clear(); alerts.clear(); flyingAlerts.clear(); scanned.clear(); scans.clear(); queued.clear(); eggs.clear(); ownGolems.clear(); map = ""; teamCount = 0; phase = BedwarsSidebar.Phase.NONE; }
    @Override public void contextLost(boolean worldChanged) { renderer.clearFrame(); if (worldChanged) reset(); }
    @Override public void reset() { clearMatch(); renderer.clearPlayers(); allowed = false; }
    public void close() { renderer.close(); }
    public static BedGeometry.Pos pos(BlockPos p) { return new BedGeometry.Pos(p.getX(), p.getY(), p.getZ()); }
    public static BlockPos block(BedGeometry.Pos p) { return new BlockPos(p.x(), p.y(), p.z()); }
    private static long chunkKey(int x, int z) { return ((long)x << 32) | (z & 0xffffffffL); }
}
