package org.conspiracraft.game.world;

import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.Noise;
import org.conspiracraft.game.rendering.Renderer;

import org.conspiracraft.game.blocks.BlockHelper;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.game.blocks.Block;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class World {
    public static int seaLevel = 138;
    public static int size = 1024;
    public static byte chunkSize = 16;
    public static int sizeChunks = size/chunkSize;
    public static short height = 320;
    public static int heightChunks = height/chunkSize;

    public static Chunk[] region1Chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static short[] heightmap = new short[size*size];

    public static LinkedList<Vector3i> lightQueue = new LinkedList<>();
    public static LinkedList<Vector4i> blockQueue = new LinkedList<>();
    public static LinkedList<Vector3i> cleaningQueue = new LinkedList<>();

    public static boolean cleanPalettes = false;
    public static boolean worldGenerated = false;
    public static int currentChunk = -1;
    public static Path worldPath = Path.of(System.getenv("APPDATA")+"/Conspiracraft/world0");

    public static void init() {
        clearWorld();
    }

    public static void run() throws IOException {
        if (!worldGenerated) {
            if (Files.exists(worldPath)) {
                String path = (World.worldPath+"/");
                for (int x = 0; x < World.sizeChunks; x++) {
                    for (int z = 0; z < World.sizeChunks; z++) {
                        loadColumn(path, x, z);
                    }
                }
                worldGenerated = true;
                Renderer.worldChanged = true;
            } else {
                currentChunk++;
                if (!cleanPalettes) {
                    if (currentChunk == sizeChunks) {
                        for (int x = 0; x < size; x++) {
                            for (int z = 0; z < size; z++) {
                                updateHeightmap(x, z, false);
                            }
                        }
                        for (int x = 1; x < size - 1; x++) {
                            for (int z = 1; z < size - 1; z++) {
                                for (int y = height - 1; y > 1; y--) {
                                    Block block = getBlock(x, y, z);
                                    if (block.s() < 20 && BlockTypes.blockTypeMap.get(block.typeId()).isTransparent) {
                                        //check if any neighbors are a higher brightness
                                        boolean shouldQ = false;
                                        if (getBlock(x, y, z + 1).s() > block.s()) {
                                            shouldQ = true;
                                        } else if (getBlock(x + 1, y, z).s() > block.s()) {
                                            shouldQ = true;
                                        } else if (getBlock(x, y, z - 1).s() > block.s()) {
                                            shouldQ = true;
                                        } else if (getBlock(x - 1, y, z).s() > block.s()) {
                                            shouldQ = true;
                                        } else if (getBlock(x, y + 1, z).s() > block.s()) {
                                            shouldQ = true;
                                        } else if (getBlock(x, y - 1, z).s() > block.s()) {
                                            shouldQ = true;
                                        }
                                        if (shouldQ) {
                                            BlockHelper.updateLight(new Vector3i(x, y, z), block, false);
                                        }
                                    }
                                }
                            }
                        }
                        cleanPalettes = true;
                        currentChunk = -1;
                    } else {
                        generateWorld();
                    }
                } else {
                    if (currentChunk == sizeChunks) {
                        worldGenerated = true;
                        Renderer.worldChanged = true;
                    } else {
                        for (int z = 0; z < sizeChunks; z++) {
                            for (int y = 0; y < heightChunks; y++) {
                                region1Chunks[condenseChunkPos(currentChunk, y, z)].cleanPalette();
                            }
                        }
                    }
                }
            }
        }
        if (cleaningQueue.size() > 3) {
            Vector3i blockData = cleaningQueue.getFirst();
            cleaningQueue.removeFirst();
            region1Chunks[condenseChunkPos(blockData.x, blockData.y, blockData.z)].cleanPalette();
        }
    }

    public static void loadColumn(String path, int x, int z) throws IOException {
        String chunkPath = path+x+"x"+z+"z.column";
        FileInputStream in = new FileInputStream(chunkPath);

        int[] data = Utils.byteArrayToIntArray(in.readAllBytes());
        int dataIndex = 0;

        for (int y = 0; y < World.heightChunks; y++) {
            int size = data[dataIndex];
            dataIndex++;
            int[] palette = new int[size];
            for (int i = size-1; i >= 0; i--) {
                palette[i] = data[dataIndex];
                dataIndex++;
            }

            size = data[dataIndex];
            dataIndex++;
            int[] blocks = new int[size];
            for (int i = size-1; i >= 0; i--) {
                blocks[i] = data[dataIndex];
                dataIndex++;
            }

            size = data[dataIndex];
            dataIndex++;
            int[] lightPalette = new int[size];
            for (int i = size-1; i >= 0; i--) {
                lightPalette[i] = data[dataIndex];
                dataIndex++;
            }

            size = data[dataIndex];
            dataIndex++;
            int[] lights = new int[size];
            for (int i = size-1; i >= 0; i--) {
                lights[i] = data[dataIndex];
                dataIndex++;
            }

            Chunk chunk = new Chunk();
            chunk.setPalette(palette);
            chunk.setBlocks(blocks);
            chunk.setLightPalette(lightPalette);
            chunk.setLights(lights);
            World.region1Chunks[World.condenseChunkPos(x, y, z)] = chunk;
        }
    }

    public static void regenerateWorld() {
        currentChunk = -1;
        worldGenerated = false;
        clearWorld();
    }

    public static void clearWorld() {
        region1Chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
        lightQueue = new LinkedList<>();
        blockQueue = new LinkedList<>();
    }

    public static void generateWorld() {
//        for (int x = currentChunk*16; x < (currentChunk*16)+16; x++) {
//            for (int z = 0; z < size; z++) {
//                for (int y = height-1; y >= 0; y--) {
//                    if (y <= seaLevel) {
//                        setBlock(x, y, z, 2, 0, false, true);
//                    } else {
//                        setBlock(x, y, z, 0, 0, false, true);
//                    }
//                }
//            }
//        }
        for (int x = currentChunk*16; x < (currentChunk*16)+16; x++) {
            for (int z = 0; z < size; z++) {
                if (z == 0 || z == size-1 || x == 0 || x == size-1) {
                    for (int y = height-1; y >= 0; y--) {
                        setBlock(x, y, z, 12, 0, false, true);
                    }
                } else {
                    float baseCellularNoise = (Noise.blue(Noise.CELLULAR_NOISE.getRGB(x - (((int) (x / Noise.CELLULAR_NOISE.getWidth())) * Noise.CELLULAR_NOISE.getWidth()), z - (((int) (z / Noise.CELLULAR_NOISE.getHeight())) * Noise.CELLULAR_NOISE.getHeight()))) / 128) - 1;
                    float basePerlinNoise = (Noise.blue(Noise.COHERERENT_NOISE.getRGB(x - (((int) (x / Noise.COHERERENT_NOISE.getWidth())) * Noise.COHERERENT_NOISE.getWidth()), z - (((int) (z / Noise.COHERERENT_NOISE.getHeight())) * Noise.COHERERENT_NOISE.getHeight()))) / 128) - 1;
                    float foliageNoise = (basePerlinNoise + 0.5f);
                    float exponentialFoliageNoise = foliageNoise * foliageNoise;
                    int surface = height - 1;
                    boolean upmost = true;
                    for (int y = surface; y >= 0; y--) {
                        double baseGradient = ConspiracraftMath.gradient(y, 157, 126, 2, -1);
                        double baseDensity = baseCellularNoise + baseGradient;
                        if (baseDensity > 0) {
                            if (upmost && y >= seaLevel) {
                                setBlock(x, y, z, 2, 0, false, true);
                                double torchChance = Math.random();
                                if (torchChance > 0.99995d) {
                                    if (torchChance > 0.99997d) {
                                        setBlock(x, y, z, 7, 0, true, true);
                                        setBlock(x, y + 1, z, 7, 0, true, true);
                                        setBlock(x, y + 2, z, 7, 0, false, true);
                                    } else {
                                        setBlock(x, y + 1, z, 14, 0, false, true);
                                    }
                                } else if (torchChance < exponentialFoliageNoise * 0.015f) { //tree 0.015
                                    int maxHeight = (int) (Math.random() * 4) + 8;
                                    for (int i = 0; i < maxHeight; i++) {
                                        setBlock(x, y + i, z, 16, 0, true, true);
                                    }
                                    int radius = (int) (maxHeight + (torchChance * 100));
                                    for (int lX = x - radius; lX <= x + radius; lX++) {
                                        for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                                            for (int lY = y + maxHeight - radius; lY <= y + maxHeight + radius; lY++) {
                                                int xDist = lX - x;
                                                int yDist = lY - (y + maxHeight);
                                                int zDist = lZ - z;
                                                int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                                                if (dist <= radius * 3) {
                                                    setBlock(lX, lY, lZ, 17, 0, false, true);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    setBlock(x, y + 1, z, 4 + (Math.random() > 0.98f ? 1 : 0), (int) (Math.random() * 3), false, true);
                                }
                                surface = y;
                                upmost = false;
                            } else {
                                setBlock(x, y, z, y > surface - 3 ? 3 : 10, 0, false, true);
                            }
                        } else {
                            if (y <= seaLevel) {
                                setBlock(x, y, z, 1, 0, false, true);
                            } else {
                                setBlock(x, y, z, 0, 0, false, true);
                            }
                        }
                    }
                    for (int y = height - 1; y > surface; y--) {
                        setBlock(x, y, z, 0, 0, false, true);
                    }
                }
            }
        }
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

    public static void queueCleaning(Vector3i pos) {
        Vector3i chunkPos = new Vector3i(pos.x/16, pos.y/16, pos.z/16);
        if (!cleaningQueue.contains(chunkPos)) {
            World.cleaningQueue.addLast(chunkPos);
        }
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
                        updateLight(new Vector3i(x, y, z));
                    }
                    region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(x-(chunkPos.x*chunkSize), y-(chunkPos.y*chunkSize), z-(chunkPos.z*chunkSize)), new Block(blockTypeId, blockSubtypeId, r, g, b, (byte) 0), new Vector3i(x, y, z));
                } else {
                    blockQueue.addLast(new Vector4i(x, y, z, Utils.packInts(blockTypeId, blockSubtypeId)));
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
        boolean setHeightmap = false;
        for (int scanY = height-1; scanY >= 0; scanY--) {
            if (scanY == 0) {
                heightmap[pos] = (short) 0;
                break;
            } else {
                Block block = getBlock(x, scanY, z);
                if (block.typeId() != 0 && !setHeightmap) {
                    setHeightmap = true;
                    heightmap[pos] = (short) (scanY);
                }
                if (!BlockTypes.blockTypeMap.get(block.typeId()).isTransparent) {
                    if (update) {
                        for (int scanExtraY = scanY - 1; scanExtraY >= 0; scanExtraY--) {
                            Block blockExtra = getBlock(x, scanExtraY, z);
                            if (BlockTypes.blockTypeMap.get(blockExtra.typeId()).isTransparent) {
                                Vector3i chunkPos = new Vector3i(x / chunkSize, scanExtraY / chunkSize, z / chunkSize);
                                region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(x - (chunkPos.x * chunkSize), scanExtraY - (chunkPos.y * chunkSize), z - (chunkPos.z * chunkSize)),
                                        new Block(blockExtra.id(), blockExtra.r(), blockExtra.g(), blockExtra.b(), (byte) 0), new Vector3i(x, scanExtraY, z));
                                recalculateLight(new Vector3i(x, scanExtraY, z), blockExtra.r(), blockExtra.g(), blockExtra.b(), blockExtra.s());
                            } else {
                                break;
                            }
                        }
                    }
                    break;
                } else {
                    Vector3i chunkPos = new Vector3i(x/chunkSize, scanY/chunkSize, z/chunkSize);
                    region1Chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(x-(chunkPos.x*chunkSize), scanY-(chunkPos.y*chunkSize), z-(chunkPos.z*chunkSize)),
                            new Block(block.id(), block.r(), block.g(), block.b(), (byte) Math.min(20, 20+Math.max(-20, scanY-Math.max(scanY, heightmap[pos])))), new Vector3i(x, scanY, z));
                    if (update) {
                        updateLight(new Vector3i(x, scanY, z));
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
                                new Block(neighbor.id(), nr, ng, nb, (byte) (neighbor.s() == 20 ? 20 : 0)), pos);
                        recalculateLight(neighborPos, neighbor.r(), neighbor.g(), neighbor.b(), neighbor.s());
                    }
                    queueLightUpdatePriority(pos);
                }
            }
        }
    }

    public static void queueLightUpdate(Vector3i pos) {
        boolean exists = lightQueue.contains(pos);
        if (!exists) {
            lightQueue.addLast(pos);
        }
    }
    public static void queueLightUpdatePriority(Vector3i pos) {
        boolean exists = lightQueue.contains(pos);
        if (!exists) {
            lightQueue.addFirst(pos);
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
