package io.github.liwwyy.orvenbw.mixin;
import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public abstract class AutoBlockNetworkMixin {
    @Inject(method="sendPacket",at=@At("HEAD"),cancellable=true)
    private void orven$orderedLag(Packet<?> packet,CallbackInfo ci) {
        var mod=OrvenBw.instance();
        if(mod!=null && mod.autoBlock().outgoing((ClientPlayNetworkHandler)(Object)this,packet)) ci.cancel();
    }
}
