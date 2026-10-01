package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftInputMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void orven$checkContext(CallbackInfo ci) {
        if (OrvenBw.instance() != null) OrvenBw.instance().features().checkContext((Minecraft) (Object) this);
    }

    // These sites run only for real press events, before vanilla queues their keybinding clicks.
    // Synthetic KeyBinding.click calls occur outside this method and cannot count as physical input.
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;click(I)V", ordinal = 0))
    private void orven$mousePress(CallbackInfo ci) {
        if (OrvenBw.instance() != null && Mouse.getEventButton() >= 0) {
            OrvenBw.instance().features().onInput((Minecraft) (Object) this, Mouse.getEventButton() - 100);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;click(I)V", ordinal = 1))
    private void orven$keyboardPress(CallbackInfo ci) {
        if (OrvenBw.instance() != null && !Keyboard.isRepeatEvent()) {
            int key = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
            OrvenBw.instance().features().onInput((Minecraft) (Object) this, key);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/living/player/LocalClientPlayerEntity;hasItemInUse()Z", ordinal = 0))
    private void orven$beforeInteractions(CallbackInfo ci) {
        if (OrvenBw.instance() != null) OrvenBw.instance().features().beforeInteractions((Minecraft) (Object) this);
    }
}
