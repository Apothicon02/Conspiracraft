package org.conspiracraft.game.world;

import org.conspiracraft.Main;
import org.conspiracraft.game.ScheduledTicker;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.joml.Vector2i;
import org.joml.Vector3i;

public class FluidHelper {
    private static final Vector3i[] neighborPositions = new Vector3i[4];
    public static Vector2i updateFluid(Vector3i pos, Vector2i fluid) {
        neighborPositions[0] = new Vector3i(pos.x, pos.y, pos.z+1);
        neighborPositions[1] = new Vector3i(pos.x+1, pos.y, pos.z);
        neighborPositions[2] = new Vector3i(pos.x, pos.y, pos.z-1);
        neighborPositions[3] = new Vector3i(pos.x-1, pos.y, pos.z);
        BlockType type = BlockTypes.blockTypeMap.get(fluid.x);
        boolean scheduleTick = false;
        if (type.isFluid) { //only flow out if it's a fluid
            Vector3i belowPos = new Vector3i(pos.x, pos.y - 1, pos.z);
            Vector2i belowFluid = new Vector2i(World.getBlock(belowPos));
            BlockType belowType = BlockTypes.blockTypeMap.get(belowFluid.x);
            if (belowType.isFluidReplaceable || (belowFluid.x == fluid.x && belowFluid.y < 16)) { //if above something it can flow into
                int splitValue = Math.min(belowType.isFluidReplaceable ? 16 : 16 - belowFluid.y, fluid.y);
                if (splitValue > 1) { //prevent reverse flow
                    fluid.y -= splitValue;
                    belowFluid = new Vector2i(fluid.x, belowFluid.y+splitValue);
                    if (fluid.y <= 0) { //set to air if all liquid flows out
                        fluid = new Vector2i(0);
                        type = BlockTypes.AIR;
                    }
                    World.setLiquid(belowPos.x, belowPos.y, belowPos.z, belowFluid.x, belowFluid.y, true, false);
                    scheduleTick = true;
                }
            }
        }
        Vector3i abovePos = new Vector3i(pos.x, pos.y + 1, pos.z);
        Vector2i aboveFluid = new Vector2i(World.getBlock(abovePos));
        BlockType aboveType = BlockTypes.blockTypeMap.get(aboveFluid.x);
        if ((type.isFluidReplaceable || (aboveFluid.x == fluid.x && fluid.y < 16)) && aboveType.isFluid) { //if below something that can flow into it
            int splitValue = Math.min(type.isFluidReplaceable ? 16 : 16-fluid.y, aboveFluid.y);
            if (splitValue > 0) { //prevent reverse flow
                fluid = new Vector2i(aboveFluid.x, fluid.y+splitValue);
                type = aboveType;
                aboveFluid.y -= splitValue;
                if (aboveFluid.y <= 0) { //set above to air if all liquid flowed out
                    aboveFluid = new Vector2i(0);
                }
                World.setLiquid(abovePos.x, abovePos.y, abovePos.z, aboveFluid.x, aboveFluid.y, true, false);
                scheduleTick = true;
            }
        }

        for (Vector3i nPos : neighborPositions) { //flow in or out from each neighbor
            Vector2i nFluid = new Vector2i(World.getBlock(nPos));
            BlockType nType = BlockTypes.blockTypeMap.get(nFluid.x);
            if (((nFluid.x == fluid.x && nFluid.y > fluid.y) || (type.isFluidReplaceable && nFluid.y > 0)) && nType.isFluid) { //flow in
                int splitValue = (int)((nFluid.y-fluid.y)/2f);
                if (splitValue > 1) { //prevent reverse flow
                    fluid = new Vector2i(nFluid.x, fluid.y + splitValue);
                    type = nType;
                    nFluid.y -= splitValue;
                    if (nFluid.y <= 0) { //set to air if all liquid flows out
                        nFluid = new Vector2i(0);
                    }
                    World.setLiquid(nPos.x, nPos.y, nPos.z, nFluid.x, nFluid.y, true, false);
                    scheduleTick = true;
                }
            } else if ((nType.isFluidReplaceable || (nFluid.x == fluid.x && nFluid.y < fluid.y)) && type.isFluid) { //flow out
                int splitValue = (int)((fluid.y-nFluid.y)/2f);
                if (splitValue > 0) { //prevent reverse flow
                    fluid.y -= splitValue;
                    nFluid = new Vector2i(fluid.x, nFluid.y + splitValue);
                    if (fluid.y <= 0) { //set to air if all liquid flows out
                        fluid = new Vector2i(0);
                        type = BlockTypes.AIR;
                    }
                    World.setLiquid(nPos.x, nPos.y, nPos.z, nFluid.x, nFluid.y, true, false);
                    scheduleTick = true;
                }
            }
        }
        if (scheduleTick) {
            ScheduledTicker.schedule.put(Main.currentTick+1, pos);
        } else {
            scheduleTick = false;
        }

//        for (Vector3i nPos : neighborPositions) {
//            Vector2i nFluid = World.getBlock(nPos);
//            if (nFluid.x == fluid.x || BlockTypes.blockTypeMap.get(nFluid.x).isFluidReplaceable || BlockTypes.blockTypeMap.get(fluid.x).isFluidReplaceable) { //only flow if both are same fluid or one is replaceable
//                if (BlockTypes.blockTypeMap.get(nFluid.x).isFluid && nFluid.y > fluid.y && nPos.y >= pos.y) { //flow in
//                    int splitValue = 0;
//                    Vector2i aboveFluid = World.getBlock(pos.x, pos.y - 1, pos.z);
//                    BlockType aboveType = BlockTypes.blockTypeMap.get(aboveFluid.x);
//                    if (aboveType.isFluid && ((centerType.isFluid && fluid.y < 20) || centerType.isFluidReplaceable)) { //if below something that can flow into it
//                        if (nPos.y > pos.y) { //cancel if nPos is not above pos
//                            splitValue = Math.min(nFluid.y, 20 - fluid.y); //transfer as much as possible while ensuring it doesnt go above 20.
//                        }
//                    } else { //flow from sides
//                        int difference = nFluid.y - fluid.y;
//                        splitValue = (int) (Math.floor(difference / 2f));
//                    }
//                    if (splitValue > 0) {
//                        nFluid.y -= splitValue;
//                        fluid = new Vector2i(1, fluid.y + splitValue);
//                        World.setBlock(nPos.x, nPos.y, nPos.z, 1, nFluid.y, true, false, true);
//                    }
//                } else if (centerType.isFluid && nFluid.y < fluid.y && nPos.y <= pos.y) { //flow out
//                    int splitValue = 0;
//                    Vector2i belowFluid = World.getBlock(pos.x, pos.y - 1, pos.z);
//                    BlockType belowType = BlockTypes.blockTypeMap.get(belowFluid.x);
//                    if (belowType.isFluidReplaceable || (belowType.isFluid && belowFluid.y < 20)) { //if above something it can flow into
//                        if (nPos.y < pos.y) { //cancel if nPos is not below pos
//                            splitValue = Math.min(fluid.y, 20 - nFluid.y); //transfer as much as possible while ensuring it doesnt go above 20.
//                        }
//                    } else { //flow to sides
//                        int difference = fluid.y - nFluid.y;
//                        splitValue = (int) (Math.floor(difference / 2f));
//                    }
//                    if (splitValue > 0) {
//                        nFluid = new Vector2i(1, nFluid.y + splitValue);
//                        fluid.y -= splitValue;
//                        World.setBlock(nPos.x, nPos.y, nPos.z, 1, nFluid.y, true, false, true);
//                    }
//                }
//            }
//        }
        return fluid;
    }
}