package io.github.liwwyy.orvenbw.mixin;
import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.render.model.entity.PlayerModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerModel.class)
public abstract class EspPoseMixin {
    @Inject(method = "setupAnimation", at = @At("TAIL"))
    private void orven$pose(float walk, float speed, float bob, float yaw, float pitch, float scale, Entity entity, CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod != null && entity instanceof PlayerEntity player) mod.esp().renderer().capture(player,(PlayerModel)(Object)this);
    }
}
