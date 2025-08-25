package org.conspiracraft.game.world.cover;

import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.Vector2i;
import org.joml.Vector4i;

import static org.conspiracraft.engine.Utils.condensePos;
import static org.conspiracraft.game.world.World.heightmap;
import static org.conspiracraft.game.world.World.inBounds;
import static org.conspiracraft.game.world.WorldGen.*;

public class Mud {
    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius, boolean replace) {
        Vector2i block = new Vector2i(blockType, blockSubType);
        BlockType type = BlockTypes.blockTypeMap.get(blockType);
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                if (inBounds(lX, y, lZ)) {
                    int condensedPos = condensePos(lX, lZ);
                    int surfaceY = heightmap[condensedPos];
                    for (int lY = Math.max(World.seaLevel, y - radius); lY <= y + radius; lY++) {
                        int xDist = lX - x;
                        int yDist = lY - y;
                        int zDist = lZ - z;
                        int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                        if (dist <= radius * 3) {
                            if (replace ? BlockTypes.blockTypeMap.get(getBlockWorldgen(lX, lY, lZ).x).blockProperties.isSolid : true) {
                                setBlockWorldgen(lX, lY, lZ, blockType, blockSubType);
                                if (type.obstructingHeightmap(block)) {
                                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], lY);
                                    if (type.blockProperties.blocksLight) {
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
    }

    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius) {
        generate(blockOn, x, y, z, blockType, blockSubType, radius, false);
    }
}
