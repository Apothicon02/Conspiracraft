package org.conspiracraft.game.world.trees;

import kotlin.Pair;
import org.conspiracraft.game.world.WorldGen;
import org.conspiracraft.game.world.trees.canopies.PalmCanopy;
import org.conspiracraft.game.world.trees.trunks.BendingTrunk;
import org.conspiracraft.game.world.trees.trunks.StraightTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class PalmTree {
    public static void generate(Vector2i blockOn, int x, int y, int z, int maxHeight, int logType, int logSubType, int leafType, int leafSubType) {
        if (blockOn.x == 23) {
            Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generatedTrunk = BendingTrunk.generateTrunk(new Random(), x, y, z, true, 0, maxHeight, logType, logSubType);
            boolean colliding = false;
            Map<Vector3i, Vector2i> blocks = new HashMap<>(generatedTrunk.getFirst());
            blocks.putAll(generatedTrunk.getFirst());
            outerLoop:
            for (Vector3i canopyPos : generatedTrunk.getSecond()) {
                Map<Vector3i, Vector2i> canopy = PalmCanopy.generateCanopy(canopyPos.x, canopyPos.y, canopyPos.z, leafType, leafSubType, maxHeight, new Vector3i(x, y, z));
                for (Vector3i pos : canopy.keySet()) {
                    if (WorldGen.getBlockWorldgen(pos.x, pos.y, pos.z).x != 0) {
                        colliding = true;
                        break outerLoop;
                    }
                }
                blocks.putAll(canopy);
            }
            if (!colliding) {
                blocks.forEach((pos, block) -> {
                    WorldGen.setBlockWorldgen(pos.x, pos.y, pos.z, block.x, block.y);
                });
            }
        }
    }
}
