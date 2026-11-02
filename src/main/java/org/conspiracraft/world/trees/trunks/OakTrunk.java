package org.conspiracraft.world.trees.trunks;

import kotlin.Pair;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.Directions;
import org.conspiracraft.world.LongPos;
import org.joml.Vector3i;

import java.util.HashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;
import java.util.Set;

public class OakTrunk extends Trunk {
    public static Pair<Long2IntOpenHashMap, Set<Long>> generateTrunk(Random random, int oX, int oY, int oZ, int trunkHeight, int blockType, int blockSubType, int branchOffset) {
        Long2IntOpenHashMap map = new Long2IntOpenHashMap(trunkHeight*8);
        Set<Long> canopies = new HashSet<>();

        int prevXPositive = 0;
        int prevZPositive = 0;
        int twistable = 0;
        int maxHeight = trunkHeight+oY;
        Vector3i pos = new Vector3i(oX, oY, oZ);
        int trunkDir = random.nextInt(Directions.horizontalDirs.length);
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[trunkDir]), Chunk.packInts(blockType, blockSubType));
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[(trunkDir+1)%Directions.horizontalDirs.length]).sub(0, 1, 0), Chunk.packInts(blockType, blockSubType));
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[(trunkDir+2)%Directions.horizontalDirs.length]).add(Directions.horizontalDirs[(trunkDir+3)%Directions.horizontalDirs.length]).sub(0, 1, 0), Chunk.packInts(blockType, blockSubType));
        trunkDir = (trunkDir+2)%Directions.horizontalDirs.length;
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[trunkDir]).add(0, trunkHeight-branchOffset-1, 0), Chunk.packInts(blockType, blockSubType));
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[(trunkDir+1)%Directions.horizontalDirs.length]).add(0, trunkHeight-branchOffset, 0), Chunk.packInts(blockType, blockSubType));
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[(trunkDir+2)%Directions.horizontalDirs.length]).add(Directions.horizontalDirs[(trunkDir+3)%Directions.horizontalDirs.length]).add(0, trunkHeight-branchOffset, 0), Chunk.packInts(blockType, blockSubType));
        for (int height = oY; height <= maxHeight-4; height++) {
//            twistable--;
//            long dir = LongPos.pack(0, 0, 0);
//            if (height > oY+2 && twistable <= 0 && random.nextFloat()*10 < 1) {
//                int xOff = (int) ((random.nextFloat()*20)-10);
//                int zOff = (int) ((random.nextFloat()*20)-10);
//                boolean xPositive = xOff >= prevXPositive;
//                boolean zPositive = zOff >= prevZPositive;
//                if (xPositive) {
//                    prevXPositive = 5;
//                    dir.x += 1;
//                } else {
//                    prevXPositive = -5;
//                    dir.x -= 1;
//                }
//                if (zPositive) {
//                    prevZPositive = 5;
//                    dir.z += 1;
//                } else {
//                    prevZPositive = -5;
//                    dir.z -= 1;
//                }
//                twistable = 2;
//            }
//            pos.add(dir);
            pos.y = height;
            map.put(LongPos.pack(pos.x, pos.y()-1, pos.z), Chunk.packInts(blockType, blockSubType));
            map.put(LongPos.pack(pos.x, pos.y, pos.z), Chunk.packInts(blockType, blockSubType));
            if (pos.y == maxHeight-4) {
                canopies.add(LongPos.pack(pos.x, pos.y()+4, pos.z));
            }
        }

        return new Pair<>(map, canopies);
    }

    public static void putColumn(Long2IntOpenHashMap map, Vector3i pos, int block) {
        map.put(LongPos.pack(pos, 0, -1, 0), block);
        map.put(LongPos.pack(pos), block);
        map.put(LongPos.pack(pos, 0, 1, 0), block);
    }
}