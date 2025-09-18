package org.conspiracraft.game.world.trees;

import kotlin.Pair;
import org.conspiracraft.game.world.FeatureHelper;
import org.conspiracraft.game.world.WorldGen;
import org.conspiracraft.game.world.trees.canopies.PalmCanopy;
import org.conspiracraft.game.world.trees.trunks.BendingTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.conspiracraft.engine.Utils.condensePos;
import static org.conspiracraft.game.world.World.heightmap;
import static org.conspiracraft.game.world.WorldGen.setLightWorldgen;

public class PalmShrub {
    public static void generate(Vector2i blockOn, int x, int y, int z, int maxHeight, int logType, int logSubType, int leafType, int leafSubType) {
        if (blockOn.x == 2) {
            Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generatedTrunk = BendingTrunk.generateTrunk(new Random(), x, y, z, true, 0, maxHeight, logType, logSubType);
            Map<Vector3i, Vector2i> blocks = new HashMap<>(generatedTrunk.getFirst());
            for (Vector3i canopyPos : generatedTrunk.getSecond()) {
                Map<Vector3i, Vector2i> canopy = PalmCanopy.generateCanopy(canopyPos.x, canopyPos.y, canopyPos.z, leafType, leafSubType, maxHeight, new Vector3i(x, y, z));
                blocks.putAll(canopy);
            }
            blocks.forEach((pos, block) -> {
                if (FeatureHelper.inBounds(pos)) {
                    if (block.y > 0) {
                        WorldGen.setBlockWorldgen(pos.x, pos.y, pos.z, block.x, block.y);
                    } else {
                        WorldGen.setBlockWorldgenUpdates(pos.x, pos.y, pos.z, block.x, block.y);
                    }
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
