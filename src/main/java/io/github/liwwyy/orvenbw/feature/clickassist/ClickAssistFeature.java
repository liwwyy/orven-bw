package io.github.liwwyy.orvenbw.feature.clickassist;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.ClientFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.world.HitResult;
import java.util.concurrent.ThreadLocalRandom;
import io.github.liwwyy.orvenbw.feature.ScoreboardGate;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

public final class ClickAssistFeature implements ClientFeature {
    private final OrvenConfig config;
    private final ClickAssistEngine engine = new ClickAssistEngine();
    private int attackCode = Integer.MIN_VALUE;
    private int useCode = Integer.MIN_VALUE;

    private final ClickProfileSession[] sessions = {new ClickProfileSession(() -> ThreadLocalRandom.current().nextDouble(), 0), new ClickProfileSession(() -> ThreadLocalRandom.current().nextDouble(), 1)};
    private final HeldClickTrigger[] heldTriggers = {new HeldClickTrigger(), new HeldClickTrigger()};
    private final boolean[] held = new boolean[2];
    private final boolean[] ownsHold = new boolean[2];
    private final boolean[] spamPressed = new boolean[2], spamLatched = new boolean[2];
    private int lastMode = -1;
    private int activeButton;
    private Minecraft client;
    private String bindSignature;
    private final int[] sources = {-1, -1};
    private final ClickProfileSession.Options[] profiles = new ClickProfileSession.Options[2];

    public void activation(int action, boolean down) {
        Minecraft mc = Minecraft.getInstance();
        if (!commonEligible(mc) || mc.screen != null || !mc.focused || mc.isPaused()) return;
        if (lastMode != config.spamMode) {
            clearSpam();
            lastMode = config.spamMode;
        }
        if (action == 0 || action == 3 || action == 4) {
            if (!config.spamEnabled) return;
            int button = action == 0 ? config.spamButton : action == 3 ? 0 : 1;
            boolean wasPressed = spamPressed[button];
            spamPressed[button] = down;
            if (down && !wasPressed && config.spamMode == 1) spamLatched[button] = !spamLatched[button];
        } else if (down && config.holdEnabled && action >= 1 && action <= 2) held[action - 1] = !held[action - 1];
    }
    private void clearSpam() {
        java.util.Arrays.fill(spamPressed, false);
        java.util.Arrays.fill(spamLatched, false);
    }

    public ClickAssistFeature(OrvenConfig config) { this.config = config; }

    @Override public void onInput(Minecraft mc, int keyCode) {
        syncBindings(mc);
        long now = System.nanoTime();
        if (keyCode == attackCode) engine.physicalClick(0, now);
        if (keyCode == useCode) engine.physicalClick(1, now);
    }

