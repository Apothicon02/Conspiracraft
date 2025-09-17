package org.conspiracraft.game.world.trees.trunks;

import kotlin.Pair;
import org.conspiracraft.engine.ConspiracraftMath;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class BendingTrunk extends Trunk {
    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, Vector2i block) {
        map.put(pos, block);
    }
    
    public static Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generateTrunk(Random random, int oX, int oY, int oZ, boolean crown, int count, int trunkHeight, int blockType, int blockSubType) {
        Vector3i origin = new Vector3i(oX, oY, oZ);
        Vector2i wood = new Vector2i(blockType, blockSubType);
        Map<Vector3i, Vector2i> map = new java.util.HashMap<>(Map.of());
        Set<Vector3i> canopies = new HashSet<>();
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
            int maxHeight = origin.y()+trunkHeight;
            if (maxHeight > highestHeight) {
                highestHeight = maxHeight;
            }
            for (int height = origin.y()-1; height <= maxHeight; height++) {
                float bendFactor = ((float) maxHeight /height)*2F;
                Vector3i pos = new Vector3i(offsetX, height, offsetZ);
                addToMap(map, pos, wood);
                if (height == maxHeight) {
                    canopies.add(ConspiracraftMath.addVec(pos, 0, 1, 0));
                    if (crown && height > 12) {
                        addToMap(map, ConspiracraftMath.addVec(pos, 2, 0, 0), new Vector2i(3, 0));
                        addToMap(map, ConspiracraftMath.addVec(pos, 1, -1, 0), new Vector2i(3, 0));
                        addToMap(map, ConspiracraftMath.addVec(pos, 0, 0, 2), new Vector2i(3, 0));
                        addToMap(map, ConspiracraftMath.addVec(pos, 0, -1, 2), new Vector2i(3, 0));
                        addToMap(map, ConspiracraftMath.addVec(pos, -2, 0, 0), new Vector2i(3, 0));
                        addToMap(map, ConspiracraftMath.addVec(pos, -1, -1, 0), new Vector2i(3, 0));
                        addToMap(map, ConspiracraftMath.addVec(pos, 0, 0, -2), new Vector2i(3, 0));
                        addToMap(map, ConspiracraftMath.addVec(pos, 0, -1, -2), new Vector2i(3, 0));
                    }
                } else if (height < maxHeight-4) {
                    if (trunks == 0) {
                        if (random.nextInt(0, 5) < 3-bendFactor) {
                            offsetX += 1;
                        }
                        if (random.nextInt(0, 5) < 3-bendFactor) {
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
                    addToMap(map, pos, wood);
                }
            }
        }
        return new Pair<>(map, canopies);
    }
}