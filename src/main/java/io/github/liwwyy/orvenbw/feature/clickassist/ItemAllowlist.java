package io.github.liwwyy.orvenbw.feature.clickassist;

import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.resource.Identifier;

/** IDs stay native OneConfig item-list values; selecting one sword opts in every sword. */
public final class ItemAllowlist {
    private ItemAllowlist() {}
    public static boolean allows(Item held, String[] ids) {
        if (held == null || ids == null) return false;
        boolean sword = held instanceof SwordItem;
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            try {
                Item selected = Item.REGISTRY.get(new Identifier(id));
                if (selected == held || sword && selected instanceof SwordItem) return true;
            } catch (IllegalArgumentException ignored) { /* A stale/custom ID cannot enable arbitrary items. */ }
        }
        return false;
    }
}
