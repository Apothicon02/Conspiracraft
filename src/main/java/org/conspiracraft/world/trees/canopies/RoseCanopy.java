package org.conspiracraft.world.trees.canopies;

import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

public class RoseCanopy extends Canopy {

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, int blockType, int blockSubType) {
        map.put(pos, new Vector2i(blockType, blockSubType));
    }

    public static Map<Vector3i, Vector2i> generateCanopy(Random random, Map<Vector3i, Vector2i> blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                for (int lY = y - height; lY <= y + height; lY++) {
                    int xDist = lX - x;
                    int yDist = lY - y;
                    int zDist = lZ - z;
                    int horizontalDist = (xDist * xDist + zDist * zDist)*3;
                    int dist = horizontalDist + yDist * yDist;
                    int yDistAbove = (lY - y)-3;
                    int distAbove = horizontalDist + yDistAbove * yDistAbove;
                    if (dist <= radius * 3 && distAbove > radius * 3) {
                        addToMap(map, new Vector3i(lX, lY, lZ), blockType, blockSubType);
                    }
                }
            }
        }
        return map;
    }
}