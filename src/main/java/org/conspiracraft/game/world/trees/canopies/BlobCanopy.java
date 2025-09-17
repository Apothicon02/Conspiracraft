package org.conspiracraft.game.world.trees.canopies;

import org.conspiracraft.game.blocks.types.BlockTypes;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.Map;

import static org.conspiracraft.engine.Utils.*;
import static org.conspiracraft.game.world.World.*;
import static org.conspiracraft.game.world.WorldGen.*;

public class BlobCanopy extends Canopy {

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, int blockType, int blockSubType) {
        map.put(pos, new Vector2i(blockType, blockSubType));
    }

    public static Map<Vector3i, Vector2i> generateCanopy(int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                if (inBounds(lX, y, lZ)) {
                    int condensedPos = condensePos(lX, lZ);
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
                                    if (BlockTypes.blockTypeMap.get(getBlockWorldgen(lX, extraY, lZ).x).blockProperties.isSolid &&
                                            !BlockTypes.blockTypeMap.get(getBlockWorldgen(lX, extraY+1, lZ).x).blockProperties.isSolid) {
                                        addToMap(map, new Vector3i(lX, extraY+1, lZ), blockType, (int) Math.abs(Math.random() * 6) + 1);
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