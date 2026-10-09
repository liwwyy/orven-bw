package io.github.liwwyy.orvenbw.feature.autoblock;

import io.github.liwwyy.orvenbw.OrvenBw;
import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.*;
import io.github.liwwyy.orvenbw.feature.esp.EspPlayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.lwjgl.input.*;
import java.util.*;

/** Owns vanilla use-key state and briefly queues already-created vanilla packets in send order. */
public final class AutoBlockFeature implements ClientFeature {
    private final OrvenConfig config;
    private final AutoBlockCycle cycle=new AutoBlockCycle();
    private final ArrayDeque<Packet<?>> pending=new ArrayDeque<>();
    private ClientPlayNetworkHandler connection;
    private Minecraft client;
    private int ticks, key=Integer.MIN_VALUE;
    private boolean draining, owned, conditions;
    private long deadline;
    public AutoBlockFeature(OrvenConfig config) { this.config=config; }
    public static boolean physical(int code) { return code<0?Mouse.isCreated()&&code+100>=0&&code+100<Mouse.getButtonCount()&&Mouse.isButtonDown(code+100):Keyboard.isCreated()&&code>0&&code<Keyboard.KEYBOARD_SIZE&&Keyboard.isKeyDown(code); }
    private boolean ready(Minecraft mc) {
        var mod=OrvenBw.instance();
        return config.autoBlockEnabled && ScoreboardGate.allows(mc,config) && mc.player!=null && mc.world!=null && mc.screen==null && mc.focused && !mc.isPaused() && mc.player.isAlive()
                && mc.player.getItemInHand()!=null && mc.player.getItemInHand().getItem() instanceof SwordItem && (mod==null||!mod.autoSoup().busy());
    }
    private static double rangeSquared(Minecraft mc,net.minecraft.entity.living.player.PlayerEntity player) {
        var b=player.getShape();
        return CombatGeometry.distanceSquared(mc.player.x,mc.player.y+mc.player.getEyeHeight(),mc.player.z,b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);
    }
    private static boolean teammate(Minecraft mc,net.minecraft.entity.living.player.PlayerEntity player) {
        if(mc.player.getScoreboardTeam()!=null && mc.player.getScoreboardTeam()==player.getScoreboardTeam()) return true;
        int own=EspPlayers.color(mc.player),other=EspPlayers.color(player);
        return own>=0 && own==other;
    }
    public boolean ownsUse() { return owned; }
    public boolean forceAnimation() { return owned && config.autoBlockAnimation && cycle.state()!=AutoBlockCycle.State.IDLE && client!=null && ready(client); }
    public boolean blockVanillaUse() { return owned && cycle.state()==AutoBlockCycle.State.LAG; }
    @Override public void tick(Minecraft mc) { client=mc; if(!ready(mc)) reset(); else if(cycle.state()==AutoBlockCycle.State.LAG && System.nanoTime()>=deadline) { flush(); cycle.attack(true,config.autoBlockAgain&&conditions,ticks); apply(mc); } }
    @Override public void beforeInteractions(Minecraft mc) {
        client=mc; ticks++;
        if(!ready(mc)) { reset(); return; }
        int use=mc.options.useKey.getKeyCode();
        if(owned && key!=use) reset(); key=use;
        boolean left=physical(mc.options.attackKey.getKeyCode()),right=physical(use);
        boolean target=mc.world.players.stream().anyMatch(p->p!=mc.player&&p.isAlive()&&!p.isSpectator()&&EspPlayers.listed(mc,p)&&rangeSquared(mc,p)<=Math.pow(Math.clamp(config.autoBlockRange,2,6),2)
                && (!config.autoBlockIgnoreTeam||!teammate(mc,p)));
        conditions=target&&(!config.autoBlockRequireLeft||left)&&(!config.autoBlockRequireRight||right);
        var before=cycle.state();
        var after=cycle.step(ticks,conditions,right&&!left,mc.player.damagedTimer,config.autoBlockDamagedOnly,config.autoBlockHurtMs,config.autoBlockHoldMs,config.autoBlockLagMs,Math.random()*100<config.autoBlockLagChance,config.autoBlockAgain);
        if(before==AutoBlockCycle.State.LAG && after!=before) flush();
        if(before!=AutoBlockCycle.State.LAG && after==AutoBlockCycle.State.LAG) {
            connection=mc.player.networkHandler;
            // Cycle itself caps at the original block deadline; wall-clock guard bounds pauses/render stalls too.
            deadline=System.nanoTime()+Math.min(500,Math.max(50,config.autoBlockLagMs-config.autoBlockHoldMs))*1_000_000L;
        }
        apply(mc);
    }
    private void apply(Minecraft mc) {
        boolean block=cycle.state()==AutoBlockCycle.State.BLOCK;
        if(cycle.state()==AutoBlockCycle.State.IDLE) { release(); return; }
        boolean start=block&&(!owned||!mc.options.useKey.isPressed());
        owned=true; KeyBinding.set(key,block);
        if(start) { if(OrvenBw.instance()!=null) OrvenBw.instance().clickOrigins().prepare("artificial","autoblock",null,System.nanoTime()); KeyBinding.click(key); }
        if(!block) while(mc.options.useKey.consumeClick()) { /* Do not let physical use restart blocking during lag. */ }
    }
    public boolean outgoing(ClientPlayNetworkHandler handler,Packet<?> packet) {
        if(draining || cycle.state()!=AutoBlockCycle.State.LAG) return false;
        if(handler!=connection || client==null || !ready(client)) { reset(); return false; }
        if(System.nanoTime()>=deadline || pending.size()>=256) { flush(); cycle.attack(true,false,ticks); apply(client); return false; }
        if(config.autoBlockPreventAttackDelay && packet instanceof PlayerInteractEntityC2SPacket p && p.getAction()==PlayerInteractEntityC2SPacket.Action.ATTACK) {
            flush(); cycle.attack(true,config.autoBlockAgain&&conditions,ticks); apply(client); return false;
        }
        pending.addLast(packet); return true;
    }
    private void flush() {
        draining=true;
        try { while(!pending.isEmpty()) { var p=pending.removeFirst(); if(connection!=null && connection.getConnection().isConnected()) connection.sendPacket(p); } }
        finally { draining=false; pending.clear(); connection=null; }
    }
    private void release() {
        if(owned && client!=null && key!=Integer.MIN_VALUE) {
            KeyBinding.set(key,client.screen==null&&client.focused&&physical(key));
            while(client.options.useKey.consumeClick()) {}
        }
        owned=false;
    }
    @Override public void reset() { flush(); cycle.reset(); release(); conditions=false; }
}
