package org.conspiracraft.game.world;

import org.conspiracraft.game.Noise;
import org.conspiracraft.game.Renderer;

import org.conspiracraft.game.blocks.BlockHelper;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.blocks.Block;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class World {
    public static int seaLevel = 137;
    public static int size = 1024;
    public static int sizeChunks = size/16;
    public static short height = 320;
    public static int heightChunks = size/16;

    public static Chunk[] region1Chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static short[] heightmap = new short[size*size];

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
        //Arrays.fill(region1Chunks, new Chunk());
        lightQueue = new ArrayList<>(List.of());
        blockQueue = new ArrayList<>(List.of());
        Arrays.fill(heightmap, height);
    }

    public static void generateWorld() {
        //FastNoiseLite cellularNoise = new FastNoiseLite((int) (Math.random()*9999));
        //FastNoiseLite noise = new FastNoiseLite((int) (Math.random()*9999));
        //cellularNoise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        //noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2S);
        Vector2i middle = new Vector2i(size/2, size/2);
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int distance = (int)(Vector2i.distance(middle.x, middle.y, x, z)/2);
                float baseCellularNoise = (Noise.blue(Noise.CELLULAR_NOISE.getRGB(x, z))/128)-1;
                float basePerlinNoise = (Noise.blue(Noise.COHERERENT_NOISE.getRGB(x, z))/128)-1;
                double seaLevelNegativeGradient = ConspiracraftMath.gradient(seaLevel, distance, 0, -4, 3);
                double seaLevelNegativeDensity = (basePerlinNoise-1) + seaLevelNegativeGradient;
                if (basePerlinNoise > -0.3 && basePerlinNoise < 0.3) {
                    double random = Math.random();
                    if (random > 0.99995) {
                        setBlock(x, (int) ((basePerlinNoise*12)+299), z, 8, 0, false, true);
                    } else if (random > 0.9999) {
                        setBlock(x, (int) ((basePerlinNoise*12)+299), z, 9, 0, false, true);
                    }
                }
                int surface = height-1;
                boolean upmost = true;
                for (int y = surface; y >= 0; y--) {
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
                            if (torchChance > 0.99997d) {
                                setBlock(x, y+1, z, torchChance > 0.999985d ? 6 : 7, 0, false, true);
                            } else {
                                setBlock(x, y+1, z, 4 + (Math.random() > 0.98f ? 1 : 0), (int)(Math.random()*3), false, true);
                            }
                            surface = y;
                            upmost = false;
                        } else {
                            setBlock(x, y, z, y > surface - 3 ? 3 : 10, 0, false, true);
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
                        if (x % 4 == 0 && z % 4 == 0) { //temporary to improve world load speed during testing.
                            Block block = getBlock(x, y + 1, z);
                            if (block != null) {
                                if (!BlockTypes.blockTypeMap.get(block.typeId()).isTransparent) {
                                    queueLightUpdate(new Vector3i(x, y, z), false);
                                }
                            }
                        }
                    }
                }
                updateHeightmap(x, z, false);
            }
        }
