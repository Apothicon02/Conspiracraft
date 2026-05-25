package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.World;
import org.conspiracraft.world.trees.trunks.TwistingTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.conspiracraft.world.World.*;

public class DeadOakTree {
    public static void generate(Random random, Vector2i blockOn, int x, int y, int z, int maxHeight, int logType, int logSubType) {
        if (blockOn.x == 2 || blockOn.x == 23) {
            Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generatedTrunk = TwistingTrunk.generateTrunk(random, x, y, z, maxHeight, 3, logType, logSubType, true, 8);
            Map<Vector3i, Vector2i> blocks = new HashMap<>(generatedTrunk.getFirst());
            AtomicBoolean colliding = new AtomicBoolean(false);
            blocks.forEach((pos, block) -> {
                if (World.getBlock(pos).x() == BlockTypes.WATER.id) {
                    colliding.set(true);
                }
            });
            if (colliding.get()) {return;}
            blocks.forEach((pos, block) -> {
                if (inBounds(pos)) {
                    setBlock(pos.x, pos.y, pos.z, block.x, block.y);
                    int condensedPos = packPos(pos.x, pos.z);
                    int surfaceY = heightmap[condensedPos];
                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], pos.y - 1);
//                    for (int extraY = pos.y - 1; extraY >= surfaceY; extraY--) {
//                        setLightWorldgen(pos.x, extraY, pos.z, new Vector4i(0, 0, 0, 0));
//                    }
                }
            });
        }
    }
}
