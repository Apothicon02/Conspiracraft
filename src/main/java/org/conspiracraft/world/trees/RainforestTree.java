package org.conspiracraft.world.trees;

import kotlin.Pair;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.LongPos;
import org.conspiracraft.world.trees.canopies.DroopingCanopy;
import org.conspiracraft.world.trees.trunks.ThickTrunk;
import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;
import java.util.Set;

public class RainforestTree {
    public static boolean generate(Random random, Bounds bounds, int x, int y, int z, int maxHeight, int radius, int leavesHeight, int logType, int logSubType, int leafType, int leafSubType, int branchChance) {
        int minCollisionY = y+5;
        Pair<Long2IntOpenHashMap, Set<Long>> generatedTrunk = ThickTrunk.generateTrunk(random, x, y, z, maxHeight, false, 3, branchChance, logType, logSubType);
        generatedTrunk.getFirst().trim();
        if (TreeHelper.collides(generatedTrunk.getFirst(), bounds, minCollisionY, false)) {return false;}
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap(radius*maxHeight*radius);
        for (long canopyPos : generatedTrunk.getSecond()) {
            int eLeavesHeight = leavesHeight;
            if (LongPos.y(canopyPos) < y+maxHeight) {
                eLeavesHeight /= 2;
            }
            DroopingCanopy.generateCanopy(random, blocks, LongPos.x(canopyPos), LongPos.y(canopyPos), LongPos.z(canopyPos), leafType, leafSubType, LongPos.y(canopyPos)-y, new Vector3i(LongPos.x(canopyPos), LongPos.y(canopyPos), LongPos.z(canopyPos)), eLeavesHeight, radius, 1.f);
        }
        blocks.trim();
        if (TreeHelper.collides(blocks, bounds, minCollisionY, false)) {return false;}
        TreeHelper.set(blocks);
        TreeHelper.set(generatedTrunk.getFirst());
        return true;
    }
}
