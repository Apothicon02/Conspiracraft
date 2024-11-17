package org.conspiracraft.game;

import org.conspiracraft.game.blocks.Light;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.conspiracraft.engine.FastNoiseLite;
import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.Block;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class World {
    public static int seaLevel = 12;
    public static int size = 808;

    public static Map<Integer, Block> region1Blocks = new HashMap<>(Map.of());
    public static IntBuffer region1Buffer;
    public static IntBuffer region1LightingBuffer;

    public static void init() {
        int fullSize = (size+1)*(size+1)*(size+1);
        int[] empty = new int[fullSize];
        region1Buffer = BufferUtils.createIntBuffer(fullSize).put(empty).flip();
        region1LightingBuffer = BufferUtils.createIntBuffer(fullSize).put(empty).flip();
        empty = null;
        FastNoiseLite noise = new FastNoiseLite((int) (Math.random()*9999));
        noise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        for (int x = 1; x <= size*2; x++) {
            for (int z = 1; z <= size*2; z++) {
                float baseCellularNoise = noise.GetNoise(x, z);
                boolean upmost = true;
                for (int y = 30; y >= 1; y--) {
                    double baseGradient = ConspiracraftMath.gradient(y, 30, 1, 2, -1);
                    if (baseCellularNoise + baseGradient > 0) {
                        if (upmost && y >= seaLevel) {
                            setBlock(x, y, z, 2, 0);
                            if (Math.random() > 0.997f) {
                                setBlock(x, y+1, z, 6, 0);
                            } else {
                                setBlock(x, y+1, z, 4 + (Math.random() > 0.98f ? 1 : 0), (int)(Math.random()*3));
                            }
                            upmost = false;
                        } else {
                            setBlock(x, y, z, 3, 0);
                        }
                    } else {
                        if (y <= seaLevel) {
                            setBlock(x, y, z, 1, 0);
                        }
                    }
                }
            }
        }
    }

    public static int condensePos(Vector3i pos) {
        return pos.x + pos.y * size + pos.z * size * size;
    }
    public static int condensePos(int x, int y, int z) {
        return x + y * size + z * size * size;
    }

    public static void setBlock(int x, int y, int z, int blockType, int blockSubtype) {
        if (x <= size && z <= size) {
            int pos = condensePos(x, y, z);
            int blockId = Utils.packInts(blockType, blockSubtype);
            region1Buffer.put(pos, blockId);
            region1Blocks.put(pos, new Block((short)(blockType), (short)(blockSubtype), new Vector3i(x, y, z)));
            Renderer.worldChanged = true;
        }
    }

    public static Light getLight(int x, int y, int z) {
        Block block = region1Blocks.get(x + y * size + z * size * size);
        if (block != null) {
            return block.light;
        } else {
            return new Light(0, 0, 0, 0);
        }
    }

    public static void updateLight(Vector3i pos) {
        Block block = region1Blocks.get(condensePos(pos));
        if (block != null) {
            block.updateLight(pos);
        }
    }

    public static void updateLightBuffer(Vector3i pos, Light light) {
        region1LightingBuffer.put(condensePos(pos), Utils.lightToInt(light));
        Renderer.lightChanged = true;
    }
}
