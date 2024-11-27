package org.conspiracraft.game.blocks;

import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.World;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.joml.Vector2i;
import org.joml.Vector3i;


import java.util.HashMap;
import java.util.Map;

public class Block {
    public int blockTypeId;
    public int blockSubtypeId;
    public Light light;

    public Block(int type, int subtype) {
        blockTypeId = type;
        blockSubtypeId = subtype;
    }
    public Block(int id) {
        Vector2i unpackedId = Utils.unpackInt(id);
        blockTypeId = unpackedId.x;
        blockSubtypeId = unpackedId.y;
    }

    public int id() {
        return Utils.packInts(blockTypeId, blockSubtypeId);
    }

    public int updateLight(Vector3i pos) {
        BlockType blockType = BlockTypes.blockTypeMap.get(blockTypeId);
        if (blockType.isTransparent || blockType instanceof LightBlockType) {
            if (light == null) {
                light = new Light(0, 0, 0,0);
            }
            if (blockType instanceof LightBlockType) {
                light = ((LightBlockType) blockType).emission;
            }
            Vector3i[] neighborBlocks = new Vector3i[]{
                    new Vector3i(pos.x, pos.y, pos.z + 1),
                    new Vector3i(pos.x + 1, pos.y, pos.z),
                    new Vector3i(pos.x, pos.y, pos.z - 1),
                    new Vector3i(pos.x - 1, pos.y, pos.z),
                    new Vector3i(pos.x, pos.y + 1, pos.z),
                    new Vector3i(pos.x, pos.y - 1, pos.z)
            };
            Map<Vector3i, Light> neighborLights = new HashMap<>(Map.of());
            for (int i = 0; i < neighborBlocks.length; i++) {
                Vector3i blockPos = neighborBlocks[i];
                Block neighbor = World.getBlock(blockPos);
                if (neighbor != null) {
                    BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.blockTypeId);
                    if (neighborBlockType.isTransparent || neighborBlockType instanceof LightBlockType) {
                        Light neighborLight = neighbor.light;
                        if (neighborLight == null) {
                            neighborLight = new Light(0, 0, 0, 0);
                        }
                        neighborLights.put(blockPos, neighborLight);
                    }
                }
            }
            Light maxNeighborLight = new Light(0, 0, 0, 0);
            neighborLights.forEach((Vector3i neighborPos, Light neighborLight) -> {
                maxNeighborLight.r(Math.max(maxNeighborLight.r(), neighborLight.r()));
                maxNeighborLight.g(Math.max(maxNeighborLight.g(), neighborLight.g()));
                maxNeighborLight.b(Math.max(maxNeighborLight.b(), neighborLight.b()));
                maxNeighborLight.s(Math.max(maxNeighborLight.s(), neighborLight.s()));
            });
            light = new Light(Math.max(light.r(), maxNeighborLight.r()-1), Math.max(light.g(), maxNeighborLight.g()-1), Math.max(light.b(), maxNeighborLight.b()-1), Math.max(light.s(), maxNeighborLight.s()-1));
            neighborLights.forEach((Vector3i neighborPos, Light neighborLight) -> {
                if (isDarker(neighborLight)) {
                    World.queueLightUpdate(neighborPos, false);
                }
            });
            return Utils.lightToInt(light);
        }
        return 0;
    }

    public boolean isDarker(Light darker) {
        return light.r()-1 > darker.r() || light.g()-1 > darker.g() || light.b()-1 > darker.b() || light.s()-1 > darker.s();
    }
}