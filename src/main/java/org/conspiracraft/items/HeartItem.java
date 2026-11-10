package org.conspiracraft.items;

import org.conspiracraft.items.types.HeartItemType;
import org.conspiracraft.items.types.ItemTypes;

public class HeartItem extends DurableItem implements Cloneable {
    @Override
    public HeartItemType type() {return (HeartItemType) type;}
    @Override
    public HeartItem durability(int durability) {
        this.durability = durability;
        if (this.durability <= 0) {
            this.type(ItemTypes.HEART_BROKEN);
        } else if (this.durability <= 50) {
            this.type(ItemTypes.HEART_HALF);
        } else {
            this.type(ItemTypes.HEART);
        }
        return this;
    }
}
