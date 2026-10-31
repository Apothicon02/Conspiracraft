package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.LongPos;
import org.conspiracraft.world.trees.canopies.BlobDrippyCanopy;
import org.conspiracraft.world.trees.trunks.ArchingTrunk;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;
import java.util.Set;

public class CherryTree {
    public static boolean generate(Random random, Bounds bounds, int x, int y, int z, int maxHeight, int radius, int logType, int logSubType, int leafType, int leafSubType, int count, int minBranchHeight, float bendMul) {
        return generate(random, bounds, x, y, z, maxHeight, radius, logType, logSubType, leafType, leafSubType, count, minBranchHeight, bendMul, false);
    }
    public static boolean generate(Random random, Bounds bounds, int x, int y, int z, int maxHeight, int radius, int logType, int logSubType, int leafType, int leafSubType, int count, int minBranchHeight, float bendMul, boolean ignoreCollision) {
        int minCollisionY = y+5;
        Pair<Long2IntOpenHashMap, Set<Long>> generatedTrunk = ArchingTrunk.generateTrunk(random, x, y, z, count, maxHeight-5, maxHeight, logType, logSubType, radius/2, minBranchHeight, bendMul);
        if (TreeHelper.collides(generatedTrunk.getFirst(), bounds, minCollisionY, false)) {return false;}
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap(radius*maxHeight*radius);
        for (long canopyPos : generatedTrunk.getSecond()) {
            BlobDrippyCanopy.generateCanopy(random, blocks, LongPos.x(canopyPos), LongPos.y(canopyPos), LongPos.z(canopyPos), leafType, leafSubType, radius, LongPos.y(canopyPos) - y);
        }
        if (TreeHelper.collides(blocks, bounds, minCollisionY, false)) {return false;}
        TreeHelper.set(blocks);
        TreeHelper.set(generatedTrunk.getFirst());
        return true;
    }
}
