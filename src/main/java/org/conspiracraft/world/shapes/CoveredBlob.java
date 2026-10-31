package org.conspiracraft.world.shapes;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

import static org.conspiracraft.world.World.*;

public class CoveredBlob {
    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int coverBlockType, int coverBlockSubType, int radius, float coverChance, int[] replace, boolean update) {
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
                    blocks.put(LongPos.pack(lX, lY, lZ), Chunk.packInts(blockType, blockSubType));
                    if (Math.random() < coverChance) {
                        if (bounds.out(lX, lY+1, lZ)) {return;}
                        blocks.putIfAbsent(LongPos.pack(lX, lY+1, lZ), Chunk.packInts(coverBlockType, coverBlockSubType));
                    }
                }
            }
        }
        blocks.forEach((pos, block) -> {
            boolean canReplace = true;
            if (replace.length > 0) {
                canReplace = false;
                int replacing = getBlockTypeUnchecked(LongPos.x(pos), LongPos.y(pos), LongPos.z(pos));
                for (int replaceable : replace) {
                    if (replaceable == replacing) {
                        canReplace = true;
                        break;
                    }
                }
            }
            if (canReplace) {
                setBlockWorldgenPotentiallyGlowing(LongPos.x(pos), LongPos.y(pos), LongPos.z(pos), block >> 16, block & 0xFFFF);
            }
        });
    }

    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int coverBlockType, int coverBlockSubType, int radius, float coverChance, int[] replace) {
        generate(bounds, x, y, z, blockType, blockSubType, coverBlockType, coverBlockSubType, radius, coverChance, replace, false);
    }

    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int coverBlockType, int coverBlockSubType, int radius, float coverChance, boolean update) {
        generate(bounds, x, y, z, blockType, blockSubType, coverBlockType, coverBlockSubType, radius, coverChance, new int[0], update);
    }

    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int coverBlockType, int coverBlockSubType, int radius, float coverChance) {
        generate(bounds, x, y, z, blockType, blockSubType, coverBlockType, coverBlockSubType, radius, coverChance, new int[0], false);
    }
}
