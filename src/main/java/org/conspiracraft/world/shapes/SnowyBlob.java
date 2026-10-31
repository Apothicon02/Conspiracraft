package org.conspiracraft.world.shapes;

import org.conspiracraft.world.World;

public class SnowyBlob {
    public static void generate(int x, int y, int z, int blockType, int blockSubType, int radius) {
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                int xDist = lX - x, zDist = lZ - z;
                int undistance = (radius * 3) - xDist*xDist - zDist*zDist;
                if (undistance < 0) {continue;}
                undistance = (int) Math.sqrt(undistance);
                int lowY = Math.max(y - undistance, y - radius);
                int highY = Math.min(y + undistance, y + radius);
                for (int lY = lowY; lY <= highY; lY++) {
                    if (World.getBlockTypeUnchecked(lX, lY, lZ) == blockType && World.getBlockTypeUnchecked(lX, lY+1, lZ) == 0) {
                        World.setBlock(lX, lY, lZ, blockType, 8);
                    }
                }
            }
        }
    }
}
