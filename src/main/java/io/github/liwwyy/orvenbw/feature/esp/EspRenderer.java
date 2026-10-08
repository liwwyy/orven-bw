package io.github.liwwyy.orvenbw.feature.esp;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.render.model.entity.PlayerModel;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import java.nio.FloatBuffer;
import java.util.*;

/** World primitives, animated skeletons, projected bounds and near/far waypoint rendering. */
public final class EspRenderer implements AutoCloseable {
    private final OrvenConfig config;
    private final EspOutline outline = new EspOutline();
    private final List<Bounds> bounds = new ArrayList<>();
    private final Map<Integer, Pose> poses = new HashMap<>();
    private float[] view, projection;
    private double cameraX, cameraY, cameraZ;
    private int width, height;
    private record Bounds(PlayerEntity player, double left, double top, double right, double bottom, int color) {}
    private record Rotation(float x, float y, float z, float rx, float ry, float rz) {
        static Rotation of(ModelPart p) { return new Rotation(p.x,p.y,p.z,p.rotationX,p.rotationY,p.rotationZ); }
    }
    private record Pose(Rotation head, Rotation body, Rotation leftArm, Rotation rightArm, Rotation leftLeg, Rotation rightLeg, int tick) {}
    public EspRenderer(OrvenConfig config) { this.config = config; }
    public static boolean drawingOutline() { return EspOutline.drawing; }
    public void capture(PlayerEntity player, PlayerModel model) {
        if (!config.modEnabled || !config.playerEspEnabled || !style(5) || EspOutline.drawing) return;
        poses.put(player.getNetworkId(), new Pose(Rotation.of(model.head), Rotation.of(model.body), Rotation.of(model.leftArm),
                Rotation.of(model.rightArm), Rotation.of(model.leftLeg), Rotation.of(model.rightLeg), player.ticks));
    }
    private boolean style(int index) { return config.espStyles != null && config.espStyles.length > index && config.espStyles[index]; }
    private int color(PlayerEntity player) {
        if (config.espRedOnDamage && player.damagedTimer > 0) return 0xffff5555;
        int team = config.espTeamColor ? EspPlayers.color(player) : -1;
        if (team >= 0) return 0xff000000 | team;
        if (config.espRainbow) return 0xff000000 | java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 5000) / 5000f, .8f, 1);
        return config.espColor == null ? 0xff00ff00 : config.espColor.getArgb();
    }
    private boolean eligible(Minecraft mc, PlayerEntity player) {
        return player.isAlive() && player.deathTicks == 0 && !player.isSpectator()
                && (!player.isInvisible() || config.espShowInvisible)
                && (player != mc.player || config.espRenderSelf && mc.options.perspective != 0)
                && (!config.espIgnoreNpcs || player == mc.player || EspPlayers.listed(mc, player))
                && mc.getCamera().squaredDistanceTo(player) <= Math.pow(Math.clamp(config.espMaxDistance, 32, 256), 2);
    }
    public void renderWorld(Minecraft mc, float delta, BedIndex beds, boolean bedwars) {
        clearFrame();
        Entity camera = mc.getCamera();
        if (camera == null || EspOutline.drawing) return;
        cameraX = camera.prevX + (camera.x - camera.prevX) * delta;
        cameraY = camera.prevY + (camera.y - camera.prevY) * delta;
        cameraZ = camera.prevZ + (camera.z - camera.prevZ) * delta;
        var window = new Window(mc); width = window.getWidth(); height = window.getHeight();
        view = matrix(GL11.GL_MODELVIEW_MATRIX); projection = matrix(GL11.GL_PROJECTION_MATRIX);
        List<PlayerEntity> players = new ArrayList<>();
        FrustumCuller frustum = new FrustumCuller(); frustum.prepare(cameraX, cameraY, cameraZ);
        if (config.playerEspEnabled) for (PlayerEntity player : mc.world.players)
            if (eligible(mc, player) && frustum.isVisible(player.getShape())) players.add(player);
        poses.keySet().removeIf(id -> mc.world.getEntity(id) == null);
        try (var ignored = new EspGlState()) {
            overlayState();
            for (PlayerEntity player : players) {
                int color = color(player);
                double x = player.prevX + (player.x - player.prevX) * delta - cameraX;
                double y = player.prevY + (player.y - player.prevY) * delta - cameraY;
                double z = player.prevZ + (player.z - player.prevZ) * delta - cameraZ;
                var shape = player.getShape();
                double x1 = x + shape.minX - player.x, x2 = x + shape.maxX - player.x;
                double y1 = y + shape.minY - player.y, y2 = y + shape.maxY - player.y;
                double z1 = z + shape.minZ - player.z, z2 = z + shape.maxZ - player.z;
                if (style(1)) box(x1,y1,z1,x2,y2,z2,color,false);
                if (style(4)) box(x1,y1,z1,x2,y2,z2,(color & 0xffffff) | 0x33000000,true);
                if (style(3)) ring(x,y + .03,z,Math.max(.4, player.width * .8),color);
                if (style(5)) skeleton(player,x,y,z,delta,color);
                if (style(0) || config.espHealthBar) captureBounds(player,x1,y1,z1,x2,y2,z2,color);
            }
            if (bedwars && config.bedWaypointsEnabled) for (var bed : beds.beds()) {
                var p = project(bed.geometry.x() - cameraX, bed.geometry.y() + .7 - cameraY, bed.geometry.z() - cameraZ);
                if (p != null && p.front() && worldMarkerSize(bed) >= 1) waypointWorld(mc,bed);
            }
            if (bedwars && config.bedAlertsEnabled && config.bedAlertHighlightObsidian) for (var bed : beds.beds()) {
                if (!bed.confirmed) continue;
                // Raven checks all six faces of each bed half; defence counting excludes the foundation.
                Set<BedGeometry.Pos> adjacent = new HashSet<>(bed.geometry.defence());
                adjacent.add(bed.geometry.foot().add(0,-1,0)); adjacent.add(bed.geometry.head().add(0,-1,0));
                for (var p : adjacent) if (mc.world.isChunkLoaded(EspFeature.block(p))
                        && mc.world.getBlockState(EspFeature.block(p)).getBlock() == net.minecraft.block.Blocks.OBSIDIAN)
                    box(p.x()-cameraX,p.y()-cameraY,p.z()-cameraZ,p.x()+1-cameraX,p.y()+1-cameraY,p.z()+1-cameraZ,0xff6a0dad,false);
            }
        }
        if (config.playerEspEnabled && style(2)) outline.render(mc,delta,players,this::color,config.espShowInvisible);
        else outline.close();
    }
    private void captureBounds(PlayerEntity player,double x1,double y1,double z1,double x2,double y2,double z2,int color) {
        double left = Double.POSITIVE_INFINITY, top = left, right = Double.NEGATIVE_INFINITY, bottom = right;
        for (double x : new double[]{x1,x2}) for (double y : new double[]{y1,y2}) for (double z : new double[]{z1,z2}) {
            var p = project(x,y,z);
            // Do not create enormous rectangles for bounds intersecting the near camera plane.
            if (p == null || !p.front() || p.depth() < -1 || p.depth() > 1) return;
            left = Math.min(left,p.x()); right = Math.max(right,p.x()); top = Math.min(top,p.y()); bottom = Math.max(bottom,p.y());
        }
        if (right < 0 || bottom < 0 || left > width || top > height) return;
        bounds.add(new Bounds(player,Math.max(-2,left),Math.max(-2,top),Math.min(width+2,right),Math.min(height+2,bottom),color));
    }
    public void renderHud(Minecraft mc, BedIndex beds, boolean bedwars) {
        if (view == null) return;
        try (var ignored = new EspGlState()) {
            for (var b : bounds) {
                if (style(0)) {
                    rect(b.left-1,b.top-1,b.right+1,b.bottom+1,0xdd000000,3);
                    rect(b.left,b.top,b.right,b.bottom,b.color,1);
                }
                if (config.espHealthBar) {
                    double fraction = Math.clamp(b.player.getHealth() / Math.max(1,b.player.getMaxHealth()),0,1);
                    GuiElement.fill((int)b.left-6,(int)b.top-1,(int)b.left-2,(int)b.bottom+1,0xdd000000);
                    int rgb = java.awt.Color.HSBtoRGB((float)fraction / 3,1,1);
                    GuiElement.fill((int)b.left-5,(int)(b.bottom-(b.bottom-b.top)*fraction),(int)b.left-3,(int)b.bottom,0xff000000 | rgb);
                }
            }
            if (!bedwars || !config.bedWaypointsEnabled) return;
            for (var bed : beds.beds()) {
                var projected = project(bed.geometry.x()-cameraX,bed.geometry.y()+.7-cameraY,bed.geometry.z()-cameraZ);
                if (projected == null) continue;
                boolean outside = !projected.front() || projected.x() < 16 || projected.x() > width-16 || projected.y() < 16 || projected.y() > height-30;
                if (outside && !config.bedEdgeMarkers) continue;
                boolean world = projected.front() && worldMarkerSize(bed) >= 1 && !outside;
                if (world) continue;
                var p = outside ? EspProjection.edge(projected,width,height,24 * scale()) : projected;
                GL11.glPushMatrix();
                try {
                    GL11.glTranslated(p.x(),p.y(),0); GL11.glScalef(scale(),scale(),1);
                    label(mc,bed);
                } finally { GL11.glPopMatrix(); }
            }
        }
    }
    private float scale() { return Math.clamp(config.bedMarkerScale,.5f,2); }
    private double worldMarkerSize(BedIndex.Bed bed) {
        double x = bed.geometry.x()-cameraX, y = bed.geometry.y()+.7-cameraY, z = bed.geometry.z()-cameraZ;
        var p = project(x,y,z);
        // Project a camera-facing vertical segment to choose the exact near/far handoff.
        var upper = project(x + view[1]*.04*scale(),y + view[5]*.04*scale(),z + view[9]*.04*scale());
        if (p == null || upper == null || !p.front()) return 0;
        return Math.hypot(upper.x()-p.x(),upper.y()-p.y()) / scale();
    }
    private void waypointWorld(Minecraft mc, BedIndex.Bed bed) {
        var dispatcher = mc.getEntityRenderDispatcher();
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(bed.geometry.x()-cameraX,bed.geometry.y()+.7-cameraY,bed.geometry.z()-cameraZ);
            GL11.glRotatef(-dispatcher.cameraYaw,0,1,0);
            GL11.glRotatef(mc.options.perspective == 2 ? -dispatcher.cameraPitch : dispatcher.cameraPitch,1,0,0);
            GL11.glScalef(-.04f*scale(),-.04f*scale(),.04f*scale());
            label(mc,bed);
            overlayState();
        } finally { GL11.glPopMatrix(); }
    }
    private void label(Minecraft mc, BedIndex.Bed bed) {
        String title = bed.team.initial() + (bed.confirmed && (bed.teamObserved || bed.team == BedTeam.UNKNOWN) ? "" : "?");
        int width = mc.textRenderer.getWidth(title);
        GlStateManager.enableTexture(); GlStateManager.enableBlend(); GlStateManager.disableDepthTest(); GlStateManager.depthMask(false);
        GuiElement.fill(-width/2-3,-3,width/2+3,10,0xaa151515);
        mc.textRenderer.drawWithShadow(title,-width/2f,0,0xff000000 | bed.team.rgb);
        int y = 13;
        double distance = Math.sqrt(bed.geometry.distanceSquared(cameraX,cameraY,cameraZ));
        if (config.bedShowDistance && distance > 5) {
            String text = Math.round(distance) + "m";
            mc.textRenderer.drawWithShadow(text,-mc.textRenderer.getWidth(text)/2f,y,0xffeeeeee); y += 11;
        }
        if (config.bedObsidianMarkers && bed.obsidian > 0) {
            String text = bed.count(); int size = mc.textRenderer.getWidth(text);
            GuiElement.fill(-size/2-3,y-2,size/2+3,y+10,0xdd300b47);
            mc.textRenderer.drawWithShadow(text,-size/2f,y,0xffb56fe0);
        }
    }
    private void skeleton(PlayerEntity player,double x,double y,double z,float delta,int color) {
        Pose pose = poses.get(player.getNetworkId());
        if (pose == null || Math.abs(player.ticks - pose.tick) > 2) return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x,y,z);
            float yaw = player.lastBodyYaw + wrap(player.bodyYaw-player.lastBodyYaw)*delta;
            GL11.glRotatef(180-yaw,0,1,0); GL11.glScalef(-.9375f,-.9375f,.9375f);
            GL11.glTranslatef(0,-1.5078125f,0);
            if (player.isSneaking()) GL11.glTranslatef(0,.2f,0);
            tint(color); GL11.glLineWidth(1.5f);
            segment(pose.head,0,-.5,0);
            segment(pose.body,0,.75,0);
            segment(pose.leftArm,0,.625,0); segment(pose.rightArm,0,.625,0);
            segment(pose.leftLeg,0,.75,0); segment(pose.rightLeg,0,.75,0);
            GL11.glBegin(GL11.GL_LINES);
            vertex(pose.leftArm.x/16.0,pose.leftArm.y/16.0,pose.leftArm.z/16.0); vertex(pose.rightArm.x/16.0,pose.rightArm.y/16.0,pose.rightArm.z/16.0);
            vertex(pose.leftLeg.x/16.0,pose.leftLeg.y/16.0,pose.leftLeg.z/16.0); vertex(pose.rightLeg.x/16.0,pose.rightLeg.y/16.0,pose.rightLeg.z/16.0);
            GL11.glEnd();
        } finally { GL11.glPopMatrix(); }
    }
    private static float wrap(float angle) { return (angle+540)%360-180; }
    private static void segment(Rotation r,double x,double y,double z) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(r.x/16,r.y/16,r.z/16);
            GL11.glRotatef((float)Math.toDegrees(r.rz),0,0,1); GL11.glRotatef((float)Math.toDegrees(r.ry),0,1,0); GL11.glRotatef((float)Math.toDegrees(r.rx),1,0,0);
            GL11.glBegin(GL11.GL_LINES); vertex(0,0,0); vertex(x,y,z); GL11.glEnd();
        } finally { GL11.glPopMatrix(); }
    }
    private EspProjection.Point project(double x,double y,double z) { return EspProjection.project(view,projection,x,y,z,width,height); }
    private static float[] matrix(int kind) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(kind,buffer);
        float[] values = new float[16]; buffer.get(values); return values;
    }
    private static void overlayState() {
        GL20.glUseProgram(0);
        GlStateManager.disableDepthTest(); GlStateManager.depthMask(false); GlStateManager.disableTexture();
        GlStateManager.disableLighting(); GlStateManager.disableFog(); GlStateManager.disableCull(); GlStateManager.disableAlphaTest();
        GlStateManager.enableBlend(); GlStateManager.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
    }
    private static void tint(int argb) { GlStateManager.color4f((argb>>16&255)/255f,(argb>>8&255)/255f,(argb&255)/255f,(argb>>>24)/255f); }
    private static void vertex(double x,double y,double z) { GL11.glVertex3d(x,y,z); }
    private static void ring(double x,double y,double z,double radius,int color) {
        tint(color); GL11.glLineWidth(1.5f); GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i=0;i<64;i++) { double angle=i*Math.PI/32; vertex(x+Math.cos(angle)*radius,y,z+Math.sin(angle)*radius); } GL11.glEnd();
    }
    private static void box(double x1,double y1,double z1,double x2,double y2,double z2,int color,boolean fill) {
        tint(color); GL11.glLineWidth(1.5f);
        double[][] points = {{x1,y1,z1},{x2,y1,z1},{x2,y1,z2},{x1,y1,z2},{x1,y2,z1},{x2,y2,z1},{x2,y2,z2},{x1,y2,z2}};
        int[][] indices = fill ? new int[][]{{0,1,2,3},{4,5,6,7},{0,1,5,4},{3,2,6,7},{0,3,7,4},{1,2,6,5}}
                : new int[][]{{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        GL11.glBegin(fill ? GL11.GL_QUADS : GL11.GL_LINES);
        for (var face : indices) for (int index : face) vertex(points[index][0],points[index][1],points[index][2]);
        GL11.glEnd();
    }
    private static void rect(double l,double t,double r,double b,int color,int weight) {
        GuiElement.fill((int)l,(int)t,(int)r,(int)t+weight,color); GuiElement.fill((int)l,(int)b-weight,(int)r,(int)b,color);
        GuiElement.fill((int)l,(int)t,(int)l+weight,(int)b,color); GuiElement.fill((int)r-weight,(int)t,(int)r,(int)b,color);
    }
    public void clearFrame() { bounds.clear(); view = projection = null; }
    public void clearPlayers() { clearFrame(); poses.clear(); }
    @Override public void close() { outline.close(); clearFrame(); poses.clear(); }
}
