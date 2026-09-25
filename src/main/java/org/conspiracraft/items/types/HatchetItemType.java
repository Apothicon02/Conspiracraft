package org.conspiracraft.items.types;

import org.conspiracraft.blocks.BlockTag;
import org.conspiracraft.blocks.Recipes;
import org.conspiracraft.items.DurableItem;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemUseResult;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.player.HandManager;
import org.conspiracraft.world.World;
import org.joml.Vector2i;

import static org.conspiracraft.player.HandManager.lmbDown;
import static org.conspiracraft.player.HandManager.rmbDown;

public class HatchetItemType extends ToolItemType {
    public HatchetItemType(String name, int strength, int maxDurability, BlockTag[] uses) {
        super(name, strength, maxDurability, uses);
    }
    @Override
    public ItemUseResult use(DDAResult dda, Item item) {
        if (lmbDown && item instanceof DurableItem durableItem) {
            Vector2i block = World.getBlock(dda.hit.x(), dda.hit.y(), dda.hit.z());
            boolean rightTool = false;
            for (BlockTag tag : uses) {
                if (tag.tagged.contains(block.x())) {
                    rightTool = true;
                    break;
                }
            }
            int delay = HandManager.mine(rightTool ? strength : 24);
            if (rightTool && delay > 1) {item = durableItem.damage(1);}
            return new ItemUseResult(delay, item, 1.f);
        } else if (rmbDown && item instanceof DurableItem durableItem) {
            Vector2i block = World.getBlock(dda.hit.x(), dda.hit.y(), dda.hit.z());
            Integer result = Recipes.strippingRecipes.get(block.x());
            if (result != null) {
                World.setBlock(dda.hit.x(), dda.hit.y(), dda.hit.z(), result, block.y());
                item = durableItem.damage(1);
                return new ItemUseResult(1, item, 1.f);
            } else {
                return new ItemUseResult(1, item, 1.f);
            }
        } else {
            return new ItemUseResult(0, item, 0.f);
        }
    }
}
