package org.conspiracraft.world.trees;

import org.conspiracraft.blocks.types.BlockTypes;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;

import static org.conspiracraft.world.World.getBlock;
import static org.conspiracraft.world.World.inBounds;

public class TreeHelper {
    public static boolean integrateCanopy(Map<Vector3i, Vector2i> canopy, Map<Vector3i, Vector2i> blocks, int minCollisionY) {
        for (Vector3i pos : canopy.keySet()) {
            Vector2i existingBlock = getBlock(pos.x, pos.y, pos.z);
            if (!blocks.containsKey(pos) && inBounds(pos)) {
                if (BlockTypes.blockTypeMap.get(existingBlock.x()).blockProperties.isFluidReplaceable) {
                    blocks.put(pos, canopy.get(pos));
                }
                if (pos.y > minCollisionY && existingBlock.x() != 0) {
                   return false;
                }
            }
        }
        return true;
    }
}
