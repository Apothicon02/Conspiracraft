package org.conspiracraft.game.blocks.types;

import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.conspiracraft.game.world.World.getBlock;
import static org.conspiracraft.game.world.WorldGen.areChunksCompressed;
import static org.conspiracraft.game.world.WorldGen.getBlockWorldgen;

public class PlantLightBlockType extends LightBlockType {

    @Override
    public void onPlace(Vector3i pos, Vector2i block, boolean isSilent) {
        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(pos.x, pos.y, pos.z));
        }
        Vector2i blockOn = areChunksCompressed ? getBlock(pos.x, pos.y-1, pos.z) : getBlockWorldgen(pos.x, pos.y-1, pos.z);
        if (blockOn.x != BlockTypes.getId(BlockTypes.GRASS)) {
            lostSupport(pos, block);
        }
    }

    public PlantLightBlockType(LightBlockProperties blockProperties) {
        super(blockProperties);
    }
}
