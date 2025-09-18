package org.conspiracraft.game.world.trees.canopies;

import org.conspiracraft.game.world.Directions;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

public class PineCanopy extends Canopy {

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, int blockType, int blockSubType) {
        map.put(pos, new Vector2i(blockType, blockSubType));
    }
    
    public static Map<Vector3i, Vector2i> generateCanopy(int x, int ogY, int z, int blockType, int blockSubType, int trunkHeight, Vector3i treeOrigin) {
        Vector3i origin = new Vector3i(x, ogY, z);
        Random random = new Random();
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        
        addToMap(map, origin, blockType, blockSubType);
        addToMap(map, new Vector3i(origin).add(0, 1, 0), blockType, blockSubType);
        addToMap(map, new Vector3i(origin).add(0, 2, 0), blockType, blockSubType);
        int max = ogY;
        int min = max-trunkHeight+2;
        for (int y = max; y >= min; y -= 3) {
            int dirOffset = random.nextInt(0, 2);
            Vector3i pos = new Vector3i(x, y, z);

            if (y <= min+trunkHeight/3 && trunkHeight > 10) {
                addLargeBranch(map, pos, random, getDir(Directions.NORTH, dirOffset), blockType, blockSubType);
                addLargeBranch(map, new Vector3i(pos).add(0, random.nextInt(0, 1), 0), random, getDir(Directions.EAST, dirOffset), blockType, blockSubType);
                addLargeBranch(map, new Vector3i(pos).add(0, random.nextInt(1, 2), 0), random, getDir(Directions.SOUTH, dirOffset), blockType, blockSubType);
                addLargeBranch(map, new Vector3i(pos).add(0, random.nextInt(0, 1), 0), random, getDir(Directions.WEST, dirOffset), blockType, blockSubType);
            } else if (y <= max-trunkHeight/5) {
                addMediumBranch(map, pos, random, getDir(Directions.NORTH, dirOffset), blockType, blockSubType);
                addMediumBranch(map, new Vector3i(pos).add(0, random.nextInt(0, 1), 0), random, getDir(Directions.EAST, dirOffset), blockType, blockSubType);
                addMediumBranch(map, new Vector3i(pos).add(0, random.nextInt(1, 2), 0), random, getDir(Directions.SOUTH, dirOffset), blockType, blockSubType);
                addSmallBranch(map, new Vector3i(pos).add(0, random.nextInt(0, 1), 0), random, getDir(Directions.WEST, dirOffset), blockType, blockSubType);
            } else {
                addSmallBranch(map, pos, random, getDir(Directions.NORTH, dirOffset), blockType, blockSubType);
                addSmallBranch(map, new Vector3i(pos).add(0, random.nextInt(1, 2), 0), random, getDir(Directions.SOUTH, dirOffset), blockType, blockSubType);
            }
        }

        return map;
    }

    private static Vector3i getDir(Vector3i dir, int offset) {
        if (offset == 1) {
            if (dir == Directions.NORTH) {
                return Directions.EAST;
            } else if (dir == Directions.EAST) {
                return Directions.SOUTH;
            } else if (dir == Directions.SOUTH) {
                return Directions.WEST;
            } else if (dir == Directions.WEST) {
                return Directions.NORTH;
            }
        } else if (offset == 2) {
            if (dir == Directions.NORTH) {
                return Directions.SOUTH;
            } else if (dir == Directions.EAST) {
                return Directions.WEST;
            } else if (dir == Directions.SOUTH) {
                return Directions.NORTH;
            } else if (dir == Directions.WEST) {
                return Directions.EAST;
            }
        }
        return dir;
    }

    private static void addLargeBranch(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, Vector3i dir, int blockType, int blockSubType) {
        if (random.nextInt(0, 4) == 0) {
            addMediumBranch(map, pos, random, dir, blockType, blockSubType);
        } else {
            int x;
            int y = 0;
            int z;
            if (dir == Directions.NORTH) {
                x = 1;
                z = 1;
            } else if (dir == Directions.EAST) {
                x = 1;
                z = -1;
            } else if (dir == Directions.SOUTH) {
                x = -1;
                z = -1;
            } else {
                x = -1;
                z = 1;
            }
            addToMap(map, new Vector3i(pos).add(x, y, z), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(0, y, z), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x, y, 0), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(0, y, z * 2), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, 0), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x, y, z * 2), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, z), blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(pos).add(x, y, z), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(0, y, z), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x, y, 0), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(0, y, z * 2), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, 0), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x, y, z * 2), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, z), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, z * 2), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x, y, z * 3), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 3, y, z), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, z * 3), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 3, y, z * 2), blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(pos).add(x * 2, y, z * 3), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 3, y, z * 2), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 3, y, z * 3), blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(pos).add(x * 3, y, z * 3), blockType, blockSubType);
        }
    }

    private static void addMediumBranch(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, Vector3i dir, int blockType, int blockSubType) {
        if (random.nextInt(0, 4) == 0) {
            addLargeBranch(map, pos, random, dir, blockType, blockSubType);
        } else {
            int x;
            int y = 0;
            int z;
            if (dir == Directions.NORTH) {
                x = 1;
                z = 1;
            } else if (dir == Directions.EAST) {
                x = 1;
                z = -1;
            } else if (dir == Directions.SOUTH) {
                x = -1;
                z = -1;
            } else {
                x = -1;
                z = 1;
            }
            addToMap(map, new Vector3i(pos).add(x, y, z), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(0, y, z), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x, y, 0), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(0, y, z * 2), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, 0), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x, y, z * 2), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, z), blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(pos).add(x, y, z * 2), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, z), blockType, blockSubType);
            addToMap(map, new Vector3i(pos).add(x * 2, y, z * 2), blockType, blockSubType);

            y--;

            addToMap(map, new Vector3i(pos).add(x * 2, y, z * 2), blockType, blockSubType);
        }
    }

    private static void addSmallBranch(Map<Vector3i, Vector2i> map, Vector3i pos, Random random, Vector3i dir, int blockType, int blockSubType) {
        int x;
        int y = 0;
        int z;
        if (dir == Directions.NORTH) {
            x = 1;
            z = 1;
        } else if (dir == Directions.EAST) {
            x = 1;
            z = -1;
        } else if (dir == Directions.SOUTH) {
            x = -1;
            z = -1;
        } else {
            x = -1;
            z = 1;
        }

        addToMap(map, new Vector3i(pos).add(x, y, 0), blockType, blockSubType);
        addToMap(map, new Vector3i(pos).add(0, y, z), blockType, blockSubType);
        addToMap(map, new Vector3i(pos).add(x, y, z), blockType, blockSubType);

        y--;

        addToMap(map, new Vector3i(pos).add(x, y, z), blockType, blockSubType);
    }
}