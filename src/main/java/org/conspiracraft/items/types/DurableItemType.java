package org.conspiracraft.items.types;

import org.conspiracraft.items.DurableItem;
import org.conspiracraft.items.Item;

public class DurableItemType extends ItemType {
    public boolean hideDurability = false;
    public int maxDurability;
    public DurableItemType(String name, int maxDurability) {
        super(name);
        this.maxDurability = maxDurability;
    }
    public DurableItemType hideDurability() {hideDurability = true; return this;}
    @Override
    public Item createItem() {
        return new DurableItem().durability(maxDurability).type(this);
    }
    @Override
    public int maxDurability() {return maxDurability;}
}
