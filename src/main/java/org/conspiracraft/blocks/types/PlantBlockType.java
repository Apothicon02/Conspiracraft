package org.conspiracraft.blocks.types;

import kotlin.Pair;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.conspiracraft.blocks.BlockTag;
import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.world.World;

import static org.conspiracraft.world.World.getBlock;

public class PlantBlockType extends BlockType {

    @Override
    public void lostSupport(Vector3i pos, Vector2i block) {
//        boolean onSediment = BlockTags.sediment.tagged.contains(getBlock(pos.x, pos.y-1, pos.z).x);
//        World.setBlock(pos.x, pos.y, pos.z, onSediment ? BlockTypes.DEAD_BUSH) : 0, onSediment ? (int)(Math.random()*1.9) : 0, true, false, 2, false);
    }

    @Override
    public Vector2i onPlace(int x, int y, int z, int blockType, int blockSubType, boolean isSilent) {
        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(x, y, z));
        }
        Vector2i blockOn = getBlock(x, y-1, z);
        boolean survives = false;
        for (Pair<BlockTag, BlockTag> pair : BlockTags.survivalTags) {
            if (pair.getFirst().tagged.contains(x)) {
                if (pair.getSecond().tagged.contains(blockOn.x)) {
                    survives = true;
                    break;
                }
            }
        }
        if (!survives) {
            //lostSupport(x, y, z, blockType, blockSubType);
        }
        return new Vector2i(blockType, blockSubType);
    }

    public PlantBlockType(int id, String name, BlockProperties blockProperties) {
        super(id, name, blockProperties);
    }
}
