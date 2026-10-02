package io.github.liwwyy.orvenbw.feature.clickassist;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.ClientFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.world.HitResult;
import java.util.concurrent.ThreadLocalRandom;
import io.github.liwwyy.orvenbw.feature.ScoreboardGate;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

public final class ClickAssistFeature implements ClientFeature {
    private final OrvenConfig config;
    private final ClickAssistEngine engine = new ClickAssistEngine(() -> ThreadLocalRandom.current().nextDouble());
    private int attackCode = Integer.MIN_VALUE;
    private int useCode = Integer.MIN_VALUE;

    private final ClickSession[] sessions = {new ClickSession(() -> ThreadLocalRandom.current().nextDouble()), new ClickSession(() -> ThreadLocalRandom.current().nextDouble())};
    private final boolean[] held = new boolean[2];
    private final boolean[] ownsHold = new boolean[2];
    private boolean spamPressed, spamLatched;
    private int lastMode = -1, lastButton = -1;
    private int activeButton;
    private Minecraft client;
    private String bindSignature;
    private final int[] sources = {-1, -1};
    private final ClickSession.Options[] profiles = new ClickSession.Options[2];

    public void activation(int action, boolean down) {
        Minecraft mc = Minecraft.getInstance();
        if (!commonEligible(mc) || mc.screen != null || !mc.focused || mc.isPaused()) return;
        if (lastMode != config.spamMode || lastButton != config.spamButton) {
            spamPressed = spamLatched = false;
            lastMode = config.spamMode; lastButton = config.spamButton;
        }
        if (action == 0) {
            if (!config.spamEnabled) return;
            spamPressed = down;
            if (down && config.spamMode == 1) spamLatched = !spamLatched;
        } else if (down && config.holdEnabled) held[action - 1] = !held[action - 1];
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
        String signature = bindState(config.spamBind) + bindState(config.holdLeftBind) + bindState(config.holdRightBind);
        if (bindSignature != null && !bindSignature.equals(signature)) reset();
        bindSignature = signature;
        if (!commonEligible(mc)) { reset(); return; }
        if (lastMode != config.spamMode || lastButton != config.spamButton) {
            spamPressed = spamLatched = false;
            for (ClickSession session : sessions) session.reset();
            lastMode = config.spamMode; lastButton = config.spamButton;
        }
        if (!config.spamEnabled) spamPressed = spamLatched = false;
        if (!config.holdEnabled) held[0] = held[1] = false;
        long now = System.nanoTime();
        boolean spam = config.spamEnabled && (config.spamMode == 1 ? spamLatched : spamPressed);
        for (int button = 0; button <= 1; button++) {
            int code = button == 0 ? attackCode : useCode;
            if (held[button]) {
                if (!ownsHold[button]) KeyBinding.click(code);
                KeyBinding.set(code, true);
                ownsHold[button] = true;
            } else if (ownsHold[button]) {
                KeyBinding.set(code, physicalDown(code));
                ownsHold[button] = false;
            }
            boolean spamming = spam && button == config.spamButton;
            boolean permitted = !held[button] && eligible(mc, button, spamming);
            boolean clicking = permitted && (spamming || config.enabled && engine.manuallyActive(button, now, config.activationCps));
            var profile = config.rateOptions(spamming);
            int source = spamming ? 1 : 0;
            if (sources[button] != source || !profile.equals(profiles[button])) {
                sessions[button].reset(); engine.cancel(button);
                sources[button] = source; profiles[button] = profile;
            }
            double base = engine.manualRate(button, now);
            double target = sessions[button].target(now, clicking, spamming ? 5 : base + 1, profile);
            double generated = Math.max(0, target - base);
            boolean clicked = engine.poll(button, now, generated, clicking,
                    spamming ? config.spamVaryTiming : config.assistVaryTiming,
                    spamming ? 0 : config.boostDelayMs * 1_000_000L);
            if (clicked) {
                KeyBinding.click(code);
                activeButton = button;
            }
        }
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
            if ((spam ? config.spamWeaponOnly : config.weaponOnly) && !allowedWeapon(held, spam)) return false;
            if ((spam ? config.spamOnlyWhileTargeting : config.onlyWhileTargeting) && (mc.crosshairTarget == null || mc.crosshairTarget.entity == null)) return false;
            return allowsBlock(spam ? config.spamClickThroughBlocks : !config.preserveMining,
                    mc.crosshairTarget != null && mc.crosshairTarget.type == HitResult.Type.BLOCK);
        }
        return (spam || config.rightClick) && (!(spam ? config.spamBlocksOnly : config.blocksOnly) || held instanceof BlockItem);
    }
    public static boolean allowsBlock(boolean throughBlocks, boolean targetingBlock) {
        return throughBlocks || !targetingBlock;
    }

    private boolean allowedWeapon(Item item, boolean spam) {
        return (spam ? config.spamSwords : config.swords) && item instanceof SwordItem
                || (spam ? config.spamAxes : config.axes) && item instanceof AxeItem
                || (spam ? config.spamRods : config.rods) && item instanceof FishingRodItem
                || (spam ? config.spamSticks : config.sticks) && item == Items.STICK
                || (spam ? config.spamHoes : config.hoes) && item instanceof HoeItem
                || (spam ? config.spamShovels : config.shovels) && item instanceof ShovelItem;
    }

    private boolean nearbyPlayer(Minecraft mc) {
        for (PlayerEntity other : mc.world.players) {
            if (other == mc.player || !other.isAlive() || other.isSpectator()) continue;
            double dx = mc.player.x - other.x;
            double dy = mc.player.y - other.y;
            double dz = mc.player.z - other.z;
            if (dx * dx + dy * dy + dz * dz <= 16.0) return true;
        }
        return false;
    }

    public ClickAssistEngine.Cps cps(int button) { return engine.cps(button, System.nanoTime()); }
    public int activeButton() { return activeButton; }
    @Override public void reset() {
        engine.reset();
        for (int button = 0; button < 2; button++) {
            if (ownsHold[button]) {
                int code = button == 0 ? attackCode : useCode;
                // Restore real input only in the same active gameplay context.
                boolean restore = client != null && client.screen == null && client.focused && !client.isPaused();
                KeyBinding.set(code, restore && physicalDown(code));
            }
            ownsHold[button] = held[button] = false;
            sessions[button].reset();
            sources[button] = -1; profiles[button] = null;
        }
        spamPressed = spamLatched = false;
    }
}
