package org.conspiracraft.game.world.trees;

import kotlin.Pair;
import org.conspiracraft.game.world.FeatureHelper;
import org.conspiracraft.game.world.WorldGen;
import org.conspiracraft.game.world.trees.canopies.BlobCanopy;
import org.conspiracraft.game.world.trees.trunks.TwistingTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.conspiracraft.engine.Utils.condensePos;
import static org.conspiracraft.game.world.World.heightmap;
import static org.conspiracraft.game.world.WorldGen.setLightWorldgen;

public class DeadOakTree {
    public static void generate(Vector2i blockOn, int x, int y, int z, int maxHeight, int logType, int logSubType) {
        if (blockOn.x == 2 || blockOn.x == 23) {
            Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generatedTrunk = TwistingTrunk.generateTrunk(x, y, z, maxHeight, logType, logSubType, true, 8);
            Map<Vector3i, Vector2i> blocks = new HashMap<>(generatedTrunk.getFirst());
            blocks.forEach((pos, block) -> {
                if (FeatureHelper.inBounds(pos)) {
                    WorldGen.setBlockWorldgen(pos.x, pos.y, pos.z, block.x, block.y);
                    int condensedPos = condensePos(pos.x, pos.z);
                    int surfaceY = heightmap[condensedPos];
                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], pos.y - 1);
                    for (int extraY = pos.y - 1; extraY >= surfaceY; extraY--) {
                        setLightWorldgen(pos.x, extraY, pos.z, new Vector4i(0, 0, 0, 0));
                    }
                }
            });
        }
    }
}
