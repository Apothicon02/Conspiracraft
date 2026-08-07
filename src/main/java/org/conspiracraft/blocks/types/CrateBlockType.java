package org.conspiracraft.blocks.types;

import org.conspiracraft.blocks.Material;
import org.joml.Vector2i;

import java.util.Map;

public class CrateBlockType extends BlockType {
	public CrateBlockType(int id, String name, Map<Integer, Material> materials, BlockProperties blockProperties) {
        super(id, name, materials, blockProperties);
    }

    @Override
    public int use(int x, int y, int z, Vector2i block) {

        return 200;
    }
}
