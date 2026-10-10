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
    private final LobbyBedScout scout=new LobbyBedScout();
    private final Map<BedGeometry.Pos,NearestLobbyWool> woolSearches=new LinkedHashMap<>();
    private final Map<BedGeometry.Pos,Long> woolRetries=new HashMap<>();
    private int lastRadius;
    private boolean restored;
    private final Map<Long, WorldChunk> scanned = new HashMap<>();
    private final ArrayDeque<Scan> scans = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    private final ArrayDeque<Egg> eggs = new ArrayDeque<>();
    private final Set<Integer> ownGolems = new HashSet<>();
    private Object world;
    private BedwarsSidebar.Phase phase = BedwarsSidebar.Phase.NONE;
    private String map = "";
    private int ticks;
    private boolean allowed, wasAlerts;
    private static final class Scan {
        final WorldChunk chunk; int section, offset;
        Scan(WorldChunk chunk) { this.chunk = chunk; }
    }
    private record Egg(double x, double y, double z, int tick) {}
    public EspFeature(OrvenConfig config) {
        this.config = config;
        diagnostics=new EspDiagnostics(FabricLoader.getInstance().getConfigDir().resolve("orven-bw/esp-debug.jsonl"),()->config.espDebug);
        renderer = new EspRenderer(config);
    }
    public BedIndex index() { return index; }
    public EspRenderer renderer() { return renderer; }
    public boolean inMatch() { return phase == BedwarsSidebar.Phase.MATCH; }
    @Override public void tick(Minecraft mc) {
        ticks++;
        long now=System.nanoTime();
        if(world!=mc.world) { worldChanged(now);world=mc.world; }
        allowed=mc.world!=null&&mc.player!=null&&ScoreboardGate.allows(mc,config);
        if(!config.modEnabled) { reset();return; }
        if(!config.bedWaypointsEnabled&&!config.bedAlertsEnabled) { scout.clear();clearMatch();return; }
        if(mc.world==null||mc.player==null) { scout.expire(now);return; }
        if(!config.bedAlertsEnabled&&wasAlerts) { alerts.clear();flyingAlerts.clear(); }
        wasAlerts=config.bedAlertsEnabled;
        var sidebar=BedwarsSidebar.parse(sidebar(mc));
        diagnostics.record("scoreboard","phase="+sidebar.phase()+" lines="+sidebar(mc));
        if(sidebar.phase()==BedwarsSidebar.Phase.LOBBY) {
            if(phase!=BedwarsSidebar.Phase.LOBBY || !sidebar.map().equals(map)) { clearMatch();scout.beginLobby(); }
        } else if(phase==BedwarsSidebar.Phase.LOBBY) {
            scout.depart(now);clearMatch();
        }
        if(phase==BedwarsSidebar.Phase.MATCH && sidebar.phase()==BedwarsSidebar.Phase.NONE) { scout.clear();clearMatch(); }
        phase=sidebar.phase();
        if(phase==BedwarsSidebar.Phase.NONE) { scout.expire(now);return; }
        if(!sidebar.map().isEmpty()) map=sidebar.map();
        if(inMatch()&&!restored) {
            if(scout.validate(now,g->matchesBed(mc,g))) {
                for(var entry:scout.matchBeds()) index.assignScouted(entry.geometry(),entry.team());
                restored=true;
                diagnostics.record("lobby-scout-valid","At least one exact bed matches; retaining "+scout.matchBeds().size()+" scouted beds for this match");
            } else if(!scout.pending()) diagnostics.record("lobby-scout-invalid","No lobby bed matched within 1.5 seconds; discarded transfer snapshot");
        }
        if(inMatch()) for(BedTeam team:sidebar.destroyed()) index.broken(team);
        if (ticks % 10 == 0) discover(mc);
        scan(mc, 16384); // At most four non-empty sections per tick, never full-world scans per frame.
        if (ticks % 4 == 0) {
            validateBeds(mc);
            if(phase==BedwarsSidebar.Phase.LOBBY) scoutWool(mc,now);
            index.updateDefence(p -> sample(mc,p));
            for(var bed:index.beds()) {
                if(!bed.teamObserved) diagnostics.record("team:"+bed.geometry.foot(),"No unambiguous nearest lobby wool: team="+bed.team+" confirmed="+bed.confirmed);
                diagnostics.record("defence:"+bed.geometry.foot(),"bed="+bed.geometry+" obsidian="+bed.count()+" samples="+bed.geometry.defence().stream().map(p->p+":"+sample(mc,p)).toList());
            }
            if (allowed && config.bedAlertsEnabled && inMatch()) {
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
        var geometry=new BedGeometry(pos(foot),head.getX()-foot.getX(),head.getZ()-foot.getZ());
        var bed=index.observe(geometry);
        if(bed!=null && phase==BedwarsSidebar.Phase.LOBBY) scout.observe(geometry,bed.team);
    }
    public void blockChanged(Minecraft mc, BlockPos pos) {
        if (!allowed || phase == BedwarsSidebar.Phase.NONE || mc.world == null) return;
        if (mc.world.getBlockState(pos).getBlock() instanceof BedBlock) observe(mc, pos);
        else {
            if(phase==BedwarsSidebar.Phase.LOBBY) for(var bed:List.copyOf(index.beds())) if(bed.geometry.contains(pos(pos))) scout.remove(bed.geometry.foot());
            index.broken(pos(pos));
        }
        // Do not delay confirmation/removal until the next periodic scan.
        index.updateDefence(p -> sample(mc, p));
    }
    private static boolean matchesBed(Minecraft mc,BedGeometry geometry) {
        if(!mc.world.isChunkLoaded(block(geometry.foot()))||!mc.world.isChunkLoaded(block(geometry.head()))) return false;
        var foot=mc.world.getBlockState(block(geometry.foot()));var head=mc.world.getBlockState(block(geometry.head()));
        return foot.getBlock() instanceof BedBlock && head.getBlock() instanceof BedBlock
                && foot.get(BedBlock.PART)==BedBlock.Part.FOOT && head.get(BedBlock.PART)==BedBlock.Part.HEAD
                && block(geometry.foot()).offset(foot.get(HorizontalFacingBlock.FACING)).equals(block(geometry.head()));
    }
    private void validateBeds(Minecraft mc) {
        for (var bed : List.copyOf(index.beds())) {
            var geometry = bed.geometry;
            for (var p : List.of(geometry.foot(), geometry.head())) if (mc.world.isChunkLoaded(block(p))) {
                if (!(mc.world.getBlockState(block(p)).getBlock() instanceof BedBlock)) { if(phase==BedwarsSidebar.Phase.LOBBY) scout.remove(geometry.foot());index.broken(p);break; }
                if (mc.world.isChunkLoaded(block(geometry.foot())) && mc.world.isChunkLoaded(block(geometry.head()))) observe(mc, block(p));
            }
        }
    }
    private void scoutWool(Minecraft mc,long now) {
        int radius=Math.clamp(config.bedWoolRadius,1,32);
        if(radius!=lastRadius) { woolSearches.clear();woolRetries.clear();lastRadius=radius; }
        for(var bed:index.beds()) if(now>=woolRetries.getOrDefault(bed.geometry.foot(),0L))
            woolSearches.computeIfAbsent(bed.geometry.foot(),p->new NearestLobbyWool(bed.geometry,radius));
        int budget=8192;
        for(var entry:List.copyOf(woolSearches.entrySet())) {
            if(budget<=0) break;
            var search=entry.getValue();
            budget-=search.scan(Math.min(2048,budget),p->{
                if(!mc.world.isChunkLoaded(block(p))) return -2;
                var state=mc.world.getBlockState(block(p));
                return state.getBlock()==Blocks.WOOL?state.get(ColoredBlock.COLOR).getId():-1;
            });
            if(!search.complete()) continue;
            var bed=index.beds().stream().filter(b->b.geometry.foot().equals(entry.getKey())).findFirst().orElse(null);
            if(bed!=null) {
                index.assignScouted(bed.geometry,search.team());scout.observe(bed.geometry,search.team());
                diagnostics.record("wool:"+entry.getKey(),"team="+search.team()+" conflicting="+search.conflict()+" unloadedNearer="+search.unknown()+" radius="+radius);
            }
            woolSearches.remove(entry.getKey());woolRetries.put(entry.getKey(),now+1_000_000_000L);
        }
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
    private void clearMatch() { index.clear(); alerts.clear(); flyingAlerts.clear(); scanned.clear(); scans.clear(); queued.clear(); eggs.clear(); ownGolems.clear(); map = ""; restored=false;woolSearches.clear();woolRetries.clear();phase = BedwarsSidebar.Phase.NONE; }
    @Override public void contextLost(boolean worldChanged) { renderer.clearFrame(); if (worldChanged) worldChanged(System.nanoTime()); }
    private void worldChanged(long now) {
        if(phase==BedwarsSidebar.Phase.LOBBY) scout.depart(now);
        else if(inMatch()) scout.clear();
        clearMatch();renderer.clearPlayers();allowed=false;
    }
    @Override public void reset() { scout.clear();clearMatch();renderer.clearPlayers();allowed=false; }
    public void close() { renderer.close(); }
    public static BedGeometry.Pos pos(BlockPos p) { return new BedGeometry.Pos(p.getX(), p.getY(), p.getZ()); }
    public static BlockPos block(BedGeometry.Pos p) { return new BlockPos(p.x(), p.y(), p.z()); }
    private static long chunkKey(int x, int z) { return ((long)x << 32) | (z & 0xffffffffL); }
}
