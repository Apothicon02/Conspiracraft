package org.conspiracraft.items.types;

import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.player.HandManager;
import org.conspiracraft.world.World;
import org.joml.Vector2i;

import static org.conspiracraft.Main.player;
import static org.conspiracraft.player.HandManager.lmbDown;

public class SpadeItemType extends ItemType {
    public SpadeItemType(String name) {
        super(name);
    }
    @Override
    public int use(DDAResult dda) {
        if (lmbDown && World.inBounds(player.selectedBlock)) {
            Vector2i block = World.getBlock(dda.hit.x(), dda.hit.y(), dda.hit.z());
            HandManager.mine(BlockTags.spadeEfficient.tagged.contains(block.x()) ? 40 : 4);
            return 1;
        } else {
            return 0;
        }
    }
}
