package org.conspiracraft.world.shapes;

import org.conspiracraft.world.World;
import org.joml.Vector2i;

import static org.conspiracraft.world.World.getBlock;

public class Blob {
    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace, boolean update) {
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                for (int lY = y - radius; lY <= y + radius; lY++) {
                    if (World.inBounds(lX, lY, lZ)) {
                        int xDist = lX - x;
                        int yDist = lY - y;
                        int zDist = lZ - z;
                        int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                        if (dist <= radius * 3) {
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
                                World.setBlock(lX, lY, lZ, blockType, blockSubType);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace) {
        generate(blockOn, x, y, z, blockType, blockSubType, radius, replace, false);
    }

    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius, boolean update) {
        generate(blockOn, x, y, z, blockType, blockSubType, radius, new int[0], update);
    }

    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius) {
        generate(blockOn, x, y, z, blockType, blockSubType, radius, new int[0], false);
    }
}
