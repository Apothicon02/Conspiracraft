package org.conspiracraft.world.trees.trunks;

import kotlin.Pair;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Random;
import java.util.Set;

public class BendingTrunk extends Trunk {
    private static void addToMap(Long2IntOpenHashMap map, long pos, int block) {
        map.put(pos, block);
    }
    
    public static Pair<Long2IntOpenHashMap, Set<Long>> generateTrunk(Random random, int oX, int oY, int oZ, boolean crown, int count, int minTrunkHeight, int maxTrunkHeight, int blockType, int blockSubType) {
        Vector3i origin = new Vector3i(oX, oY, oZ);
        int wood = Chunk.packInts(blockType, blockSubType);
        Long2IntOpenHashMap map = new Long2IntOpenHashMap(maxTrunkHeight*4);
        Set<Long> canopies = new HashSet<>();
        int extra = random.nextInt(0, 10);
        if (extra >= 10) {
            extra = 1;
        } else if (extra >= 6) {
            extra = 0;
        } else {
            extra = -1;
        }
        if (count > 0) {
            extra = count;
        }
        int highestHeight = 0;

        for (int trunks = 0; trunks <= extra+1; trunks++) {
            int offsetX = origin.x();
            int offsetZ = origin.z();
            int maxHeight = origin.y()+(minTrunkHeight < maxTrunkHeight ? random.nextInt(minTrunkHeight, maxTrunkHeight) : maxTrunkHeight);
            if (maxHeight > highestHeight) {
                highestHeight = maxHeight;
            }
            for (int height = origin.y()-1; height <= maxHeight; height++) {
                float bendFactor = ((float) maxHeight /height)*2F;
                Vector3i pos = new Vector3i(offsetX, height, offsetZ);
                addToMap(map, LongPos.pack(pos), wood);
                if (height == maxHeight) {
                    canopies.add(LongPos.pack(pos, 0, 1, 0));
                    if (crown && height > 12) { //coconuts
                        addToMap(map, LongPos.pack(pos, 2, 0, 0), Chunk.packInts(BlockTypes.DEAD_LEAVES.id, 0));
                        addToMap(map, LongPos.pack(pos, 1, -1, 0), Chunk.packInts(BlockTypes.DEAD_LEAVES.id, 0));
                        addToMap(map, LongPos.pack(pos, 0, 0, 2), Chunk.packInts(BlockTypes.DEAD_LEAVES.id, 0));
                        addToMap(map, LongPos.pack(pos, 0, -1, 2), Chunk.packInts(BlockTypes.DEAD_LEAVES.id, 0));
                        addToMap(map, LongPos.pack(pos, -2, 0, 0), Chunk.packInts(BlockTypes.DEAD_LEAVES.id, 0));
                        addToMap(map, LongPos.pack(pos, -1, -1, 0), Chunk.packInts(BlockTypes.DEAD_LEAVES.id, 0));
                        addToMap(map, LongPos.pack(pos, 0, 0, -2), Chunk.packInts(BlockTypes.DEAD_LEAVES.id, 0));
                        addToMap(map, LongPos.pack(pos, 0, -1, -2), Chunk.packInts(BlockTypes.DEAD_LEAVES.id, 0));
                    }
                } else if (height < maxHeight-4) {
                    if (trunks == 0 || trunks == 4) {
                        if (random.nextInt(0, 5) < 3-bendFactor) {
                            offsetX += 1;
                        }
                        if (random.nextInt(0, 5) < 3-bendFactor) {
                            offsetZ += 1;
                        }
                    } else if (trunks == 1 || trunks == 5) {
                        if (random.nextInt(0, 5) < 4-bendFactor) {
                            offsetX += 1;
                        }
                        if (random.nextInt(0, 5) < 4-bendFactor) {
                            offsetZ -= 1;
                        }
                    } else if (trunks == 2 || trunks == 6) {
                        if (random.nextInt(0, 5) < 4-bendFactor) {
                            offsetX -= 1;
                        }
                        if (random.nextInt(0, 5) < 4-bendFactor) {
                            offsetZ += 1;
                        }
                    } else {
                        if (random.nextInt(0, 5) < 4-bendFactor) {
                            offsetX -= 1;
                        }
                        if (random.nextInt(0, 5) < 4-bendFactor) {
                            offsetZ -= 1;
                        }
                    }
                    pos = new Vector3i(offsetX, height, offsetZ);
                    addToMap(map, LongPos.pack(pos), wood);
                }
            }
        }
        return new Pair<>(map, canopies);
    }
}