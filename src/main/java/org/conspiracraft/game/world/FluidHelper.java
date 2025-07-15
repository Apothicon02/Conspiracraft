package org.conspiracraft.game.world;

import org.conspiracraft.Main;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.ScheduledTicker;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.joml.Math;
import org.joml.Vector2i;
import org.joml.Vector3i;

public class FluidHelper {
    private static final Vector3i[] neighborPositions = new Vector3i[4];
    public static void updateFluid(Vector3i pos, Vector2i fluid) {
        neighborPositions[0] = new Vector3i(pos.x, pos.y, pos.z+1);
        neighborPositions[1] = new Vector3i(pos.x+1, pos.y, pos.z);
        neighborPositions[2] = new Vector3i(pos.x, pos.y, pos.z-1);
        neighborPositions[3] = new Vector3i(pos.x-1, pos.y, pos.z);
        BlockType type = BlockTypes.blockTypeMap.get(fluid.x);
        boolean scheduleTick = false;
        int maxChange = 0;

        for (Vector3i nPos : neighborPositions) {
            Vector2i nFluid = World.getBlock(nPos);
            BlockType nType = BlockTypes.blockTypeMap.get(nFluid.x);
            boolean areBothFluid = nFluid.x == fluid.x && nFluid.y != fluid.y;
            boolean isMainFluid = areBothFluid ? true : (nType.blockProperties.isFluidReplaceable && type.blockProperties.isFluid);
            boolean isNFluid = areBothFluid ? true : (type.blockProperties.isFluidReplaceable && nType.blockProperties.isFluid);
            if (isMainFluid || isNFluid) {
                int newLevel = areBothFluid ? (fluid.y + nFluid.y) : (isMainFluid ? (fluid.y) : (nFluid.y));
                if (newLevel > 1) {
                    int prevFluidLevel = fluid.y;
                    int prevNFluidLevel = nFluid.y;
                    if (!areBothFluid) {
                        if (isMainFluid) {
                            prevNFluidLevel = 0;
                            nFluid.x = fluid.x;
                        } else {
                            prevFluidLevel = 0;
                            fluid.x = nFluid.x;
                        }
                    }
                    fluid.y = newLevel/2;
                    nFluid.y = newLevel/2;

                    if (newLevel % 2 != 0) { //if odd, neighbor gets extra fluid.
                        nFluid.y++;
                    }

                    if (prevFluidLevel != fluid.y || prevNFluidLevel != nFluid.y) { //only update if something changed.
                        maxChange = Math.max(maxChange, Math.max(Math.abs(prevFluidLevel-fluid.y), Math.abs(prevNFluidLevel-nFluid.y)));
                        scheduleTick = true;
                        World.setBlock(nPos.x, nPos.y, nPos.z, nFluid.x, nFluid.y, true, false, 4, true);
                    }
                }
            }
        }

        if (scheduleTick) {
            World.setBlock(pos.x, pos.y, pos.z, fluid.x, fluid.y, true, false, 4, maxChange < 2);
        }
    }
}