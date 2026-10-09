package io.github.liwwyy.orvenbw.mixin;
import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.client.render.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ItemInHandRenderer.class)
public abstract class AutoBlockAnimationMixin {
    @Redirect(method="renderInFirstPerson",at=@At(value="INVOKE",target="Lnet/minecraft/client/entity/living/player/ClientPlayerEntity;getItemUseTimer()I"))
    private int orven$localBlockPose(ClientPlayerEntity player) {
        var mod=OrvenBw.instance();
        return mod!=null&&mod.autoBlock().forceAnimation()?Math.max(1,player.getItemUseTimer()):player.getItemUseTimer();
    }
}
