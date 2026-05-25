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
                if (inBounds(newX, y-1, newZ)) {
                    Vector3i bPos = new Vector3i(newX, y-1, newZ);
                    Vector3i aPos = new Vector3i(newX, y, newZ);
                    for (int i = 0; i <= 24; i++) {
                        bPos.sub(0, 1, 0);
                        aPos.sub(0, 1, 0);
                        if ((!BlockTypes.blockTypes[getBlock(aPos).x()].blockProperties.isSolid && !blocks.containsKey(aPos) && !map.containsKey(aPos) &&
                                (BlockTypes.blockTypes[getBlock(bPos).x()].blockProperties.isSolid || solid(blocks.get(bPos))))) {
                            addToMap(map, new Vector3i(aPos), blockType, (int) Math.abs(random.nextDouble() * 6) + 1);
                            break;
                        }
                    }
                    if (!((newX == minX || newX == maxX) && (newZ == minZ || newZ == maxZ)) && newX != x && newZ != z && (newX == minX || newX == maxX || newZ == minZ || newZ == maxZ)) {
                        addToMap(map, new Vector3i(newX, y-1, newZ), blockType, blockSubType);
                        if (random.nextDouble() * 10 < 4) {
                            addToMap(map, new Vector3i(newX, y - 2, newZ), blockType, blockSubType);
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