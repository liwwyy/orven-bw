package io.github.liwwyy.orvenbw.feature.esp;

import java.util.*;
import java.util.function.ToIntFunction;

/** Budgeted nearest-first spherical search. -1 is known non-wool; -2 is unloaded terrain. */
public final class NearestLobbyWool {
    private record Offset(int x,int y,int z,double distance) {}
    private record Key(int radius,int dx,int dz) {}
    private static final Map<Key,List<Offset>> ORDERS=new LinkedHashMap<>();
    private final BedGeometry geometry;
    private final List<Offset> offsets;
    private int cursor, nearestColour=-1;
    private double best=Double.POSITIVE_INFINITY;
    private BedTeam team=BedTeam.UNKNOWN;
    private boolean conflict,unknown,complete;
    public NearestLobbyWool(BedGeometry geometry,int requestedRadius) {
        this.geometry=geometry;
        int radius=Math.clamp(requestedRadius,1,32);
        Key key=new Key(radius,geometry.dx(),geometry.dz());
        offsets=ORDERS.computeIfAbsent(key,k->{
            List<Offset> values=new ArrayList<>();
            for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++) {
                double d=Math.pow(x-geometry.dx()*.5,2)+y*y+Math.pow(z-geometry.dz()*.5,2);
                if(d<=radius*radius) values.add(new Offset(x,y,z,d));
            }
            values.sort(Comparator.comparingDouble(Offset::distance));return List.copyOf(values);
        });
        if(ORDERS.size()>8) ORDERS.remove(ORDERS.keySet().iterator().next());
    }
    public int scan(int budget,ToIntFunction<BedGeometry.Pos> sample) {
        int used=0;
        while(!complete&&used<budget&&cursor<offsets.size()) {
            var offset=offsets.get(cursor++);
            if(offset.distance()>best) { complete=true;break; }
            int wool=sample.applyAsInt(geometry.foot().add(offset.x(),offset.y(),offset.z()));used++;
            if(wool==-2) unknown=true;
            if(wool<0) continue;
            BedTeam color=fromWool(wool);
            if(best==Double.POSITIVE_INFINITY) { best=offset.distance();team=color;nearestColour=wool; }
            else if(wool!=nearestColour) conflict=true;
        }
        if(cursor>=offsets.size()) complete=true;
        return used;
    }
    public boolean complete() { return complete; }
    public boolean conflict() { return conflict; }
    public boolean unknown() { return unknown; }
    public BedTeam team() { return complete&&!conflict&&!unknown?team:BedTeam.UNKNOWN; }
    public static BedTeam fromWool(int metadata) {
        return switch(metadata) {
            case 0->BedTeam.WHITE;case 2,6->BedTeam.PINK;case 3,9->BedTeam.AQUA;
            case 4->BedTeam.YELLOW;case 5,13->BedTeam.GREEN;case 7,8->BedTeam.GRAY;
            case 11->BedTeam.BLUE;case 14->BedTeam.RED;default->BedTeam.UNKNOWN;
        };
    }
}
