package io.github.liwwyy.orvenbw.mixin;

import net.minecraft.client.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface InteractionManagerAccessor {
    @Invoker("updateSelectedHotbarSlot") void orven$updateSelectedHotbarSlot();
}
