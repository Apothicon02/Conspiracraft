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

        for (Vector3i nPos : neighborPositions) {
            Vector2i nFluid = World.getBlock(nPos);
            BlockType nType = BlockTypes.blockTypeMap.get(nFluid.x);
            boolean areBothFluid = nFluid.x == fluid.x && nFluid.y != fluid.y;
            boolean isMainFluid = areBothFluid ? true : (nType.isFluidReplaceable && type.isFluid);
            boolean isNFluid = areBothFluid ? true : (type.isFluidReplaceable && nType.isFluid);
            if (isMainFluid || isNFluid) {
                int difference = (int)(Math.floor((nType.isFluidReplaceable ? fluid.y : nFluid.y-fluid.y)/2.f));
                if (Math.abs(difference) > 0) {
                    scheduleTick = true;
                    if (!areBothFluid) {
                        if (isMainFluid) {
                            nFluid.x = fluid.x;
                        } else {
                            fluid.x = nFluid.x;
                        }
                    }
                    fluid.y -= difference;
                    nFluid.y += difference;

                    World.setBlock(nPos.x, nPos.y, nPos.z, nFluid.x, nFluid.y, true, false, true); //unnecessary
                }
            }
        }

        if (scheduleTick) {
            World.setBlock(pos.x, pos.y, pos.z, fluid.x, fluid.y, true, false, true); //maybe
            if (org.joml.Math.abs(pos.x - Main.player.pos.x) + org.joml.Math.abs(pos.y - Main.player.pos.y) + org.joml.Math.abs(pos.z - Main.player.pos.z) < 32) {
                Main.player.waterFlow = (float) org.joml.Math.max(Main.player.waterFlow, Math.min(0, Utils.distance(pos.x, pos.y, pos.z, Main.player.pos.x, Main.player.pos.y, Main.player.pos.z)-16)*-1);
            }
        }
    }
}