package org.conspiracraft.game.world.trees;

import org.conspiracraft.game.world.trees.canopies.JungleCanopy;
import org.conspiracraft.game.world.trees.trunks.TwistingTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Set;

public class JungleTree {
    public static void generate(Vector2i blockOn, int x, int y, int z, int maxHeight, int radius, int logType, int logSubType, int leafType, int leafSubType, boolean overgrown) {
        if (blockOn.x == 2) {
            Set<Vector3i> canopies = TwistingTrunk.generateTrunk(x, y, z, maxHeight, logType, logSubType, overgrown, 1);
            for (Vector3i canopyPos : canopies) {
                JungleCanopy.generateCanopy(canopyPos.x, canopyPos.y, canopyPos.z, leafType, leafSubType, radius, 1);
            }
        }
    }
}
