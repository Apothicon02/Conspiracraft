package org.conspiracraft.world.trees.trunks;

import kotlin.Pair;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.BlockPos;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ThickTrunk extends Trunk {
    public static Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generateTrunk(Random random, int oX, int oY, int oZ, int trunkHeight, boolean singleCanopy, int branchChance, int blockType, int blockSubType) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        Set<Vector3i> canopies = new HashSet<>();

        int baseRadius = 1;
        BlockPos pos = new BlockPos(oX, oY-4, oZ);
        int trunkOffset = (baseRadius*3)+3;
        int min = (int) (trunkHeight*0.2) + trunkOffset;
        int max = (int) (trunkHeight*0.27) + trunkOffset;
        makeColumn(random, map, pos.north(baseRadius+1), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.east(baseRadius+1), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.south(baseRadius+1), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.west(baseRadius+1), min, max, blockType, blockSubType);
        min = (int) (trunkHeight*0.08) + trunkOffset;
        max = (int) (trunkHeight*0.13) + trunkOffset;
        makeColumn(random, map, pos.north(baseRadius+1).east(), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.north(baseRadius+1).west(), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.east(baseRadius+1).north(), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.east(baseRadius+1).south(), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.south(baseRadius+1).east(), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.south(baseRadius+1).west(), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.west(baseRadius+1).north(), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.west(baseRadius+1).south(), min, max, blockType, blockSubType);
        min = (int) (trunkHeight*0.03) + trunkOffset;
        max = (int) (trunkHeight*0.05) + trunkOffset;
        makeColumn(random, map, pos.north(baseRadius+1).east(2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.north(baseRadius+1).west(2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.east(baseRadius+1).north(2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.east(baseRadius+1).south(2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.south(baseRadius+1).east(2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.south(baseRadius+1).west(2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.west(baseRadius+1).north(2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.west(baseRadius+1).south(2), min, max, blockType, blockSubType);
        min = (int) (trunkHeight*0.06) + trunkOffset;
        max = (int) (trunkHeight*0.08) + trunkOffset;
        makeColumn(random, map, pos.north(baseRadius+2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.east(baseRadius+2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.south(baseRadius+2), min, max, blockType, blockSubType);
        makeColumn(random, map, pos.west(baseRadius+2), min, max, blockType, blockSubType);
        pos.add(0, 3, 0);

        int offset = random.nextInt(-4, 4);
        for (int i = 0; i <= trunkHeight; i++) {
            pos.add(0, 1, 0);
            int currentHeight = i+offset;
            if (currentHeight >= trunkHeight/1.25) {
                if (baseRadius > 1) {
                    makeSquare(map, pos.immutable(), baseRadius-1, false, blockType, blockSubType);
                } else {
                    map.put(pos.immutable(), new Vector2i(blockType, blockSubType));
                }
                if (!singleCanopy && i < trunkHeight-1 && random.nextInt(0, 10) < branchChance) {
                    canopies.add(makeBranch(random, map, pos, random.nextInt(1, 2)+baseRadius, blockType, blockSubType));
                }
            } else if (currentHeight >= trunkHeight/1.75) {
                makeSquare(map, pos.immutable(), baseRadius, false, blockType, blockSubType);
                if (!singleCanopy && random.nextInt(0, 10) < branchChance) {
                    canopies.add(makeBranch(random, map, pos, random.nextInt(1, 2)+baseRadius, blockType, blockSubType));
                }
            } else {
                makeSquare(map, pos.immutable(), baseRadius, true, blockType, blockSubType);
                double actualTrunkHeight = Utils.gradient(pos.y(), oY+(trunkHeight/4), oY+trunkHeight, 3, 1);
                if (!singleCanopy && actualTrunkHeight != 1 && random.nextInt(0, 10) < actualTrunkHeight) {
                    canopies.add(makeBranch(random, map, pos, random.nextInt(1, 2)+baseRadius, blockType, blockSubType));
                }
            }
        }
        canopies.add(new Vector3i(oX, oY+trunkHeight+1, oZ));
        
        return new Pair<>(map, canopies);
    }

    private static void makeColumn(Random random, Map<Vector3i, Vector2i> map, Vector3i pos, int minHeight, int maxHeight, int blockType, int blockSubType) {
        minHeight--;
        if (minHeight >= maxHeight) {
            minHeight = maxHeight-1;
        }
        Vector3i newPos = new Vector3i(pos);
        for (int y = 0; y <= random.nextInt(minHeight, maxHeight); y++) {
            newPos.add(0, 1, 0);
            map.put(new Vector3i(newPos), new Vector2i(blockType, blockSubType));
        }
    }

    private static BlockPos makeBranch(Random random, Map<Vector3i, Vector2i> map, BlockPos pos, int length, int blockType, int blockSubType) {
        BlockPos dir = new BlockPos(random.nextBoolean() ? 1 : -1, 0, random.nextBoolean() ? 1 : -1);
        for (int i = 1; i <= length; i++) {
            BlockPos newPos = new BlockPos(pos.x()+(dir.x()*i), pos.y(), pos.z()+(dir.z()*i));
            map.put(newPos, new Vector2i(blockType, blockSubType));
            if (i == length) {
                return newPos.above();
            }
        }
        return pos;
    }

    private static void makeSquare(Map<Vector3i, Vector2i> map, Vector3i pos, int radius, boolean corners, int blockType, int blockSubType) {
        int minX = pos.x()-radius;
        int maxX = pos.x()+radius;
        int minZ = pos.z()-radius;
        int maxZ = pos.z()+radius;
        for (int x = pos.x()-radius; x <= maxX; x++) {
            for (int z = pos.z()-radius; z <= maxZ; z++) {
                if (!((x == minX || x == maxX) && (z == minZ || z == maxZ)) || corners) {
                    BlockPos newPos = new BlockPos(x, pos.y(), z);
                    map.put(newPos, new Vector2i(blockType, blockSubType));
                }
            }
        }
    }
}