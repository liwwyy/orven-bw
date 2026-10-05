package io.github.liwwyy.orvenbw.feature.autotool;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.resource.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlockFamiliesTest {
    private static class TestBlock extends Block { TestBlock(Material material) { super(material); } }
    private static Block register(int index, String name, Material material) {
        Block block = new TestBlock(material);
        Block.REGISTRY.register(index, new Identifier(name), block);
        Item.REGISTRY.register(index, new Identifier(name), new BlockItem(block));
        return block;
    }
    @Test void colorFamiliesAndWoodGroupTogetherWithoutExpandingLaddersOrStone() {
        Block glass = register(22000, "minecraft:glass", Material.GLASS);
        Block stained = register(22001, "minecraft:stained_glass", Material.GLASS);
        Block clay = register(22002, "minecraft:hardened_clay", Material.STONE);
        Block stainedClay = register(22003, "minecraft:stained_hardened_clay", Material.STONE);
        Block plank = register(22004, "minecraft:planks", Material.WOOD);
        Block log = register(22005, "minecraft:log2", Material.WOOD);
        Block ladder = register(22006, "minecraft:ladder", Material.WOOD);
        Block stone = register(22007, "test:stone", Material.STONE);
        Block chest = register(22008, "minecraft:chest", Material.WOOD);
        Block trapped = register(22009, "minecraft:trapped_chest", Material.WOOD);
        Block ender = register(22010, "minecraft:ender_chest", Material.STONE);
        assertTrue(BlockFamilies.matches(stained, new String[]{"minecraft:glass"}));
        assertTrue(BlockFamilies.matches(glass, new String[]{"minecraft:stained_glass"}));
        assertTrue(BlockFamilies.matches(stainedClay, new String[]{"minecraft:hardened_clay"}));
        assertTrue(BlockFamilies.matches(clay, new String[]{"minecraft:stained_hardened_clay"}));
        assertTrue(BlockFamilies.matches(log, new String[]{"minecraft:planks"}));
        assertTrue(BlockFamilies.matches(plank, new String[]{"minecraft:log2"}));
        assertFalse(BlockFamilies.matches(ladder, new String[]{"minecraft:planks"}));
        assertFalse(BlockFamilies.matches(stone, new String[]{"minecraft:planks"}));
        assertTrue(BlockFamilies.matches(ladder, new String[]{"minecraft:ladder"}));
        assertFalse(BlockFamilies.matches(stone, new String[]{"invalid::id", "missing:block"}));
        for (Block container : new Block[]{chest, trapped, ender}) assertFalse(BlockFamilies.matches(container, new String[]{"minecraft:planks"}));
    }
}
