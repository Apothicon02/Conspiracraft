package org.conspiracraft.game.blocks;

import org.conspiracraft.game.World;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.joml.Vector3i;

import java.util.Map;

public class Block {
    public BlockType blockType;
    public short blockSubtype;
    public Light light;

    public Block(short type, short subtype) {
        blockType = BlockTypes.blockTypeMap.get(type);
        blockSubtype = subtype;
    }

    public void updateLight(Vector3i pos) {
        if (blockType.isTransparent) {
            if (light == null) {
                light = new Light(0, 0, 0,0);
            }
            Map<Vector3i, Light> neighborLights = Map.of(
                    new Vector3i(pos.x, pos.y, pos.z + 1), World.getLight(pos.x, pos.y, pos.z + 1),
                    new Vector3i(pos.x + 1, pos.y, pos.z), World.getLight(pos.x + 1, pos.y, pos.z),
                    new Vector3i(pos.x, pos.y, pos.z - 1), World.getLight(pos.x, pos.y, pos.z - 1),
                    new Vector3i(pos.x - 1, pos.y, pos.z), World.getLight(pos.x - 1, pos.y, pos.z),
                    new Vector3i(pos.x, pos.y + 1, pos.z), World.getLight(pos.x, pos.y + 1, pos.z),
                    new Vector3i(pos.x, pos.y - 1, pos.z), World.getLight(pos.x, pos.y - 1, pos.z)
            );
            Light maxNeighborLight = new Light(0, 0, 0, 0);
            neighborLights.forEach((Vector3i neighborPos, Light neighborLight) -> {
                maxNeighborLight.r(Math.max(maxNeighborLight.r(), neighborLight.r()));
                maxNeighborLight.g(Math.max(maxNeighborLight.g(), neighborLight.g()));
                maxNeighborLight.b(Math.max(maxNeighborLight.b(), neighborLight.b()));
                maxNeighborLight.s(Math.max(maxNeighborLight.s(), neighborLight.s()));
            });
            if (blockType instanceof LightBlockType) {
                light = ((LightBlockType) blockType).emission;
            }
            light = new Light(Math.max(light.r(), maxNeighborLight.r() - 1), Math.max(light.g(), maxNeighborLight.g() - 1), Math.max(light.b(), maxNeighborLight.b() - 1), Math.max(light.s(), maxNeighborLight.s() - 1));
            World.updateLightBuffer(pos, light);
            neighborLights.forEach((Vector3i neighborPos, Light neighborLight) -> {
                if (isDarker(neighborLight) && World.getBlock(neighborPos) != null && !World.lightQueue.contains(neighborPos)) {
                    World.lightQueue.add(neighborPos);
                }
            });
        }
    }

    public boolean isDarker(Light darker) {
        return light.r()-1 > darker.r() || light.g()-1 > darker.g() || light.b()-1 > darker.b() || light.s()-1 > darker.s();
    }
}