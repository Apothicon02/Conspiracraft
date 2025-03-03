package org.conspiracraft.game.world.trees.canopies;

import org.joml.Vector4i;

import static org.conspiracraft.engine.Utils.*;
import static org.conspiracraft.game.world.World.*;
import static org.conspiracraft.game.world.WorldGen.*;

public class BlobCanopy extends Canopy {
    public static void generateCanopy(int x, int y, int z, int blockType, int blockSubType, int radius) {
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                for (int lY = y - radius; lY <= y + radius; lY++) {
                    int xDist = lX - x;
                    int yDist = lY - y;
                    int zDist = lZ - z;
                    int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                    if (dist <= radius * 3 && inBounds(lX, lY, lZ)) {
                        setBlockWorldgen(lX, lY, lZ, blockType, blockSubType);
                        int condensedPos = condensePos(lX, lZ);
                        heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], lY);
                        for (int extraY = lY; extraY >= seaLevel; extraY--) {
                            setLightWorldgen(lX, extraY, lZ, new Vector4i(0, 0, 0, 0));
                        }
                    }
                }
            }
        }
    }
}