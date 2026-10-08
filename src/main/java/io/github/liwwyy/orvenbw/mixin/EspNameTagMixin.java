package io.github.liwwyy.orvenbw.mixin;
import io.github.liwwyy.orvenbw.feature.esp.EspRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.living.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntityRenderer.class)
public abstract class EspNameTagMixin {
    @Inject(method = "shouldRenderNameTag(Lnet/minecraft/entity/living/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void orven$outlineName(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (EspRenderer.drawingOutline()) cir.setReturnValue(false);
    }
}
