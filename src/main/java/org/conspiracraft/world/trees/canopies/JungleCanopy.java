package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;

import static org.conspiracraft.world.World.*;

public class JungleCanopy extends Canopy {
    private static void addToMap(Long2IntOpenHashMap map, long pos, int blockType, int blockSubType) {
        map.put(pos, Chunk.packInts(blockType, blockSubType));
    }

    public static Long2IntOpenHashMap generateCanopy(Random random, Long2IntOpenHashMap blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        addSquare(blocks, x, y+1, z, radius-1, false, blockType, blockSubType);
        for (int i = 0; i < height; i++) {
            addSquare(blocks, x, y-i, z, radius, false, blockType, blockSubType);
        }
        int minX = x-radius;
        int maxX = x+radius;
        int minZ = z-radius;
        int maxZ = z+radius;
        for (int lX = x-radius; lX <= maxX; lX++) {
            for (int lZ = z - radius; lZ <= maxZ; lZ++) {
                int minY = y+height;
                if (!((lX == minX || lX == maxX) && (lZ == minZ || lZ == maxZ)) && lX != x && lZ != z && (lX == minX || lX == maxX || lZ == minZ || lZ == maxZ)) {
                    addToMap(blocks, LongPos.pack(lX, y-1, lZ), blockType, blockSubType);
                    minY = Math.min(minY, y-1);
                    if (random.nextDouble() * 10 < 4) {
                        addToMap(blocks, LongPos.pack(lX, y - 2, lZ), blockType, blockSubType);
                        minY = Math.min(minY, y-2);
                    }
                }
                if (BlockTags.leaves.tagged.contains(blockType)) {
                    long bPos = LongPos.pack(lX, minY-height, lZ);
                    long aPos = LongPos.pack(lX, (1+minY)-height, lZ);
                    if ((isAir(aPos) && !blocks.containsKey(aPos) &&
                            (BlockTypes.blockTypes[getBlockWorldgen(bPos)>> 16].blockProperties.isSolid || solid(blocks.get(bPos))))) {
                        addToMap(blocks, aPos, blockType, (int) Math.abs(random.nextDouble() * 6) + 1);
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

    private static void addSquare(Long2IntOpenHashMap map, int x, int y, int z, int radius, boolean corners, int blockType, int blockSubType) {
        int minX = x-radius;
        int maxX = x+radius;
        int minZ = z-radius;
        int maxZ = z+radius;
        for (int newX = x-radius; newX <= maxX; newX++) {
            for (int newZ = z-radius; newZ <= maxZ; newZ++) {
                if (!((newX == minX || newX == maxX) && (newZ == minZ || newZ == maxZ)) || corners) {
                    addToMap(map, LongPos.pack(newX, y, newZ), blockType, blockSubType);
                }
            }
        }
    }
}