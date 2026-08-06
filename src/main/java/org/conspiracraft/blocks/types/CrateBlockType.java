package org.conspiracraft.blocks.types;

import org.joml.Vector2i;

public class CrateBlockType extends BlockType {
	public CrateBlockType(int id, String name, BlockProperties blockProperties) {
        super(id, name, blockProperties);
    }

    @Override
    public int use(int x, int y, int z, Vector2i block) {

        return 200;
    }
}
