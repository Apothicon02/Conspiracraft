package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.world.FluidHelper;
import org.conspiracraft.game.world.GasHelper;
import org.conspiracraft.game.world.World;
import org.conspiracraft.game.world.WorldGen;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.game.world.World.*;
import static org.conspiracraft.game.world.WorldGen.areChunksCompressed;

public class BlockType {
    public BlockProperties blockProperties;

    public boolean needsSupport(Vector2i block) {
        return blockProperties.needsSupport;
    }

    public void lostSupport(Vector3i pos, Vector2i block) {
        if (!areChunksCompressed) {
            WorldGen.setBlockWorldgenUpdates(pos.x, pos.y, pos.z, 0, 0);
        } else {
            World.setBlock(pos.x, pos.y, pos.z, 0, 0, true, false, 2, false);
        }
    }

    public boolean obstructingHeightmap(Vector2i block) {
        return blockProperties.obstructsHeightmap;
    }

    public BlockType(BlockProperties blockProperties) {
        this.blockProperties = blockProperties;
    }

    public void updateSupport(Vector3i pos) {
        Vector2i block = getBlock(pos);
        if (!blockProperties.isSolid) {
            Vector3i abovePos = new Vector3i(pos.x, pos.y + 1, pos.z);
            Vector2i aboveBlock = getBlock(abovePos);
            if (aboveBlock != null) {
                int aboveBlockId = aboveBlock.x();
                if (BlockTypes.blockTypeMap.get(aboveBlockId).needsSupport(aboveBlock)) {
                    lostSupport(abovePos, aboveBlock);
                }
            }
        }
        if (needsSupport(block)) {
            Vector2i belowBlock = getBlock(new Vector3i(pos.x, pos.y - 1, pos.z));
            if (belowBlock != null) {
                int belowBlockId = belowBlock.x();
                if (!BlockTypes.blockTypeMap.get(belowBlockId).blockProperties.isSolid) {
                    lostSupport(pos, block);
                }
            }
        }
    }

    public void tick(Vector4i pos) {
        if (inBounds(pos.x, pos.y, pos.z)) {
            Vector3i justPos = new Vector3i(pos.x, pos.y, pos.z);
            fluidTick(justPos);
            updateSupport(justPos);
        }
    }

    public void fluidTick(Vector3i pos) {
        Vector2i block = getBlock(pos);
        FluidHelper.updateFluid(pos, block);
        GasHelper.updateGas(pos, block);
    }

    public void onPlace(Vector3i pos, Vector2i block, boolean isSilent) {
        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(pos.x, pos.y, pos.z));
        }
        for (Vector3i nPos : new Vector3i[]{new Vector3i(pos.x, pos.y - 1, pos.z), new Vector3i(pos.x, pos.y + 1, pos.z), new Vector3i(pos.x - 1, pos.y, pos.z),
                new Vector3i(pos.x + 1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z - 1), new Vector3i(pos.x, pos.y, pos.z + 1)}) {
            Vector2i nBlock = World.getBlock(nPos.x, nPos.y, nPos.z);
            BlockType blockType = BlockTypes.blockTypeMap.get(nBlock.x);
            if (blockType instanceof WaterBlockType) {
                ((WaterBlockType) blockType).moisturize(nPos);
            }
        }
    }
}
