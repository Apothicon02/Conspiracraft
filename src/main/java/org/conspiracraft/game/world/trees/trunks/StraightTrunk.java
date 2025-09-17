package org.conspiracraft.game.world.trees.trunks;

import kotlin.Pair;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Map;
import java.util.Set;

public class StraightTrunk extends Trunk {
    public static Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generateTrunk(int oX, int oY, int oZ, int trunkHeight, int blockType, int blockSubType) {
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        int maxHeight = oY+trunkHeight;
        for (int y = oY; y < maxHeight; y++) {
            map.put(new Vector3i(oX, y, oZ), new Vector2i(blockType, blockSubType));
        }
        return new Pair<>(map, Set.of(new Vector3i(oX, maxHeight, oZ)));
    }
}