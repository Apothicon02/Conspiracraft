package org.conspiracraft.game.world;

import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.game.world.World.*;

public class BlockHelper {
    private static final Vector3i[] neighborBlocks = new Vector3i[6];
    private static final Vector3i[] neighborPositions = new Vector3i[6];
    private static final Vector4i[] neighborLights = new Vector4i[6];
    public static int updateLight(Vector3i pos, Vector2i block, Vector4i light, boolean clean) {
        BlockType blockType = BlockTypes.blockTypeMap.get(block.x);
        int corners = getCorner(pos.x, pos.y, pos.z);
        if (!isSolid(block.x, corners) || blockType instanceof LightBlockType) {
            int r = light.x();
            int g = light.y();
            int b = light.z();
            int s = light.w();
            neighborBlocks[0] = new Vector3i(pos.x, pos.y, pos.z+1);
            neighborBlocks[1] = new Vector3i(pos.x+1, pos.y, pos.z);
            neighborBlocks[2] = new Vector3i(pos.x, pos.y, pos.z-1);
            neighborBlocks[3] = new Vector3i(pos.x-1, pos.y, pos.z);
            neighborBlocks[4] = new Vector3i(pos.x, pos.y+1, pos.z);
            neighborBlocks[5] = new Vector3i(pos.x, pos.y-1, pos.z);

            for (byte i = 0; i < 6; i++) {
                Vector3i neighborPos = neighborBlocks[i];
                Vector2i neighbor = World.getBlock(neighborPos);
                Vector4i neighborLight = World.getLight(neighborPos);
                if (neighbor != null && neighborLight != null) {
                    BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.x);
                    if (!isSolid(neighbor.x, getCorner(neighborPos.x, neighborPos.y, neighborPos.z)) || neighborBlockType instanceof LightBlockType) {
                        neighborPositions[i] = neighborPos;
                        neighborLights[i] = new Vector4i(neighborLight.x(), neighborLight.y(), neighborLight.z(), neighborLight.w());
                        r = Math.max(r, neighborLight.x()-1);
                        g = Math.max(g, neighborLight.y()-1);
                        b = Math.max(b, neighborLight.z()-1);
                        s = Math.max(s, neighborLight.w()-1);
                    } else {
                        neighborPositions[i] = null;
                    }
                } else {
                    neighborPositions[i] = null;
                }
            }
            Vector3i chunkPos = new Vector3i(pos.x/chunkSize, pos.y/chunkSize, pos.z/chunkSize);
            int condensedPos = Utils.condensePos(pos.x, pos.z);
            World.chunks[Utils.condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setLight(Utils.condenseLocalPos(pos.x-(chunkPos.x*chunkSize), pos.y-(chunkPos.y*chunkSize), pos.z-(chunkPos.z*chunkSize)), new Vector4i(r, g, b, (pos.y > World.heightmap[condensedPos] ? 20 : s)), pos);
            for (byte i = 0; i < 6; i++) {
                if (neighborPositions[i] != null) {
                    if (isDarker(r, g, b, s, neighborLights[i])) {
                        if (clean) {
                            World.queueLightUpdate(neighborPositions[i]);
                        } else {
                            updateLight(neighborPositions[i], World.getBlock(neighborPositions[i]), neighborLights[i], false);
                        }
                    }
                }
            }
            return r << 16 | g << 8 | b | s << 24;
        }
        return 0;
    }
    public static int updateLight(Vector3i pos, Vector2i block, Vector4i light) {
        return updateLight(pos, block, light, true);
    }

    public static boolean isDarker(int r, int g, int b, int s, Vector4i darker) {
        return r-1 > darker.x() || g-1 > darker.y() || b-1 > darker.z() || s-1 > darker.w();
    }
}