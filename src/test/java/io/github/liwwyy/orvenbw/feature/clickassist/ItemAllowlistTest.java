package io.github.liwwyy.orvenbw.feature.clickassist;

import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.resource.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemAllowlistTest {
    @Test void nativeRegistryIdsAcceptRawBeefCustomItemsAndTheWholeSwordClass() {
        // Register a small fixture without bootstrapping game recipes in the named unit-test classpath.
        Item beef = new Item(), diamond = new Item(), stick = new Item();
        Item ironSword = new SwordItem(Item.Tier.IRON), otherSword = new SwordItem(Item.Tier.DIAMOND);
        Item.REGISTRY.register(20000, new Identifier("minecraft:beef"), beef);
        Item.REGISTRY.register(20001, new Identifier("minecraft:iron_sword"), ironSword);
        Item.REGISTRY.register(20002, new Identifier("test:diamond"), diamond);
        String[] ids = {"minecraft:beef", "minecraft:iron_sword", "test:diamond"};
        assertTrue(ItemAllowlist.allows(beef, ids));
        assertTrue(ItemAllowlist.allows(ironSword, ids));
        assertTrue(ItemAllowlist.allows(otherSword, ids));
        assertTrue(ItemAllowlist.allows(diamond, ids));
        assertFalse(ItemAllowlist.allows(stick, ids));
        assertFalse(ItemAllowlist.allows(beef, new String[]{"missing:item"}));
        assertFalse(ItemAllowlist.allows(null, ids));
        assertFalse(ItemAllowlist.allows(beef, new String[0]));
        assertTrue(ItemAllowlist.allowsHand(null, ids, true, true));
        assertFalse(ItemAllowlist.allowsHand(null, ids, false, false));
        assertTrue(ItemAllowlist.allowsHand(stick, ids, false, false));
        assertFalse(ItemAllowlist.allowsHand(stick, ids, true, true));
    }
}
