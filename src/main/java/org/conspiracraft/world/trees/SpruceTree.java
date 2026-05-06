package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.World;
import org.conspiracraft.world.trees.canopies.SpruceCanopy;
import org.conspiracraft.world.trees.trunks.StraightTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.conspiracraft.world.World.*;
import static org.conspiracraft.world.trees.TreeHelper.integrateCanopy;

public class SpruceTree {
    public static void generate(Random random, Vector2i blockOn, int x, int y, int z, int maxHeight, boolean snowy, int logType, int logSubType, int leafType, int leafSubType) {
        if (blockOn.x == 2 || blockOn.x == 54) {
            Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generatedTrunk = StraightTrunk.generateTrunk(x, y, z, maxHeight, logType, logSubType);
            boolean colliding = false;
            Map<Vector3i, Vector2i> blocks = new HashMap<>(generatedTrunk.getFirst());
            int minCollisionY = y+3;
            for (Vector3i canopyPos : generatedTrunk.getSecond()) {
                Map<Vector3i, Vector2i> canopy = SpruceCanopy.generateCanopy(random, blocks, canopyPos.x, canopyPos.y, canopyPos.z, leafType, leafSubType, maxHeight, new Vector3i(x, y, z));
                if (!integrateCanopy(canopy, blocks, minCollisionY)) {
                    colliding = true;
                    break;
                }
            }
            if (!colliding) {
                blocks.forEach((pos, block) -> {
                    if (inBounds(pos)) {
                        int subtype = block.y();
                        if (snowy && block.x() == BlockTypes.SPRUCE_LEAVES.id) {
                            AtomicBoolean airAbove = new AtomicBoolean(true);
                            blocks.forEach((nPos, nBlock) -> {
                                if (nPos.x() == pos.x() && nPos.z() == pos.z() && nPos.y == pos.y()+1) {
                                    airAbove.set(false);
                                }
                            });
                            if (airAbove.get()) {
                                subtype+=8;
                            }
                        }
                        World.setBlock(pos.x, pos.y, pos.z, block.x, subtype);
                        int condensedPos = packPos(pos.x, pos.z);
                        int surfaceY = heightmap[condensedPos];
                        heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], pos.y - 1);
                        for (int extraY = pos.y - 1; extraY >= surfaceY; extraY--) {
                            //setLight(pos.x, extraY, pos.z, new Vector4i(0, 0, 0, 0));
                        }
                    }
                });
            }
        }
    }
}
