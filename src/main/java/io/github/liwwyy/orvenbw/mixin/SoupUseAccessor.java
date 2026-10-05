package io.github.liwwyy.orvenbw.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface SoupUseAccessor {
    @Invoker("doUse") void orven$useSoup();
}
