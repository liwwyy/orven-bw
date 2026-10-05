package io.github.liwwyy.orvenbw.mixin;

import io.github.liwwyy.orvenbw.OrvenBw;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerInventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class AutoToolInputMixin {
    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/living/player/PlayerInventory;selectedSlot:I", opcode = Opcodes.PUTFIELD))
    private void orven$hotbarKey(PlayerInventory inventory, int slot) {
        var mod = OrvenBw.instance();
        if (mod == null || !mod.autoTool().requestSlot(slot)) inventory.selectedSlot = slot;
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/living/player/PlayerInventory;scrollInHotbar(I)V"))
    private void orven$hotbarScroll(PlayerInventory inventory, int amount) {
        var mod = OrvenBw.instance();
        int slot = Math.floorMod(inventory.selectedSlot - Integer.signum(amount), 9);
        if (mod == null || !mod.autoTool().requestSlot(slot)) inventory.scrollInHotbar(amount);
    }
}
