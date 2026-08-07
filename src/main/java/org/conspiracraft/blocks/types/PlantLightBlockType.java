package org.conspiracraft.blocks.types;

import org.conspiracraft.blocks.Material;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.Map;

import static org.conspiracraft.world.World.getBlock;

public class PlantLightBlockType extends LightBlockType {

    @Override
    public Vector2i onPlace(int x, int y, int z, int blockType, int blockSubType, boolean isSilent) {
        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(x, y, z));
        }
        Vector2i blockOn = getBlock(x, y-1, z);
        if (blockOn.x != BlockTypes.GRASS.id) {
            //lostSupport(x, y, z, blockType, blockSubType);
        }
        return new Vector2i(blockType, blockSubType);
    }

    public PlantLightBlockType(int id, String name, Map<Integer, Material> materials, LightBlockProperties blockProperties) {
        super(id, name, materials, blockProperties);
    }
    public PlantLightBlockType(int id, String name, LightBlockProperties blockProperties) {
        super(id, name, blockProperties);
    }
}
