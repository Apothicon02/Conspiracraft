package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.LongPos;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.conspiracraft.world.trees.canopies.SpruceCanopy;
import org.conspiracraft.world.trees.trunks.StraightTrunk;

import java.util.Random;
import java.util.Set;

public class BirchTree {
    public static boolean generate(Random random, Bounds bounds, int x, int y, int z, int maxHeight, int radius, int logType, int logSubType, int leafType, int leafSubType) {
        return generate(random, bounds, x, y, z, maxHeight, radius, logType, logSubType, leafType, leafSubType, false);
    }
    public static boolean generate(Random random, Bounds bounds, int x, int y, int z, int maxHeight, int radius, int logType, int logSubType, int leafType, int leafSubType, boolean ignoreCollision) {
        int minCollisionY = y+5;
        Pair<Long2IntOpenHashMap, Set<Long>> generatedTrunk = StraightTrunk.generateTrunk(x, y, z, maxHeight, logType, logSubType);
        generatedTrunk.getFirst().trim();
        if (TreeHelper.collides(generatedTrunk.getFirst(), bounds, minCollisionY, ignoreCollision)) {return false;}
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap(6*maxHeight*6);
        for (long canopyPos : generatedTrunk.getSecond()) {
            SpruceCanopy.generateCanopy(random, blocks, LongPos.x(canopyPos), LongPos.y(canopyPos)+1+(maxHeight/2), LongPos.z(canopyPos), leafType, leafSubType, maxHeight/2);
        }
        blocks.trim();
        if (TreeHelper.collides(blocks, bounds, minCollisionY, ignoreCollision)) {return false;}
        TreeHelper.set(blocks);
        TreeHelper.set(generatedTrunk.getFirst());
        return true;
    }
}
