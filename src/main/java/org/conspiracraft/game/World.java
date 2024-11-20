package org.conspiracraft.game;

import org.conspiracraft.game.blocks.Light;

import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.engine.FastNoiseLite;
import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.Block;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class World {
    public static int seaLevel = 137;
    public static int size = 512;
    public static int fullSize = (size+1)*(size+1)*(size+1);

    public static Block[] region1Blocks = new Block[fullSize];

    public static List<Vector3i> lightQueue = new ArrayList<>(List.of());
    public static List<Vector2i> blockQueue = new ArrayList<>(List.of());

    public static void init() {
        clearWorld();
        generateWorld();
    }

    public static void regenerateWorld() {
        clearWorld();
        generateWorld();
    }

    public static void clearWorld() {
        region1Blocks = new Block[fullSize];
        lightQueue = new ArrayList<>(List.of());
        blockQueue = new ArrayList<>(List.of());
    }

    public static void generateWorld() {
        FastNoiseLite cellularNoise = new FastNoiseLite((int) (Math.random()*9999));
        FastNoiseLite noise = new FastNoiseLite((int) (Math.random()*9999));
        cellularNoise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2S);
        Vector2i middle = new Vector2i(size/2, size/2);
        for (int x = 1; x <= size; x++) {
            for (int z = 1; z <= size; z++) {
                int distance = (int)(Vector2i.distance(middle.x, middle.y, x, z));
                float baseCellularNoise = cellularNoise.GetNoise(x, z);
                float basePerlinNoise = noise.GetNoise(x, z);
                double seaLevelNegativeGradient = ConspiracraftMath.gradient(seaLevel, distance, 0, -4, 3);
                double seaLevelNegativeDensity = (basePerlinNoise-1) + seaLevelNegativeGradient;
                boolean upmost = true;
                for (int y = size-1; y >= 1; y--) {
                    double negativeGradient = ConspiracraftMath.gradient(y, distance, 0, -4, 3);
                    double negativeDensity = (basePerlinNoise-1) + negativeGradient;
                    double baseDensity = 0;
                    if (negativeDensity < 0) {
                        baseDensity = 0;
                    } else {
                        double baseGradient = ConspiracraftMath.gradient(y, 157, 126, 2, -1);
                        baseDensity = baseCellularNoise + baseGradient;
                    }
                    if (baseDensity > 0) {
                        if (upmost && y >= seaLevel) {
                            setBlock(x, y, z, 2, 0, false);
                            double torchChance = Math.random();
                            if (torchChance > 0.9999d) {
                                setBlock(x, y+1, z, torchChance > 0.99995d ? 6 : 7, 0, false);
                            } else {
                                setBlock(x, y+1, z, 4 + (Math.random() > 0.98f ? 1 : 0), (int)(Math.random()*3), false);
                            }
                            upmost = false;
                        } else {
                            setBlock(x, y, z, 3, 0, false);
                            if (upmost) {
                                boolean replace = false;
                                int seaFloor = y+1;
                                if (seaLevelNegativeDensity-0.1 < 0) {
                                    seaFloor = 1;
                                    replace = true;
                                }
                                for (int waterY = seaFloor; waterY <= seaLevel; waterY++) {
                                    setBlock(x, waterY, z, 1, 0, replace);
                                }
                            }
                        }
                    } else {
                        setBlock(x, y, z, 0, 0, false);
                    }
                }
            }
        }
        Renderer.worldChanged = true;
    }

    public static int condensePos(int x, int y, int z) {
        return x + y * size + z * size * size;
    }
    public static int condensePos(Vector3i pos) {
        return pos.x + pos.y * size + pos.z * size * size;
    }

    public static Block getBlock(Vector3i blockPos) {
        if (blockPos.x > 0 && blockPos.x <= size && blockPos.z > 0 && blockPos.z <= size) {
            return region1Blocks[condensePos(blockPos.x, blockPos.y, blockPos.z)];
        }
        return null;
    }

    public static Block setBlock(int x, int y, int z, int blockType, int blockSubtype, boolean replace) {
        if (x > 0 && x <= size && z > 0 && z <= size) {
            int pos = condensePos(x, y, z);
            Block existing = region1Blocks[pos];
            if (replace || (existing == null || existing.blockType.equals(BlockTypes.AIR))) {
                int blockId = Utils.packInts(blockType, blockSubtype);
                blockQueue.add(new Vector2i(pos, blockId));
                Block block = new Block((short) (blockType), (short) (blockSubtype));
                region1Blocks[pos] = block;
                if (block.blockType instanceof LightBlockType) {
                    lightQueue.add(new Vector3i(x, y, z));
                }
                return block;
            }
        }
        return null;
    }

    public static Light getLight(int x, int y, int z) {
        Block block = region1Blocks[condensePos(x, y, z)];
        if (block != null) {
            if (block.blockType.isTransparent) {
                Light blockLight = block.light;
                if (blockLight != null) {
                    return blockLight;
                } else {
                    return new Light(0);
                }
            }
        }
        return null;
    }

    public static Light getLight(Vector3i pos) {
        return getLight(pos.x, pos.y, pos.z);
    }

    public static int updateLight(Vector3i pos) {
        Block block = region1Blocks[condensePos(pos)];
        if (block != null) {
            return block.updateLight(pos);
        } else {
            return 0;
        }
    }
}
