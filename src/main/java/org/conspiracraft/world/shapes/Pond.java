package org.conspiracraft.world.shapes;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.Bounds;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.conspiracraft.world.World.*;

public class Pond {
    public static void generate(Random random, Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace, boolean update) {
        boolean dir = random.nextBoolean();
        Long2IntOpenHashMap blocks = new Long2IntOpenHashMap();
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                int xDist = lX - x;
                int zDist = lZ - z;
                int dist = xDist * xDist + zDist * zDist;
                if (dist <= radius * 2) {
                    if (bounds.out(lX-1, y-1, lZ-1) || bounds.out(lX+1, y+1, lZ+1)) {return;}
                    boolean canReplace = true;
                    if (replace.length > 0) {
                        canReplace = false;
                        int replacing = getBlockTypeUnchecked(lX, y, lZ);
                        for (int replaceable : replace) {
                            if (replaceable == replacing) {
                                canReplace = true;
                                break;
                            }
                        }
                    }
                    if (!canReplace || getBlockTypeUnchecked(lX, y+1, lZ) > 0 || !BlockTypes.blockTypes[getBlockTypeUnchecked(lX, y-1, lZ)].blockProperties.isSolid ||
                            notSolidOrWater(lX+1, y, lZ) || notSolidOrWater(lX-1, y, lZ) || notSolidOrWater(lX, y, lZ+1) || notSolidOrWater(lX, y, lZ-1)) {return;}
                    blocks.put(LongPos.pack(lX, y, lZ), Chunk.packInts(blockType, blockSubType));
                    blocks.put(LongPos.pack(lX, y-1, lZ), Chunk.packInts(BlockTypes.MUD.id, 0));
                    if (dir) {
                        blocks.putIfAbsent(LongPos.pack(lX - 1, y, lZ), Chunk.packInts(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(LongPos.pack(lX, y, lZ - 1), Chunk.packInts(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(LongPos.pack(lX - 2, y, lZ), Chunk.packInts(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(LongPos.pack(lX, y, lZ - 2), Chunk.packInts(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(LongPos.pack(lX-2, y, lZ - 2), Chunk.packInts(BlockTypes.MUD.id, 0));
                    } else {
                        blocks.putIfAbsent(LongPos.pack(lX + 1, y, lZ), Chunk.packInts(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(LongPos.pack(lX, y, lZ + 1), Chunk.packInts(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(LongPos.pack(lX + 2, y, lZ), Chunk.packInts(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(LongPos.pack(lX, y, lZ + 2), Chunk.packInts(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(LongPos.pack(lX+2, y, lZ + 2), Chunk.packInts(BlockTypes.MUD.id, 0));
                    }
                }
            }
        }
        blocks.forEach((pos, block) -> {
            setBlockOrFluidWorldgen(LongPos.x(pos), LongPos.y(pos), LongPos.z(pos), block >> 16, block & 0xFFFF);
        });
    }

    public static boolean notSolidOrWater(int x, int y, int z) {
        int blockType = getBlockTypeUnchecked(x, y, z);
        return !(blockType == BlockTypes.WATER.id || BlockTypes.blockTypes[blockType].blockProperties.isSolid);
    }

    public static void generate(Random random, Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace) {
        generate(random, bounds, x, y, z, blockType, blockSubType, radius, replace, false);
    }

    public static void generate(Random random, Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, boolean update) {
        generate(random, bounds, x, y, z, blockType, blockSubType, radius, new int[0], update);
    }

    public static void generate(Random random, Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius) {
        generate(random, bounds, x, y, z, blockType, blockSubType, radius, new int[0], false);
    }
}
