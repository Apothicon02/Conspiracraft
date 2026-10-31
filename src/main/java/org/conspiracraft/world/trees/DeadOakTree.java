package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.World;
import org.conspiracraft.world.trees.trunks.TwistingTrunk;
import org.joml.Vector2i;

import java.util.HashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.conspiracraft.world.World.*;

public class DeadOakTree {
    public static void generate(Random random, Bounds bounds, int x, int y, int z, int maxHeight, int logType, int logSubType) {
        Pair<Long2IntOpenHashMap, Set<Long>> generatedTrunk = TwistingTrunk.generateTrunk(random, x, y, z, maxHeight, 3, logType, logSubType, true, 8);
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap(20*maxHeight);
        blocks.putAll(generatedTrunk.getFirst());
        blocks.forEach((pos, block) -> {
            setBlockWorldgen(pos, block >> 16, block & 0xFFFF);
        });
    }
}
