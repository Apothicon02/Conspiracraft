package org.terraflat.game;

import org.lwjgl.BufferUtils;
import org.terraflat.engine.FastNoiseLite;
import org.terraflat.engine.TerraflatMath;
import org.terraflat.engine.Utils;

import java.nio.IntBuffer;

public class World {
    public static int seaLevel = 12;
    public static int size = 808;

    public static IntBuffer region1Buffer;
    public static IntBuffer region2Buffer;
    public static IntBuffer region3Buffer;
    public static IntBuffer region4Buffer;

    public static void init() {
        int fullSize = (size+1)*(size+1)*(size+1);
        region1Buffer = BufferUtils.createIntBuffer(fullSize).put(new int[fullSize]).flip();;
        FastNoiseLite noise = new FastNoiseLite((int) (Math.random()*9999));
        noise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        for (int x = 1; x <= size; x++) {
            for (int z = 1; z <= size; z++) {
                float baseCellularNoise = noise.GetNoise(x, z);
                boolean upmost = true;
                for (int y = 30; y >= 1; y--) {
                    double baseGradient = TerraflatMath.gradient(y, 30, 1, 2, -1);
                    if (baseCellularNoise + baseGradient > 0) {
                        if (upmost && y >= seaLevel) {
                            setBlock(x, y, z, 2, 0);
                            setBlock(x, y+1, z, 4 + (Math.random() > 0.98f ? 1 : 0), (int)(Math.random()*3));
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
        int pos = x + y * size + z * size * size;
        region1Buffer.put(pos, Utils.packInts(blockType, blockSubtype));
    }
}
