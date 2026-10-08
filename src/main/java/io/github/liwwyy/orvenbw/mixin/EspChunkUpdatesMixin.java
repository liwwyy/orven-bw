package io.github.liwwyy.orvenbw.mixin;
import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public abstract class EspChunkUpdatesMixin {
    @Inject(method = "handleChatMessage", at = @At("TAIL"))
    private void orven$bedChat(ChatMessageS2CPacket packet, CallbackInfo ci) {
        var mod = OrvenBw.instance(); if (mod != null && packet.getType() != 2) mod.esp().chat(packet.getMessage().getFormattedString());
    }
    @Inject(method = "handleWorldChunk", at = @At("TAIL"))
    private void orven$chunk(WorldChunkS2CPacket packet, CallbackInfo ci) {
        var mod = OrvenBw.instance(); if (mod != null) mod.esp().chunkChanged(Minecraft.getInstance(),packet.getChunkX(),packet.getChunkZ());
    }
    @Inject(method = "handleWorldChunks", at = @At("TAIL"))
    private void orven$chunks(WorldChunksS2CPacket packet, CallbackInfo ci) {
        var mod = OrvenBw.instance(); if (mod != null) for (int i=0;i<packet.getChunkCount();i++) mod.esp().chunkChanged(Minecraft.getInstance(),packet.getChunkX(i),packet.getChunkZ(i));
    }
}
