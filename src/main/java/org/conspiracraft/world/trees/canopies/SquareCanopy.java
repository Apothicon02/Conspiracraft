package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.BlockPos;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

import static org.conspiracraft.world.World.*;

public class SquareCanopy extends Canopy {

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, int blockType, int blockSubType) {
        map.put(pos, new Vector2i(blockType, blockSubType));
    }

    public static Map<Vector3i, Vector2i> generateCanopy(Random random, Map<Vector3i, Vector2i> blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());

        y -= (height-1);
        for (int i = 0; i <= height; i++) {
            int actualRadius = radius;
            if ((i == 0 && height > 2) || ((i == height && height < 5)) || (i == height-1 && height >= 5)) {
                actualRadius--;
            } else if (height >= 5 && i == height) {
                actualRadius -= 2;
            }
            addSquare(random, blocks, map, new Vector3i(x, y+i, z), actualRadius, false, blockType, blockSubType);
        }
        return map;
    }

    private static void addSquare(Random random, Map<Vector3i, Vector2i> blocks, Map<Vector3i, Vector2i> map, Vector3i pos, int radius, boolean corners, int blockType, int blockSubType) {
        int minX = pos.x()-radius;
        int maxX = pos.x()+radius;
        int minZ = pos.z()-radius;
        int maxZ = pos.z()+radius;
        for (int x = pos.x()-radius; x <= maxX; x++) {
            for (int z = pos.z()-radius; z <= maxZ; z++) {
                if (inBounds(x, pos.y(), z) && (!((x == minX || x == maxX) && (z == minZ || z == maxZ)) || corners)) {
                    addToMap(map, new BlockPos(x, pos.y(), z), blockType, blockSubType);
                    Vector3i bPos = new Vector3i(x, pos.y()-1, z);
                    Vector3i aPos = new Vector3i(x, pos.y(), z);
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

    private static boolean solid(Vector2i block) {
        if (block == null) {return false;}
        return BlockTypes.blockTypes[block.x()].blockProperties.isSolid;
    }
}