package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.utils.Utils;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

public class PalmCanopy extends Canopy {
    private static Vector2i getLeaves(Random random, Vector3i pos, int blockType, int blockSubType) {return new Vector2i(blockType, blockSubType);}

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, int blockType, int blockSubType) {
        map.put(pos, getLeaves(random, pos, blockType, blockSubType));
    }

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, int trunkDist, Vector3i origin, Vector3i treeOrigin, int blockType, int blockSubType) {
        int x = pos.x();
        int y = pos.y();
        int z = pos.z();
        int xDist = x-origin.x();
        int zDist = z-origin.z();
        int dist = xDist*xDist+zDist*zDist;
        int xTreeDist = x-treeOrigin.x();
        int zTreeDist = z-treeOrigin.z();
        int treeDist = xTreeDist * xTreeDist + zTreeDist * zTreeDist;
        Vector3i offsetPos = new Vector3i(x, y-(treeDist/52)-(dist/52)+(trunkDist/38), z);
        map.put(offsetPos, getLeaves(random, offsetPos, blockType, blockSubType));
    }

    private static void createFrond(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, int trunkDist, Vector3i origin, Vector3i treeOrigin, int northSouth, int eastWest, int blockType, int blockSubType) {
        addSquare(map, Utils.addVec(pos, northSouth, 0, eastWest), random, 1, true, trunkDist, origin, treeOrigin, blockType, blockSubType);
        addSquare(map, Utils.addVec(pos, northSouth, -1, eastWest), random, 1, true, trunkDist, origin, treeOrigin, blockType, blockSubType);

        for (int i = 3; i <= 5; i++) {
            addSquare(map, Utils.addVec(pos, northSouth * i, 0, eastWest * i), random, 0, true, trunkDist, origin, treeOrigin, blockType, blockSubType);
            addSquare(map, Utils.addVec(pos, northSouth * i, -1 , eastWest * i), random, 0, true, trunkDist, origin, treeOrigin, blockType, blockSubType);
            addSquare(map, Utils.addVec(pos, northSouth * i, -2 , eastWest * i), random, 0, true, trunkDist, origin, treeOrigin, blockType, blockSubType);
        }

        addToMap(map, Utils.addVec(pos, northSouth*6, -1, eastWest*6), random, trunkDist, origin, treeOrigin, blockType, blockSubType);
        addToMap(map, Utils.addVec(pos, northSouth*6, -2, eastWest*6), random, trunkDist, origin, treeOrigin, blockType, blockSubType);
        addToMap(map, Utils.addVec(pos, northSouth*6, -3, eastWest*6), random, trunkDist, origin, treeOrigin, blockType, blockSubType);
        addToMap(map, Utils.addVec(pos, northSouth*6, -4, eastWest*6), random, trunkDist, origin, treeOrigin, blockType, blockSubType);

        addToMap(map, Utils.addVec(pos, northSouth*7, -2, eastWest*7), random, trunkDist, origin, treeOrigin, blockType, blockSubType);
        addToMap(map, Utils.addVec(pos, northSouth*7, -3, eastWest*7), random, trunkDist, origin, treeOrigin, blockType, blockSubType);
        addToMap(map, Utils.addVec(pos, northSouth*7, -4, eastWest*7), random, trunkDist, origin, treeOrigin, blockType, blockSubType);
        addToMap(map, Utils.addVec(pos, northSouth*7, -5, eastWest*7), random, trunkDist, origin, treeOrigin, blockType, blockSubType);
        addToMap(map, Utils.addVec(pos, northSouth*7, -6, eastWest*7), random, trunkDist, origin, treeOrigin, blockType, blockSubType);
    }
    
    public static Map<Vector3i, Vector2i> generateCanopy(Random random, Map<Vector3i, Vector2i> blocks, int x, int y, int z, int blockType, int blockSubType, int trunkHeight, Vector3i treeOrigin) {
        Vector3i origin = new Vector3i(x, y, z);
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());

        if (origin.y()-treeOrigin.y() > 12) {
            addSquare(map, origin.add(0, -1, 0), random, 1, true, blockType, blockSubType);
            addSquare(map, origin, random, 1, true, blockType, blockSubType);
            addSquare(map, origin.add(0, 1, 0), random, 1, true, blockType, blockSubType);
            int xTrunkDist = origin.x()-treeOrigin.x();
            int zTrunkDist = origin.z()-treeOrigin.z();
            int trunkDist = xTrunkDist*xTrunkDist+zTrunkDist*zTrunkDist;
            createFrond(map, origin.add(0, random.nextInt(0, 1), 0), random, trunkDist, origin, treeOrigin, 1, 0, blockType, blockSubType);
            createFrond(map, origin.add(0, random.nextInt(0, 1), 0), random, trunkDist, origin, treeOrigin, 0, 1, blockType, blockSubType);
            createFrond(map, origin.add(0, random.nextInt(0, 1), 0), random, trunkDist, origin, treeOrigin, -1, 0, blockType, blockSubType);
            createFrond(map, origin.add(0, random.nextInt(0, 1), 0), random, trunkDist, origin, treeOrigin, 0, -1, blockType, blockSubType);
            createFrond(map, origin.add(0, random.nextInt(0, 1), 0), random, trunkDist, origin, treeOrigin, 1, 1, blockType, blockSubType);
            createFrond(map, origin.add(0, random.nextInt(0, 1), 0), random, trunkDist, origin, treeOrigin, -1, 1, blockType, blockSubType);
            createFrond(map, origin.add(0, random.nextInt(0, 1), 0), random, trunkDist, origin, treeOrigin, -1, -1, blockType, blockSubType);
            createFrond(map, origin.add(0, random.nextInt(0, 1), 0), random, trunkDist, origin, treeOrigin, 1, -1, blockType, blockSubType);
        } else {
            x = origin.x();
            y = origin.y();
            z = origin.z();

            addToMap(map, new Vector3i(x, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 2), random, blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(x + 1, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 3, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 3, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 3), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 3), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z - 2), random, blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(x - 1, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 3), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 4), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 3), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 4), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 3, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 4, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 3, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 4, y, z), random, blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(x - 1, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 3), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 4), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 3), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 4), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 3, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 4, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 3, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 4, y, z), random, blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(x - 1, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 1, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z + 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 1, y, z - 1), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 4, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 4, y, z), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z + 4), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x, y, z - 4), random, blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(x - 2, y, z - 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x - 2, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z + 2), random, blockType, blockSubType);
            addToMap(map, new Vector3i(x + 2, y, z - 2), random, blockType, blockSubType);
        }

        return map;
    }

    private static void addSquare(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, int radius, boolean corners, int trunkDist, Vector3i origin, Vector3i treeOrigin, int blockType, int blockSubType) {
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
                    addToMap(map, new Vector3i(x, pos.y(), z), random, trunkDist, origin, treeOrigin, blockType, blockSubType);
                }
            }
        }
    }

    private static void addSquare(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, int radius, boolean corners, int blockType, int blockSubType) {
        int minX = pos.x()-radius;
        int maxX = pos.x()+radius;
        int minZ = pos.z()-radius;
        int maxZ = pos.z()+radius;
        for (int x = pos.x()-radius; x <= maxX; x++) {
            for (int z = pos.z()-radius; z <= maxZ; z++) {
                if (!((x == minX || x == maxX) && (z == minZ || z == maxZ)) || corners) {
                    addToMap(map, new Vector3i(x, pos.y(), z), random, blockType, blockSubType);
                }
            }
        }
    }
}