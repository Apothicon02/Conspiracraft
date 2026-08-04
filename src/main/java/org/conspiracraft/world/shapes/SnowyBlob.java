package org.conspiracraft.world.shapes;

import org.conspiracraft.world.World;
import org.joml.Vector2i;

import static org.conspiracraft.world.World.getBlock;

public class SnowyBlob {
    public static void generate(int x, int y, int z, int blockType, int blockSubType, int radius) {
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                for (int lY = y - radius; lY <= y + radius; lY++) {
                    if (World.inBounds(lX, lY, lZ)) {
                        int xDist = lX - x;
                        int yDist = lY - y;
                        int zDist = lZ - z;
                        int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                        if (dist <= radius * 3) {
                            if (World.getBlock(lX, lY, lZ).x() == blockType && World.getBlock(lX, lY+1, lZ).x() == 0) {
                                World.setBlock(lX, lY, lZ, blockType, 8);
                            }
                        }
                    }
                }
            }
        }
    }
}
