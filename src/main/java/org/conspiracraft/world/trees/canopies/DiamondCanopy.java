package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.SimplexNoise;
import org.joml.Vector2i;
import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;

import static org.conspiracraft.world.World.getBlockWorldgen;
import static org.conspiracraft.world.World.isAir;

public class DiamondCanopy extends Canopy {

    private static void addToMap(Long2IntOpenHashMap map, long pos, int blockType, int blockSubType) {
        map.put(pos, Chunk.packInts(blockType, blockSubType));
    }

    public static Long2IntOpenHashMap generateCanopy(Random random, Long2IntOpenHashMap blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                int xDist = Math.abs(lX - x);
                int zDist = Math.abs(lZ - z);
                int minY = y+height;
                boolean set = false;
                for (int lY = y - height; lY <= y + height; lY++) {
                    int yDist = Math.abs(lY - y);
                    int dist = xDist+zDist+yDist;
                    if (dist <= radius && xDist < radius*0.75f && yDist < radius*0.75f && zDist < radius*0.75f) {
                        addToMap(blocks, LongPos.pack(lX, lY, lZ), blockType, blockSubType);
                        if (random.nextInt(4) == 0 && yDist < (radius*0.75f)-1) {
                            addToMap(blocks, LongPos.pack(lX, lY+1, lZ), blockType, blockSubType);
                        }
                        minY = Math.min(minY, lY);
                        set = true;
                    }
                }
                if (set) {
                    int droop = (int) (minY-Math.max(0, random.nextInt(4)-2));
                    Vector3i bPos = new Vector3i(lX, y - 1, lZ);
                    Vector3i aPos = new Vector3i(lX, y, lZ);
                    for (int i = 0; i <= 6; i++) {
                        bPos.sub(0, 1, 0);
                        aPos.sub(0, 1, 0);
                        if (aPos.y() >= droop) {
                            addToMap(blocks, LongPos.pack(aPos), blockType, blockSubType);
                        }
                    }
                }
            }
        }
        return blocks;
    }

    private static boolean solid(int block) {
        if (block == 0) {return false;}
        return BlockTypes.blockTypes[block >> 16].blockProperties.isSolid;
    }
}