//        int radius = 16;
//        for (int x = 0; x < size; x++) {
//            for (int z = 0; z < size; z++) {
//                if (x > radius+2 && x < size-radius-2 && z > radius+2 && z < size-radius-2) {
//                    FastNoiseLite heightNoise = new FastNoiseLite((int) (Math.random() * 9999));
//                    heightNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
//                    FastNoiseLite horizontalNoise = new FastNoiseLite((int) (Math.random() * 9999));
//                    horizontalNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2S);
//                    float horizontalDensity = horizontalNoise.GetNoise(x/25f, z/25f);
//                    if (horizontalDensity > -0.001 && horizontalDensity < 0.001) {
//                        int height = (int) (heightNoise.GetNoise(x/64f, z/64f) * 256);
//                        if (height < 0) {
//                            height = height/2;
//                        }
//                        int centerY = 100 + height;
//                        for (int rX = x - radius; rX < x + radius; rX++) {
//                            for (int rZ = z - radius; rZ < z + radius; rZ++) {
//                                for (int rY = centerY - radius; rY < centerY + radius; rY++) {
//                                    Block block = getBlock(rX, rY, rZ);
//                                    Block aboveBlock = getBlock(rX, rY+1, rZ);
//                                    Block northBlock = getBlock(rX, rY, rZ+1);
//                                    Block southBlock = getBlock(rX, rY, rZ-1);
//                                    Block eastBlock = getBlock(rX+1, rY, rZ);
//                                    Block westBlock = getBlock(rX-1, rY, rZ);
//                                    if (!(block != null && block.blockTypeId == 1) && !(aboveBlock != null && aboveBlock.blockTypeId == 1) && !(northBlock != null && northBlock.blockTypeId == 1) && !(southBlock != null && southBlock.blockTypeId == 1) && !(eastBlock != null && eastBlock.blockTypeId == 1) && !(westBlock != null && westBlock.blockTypeId == 1)) {
//                                        if (Vector3i.distance(x, centerY, z, rX, rY, rZ) < radius) {
//                                            setBlock(rX, rY, rZ, 0, 0, true, true);
//                                            if (heightmap[condensePos(rX, rZ)] == rY) {
//                                                queueLightUpdate(new Vector3i(rX, rY, rZ), false);
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        for (int x = 0; x < size; x++) {
//            for (int z = 0; z < size; z++) {
//                updateHeightmap(x, z, false);
//            }
//        }
        Renderer.worldChanged = true;
    }

    public static int condensePos(int x, int z) {
        return x * size + z;
    }
    public static int condensePos(int x, int y, int z) {
        return (((x*size)+z)*height)+y;
    }
    public static int condensePos(Vector3i pos) {
        return (((pos.x*size)+pos.z)*height)+pos.y;
    }
    public static int condenseLocalPos(int x, int y, int z) {
        return (((x*16)+z)*16)+y;
    }
    public static int condenseLocalPos(Vector3i pos) {
        return (((pos.x*16)+pos.z)*16)+pos.y;
    }
    public static int condenseChunkPos(Vector3i pos) {
        return (((pos.x*sizeChunks)+pos.z)*heightChunks)+pos.y;
    }
    public static int condenseChunkPos(int x, int y, int z) {
        return (((x*sizeChunks)+z)*heightChunks)+y;
    }

    public static Block getBlock(Vector3i blockPos) {
        if (blockPos.x >= 0 && blockPos.x < size && blockPos.z >= 0 && blockPos.z < size && blockPos.y >= 0 && blockPos.y < height) {
            Vector3i chunkPos = new Vector3i(blockPos.x/16, blockPos.y/16, blockPos.z/16);
            int condensedChunkPos = condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z);
            Chunk chunk = region1Chunks[condensedChunkPos];
            if (chunk == null) {
                region1Chunks[condensedChunkPos] = new Chunk();
                chunk = region1Chunks[condensedChunkPos];
            }
            return chunk.getBlock(condenseLocalPos(blockPos.x-(chunkPos.x*16), blockPos.y-(chunkPos.y*16), blockPos.z-(chunkPos.z*16)));
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
        if (x > 0 && x < size && z > 0 && z < size && y > 0 && y < height) {
            Block existing = getBlock(x, y, z);
            if (replace || (existing == null || existing.typeId() == 0)) {
                int blockId = Utils.packInts(blockTypeId, blockSubtypeId);
                if (instant) {
                    Vector3i chunkPos = new Vector3i(x/16, y/16, z/16);
                    region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(x-(chunkPos.x*16), y-(chunkPos.y*16), z-(chunkPos.z*16)), new Block(blockTypeId, blockSubtypeId));
                    if (BlockTypes.blockTypeMap.get(blockTypeId) instanceof LightBlockType) {
                        queueLightUpdate(new Vector3i(x, y, z), false);
                    }
                } else {
                    blockQueue.add(new Vector4i(x, y, z, blockId));
                }
                Block aboveBlock = getBlock(x, y+1, z);
                if (aboveBlock != null) {
                    int aboveBlockId = aboveBlock.typeId();
                    if (aboveBlockId == 4 || aboveBlockId == 5) {
                        setBlock(x, y + 1, z, 0, 0, true, instant);
                    }
                }
            }
        }
    }

    public static void updateHeightmap(int x, int z, boolean update) {
        int pos = condensePos(x, z);
        int oldHeight = heightmap[pos];
        int tempHeight = height-1;
        for (int scanY = tempHeight; scanY >= 0; scanY--) {
            if (scanY == 0) {
                tempHeight = 0;
            } else {
                Block block = getBlock(x, scanY, z);
                if (block != null) {
                    if (!BlockTypes.blockTypeMap.get(block.typeId()).isTransparent) {
                        tempHeight = scanY;
                        break;
                    } else {
                        Vector3i chunkPos = new Vector3i(x/16, scanY/16, z/16);
                        region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(x-(chunkPos.x*16), scanY-(chunkPos.y*16), z-(chunkPos.z*16)),
                                new Block(block.id(), (byte) 0, (byte) 0, (byte) 0, (byte) 12));
                        if (update && oldHeight >= scanY) {
                            queueLightUpdate(new Vector3i(x, scanY, z), false);
                        }
                    }
                }
            }
        }
        heightmap[pos] = (short) tempHeight;
    }

    public static byte[] getLight(int x, int y, int z) {
        Block block = getBlock(new Vector3i(x, y, z));
        if (block != null) {
            return new byte[]{block.r(), block.g(), block.b(), block.s()};
        }
        return null;
    }

    public static byte[] getLight(Vector3i pos) {
        return getLight(pos.x, pos.y, pos.z);
    }

    public static void recalculateLight(Vector3i pos, byte r, byte g, byte b, byte s) {
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
                BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.typeId());
                if (neighborBlockType.isTransparent || neighborBlockType instanceof LightBlockType) {
                    boolean sunlit =  neighbor.s() == 12;
                    if (sunlit) {
                        if (heightmap[condensePos(neighborPos.x, neighborPos.z)] >= neighborPos.y) {
                            sunlit = false;
                            for (int belowY = neighborPos.y; belowY >= 0; belowY--) {
                                Vector3i belowPos = new Vector3i(neighborPos.x, belowY, neighborPos.z);
                                Block block = getBlock(belowPos);
                                if (block != null && block.s() == 12) {
                                    Vector3i chunkPos = new Vector3i(neighborPos.x/16, belowY/16, neighborPos.z/16);
                                    region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(neighborPos.x-(chunkPos.x*16), belowY-(chunkPos.y*16), neighborPos.z-(chunkPos.z*16)),
                                            new Block(block.id(), block.r(), block.g(), block.b(), (byte) 0));
                                    recalculateLight(belowPos, block.r(), block.g(), block.b(), (byte) 12);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    if ((neighbor.r() > 0 && neighbor.r() < r) || (neighbor.g() > 0 && neighbor.g() < g) || (neighbor.b() > 0 && neighbor.b() < b) || (neighbor.s() > 0 && neighbor.s() < s)) {
                        Vector3i chunkPos = new Vector3i(neighborPos.x/16, neighborPos.y/16, neighborPos.z/16);
                        region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(neighborPos.x-(chunkPos.x*16), neighborPos.y-(chunkPos.y*16), neighborPos.z-(chunkPos.z*16)),
                                new Block(neighbor.id(), (byte) 0, (byte) 0, (byte) 0, (byte) (sunlit ? 12 : 0)));
                        recalculateLight(neighborPos, neighbor.r(), neighbor.g(), neighbor.b(), neighbor.s());
                    }
                    queueLightUpdate(pos, true);
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
        Block block = getBlock(pos);
        if (block != null) {
            return BlockHelper.updateLight(pos, block);
        } else {
            return 0;
        }
    }
}
