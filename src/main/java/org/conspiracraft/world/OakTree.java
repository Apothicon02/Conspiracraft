package org.conspiracraft.world;

import kotlin.Pair;
import org.conspiracraft.world.trees.canopies.BlobCanopy;
import org.conspiracraft.world.trees.trunks.TwistingTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.conspiracraft.world.World.*;

public class OakTree {
    public static boolean generate(Random random, Vector2i blockOn, int x, int y, int z, int maxHeight, int radius, int leavesHeight, int logType, int logSubType, int leafType, int leafSubType) {
        if (blockOn.x == 2) {
            Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generatedTrunk = TwistingTrunk.generateTrunk(random, x, y, z, maxHeight, 3, logType, logSubType, true, 8);
            boolean colliding = false;
            Map<Vector3i, Vector2i> blocks = new HashMap<>(generatedTrunk.getFirst());
            int minCollisionY = y+5;
            outerLoop:
            for (Vector3i canopyPos : generatedTrunk.getSecond()) {
                Map<Vector3i, Vector2i> canopy = BlobCanopy.generateCanopy(random, blocks, canopyPos.x, canopyPos.y, canopyPos.z, leafType, leafSubType, radius, leavesHeight);
                for (Vector3i pos : canopy.keySet()) {
                    if (pos.y > minCollisionY && inBounds(pos) && !blocks.containsKey(pos) && World.getBlock(pos.x, pos.y, pos.z).x != 0) {
                        colliding = true;
                        break outerLoop;
                    }
                }
                blocks.putAll(canopy);
            }
            if (!colliding) {
                blocks.forEach((pos, block) -> {
                    if (inBounds(pos)) {
                        World.setBlock(pos.x, pos.y, pos.z, block.x, block.y);
                        int condensedPos = packPos(pos.x, pos.z);
                        int surfaceY = heightmap[condensedPos];
                        heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], pos.y - 1);
                        for (int extraY = pos.y - 1; extraY >= surfaceY; extraY--) {
                            //setLight(pos.x, extraY, pos.z, new Vector4i(0, 0, 0, 0));
                        }
                    }
                });
            }
            return !colliding;
        }
        return false;
    }
}
