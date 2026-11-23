package org.conspiracraft.items.types;

import org.conspiracraft.audio.*;
import org.conspiracraft.items.FoodItem;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemUseResult;
import org.conspiracraft.physics.DDAResult;
import org.joml.Vector3f;

import static org.conspiracraft.Main.player;

public class FoodItemType extends ItemType {
    public static final SFX[] biteSFXs = new SFX[]{Sounds.BITE, Sounds.BITE2, Sounds.BITE3};
    public final int bitesMax;
    public FoodItemType(String name, int bitesMax) {
        super(name);
        this.bitesMax = bitesMax;
    }
    @Override
    public Item createItem() {
        return new FoodItem().bitesLeft(bitesMax).type(this);
    }
    @Override
    public ItemUseResult use(DDAResult dda, Item item) {
        if (player.inputHandler.leftButtonPressed && item instanceof FoodItem foodItem) {
            int prevAmount = foodItem.amount;
            foodItem = foodItem.bite(1);
            if (foodItem == null || foodItem.amount < prevAmount) {
                player.heal(200);
                Source source = new Source(new Vector3f(player.pos), 1.f, 1.f, 0.f, 0);
                source.play(Sounds.BURP, true);
                AudioController.disposableSources.add(source);
            } else {
                Source source = new Source(new Vector3f(player.pos), 1.f, 1.f, 0.f, 0);
                source.play(biteSFXs[(int)(Math.random()*biteSFXs.length)], true);
                AudioController.disposableSources.add(source);
            }
            return new ItemUseResult((int) (400+(Math.random()*100)), foodItem, 0.f);
        } else {
            return new ItemUseResult(0, item, 0.f);
        }
    }
    public int bitesMax() {
        return bitesMax;
    }
}
