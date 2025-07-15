package org.conspiracraft.game.world.trees.canopies;

import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.conspiracraft.game.world.WorldGen;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.Map;
import java.util.Random;

import static org.conspiracraft.engine.Utils.condensePos;
import static org.conspiracraft.game.world.World.*;
import static org.conspiracraft.game.world.WorldGen.*;

public class JungleCanopy extends Canopy {
    public static void generateCanopy(int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        addSquare(x, y+1, z, radius-1, false, blockType, blockSubType);
        for (int i = 0; i < height; i++) {
            addSquare(x, y-i, z, radius, false, blockType, blockSubType);
        }
        int minX = x-radius;
        int maxX = x+radius;
        int minZ = z-radius;
        int maxZ = z+radius;
        for (int newX = x-radius; newX <= maxX; newX++) {
            for (int newZ = z - radius; newZ <= maxZ; newZ++) {
                if (World.inBounds(newX, y-1, newZ) && !((newX == minX || newX == maxX) && (newZ == minZ || newZ == maxZ)) && newX != x && newZ != z && (newX == minX || newX == maxX || newZ == minZ || newZ == maxZ)) {
                    WorldGen.setBlockWorldgen(newX, y-1, newZ, blockType, blockSubType);
                    int condensedPos = condensePos(newX, newZ);
                    int surfaceY = heightmap[condensedPos];
                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], y-1);
                    for (int extraY = y-1; extraY >= seaLevel; extraY--) {
                        setLightWorldgen(newX, extraY, newZ, new Vector4i(0, 0, 0, 0));
                        if (extraY == surfaceY) {
                            if (BlockTypes.blockTypeMap.get(getBlockWorldgen(newX, extraY, newZ).x).blockProperties.isSolid) {
                                setBlockWorldgen(newX, extraY + 1, newZ, blockType, 0);
                                setBlockWorldgen(newX, extraY + 2, newZ, blockType, (int) Math.abs(Math.random() * 6) + 1);
                            }
                        }
                    }
                    if (Math.random()*10 < 4) {
                        WorldGen.setBlockWorldgen(newX, y-2, newZ, blockType, blockSubType);
                    }
                }
            }
        }
    }

    private static void addSquare(int x, int y, int z, int radius, boolean corners, int blockType, int blockSubType) {
        int minX = x-radius;
        int maxX = x+radius;
        int minZ = z-radius;
        int maxZ = z+radius;
        for (int newX = x-radius; newX <= maxX; newX++) {
            for (int newZ = z-radius; newZ <= maxZ; newZ++) {
                if (World.inBounds(newX, y, newZ) && !((newX == minX || newX == maxX) && (newZ == minZ || newZ == maxZ)) || corners) {
                    WorldGen.setBlockWorldgen(newX, y, newZ, blockType, blockSubType);
                    int condensedPos = condensePos(newX, newZ);
                    int surfaceY = heightmap[condensedPos];
                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], y);
                    for (int extraY = y; extraY >= seaLevel; extraY--) {
                        setLightWorldgen(newX, extraY, newZ, new Vector4i(0, 0, 0, 0));
                        if (extraY == surfaceY) {
                            if (BlockTypes.blockTypeMap.get(getBlockWorldgen(newX, extraY, newZ).x).blockProperties.isSolid) {
                                setBlockWorldgenNoReplaceSolids(newX, extraY + 1, newZ, blockType, 0);
                                setBlockWorldgenNoReplaceSolids(newX, extraY + 2, newZ, blockType, (int) Math.abs(Math.random() * 6) + 1);
                            }
                        }
                    }
                }
            }
        }
    }
}