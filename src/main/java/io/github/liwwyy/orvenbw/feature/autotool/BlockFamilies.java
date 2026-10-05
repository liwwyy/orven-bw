package io.github.liwwyy.orvenbw.feature.autotool;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.resource.Identifier;

/** Match native block-item IDs while grouping legacy color metadata and wooden building blocks. */
public final class BlockFamilies {
    private BlockFamilies() {}
    public static boolean matches(Block block, String[] ids) {
        if (block == null || ids == null) return false;
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            try {
                Item item = Item.REGISTRY.get(new Identifier(id));
                if (item instanceof BlockItem selected && sameFamily(block, selected.getBlock())) return true;
            } catch (IllegalArgumentException ignored) { }
        }
        return false;
    }
    static boolean sameFamily(Block block, Block selected) {
        if (block == selected) return true;
        String a = family(block), b = family(selected);
        return a != null && a.equals(b);
    }
    private static String family(Block block) {
        Identifier key = Block.REGISTRY.getKey(block);
        String id = key == null ? "" : key.toString();
        if (id.equals("minecraft:glass") || id.equals("minecraft:stained_glass")) return "glass";
        if (id.equals("minecraft:hardened_clay") || id.equals("minecraft:stained_hardened_clay")) return "clay";
        return !id.equals("minecraft:ladder") && !id.equals("minecraft:chest")
                && !id.equals("minecraft:trapped_chest") && !id.equals("minecraft:ender_chest")
                && block.getMaterial() == Material.WOOD ? "wood" : null;
    }
}
