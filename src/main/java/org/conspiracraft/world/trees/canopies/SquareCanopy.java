package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.BlockPos;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;
import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;

import static org.conspiracraft.world.World.*;

public class SquareCanopy extends Canopy {

    private static void addToMap(Long2IntOpenHashMap map, long pos, int blockType, int blockSubType) {
        map.put(pos, Chunk.packInts(blockType, blockSubType));
    }

    public static Long2IntOpenHashMap generateCanopy(Random random, Long2IntOpenHashMap blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        y -= (height-1);
        for (int i = 0; i <= height; i++) {
            int actualRadius = radius;
            if ((i == 0 && height > 2) || ((i == height && height < 5)) || (i == height-1 && height >= 5)) {
                actualRadius--;
            } else if (height >= 5 && i == height) {
                actualRadius -= 2;
            }
            addSquare(random, blocks, blocks, new Vector3i(x, y+i, z), actualRadius, false, blockType, blockSubType);
        }
        return blocks;
    }

    private static void addSquare(Random random, Long2IntOpenHashMap blocks, Long2IntOpenHashMap map, Vector3i pos, int radius, boolean corners, int blockType, int blockSubType) {
        int minX = pos.x()-radius;
        int maxX = pos.x()+radius;
        int minZ = pos.z()-radius;
        int maxZ = pos.z()+radius;
        for (int x = pos.x()-radius; x <= maxX; x++) {
            for (int z = pos.z()-radius; z <= maxZ; z++) {
                if ((!((x == minX || x == maxX) && (z == minZ || z == maxZ)) || corners)) {
                    addToMap(map, LongPos.pack(x, pos.y(), z), blockType, blockSubType);
                    if (BlockTags.leaves.tagged.contains(blockType)) {
                        long bPos = LongPos.pack(x, pos.y()-height, z);
                        long aPos = LongPos.pack(x, (1+pos.y())-height, z);
                        if ((isAir(aPos) && !blocks.containsKey(aPos) && !map.containsKey(aPos) &&
                                (BlockTypes.blockTypes[getBlockWorldgen(bPos).x()].blockProperties.isSolid || solid(blocks.get(bPos))))) {
                            addToMap(map, aPos, blockType, (int) Math.abs(random.nextDouble() * 6) + 1);
                        }
                    }
                }
            }
        }
    }

    private static boolean solid(int block) {
        if (block == 0) {return false;}
        return BlockTypes.blockTypes[block >> 16].blockProperties.isSolid;
    }
}