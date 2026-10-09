package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameGui.class)
public abstract class HitEffectsRenderMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void orven$renderHitEffects(float tickDelta, CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod != null) { mod.hitEffects().render(Minecraft.getInstance()); mod.esp().renderHud(Minecraft.getInstance()); mod.indicators().renderHud(Minecraft.getInstance()); }
    }
}
