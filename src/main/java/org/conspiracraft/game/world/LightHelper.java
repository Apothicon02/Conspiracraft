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
    public static int updateLight(Vector3i pos, Vector2i block, Vector4i light, int stack) {
        stack++;
        BlockType blockType = BlockTypes.blockTypeMap.get(block.x);
        int corners = getCorner(pos.x, pos.y, pos.z);
        boolean isLight = blockType instanceof LightBlockType;
        if (!blocksLight(block.x, corners) || isLight) {
            int r = Math.max(light.x(), isLight ? ((LightBlockType) blockType).r : 0);
            int g = Math.max(light.y(), isLight ? ((LightBlockType) blockType).g : 0);
            int b = Math.max(light.z(), isLight ? ((LightBlockType) blockType).b : 0);
            int s = (pos.y > World.heightmap[Utils.condensePos(pos.x, pos.z)] ? 20 : light.w);
            for (Vector3i neighborPos : new Vector3i[]{
                    new Vector3i(pos.x, pos.y, pos.z+1), new Vector3i(pos.x+1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z-1),
                    new Vector3i(pos.x-1, pos.y, pos.z), new Vector3i(pos.x, pos.y+1, pos.z), new Vector3i(pos.x, pos.y-1, pos.z)
            }) {
                Vector2i neighbor = World.getBlock(neighborPos);
                Vector4i neighborLight = World.getLight(neighborPos);
                if (neighborLight != null) {
                    BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.x);
                    boolean isNLight = neighborBlockType instanceof LightBlockType;
                    if (!blocksLight(neighbor.x, getCorner(neighborPos.x, neighborPos.y, neighborPos.z)) || isNLight) {
                        r = Math.max(r, Math.max(neighborLight.x(), isNLight ? ((LightBlockType) neighborBlockType).r : 0)-1);
                        g = Math.max(g, Math.max(neighborLight.y(), isNLight ? ((LightBlockType) neighborBlockType).g : 0)-1);
                        b = Math.max(b, Math.max(neighborLight.z(), isNLight ? ((LightBlockType) neighborBlockType).b : 0)-1);
                        s = Math.max(s, neighborLight.w()-1);
                    }
                }
            }
            Vector3i chunkPos = new Vector3i(pos.x>> 4, pos.y>> 4, pos.z>> 4);
            World.chunks[Utils.condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setLight(new Vector3i(pos.x-(chunkPos.x*chunkSize), pos.y-(chunkPos.y*chunkSize), pos.z-(chunkPos.z*chunkSize)), new Vector4i(r, g, b, s), pos);
            for (Vector3i neighborPos : new Vector3i[]{
                    new Vector3i(pos.x, pos.y, pos.z+1), new Vector3i(pos.x+1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z-1),
                    new Vector3i(pos.x-1, pos.y, pos.z), new Vector3i(pos.x, pos.y+1, pos.z), new Vector3i(pos.x, pos.y-1, pos.z)
            }) {
                Vector4i nLight = getLight(neighborPos);
                if (nLight != null) {
                    if (isDarker(r, g, b, s, nLight)) {
                        if (stack < 10000) {
                            updateLight(neighborPos, World.getBlock(neighborPos), nLight, stack);
                        }
                    }
                }
            }
            return r << 16 | g << 8 | b | s << 24;
        }
        return 0;
    }
    public static int updateLight(Vector3i pos, Vector2i block, Vector4i light) {
        return updateLight(pos, block, light, 0);
    }
    public static boolean isDarker(int r, int g, int b, int s, Vector4i darker) {
        return r-2 > darker.x() || g-2 > darker.y() || b-2 > darker.z() || s-2 > darker.w();
    }
}