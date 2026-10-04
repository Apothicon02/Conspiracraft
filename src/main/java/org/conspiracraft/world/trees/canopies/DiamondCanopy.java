package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.types.BlockTypes;
import org.joml.SimplexNoise;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

import static org.conspiracraft.world.World.getBlockWorldgen;

public class DiamondCanopy extends Canopy {

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, int blockType, int blockSubType) {
        map.put(pos, new Vector2i(blockType, blockSubType));
    }

    public static Map<Vector3i, Vector2i> generateCanopy(Random random, Map<Vector3i, Vector2i> blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                int minY = y+height;
                boolean set = false;
                for (int lY = y - height; lY <= y + height; lY++) {
                    int xDist = Math.abs(lX - x);
                    int yDist = Math.abs(lY - y);
                    int zDist = Math.abs(lZ - z);
                    int dist = xDist+zDist+yDist;
                    if (dist <= radius && xDist < radius*0.75f && yDist < radius*0.75f && zDist < radius*0.75f) {
                        addToMap(map, new Vector3i(lX, lY, lZ), blockType, blockSubType);
                        if (random.nextInt(4) == 0 && yDist < (radius*0.75f)-1) {
                            addToMap(map, new Vector3i(lX, lY+1, lZ), blockType, blockSubType);
                        }
                        minY = Math.min(minY, lY);
                        set = true;
                    }
                }
                if (set) {
                    int droop = (int) (minY-Math.max(0, random.nextInt(4)-2));
                    Vector3i bPos = new Vector3i(lX, y - 1, lZ);
                    Vector3i aPos = new Vector3i(lX, y, lZ);
                    for (int i = 0; i <= 6; i++) {
                        bPos.sub(0, 1, 0);
                        aPos.sub(0, 1, 0);
                        if (aPos.y() >= droop) {
                            addToMap(map, new Vector3i(aPos), blockType, blockSubType);
                        }
                    }
                }
            }
        }
        return map;
    }

    private static boolean solid(Vector2i block) {
        if (block == null) {return false;}
        return BlockTypes.blockTypes[block.x()].blockProperties.isSolid;
    }
}