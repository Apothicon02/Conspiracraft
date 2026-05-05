package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.world.World;
import org.conspiracraft.world.trees.canopies.PalmCanopy;
import org.conspiracraft.world.trees.trunks.BendingTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.conspiracraft.world.World.*;
import static org.conspiracraft.world.trees.TreeHelper.integrateCanopy;

public class PalmTree {
    public static void generate(Random random, Vector2i blockOn, int x, int y, int z, int maxHeight, int logType, int logSubType, int leafType, int leafSubType) {
        Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generatedTrunk = BendingTrunk.generateTrunk(random, x, y, z, true, 0, maxHeight, logType, logSubType);
        boolean colliding = false;
        Map<Vector3i, Vector2i> blocks = new HashMap<>(generatedTrunk.getFirst());
        int minCollisionY = y+5;
        for (Vector3i canopyPos : generatedTrunk.getSecond()) {
            Map<Vector3i, Vector2i> canopy = PalmCanopy.generateCanopy(random, blocks, canopyPos.x, canopyPos.y, canopyPos.z, leafType, leafSubType, maxHeight, new Vector3i(x, y, z));
            if (!integrateCanopy(canopy, blocks, minCollisionY)) {
                colliding = true;
                break;
            }
        }
        if (!colliding) {
            blocks.forEach((pos, block) -> {
                if (World.inBounds(pos)) {
                    World.setBlock(pos.x, pos.y, pos.z, block.x, block.y);
                    int condensedPos = packPos(pos.x, pos.z);
                    int surfaceY = heightmap[condensedPos];
                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], pos.y - 1);
//                        for (int extraY = pos.y - 1; extraY >= surfaceY; extraY--) {
//                            setLight(pos.x, extraY, pos.z, new Vector4i(0, 0, 0, 0));
//                        }
                }
            });
        }
    }
}
