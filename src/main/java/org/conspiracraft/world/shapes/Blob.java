package org.conspiracraft.world.shapes;

import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

import static org.conspiracraft.world.World.getBlock;
import static org.conspiracraft.world.World.setBlockWorldgen;

public class Blob {
    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace, boolean update) {
        Map<Vector3i, Vector2i> blocks = new HashMap<>();
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                for (int lY = y - radius; lY <= y + radius; lY++) {
                    int xDist = lX - x;
                    int yDist = lY - y;
                    int zDist = lZ - z;
                    int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                    if (dist <= radius * 3) {
                        if (bounds.out(lX, lY, lZ)) {return;}
                        boolean canReplace = true;
                        if (replace.length > 0) {
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

    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace) {
        generate(bounds, x, y, z, blockType, blockSubType, radius, replace, false);
    }

    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, boolean update) {
        generate(bounds, x, y, z, blockType, blockSubType, radius, new int[0], update);
    }

    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius) {
        generate(bounds, x, y, z, blockType, blockSubType, radius, new int[0], false);
    }
}
