package org.conspiracraft.game.world;

import org.conspiracraft.Main;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.ScheduledTicker;
import org.conspiracraft.game.blocks.Tags;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.joml.Math;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Collections;

public class FluidHelper {
    private static final Vector3i[] neighborPositions = new Vector3i[4];
    public static void updateFluid(Vector3i pos, Vector2i fluid) {
        double random = Math.random();
        if (random < 0.25f) {
            neighborPositions[0] = new Vector3i(pos.x, pos.y, pos.z+1);
            neighborPositions[1] = new Vector3i(pos.x+1, pos.y, pos.z);
            neighborPositions[2] = new Vector3i(pos.x, pos.y, pos.z-1);
            neighborPositions[3] = new Vector3i(pos.x-1, pos.y, pos.z);
        } else if (random < 0.5f) {
            neighborPositions[0] = new Vector3i(pos.x, pos.y, pos.z-1);
            neighborPositions[1] = new Vector3i(pos.x+1, pos.y, pos.z);
            neighborPositions[2] = new Vector3i(pos.x, pos.y, pos.z+1);
            neighborPositions[3] = new Vector3i(pos.x-1, pos.y, pos.z);
        } else if (random < 0.75f) {
            neighborPositions[0] = new Vector3i(pos.x+1, pos.y, pos.z);
            neighborPositions[1] = new Vector3i(pos.x, pos.y, pos.z+1);
            neighborPositions[2] = new Vector3i(pos.x, pos.y, pos.z-1);
            neighborPositions[3] = new Vector3i(pos.x-1, pos.y, pos.z);
        } else {
            neighborPositions[0] = new Vector3i(pos.x-1, pos.y, pos.z);
            neighborPositions[1] = new Vector3i(pos.x+1, pos.y, pos.z);
            neighborPositions[2] = new Vector3i(pos.x, pos.y, pos.z-1);
            neighborPositions[3] = new Vector3i(pos.x, pos.y, pos.z+1);
        }
        BlockType type = BlockTypes.blockTypeMap.get(fluid.x);
        boolean scheduleTick = false;
        int maxChange = 0;

        if (type.blockProperties.isFluid) {
            Vector3i bPos = new Vector3i(pos.x, pos.y-1, pos.z);
            Vector2i bFluid = World.getBlock(bPos);
            if (Tags.soakers.tagged.contains(bFluid.x)) {
                maxChange = fluid.y;
                fluid.x = 0;
                fluid.y = 0;
                scheduleTick = true;
            } else if (Tags.crystals.tagged.contains(bFluid.x) && fluid.x == 1 && fluid.y >= 15) {
                maxChange = fluid.y;
                fluid.x = 7;
                fluid.y = 0;
                scheduleTick = true;
            } else {
                BlockType bType = BlockTypes.blockTypeMap.get(bFluid.x);
                boolean fluidReplacable = bType.blockProperties.isFluidReplaceable;
                int room = fluidReplacable ? 15 : 15 - bFluid.y;
                if ((bType.blockProperties.isFluid && bFluid.x == fluid.x && room > 0) || fluidReplacable) {
                    bFluid.x = fluid.x;
                    int flow = Math.min(room, fluid.y);
                    if (fluidReplacable) {
                        bFluid.y = flow;
                    } else {
                        bFluid.y += flow;
                    }
                    fluid.y -= flow;
                    if (fluid.y < 1) {
                        fluid.x = 0;
                    }

                    maxChange = flow;
                    scheduleTick = true;
                    World.setBlock(bPos.x, bPos.y, bPos.z, bFluid.x, bFluid.y, true, false, 4, true);
                }
            }
        }

        Vector3i aPos = new Vector3i(pos.x, pos.y+1, pos.z);
        Vector2i aFluid = World.getBlock(aPos);
        BlockType aType = BlockTypes.blockTypeMap.get(aFluid.x);
        boolean fluidReplacable = type.blockProperties.isFluidReplaceable;
        int room = fluidReplacable ? 15 : 15-fluid.y;
        if (Tags.crystals.tagged.contains(aFluid.x) && fluid.x == 1 && fluid.y >= 15) {
            maxChange = fluid.y;
            fluid.x = 7;
            fluid.y = 0;
            scheduleTick = true;
        } else if ((aType.blockProperties.isFluid && (aFluid.x == fluid.x || type.blockProperties.isFluidReplaceable) && room > 0)) {
            fluid.x = aFluid.x;
            int flow = Math.min(room, aFluid.y);
            if (fluidReplacable) {
                fluid.y = flow;
            } else {
                fluid.y += flow;
            }
            aFluid.y -= flow;
            if (aFluid.y < 1) {
                aFluid.x = 0;
            }

            maxChange = flow;
            scheduleTick = true;
            World.setBlock(aPos.x, aPos.y, aPos.z, aFluid.x, aFluid.y, true, false, 4, true);
        }

        for (Vector3i nPos : neighborPositions) {
            Vector2i nFluid = World.getBlock(nPos);
            BlockType nType = BlockTypes.blockTypeMap.get(nFluid.x);
            boolean areBothFluid = nFluid.x == fluid.x && nFluid.y != fluid.y && nType.blockProperties.isFluid;
            boolean isMainFluid = areBothFluid ? true : (nType.blockProperties.isFluidReplaceable && type.blockProperties.isFluid);
            boolean isNFluid = areBothFluid ? true : (type.blockProperties.isFluidReplaceable && nType.blockProperties.isFluid);
            if (Tags.crystals.tagged.contains(nFluid.x) && fluid.x == 1 && fluid.y >= 15) {
                maxChange = fluid.y;
                fluid.x = 7;
                fluid.y = 0;
                scheduleTick = true;
            } else if (isMainFluid || isNFluid) {
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
                    fluid.y = newLevel / 2;
                    nFluid.y = newLevel / 2;

                    if (newLevel % 2 != 0) { //if odd, neighbor gets extra fluid.
                        nFluid.y++;
                    }

                    if (prevFluidLevel != fluid.y || prevNFluidLevel != nFluid.y) { //only update if something changed.
                        maxChange = Math.max(maxChange, Math.max(Math.abs(prevFluidLevel - fluid.y), Math.abs(prevNFluidLevel - nFluid.y)));
                        scheduleTick = true;
                        World.setBlock(nPos.x, nPos.y, nPos.z, nFluid.x, nFluid.y, true, false, 4, true);
                    }
                }
            }
        }

        if (scheduleTick) {
            World.setBlock(pos.x, pos.y, pos.z, fluid.x, fluid.y, true, false, 4, true); //maxChange < 2
        }
    }
}