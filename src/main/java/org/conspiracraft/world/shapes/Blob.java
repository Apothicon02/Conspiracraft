package org.conspiracraft.world.shapes;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.conspiracraft.world.*;
import org.joml.Vector3i;

import java.util.Map;

import static org.conspiracraft.world.World.*;

public class Blob {
    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace, boolean update) {
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap();
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                int xDist = lX - x, zDist = lZ - z;
                int undistance = (radius * 3) - xDist*xDist - zDist*zDist;
                if (undistance < 0) {continue;}
                undistance = (int) Math.sqrt(undistance);
                int lowY = Math.max(y - undistance, y - radius);
                int highY = Math.min(y + undistance, y + radius);
                for (int lY = lowY; lY <= highY; lY++) {
                    if (bounds.out(lX, lY, lZ)) {return;}
                    boolean canReplace = true;
                    if (replace.length > 0) {
                        canReplace = false;
                        int replacing = getBlockTypeUnchecked(lX, lY, lZ);
                        for (int replaceable : replace) {
                            if (replaceable == replacing) {
                                canReplace = true;
                                break;
                            }
                        }
                    }
                    if (canReplace) {
                        blocks.put(LongPos.pack(lX, lY, lZ), Chunk.packInts(blockType, blockSubType));
                    }
                }
            }
        }
        blocks.forEach((pos, block) -> {
            setBlockWorldgen(pos, block >> 16, block & 0xFFFF);
        });
    }

    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace) {
        generate(bounds, x, y, z, blockType, blockSubType, radius, replace, false);
    }

    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, boolean update) {
        generate(bounds, x, y, z, blockType, blockSubType, radius, new int[0], update);
    }

    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius) {
        generate(bounds, x, y, z, blockType, blockSubType, radius, new int[0], false);
    }

    public static void generatePostWorldgen(int x, int y, int z, int blockType, int blockSubType, int radius) {
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap();
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                int xDist = lX - x, zDist = lZ - z;
                int undistance = (radius * 3) - xDist*xDist - zDist*zDist;
                if (undistance < 0) {continue;}
                undistance = (int) Math.sqrt(undistance);
                int lowY = Math.max(y - undistance, y - radius);
                int highY = Math.min(y + undistance, y + radius);
                for (int lY = lowY; lY <= highY; lY++) {
                    blocks.put(LongPos.pack(lX, lY, lZ), Chunk.packInts(blockType, blockSubType));
                }
            }
        }
        blocks.forEach((pos, block) -> {
            setBlock(LongPos.x(pos), LongPos.y(pos), LongPos.z(pos), block >> 16, block & 0xFFFF, true, false, true);
        });
    }
}
