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
            uniform sampler2D sceneDepth;
            uniform sampler2D entityDepth;
            uniform vec2 pixel;
            uniform vec2 projectionDepth;
            uniform bool occludedOnly;
            uniform float borderWidth;
            float covered(vec2 uv) {
                float a=texture2D(image,uv).a;
                if(occludedOnly && texture2D(entityDepth,uv).r <= texture2D(sceneDepth,uv).r+0.00000012) a=0.0;
                return a;
            }
            void main() {
                vec2 uv=gl_TexCoord[0].xy;
                float centre=covered(uv);
                vec4 nearest=vec4(0.0);
                for(int x=-6;x<=6;x++) for(int y=-6;y<=6;y++) {
                    if(max(abs(float(x)),abs(float(y)))>ceil(borderWidth)) continue;
                    vec2 at=uv+vec2(float(x),float(y))*pixel;
                    vec4 colour=texture2D(image,at);
                    float depth=texture2D(entityDepth,at).r*2.0-1.0;
                    float distance=abs(projectionDepth.y/(depth+projectionDepth.x));
                    float radius=clamp(borderWidth*12.0/max(distance,1.0),0.35,borderWidth);
                    float weight=clamp(radius+0.5-max(abs(float(x)),abs(float(y))),0.0,1.0);
                    colour.a=covered(at)*weight;
                    if(colour.a>nearest.a) nearest=colour;
                }
                gl_FragColor=vec4(nearest.rgb,nearest.a*(1.0-centre));
            }
            """;
    private final EspDiagnostics diagnostics;
    EspOutline(EspDiagnostics diagnostics) { this.diagnostics=diagnostics; }
    private RenderTarget target;
    private int mask, edge, sceneDepth, entityDepth;
    private boolean failed, terrainReady;
    private int terrainWidth,terrainHeight;
    static boolean drawing;
    void beginWorld() { terrainReady=false; }
    void captureTerrain(Minecraft mc) {
        if(failed || !GLX.useFbo() || !GLContext.getCapabilities().OpenGL20) return;
        try(var ignored=new EspGlState()) {
            if(sceneDepth==0) sceneDepth=GL11.glGenTextures();
            copyDepth(sceneDepth,mc.width,mc.height);
            terrainWidth=mc.width;terrainHeight=mc.height;terrainReady=true;
        } catch(RuntimeException error) { fail(error); }
    }
    void render(Minecraft mc, float delta, List<PlayerEntity> players, ToIntFunction<PlayerEntity> color, boolean invisible, boolean occludedOnly, float[] projection, float width) {
        if (failed || drawing || players.isEmpty() || occludedOnly && (!terrainReady || terrainWidth!=mc.width || terrainHeight!=mc.height)) return;
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
            if(sceneDepth==0) sceneDepth=GL11.glGenTextures();
            if(entityDepth==0) entityDepth=GL11.glGenTextures();
            // RenderTarget construction/resizing unbinds to framebuffer zero. Restore the world
            // framebuffer before taking its depth, including the first frame after a resize.
            GLX.bindFramebuffer(GLX.GL_FRAMEBUFFER,framebuffer);
            if(!occludedOnly) copyDepth(sceneDepth,mc.width,mc.height);
            GlStateManager.depthMask(true); GlStateManager.colorMask(true,true,true,true);
            target.clear(); target.bindWrite(true);
            GlStateManager.disableFog(); GlStateManager.disableLighting(); GlStateManager.enableTexture();
            GlStateManager.enableDepthTest(); GlStateManager.depthFunc(GL11.GL_LEQUAL); GlStateManager.depthMask(true);
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
            copyDepth(entityDepth,mc.width,mc.height);
            GLX.bindFramebuffer(GLX.GL_FRAMEBUFFER,framebuffer);
            GL20.glUseProgram(edge); GL20.glUniform1i(GL20.glGetUniformLocation(edge, "image"), 0);
            GL20.glUniform2f(GL20.glGetUniformLocation(edge, "pixel"), 1f / target.width, 1f / target.height);
            GL20.glUniform1i(GL20.glGetUniformLocation(edge,"sceneDepth"),1);
            GL20.glUniform1i(GL20.glGetUniformLocation(edge,"entityDepth"),2);
            GL20.glUniform1i(GL20.glGetUniformLocation(edge,"occludedOnly"),occludedOnly?1:0);
            GL20.glUniform1f(GL20.glGetUniformLocation(edge,"borderWidth"),Math.clamp(width,1,6));
            GL20.glUniform2f(GL20.glGetUniformLocation(edge,"projectionDepth"),projection[10],projection[14]);
            GlStateManager.activeTexture(GL13.GL_TEXTURE1); GlStateManager.bindTexture(sceneDepth);
            GlStateManager.activeTexture(GL13.GL_TEXTURE2); GlStateManager.bindTexture(entityDepth);
            GlStateManager.activeTexture(GL13.GL_TEXTURE0);
            GlStateManager.disableDepthTest(); GlStateManager.depthMask(false);
            // RenderTarget.draw() establishes its own pixel-space matrices and texture coordinates.
            GlStateManager.enableBlend(); GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            target.draw(mc.width, mc.height, false);
        } catch (RuntimeException error) { fail(error); }
        finally {
            terrainReady=false;
            drawing = false; mc.options.entityShadows = shadows; mc.getEntityRenderDispatcher().setRenderShadow(dispatcherShadow);
            GL20.glUseProgram(oldProgram); GLX.bindFramebuffer(GLX.GL_FRAMEBUFFER,framebuffer);
        }
    }
    private static void copyDepth(int texture,int width,int height) {
        GlStateManager.activeTexture(GL13.GL_TEXTURE0); GlStateManager.bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL14.GL_TEXTURE_COMPARE_MODE,GL11.GL_NONE);
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D,0,GL14.GL_DEPTH_COMPONENT32,0,0,width,height,0);
    }
    private void fail(RuntimeException error) {
        if (!failed) OrvenBw.LOGGER.warn("Player ESP Outline unavailable; other render styles remain usable", error);
        diagnostics.record("outline-failure",error.toString());
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
        terrainReady=false;
        if (target != null) { target.destroyBuffers(); target = null; }
        if (mask != 0) { GL20.glDeleteProgram(mask); mask = 0; }
        if(sceneDepth!=0) { GL11.glDeleteTextures(sceneDepth); sceneDepth=0; }
        if(entityDepth!=0) { GL11.glDeleteTextures(entityDepth); entityDepth=0; }
        if (edge != 0) { GL20.glDeleteProgram(edge); edge = 0; }
    }
}
