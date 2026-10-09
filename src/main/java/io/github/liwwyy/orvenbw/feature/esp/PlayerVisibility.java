package io.github.liwwyy.orvenbw.feature.esp;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class PlayerVisibility {
    private PlayerVisibility() {}
    private static boolean clear(Minecraft mc, PlayerEntity player, double fraction) {
        Vec3d eye=new Vec3d(mc.player.x,mc.player.y+mc.player.getEyeHeight(),mc.player.z);
        var b=player.getShape();
        return mc.world.rayTrace(eye,new Vec3d(player.x,b.minY+(b.maxY-b.minY)*fraction,player.z),false,true,false)==null;
    }
    public static boolean visible(Minecraft mc,PlayerEntity player) { return clear(mc,player,.9)||clear(mc,player,.5)||clear(mc,player,.1); }
    public static boolean partlyHidden(Minecraft mc,PlayerEntity player) { return !clear(mc,player,.9)||!clear(mc,player,.5)||!clear(mc,player,.1); }
}
