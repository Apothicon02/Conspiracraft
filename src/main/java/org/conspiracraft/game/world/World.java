package org.conspiracraft.game.world;

import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.Noise;
import org.conspiracraft.game.Renderer;

import org.conspiracraft.game.blocks.BlockHelper;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.game.blocks.Block;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class World {
    public static int seaLevel = 138;
    public static int size = 1024;
    public static byte chunkSize = 16;
    public static int sizeChunks = size/chunkSize;
    public static short height = 320;
    public static int heightChunks = height/chunkSize;

    public static Chunk[] region1Chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static short[] heightmap = new short[size*size];

    public static List<Short> cornerQueue = new ArrayList<>(List.of());
    public static List<Short> lightQueue = new ArrayList<>(List.of());
    public static List<Short> blockQueue = new ArrayList<>(List.of());

    public static boolean worldGenerated = false;
    public static int currentChunk = -1;

    public static void init() {
        clearWorld();
    }

    public static void run() {
        if (!worldGenerated) {
            currentChunk++;
            if (currentChunk == sizeChunks) {
                boolean prevSolid = false;
                for (int x = 0; x < size; x++) {
                    for (int z = 0; z < size; z++) {
                        updateHeightmap(x, z, false);
                    }
                }
                for (int x = 1; x < size-1; x++) {
                    for (int z = 1; z < size-1; z++) {
                        for (int y = height-1; y > 1; y--) {
                            Block block = getBlock(x, y, z);
                            boolean solid = !BlockTypes.blockTypeMap.get(block.typeId()).isTransparent;
                            if (block.s() < 20 && !solid && prevSolid) {
                                //check if any neighbors are a higher brightness
                                if (getBlock(x, y, z+1).s() > block.s()) {
                                    queueLightUpdate(new Vector3i(x, y, z));
                                } else if (getBlock(x+1, y, z).s() > block.s()) {
                                    queueLightUpdate(new Vector3i(x, y, z));
                                } else if (getBlock(x, y, z-1).s() > block.s()) {
                                    queueLightUpdate(new Vector3i(x, y, z));
                                } else if (getBlock(x-1, y, z).s() > block.s()) {
                                    queueLightUpdate(new Vector3i(x, y, z));
                                } else if (getBlock(x, y+1, z).s() > block.s()) {
                                    queueLightUpdate(new Vector3i(x, y, z));
                                } else if (getBlock(x, y-1, z).s() > block.s()) {
                                    queueLightUpdate(new Vector3i(x, y, z));
                                }
                            }
                            prevSolid = solid;
                        }
                    }
                }
                worldGenerated = true;
                Renderer.worldChanged = true;
            } else {
                generateWorld();
                for (int z = 0; z < sizeChunks; z++) {
                    for (int y = 0; y < heightChunks; y++) {
                        region1Chunks[condenseChunkPos(currentChunk, y, z)].cleanPalette();
                    }
                }
            }
        }
    }

    public static void regenerateWorld() {
        currentChunk = -1;
        worldGenerated = false;
        clearWorld();
    }

    public static void clearWorld() {
        region1Chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
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
        for (int x = currentChunk*16; x < (currentChunk*16)+16; x++) {
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
                    double baseDensity = 0;
                    if (distance > size/4.02) {
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
                    }
                }
                for (int y = height-1; y > surface; y--) {
                    setBlock(x, y, z, 0, 0, false, true);
                }
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
    }

    public static int condensePos(int x, int z) {
        return (x * size) + z;
    }
    public static int condensePos(int x, int y, int z) {
        return (((x*size)+z)*height)+y;
    }
    public static int condensePos(int x, int y, int z, int customSize) {
        return (((x*customSize)+z)*height)+y;
    }
    public static int condensePos(Vector3i pos) {
        return (((pos.x*size)+pos.z)*height)+pos.y;
    }
    public static int condenseLocalPos(int x, int y, int z) {
        return (((x*chunkSize)+z)*chunkSize)+y;
    }
    public static int condenseLocalPos(Vector3i pos) {
        return (((pos.x*chunkSize)+pos.z)*chunkSize)+pos.y;
    }
    public static int condenseChunkPos(Vector3i pos) {
        return (((pos.x*sizeChunks)+pos.z)*heightChunks)+pos.y;
    }
    public static int condenseChunkPos(int x, int y, int z) {
        return (((x*sizeChunks)+z)*heightChunks)+y;
    }

    public static byte getCorners(int x, int y, int z) {
        if (x >= 0 && x < size && z >= 0 && z < size && y >= 0 && y < height) {
            Vector3i chunkPos = new Vector3i(x/chunkSize, y/chunkSize, z/chunkSize);
            return region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].getCorners(condenseLocalPos(x-(chunkPos.x*chunkSize), y-(chunkPos.y*chunkSize), z-(chunkPos.z*chunkSize)));
        }
        return Utils.convertBoolArrayToByte(new boolean[]{true, true, true, true, true, true, true, true});
    }

    public static Block getBlock(Vector3i blockPos) {
        if (blockPos.x >= 0 && blockPos.x < size && blockPos.z >= 0 && blockPos.z < size && blockPos.y >= 0 && blockPos.y < height) {
            Vector3i chunkPos = new Vector3i(blockPos.x/chunkSize, blockPos.y/chunkSize, blockPos.z/chunkSize);
            int condensedChunkPos = condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z);
            Chunk chunk = region1Chunks[condensedChunkPos];
            if (chunk == null) {
                region1Chunks[condensedChunkPos] = new Chunk();
                chunk = region1Chunks[condensedChunkPos];
            }
            return chunk.getBlock(condenseLocalPos(blockPos.x-(chunkPos.x*chunkSize), blockPos.y-(chunkPos.y*chunkSize), blockPos.z-(chunkPos.z*chunkSize)));
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
                if (instant) {
                    Vector3i chunkPos = new Vector3i(x/chunkSize, y/chunkSize, z/chunkSize);
                    byte r = 0;
                    byte g = 0;
                    byte b = 0;
                    if (BlockTypes.blockTypeMap.get(blockTypeId) instanceof LightBlockType lType) {
                        r = lType.r;
                        g = lType.g;
                        b = lType.b;
                        queueLightUpdate(new Vector3i(x, y, z));
                    }
                    region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(x-(chunkPos.x*chunkSize), y-(chunkPos.y*chunkSize), z-(chunkPos.z*chunkSize)), new Block(blockTypeId, blockSubtypeId, r, g, b, (byte) 0));
                } else {
                    blockQueue.addLast((short) x);
                    blockQueue.addLast((short) y);
                    blockQueue.addLast((short) z);
                    blockQueue.addLast((short) blockTypeId);
                    blockQueue.addLast((short) blockSubtypeId);
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
        for (int scanY = height-1; scanY >= 0; scanY--) {
            if (scanY == 0) {
                heightmap[pos] = (short) 0;
                break;
            } else {
                Block block = getBlock(x, scanY, z);
                if (!BlockTypes.blockTypeMap.get(block.typeId()).isTransparent) {
                    if (update) {
                        for (int scanExtraY = scanY - 1; scanExtraY >= 0; scanExtraY--) {
                            Block blockExtra = getBlock(x, scanExtraY, z);
                            if (BlockTypes.blockTypeMap.get(blockExtra.typeId()).isTransparent) {
                                Vector3i chunkPos = new Vector3i(x / chunkSize, scanExtraY / chunkSize, z / chunkSize);
                                region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(x - (chunkPos.x * chunkSize), scanExtraY - (chunkPos.y * chunkSize), z - (chunkPos.z * chunkSize)),
                                        new Block(blockExtra.id(), blockExtra.r(), blockExtra.g(), blockExtra.b(), (byte) 0), true);
                                recalculateLight(new Vector3i(x, scanExtraY, z), blockExtra.r(), blockExtra.g(), blockExtra.b(), blockExtra.s());
                            } else {
                                break;
                            }
                        }
                    }
                    break;
                } else if (block.s() < 20) {
                    Vector3i chunkPos = new Vector3i(x/chunkSize, scanY/chunkSize, z/chunkSize);
                    region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(x-(chunkPos.x*chunkSize), scanY-(chunkPos.y*chunkSize), z-(chunkPos.z*chunkSize)),
                            new Block(block.id(), block.r(), block.g(), block.b(), (byte) 20), update);
                    if (update) {
                        queueLightUpdate(new Vector3i(x, scanY, z));
                    }
                }
            }
        }
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
                    if ((neighbor.r() > 0 && neighbor.r() < r) || (neighbor.g() > 0 && neighbor.g() < g) || (neighbor.b() > 0 && neighbor.b() < b) || (neighbor.s() > 0 && neighbor.s() < s)) {
                        Vector3i chunkPos = new Vector3i(neighborPos.x/16, neighborPos.y/16, neighborPos.z/16);
                        byte nr = 0;
                        byte ng = 0;
                        byte nb = 0;
                        if (neighborBlockType instanceof LightBlockType lBlock) {
                            nr = lBlock.r;
                            ng = lBlock.g;
                            nb = lBlock.b;
                        }
                        region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(neighborPos.x-(chunkPos.x*16), neighborPos.y-(chunkPos.y*16), neighborPos.z-(chunkPos.z*16)),
                                new Block(neighbor.id(), nr, ng, nb, (byte) (neighbor.s() == 20 ? 20 : 0)), true);
                        recalculateLight(neighborPos, neighbor.r(), neighbor.g(), neighbor.b(), neighbor.s());
                    }
                    queueLightUpdatePriority(pos);
                }
            }
        }
    }

    public static void queueLightUpdate(Vector3i pos) {
        boolean exists = false;
        for (int i = 0; i < lightQueue.size(); i+=3) {
            if (lightQueue.get(i+2) == pos.z && lightQueue.get(i+1) == pos.y && lightQueue.get(i) == pos.x) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            lightQueue.addLast((short) pos.x);
            lightQueue.addLast((short) pos.y);
            lightQueue.addLast((short) pos.z);
        }
    }
    public static void queueLightUpdatePriority(Vector3i pos) {
        boolean exists = false;
        for (int i = 0; i < lightQueue.size(); i+=3) {
            if (lightQueue.get(i+2) == pos.z && lightQueue.get(i+1) == pos.y && lightQueue.get(i) == pos.x) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            lightQueue.addFirst((short) pos.z);
            lightQueue.addFirst((short) pos.y);
            lightQueue.addFirst((short) pos.x);
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
