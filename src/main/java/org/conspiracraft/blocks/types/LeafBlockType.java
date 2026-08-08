package org.conspiracraft.blocks.types;

import org.conspiracraft.blocks.Material;
import org.joml.Vector2i;

import java.util.Map;

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
    public boolean blocksLight(int type, int subType) {
        return subType == 0 ? blockProperties.blocksLight : false;
    }

    public LeafBlockType(int id, String name, Map<Integer, Material> materials, BlockProperties blockProperties) {
        super(id, name, materials, blockProperties);
    }
    public LeafBlockType(int id, String name, BlockProperties blockProperties) {
        super(id, name, blockProperties);
    }
}
