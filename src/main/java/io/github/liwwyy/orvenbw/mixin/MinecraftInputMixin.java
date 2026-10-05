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
        var mod = OrvenBw.instance();
        if (mod != null) {
            Minecraft mc = (Minecraft) (Object) this;
            if (!mod.config().debugEnabled || mc.screen != null || !mc.focused || mc.world == null)
                mod.clickOrigins().reset();
            else mod.clickOrigins().nextTick();
            mod.features().checkContext(mc);
        }
    }

    // These sites run only for real press events, before vanilla queues their keybinding clicks.
    // Synthetic KeyBinding.click calls occur outside this method and cannot count as physical input.
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;click(I)V", ordinal = 0))
    private void orven$mousePress(CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod != null && Mouse.getEventButton() >= 0) {
            Minecraft mc = (Minecraft) (Object) this;
            int key = Mouse.getEventButton() - 100;
            if (mod.config().debugEnabled && (key == mc.options.attackKey.getKeyCode() || key == mc.options.useKey.getKeyCode()))
                mod.clickOrigins().prepare("physical", "mouse", null, -1);
            mod.features().onInput(mc, key);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;click(I)V", ordinal = 1))
    private void orven$keyboardPress(CallbackInfo ci) {
        if (OrvenBw.instance() != null && !Keyboard.isRepeatEvent()) {
            int key = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
            Minecraft mc = (Minecraft) (Object) this;
            if (OrvenBw.instance().config().debugEnabled &&
                    (key == mc.options.attackKey.getKeyCode() || key == mc.options.useKey.getKeyCode()))
                OrvenBw.instance().clickOrigins().prepare("physical", "keyboard_binding", null, -1);
            if (key == mc.options.attackKey.getKeyCode() || key == mc.options.useKey.getKeyCode())
                OrvenBw.instance().debugLog().event("physical", "keyboard_binding", key == mc.options.attackKey.getKeyCode() ? "left" : "right", "press", key, -1);
            OrvenBw.instance().features().onInput((Minecraft) (Object) this, key);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/living/player/LocalClientPlayerEntity;hasItemInUse()Z", ordinal = 0))
    private void orven$beforeInteractions(CallbackInfo ci) {
        if (OrvenBw.instance() != null) OrvenBw.instance().features().beforeInteractions((Minecraft) (Object) this);
    }
}
