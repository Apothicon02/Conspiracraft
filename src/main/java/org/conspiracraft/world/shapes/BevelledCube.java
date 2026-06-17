package org.conspiracraft.world.shapes;

import org.conspiracraft.world.World;
import org.joml.Vector2i;

import static org.conspiracraft.world.World.getBlock;

public class BevelledCube {
    public static void generate(int x, int y, int z, int blockType, int blockSubType, int radius) {
        int halfRadius = radius / 2;
        for (int lX = x - halfRadius; lX <= x + halfRadius; lX++) {
            for (int lZ = z - halfRadius; lZ <= z + halfRadius; lZ++) {
                for (int lY = y - halfRadius; lY <= y + halfRadius; lY++) {
                    if (World.inBounds(lX, lY, lZ)) {
                        int xDist = lX - x;
                        int yDist = lY - y;
                        int zDist = lZ - z;
                        int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                        if (dist <= radius * 8) {
                            World.setBlock(lX, lY, lZ, blockType, blockSubType);
                        }
                    }
                }
            }
        }
    }
}
