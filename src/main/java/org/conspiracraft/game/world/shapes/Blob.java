package org.conspiracraft.game.world.shapes;

import org.joml.Vector2i;
import org.joml.Vector4i;

import static org.conspiracraft.engine.Utils.condensePos;
import static org.conspiracraft.game.world.World.heightmap;
import static org.conspiracraft.game.world.World.inBounds;
import static org.conspiracraft.game.world.WorldGen.*;

public class Blob {
    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius) {
        if (blockOn.x == 10) {
            for (int lX = x - radius; lX <= x + radius; lX++) {
                for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                    if (inBounds(lX, y, lZ)) {
                        int condensedPos = condensePos(lX, lZ);
                        int surfaceY = heightmap[condensedPos];
                        for (int lY = y - radius; lY <= y + radius; lY++) {
                            int xDist = lX - x;
                            int yDist = lY - y;
                            int zDist = lZ - z;
                            int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                            if (dist <= radius * 3) {
                                setBlockWorldgen(lX, lY, lZ, blockType, blockSubType);
                                heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], lY);
                                for (int extraY = lY; extraY >= surfaceY; extraY--) {
                                    setLightWorldgen(lX, extraY, lZ, new Vector4i(0, 0, 0, 0));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
