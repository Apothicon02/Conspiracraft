package org.conspiracraft.blocks.types;

import org.conspiracraft.blocks.Material;
import org.conspiracraft.blocks.Materials;
import org.conspiracraft.physics.AABB;
import org.conspiracraft.utils.Utils;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;
import org.conspiracraft.blocks.BlockTag;
import java.util.List;
import java.util.Map;

import static org.conspiracraft.world.World.*;

public class BlockType {
    public final int id;
    public final String name;
    public BlockProperties blockProperties;
    public List<BlockTag> tags = List.of();
    public boolean altTexLoad = false;
    public BlockType altTexLoad(boolean altTexLoad) {
        this.altTexLoad = altTexLoad;
        return this;
    }

    public AABB[] getAABB(int subType, float x, float y, float z, boolean forCollision) {
        if (blockProperties.hasSlab) {
            if (subType == 1) {
                return new AABB[]{new AABB((int)x, (int)(x+1), ((int)y)+0.5f, (int)(y+1), (int)z, (int)(z+1))};
            } else if (subType == 2) {
                return new AABB[]{new AABB((int)x, (int)(x+1), (int)y, ((int)(y+1))-0.5f, (int)z, (int)(z+1))};
            }
        }
        return new AABB[]{new AABB((int)x, (int)(x+1), (int)y, (int)(y+1), (int)z, (int)(z+1))};
    }

    public float getResistance() {
        return blockProperties.resistance;
    }

    public boolean blocksLight(Vector2i block) {
        return blocksLight(block.x(), block.y());
    }
    public boolean blocksLight(int type, int subType) {
        return blockProperties.blocksLight;
    }

    public boolean needsSupport(Vector2i block) {
        return blockProperties.needsSupport;
    }

    public void lostSupport(Vector3i pos, Vector2i block) {
        //World.setBlock(pos.x, pos.y, pos.z, 0, 0, true, false, 2, false);
    }

    public boolean obstructingHeightmap(Vector2i block) {
        return blockProperties.obstructsHeightmap;
    }

    public boolean permeable() {
        return !blockProperties.isSolid || blockProperties.permeable;
    }

    public Map<Integer, Material> materials;
    public BlockType(int id, String name, Map<Integer, Material> materials, BlockProperties blockProperties) {
        this.id = id;
        this.name = name;
        this.materials = materials;
        this.blockProperties = blockProperties;
    }
    public BlockType(int id, String name, BlockProperties blockProperties) {
        this.id = id;
        this.name = name;
        this.materials = Map.of(Utils.packColor(255), Materials.KYANITE);
        this.blockProperties = blockProperties;
    }

    public int use(Vector3f pos, Vector2i block) {return use((int)pos.x(), (int)pos.y(), (int)pos.z(), block);}
    public int use(int x, int y, int z, Vector2i block) {return 0;}
    public void neighborUpdated(int x, int y, int z, Vector2i block) {}

    public void updateSupport(Vector3i pos) {
//        Vector2i block = getBlock(pos);
//        if (!blockProperties.isSolid) {
//            Vector3i abovePos = new Vector3i(pos.x, pos.y + 1, pos.z);
//            Vector2i aboveBlock = getBlock(abovePos);
//            if (aboveBlock != null) {
//                int aboveBlockId = aboveBlock.x();
//                if (BlockTypes.blockTypes[aboveBlockId].needsSupport(aboveBlock)) {
//                    lostSupport(abovePos, aboveBlock);
//                }
//            }
//        }
//        if (needsSupport(block)) {
//            Vector2i belowBlock = getBlock(new Vector3i(pos.x, pos.y - 1, pos.z));
//            if (belowBlock != null) {
//                int belowBlockId = belowBlock.x();
//                if (!BlockTypes.blockTypes[belowBlockId].blockProperties.isSolid) {
//                    lostSupport(pos, block);
//                }
//            }
//        }
    }

    public void tick(Vector4i pos) {
        if (inBounds(pos.x, pos.y, pos.z)) {
            Vector3i justPos = new Vector3i(pos.x, pos.y, pos.z);
            updateSupport(justPos);
        }
    }

    public boolean whilePlayerBreaking(Vector3i pos, Vector2i blockBreaking, Vector2i hand) {
        return true;
    }

    public Vector2i onPlace(int x, int y, int z, int blockType, int blockSubType, boolean isSilent) {
        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(x, y, z));
        }
        return new Vector2i(blockType, blockSubType);
//        for (Vector3i nPos : new Vector3i[]{new Vector3i(pos.x, pos.y - 1, pos.z), new Vector3i(pos.x, pos.y + 1, pos.z), new Vector3i(pos.x - 1, pos.y, pos.z),
//                new Vector3i(pos.x + 1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z - 1), new Vector3i(pos.x, pos.y, pos.z + 1)}) {
//            Vector2i nBlock = World.getBlock(nPos.x, nPos.y, nPos.z);
//            if (nBlock != null) {
//                BlockType blockType = BlockTypes.blockTypes[nBlock.x);
//                if (blockType instanceof WaterBlockType) {
//                    ((WaterBlockType) blockType).moisturize(nPos);
//                }
//            }
//        }
    }
}
