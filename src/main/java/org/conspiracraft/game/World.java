package org.conspiracraft.game;

import org.conspiracraft.game.blocks.Light;

import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.engine.FastNoiseLite;
import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.Block;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.List;

public class World {
    public static int seaLevel = 137;
    public static int size = 512;
    public static int fullSize = (size+1)*(size+1)*(size+1);

    public static Block[] region1Blocks = new Block[fullSize];
    public static int[] heightmap = new int[(size+1)*(size+1)];

    public static List<Vector3i> lightQueue = new ArrayList<>(List.of());
    public static List<Vector4i> blockQueue = new ArrayList<>(List.of());

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
                if (basePerlinNoise > -0.3 && basePerlinNoise < 0.3) {
                    double random = Math.random();
                    if (random > 0.9995) {
                        setBlock(x, (int) ((basePerlinNoise*12)+299), z, 8, 0, false, true);
                    } else if (random > 0.999) {
                        setBlock(x, (int) ((basePerlinNoise*12)+299), z, 9, 0, false, true);
                    }
                }
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
                            setBlock(x, y, z, 2, 0, false, true);
                            double torchChance = Math.random();
                            if (torchChance > 0.9999d) {
                                setBlock(x, y+1, z, torchChance > 0.99995d ? 6 : 7, 0, false, true);
                            } else {
                                setBlock(x, y+1, z, 4 + (Math.random() > 0.98f ? 1 : 0), (int)(Math.random()*3), false, true);
                            }
                            upmost = false;
                        } else {
                            setBlock(x, y, z, 3, 0, false, true);
                            if (upmost) {
                                boolean replace = false;
                                int seaFloor = y+1;
                                if (seaLevelNegativeDensity-0.1 < 0) {
                                    seaFloor = 1;
                                    replace = true;
                                }
                                for (int waterY = seaFloor; waterY <= seaLevel; waterY++) {
                                    setBlock(x, waterY, z, 1, 0, replace, true);
                                }
                            }
                        }
                    } else {
                        setBlock(x, y, z, 0, 0, false, true);
                    }
                }
                updateSunlight(x, z);
            }
        }
        Renderer.worldChanged = true;
    }

    public static int condensePos(int x, int z) {
        return x * size + z;
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

    public static void setBlock(int x, int y, int z, int blockTypeId, int blockSubtypeId, boolean replace, boolean instant) {
        if (x > 0 && x <= size && z > 0 && z <= size) {
            int pos = condensePos(x, y, z);
            Block existing = region1Blocks[pos];
            if (replace || (existing == null || existing.blockTypeId == 0)) {
                int blockId = Utils.packInts(blockTypeId, blockSubtypeId);
                if (instant) {
                    Vector3i BlockPos = new Vector3i(x, y, z);
                    Block block = new Block((short) blockTypeId, (short) blockSubtypeId);
                    BlockType blockType = BlockTypes.blockTypeMap.get((short)(blockTypeId));
                    if (blockType instanceof LightBlockType) {
                        lightQueue.add(BlockPos);
                    }
                    region1Blocks[pos] = block;
                    updateHeightmap(x, blockType.isTransparent ? 0 : y, z);
                } else {
                    blockQueue.add(new Vector4i(x, y, z, blockId));
                    updateHeightmap(x, BlockTypes.blockTypeMap.get((short)(blockTypeId)).isTransparent ? 0 : y, z);
                    updateSunlight(x, z);
                }
            }
        }
    }

    public static void updateSunlight(int x, int z) {
        boolean shadow = false;
        int heightmapPos = heightmap[condensePos(x, z)];
        for (int blockY = 512; blockY > 0; blockY--) {
            Block block = getBlock(new Vector3i(x, blockY, z));
            if (block != null && BlockTypes.blockTypeMap.get(block.blockTypeId).isTransparent) {
                int sun = blockY > heightmapPos ? 12 : 0;
                if (block.light == null) {
                    block.light = new Light(0, 0, 0, sun);
                } else {
                    block.light.s(sun);
                }
                if (sun == 0 && !shadow) {
                    shadow = true;
                    lightQueue.add(new Vector3i(x, blockY, z));
                }
            }
        }
    }

    public static void updateHeightmap(int x, int y, int z) {
        int horizontalPos = condensePos(x, z);
        if (heightmap[horizontalPos] < y) {
            heightmap[horizontalPos] = y;
        }
    }
    public static void updateHeightmap(Vector3i pos) {
        updateHeightmap(pos.x, pos.y, pos.z);
    }

    public static Light getLight(int x, int y, int z) {
        Block block = getBlock(new Vector3i(x, y, z));
        if (block != null) {
            if (BlockTypes.blockTypeMap.get(block.blockTypeId).isTransparent) {
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
