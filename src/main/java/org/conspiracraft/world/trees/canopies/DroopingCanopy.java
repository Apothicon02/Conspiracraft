package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.BlockPos;
import org.conspiracraft.world.Directions;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

public class DroopingCanopy extends Canopy {
    private static Vector2i getLeaves(Random random, Vector3i pos) {return new Vector2i(BlockTypes.ACACIA_LEAVES.id, 0);}

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, Random random) {
        map.put(pos, getLeaves(random, pos));
    }

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, int minY) {
        if (pos.y() < minY) {
            pos.add(Directions.NORTH).add(Directions.EAST);
            map.put(pos, getLeaves(random, pos));
        } else {
            map.put(pos, getLeaves(random, pos));
        }
    }
    
    public static Map<Vector3i, Vector2i> generateCanopy(Random random, Map<Vector3i, Vector2i> blocks, int ogX, int ogY, int ogZ, int blockType, int blockSubType, int trunkHeight, Vector3i treeOrigin, int height, int maxRadius) {
        Vector3i origin = new Vector3i(ogX, ogY, ogZ);
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());

        for (int x = origin.x()-maxRadius; x <= origin.x()+maxRadius; x++) {
            for (int z = origin.z()-maxRadius; z <= origin.z()+maxRadius; z++) {
                int minY = origin.y();
                int maxY = (origin.y()+height);
                for (int y = minY; y <= maxY; y++) {
                    int radius = maxRadius;
                    if ((y == minY && height > 2) || (y == maxY-1 && height >= 4)) {
                        radius--;
                    } else if (y == maxY && height >= 5) {
                        radius-=2;
                    }
                    radius*=radius;
                    int xDist = x-origin.x();
                    int zDist = z-origin.z();
                    int dist = xDist*xDist+zDist*zDist;
                    if (dist <= radius) {
                        int droop = 0;
                        if (dist >= radius-2 && y < minY+(height/2)) {
                            //droop = (int) (NoiseCoverPlacement.HEIGHT_NOISE.getValue(x, z, false)*3);
                            if (dist >= radius-2) {
                                droop++;
                            }
                        }
                        int xOgDist = x-treeOrigin.x();
                        int zOgDist = z-treeOrigin.z();
                        int originDist = xOgDist*xOgDist+zOgDist*zOgDist;
                        BlockPos pos = new BlockPos(x, y-(originDist/35)-(dist/22), z);
                        for (int i = 0; i <= droop; i++) {
                            if (droop == 2) {
                                addSquare(map, pos.below(i), random, 1, true, minY);
                            } else {
                                addToMap(map, pos.below(i), random, minY);
                            }
                        }
                    }
                }
            }
        }

        return map;
    }

    private static void addSquare(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, int radius, boolean corners, int minY) {
        int minX = pos.x()-radius;
        int maxX = pos.x()+radius;
        int minZ = pos.z()-radius;
        int maxZ = pos.z()+radius;
        if (radius == 0) {
            maxX+=1;
            maxZ+=1;
        }
        for (int x = pos.x()-radius; x <= maxX; x++) {
            for (int z = pos.z()-radius; z <= maxZ; z++) {
                if (!((x == minX || x == maxX) && (z == minZ || z == maxZ)) || corners) {
                    addToMap(map, new Vector3i(x, pos.y(), z), random, minY);
                }
            }
        }
    }
}