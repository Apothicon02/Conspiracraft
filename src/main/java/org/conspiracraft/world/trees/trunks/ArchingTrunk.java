package org.conspiracraft.world.trees.trunks;

import kotlin.Pair;
import org.conspiracraft.utils.Utils;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ArchingTrunk extends Trunk {
    private static void addToMap(Map<Vector3i, Vector2i> map, Vector3i pos, Vector2i block) {
        map.put(pos, block);
    }
    
    public static Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generateTrunk(Random random, int oX, int oY, int oZ, int count, int minTrunkHeight, int maxTrunkHeight, int blockType, int blockSubType, int halfLeafRadius) {
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
            int actualHeight = minTrunkHeight < maxTrunkHeight ? random.nextInt(minTrunkHeight, maxTrunkHeight) : maxTrunkHeight;
            int maxHeight = origin.y()+actualHeight;
            if (maxHeight > highestHeight) {
                highestHeight = maxHeight;
            }
            for (int height = origin.y()-1; height <= maxHeight; height++) {
                float bendFactor = ((float) maxHeight /height)*2F;
                Vector3i pos = new Vector3i(offsetX, height, offsetZ);
                addToMap(map, pos, wood);
                double heightFactor = Math.pow((((float)height / maxHeight)-0.4f)*2, 5);
                if (heightFactor < 0.1f) {
                    addToMap(map, new Vector3i(pos).add(1, 0, 0), wood);
                    addToMap(map, new Vector3i(pos).add(0, 0, 1), wood);
                    addToMap(map, new Vector3i(pos).add(1, 0, 1), wood);
                }
                if (height == maxHeight) {
                    canopies.add(Utils.addVec(pos, 0, 1, 0));
                } else if (height < maxHeight-4 && random.nextFloat() < heightFactor) {
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
                    if (heightFactor > 0.66f && random.nextFloat() < 0.25f) {
                        height--;
                        if (random.nextFloat() < 0.125f) {
                            for (int y = height+1; y < height+halfLeafRadius; y++) {
                                addToMap(map, new Vector3i(offsetX, y, offsetZ), wood);
                                addToMap(map, new Vector3i(offsetX+1, y, offsetZ+1), wood);
                            }
                            canopies.add(Utils.addVec(pos, 0, halfLeafRadius, 0));
                        }
                    } else if (heightFactor > 0.25f && random.nextBoolean()) {
                        height--;
                    }
                    pos = new Vector3i(offsetX, height, offsetZ);
                    addToMap(map, pos, wood);
                }
            }
        }
        return new Pair<>(map, canopies);
    }
}