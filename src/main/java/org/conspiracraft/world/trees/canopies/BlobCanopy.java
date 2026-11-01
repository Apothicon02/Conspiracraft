package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.conspiracraft.world.World;
import org.joml.Vector2i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;

import static org.conspiracraft.world.World.*;

public class BlobCanopy extends Canopy {

    private static void addToMap(Long2IntOpenHashMap map, long pos, int blockType, int blockSubType) {
        map.put(pos, Chunk.packInts(blockType, blockSubType));
    }

    public static Long2IntOpenHashMap generateCanopy(Random random, Long2IntOpenHashMap blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                boolean set = false;
                for (int lY = y - height; lY <= y + height; lY++) {
                    int xDist = lX - x;
                    int yDist = lY - y;
                    int zDist = lZ - z;
                    int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                    if (dist <= radius * 3) {
                        addToMap(blocks, LongPos.pack(lX, lY, lZ), blockType, blockSubType);
                        set = true;
                    }
                }
                if (set) {
                    for (int i = 0; i <= 24; i++) {
                        long bPos = LongPos.pack(lX, y - 1, lZ);
                        long aPos = LongPos.pack(lX, y, lZ);
                        if ((isAir(aPos) && !blocks.containsKey(aPos) &&
                                (BlockTypes.blockTypes[getBlockWorldgen(bPos).x()].blockProperties.isSolid || solid(blocks.get(bPos))))) {
                            addToMap(blocks, aPos, blockType, (int) Math.abs(random.nextDouble() * 6) + 1);
                            break;
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