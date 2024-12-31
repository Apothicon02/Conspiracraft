package org.conspiracraft.game.blocks;

import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.world.World;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.game.world.World.*;

public class BlockHelper {
    private static final Vector3i[] neighborBlocks = new Vector3i[6];
    private static final Vector3i[] neighborPositions = new Vector3i[6];
    private static final Vector4i[] neighborLights = new Vector4i[6];
    public static int updateLight(Vector3i pos, Block block, boolean clean) {
        BlockType blockType = BlockTypes.blockTypeMap.get(block.typeId());
        if (blockType.isTransparent || blockType instanceof LightBlockType) {
            byte r = block.r();
            byte g = block.g();
            byte b = block.b();
            byte s = block.s();
            neighborBlocks[0] = new Vector3i(pos.x, pos.y, pos.z+1);
            neighborBlocks[1] = new Vector3i(pos.x+1, pos.y, pos.z);
            neighborBlocks[2] = new Vector3i(pos.x, pos.y, pos.z-1);
            neighborBlocks[3] = new Vector3i(pos.x-1, pos.y, pos.z);
            neighborBlocks[4] = new Vector3i(pos.x, pos.y+1, pos.z);
            neighborBlocks[5] = new Vector3i(pos.x, pos.y-1, pos.z);

            for (byte i = 0; i < 6; i++) {
                Vector3i neighborPos = neighborBlocks[i];
                Block neighbor = World.getBlock(neighborPos);
                if (neighbor != null) {
                    BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.typeId());
                    if (neighborBlockType.isTransparent || neighborBlockType instanceof LightBlockType) {
                        neighborPositions[i] = neighborPos;
                        neighborLights[i] = new Vector4i(neighbor.r(), neighbor.g(), neighbor.b(), neighbor.s());
                        r = (byte) Math.max(r, neighbor.r()-1);
                        g = (byte) Math.max(g, neighbor.g()-1);
                        b = (byte) Math.max(b, neighbor.b()-1);
                        s = (byte) Math.max(s, neighbor.s()-1);
                    } else {
                        neighborPositions[i] = null;
                    }
                } else {
                    neighborPositions[i] = null;
                }
            }
            Vector3i chunkPos = new Vector3i(pos.x/chunkSize, pos.y/chunkSize, pos.z/chunkSize);
            int condensedPos = World.condensePos(pos.x, pos.z);
            World.region1Chunks[World.condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(World.condenseLocalPos(pos.x-(chunkPos.x*chunkSize), pos.y-(chunkPos.y*chunkSize), pos.z-(chunkPos.z*chunkSize)), new Block(block.typeId(), block.subtypeId(), r, g, b, (byte) (pos.y > World.heightmap[condensedPos] ? 20 : s)), pos);
            for (byte i = 0; i < 6; i++) {
                if (neighborPositions[i] != null) {
                    if (isDarker(r, g, b, s, neighborLights[i])) {
                        if (clean) {
                            World.queueLightUpdate(neighborPositions[i]);
                        } else {
                            updateLight(neighborPositions[i], World.getBlock(neighborPositions[i]), false);
                        }
                    }
                }
            }
            return r << 16 | g << 8 | b | s << 24;
        }
        return 0;
    }
    public static int updateLight(Vector3i pos, Block block) {
        return updateLight(pos, block, true);
    }

    public static boolean isDarker(byte r, byte g, byte b, byte s, Vector4i darker) {
        return r-1 > darker.x() || g-1 > darker.y() || b-1 > darker.z() || s-1 > darker.w();
    }
}
