package org.conspiracraft.game.world.trees;

import org.conspiracraft.game.world.trees.canopies.BlobCanopy;
import org.conspiracraft.game.world.trees.canopies.JungleCanopy;
import org.conspiracraft.game.world.trees.trunks.StraightTrunk;
import org.conspiracraft.game.world.trees.trunks.TwistingTrunk;
import org.joml.Vector3i;

import java.util.Set;

public class OakTree {
    public static void generate(int x, int y, int z, int maxHeight, int radius, int logType, int logSubType, int leafType, int leafSubType) {
        Set<Vector3i> canopies = StraightTrunk.generateTrunk(x, y, z, maxHeight, logType, logSubType);
        for (Vector3i canopyPos : canopies) {
            BlobCanopy.generateCanopy(canopyPos.x, canopyPos.y, canopyPos.z, leafType, leafSubType, radius);
        }
    }
}
