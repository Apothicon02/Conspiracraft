package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.LongPos;
import org.conspiracraft.world.trees.canopies.PineCanopy;
import org.conspiracraft.world.trees.trunks.StraightTrunk;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;
import java.util.Set;

public class PineTree {
    public static boolean generate(Random random, Bounds bounds, int x, int y, int z, int maxHeight, boolean snowy, int logType, int logSubType, int leafType, int leafSubType) {
        int minCollisionY = y+3;
        Pair<Long2IntOpenHashMap, Set<Long>> generatedTrunk = StraightTrunk.generateTrunk(x, y, z, maxHeight, logType, logSubType);
        if (TreeHelper.collides(generatedTrunk.getFirst(), bounds, minCollisionY, false)) {return false;}
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap(6*maxHeight*6);
        for (long canopyPos : generatedTrunk.getSecond()) {
            PineCanopy.generateCanopy(random, blocks, LongPos.x(canopyPos), LongPos.y(canopyPos), LongPos.z(canopyPos), leafType, leafSubType, maxHeight, LongPos.pack(x, y, z));
        }
        if (TreeHelper.collides(blocks, bounds, minCollisionY, false)) {return false;}
        TreeHelper.set(blocks);
        TreeHelper.set(generatedTrunk.getFirst());
        return true;
    }
}
