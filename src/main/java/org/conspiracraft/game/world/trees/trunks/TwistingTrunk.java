package org.conspiracraft.game.world.trees.trunks;

import kotlin.Pair;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TwistingTrunk extends Trunk {
    public static Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generateTrunk(int oX, int oY, int oZ, int trunkHeight, int blockType, int blockSubType, boolean overgrown, int minBranchHeight) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        Set<Vector3i> canopies = new HashSet<>();

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
            if (height > oY+2 && twistable <= 0 && Math.random()*10 < 4) {
                int xOff = (int) ((Math.random()*20)-10);
                int zOff = (int) ((Math.random()*20)-10);
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
                branch = true;
                twistable = 2;
            }
            pos.add(dir);
            pos.y = height;
            makeSquare(map, new Vector3i(pos.x, pos.y-1, pos.z), blockType, blockSubType);
            makeSquare(map, pos, blockType, blockSubType);
            if (branch && pos.y >= minBranch) {
                canopies.add(makeBranch(map, pos, dir, blockType, blockSubType));
                if (overgrown) {
                    canopies.add(makeBranch(map, pos, new Vector3i(dir.x * (Math.random() >= 0.5f ? 1 : 0), +2, dir.z * (Math.random() >= 0.5f ? 1 : 0)), blockType, blockSubType));
                }
            }
            if (pos.y == maxHeight) {
                canopies.add(new Vector3i(pos.x, pos.y+1, pos.z));
                if (overgrown) {
                    canopies.add(new Vector3i(pos.x, pos.y-1, pos.z+3));
                    canopies.add(new Vector3i(pos.x+3, pos.y, pos.z));
                    canopies.add(new Vector3i(pos.x, pos.y-1, pos.z-3));
                    canopies.add(new Vector3i(pos.x-3, pos.y, pos.z));
                }
            }
        }

        return new Pair<>(map, canopies);
    }

    private static void makeSquare(Map<Vector3i, Vector2i> map, Vector3i pos, int blockType, int blockSubType) {
        map.put(new Vector3i(pos.x, pos.y, pos.z), new Vector2i(blockType, blockSubType));
        map.put(new Vector3i(pos.x, pos.y, pos.z+1), new Vector2i(blockType, blockSubType));
        map.put(new Vector3i(pos.x+1, pos.y, pos.z), new Vector2i(blockType, blockSubType));
        map.put(new Vector3i(pos.x+1, pos.y, pos.z+1), new Vector2i(blockType, blockSubType));
    }

    private static Vector3i makeBranch(Map<Vector3i, Vector2i> map, Vector3i pos, Vector3i dir, int blockType, int blockSubType) {
        makeSquare(map, new Vector3i(pos.x+dir.x, pos.y, pos.z+dir.z), blockType, blockSubType);
        makeSquare(map, new Vector3i(pos.x+(dir.x*2), pos.y-1, pos.z+(dir.z*2)), blockType, blockSubType);
        makeSquare(map, new Vector3i(pos.x+(dir.x*3), pos.y-1, pos.z+(dir.z*3)), blockType, blockSubType);
        map.put(new Vector3i(pos.x+(dir.x*4), pos.y, pos.z+(dir.z*4)), new Vector2i(blockType, blockSubType));
        map.put(new Vector3i(pos.x+(dir.x*5), pos.y, pos.z+(dir.z*5)), new Vector2i(blockType, blockSubType));
        return new Vector3i(pos.x+(dir.x*5), pos.y+1, pos.z+(dir.z*5));
    }
}