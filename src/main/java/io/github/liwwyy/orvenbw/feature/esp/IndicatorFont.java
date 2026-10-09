package io.github.liwwyy.orvenbw.feature.esp;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Small GL glyph atlas built from OneConfig's bundled Poppins; no native GUI or window needed. */
final class IndicatorFont implements AutoCloseable {
    private int texture;
    private final int[] advance=new int[96];
    private boolean failed;
    private boolean load() {
        if(texture!=0) return true; if(failed) return false;
        try(var stream=getClass().getClassLoader().getResourceAsStream("assets/oneconfig/fonts/Poppins/Poppins-Regular.ttf")) {
            if(stream==null) throw new IllegalStateException("OneConfig Poppins resource unavailable");
            Font font=Font.createFont(Font.TRUETYPE_FONT,stream).deriveFont(12f);
            BufferedImage image=new BufferedImage(512,144,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();
            try {
                g.setFont(font); g.setColor(Color.WHITE); g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                for(int i=0;i<96;i++) { char c=(char)(i+32); advance[i]=g.getFontMetrics().charWidth(c); g.drawString(String.valueOf(c),(i%16)*32,(i/16)*24+15); }
            } finally { g.dispose(); }
            var pixels=BufferUtils.createByteBuffer(512*144*4);
            for(int y=0;y<144;y++) for(int x=0;x<512;x++) { int c=image.getRGB(x,y); pixels.put((byte)255).put((byte)255).put((byte)255).put((byte)(c>>>24)); }
            pixels.flip(); texture=GL11.glGenTextures(); GlStateManager.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,512,144,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
            return true;
        } catch(Exception error) { failed=true; OrvenBw.LOGGER.warn("Indicator Poppins font unavailable; using Minecraft font",error); return false; }
    }
    void centered(Minecraft mc,String text,float x,float y,int color,boolean poppins) {
        if(!poppins||!load()) { mc.textRenderer.drawWithShadow(text,x-mc.textRenderer.getWidth(text)/2f,y,color); return; }
        int width=0; for(char c:text.toCharArray()) width+=advance[Math.clamp(c-32,0,95)];
        float left=x-width/2f;
        GlStateManager.enableTexture(); GlStateManager.bindTexture(texture); GlStateManager.color4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
        GL11.glBegin(GL11.GL_QUADS);
        for(char c:text.toCharArray()) {
            int i=Math.clamp(c-32,0,95); float u=(i%16)*32/512f,v=(i/16)*24/144f;
            GL11.glTexCoord2f(u,v); GL11.glVertex2f(left,y-3);
            GL11.glTexCoord2f(u+32/512f,v); GL11.glVertex2f(left+32,y-3);
            GL11.glTexCoord2f(u+32/512f,v+24/144f); GL11.glVertex2f(left+32,y+21);
            GL11.glTexCoord2f(u,v+24/144f); GL11.glVertex2f(left,y+21); left+=advance[i];
        }
        GL11.glEnd();
    }
    @Override public void close() { if(texture!=0) { GL11.glDeleteTextures(texture); texture=0; } }
}
