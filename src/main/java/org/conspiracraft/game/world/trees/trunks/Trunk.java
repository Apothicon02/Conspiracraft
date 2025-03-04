package org.conspiracraft.game.world.trees.trunks;

import org.joml.Vector3i;

import java.util.Set;

public abstract class Trunk {
    public static Set<Vector3i> generateTrunk(int oX, int oY, int oZ, int trunkHeight, int blockType, int blockSubType) {
        return Set.of(new Vector3i(oX, oY, oZ));
    }
}
