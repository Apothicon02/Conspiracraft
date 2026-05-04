package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.world.BlockPos;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Random;

public class SpruceCanopy extends Canopy {
    private static Vector2i leaves = new Vector2i(0);

    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos) {
        map.put(pos, leaves);
    }
    
    public static Map<Vector3i, Vector2i> generateCanopy(Random random, Map<Vector3i, Vector2i> blocks, int x, int ogY, int z, int blockType, int blockSubType, int trunkHeight, Vector3i treeOrigin) {
        BlockPos origin = new BlockPos(x, ogY, z);
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());

        leaves = new Vector2i(blockType, blockSubType);
        float repeations = trunkHeight/8F;
        origin = origin.below((int) (trunkHeight-(repeations*2))+2);
        addSquare(map, origin, 1, true);
        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 2, false);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 2, false);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 1, true);
            addPlus(map, origin, 2);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 1, true);
            addToMap(map, origin.north(2));
            addToMap(map, origin.east(2));
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 1, false);
            addToMap(map, origin.north(2));
            addToMap(map, origin.east(2));
            addToMap(map, origin.north().east());
            addToMap(map, origin.north().west());
            addToMap(map, origin.south().east());
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(map, origin, 1, false);
            addToMap(map, origin.north().east());
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addToMap(map, origin);
            addToMap(map, origin.north());
            addToMap(map, origin.east());
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addToMap(map, origin);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addToMap(map, origin);
        }

        return map;
    }

    private static void addSquare(Map<Vector3i, Vector2i> map, BlockPos pos, int radius, boolean corners) {
        int minX = pos.x()-radius;
        int maxX = pos.x()+radius;
        int minZ = pos.z()-radius;
        int maxZ = pos.z()+radius;
        for (int x = pos.x()-radius; x <= maxX; x++) {
            for (int z = pos.z()-radius; z <= maxZ; z++) {
                if (!((x == minX || x == maxX) && (z == minZ || z == maxZ)) || corners) {
                    addToMap(map, new Vector3i(x, pos.y(), z));
                }
            }
        }
    }

    private static void addPlus(Map<Vector3i, Vector2i> map, BlockPos pos, int radius) {
        addToMap(map, pos.north(radius));
        addToMap(map, pos.east(radius));
        addToMap(map, pos.south(radius));
        addToMap(map, pos.west(radius));
    }
}