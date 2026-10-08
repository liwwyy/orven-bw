package io.github.liwwyy.orvenbw.feature.esp;

import java.lang.classfile.*;
import java.lang.classfile.instruction.ConstantInstruction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Inspect class bytes only: never construct or launch a Minecraft instance. */
class EspHooksTest {
    private ClassModel model(String name) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("net/minecraft/" + name + ".class")) {
            assertNotNull(stream,name); return ClassFile.of().parse(stream.readAllBytes());
        }
    }
    private MethodModel method(String owner,String name,String descriptor) throws Exception {
        return model(owner).methods().stream().filter(m -> m.methodName().stringValue().equals(name) && m.methodType().stringValue().equals(descriptor))
                .findFirst().orElseThrow(() -> new AssertionError(owner + "." + name + descriptor));
    }
    @Test void worldHookIsBeforeTheHandPassInTheExactMappedOverload() throws Exception {
        var method=method("client/render/GameRenderer","render","(IFJ)V");
        assertEquals(1,method.code().orElseThrow().elementStream()
                .filter(e -> e instanceof ConstantInstruction constant && "hand".equals(constant.constantValue())).count());
    }
    @Test void poseNameChunkAndShutdownSignaturesExistWithoutInitializingMinecraft() throws Exception {
        method("client/render/model/entity/PlayerModel","setupAnimation","(FFFFFFLnet/minecraft/entity/Entity;)V");
        var tag=method("client/render/entity/LivingEntityRenderer","shouldRenderNameTag","(Lnet/minecraft/entity/living/LivingEntity;)Z");
        assertFalse(tag.flags().has(java.lang.reflect.AccessFlag.BRIDGE));
        method("client/world/ClientWorld","setBlockStateFromPacket","(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/BlockState;)Z");
        for (String[] hook : new String[][]{{"handleWorldChunk","WorldChunkS2CPacket"},{"handleWorldChunks","WorldChunksS2CPacket"},{"handleChatMessage","ChatMessageS2CPacket"}})
            method("client/network/handler/ClientPlayNetworkHandler",hook[0],"(Lnet/minecraft/network/packet/s2c/play/"+hook[1]+";)V");
        method("client/Minecraft","shutdown","()V");
        assertTrue(model("client/world/chunk/ClientChunkCache").fields().stream().anyMatch(f -> f.fieldName().stringValue().equals("chunks") && f.fieldType().stringValue().equals("Ljava/util/List;")));
    }
}
