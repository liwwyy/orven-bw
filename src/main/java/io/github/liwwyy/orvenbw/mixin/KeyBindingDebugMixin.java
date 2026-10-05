package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe every attack/use binding queue call, including calls from other mods. */
@Mixin(KeyBinding.class)
public abstract class KeyBindingDebugMixin {
    @Inject(method = "click", at = @At("HEAD"))
    private static void orven$queued(int keyCode, CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod == null || !mod.config().debugEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        int side = keyCode == mc.options.attackKey.getKeyCode() ? 0
                : keyCode == mc.options.useKey.getKeyCode() ? 1 : -1;
        if (side >= 0) mod.clickOrigins().queued(side, keyCode, System.nanoTime());
    }

    @Inject(method = "consumeClick", at = @At("RETURN"))
    private void orven$consumed(CallbackInfoReturnable<Boolean> ci) {
        var mod = OrvenBw.instance();
        if (mod == null || !mod.config().debugEnabled || !ci.getReturnValue()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        int side = (Object) this == mc.options.attackKey ? 0
                : (Object) this == mc.options.useKey ? 1 : -1;
        if (side >= 0) mod.clickOrigins().consumed(side);
    }
}
