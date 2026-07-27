package org.conspiracraft.items.types;

import org.conspiracraft.blocks.BlockTag;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.items.DurableItem;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.ItemUseResult;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.world.World;
import org.joml.Vector2i;

import static org.conspiracraft.Main.player;
import static org.conspiracraft.player.HandManager.lmbDown;

public class ChiselItemType extends ToolItemType {
    public ChiselItemType(String name, int strength, int maxDurability, BlockTag[] uses) {
        super(name, strength, maxDurability, uses);
    }
    @Override
    public ItemUseResult use(DDAResult dda, Item item) {
        if (lmbDown && World.inBounds(player.selectedBlock) && item instanceof DurableItem durableItem) {
            Vector2i block = World.getBlock(dda.hit.x(), dda.hit.y(), dda.hit.z());
            int delay = 0;
            //if (BlockTypes.blockTypes[block.x()].blockProperties.hasSlab) {
                delay = 400;
                World.setBlock(dda.hit.x(), dda.hit.y(), dda.hit.z(), block.x(), block.y()+1, true); //block.y() >= 2 ? 0 :
                item = durableItem.damage(1);
            //}
            return new ItemUseResult(delay, item);
        } else {
            return new ItemUseResult(0, item);
        }
    }
}
