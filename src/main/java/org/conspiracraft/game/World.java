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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class World {
    public static int seaLevel = 137;
    public static int size = 512;
    public static int fullSize = (size+1)*(size+1)*(size+1);

    public static Block[] region1Blocks = new Block[fullSize];
    public static Vector2i[] heightmap = new Vector2i[(size+1)*(size+1)];

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
    public static Block getBlock(int x, int y, int z) {
        return getBlock(new Vector3i(x, y, z));
    }
    public static Block getBlock(float x, float y, float z) {
        return getBlock(new Vector3i((int) x, (int) y, (int) z));
    }

    public static void setBlock(int x, int y, int z, int blockTypeId, int blockSubtypeId, boolean replace, boolean instant) {
        if (x > 0 && x <= size && z > 0 && z <= size) {
            int pos = condensePos(x, y, z);
            Block existing = region1Blocks[pos];
            if (replace || (existing == null || existing.blockTypeId == 0)) {
                int blockId = Utils.packInts(blockTypeId, blockSubtypeId);
                if (instant) {
                    Vector3i BlockPos = new Vector3i(x, y, z);
                    Block block = new Block(blockTypeId, blockSubtypeId);
                    BlockType blockType = BlockTypes.blockTypeMap.get(blockTypeId);
                    if (blockType instanceof LightBlockType) {
                        queueLightUpdate(BlockPos, false);
                    }
                    region1Blocks[pos] = block;
                    updateHeightmap(x, y, z, blockType.isTransparent);
                } else {
                    blockQueue.add(new Vector4i(x, y, z, blockId));
                }
            }
        }
    }

    public static void updateSunlight(int x, int z) {
        boolean shadow = false;
        Vector2i heightmapPos = heightmap[condensePos(x, z)];
        for (int blockY = 511; blockY >= 0; blockY--) {
            Block block = getBlock(new Vector3i(x, blockY, z));
            if (block != null && BlockTypes.blockTypeMap.get(block.blockTypeId).isTransparent) {
                int sun = blockY > heightmapPos.y ? 12 : 0;
                if (block.light == null) {
                    block.light = new Light(0, 0, 0, sun);
                } else {
                    block.light.s(sun);
                }
                if (sun == 0 && !shadow) {
                    shadow = true;
                    queueLightUpdate(new Vector3i(x, blockY, z), false);
                }
            }
        }
        boolean shadowInv = false;
        for (int blockY = 0; blockY < 512; blockY++) {
            Block block = getBlock(new Vector3i(x, blockY, z));
            if (block != null && BlockTypes.blockTypeMap.get(block.blockTypeId).isTransparent) {
                int sun = blockY < heightmapPos.y ? 6 : 0;
                if (block.light == null) {
                    block.light = new Light(0, 0, 0, sun);
                } else {
                    block.light.s(Math.max(sun, block.light.s()));
                }
                if (sun == 0 && !shadowInv) {
                    shadowInv = true;
                    queueLightUpdate(new Vector3i(x, blockY, z), false);
                }
            }
        }
    }

    public static void updateHeightmap(int x, int y, int z, boolean transparent) {
        int horizontalPos = condensePos(x, z);
        if (heightmap[horizontalPos] == null) {
            heightmap[horizontalPos] = new Vector2i(0, 0);
        }
        if (!transparent) {
            if (heightmap[horizontalPos].x > y) {
                heightmap[horizontalPos].x = y;
            }
            if (heightmap[horizontalPos].y < y) {
                heightmap[horizontalPos].y = y;
            }
        } else {
            if (heightmap[horizontalPos].x == y) {
                //starting at y, scan down until finding a solid block, then make that the new heightmap pos.
            }
            if (heightmap[horizontalPos].y == y) {
                //starting at y, scan up until finding a solid block, then make that the new heightmap pos.
            }
        }
    }
    public static void updateHeightmap(Vector3i pos, boolean transparent) {
        updateHeightmap(pos.x, pos.y, pos.z, transparent);
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

    public static void recalculateLight(Vector3i pos, Light light) {
        for (Vector3i neighborPos : new Vector3i[]{
                new Vector3i(pos.x, pos.y, pos.z + 1),
                new Vector3i(pos.x + 1, pos.y, pos.z),
                new Vector3i(pos.x, pos.y, pos.z - 1),
                new Vector3i(pos.x - 1, pos.y, pos.z),
                new Vector3i(pos.x, pos.y + 1, pos.z),
                new Vector3i(pos.x, pos.y - 1, pos.z)
        }) {
            Block neighbor = World.getBlock(neighborPos);
            if (neighbor != null) {
                BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.blockTypeId);
                if (neighborBlockType.isTransparent || neighborBlockType instanceof LightBlockType) {
                    Light neighborLight = neighbor.light;
                    if (neighborLight != null) {
                        if ((neighborLight.r() > 0 && neighborLight.r() < light.r()) || (neighborLight.g() > 0 && neighborLight.g() < light.g()) || (neighborLight.b() > 0 && neighborLight.b() < light.b()) || (neighborLight.s() > 0 && neighborLight.s() < light.s())) {
                            neighbor.light = new Light(0, 0, 0, neighborLight.s());
                            recalculateLight(neighborPos, neighborLight);
                        }
                        queueLightUpdate(pos, true);
                    }
                }
            }
        }
    }

    public static void queueLightUpdate(Vector3i pos, boolean priority) {
        if (!lightQueue.contains(pos)) {
            if (priority) {
                lightQueue.addFirst(pos);
            } else {
                lightQueue.addLast(pos);
            }
        }
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
