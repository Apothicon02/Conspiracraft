package org.conspiracraft.game.world.trees;

import org.conspiracraft.game.world.WorldGen;
import org.conspiracraft.game.world.trees.canopies.PalmCanopy;
import org.conspiracraft.game.world.trees.trunks.StraightTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Set;

public class PalmTree {
    public static void generate(Vector2i blockOn, int x, int y, int z, int maxHeight, int logType, int logSubType, int leafType, int leafSubType) {
        if (blockOn.x == 23) {
            Set<Vector3i> canopies = StraightTrunk.generateTrunk(x, y, z, maxHeight, logType, logSubType);
            for (Vector3i canopyPos : canopies) {
                Map<Vector3i, Vector2i> canopy = PalmCanopy.generateCanopy(canopyPos.x, canopyPos.y, canopyPos.z, leafType, leafSubType, maxHeight, new Vector3i(x, y, z));
                boolean colliding = false;
//                for (Vector3i pos : canopy.keySet()) {
//                    if (WorldGen.getBlockWorldgen(pos.x, pos.y, pos.z).x != 0) {
//                        colliding = true;
//                    }
//                }
                if (!colliding) {
                    canopy.forEach((pos, block) -> {
                        WorldGen.setBlockWorldgen(pos.x, pos.y, pos.z, block.x, block.y);
                    });
                }
            }
        }
    }
}
