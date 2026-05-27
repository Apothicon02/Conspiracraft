package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.World;
import org.conspiracraft.world.trees.canopies.DroopingCanopy;
import org.conspiracraft.world.trees.canopies.PalmCanopy;
import org.conspiracraft.world.trees.trunks.BendingTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.conspiracraft.world.World.heightmap;
import static org.conspiracraft.world.World.packPos;
import static org.conspiracraft.world.trees.TreeHelper.integrateCanopy;

public class AcaciaTree {
    public static void generate(Random random, Vector2i blockOn, int x, int y, int z, int maxHeight, int logType, int logSubType, int leafType, int leafSubType) {
        Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generatedTrunk = BendingTrunk.generateTrunk(random, x, y, z, false, random.nextInt(4, 6), maxHeight, maxHeight, logType, logSubType);
        AtomicBoolean colliding = new AtomicBoolean(false);
        Map<Vector3i, Vector2i> blocks = new HashMap<>(generatedTrunk.getFirst());
        blocks.forEach((pos, block) -> {
            if (World.getBlock(pos).x() == BlockTypes.WATER.id) {
                colliding.set(true);
            }
        });
        if (colliding.get()) {return;}
        int minCollisionY = y+5;
        for (Vector3i canopyPos : generatedTrunk.getSecond()) {
            Map<Vector3i, Vector2i> canopy = DroopingCanopy.generateCanopy(random, blocks, canopyPos.x, canopyPos.y, canopyPos.z, leafType, leafSubType, maxHeight, new Vector3i(x, y, z), random.nextInt(4, 5), random.nextInt(7, 9), 1.f);
            if (!integrateCanopy(canopy, blocks, minCollisionY)) {
                colliding.set(true);
                break;
            }
        }
        if (!colliding.get()) {
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
