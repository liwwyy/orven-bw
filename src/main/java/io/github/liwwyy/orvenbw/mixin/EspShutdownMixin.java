package io.github.liwwyy.orvenbw.mixin;
import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class EspShutdownMixin {
    @Inject(method = "shutdown", at = @At("HEAD"))
    private void orven$closeEsp(CallbackInfo ci) {
        var mod = OrvenBw.instance(); if (mod != null && Display.isCreated()) mod.esp().close();
    }
}
