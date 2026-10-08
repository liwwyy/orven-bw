package io.github.liwwyy.orvenbw.feature.esp;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.pipeline.RenderTarget;
import net.minecraft.client.render.platform.*;
import net.minecraft.entity.living.player.PlayerEntity;
import org.lwjgl.opengl.*;
import java.util.List;
import java.util.function.ToIntFunction;

/** Silhouette-mask pass followed by a two-pixel edge shader; lazily allocated on the GL thread. */
final class EspOutline implements AutoCloseable {
    private static final String VERTEX = """
            #version 120
            void main() { gl_Position = ftransform(); gl_TexCoord[0] = gl_MultiTexCoord0; }
            """;
    private static final String MASK = """
            #version 120
            uniform sampler2D image;
            uniform vec4 tint;
            void main() { gl_FragColor = vec4(tint.rgb, tint.a * texture2D(image, gl_TexCoord[0].xy).a); }
            """;
    private static final String EDGE = """
            #version 120
            uniform sampler2D image;
            uniform vec2 pixel;
            void main() {
                vec2 uv = gl_TexCoord[0].xy;
                vec4 centre = texture2D(image, uv);
                vec4 nearest = vec4(0.0);
                for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) {
                    vec4 sampleColour = texture2D(image, uv + vec2(float(x), float(y)) * pixel);
                    if (sampleColour.a > nearest.a) nearest = sampleColour;
                }
                gl_FragColor = vec4(nearest.rgb, nearest.a * (1.0 - centre.a));
            }
            """;
    private RenderTarget target;
    private int mask, edge;
    private boolean failed;
    static boolean drawing;
    void render(Minecraft mc, float delta, List<PlayerEntity> players, ToIntFunction<PlayerEntity> color, boolean invisible) {
        if (failed || drawing || players.isEmpty()) return;
        if (!GLX.useFbo() || !GLContext.getCapabilities().OpenGL20) { fail(new IllegalStateException("ESP Outline requires framebuffer and GLSL support")); return; }
        boolean shadows = mc.options.entityShadows, dispatcherShadow = mc.getEntityRenderDispatcher().shouldRenderShadow();
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int framebuffer = GL11.glGetInteger(0x8ca6);
        try (var ignored = new EspGlState()) {
            if (mask == 0) mask = program(MASK);
            if (edge == 0) edge = program(EDGE);
            if (target == null || target.viewWidth != mc.width || target.viewHeight != mc.height) {
                if (target != null) target.destroyBuffers();
                target = new RenderTarget(mc.width, mc.height, true); target.setClearColor(0,0,0,0);
            }
            target.clear(); target.bindWrite(true);
            GlStateManager.disableFog(); GlStateManager.disableLighting(); GlStateManager.enableTexture();
            GlStateManager.disableDepthTest(); GlStateManager.depthMask(false);
            GlStateManager.enableBlend(); GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            mc.options.entityShadows = false; mc.getEntityRenderDispatcher().setRenderShadow(false); drawing = true;
            GL20.glUseProgram(mask); GL20.glUniform1i(GL20.glGetUniformLocation(mask, "image"), 0);
            for (PlayerEntity player : players) {
                int rgb = color.applyAsInt(player);
                GL20.glUniform4f(GL20.glGetUniformLocation(mask, "tint"), (rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, 1);
                boolean hidden = player.isInvisible();
                try {
                    if (hidden && invisible) player.setInvisible(false);
                    mc.getEntityRenderDispatcher().render(player, delta, true);
                } finally { if (hidden && invisible) player.setInvisible(true); }
            }
            drawing = false;
            GLX.bindFramebuffer(GLX.GL_FRAMEBUFFER,framebuffer);
            GL20.glUseProgram(edge); GL20.glUniform1i(GL20.glGetUniformLocation(edge, "image"), 0);
            GL20.glUniform2f(GL20.glGetUniformLocation(edge, "pixel"), 1f / target.width, 1f / target.height);
            // RenderTarget.draw() establishes its own pixel-space matrices and texture coordinates.
            GlStateManager.enableBlend(); GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            target.draw(mc.width, mc.height, false);
        } catch (RuntimeException error) { fail(error); }
        finally {
            drawing = false; mc.options.entityShadows = shadows; mc.getEntityRenderDispatcher().setRenderShadow(dispatcherShadow);
            GL20.glUseProgram(oldProgram); GLX.bindFramebuffer(GLX.GL_FRAMEBUFFER,framebuffer);
        }
    }
    private void fail(RuntimeException error) {
        if (!failed) OrvenBw.LOGGER.warn("Player ESP Outline unavailable; other render styles remain usable", error);
        failed = true; close();
    }
    private static int shader(int kind, String source) {
        int shader = GL20.glCreateShader(kind);
        GL20.glShaderSource(shader, source); GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096); GL20.glDeleteShader(shader); throw new IllegalStateException(log);
        }
        return shader;
    }
    private static int program(String fragment) {
        int vertex = 0, frag = 0, result = 0;
        try {
            vertex = shader(GL20.GL_VERTEX_SHADER, VERTEX); frag = shader(GL20.GL_FRAGMENT_SHADER, fragment);
            result = GL20.glCreateProgram(); GL20.glAttachShader(result, vertex); GL20.glAttachShader(result, frag); GL20.glLinkProgram(result);
            if (GL20.glGetProgrami(result, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) throw new IllegalStateException(GL20.glGetProgramInfoLog(result, 4096));
            return result;
        } catch (RuntimeException e) { if (result != 0) GL20.glDeleteProgram(result); throw e; }
        finally { if (vertex != 0) GL20.glDeleteShader(vertex); if (frag != 0) GL20.glDeleteShader(frag); }
    }
    @Override public void close() {
        if (target != null) { target.destroyBuffers(); target = null; }
        if (mask != 0) { GL20.glDeleteProgram(mask); mask = 0; }
        if (edge != 0) { GL20.glDeleteProgram(edge); edge = 0; }
    }
}
