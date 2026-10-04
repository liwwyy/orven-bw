package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe real mouse events wherever vanilla consumes them, including GUI screens. */
@Mixin(value = Mouse.class, remap = false)
public abstract class MouseDebugMixin {
    @Inject(method = "next", at = @At("RETURN"), remap = false)
    private static void orven$debugMouseEvent(CallbackInfoReturnable<Boolean> ci) {
        var mod = OrvenBw.instance();
        if (mod == null || !ci.getReturnValue() || Mouse.getEventButton() < 0) return;
        int button = Mouse.getEventButton();
        mod.debugLog().event("physical", "mouse", button == 0 ? "left" : button == 1 ? "right" : "mouse_" + button,
                Mouse.getEventButtonState() ? "press" : "release", button - 100, Mouse.getEventNanoseconds());
    }
}
