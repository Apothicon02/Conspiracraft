package org.conspiracraft.game.blocks.types;

import org.joml.Vector3i;

import static org.conspiracraft.game.world.World.setBlock;

public class CloudBlockType extends BlockType {

    public CloudBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }

    @Override
    public void randomTick(Vector3i pos) {
        setBlock(pos.x, pos.y-1, pos.z, 1, 1, false, false, 1, false);
    }
}
