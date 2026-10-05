package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Record the shared vanilla action path for physical and generated binding clicks. */
@Mixin(Minecraft.class)
public abstract class ClickActionDebugMixin {
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void orven$attackStart(CallbackInfo ci) { start(0); }
    @Inject(method = "doAttack", at = @At("RETURN"))
    private void orven$attackEnd(CallbackInfo ci) { end(); }
    @Inject(method = "doUse", at = @At("HEAD"))
    private void orven$useStart(CallbackInfo ci) { start(1); }
    @Inject(method = "doUse", at = @At("RETURN"))
    private void orven$useEnd(CallbackInfo ci) { end(); }

    private static void start(int side) {
        var mod = OrvenBw.instance();
        if (mod != null && mod.config().debugEnabled)
            mod.clickOrigins().beginAction(side, System.nanoTime());
    }
    private static void end() {
        var mod = OrvenBw.instance();
        if (mod != null) mod.clickOrigins().endAction();
    }
}
