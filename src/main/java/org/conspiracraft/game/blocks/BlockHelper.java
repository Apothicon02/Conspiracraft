package org.conspiracraft.game.blocks;

import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.world.World;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class BlockHelper {
    public static Block litAirBlock = new Block(0, 0, (byte) 0, (byte) 0, (byte) 0, (byte) 14);
    public static Block unlitAirBlock = new Block(0, 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0);

    public static int updateLight(Vector3i pos, Block block) {
        BlockType blockType = BlockTypes.blockTypeMap.get(block.typeId());
        if (blockType.isTransparent || blockType instanceof LightBlockType) {
            byte r = block.r();
            byte g = block.g();
            byte b = block.b();
            byte s = block.s();
            Vector3i[] neighborBlocks = new Vector3i[]{
                    new Vector3i(pos.x, pos.y, pos.z + 1),
                    new Vector3i(pos.x + 1, pos.y, pos.z),
                    new Vector3i(pos.x, pos.y, pos.z - 1),
                    new Vector3i(pos.x - 1, pos.y, pos.z),
                    new Vector3i(pos.x, pos.y + 1, pos.z),
                    new Vector3i(pos.x, pos.y - 1, pos.z)
            };
            Map<Vector3i, Vector4i> neighborLights = new HashMap<>(Map.of());
            for (int i = 0; i < neighborBlocks.length; i++) {
                Vector3i blockPos = neighborBlocks[i];
                Block neighbor = World.getBlock(blockPos);
                if (neighbor != null) {
                    BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.typeId());
                    if (neighborBlockType.isTransparent || neighborBlockType instanceof LightBlockType) {
                        neighborLights.put(blockPos, new Vector4i(neighbor.r(), neighbor.g(), neighbor.b(), neighbor.s()));
                    }
                }
            }
            AtomicReference<Byte> maxNeighborR = new AtomicReference<>((byte) 0);
            AtomicReference<Byte> maxNeighborG = new AtomicReference<>((byte) 0);
            AtomicReference<Byte> maxNeighborB = new AtomicReference<>((byte) 0);
            AtomicReference<Byte> maxNeighborS = new AtomicReference<>((byte) 0);
            neighborLights.forEach((Vector3i neighborPos, Vector4i neighborLight) -> {
                maxNeighborR.set((byte) Math.max(maxNeighborR.get(), neighborLight.x));
                maxNeighborG.set((byte) Math.max(maxNeighborG.get(), neighborLight.y));
                maxNeighborB.set((byte) Math.max(maxNeighborB.get(), neighborLight.z));
                maxNeighborS.set((byte) Math.max(maxNeighborS.get(), neighborLight.w));
            });
            r = (byte) Math.max(r, maxNeighborR.get()-1);
            g = (byte) Math.max(g, maxNeighborG.get()-1);
            b = (byte) Math.max(b, maxNeighborB.get()-1);
            s = (byte) Math.max(s, maxNeighborS.get()-1);
            final Vector4i light = new Vector4i(r, g, b, s);
            neighborLights.forEach((Vector3i neighborPos, Vector4i neighborLight) -> {
                if (isDarker(light, neighborLight)) {
                    Block neighbor = World.getBlock(neighborPos);
                    Vector3i chunkPos = new Vector3i(neighborPos.x/16, neighborPos.y/16, neighborPos.z/16);
                    World.region1Chunks[World.condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(World.condenseLocalPos(neighborPos.x-(chunkPos.x*16), neighborPos.y-(chunkPos.y*16), neighborPos.z-(chunkPos.z*16)), new Block(neighbor.typeId(), neighbor.subtypeId(), (byte) light.x, (byte) light.y, (byte) light.z, (byte) light.w));
                    World.queueLightUpdate(neighborPos, false);
                }
            });
            return Utils.vector4iToInt(light);
        }
        return 0;
    }

    public static boolean isDarker(Vector4i brighter, Vector4i darker) {
        return brighter.x()-1 > darker.x() || brighter.y()-1 > darker.y() || brighter.z()-1 > darker.z() || brighter.w()-1 > darker.w();
    }
}
