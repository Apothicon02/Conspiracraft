package org.conspiracraft.game.world;

import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.conspiracraft.game.world.World.*;

public class LightHelper {
    private static final Vector3i[] neighborPositions = new Vector3i[6];
    private static final Vector4i[] neighborLights = new Vector4i[6];
    public static int updateLight(Vector3i pos, Vector2i block, Vector4i light, boolean clean) {
        BlockType blockType = BlockTypes.blockTypeMap.get(block.x);
        int corners = getCorner(pos.x, pos.y, pos.z);
        if (!blocksLight(block.x, corners) || blockType instanceof LightBlockType) {
            int r = light.x();
            int g = light.y();
            int b = light.z();
            int s = (pos.y > World.heightmap[Utils.condensePos(pos.x, pos.z)] ? 20 : light.w);
            int i = 0;
            for (Vector3i neighborPos : new Vector3i[]{
                    new Vector3i(pos.x, pos.y, pos.z+1), new Vector3i(pos.x+1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z-1),
                    new Vector3i(pos.x-1, pos.y, pos.z), new Vector3i(pos.x, pos.y+1, pos.z), new Vector3i(pos.x, pos.y-1, pos.z)
            }) {
                Vector2i neighbor = World.getBlock(neighborPos);
                Vector4i neighborLight = World.getLight(neighborPos);
                if (neighbor != null && neighborLight != null) {
                    BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.x);
                    if (!blocksLight(neighbor.x, getCorner(neighborPos.x, neighborPos.y, neighborPos.z)) || neighborBlockType instanceof LightBlockType) {
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
                i++;
            }
            Vector3i chunkPos = new Vector3i(pos.x>> 4, pos.y>> 4, pos.z>> 4);
            World.chunks[Utils.condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setLight(new Vector3i(pos.x-(chunkPos.x*chunkSize), pos.y-(chunkPos.y*chunkSize), pos.z-(chunkPos.z*chunkSize)), new Vector4i(r, g, b, s), pos);
            int e = 0;
            for (Vector3i neighborPos : neighborPositions) {
                if (neighborPos != null) {
                    if (isDarker(r, g, b, s, neighborLights[e])) {
                        if (clean) {
                            World.queueLightUpdate(neighborPos);
                        } else {
                            updateLight(neighborPos, World.getBlock(neighborPos), neighborLights[e], false);
                        }
                    }
                }
                e++;
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