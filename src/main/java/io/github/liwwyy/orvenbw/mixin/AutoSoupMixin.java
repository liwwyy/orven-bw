package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    // Vanilla releases any item use if the physical use key is up. Our soup action owns
    // its release deadline; other uses and manual slot changes retain vanilla behavior.
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientPlayerInteractionManager;stopUsingHand(Lnet/minecraft/entity/living/player/PlayerEntity;)V"))
    private void orven$keepSoupUse(ClientPlayerInteractionManager manager, PlayerEntity player) {
        var mod = OrvenBw.instance();
        if (mod == null || !mod.autoSoup().protectsUse((Minecraft) (Object) this)) manager.stopUsingHand(player);
    }
    @Inject(method = "doUse", at = @At("HEAD"), cancellable = true)
    private void orven$protectSoupUse(CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod != null && mod.autoSoup().busy() && !mod.autoSoup().issuingUse()) ci.cancel();
    }
}
