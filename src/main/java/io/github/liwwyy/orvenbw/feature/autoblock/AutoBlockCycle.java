package io.github.liwwyy.orvenbw.feature.autoblock;

/** Raven's tick-based hold/lag cycle. The lag deadline starts with the block, not its release. */
public final class AutoBlockCycle {
    public enum State { IDLE, BLOCK, LAG }
    private State state=State.IDLE;
    private int start, lastHurt;
    private boolean manual;
    public State state() { return state; }
    public void reset() { state=State.IDLE; manual=false; lastHurt=0; }
    public void attack(boolean immediate,boolean again,int tick) {
        if(state==State.LAG && immediate) { state=again?State.BLOCK:State.IDLE; start=tick; }
    }
    public State step(int tick,boolean eligible,boolean manualOnly,int hurt,boolean damagedOnly,int hurtMs,int holdMs,int lagMs,boolean lagRoll,boolean again) {
        boolean fresh=hurt>lastHurt; lastHurt=hurt;
        if(!eligible&&!manualOnly) { reset(); return state; }
        if(manualOnly) { if(state!=State.BLOCK || !manual) start=tick; state=State.BLOCK; manual=true; return state; }
        if(manual) { state=State.IDLE; manual=false; }
        if(state==State.LAG) {
            if(tick-start>=ticks(lagMs)) { state=again?State.BLOCK:State.IDLE; if(again) start=tick; }
            return state;
        }
        if(state==State.IDLE && (!damagedOnly || hurt==Math.clamp(Math.round(hurtMs/50f),1,10))) { state=State.BLOCK; start=tick; }
        if(state==State.BLOCK && (tick-start>=ticks(holdMs) || damagedOnly&&fresh))
            state=lagRoll && tick-start<ticks(lagMs)?State.LAG:State.IDLE;
        return state;
    }
    public static int ticks(int ms) { return (int)Math.ceil(Math.clamp(ms,50,500)/50.0); }
}
