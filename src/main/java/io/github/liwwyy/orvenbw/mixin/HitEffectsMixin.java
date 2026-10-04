package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class HitEffectsMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void orven$observeAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (OrvenBw.instance() != null) OrvenBw.instance().hitEffects().attack(Minecraft.getInstance(), player, target);
    }
}