    @Override public void beforeInteractions(Minecraft mc) {
        syncBindings(mc);
        client = mc;
        String signature = bindState(config.spamLeftBind) + bindState(config.spamRightBind) + bindState(config.holdLeftBind) + bindState(config.holdRightBind);
        if (bindSignature != null && !bindSignature.equals(signature)) reset();
        bindSignature = signature;
        if (!commonEligible(mc)) { reset(); return; }
        var soupMod = io.github.liwwyy.orvenbw.OrvenBw.instance();
        if (soupMod != null && soupMod.autoSoup().busy()) {
            for (int button = 0; button < 2; button++) {
                engine.cancel(button); sessions[button].reset(); heldTriggers[button].reset();
                sources[button] = -1; profiles[button] = null;
                if (ownsHold[button]) {
                    KeyBinding.set(button == 0 ? attackCode : useCode, physicalDown(button == 0 ? attackCode : useCode));
                    ownsHold[button] = false;
                }
            }
            return;
        }
        if (lastMode != config.spamMode) {
            clearSpam();
            for (int button = 0; button < 2; button++) { sessions[button].reset(); engine.cancel(button); }
            lastMode = config.spamMode;
        }
        if (!config.spamEnabled) clearSpam();
        if (!config.holdEnabled) held[0] = held[1] = false;
        long now = System.nanoTime();
        var conditions = heldConditions(mc);
        ItemStack heldStack = mc.player.getItemInHand();
        Item heldItem = heldStack == null ? null : heldStack.getItem();
        for (int button = 0; button <= 1; button++) {
            if(button==1 && soupMod!=null && soupMod.autoBlock().ownsUse()) {
                engine.cancel(button); sessions[button].reset(); heldTriggers[button].reset(); sources[button]=-1; profiles[button]=null; ownsHold[button]=false; continue;
            }
            boolean leftFist = button == 0 && heldItem == null && config.heldClickAllowFist;
            var sideConditions = new HeldClickTrigger.Conditions(
                    conditions.available() && (button != 0 || heldItem != null || leftFist),
                    conditions.entity(), conditions.weapon() || leftFist, conditions.nearby());
            boolean heldPermitted = sideConditions.allows(config.heldClickEntityOnly, config.heldClickWeaponOnly, config.heldClickRequiresPlayer);
            int code = button == 0 ? attackCode : useCode;
            if (held[button]) {
                if (!ownsHold[button]) {
                    var mod = io.github.liwwyy.orvenbw.OrvenBw.instance();
                    if (mod != null && config.debugEnabled)
                        mod.clickOrigins().prepare("artificial", "button_hold", null, System.nanoTime());
                    KeyBinding.click(code);
                    debug("button_hold", button, "hold_down", code);
                }
                KeyBinding.set(code, true);
                ownsHold[button] = true;
            } else if (ownsHold[button]) {
                KeyBinding.set(code, physicalDown(code));
                ownsHold[button] = false;
                debug("button_hold", button, "hold_up", code);
            }
            boolean explicitSpam = config.spamEnabled && (config.spamMode == 1 ? spamLatched[button] : spamPressed[button]);
            boolean heldSpam = heldTriggers[button].active(now, physicalDown(code),
                    config.heldClickEnabled && !held[button]
                            && (button == 0 ? config.heldClickLeft : config.heldClickRight)
                            && heldPermitted,
                    config.heldClickDelayMs * 1_000_000L,
                    config.heldClickInstant && sideConditions.instantReady());
            int source = HeldClickTrigger.source(held[button], explicitSpam, heldSpam,
                    config.enabled && engine.manuallyActive(button, now, config.activationCps));
            boolean spamming = source == 1 || source == 2;
            boolean clicking = source >= 0 && (source == 2 ? heldPermitted : eligible(mc, button, spamming));
            var profile = config.profileOptions();
            if (sources[button] != source || !profile.equals(profiles[button])) {
                sessions[button].reset(); engine.cancel(button);
                sources[button] = source; profiles[button] = profile;
            }
            double base = engine.manualRate(button, now);
            boolean maintain = button == 0 && config.entityCpsFloorEnabled && entityTarget(mc)
                    && mc.crosshairTarget.entity instanceof LivingEntity entity && entity.isAlive();
            double target = sessions[button].target(now, clicking, profile, maintain, config.entityCpsFloor);
            double generated = Math.max(0, target - base);
            final int side = button, clickSource = source;
            engine.pollDue(button, now, generated, clicking, spamming ? 0 : config.boostDelayMs * 1_000_000L,
                    profile.ceiling(), sessions[button]::intervalWeight, intended -> {
                        var mod = io.github.liwwyy.orvenbw.OrvenBw.instance();
                        if (mod != null && config.debugEnabled)
                            mod.clickOrigins().prepare("artificial",
                                    clickSource == 1 ? "spam_click" : clickSource == 2 ? "mouse_hold_click" : "cps_boost",
                                    profile.name(), intended);
                        KeyBinding.click(code);
                        activeButton = side;
                    });
        }
    }

    private static void debug(String method, int button, String action, int code) {
        var mod = io.github.liwwyy.orvenbw.OrvenBw.instance();
        if (mod != null) mod.debugLog().event("artificial", method, button == 0 ? "left" : "right", action, code, -1);
    }
    private static String bindState(org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind bind) {
        return java.util.Arrays.toString(bind.getKeyCodes()) + java.util.Arrays.toString(bind.getMouseBtns()) + ":" + bind.getMods() + ";";
    }
    private static boolean physicalDown(int code) {
        if (code < 0) return Mouse.isCreated() && code + 100 >= 0 && code + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(code + 100);
        return Keyboard.isCreated() && code > 0 && code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
    }

    private void syncBindings(Minecraft mc) {
        int attack = mc.options.attackKey.getKeyCode();
        int use = mc.options.useKey.getKeyCode();
        if (attack != attackCode || use != useCode) {
            reset();
            attackCode = attack;
            useCode = use;
        }
    }

