package org.conspiracraft.world.shapes;

import org.conspiracraft.world.Bounds;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

import static org.conspiracraft.world.World.*;

public class Disc {
    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int height, int[] replace) {
        Map<Vector3i, Vector2i> blocks = new HashMap<>();
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                for (int lY = y - height; lY <= y + height; lY++) {
                    int xDist = lX - x;
                    int yDist = lY - y;
                    int zDist = lZ - z;
                    int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                    if (dist <= radius * 3) {
                        if (bounds.out(lX, lY, lZ)) {return;}
                        boolean canReplace = true;
                        if (replace != null && replace.length > 0) {
                            canReplace = false;
                            int replacing = getBlock(lX, lY, lZ).x;
                            for (int replaceable : replace) {
                                if (replaceable == replacing) {
                                    canReplace = true;
                                    break;
                                }
                            }
                        }
                        if (canReplace) {
                            blocks.put(new Vector3i(lX, lY, lZ), new Vector2i(blockType, blockSubType));
                        }
                    }
                }
            }
        }
        blocks.forEach((pos, block) -> {
            setBlockWorldgen(pos.x, pos.y, pos.z, block.x, block.y);
        });
    }
    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        generate(bounds, x, y, z, blockType, blockSubType, radius, height, null);
    }
}
