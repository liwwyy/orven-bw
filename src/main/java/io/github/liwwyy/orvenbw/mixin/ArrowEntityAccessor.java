package io.github.liwwyy.orvenbw.mixin;
import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ArrowEntity.class)
public interface ArrowEntityAccessor { @Accessor("inGround") boolean orven$inGround(); }
