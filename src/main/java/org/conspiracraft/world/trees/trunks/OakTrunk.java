package org.conspiracraft.world.trees.trunks;

import kotlin.Pair;
import org.conspiracraft.world.Directions;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class OakTrunk extends Trunk {
    public static Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generateTrunk(Random random, int oX, int oY, int oZ, int trunkHeight, int blockType, int blockSubType, int branchOffset) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        Set<Vector3i> canopies = new HashSet<>();

        int prevXPositive = 0;
        int prevZPositive = 0;
        int twistable = 0;
        int maxHeight = trunkHeight+oY;
        Vector3i pos = new Vector3i(oX, oY, oZ);
        int trunkDir = random.nextInt(Directions.horizontalDirs.length);
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[trunkDir]), new Vector2i(blockType, blockSubType));
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[(trunkDir+1)%Directions.horizontalDirs.length]).sub(0, 1, 0), new Vector2i(blockType, blockSubType));
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[(trunkDir+2)%Directions.horizontalDirs.length]).add(Directions.horizontalDirs[(trunkDir+3)%Directions.horizontalDirs.length]).sub(0, 1, 0), new Vector2i(blockType, blockSubType));
        trunkDir = (trunkDir+2)%Directions.horizontalDirs.length;
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[trunkDir]).add(0, trunkHeight-branchOffset-1, 0), new Vector2i(blockType, blockSubType));
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[(trunkDir+1)%Directions.horizontalDirs.length]).add(0, trunkHeight-branchOffset, 0), new Vector2i(blockType, blockSubType));
        putColumn(map, new Vector3i(pos).add(Directions.horizontalDirs[(trunkDir+2)%Directions.horizontalDirs.length]).add(Directions.horizontalDirs[(trunkDir+3)%Directions.horizontalDirs.length]).add(0, trunkHeight-branchOffset, 0), new Vector2i(blockType, blockSubType));
        for (int height = oY; height <= maxHeight-4; height++) {
//            twistable--;
//            Vector3i dir = new Vector3i(0, 0, 0);
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
            map.put(new Vector3i(pos.x, pos.y()-1, pos.z), new Vector2i(blockType, blockSubType));
            map.put(new Vector3i(pos.x, pos.y, pos.z), new Vector2i(blockType, blockSubType));
            if (pos.y == maxHeight-4) {
                canopies.add(new Vector3i(pos.x, pos.y()+5, pos.z));
            }
        }

        return new Pair<>(map, canopies);
    }

    public static void putColumn(Map<Vector3i, Vector2i> map, Vector3i pos, Vector2i block) {
        map.put(new Vector3i(pos).sub(0, 1, 0), block);
        map.put(pos, block);
        map.put(new Vector3i(pos).add(0, 1, 0), block);
    }
}