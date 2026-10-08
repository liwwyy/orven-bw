package io.github.liwwyy.orvenbw.mixin;
import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class EspWorldRenderMixin {
    // The world matrices are still active here, before the hand pass changes projection/clears depth.
    @Inject(method = "render(IFJ)V", at = @At(value = "CONSTANT", args = "stringValue=hand"))
    private void orven$esp(int pass, float delta, long limit, CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod != null) mod.esp().renderWorld(Minecraft.getInstance(),delta);
    }
}
