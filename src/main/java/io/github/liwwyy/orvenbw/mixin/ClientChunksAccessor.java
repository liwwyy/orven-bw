package io.github.liwwyy.orvenbw.mixin;
import java.util.List;
import net.minecraft.client.world.chunk.ClientChunkCache;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ClientChunkCache.class)
public interface ClientChunksAccessor {
    @Accessor("chunks") List<WorldChunk> orven$loadedChunks();
}