    private boolean commonEligible(Minecraft mc) {
        return ScoreboardGate.allows(mc, config) && mc.player != null && mc.world != null
                && mc.screen == null && mc.focused && !mc.isPaused() && mc.player.isAlive() && !mc.player.isSpectator();
    }
    private boolean eligible(Minecraft mc, int button, boolean spam) {
        if (!commonEligible(mc) || mc.player.hasItemInUse()) return false;
        if ((spam ? config.spamDisableInCreative : config.disableInCreative) && mc.player.abilities.creativeMode) return false;
        if ((spam ? config.spamRequiresPlayer : config.requiresPlayer) && !nearbyPlayer(mc)) return false;
        ItemStack stack = mc.player.getItemInHand();
        Item held = stack == null ? null : stack.getItem();
        if (button == 0) {
            if (!spam && !config.leftClick) return false;
            if (spam ? !ItemAllowlist.allowsHand(held, config.spamItems, config.spamWeaponOnly, config.spamAllowFist)
                    : !ItemAllowlist.allowsHand(held, config.assistItems, config.weaponOnly, config.assistAllowFist)) return false;
            if ((spam ? config.spamOnlyWhileTargeting : config.onlyWhileTargeting) && (mc.crosshairTarget == null || mc.crosshairTarget.entity == null)) return false;
            return allowsBlock(spam ? config.spamClickThroughBlocks : !config.preserveMining,
                    mc.crosshairTarget != null && mc.crosshairTarget.type == HitResult.Type.BLOCK);
        }
        return (spam || config.rightClick) && (!(spam ? config.spamBlocksOnly : config.blocksOnly) || held instanceof BlockItem);
    }
    public static boolean allowsBlock(boolean throughBlocks, boolean targetingBlock) {
        return throughBlocks || !targetingBlock;
    }

    private static boolean entityTarget(Minecraft mc) {
        return mc.crosshairTarget != null && mc.crosshairTarget.type == HitResult.Type.ENTITY
                && mc.crosshairTarget.entity != null;
    }
    private boolean heldItemAllowed(Minecraft mc) {
        ItemStack stack = mc.player.getItemInHand();
        return stack != null && ItemAllowlist.allows(stack.getItem(), config.heldItems);
    }
    private HeldClickTrigger.Conditions heldConditions(Minecraft mc) {
        if (!config.heldClickEnabled)
            return new HeldClickTrigger.Conditions(false, false, false, false);
        boolean available = commonEligible(mc) && !mc.player.hasItemInUse()
                && (!config.heldCrouchCancel || !(mc.player.isSneaking() || physicalDown(mc.options.sneakKey.getKeyCode())));
        return new HeldClickTrigger.Conditions(available, entityTarget(mc), heldItemAllowed(mc), nearbyPlayer(mc));
    }
    private boolean nearbyPlayer(Minecraft mc) {
        var network = mc.getNetworkHandler();
        if (network == null) return false;
        var tabNames = network.getOnlinePlayers().stream()
                .map(info -> info.getProfile().getName()).toList();
        for (PlayerEntity other : mc.world.players) {
            double dx = mc.player.x - other.x;
            double dy = mc.player.y - other.y;
            double dz = mc.player.z - other.z;
            if (NearbyPlayers.qualifies(other == mc.player, other.isAlive(), other.isSpectator(),
                    dx * dx + dy * dy + dz * dz, other.getName(), tabNames)) return true;
        }
        return false;
    }

    public long lastClickNanos() { return engine.lastClickNanos(); }
    public ClickAssistEngine.Cps cps(int button) { return engine.cps(button, System.nanoTime()); }
    public int activeButton() {
        long now = System.nanoTime();
        activeButton = engine.dominantButton(now, activeButton);
        return activeButton;
    }
    @Override public void reset() {
        engine.reset();
        for (int button = 0; button < 2; button++) {
            if (ownsHold[button]) {
                int code = button == 0 ? attackCode : useCode;
                // Restore real input only in the same active gameplay context.
                boolean restore = client != null && client.screen == null && client.focused && !client.isPaused();
                KeyBinding.set(code, restore && physicalDown(code));
                debug("button_hold", button, "hold_up", code);
            }
            ownsHold[button] = held[button] = false;
            sessions[button].reset();
            heldTriggers[button].reset();
            sources[button] = -1; profiles[button] = null;
        }
        clearSpam();
    }
}
