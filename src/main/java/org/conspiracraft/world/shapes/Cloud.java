package org.conspiracraft.world.shapes;

import org.conspiracraft.world.World;
import org.joml.Vector2i;

import java.util.BitSet;

public class Cloud {
    public static void generate(int x, int y, int z, int blockType, int blockSubType, int radius) {
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                for (int lY = y - radius; lY <= y + radius; lY++) {
                    World.setBlock(lX, lY, lZ, blockType, blockSubType);
                }
            }
        }
    }
}
