package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.LongPos;
import org.conspiracraft.world.Region;
import org.conspiracraft.world.trees.canopies.SquareCanopy;
import org.conspiracraft.world.trees.trunks.ThickTrunk;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;
import java.util.Set;

import static org.conspiracraft.world.World.*;
import static org.conspiracraft.world.World.regionBits;

public class RedwoodTree {
    public static boolean generate(Random random, Bounds bounds, int x, int y, int z, int maxHeight, int radius, int leavesHeight, int logType, int logSubType, int leafType, int leafSubType, int minBranchHeight, int branchChance) {
        Region region = getRegion(packRegionPos(x>>regionBits, y>>regionBits, z>>regionBits));
        int minCollisionY = y+5;
        Pair<Long2IntOpenHashMap, Set<Long>> generatedTrunk = ThickTrunk.generateTrunk(random, x, y, z, maxHeight, false, minBranchHeight, branchChance, logType, logSubType);
        generatedTrunk.getFirst().trim();
        if (TreeHelper.collides(region, generatedTrunk.getFirst(), bounds, minCollisionY, false)) {return false;}
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap(radius*maxHeight*radius);
        for (long canopyPos : generatedTrunk.getSecond()) {
            SquareCanopy.generateCanopy(random, blocks, region, LongPos.x(canopyPos), LongPos.y(canopyPos), LongPos.z(canopyPos), leafType, leafSubType, radius, leavesHeight);
        }
blocks.trim();
        if (TreeHelper.collides(region, blocks, bounds, minCollisionY, false)) {return false;}
        TreeHelper.set(region, blocks);
        TreeHelper.set(region, generatedTrunk.getFirst());
        return false;
    }
}
