package org.conspiracraft.game.world;

import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.Noise;
import org.conspiracraft.game.rendering.Renderer;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import static org.conspiracraft.engine.Utils.*;

public class World {
    public static int seaLevel = 63;
    public static int size = 6144;
    public static byte chunkSize = 16;
    public static int sizeChunks = size/chunkSize;
    public static short height = 320;
    public static int heightChunks = height/chunkSize;
    public static Path worldPath = Path.of(System.getenv("APPDATA")+"/Conspiracraft/world0");

    public static boolean cleanPalettes = false;
    public static boolean worldGenerated = false;
    public static int currentChunk = -1;

    public static Chunk[] chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static LinkedList<Vector4i> blockQueue = new LinkedList<>();
    public static boolean[] columnUpdates = new boolean[sizeChunks*sizeChunks];

    public static void run() throws IOException {
        if (Files.exists(worldPath)) {
            if (!worldGenerated) {
                String path = (World.worldPath + "/");
                for (int x = 0; x < World.sizeChunks; x++) {
                    for (int z = 0; z < World.sizeChunks; z++) {
                        loadColumn(path, x, z);
                    }
                }
                worldGenerated = true;
                Renderer.worldChanged = true;
            }
        } else {
            if (!worldGenerated) {
                currentChunk++;
                if (!cleanPalettes) {
                    if (currentChunk == sizeChunks) {
//                    for (int x = 0; x < size; x++) {
//                        for (int z = 0; z < size; z++) {
//                            updateHeightmap(x, z, false);
//                        }
//                    }
//                    for (int x = 1; x < size - 1; x++) {
//                        for (int z = 1; z < size - 1; z++) {
//                            for (int y = height - 1; y > 1; y--) {
//                                Block block = getBlock(x, y, z);
//                                if (block.s() < 20 && BlockTypes.blockTypeMap.get(block.typeId()).isTransparent) {
//                                    //check if any neighbors are a higher brightness
//                                    boolean shouldQ = false;
//                                    if (getBlock(x, y, z + 1).s() > block.s()) {
//                                        shouldQ = true;
//                                    } else if (getBlock(x + 1, y, z).s() > block.s()) {
//                                        shouldQ = true;
//                                    } else if (getBlock(x, y, z - 1).s() > block.s()) {
//                                        shouldQ = true;
//                                    } else if (getBlock(x - 1, y, z).s() > block.s()) {
//                                        shouldQ = true;
//                                    } else if (getBlock(x, y + 1, z).s() > block.s()) {
//                                        shouldQ = true;
//                                    } else if (getBlock(x, y - 1, z).s() > block.s()) {
//                                        shouldQ = true;
//                                    }
//                                    if (shouldQ) {
//                                        BlockHelper.updateLight(new Vector3i(x, y, z), block, false);
//                                    }
//                                }
//                            }
//                        }
//                    }
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
//                    for (int z = 0; z < sizeChunks; z++) {
//                        for (int y = 0; y < heightChunks; y++) {
//                            chunks[condenseChunkPos(currentChunk, y, z)].cleanPalette();
//                        }
//                    }
                    }
                }
            }
        }
//        if (cleaningQueue.size() > 3) {
//            Vector3i blockData = cleaningQueue.getFirst();
//            cleaningQueue.removeFirst();
//            region1Chunks[condenseChunkPos(blockData.x, blockData.y, blockData.z)].cleanPalette();
//        }
    }

    public static void loadColumn(String path, int x, int z) throws IOException {
        String chunkPath = path+x+"x"+z+"z.column";
        FileInputStream in = new FileInputStream(chunkPath);

        int[] data = Utils.byteArrayToIntArray(in.readAllBytes());
        int dataIndex = 0;

        for (int y = 0; y < World.heightChunks; y++) {
            int dataSize = data[dataIndex];
            dataIndex++;
            int[] palette = new int[dataSize];
            for (int i = dataSize-1; i >= 0; i--) {
                palette[i] = data[dataIndex];
                dataIndex++;
            }

            dataSize = data[dataIndex];
            dataIndex++;
            int[] blocks = new int[dataSize];
            for (int i = dataSize-1; i >= 0; i--) {
                blocks[i] = data[dataIndex];
                dataIndex++;
            }

            Chunk chunk = new Chunk();
            chunk.setBlockPalette(palette);
            chunk.setBlockData(blocks);
            World.chunks[condenseChunkPos(x, y, z)] = chunk;
        }
    }


    public static void generateWorld() {
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
                        double baseGradient = ConspiracraftMath.gradient(y, 72, 54, 2, -1);
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

    public static void queueColumnUpdate(Vector3i pos) {
        columnUpdates[condenseChunkPos(pos.x/16, pos.z/16)] = true;
    }

    public static Vector2i getBlock(Vector3i blockPos) {
        if (blockPos.x >= 0 && blockPos.x < size && blockPos.z >= 0 && blockPos.z < size && blockPos.y >= 0 && blockPos.y < height) {
            Vector3i chunkPos = new Vector3i(blockPos.x/chunkSize, blockPos.y/chunkSize, blockPos.z/chunkSize);
            int condensedChunkPos = condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z);
            Chunk chunk = chunks[condensedChunkPos];
            if (chunk == null) {
                chunks[condensedChunkPos] = new Chunk();
                chunk = chunks[condensedChunkPos];
            }
            return chunk.getBlock(condenseLocalPos(blockPos.x-(chunkPos.x*chunkSize), blockPos.y-(chunkPos.y*chunkSize), blockPos.z-(chunkPos.z*chunkSize)));
        }
        return null;
    }
    public static Vector2i getBlock(int x, int y, int z) {
        return getBlock(new Vector3i(x, y, z));
    }
    public static Vector2i getBlock(float x, float y, float z) {
        return getBlock(new Vector3i((int) x, (int) y, (int) z));
    }

    public static void setBlock(int x, int y, int z, int blockTypeId, int blockSubtypeId, boolean replace, boolean instant) {
        if (x > 0 && x < size && z > 0 && z < size && y > 0 && y < height) {
            Vector2i existing = getBlock(x, y, z);
            if (replace || (existing == null || existing.x() == 0)) {
                if (instant) {
                    Vector3i chunkPos = new Vector3i(x/chunkSize, y/chunkSize, z/chunkSize);
//                    byte r = 0;
//                    byte g = 0;
//                    byte b = 0;
//                    if (BlockTypes.blockTypeMap.get(blockTypeId) instanceof LightBlockType lType) {
//                        r = lType.r;
//                        g = lType.g;
//                        b = lType.b;
//                        updateLight(new Vector3i(x, y, z));
//                    }
                    chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setBlock(condenseLocalPos(x-(chunkPos.x*chunkSize), y-(chunkPos.y*chunkSize), z-(chunkPos.z*chunkSize)), blockTypeId, blockSubtypeId, new Vector3i(x, y, z));
                } else {
                    blockQueue.addLast(new Vector4i(x, y, z, Utils.packInts(blockTypeId, blockSubtypeId)));
                }
                Vector2i aboveBlock = getBlock(x, y+1, z);
                if (aboveBlock != null) {
                    int aboveBlockId = aboveBlock.x();
                    if (aboveBlockId == 4 || aboveBlockId == 5) {
                        setBlock(x, y + 1, z, 0, 0, true, instant);
                    }
                }
            }
        }
    }
}