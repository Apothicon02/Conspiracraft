package org.conspiracraft.game.world.shapes;

import org.conspiracraft.game.blocks.types.BlockTypes;
import org.joml.Vector2i;
import org.joml.Vector4i;

import static org.conspiracraft.engine.Utils.condensePos;
import static org.conspiracraft.game.world.World.*;
import static org.conspiracraft.game.world.WorldGen.setBlockWorldgen;
import static org.conspiracraft.game.world.WorldGen.setLightWorldgen;

public class Pillar {
    public static void generate(Vector2i blockOn, int x, int y, int z, int height, int blockType, int blockSubType) {
        if (blockOn.x == 23) {
            int maxHeight = y+height;
            for (int newY = y; newY < maxHeight; newY++) {
                setBlockWorldgen(x, newY, z, blockType, blockSubType);
            }
            if (BlockTypes.blockTypeMap.get(blockType).obstructingHeightmap(new Vector2i(blockType, blockSubType))) {
                int condensedPos = condensePos(x, z);
                int surfaceY = heightmap[condensedPos];
                heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], maxHeight);
                for (int newY = maxHeight; newY >= surfaceY; newY--) {
                    setLightWorldgen(x, newY, z, new Vector4i(0, 0, 0, 0));
                }
            }
        }
    }
}
