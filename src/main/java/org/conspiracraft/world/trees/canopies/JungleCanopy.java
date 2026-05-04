package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.types.BlockTypes;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

import static org.conspiracraft.world.World.*;

public class JungleCanopy extends Canopy {
    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, int blockType, int blockSubType) {
        map.put(pos, new Vector2i(blockType, blockSubType));
    }

    public static Map<Vector3i, Vector2i> generateCanopy(Random random, Map<Vector3i, Vector2i> blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        addSquare(map, x, y+1, z, radius-1, false, blockType, blockSubType);
        for (int i = 0; i < height; i++) {
            addSquare(map, x, y-i, z, radius, false, blockType, blockSubType);
        }
        int minX = x-radius;
        int maxX = x+radius;
        int minZ = z-radius;
        int maxZ = z+radius;
        for (int newX = x-radius; newX <= maxX; newX++) {
            for (int newZ = z - radius; newZ <= maxZ; newZ++) {
                if (inBounds(newX, y-1, newZ) && !((newX == minX || newX == maxX) && (newZ == minZ || newZ == maxZ)) && newX != x && newZ != z && (newX == minX || newX == maxX || newZ == minZ || newZ == maxZ)) {
                    addToMap(map, new Vector3i(newX, y-1, newZ), blockType, blockSubType);
                    int condensedPos = packPos(newX, newZ);
                    int surfaceY = heightmap[condensedPos];
                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], y-1);
                    for (int extraY = y-1; extraY >= surfaceY; extraY--) {
                        if (extraY == surfaceY) {
                            Vector3i abovePos = new Vector3i(newX, extraY + 1, newZ);
                            if (BlockTypes.blockTypeMap.get(getBlock(newX, extraY, newZ).x).blockProperties.isSolid &&
                                    !BlockTypes.blockTypeMap.get(getBlock(abovePos).x).blockProperties.isSolid && !blocks.containsKey(abovePos) && !map.containsKey(abovePos)) {
                                addToMap(map, abovePos, blockType, 0);
                                Vector3i aboveAbovePos = new Vector3i(newX, extraY + 2, newZ);
                                if (!BlockTypes.blockTypeMap.get(getBlock(aboveAbovePos).x).blockProperties.isSolid && !blocks.containsKey(aboveAbovePos) && !map.containsKey(aboveAbovePos)) {
                                    addToMap(map, new Vector3i(aboveAbovePos), blockType, (int) Math.abs(random.nextDouble() * 6) + 1);
                                }
                            }
                        }
                    }
                    if (random.nextDouble()*10 < 4) {
                        addToMap(map, new Vector3i(newX, y-2, newZ), blockType, blockSubType);
                    }
                }
            }
        }
        return map;
    }

    private static void addSquare(Map<Vector3i, Vector2i> map, int x, int y, int z, int radius, boolean corners, int blockType, int blockSubType) {
        int minX = x-radius;
        int maxX = x+radius;
        int minZ = z-radius;
        int maxZ = z+radius;
        for (int newX = x-radius; newX <= maxX; newX++) {
            for (int newZ = z-radius; newZ <= maxZ; newZ++) {
                if (inBounds(newX, y, newZ) && !((newX == minX || newX == maxX) && (newZ == minZ || newZ == maxZ)) || corners) {
                    addToMap(map, new Vector3i(newX, y, newZ), blockType, blockSubType);
                }
            }
        }
    }
}