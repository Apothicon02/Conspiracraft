package org.conspiracraft.items.types;

import org.conspiracraft.blocks.BlockTag;
import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.items.Item;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.player.HandManager;
import org.conspiracraft.world.World;
import org.joml.Vector2i;

import static org.conspiracraft.Main.player;
import static org.conspiracraft.player.HandManager.lmbDown;

public class ToolItemType extends ItemType {
    public int strength;
    public BlockTag[] uses;
    public ToolItemType(String name, int strength, BlockTag[] uses) {
        super(name);
        this.strength = strength;
        this.uses = uses;
    }
    @Override
    public int use(DDAResult dda, Item item) {
        if (lmbDown && World.inBounds(player.selectedBlock)) {
            Vector2i block = World.getBlock(dda.hit.x(), dda.hit.y(), dda.hit.z());
            boolean rightTool = false;
            for (BlockTag tag : uses) {
                if (tag.tagged.contains(block.x())) {
                    rightTool = true;
                    break;
                }
            }
            HandManager.mine(rightTool ? strength : 4);
            return 1;
        } else {
            return 0;
        }
    }
}
