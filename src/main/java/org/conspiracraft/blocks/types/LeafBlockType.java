package org.conspiracraft.blocks.types;

import org.conspiracraft.blocks.Material;
import org.joml.Vector2i;

import java.util.Map;

public class LeafBlockType extends BlockType {

    @Override
    public boolean needsSupport(int block) {
        return blockProperties.needsSupport ? true : (block & 0xFFFF) > 0;
    }
    @Override
    public boolean obstructingHeightmap(int block) {
        return blockProperties.obstructsHeightmap || (block & 0xFFFF) == 0;
    }
    @Override
    public boolean blocksLight(int type, int subType) {
        return subType == 0 ? blockProperties.blocksLight : false;
    }

    public LeafBlockType(String name, Map<Integer, Material> materials, BlockProperties blockProperties) {
        super(name, materials, blockProperties);
    }
    public LeafBlockType(String name, BlockProperties blockProperties) {
        super(name, blockProperties);
    }
}
