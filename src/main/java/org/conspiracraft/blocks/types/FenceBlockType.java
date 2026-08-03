package org.conspiracraft.blocks.types;

import org.conspiracraft.physics.AABB;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3i;

public class FenceBlockType extends BlockType {
    public FenceBlockType(int id, String name, BlockProperties blockProperties) {
        super(id, name, blockProperties);
    }

    @Override
    public AABB[] getAABB(int subType, float x, float y, float z, boolean forCollision) {
        return switch (subType) {
            case 1 -> new AABB[]{new AABB(((int) x) + 0.45f, ((int) (x + 1)), (int) y, ((int) (y + 1)) + 0.5f, ((int) z) + 0.45f, ((int) (z + 1)) - 0.45f)};
            case 2 -> new AABB[]{new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1)))};
            case 3 -> new AABB[]{new AABB(((int)x), ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1))-0.45f)};
            case 4 -> new AABB[]{new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z), ((int)(z+1))-0.45f)};
            case 5 -> new AABB[]{new AABB(((int)x), ((int)(x+1)), (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1))-0.45f)};
            case 6 -> new AABB[]{new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z), ((int)(z+1)))};
            case 7 -> new AABB[]{new AABB(((int) x) + 0.45f, ((int) (x + 1)), (int) y, ((int) (y + 1)) + 0.5f, ((int) z) + 0.45f, ((int) (z + 1)) - 0.45f),
                    new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1)))};
            case 8 -> new AABB[]{new AABB(((int)x), ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1))-0.45f),
                        new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1)))};
            case 9 -> new AABB[]{new AABB(((int)x), ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1))-0.45f),
                    new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z), ((int)(z+1))-0.45f)};
            case 10 -> new AABB[]{new AABB(((int) x) + 0.45f, ((int) (x + 1)), (int) y, ((int) (y + 1)) + 0.5f, ((int) z) + 0.45f, ((int) (z + 1)) - 0.45f),
                    new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z), ((int)(z+1))-0.45f)};
            case 11 -> new AABB[]{new AABB(((int) x), ((int) (x + 1)), (int) y, ((int) (y + 1)) + 0.5f, ((int) z) + 0.45f, ((int) (z + 1)) - 0.45f),
                    new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1)))};
            case 12 -> new AABB[]{new AABB(((int) x), ((int) (x + 1))-0.45f, (int) y, ((int) (y + 1)) + 0.5f, ((int) z) + 0.45f, ((int) (z + 1)) - 0.45f),
                    new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z), ((int)(z+1)))};
            case 13 -> new AABB[]{new AABB(((int) x), ((int) (x + 1)), (int) y, ((int) (y + 1)) + 0.5f, ((int) z) + 0.45f, ((int) (z + 1)) - 0.45f),
                    new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z), ((int)(z+1))-0.45f)};
            case 14 -> new AABB[]{new AABB(((int) x)+0.45f, ((int) (x + 1)), (int) y, ((int) (y + 1)) + 0.5f, ((int) z) + 0.45f, ((int) (z + 1)) - 0.45f),
                    new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z), ((int)(z+1)))};
            case 15 -> new AABB[]{new AABB(((int)x), ((int)(x+1)), (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1))-0.45f),
                    new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z), ((int)(z+1)))};
            default -> new AABB[]{new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, ((int)(y+1))+0.5f, ((int)z)+0.45f, ((int)(z+1))-0.45f)};
        };
    }

    public static final int[] shapeMapping = new int[]{0, 3, 4, 9, 1, 5, 10, 13, 2, 8, 6, 12, 7, 11, 14, 15};
    @Override
    public void neighborUpdated(int x, int y, int z, Vector2i block) {
        boolean north = canConnectTo(new Vector3i(x, y, z+1)), east = canConnectTo(new Vector3i(x+1, y, z)),
                south = canConnectTo(new Vector3i(x, y, z-1)), west = canConnectTo(new Vector3i(x-1, y, z));
        byte shape = (byte) ((north ? 1 << 3 : 0) | (east ? 1 << 2 : 0) | (south ? 1 << 1 : 0) | (west ? 1 : 0));
        World.setBlock(x, y, z, block.x(), shapeMapping[shape], false, false, true);
    }

    public static boolean canConnectTo(Vector3i pos) {
        Vector2i nBlock = World.getBlock(pos);
        BlockType nType = BlockTypes.blockTypes[nBlock.x()];
        return nType instanceof FenceBlockType || nType instanceof GateBlockType || nType.blockProperties.isSolid;
    }
}
