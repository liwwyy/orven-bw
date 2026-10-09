package io.github.liwwyy.orvenbw.feature.esp;

import java.util.*;

/** Side-effect-free vanilla projectile integration with swept collision callbacks. */
public final class ProjectilePath {
    public record Point(double x,double y,double z) {
        public Point add(Point p) { return new Point(x+p.x,y+p.y,z+p.z); }
        public Point scale(double n) { return new Point(x*n,y*n,z*n); }
    }
    public enum Kind { ARROW, PEARL, FIREBALL }
    public interface Environment {
        boolean loaded(Point p);
        boolean water(Point p);
        Point collision(Point from,Point to,int tick);
        default Point flow(Point p) { return new Point(0,0,0); }
    }
    public record Result(List<Point> points,Point impact) {}
    public static Result predict(Point origin,Point velocity,Point acceleration,Kind kind,Environment environment) {
        List<Point> points=new ArrayList<>(); points.add(origin);
        Point position=origin;
        for(int tick=0;tick<120;tick++) {
            boolean water=environment.water(position);
            if(water) velocity=velocity.add(environment.flow(position).scale(.014));
            for(int sub=0;sub<4;sub++) {
                Point next=position.add(velocity.scale(.25));
                if(!environment.loaded(next)) return new Result(List.copyOf(points),null);
                Point collision=environment.collision(position,next,tick);
                if(collision!=null) { points.add(collision); return new Result(List.copyOf(points),collision); }
                position=next; points.add(position);
            }
            double drag=water?(kind==Kind.ARROW?.6:.8):(kind==Kind.FIREBALL?.95:.99);
            velocity=kind==Kind.FIREBALL?velocity.add(acceleration).scale(drag):velocity.scale(drag).add(new Point(0,kind==Kind.ARROW?-.05:-.03,0));
            if(!Double.isFinite(velocity.x+velocity.y+velocity.z)) break;
        }
        return new Result(List.copyOf(points),null);
    }
}
