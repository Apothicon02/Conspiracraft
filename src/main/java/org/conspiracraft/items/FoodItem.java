package org.conspiracraft.items;

import org.conspiracraft.items.types.FoodItemType;

public class FoodItem extends Item implements Cloneable {
    public int bitesLeft = 1;

    @Override
    public FoodItemType type() {return (FoodItemType) type;}

    public FoodItem bitesLeft(int bitesLeft) {
        this.bitesLeft = bitesLeft;
        if (this.bitesLeft <= 0) {
            amount--;
            this.bitesLeft = ((FoodItemType)this.type).bitesMax();
        }
        return amount <= 0 ? null : this;
    }
    public FoodItem bite(int bites) {
        return bitesLeft(Math.clamp(this.bitesLeft-bites, 0, ((FoodItemType)this.type).bitesMax()));
    }
}
