package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.world.World;
import org.conspiracraft.game.world.WorldGen;
import org.joml.Vector2i;
import org.joml.Vector3i;

public class PowderBlockType extends BlockType {

    @Override
    public void lostSupport(Vector3i blockPos, Vector2i block, boolean worldgen) {
        Vector3i pos = new Vector3i(blockPos);
        if (worldgen) {
            if (BlockTypes.blockTypeMap.get(WorldGen.getBlockWorldgen(pos.x, pos.y-1, pos.z).x).blockProperties.isFluidReplaceable) {
                WorldGen.setBlockWorldgenUpdates(pos.x, pos.y, pos.z, 0, 0);
                pos.sub(0, 1, 0);
            }
            boolean onSolidBlock = BlockTypes.blockTypeMap.get(WorldGen.getBlockWorldgen(pos.x, pos.y-1, pos.z).x).blockProperties.isSolid;
            WorldGen.setBlockWorldgenUpdates(pos.x, pos.y, pos.z, onSolidBlock && block.y >= 5 ? block.x+1 : block.x, onSolidBlock ? 0 : Math.min(5, block.y+1));
        } else {
            if (BlockTypes.blockTypeMap.get(World.getBlock(pos.x, pos.y-1, pos.z).x).blockProperties.isFluidReplaceable) {
                World.setBlock(pos.x, pos.y, pos.z, 0, 0, true, false, 2, true);
                pos.sub(0, 1, 0);
            }
            boolean onSolidBlock = BlockTypes.blockTypeMap.get(World.getBlock(pos.x, pos.y-1, pos.z).x).blockProperties.isSolid;
            World.setBlock(pos.x, pos.y, pos.z, onSolidBlock && block.y >= 5 ? block.x+1 : block.x, onSolidBlock ? 0 : Math.min(5, block.y+1), true, false, 2, false);
        }
    }

    public PowderBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }
}
