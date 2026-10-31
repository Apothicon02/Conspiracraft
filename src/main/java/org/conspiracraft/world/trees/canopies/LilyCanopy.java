package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;

public class LilyCanopy extends Canopy {

    private static void addToMap(Long2IntOpenHashMap map, long pos, int blockType, int blockSubType) {
        map.put(pos, Chunk.packInts(blockType, blockSubType));
    }

    public static Long2IntOpenHashMap generateCanopy(Random random, Long2IntOpenHashMap blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        int nectarHeight = y+(height/2)-1;
        addToMap(blocks, LongPos.pack(x, nectarHeight, z), BlockTypes.DANDELION_PETALS.id, 0);
        int highestCenter = y;
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                for (int lY = y - height; lY <= y + height; lY++) {
                    int xDist = lX - x;
                    int yDist = lY - y;
                    int zDist = lZ - z;
                    int horizontalDist = (xDist * xDist + zDist * zDist)*3;
                    int dist = horizontalDist + yDist * yDist;
                    int yDistAbove = (lY - y)-3;
                    int distAbove = horizontalDist + yDistAbove * yDistAbove;
                    if (dist <= radius * 3 && distAbove > radius * 3) {
                        addToMap(blocks, LongPos.pack(lX, lY, lZ), blockType, blockSubType);
                        if (lX == x && lZ == z) {
                            highestCenter = Math.max(highestCenter, lY);
                        }
                    }
                }
            }
        }
        for (int lY = highestCenter; lY < nectarHeight; lY++) {
            addToMap(blocks, LongPos.pack(x, lY, z), BlockTypes.STEM_FENCE.id, 0);
        }
        return blocks;
    }

    private static boolean solid(int block) {
        if (block == 0) {return false;}
        return BlockTypes.blockTypes[block >> 16].blockProperties.isSolid;
    }
}