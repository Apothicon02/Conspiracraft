package org.conspiracraft.items.types;

import org.conspiracraft.items.HeartItem;
import org.conspiracraft.items.Item;

public class HeartItemType extends DurableItemType {
    public HeartItemType(String name, int maxDurability) {
        super(name, maxDurability);
    }
    @Override
    public Item createItem() {
        return new HeartItem().durability(maxDurability).type(this);
    }
}
