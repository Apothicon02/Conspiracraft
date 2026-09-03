package org.conspiracraft.world.trees;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.Bounds;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;

import static org.conspiracraft.world.World.getBlockWorldgen;

public class TreeHelper {
    public static boolean integrateCanopy(Bounds bounds, Map<Vector3i, Vector2i> canopy, Map<Vector3i, Vector2i> blocks, int minCollisionY) {
        for (Vector3i pos : canopy.keySet()) {
            if (bounds.out(pos)) {return false;}
            Vector2i existingBlock = getBlockWorldgen(pos.x, pos.y, pos.z);
            if (!blocks.containsKey(pos)) {
                if (BlockTypes.blockTypes[existingBlock.x()].blockProperties.isFluidReplaceable) {
                    blocks.put(pos, canopy.get(pos));
                }
                if (pos.y > minCollisionY && existingBlock.x() != 0) {
                   return false;
                }
            }
        }
        return true;
    }
    public static boolean oldIntegrateCanopy(Map<Vector3i, Vector2i> canopy, Map<Vector3i, Vector2i> blocks, int minCollisionY) {
        for (Vector3i pos : canopy.keySet()) {
            Vector2i existingBlock = getBlockWorldgen(pos.x, pos.y, pos.z);
            if (!blocks.containsKey(pos)) {
                if (BlockTypes.blockTypes[existingBlock.x()].blockProperties.isFluidReplaceable) {
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
