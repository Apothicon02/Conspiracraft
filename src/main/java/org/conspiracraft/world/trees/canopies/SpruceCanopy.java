package org.conspiracraft.world.trees.canopies;

import org.conspiracraft.world.BlockPos;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;

public class SpruceCanopy extends Canopy {
    public static Long2IntOpenHashMap generateCanopy(Random random, Long2IntOpenHashMap blocks, int x, int ogY, int z, int blockType, int blockSubType, int trunkHeight, long treeOrigin) {
        BlockPos origin = new BlockPos(x, ogY, z);

        int leaves = Chunk.packInts(blockType, blockSubType);
        float repeations = trunkHeight/8F;
        origin = origin.below((int) (trunkHeight-(repeations*2))+2);
        addSquare(blocks, origin, 1, true, leaves);
        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(blocks, origin, 2, false, leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(blocks, origin, 2, false, leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(blocks, origin, 1, true, leaves);
            addPlus(blocks, origin, 2, leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(blocks, origin, 1, true, leaves);
            blocks.put(LongPos.pack(origin.north(2)), leaves);
            blocks.put(LongPos.pack(origin.east(2)), leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(blocks, origin, 1, false, leaves);
            blocks.put(LongPos.pack(origin.north(2)), leaves);
            blocks.put(LongPos.pack(origin.east(2)), leaves);
            blocks.put(LongPos.pack(origin.north().east()), leaves);
            blocks.put(LongPos.pack(origin.north().west()), leaves);
            blocks.put(LongPos.pack(origin.south().east()), leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            addSquare(blocks, origin, 1, false, leaves);
            blocks.put(LongPos.pack(origin.north().east()), leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            blocks.put(LongPos.pack(origin), leaves);
            blocks.put(LongPos.pack(origin.north()), leaves);
            blocks.put(LongPos.pack(origin.east()), leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            blocks.put(LongPos.pack(origin), leaves);
        }

        for (float i = repeations; i > 0; i--) {
            origin = origin.above();
            blocks.put(LongPos.pack(origin), leaves);
        }

        return blocks;
    }

    private static void addSquare(Long2IntOpenHashMap map, BlockPos pos, int radius, boolean corners, int leaves) {
        int minX = pos.x()-radius;
        int maxX = pos.x()+radius;
        int minZ = pos.z()-radius;
        int maxZ = pos.z()+radius;
        for (int x = pos.x()-radius; x <= maxX; x++) {
            for (int z = pos.z()-radius; z <= maxZ; z++) {
                if (!((x == minX || x == maxX) && (z == minZ || z == maxZ)) || corners) {
                    map.put(LongPos.pack(x, pos.y(), z), leaves);
                }
            }
        }
    }

    private static void addPlus(Long2IntOpenHashMap map, BlockPos pos, int radius, int leaves) {
        map.put(LongPos.pack(pos.north(radius)), leaves);
        map.put(LongPos.pack(pos.east(radius)), leaves);
        map.put(LongPos.pack(pos.south(radius)), leaves);
        map.put(LongPos.pack(pos.west(radius)), leaves);
    }
}