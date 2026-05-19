package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

import static org.conspiracraft.world.World.*;

public class BlobCanopy extends Canopy {

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, int blockType, int blockSubType) {
        map.put(pos, new Vector2i(blockType, blockSubType));
    }

    public static Map<Vector3i, Vector2i> generateCanopy(Random random, Map<Vector3i, Vector2i> blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                if (inBounds(lX, y, lZ)) {
                    int condensedPos = packPos(lX, lZ);
                    int surfaceY = heightmap[condensedPos];
                    for (int lY = y - height; lY <= y + height; lY++) {
                        int xDist = lX - x;
                        int yDist = lY - y;
                        int zDist = lZ - z;
                        int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                        if (dist <= radius * 3) {
                            addToMap(map, new Vector3i(lX, lY, lZ), blockType, blockSubType);
                            heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], lY);
                            for (int extraY = lY; extraY >= surfaceY; extraY--) {
                                if (extraY == surfaceY) {
                                    Vector3i abovePos = new Vector3i(lX, extraY+1, lZ);
                                    if (BlockTypes.blockTypes[getBlock(lX, extraY, lZ).x].blockProperties.isSolid &&
                                            !BlockTypes.blockTypes[getBlock(abovePos).x].blockProperties.isSolid && !blocks.containsKey(abovePos) && !map.containsKey(abovePos)) {
                                        addToMap(map, abovePos, blockType, (int) Math.abs(random.nextDouble() * 6) + 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return map;
    }
}