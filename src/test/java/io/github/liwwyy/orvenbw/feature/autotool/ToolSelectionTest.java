package io.github.liwwyy.orvenbw.feature.autotool;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolSelectionTest {
    private static class MiningBlock extends Block { MiningBlock() { super(Material.STONE); } }
    private static ItemStack tool(float speed, boolean harvests) {
        return new ItemStack(new Item() {
            @Override public float getMiningSpeed(ItemStack stack, Block block) { return speed; }
            @Override public boolean canMineBlock(Block block) { return harvests; }
        });
    }
    @Test void onlyHotbarToolsAreConsideredAndEqualToolsKeepTheSelectedSlot() {
        Block block = new MiningBlock();
        ItemStack[] slots = new ItemStack[36];
        slots[1] = tool(6, true); slots[4] = tool(6, true); slots[10] = tool(20, true);
        assertEquals(4, AutoToolFeature.bestSlot(slots, block, 4));
        assertEquals(1, AutoToolFeature.bestSlot(slots, block, 0));
        assertEquals(-1, AutoToolFeature.bestSlot(new ItemStack[]{tool(1, false)}, block, 0));
    }
    @Test void efficiencyAndHarvestPenaltyAffectWhichToolIsChosen() {
        Block block = new MiningBlock();
        ItemStack[] slots = {tool(8, false), tool(6, true)};
        assertEquals(1, AutoToolFeature.bestSlot(slots, block, 0));
        slots[0] = tool(4, true);
        slots[0].addEnchantment(Enchantment.EFFICIENCY, 3);
        assertEquals(0, AutoToolFeature.bestSlot(slots, block, 1));
    }
}
