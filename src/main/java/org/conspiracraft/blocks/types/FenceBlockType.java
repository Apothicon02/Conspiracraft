package org.conspiracraft.blocks.types;

import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3i;

public class FenceBlockType extends BlockType {
    public FenceBlockType(int id, String name, BlockProperties blockProperties) {
        super(id, name, blockProperties);
    }

    public static final int[] shapeMapping = new int[]{0, 3, 4, 9, 1, 5, 10, 13, 2, 8, 6, 12, 7, 11, 14, 15};
    @Override
    public void neighborUpdated(int x, int y, int z, Vector2i block) {
        boolean north = canConnectTo(new Vector3i(x, y, z+1)), east = canConnectTo(new Vector3i(x+1, y, z)),
                south = canConnectTo(new Vector3i(x, y, z-1)), west = canConnectTo(new Vector3i(x-1, y, z));
        byte shape = (byte) ((north ? 1 << 3 : 0) | (east ? 1 << 2 : 0) | (south ? 1 << 1 : 0) | (west ? 1 : 0));
        World.setBlock(x, y, z, block.x(), shapeMapping[shape], false, false);
    }

    public static boolean canConnectTo(Vector3i pos) {
        Vector2i nBlock = World.getBlock(pos);
        BlockType nType = BlockTypes.blockTypes[nBlock.x()];
        return nType instanceof FenceBlockType || nType.blockProperties.isSolid;
    }
}
