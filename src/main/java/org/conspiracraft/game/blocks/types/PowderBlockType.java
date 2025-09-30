package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.world.World;
import org.conspiracraft.game.world.WorldGen;
import org.joml.Vector2i;
import org.joml.Vector3i;

import static org.conspiracraft.game.world.WorldGen.areChunksCompressed;

public class PowderBlockType extends BlockType {

    @Override
    public void lostSupport(Vector3i blockPos, Vector2i block) {
        Vector3i pos = new Vector3i(blockPos);
        if (!areChunksCompressed) {
            if (BlockTypes.blockTypeMap.get(WorldGen.getBlockWorldgen(pos.x, pos.y-1, pos.z).x).blockProperties.isFluidReplaceable) {
                WorldGen.setBlockWorldgenUpdates(pos.x, pos.y, pos.z, 0, 0);
                WorldGen.setBlockWorldgenUpdates(pos.x, pos.y-1, pos.z, block.x, Math.min(5, block.y+1));
            } else {
                WorldGen.setBlockWorldgenUpdates(pos.x, pos.y, pos.z, block.y >= 5 ? block.x+1 : block.x, 0);
            }
        } else {
            if (BlockTypes.blockTypeMap.get(World.getBlock(pos.x, pos.y-1, pos.z).x).blockProperties.isFluidReplaceable) {
                World.setBlock(pos.x, pos.y, pos.z, 0, 0, true, false, 2, true);
                World.setBlock(pos.x, pos.y-1, pos.z, block.x, Math.min(5, block.y+1), true, false, 2, false);
            } else {
                World.setBlock(pos.x, pos.y, pos.z, block.y >= 5 ? block.x+1 : block.x, 0, true, false, -1, false);
            }
        }
    }

    public PowderBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }
}
