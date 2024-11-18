package org.conspiracraft.game;

import org.conspiracraft.game.blocks.Light;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.conspiracraft.engine.FastNoiseLite;
import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.Block;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class World {
    public static int seaLevel = 12;
    public static int size = 808;
    public static int fullSize = (size+1)*(size+1)*(size+1);

    public static Map<Integer, Block> region1Blocks = new HashMap<>(Map.of());
    public static IntBuffer region1Buffer;
    public static IntBuffer region1LightingBuffer;

    public static List<Vector3i> lightQueue = new ArrayList<>(List.of());

    public static void init() {
        clearWorld();
        generateWorld();
    }

    public static void tick() {
        updateLight(lightQueue.getFirst());
        lightQueue.removeFirst();
    }

    public static void regenerateWorld() {
        clearWorld();
        generateWorld();
    }

    public static void clearWorld() {
        int[] empty = new int[fullSize];
        region1Buffer = BufferUtils.createIntBuffer(fullSize).put(empty).flip();
        region1LightingBuffer = BufferUtils.createIntBuffer(fullSize).put(empty).flip();
        empty = null;
        Renderer.worldChanged = true;
        Renderer.lightChanged = true;
    }

    public static void generateWorld() {
        FastNoiseLite noise = new FastNoiseLite((int) (Math.random()*9999));
        noise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        for (int x = 1; x <= size; x++) {
            for (int z = 1; z <= size; z++) {
                float baseCellularNoise = noise.GetNoise(x, z);
                boolean upmost = true;
                for (int y = 30; y >= 1; y--) {
                    double baseGradient = ConspiracraftMath.gradient(y, 30, 1, 2, -1);
                    if (baseCellularNoise + baseGradient > 0) {
                        if (upmost && y >= seaLevel) {
                            setBlock(x, y, z, 2, 0);
                            double torchChance = Math.random();
                            if (torchChance > 0.9999d) {
                                setBlock(x, y+1, z, torchChance > 0.99995d ? 6 : 7, 0);
                                lightQueue.add(new Vector3i(x, y+1, z));
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
                        } else {
                            setBlock(x, y, z, 0, 0);
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

    public static Block getBlock(Vector3i blockPos) {
        if (blockPos.x > 0 && blockPos.x <= size && blockPos.z > 0 && blockPos.z <= size) {
            return region1Blocks.get(condensePos(blockPos.x, blockPos.y, blockPos.z));
        }
        return null;
    }

    public static Block setBlock(int x, int y, int z, int blockType, int blockSubtype) {
        if (x > 0 && x <= size && z > 0 && z <= size) {
            int pos = condensePos(x, y, z);
            int blockId = Utils.packInts(blockType, blockSubtype);
            region1Buffer.put(pos, blockId);
            Block block = new Block((short)(blockType), (short)(blockSubtype));
            region1Blocks.put(pos, block);
            Renderer.worldChanged = true;
            return block;
        } else {
            return null;
        }
    }

    public static Light getLight(int x, int y, int z) {
        Block block = region1Blocks.get(x + y * size + z * size * size);
        if (block != null) {
            Light blockLight = block.light;
            if (blockLight == null) {
                return new Light(0, 0, 0,0);
            } else {
                return blockLight;
            }
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
    }
}
