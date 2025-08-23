package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.world.FluidHelper;
import org.conspiracraft.game.world.World;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.conspiracraft.game.world.World.getBlock;
import static org.conspiracraft.game.world.World.setBlock;

public class BlockType {
    public BlockProperties blockProperties;

    public boolean needsSupport(Vector2i block) {
        return blockProperties.needsSupport;
    }

    public boolean obstructingHeightmap(Vector2i block) {
        return blockProperties.obstructsHeightmap;
    }

    public BlockType(BlockProperties blockProperties) {
        this.blockProperties = blockProperties;
    }

    public void tick(Vector3i pos) {
        if (pos.x > 0 && pos.x < World.size-1 && pos.z > 0 && pos.z < World.size-1) {
            FluidHelper.updateFluid(pos, getBlock(pos));
        }
    }

    public void randomTick(Vector3i pos) {}

    public void onPlace(Vector3i pos, boolean isSilent) {
        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(pos.x, pos.y, pos.z));
        }

        if (!blockProperties.isSolid) {
            Vector2i aboveBlock = getBlock(new Vector3i(pos.x, pos.y + 1, pos.z));
            if (aboveBlock != null) {
                int aboveBlockId = aboveBlock.x();
                if (BlockTypes.blockTypeMap.get(aboveBlockId).needsSupport(aboveBlock)) {
                    setBlock(pos.x, pos.y + 1, pos.z, 0, 0, true, true, 1, false);
                }
            }
        }
    }
}
