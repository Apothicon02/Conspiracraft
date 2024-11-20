package org.conspiracraft.game.blocks;

import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.World;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.types.Vector3s;

import java.util.HashMap;
import java.util.Map;

public class Block {
    public BlockType blockType;
    public short blockSubtype;
    public Light light;

    public Block(short type, short subtype) {
        blockType = BlockTypes.blockTypeMap.get(type);
        blockSubtype = subtype;
    }

    public int updateLight(Vector3s pos) {
        if (blockType.isTransparent) {
            if (light == null) {
                if (blockType instanceof LightBlockType) {
                    light = ((LightBlockType) blockType).emission;
                } else {
                    light = new Light(0, 0, 0,0);
                }
            }
            Vector3s[] neighborBlocks = new Vector3s[]{
                    new Vector3s(pos.x, pos.y, pos.z + 1),
                    new Vector3s(pos.x + 1, pos.y, pos.z),
                    new Vector3s(pos.x, pos.y, pos.z - 1),
                    new Vector3s(pos.x - 1, pos.y, pos.z),
                    new Vector3s(pos.x, pos.y + 1, pos.z),
                    new Vector3s(pos.x, pos.y - 1, pos.z)
            };
            Map<Vector3s, Light> neighborLights = new HashMap<>(Map.of());
            for (int i = 0; i < neighborBlocks.length; i++) {
                Vector3s blockPos = neighborBlocks[i];
                Light neighborLight = World.getLight(blockPos);
                if (neighborLight != null) {
                    neighborLights.put(blockPos, neighborLight);
                }
            }
            Light maxNeighborLight = new Light(0, 0, 0, 0);
            neighborLights.forEach((Vector3s neighborPos, Light neighborLight) -> {
                maxNeighborLight.r(Math.max(maxNeighborLight.r(), neighborLight.r()));
                maxNeighborLight.g(Math.max(maxNeighborLight.g(), neighborLight.g()));
                maxNeighborLight.b(Math.max(maxNeighborLight.b(), neighborLight.b()));
                maxNeighborLight.s(Math.max(maxNeighborLight.s(), neighborLight.s()));
            });
            light = new Light(Math.max(light.r(), maxNeighborLight.r())- 1, Math.max(light.g(), maxNeighborLight.g())- 1, Math.max(light.b(), maxNeighborLight.b())- 1, Math.max(light.s(), maxNeighborLight.s())- 1);
            neighborLights.forEach((Vector3s neighborPos, Light neighborLight) -> {
                if (isDarker(neighborLight) && !World.lightQueue.contains(neighborPos)) {
                    World.lightQueue.add(neighborPos);
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