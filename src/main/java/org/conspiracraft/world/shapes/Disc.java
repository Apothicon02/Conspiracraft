package org.conspiracraft.world.shapes;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;

import static org.conspiracraft.world.World.*;

public class Disc {
    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int height, int[] replace) {
        height/=2;
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap();
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                int xDist = lX - x, zDist = lZ - z;
                int undistance = (radius * 3) - xDist*xDist - zDist*zDist;
                if (undistance < 0) {continue;}
                undistance = (int) Math.sqrt(undistance);
                int lowY = Math.max(y - undistance, y - height);
                int highY = Math.min(y + undistance, y + height);
                for (int lY = lowY; lY <= highY; lY++) {
                    if (bounds.out(lX, lY, lZ)) {return;}
                    boolean canReplace = true;
                    if (replace != null && replace.length > 0) {
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
    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int height) {
        generate(bounds, x, y, z, blockType, blockSubType, radius, height, null);
    }
}
