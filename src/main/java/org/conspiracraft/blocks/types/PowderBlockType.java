package org.conspiracraft.blocks.types;

import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.conspiracraft.blocks.Recipes;
import org.conspiracraft.world.World;


public class PowderBlockType extends BlockType {

    @Override
    public void lostSupport(Vector3i pos, Vector2i block) {
//        if (BlockTypes.blockTypeMap.get(World.getBlock(pos.x(), pos.y()-1, pos.z()).x).blockProperties.isFluidReplaceable) {
//            World.setBlock(pos.x(), pos.y(), pos.z(), 0, 0, true, false, 2, true);
//            if (BlockTypes.blockTypeMap.get(World.getBlock(pos.x(), pos.y()-2, pos.z()).x).blockProperties.isFluidReplaceable) {
//                World.setBlock(pos.x(), pos.y()-1, pos.z(), block.x(), Math.min(5, block.y()+1), true, false, 2, false);
//            } else {
//                World.setBlock(pos.x(), pos.y()-1, pos.z(), block.y >= 5 ? 0 : block.x(), 0, true, false, -1, false);
//                Recipes.drop(block.x(), new Vector3f(pos.x(), pos.y()-1, pos.z()));
//            }
//        }
    }

    @Override
    public boolean obstructingHeightmap(Vector2i block) {
        return blockProperties.obstructsHeightmap && block.y == 0;
    }

    @Override
    public boolean blocksLight(Vector2i block) {
        return blockProperties.blocksLight && block.y == 0;
    }

    public PowderBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }
}
