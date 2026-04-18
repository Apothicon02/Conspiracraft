package org.conspiracraft.blocks.types;

import org.joml.Vector2i;

public class LeafBlockType extends BlockType {

    @Override
    public boolean needsSupport(Vector2i block) {
        return blockProperties.needsSupport ? true : block.y > 0;
    }
    @Override
    public boolean obstructingHeightmap(Vector2i block) {
        return blockProperties.obstructsHeightmap ? true : block.y > 0;
    }
    @Override
    public boolean blocksLight(Vector2i block) {
        return block.y == 0 ? blockProperties.blocksLight : false;
    }

    public LeafBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }
}
