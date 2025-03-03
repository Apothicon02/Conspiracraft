package org.conspiracraft.game.world.trees.trunks;

import org.joml.Vector3i;

import java.util.List;

public abstract class Trunk {
    public static List<Vector3i> generateTrunk(int oX, int oY, int oZ, int trunkHeight,int blockType, int blockSubType) {
        return List.of(new Vector3i(oX, oY, oZ));
    }
}
