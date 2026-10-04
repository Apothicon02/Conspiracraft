package org.conspiracraft.world.shapes;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.Bounds;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.conspiracraft.world.World.getBlock;
import static org.conspiracraft.world.World.setBlockWorldgen;

public class Pond {
    public static void generate(Random random, Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace, boolean update) {
        boolean dir = random.nextBoolean();
        Map<Vector3i, Vector2i> blocks = new HashMap<>();
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
                        int replacing = getBlock(lX, y, lZ).x;
                        for (int replaceable : replace) {
                            if (replaceable == replacing) {
                                canReplace = true;
                                break;
                            }
                        }
                    }
                    if (!canReplace || getBlock(lX, y+1, lZ).x > 0 || !BlockTypes.blockTypes[getBlock(lX, y-1, lZ).x].blockProperties.isSolid ||
                            notSolidOrWater(lX+1, y, lZ) || notSolidOrWater(lX-1, y, lZ) || notSolidOrWater(lX, y, lZ+1) || notSolidOrWater(lX, y, lZ-1)) {return;}
                    blocks.put(new Vector3i(lX, y, lZ), new Vector2i(blockType, blockSubType));
                    blocks.put(new Vector3i(lX, y-1, lZ), new Vector2i(BlockTypes.MUD.id, 0));
                    if (dir) {
                        blocks.putIfAbsent(new Vector3i(lX - 1, y, lZ), new Vector2i(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(new Vector3i(lX, y, lZ - 1), new Vector2i(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(new Vector3i(lX - 2, y, lZ), new Vector2i(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(new Vector3i(lX, y, lZ - 2), new Vector2i(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(new Vector3i(lX-2, y, lZ - 2), new Vector2i(BlockTypes.MUD.id, 0));
                    } else {
                        blocks.putIfAbsent(new Vector3i(lX + 1, y, lZ), new Vector2i(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(new Vector3i(lX, y, lZ + 1), new Vector2i(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(new Vector3i(lX + 2, y, lZ), new Vector2i(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(new Vector3i(lX, y, lZ + 2), new Vector2i(BlockTypes.MUD.id, 0));
                        blocks.putIfAbsent(new Vector3i(lX+2, y, lZ + 2), new Vector2i(BlockTypes.MUD.id, 0));
                    }
                }
            }
        }
        blocks.forEach((pos, block) -> {
            setBlockWorldgen(pos.x, pos.y, pos.z, block.x, block.y);
        });
    }

    public static boolean notSolidOrWater(int x, int y, int z) {
        int blockType = getBlock(x, y, z).x;
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
