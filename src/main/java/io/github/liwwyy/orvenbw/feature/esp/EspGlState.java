package io.github.liwwyy.orvenbw.feature.esp;

import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import java.nio.FloatBuffer;

/** Restore both OpenGL state and vanilla's cached state after overlays/entity passes. */
final class EspGlState implements AutoCloseable {
    private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int[] textureIds = new int[3];
    private final boolean[] textures = new boolean[3], lights = new boolean[8];
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND),
            alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST), lighting = GL11.glIsEnabled(GL11.GL_LIGHTING),
            fog = GL11.glIsEnabled(GL11.GL_FOG), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE),
            mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), normalize = GL11.glIsEnabled(GL11.GL_NORMALIZE),
            rescale = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL), colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
    private final int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
            srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
            alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC), depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    private final float alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
    private final FloatBuffer color = BufferUtils.createFloatBuffer(4), clearColor = BufferUtils.createFloatBuffer(4);
    private final java.nio.ByteBuffer colorMask = BufferUtils.createByteBuffer(4);
    EspGlState() {
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
        GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, colorMask);
        for (int i = 0; i < textureIds.length; i++) {
            GlStateManager.activeTexture(GL13.GL_TEXTURE0 + i);
            textureIds[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D); textures[i] = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        }
        GlStateManager.activeTexture(activeTexture);
        for (int i = 0; i < lights.length; i++) lights[i] = GL11.glIsEnabled(GL11.GL_LIGHT0 + i);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        GL11.glMatrixMode(matrixMode);
    }
    @Override public void close() {
        // Restore through vanilla first, so glPopAttrib doesn't leave its cached booleans stale.
        if (depth) GlStateManager.enableDepthTest(); else GlStateManager.disableDepthTest();
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (alpha) GlStateManager.enableAlphaTest(); else GlStateManager.disableAlphaTest();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        if (fog) GlStateManager.enableFog(); else GlStateManager.disableFog();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        if (normalize) GlStateManager.enableNormalize(); else GlStateManager.disableNormalize();
        if (rescale) GlStateManager.enableRescaleNormal(); else GlStateManager.disableRescaleNormal();
        if (colorMaterial) GlStateManager.enableColorMaterial(); else GlStateManager.disableColorMaterial();
        for (int i = 0; i < lights.length; i++) if (lights[i]) GlStateManager.enableLight(i); else GlStateManager.disableLight(i);
        GlStateManager.depthMask(mask); GlStateManager.depthFunc(depthFunc); GlStateManager.alphaFunc(alphaFunc, alphaRef);
        GlStateManager.blendFuncSeparate(src, dst, srcAlpha, dstAlpha);
        for (int i = 0; i < textureIds.length; i++) {
            GlStateManager.activeTexture(GL13.GL_TEXTURE0 + i); GlStateManager.bindTexture(textureIds[i]);
            if (textures[i]) GlStateManager.enableTexture(); else GlStateManager.disableTexture();
        }
        GlStateManager.activeTexture(activeTexture);
        GlStateManager.color4f(color.get(0), color.get(1), color.get(2), color.get(3));
        GlStateManager.colorMask(colorMask.get(0) != 0,colorMask.get(1) != 0,colorMask.get(2) != 0,colorMask.get(3) != 0);
        GlStateManager.clearColor(clearColor.get(0),clearColor.get(1),clearColor.get(2),clearColor.get(3));
        GL20.glUseProgram(program);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glMatrixMode(matrixMode); GL11.glPopAttrib();
    }
}
