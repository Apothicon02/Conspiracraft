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
                    boolean set = false;
                    for (int lY = y - height; lY <= y + height; lY++) {
                        int xDist = lX - x;
                        int yDist = lY - y;
                        int zDist = lZ - z;
                        int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                        if (dist <= radius * 3) {
                            addToMap(map, new Vector3i(lX, lY, lZ), blockType, blockSubType);
                            set = true;
                        }
                    }
                    if (set) {
                        Vector3i bPos = new Vector3i(lX, y - 1, lZ);
                        Vector3i aPos = new Vector3i(lX, y, lZ);
                        for (int i = 0; i <= 24; i++) {
                            bPos.sub(0, 1, 0);
                            aPos.sub(0, 1, 0);
                            if ((!BlockTypes.blockTypes[getBlock(aPos).x()].blockProperties.isSolid && !blocks.containsKey(aPos) && !map.containsKey(aPos) &&
                                    (BlockTypes.blockTypes[getBlock(bPos).x()].blockProperties.isSolid || solid(blocks.get(bPos))))) {
                                addToMap(map, new Vector3i(aPos), blockType, (int) Math.abs(random.nextDouble() * 6) + 1);
                                break;
                            }
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