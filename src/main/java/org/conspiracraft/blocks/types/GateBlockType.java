package org.conspiracraft.blocks.types;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.Material;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.Map;

public class GateBlockType extends BlockType {
	public GateBlockType(String name, Map<Integer, Material> materials, BlockProperties blockProperties) {
        super(name, materials, blockProperties);
    }

    @Override
    public AABB[] getAABB(int subType, double x, double y, double z, boolean forCollision) {
        boolean isEven = (subType | 1) > subType;
        return (isEven && forCollision) ? null : (subType < 4 ? new AABB[]{new AABB((int)x, (int)(x+1), (int)y, (int)(y+1), ((int)z)+0.45f, ((int)(z+1))-0.45f)} : new AABB[]{new AABB(((int)x)+0.45f, ((int)(x+1))-0.45f, (int)y, (int)(y+1), (int)z, (int)(z+1))});
    }

    @Override
    public int use(int x, int y, int z, int block) {
        boolean isEven = ((block & 0xFFFF) | 1) > (block & 0xFFFF);
        World.setBlock(x, y, z, block >> 16, isEven ? (block & 0xFFFF)+1 : (block & 0xFFFF)-1, false, false, false);
        return 200;
    }

    @Override
    public int onPlace(int x, int y, int z, int blockType, int blockSubType, boolean isSilent) {
        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(x, y, z));
        }
        int existing = World.getBlock(x, y, z);
        if (existing >> 16 != blockType) {
            double zDist = z-Main.player.pos.z(), xDist = x-Main.player.pos.x();
            double zDistAbs = Math.abs(z-Main.player.pos.z()), xDistAbs = Math.abs(x-Main.player.pos.x());
            if (zDistAbs > xDistAbs) {
                if (zDist > 0) {
                    blockSubType = 2;
                }
            } else {
                if (xDist > 0) {
                    blockSubType = 4;
                } else {
                    blockSubType = 6;
                }
            }
        }
        return Chunk.packInts(blockType, blockSubType);
    }
}
