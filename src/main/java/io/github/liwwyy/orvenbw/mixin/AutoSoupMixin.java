package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class AutoSoupMixin {
    @Inject(method = "runGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(FJ)V"))
    private void orven$soupFrame(CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod != null) mod.autoSoup().frame((Minecraft) (Object) this);
    }
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void orven$cancelSoupAttack(CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod != null && mod.autoSoup().blocksLeft()) ci.cancel();
    }
    @Inject(method = "handleMouseDown", at = @At("HEAD"), cancellable = true)
    private void orven$cancelSoupMining(boolean holdingAttack, CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod != null && mod.autoSoup().blocksLeft()) {
            Minecraft mc = (Minecraft) (Object) this;
            if (mc.interactionManager != null) mc.interactionManager.stopMiningBlock();
            ci.cancel();
        }
    }
    @Inject(method = "doUse", at = @At("HEAD"), cancellable = true)
    private void orven$protectSoupUse(CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod != null && (mod.autoBlock().blockVanillaUse() || mod.autoSoup().busy() && !mod.autoSoup().protectsUse((Minecraft) (Object) this))) ci.cancel();
    }
}
