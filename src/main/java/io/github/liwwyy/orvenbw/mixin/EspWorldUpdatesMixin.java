package io.github.liwwyy.orvenbw.mixin;
import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ClientWorld.class)
public abstract class EspWorldUpdatesMixin {
    @Inject(method = "setBlockStateFromPacket", at = @At("RETURN"))
    private void orven$block(BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        var mod = OrvenBw.instance(); var mc = Minecraft.getInstance();
        if (mod != null && mc.world == (Object)this) mod.esp().blockChanged(mc,pos);
    }
}
