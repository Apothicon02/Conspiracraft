package org.conspiracraft.world.trees.trunks;

import kotlin.Pair;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;
import java.util.Set;

public class TwistingTrunk extends Trunk {
    public static Pair<Long2IntOpenHashMap, Set<Long>> generateTrunk(Random random, int oX, int oY, int oZ, int trunkHeight, int minCanopyHeight, int blockType, int blockSubType, boolean overgrown, int minBranchHeight) {
        Long2IntOpenHashMap map = new Long2IntOpenHashMap(trunkHeight*8);
        Set<Long> canopies = new HashSet<>();

        int minBranch = minBranchHeight+oY;
        int prevXPositive = 0;
        int prevZPositive = 0;
        int twistable = 0;
        int maxHeight = trunkHeight+oY;
        Vector3i pos = new Vector3i(oX, oY, oZ);
        for (int height = oY-1; height <= maxHeight; height++) {
            twistable--;
            boolean branch = false;
            Vector3i dir = new Vector3i(0, 0, 0);
            if (height > oY+2 && twistable <= 0 && random.nextFloat()*10 < 4) {
                int xOff = (int) ((random.nextFloat()*20)-10);
                int zOff = (int) ((random.nextFloat()*20)-10);
                boolean xPositive = xOff >= prevXPositive;
                boolean zPositive = zOff >= prevZPositive;
                if (xPositive) {
                    prevXPositive = 5;
                    dir.x += 1;
                } else {
                    prevXPositive = -5;
                    dir.x -= 1;
                }
                if (zPositive) {
                    prevZPositive = 5;
                    dir.z += 1;
                } else {
                    prevZPositive = -5;
                    dir.z -= 1;
                }
                branch = height >= oY+minCanopyHeight;
                twistable = 2;
            }
            pos.add(dir);
            pos.y = height;
            makeSquare(map, new Vector3i(pos.x, pos.y-1, pos.z), blockType, blockSubType);
            makeSquare(map, new Vector3i(pos), blockType, blockSubType);
            if (branch && pos.y >= minBranch) {
                canopies.add(makeBranch(map, pos, dir, blockType, blockSubType));
                if (overgrown) {
                    canopies.add(makeBranch(map, pos, new Vector3i(dir.x * (random.nextFloat() >= 0.5f ? 1 : 0), +2, dir.z * (random.nextFloat() >= 0.5f ? 1 : 0)), blockType, blockSubType));
                }
            }
            if (pos.y == maxHeight) {
                canopies.add(LongPos.pack(pos.x, pos.y+1, pos.z));
                if (overgrown) {
                    canopies.add(LongPos.pack(pos.x, pos.y-1, pos.z+3));
                    canopies.add(LongPos.pack(pos.x+3, pos.y, pos.z));
                    canopies.add(LongPos.pack(pos.x, pos.y-1, pos.z-3));
                    canopies.add(LongPos.pack(pos.x-3, pos.y, pos.z));
                }
            }
        }

        return new Pair<>(map, canopies);
    }

    private static void makeSquare(Long2IntOpenHashMap map, Vector3i pos, int blockType, int blockSubType) {
        map.put(LongPos.pack(pos.x, pos.y, pos.z), Chunk.packInts(blockType, blockSubType));
        map.put(LongPos.pack(pos.x, pos.y, pos.z+1), Chunk.packInts(blockType, blockSubType));
        map.put(LongPos.pack(pos.x+1, pos.y, pos.z), Chunk.packInts(blockType, blockSubType));
        map.put(LongPos.pack(pos.x+1, pos.y, pos.z+1), Chunk.packInts(blockType, blockSubType));
    }

    private static long makeBranch(Long2IntOpenHashMap map, Vector3i pos, Vector3i dir, int blockType, int blockSubType) {
        makeSquare(map, new Vector3i(pos.x+dir.x, pos.y, pos.z+dir.z), blockType, blockSubType);
        makeSquare(map, new Vector3i(pos.x+(dir.x*2), pos.y-1, pos.z+(dir.z*2)), blockType, blockSubType);
        makeSquare(map, new Vector3i(pos.x+(dir.x*3), pos.y-1, pos.z+(dir.z*3)), blockType, blockSubType);
        map.put(LongPos.pack(pos.x+(dir.x*4), pos.y, pos.z+(dir.z*4)), Chunk.packInts(blockType, blockSubType));
        map.put(LongPos.pack(pos.x+(dir.x*5), pos.y, pos.z+(dir.z*5)), Chunk.packInts(blockType, blockSubType));
        return LongPos.pack(pos.x+(dir.x*5), pos.y+1, pos.z+(dir.z*5));
    }
}