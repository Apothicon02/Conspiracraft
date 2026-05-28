package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.world.BlockPos;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

public class SpruceCanopy extends Canopy {
    public static Map<Vector3i, Vector2i> generateCanopy(Random random, Map<Vector3i, Vector2i> blocks, int x, int ogY, int z, int blockType, int blockSubType, int trunkHeight, Vector3i treeOrigin) {
        BlockPos origin = new BlockPos(x, ogY, z);
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());

        Vector2i leaves = new Vector2i(blockType, blockSubType);
        float repeations = trunkHeight/8F;
        origin = origin.below((int) (trunkHeight-(repeations*2))+2);
        addSquare(map, origin, 1, true, leaves);
        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 2, false, leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 2, false, leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 1, true, leaves);
            addPlus(map, origin, 2, leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 1, true, leaves);
            map.put(origin.north(2), leaves);
            map.put(origin.east(2), leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 1, false, leaves);
            map.put(origin.north(2), leaves);
            map.put(origin.east(2), leaves);
            map.put(origin.north().east(), leaves);
            map.put(origin.north().west(), leaves);
            map.put(origin.south().east(), leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 1, false, leaves);
            map.put(origin.north().east(), leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            map.put(origin, leaves);
            map.put(origin.north(), leaves);
            map.put(origin.east(), leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            map.put(origin, leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            map.put(origin, leaves);
        }

        return map;
    }

    private static void addSquare(Map<Vector3i, Vector2i> map, BlockPos pos, int radius, boolean corners, Vector2i leaves) {
        int minX = pos.x()-radius;
        int maxX = pos.x()+radius;
        int minZ = pos.z()-radius;
        int maxZ = pos.z()+radius;
        for (int x = pos.x()-radius; x <= maxX; x++) {
            for (int z = pos.z()-radius; z <= maxZ; z++) {
                if (!((x == minX || x == maxX) && (z == minZ || z == maxZ)) || corners) {
                    map.put(new Vector3i(x, pos.y(), z), leaves);
                }
            }
        }
    }

    private static void addPlus(Map<Vector3i, Vector2i> map, BlockPos pos, int radius, Vector2i leaves) {
        map.put(pos.north(radius), leaves);
        map.put(pos.east(radius), leaves);
        map.put(pos.south(radius), leaves);
        map.put(pos.west(radius), leaves);
    }
}