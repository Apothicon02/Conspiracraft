package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.LongPos;
import org.conspiracraft.world.trees.canopies.DroopingCanopy;
import org.conspiracraft.world.trees.trunks.BendingTrunk;
import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;
import java.util.Set;

public class AcaciaTree {
    public static boolean generate(Random random, Bounds bounds, int x, int y, int z, int maxHeight, int logType, int logSubType, int leafType, int leafSubType) {
        int minCollisionY = y+5;
        Pair<Long2IntOpenHashMap, Set<Long>> generatedTrunk = BendingTrunk.generateTrunk(random, x, y, z, false, random.nextInt(4, 6), maxHeight, maxHeight, logType, logSubType);
        if (TreeHelper.collides(generatedTrunk.getFirst(), bounds, minCollisionY, false)) {return false;}
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap(20*maxHeight);
        for (long canopyPos : generatedTrunk.getSecond()) {
            DroopingCanopy.generateCanopy(random, blocks, LongPos.x(canopyPos), LongPos.y(canopyPos), LongPos.z(canopyPos), leafType, leafSubType, maxHeight, new Vector3i(x, y, z), random.nextInt(4, 5), random.nextInt(7, 9), 1.f);
        }
        if (TreeHelper.collides(blocks, bounds, minCollisionY, false)) {return false;}
        TreeHelper.set(blocks);
        TreeHelper.set(generatedTrunk.getFirst());
        return true;
    }
}
