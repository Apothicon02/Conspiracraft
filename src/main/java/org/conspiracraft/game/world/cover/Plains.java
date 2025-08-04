package org.conspiracraft.game.world.cover;

import org.joml.Vector2i;

import static org.conspiracraft.game.world.WorldGen.setBlockWorldgenNoReplace;

public class Plains {

    public static void generate(Vector2i blockOn, int x, int y, int z) {
        if (blockOn.x == 2) {
            double flowerChance = Math.random();
            setBlockWorldgenNoReplace(x, y + 1, z, 4 + (flowerChance > 0.95f ? (flowerChance > 0.97f ? 14 : 1) : 0), (int) (Math.random() * 3));
        }
    }
}
