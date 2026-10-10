package io.github.liwwyy.orvenbw.feature.esp;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.*;
import io.github.liwwyy.orvenbw.mixin.ArrowEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.*;
import net.minecraft.client.render.platform.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.*;
import net.minecraft.util.math.*;
import net.minecraft.block.material.Material;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import java.util.*;

/** Raven's five-tick approach filter, configurable HUD pointers, and bounded cached trajectories. */
public final class IndicatorsFeature implements ClientFeature,AutoCloseable {
    private final OrvenConfig config;
    private final IndicatorFont font=new IndicatorFont();
    private final Map<Integer,ProjectilePath.Point> previous=new HashMap<>();
    private final List<Tracked> tracked=new ArrayList<>();
    private float[] view,projection;
    private double cx,cy,cz;
    private int ticks,width,height;
    private Object world;
    private record Tracked(Entity entity,Item item,int color,boolean approaching,ProjectilePath.Result path) {}
    public IndicatorsFeature(OrvenConfig config) { this.config=config; }
    private boolean ready(Minecraft mc) { return config.indicatorsEnabled&&ScoreboardGate.allows(mc,config)&&mc.world!=null&&mc.player!=null&&mc.screen==null&&!mc.options.hideGui&&!mc.isPaused(); }
    private Item item(Entity e) {
        if(e instanceof ArrowEntity && config.indicatorArrows && !((ArrowEntityAccessor)e).orven$inGround()) return Items.ARROW;
        if(e instanceof EnderPearlEntity && config.indicatorPearls) return Items.ENDER_PEARL;
        if((e instanceof FireballEntity || e instanceof SmallFireballEntity) && config.indicatorFireballs) return Items.FIRE_CHARGE;
        if(e instanceof EggEntity && config.indicatorEggs) return Items.EGG;
        if(e instanceof SnowballEntity && config.indicatorSnowballs) return Items.SNOWBALL;
        return null;
    }
    private int color(Item item) { return !config.indicatorColors?0xffffffff:item==Items.ENDER_PEARL?0xffd200ff:item==Items.FIRE_CHARGE?0xffff9600:item==Items.EGG?0xffffee9a:0xffffffff; }
    @Override public void tick(Minecraft mc) {
        if(world!=mc.world) { reset(); world=mc.world; }
        if(!ready(mc)) { reset(); return; }
        if(++ticks%5!=1) return;
        List<Entity> entities=new ArrayList<>();
        for(var e:mc.world.getEntities()) if(!e.removed&&item(e)!=null) entities.add(e);
        entities.sort(Comparator.comparingDouble(e->mc.player.squaredDistanceTo(e)));
        Set<Integer> present=new HashSet<>(); tracked.clear(); int predicted=0;
        for(var e:entities) {
            Item item=item(e); int id=e.getNetworkId(); present.add(id);
            var point=new ProjectilePath.Point(e.x,e.y,e.z); var old=previous.put(id,point);
            boolean approaching=old!=null&&distance(old,mc.player)-distance(point,mc.player)>1;
            boolean path=item==Items.ARROW?config.indicatorArrowPath:item==Items.ENDER_PEARL?config.indicatorPearlPath:item==Items.FIRE_CHARGE&&config.indicatorFireballPath;
            tracked.add(new Tracked(e,item,color(item),approaching,path&&predicted++<32?predict(mc,e):null));
        }
        previous.keySet().retainAll(present);
    }
    private static double distance(ProjectilePath.Point p,Entity e) { return Math.sqrt(Math.pow(p.x()-e.x,2)+Math.pow(p.y()-e.y,2)+Math.pow(p.z()-e.z,2)); }
    private ProjectilePath.Result predict(Minecraft mc,Entity entity) {
        var kind=entity instanceof ArrowEntity?ProjectilePath.Kind.ARROW:entity instanceof ProjectileEntity?ProjectilePath.Kind.FIREBALL:ProjectilePath.Kind.PEARL;
        var acceleration=entity instanceof ProjectileEntity p?new ProjectilePath.Point(p.accelerationX,p.accelerationY,p.accelerationZ):new ProjectilePath.Point(0,0,0);
        Entity owner=entity instanceof ArrowEntity a?a.shooter:entity instanceof ProjectileEntity p?p.shooter:entity instanceof ThrownEntity t?t.getThrower():null;
        return ProjectilePath.predict(new ProjectilePath.Point(entity.x,entity.y,entity.z),new ProjectilePath.Point(entity.velocityX,entity.velocityY,entity.velocityZ),acceleration,kind,new ProjectilePath.Environment() {
            public boolean loaded(ProjectilePath.Point p) { return p.y()>=0&&p.y()<256&&mc.world.isChunkLoaded(new BlockPos(p.x(),p.y(),p.z())); }
            private ProjectilePath.Point fluidPosition,flow=new ProjectilePath.Point(0,0,0);
            private boolean inWater;
            private void sampleFluid(ProjectilePath.Point p) {
                if(p.equals(fluidPosition)) return; fluidPosition=p; inWater=false;
                double fx=0,fy=0,fz=0,radius=entity.width/2;
                int minX=(int)Math.floor(p.x()-radius+.001),maxX=(int)Math.floor(p.x()+radius+1-.001);
                int minY=(int)Math.floor(p.y()+.401),maxY=(int)Math.floor(p.y()+entity.height+.599);
                int minZ=(int)Math.floor(p.z()-radius+.001),maxZ=(int)Math.floor(p.z()+radius+1-.001);
                for(int x=minX;x<maxX;x++) for(int y=minY;y<maxY;y++) for(int z=minZ;z<maxZ;z++) {
                    var pos=new BlockPos(x,y,z); if(!mc.world.isChunkLoaded(pos)) continue;
                    var blockState=mc.world.getBlockState(pos);
                    if(blockState.getBlock().getMaterial()!=Material.WATER) continue;
                    double surface=y+1-net.minecraft.block.LiquidBlock.getHeightLoss(blockState.get(net.minecraft.block.LiquidBlock.LEVEL));
                    if(maxY<surface) continue; inWater=true;
                    // Verified vanilla method returns an added flow vector; it does not mutate the entity.
                    var v=blockState.getBlock().applyMaterialDrag(mc.world,pos,entity,new Vec3d(0,0,0)); fx+=v.x;fy+=v.y;fz+=v.z;
                }
                double length=Math.sqrt(fx*fx+fy*fy+fz*fz);flow=length>0?new ProjectilePath.Point(fx/length,fy/length,fz/length):new ProjectilePath.Point(0,0,0);
            }
            public boolean water(ProjectilePath.Point p) { sampleFluid(p);return inWater; }
            public ProjectilePath.Point flow(ProjectilePath.Point p) { sampleFluid(p);return flow; }
            private ProjectilePath.Bounds impactBounds;
            private int impactEntityId=-1;
            public ProjectilePath.Bounds impactBounds() { return impactBounds; }
            public int impactEntityId() { return impactEntityId; }
            public ProjectilePath.Point collision(ProjectilePath.Point from,ProjectilePath.Point to,int tick) {
                impactBounds=null;impactEntityId=-1;
                Vec3d a=vec(from),b=vec(to); var hit=mc.world.rayTrace(a,b,false,true,false);
                Vec3d nearest=hit==null?null:hit.facePos;
                if(hit!=null && hit.getPos()!=null) { var pos=hit.getPos();impactBounds=new ProjectilePath.Bounds(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+1,pos.getZ()+1); }
                double best=nearest==null?Double.POSITIVE_INFINITY:a.squaredDistanceTo(nearest);
                double radius=kind==ProjectilePath.Kind.FIREBALL?entity.width/2:0;
                Box sweep=new Box(Math.min(from.x(),to.x())-radius,Math.min(from.y(),to.y())-radius,Math.min(from.z(),to.z())-radius,Math.max(from.x(),to.x())+radius,Math.max(from.y(),to.y())+radius,Math.max(from.z(),to.z())+radius);
                for(Box box:mc.world.getBlockCollisions(sweep.grown(.01,.01,.01))) {
                    var shape=box.grown(radius,radius,radius); var clip=shape.contains(a)?a:shape.clip(a,b)==null?null:shape.clip(a,b).facePos;
                    if(clip!=null&&a.squaredDistanceTo(clip)<=best) { nearest=clip; best=a.squaredDistanceTo(clip); impactBounds=bounds(box);impactEntityId=-1; }
                }
                for(var target:mc.world.getEntities(entity,sweep.grown(1,1,1))) {
                    if(!(target instanceof LivingEntity)||!target.isAlive()||!target.hasCollision()||(target==owner&&entity.ticks+tick<(kind==ProjectilePath.Kind.FIREBALL?25:5))||target instanceof PlayerEntity p&&!EspPlayers.listed(mc,p)) continue;
                    var shape=target.getShape().grown(.3+radius,.3+radius,.3+radius); var clip=shape.contains(a)?a:shape.clip(a,b)==null?null:shape.clip(a,b).facePos;
                    if(clip!=null&&a.squaredDistanceTo(clip)<best) { nearest=clip; best=a.squaredDistanceTo(clip);impactBounds=bounds(target.getShape());impactEntityId=target.getNetworkId(); }
                }
                return nearest==null?null:new ProjectilePath.Point(nearest.x,nearest.y,nearest.z);
            }
        });
    }
    private static Vec3d vec(ProjectilePath.Point p) { return new Vec3d(p.x(),p.y(),p.z()); }
    public void renderWorld(Minecraft mc,float delta) {
        view=projection=null; if(!ready(mc)) return;
        Entity camera=mc.getCamera(); if(camera==null) return;
        cx=camera.prevX+(camera.x-camera.prevX)*delta;cy=camera.prevY+(camera.y-camera.prevY)*delta;cz=camera.prevZ+(camera.z-camera.prevZ)*delta;
        var window=new Window(mc);width=window.getWidth();height=window.getHeight();view=matrix(GL11.GL_MODELVIEW_MATRIX);projection=matrix(GL11.GL_PROJECTION_MATRIX);
        try(var ignored=new EspGlState()) {
            state();
            for(var t:tracked) if(!t.entity.removed&&(!config.indicatorApproaching||t.approaching)&&t.path!=null) {
                tint(t.color);GL11.glLineWidth(2);GL11.glBegin(GL11.GL_LINE_STRIP);
                for(var p:t.path.points()) GL11.glVertex3d(p.x()-cx,p.y()-cy,p.z()-cz);
                GL11.glEnd();
                if(t.path.impact()!=null) {
                    var box=t.path.bounds();int impactColor=t.color;
                    if(t.path.entityId()>=0) {
                        var target=mc.world.getEntity(t.path.entityId());
                        if(target!=null&&!target.removed) {
                            var shape=target.getShape().moved((target.prevX-target.x)*(1-delta),(target.prevY-target.y)*(1-delta),(target.prevZ-target.z)*(1-delta));
                            box=bounds(shape);impactColor=0xffff3333;
                        } else continue;
                    }
                    impact(t.path.impact(),box,impactColor);
                }
                if(t.item==Items.ARROW) {
                    var p=t.path.points().getFirst();GL11.glPushMatrix();
                    GL11.glTranslated(p.x()-cx,p.y()-cy,p.z()-cz);GL11.glRotatef(-t.entity.yaw,0,1,0);GL11.glRotatef(t.entity.pitch,1,0,0);GL11.glRotated((t.entity.ticks+delta)*20,0,0,1);
                    GL11.glBegin(GL11.GL_LINES);for(int i=0;i<4;i++) { double a=i*Math.PI/2;GL11.glVertex3d(Math.cos(a)*.02,Math.sin(a)*.02,-.12);GL11.glVertex3d(Math.cos(a)*.15,Math.sin(a)*.15,-.12); }GL11.glEnd();GL11.glPopMatrix();
                }
            }
        }
    }
    private static ProjectilePath.Bounds bounds(Box b) { return new ProjectilePath.Bounds(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ); }
    private void impact(ProjectilePath.Point p,ProjectilePath.Bounds bounds,int color) {
        tint((color&0xffffff)|0x30000000);GL11.glBegin(GL11.GL_QUADS);
        double x=p.x()-cx,y=p.y()-cy,z=p.z()-cz,r=.18;
        double[][] v={{x-r,y-r,z-r},{x+r,y-r,z-r},{x+r,y-r,z+r},{x-r,y-r,z+r},{x-r,y+r,z-r},{x+r,y+r,z-r},{x+r,y+r,z+r},{x-r,y+r,z+r}};
        if(bounds!=null) {
            double x1=bounds.minX()-cx,y1=bounds.minY()-cy,z1=bounds.minZ()-cz,x2=bounds.maxX()-cx,y2=bounds.maxY()-cy,z2=bounds.maxZ()-cz;
            v=new double[][]{{x1,y1,z1},{x2,y1,z1},{x2,y1,z2},{x1,y1,z2},{x1,y2,z1},{x2,y2,z1},{x2,y2,z2},{x1,y2,z2}};
        }
        int[][] faces={{0,1,2,3},{4,5,6,7},{0,1,5,4},{3,2,6,7},{0,3,7,4},{1,2,6,5}};
        for(var f:faces)for(int i:f)GL11.glVertex3d(v[i][0],v[i][1],v[i][2]);GL11.glEnd();
        tint(color);GL11.glBegin(GL11.GL_LINES);int[][] edges={{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for(var e:edges)for(int i:e)GL11.glVertex3d(v[i][0],v[i][1],v[i][2]);GL11.glEnd();
    }
    public void renderHud(Minecraft mc) {
        if(view==null||!ready(mc)) return;
        try(var ignored=new EspGlState()) {
            state();
            for(var t:tracked) {
                if(t.entity.removed||config.indicatorApproaching&&!t.approaching) continue;
                var p=EspProjection.project(view,projection,t.entity.x-cx,t.entity.y-cy,t.entity.z-cz,width,height); if(p==null)continue;
                boolean visible=p.front()&&p.depth()>=-1&&p.depth()<=1&&p.x()>=0&&p.x()<=width&&p.y()>=0&&p.y()<=height;
                if(config.indicatorOffscreen&&visible)continue;
                double dx=p.x()-width/2.0,dy=p.y()-height/2.0,angle=Math.atan2(dy,dx);
                double radius=Math.clamp(config.indicatorRadius,30,200)+(config.indicatorItems?20:0);
                if(p.front()&&Math.hypot(dx,dy)<radius+15)continue;
                GL11.glPushMatrix();
                try {
                    GL11.glTranslated(width/2.0+Math.cos(angle)*radius,height/2.0+Math.sin(angle)*radius,0);GL11.glRotated(Math.toDegrees(angle)+90,0,0,1);
                    tint(t.color);GlStateManager.disableTexture();
                    if(config.indicatorShape==2) { GL11.glBegin(GL11.GL_TRIANGLES);GL11.glVertex2d(0,-5);GL11.glVertex2d(-5,5);GL11.glVertex2d(5,5);GL11.glEnd(); }
                    else if(config.indicatorShape==1) { GlStateManager.enableTexture();GL11.glRotated(-90,0,0,1);font.centered(mc,">",0,-4,t.color,config.indicatorFont==1); }
                    else { GL11.glLineWidth(3);GL11.glBegin(GL11.GL_LINE_STRIP);GL11.glVertex2d(-4.5,3);GL11.glVertex2d(0,-3);GL11.glVertex2d(4.5,3);GL11.glEnd(); }
                } finally { GL11.glPopMatrix(); }
                if(config.indicatorDistance) { GlStateManager.enableTexture();font.centered(mc,Math.round(t.entity.distanceTo(mc.player))+"m",(float)(width/2.0+Math.cos(angle)*(radius-13)),(float)(height/2.0+Math.sin(angle)*(radius-13))-4,t.color,config.indicatorFont==1); }
                if(config.indicatorItems) {
                    GL11.glPushMatrix();try {
                        GL11.glTranslated(width/2.0+Math.cos(angle)*(radius-29)-8,height/2.0+Math.sin(angle)*(radius-29)-8,0);
                        GlStateManager.enableTexture();GlStateManager.enableDepthTest();Lighting.turnOn();mc.getItemRenderer().renderGuiItem(new ItemStack(t.item),0,0);Lighting.turnOff();state();
                    }finally { GL11.glPopMatrix(); }
                }
            }
        }
    }
    private static float[] matrix(int kind) { var b=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(kind,b);float[] m=new float[16];b.get(m);return m; }
    private static void state() { GL20.glUseProgram(0);GlStateManager.disableDepthTest();GlStateManager.depthMask(false);GlStateManager.disableTexture();GlStateManager.disableLighting();GlStateManager.disableFog();GlStateManager.disableAlphaTest();GlStateManager.disableCull();GlStateManager.enableBlend();GlStateManager.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA); }
    private static void tint(int c) { GlStateManager.color4f((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f,(c>>>24)/255f); }
    @Override public void reset() { tracked.clear();previous.clear();view=projection=null;ticks=0; }
    @Override public void close() { reset();font.close(); }
}
