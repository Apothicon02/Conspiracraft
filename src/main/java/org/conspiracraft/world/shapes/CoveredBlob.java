package org.conspiracraft.world.shapes;

import org.conspiracraft.world.Bounds;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

import static org.conspiracraft.world.World.*;

public class CoveredBlob {
    public static void generate(Bounds bounds, int x, int y, int z, int blockType, int blockSubType, int coverBlockType, int coverBlockSubType, int radius, float coverChance, int[] replace, boolean update) {
        Map<Vector3i, Vector2i> blocks = new HashMap<>();
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                for (int lY = y - radius; lY <= y + radius; lY++) {
                    int xDist = lX - x;
                    int yDist = lY - y;
                    int zDist = lZ - z;
                    int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                    if (dist <= radius * 3) {
                        if (bounds.out(lX, lY, lZ)) {return;}
                        blocks.put(new Vector3i(lX, lY, lZ), new Vector2i(blockType, blockSubType));
                        if (Math.random() < coverChance) {
                            if (bounds.out(lX, lY+1, lZ)) {return;}
                            blocks.putIfAbsent(new Vector3i(lX, lY+1, lZ), new Vector2i(coverBlockType, coverBlockSubType));
                        }
                    }
                }
            }
        }
        blocks.forEach((pos, block) -> {
            boolean canReplace = true;
            if (replace.length > 0) {
                canReplace = false;
                int replacing = getBlock(pos).x;
                for (int replaceable : replace) {
                    if (replaceable == replacing) {
                        canReplace = true;
                        break;
                    }
                }
            }
            if (canReplace) {
                setBlockWorldgenPotentiallyGlowing(pos.x, pos.y, pos.z, block.x, block.y);
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
