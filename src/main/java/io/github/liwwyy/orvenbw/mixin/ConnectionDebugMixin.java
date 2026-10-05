package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.network.Connection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerUseC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Timestamp an outgoing interaction submission while its vanilla action is active. */
@Mixin(Connection.class)
public abstract class ConnectionDebugMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void orven$packet(Packet packet, CallbackInfo ci) {
        var mod = OrvenBw.instance();
        if (mod == null || !mod.config().debugEnabled) return;
        String side, kind;
        if (packet instanceof PlayerInteractEntityC2SPacket interaction) {
            side = interaction.getAction() == PlayerInteractEntityC2SPacket.Action.ATTACK ? "left" : "right";
            kind = "entity_" + interaction.getAction().name().toLowerCase(java.util.Locale.ROOT);
        } else if (packet instanceof PlayerUseC2SPacket) {
            side = "right"; kind = "use";
        } else if (packet instanceof PlayerHandActionC2SPacket hand
                && hand.getAction() == PlayerHandActionC2SPacket.Action.START_DESTROY_BLOCK) {
            side = "left"; kind = "start_destroy_block";
        } else return;
        mod.clickOrigins().packet(side, kind, System.nanoTime());
    }
}
