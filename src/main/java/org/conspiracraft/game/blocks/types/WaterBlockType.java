package org.conspiracraft.game.blocks.types;

import org.conspiracraft.game.blocks.Tags;
import org.conspiracraft.game.world.World;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.game.world.World.inBounds;

public class WaterBlockType extends BlockType {

    public void moisturize(Vector3i pos) {
        if (World.getBlock(pos.x, pos.y-1, pos.z).x == BlockTypes.getId(BlockTypes.DIRT)) {
            World.setBlock(pos.x, pos.y, pos.z, 0, 0, true, false, 3, false);
            World.setBlock(pos.x, pos.y-1, pos.z, BlockTypes.getId(BlockTypes.MUD), 0, true, false, 3, false);
        } else if (World.getBlock(pos.x, pos.y-1, pos.z).x == BlockTypes.getId(BlockTypes.GRASS)) {
            int blockType = 4;
            if (Math.random() < 0.33f) { //33% chance to generate a flower
                blockType = Tags.shortFlowers.tagged.get((int)(Math.random()*Tags.shortFlowers.tagged.size()));
            }
            World.setBlock(pos.x, pos.y, pos.z, blockType, (int)(Math.random()*4), true, false, 3, false);
        }
    }

    @Override
    public void tick(Vector4i pos) {
        if (inBounds(pos.x, pos.y, pos.z)) {
            Vector3i justPos = new Vector3i(pos.x, pos.y, pos.z);
            fluidTick(justPos);
            updateSupport(justPos);
            moisturize(justPos);
        }
    }

    public WaterBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }
}
