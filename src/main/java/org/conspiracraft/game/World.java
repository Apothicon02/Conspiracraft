package org.conspiracraft.game;

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

    public static void setBlock(int x, int y, int z, int blockType, int blockSubtype) {
        if (x <= size && z <= size) {
            int pos = x + y * size + z * size * size;
            int blockId = Utils.packInts(blockType, blockSubtype);
            region1Buffer.put(pos, blockId);
            region1Blocks.put(pos, new Block(blockId));
        }
    }
}
