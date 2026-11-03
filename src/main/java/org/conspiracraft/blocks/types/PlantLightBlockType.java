package org.conspiracraft.blocks.types;

import org.conspiracraft.blocks.Material;
import org.conspiracraft.world.Chunk;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.Map;

import static org.conspiracraft.world.World.getBlock;

public class PlantLightBlockType extends LightBlockType {

    @Override
    public int onPlace(int x, int y, int z, int blockType, int blockSubType, boolean isSilent) {
        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(x, y, z));
        }
        int blockOn = getBlock(x, y-1, z);
        if (blockOn >> 16 != BlockTypes.GRASS.id) {
            //lostSupport(x, y, z, blockType, blockSubType);
        }
        return Chunk.packInts(blockType, blockSubType);
    }

    public PlantLightBlockType(String name, Map<Integer, Material> materials, LightBlockProperties blockProperties) {
        super(name, materials, blockProperties);
    }
    public PlantLightBlockType(String name, LightBlockProperties blockProperties) {
        super(name, blockProperties);
    }
}
