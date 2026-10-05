package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class HitEffectsStatusMixin {
    @Inject(method = "doEvent", at = @At("HEAD"))
    private void orven$hurtNotification(byte event, CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (event == 2 && mod != null) mod.hitEffects().hurt(Minecraft.getInstance(), (LivingEntity) (Object) this);
    }
}
