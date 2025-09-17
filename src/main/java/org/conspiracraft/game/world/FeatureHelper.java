package org.conspiracraft.game.world;

import org.joml.Vector3i;

import static org.conspiracraft.game.world.World.height;
import static org.conspiracraft.game.world.World.size;

public class FeatureHelper {
    public static boolean inBounds(Vector3i pos) {
        return (pos.x >= 0 && pos.y >= 0 && pos.z >= 0 && pos.x < size && pos.y < height && pos.z < size);
    }
}
