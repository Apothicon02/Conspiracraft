package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.SimplexNoise;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;

import static org.conspiracraft.world.World.*;

public class BlobDrippyCanopy extends Canopy {

    private static void addToMap(Long2IntOpenHashMap map, long pos, int blockType, int blockSubType) {
        map.put(pos, Chunk.packInts(blockType, blockSubType));
    }

    public static Long2IntOpenHashMap generateCanopy(Random random, Long2IntOpenHashMap blocks, int x, int y, int z, int blockType, int blockSubType, int radius, int height, int droopiness) {
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                int xDist = lX - x, zDist = lZ - z;
                int undistance = (radius * 3) - xDist*xDist - zDist*zDist;
                if (undistance < 0) {continue;}
                undistance = (int) Math.sqrt(undistance);
                int lowY = (int) Utils.mix(Math.max(y - undistance, y - height), y, 0.7f);
                int highY = Math.min(y + undistance, y + height);
                if (highY <= lowY) {continue;}
                lowY -= (int) (SimplexNoise.noise(lX/10.f, lZ/10.f)*droopiness);
                int minY = y+height;
                for (int lY = lowY; lY <= highY; lY++) {
                    addToMap(blocks, LongPos.pack(lX, lY, lZ), blockType, blockSubType);
                    minY = Math.min(minY, lY);
                }
                if (BlockTags.leaves.tagged.contains(blockType)) {
                    long bPos = LongPos.pack(lX, minY-height, lZ);
                    long aPos = LongPos.pack(lX, (1+minY)-height, lZ);
                    if ((isAir(aPos) && !blocks.containsKey(aPos) &&
                            (BlockTypes.blockTypes[getBlockWorldgen(bPos) >> 16].blockProperties.isSolid || solid(blocks.get(bPos))))) {
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
}