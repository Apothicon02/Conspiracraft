package org.conspiracraft.world.shapes;

import org.conspiracraft.blocks.types.BlockTypes;
import org.joml.Vector2i;

import static org.conspiracraft.world.World.*;

public class Pillar {
    public static void generate(Vector2i blockOn, int x, int y, int z, int height, int blockType, int blockSubType) {
        if (blockOn.x == 23) {
            int maxHeight = y+height;
            for (int newY = y; newY < maxHeight; newY++) {
                setBlock(x, newY, z, blockType, blockSubType);
            }
            if (BlockTypes.blockTypeMap.get(blockType).obstructingHeightmap(new Vector2i(blockType, blockSubType))) {
                int condensedPos = packPos(x, z);
                int surfaceY = heightmap[condensedPos];
                heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], maxHeight);
//                for (int newY = maxHeight; newY >= surfaceY; newY--) {
//                    setLight(x, newY, z, new Vector4i(0, 0, 0, 0));
//                }
            }
        }
    }
}
