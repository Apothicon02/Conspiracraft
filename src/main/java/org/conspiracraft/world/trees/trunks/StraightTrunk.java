package org.conspiracraft.world.trees.trunks;

import kotlin.Pair;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.LongPos;
import org.joml.Vector2i;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Set;

public class StraightTrunk extends Trunk {
    public static Pair<Long2IntOpenHashMap, Set<Long>> generateTrunk(int oX, int oY, int oZ, int trunkHeight, int blockType, int blockSubType) {
        Long2IntOpenHashMap map = new Long2IntOpenHashMap(trunkHeight);
        int maxHeight = oY+trunkHeight;
        for (int y = oY; y < maxHeight; y++) {
            map.put(LongPos.pack(oX, y, oZ), Chunk.packInts(blockType, blockSubType));
        }
        return new Pair<>(map, Set.of(LongPos.pack(oX, maxHeight, oZ)));
    }
}