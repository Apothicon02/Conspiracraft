package org.conspiracraft.game.world.trees.trunks;

import org.joml.Vector3i;

import java.util.List;

import static org.conspiracraft.game.world.WorldGen.*;

public class StraightTrunk extends Trunk {
    public static List<Vector3i> generateTrunk(int oX, int oY, int oZ, int trunkHeight, int blockType, int blockSubType) {
        int maxHeight = oY+trunkHeight;
        for (int y = oY; y < maxHeight; y++) {
            setBlockWorldgen(oX, y, oZ, blockType, blockSubType);
        }
        return List.of(new Vector3i(oX, maxHeight, oZ));
    }
}