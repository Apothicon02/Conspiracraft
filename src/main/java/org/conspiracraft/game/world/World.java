package org.conspiracraft.game.world;

import org.conspiracraft.Main;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.ScheduledTicker;
import org.conspiracraft.game.audio.AudioController;
import org.conspiracraft.game.audio.Source;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.Renderer;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

import static org.conspiracraft.engine.Utils.*;
import static org.conspiracraft.game.world.WorldGen.*;

public class World {
    public static int seaLevel = 63;
    public static int size = 4208; //6976
    public static int halfSize = size/2;
    public static int quarterSize = size/4;
    public static int eighthSize = size/8;
    public static byte chunkSize = 16;
    public static byte subChunkSize = (byte) (chunkSize/2);
    public static int sizeChunks = size / chunkSize;
    public static short height = 432;
    public static int heightChunks = height / chunkSize;
    public static Path worldPath = Path.of(System.getenv("APPDATA")+"/Conspiracraft/world0");
    public static boolean quarterWorld = false;
    public static boolean doLight = true;

    public static boolean createdChunks = false;
    public static boolean worldGenerated = false;
    public static boolean heightmapGenerated = false;
    public static boolean surfaceGenerated = false;
    public static boolean featuresGenerated = false;
    public static int currentChunk = -1;

    public static Chunk[] chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static boolean chunkEmptinessChanged = false;
    public static int[] chunkEmptiness = new int[1+((sizeChunks*sizeChunks*heightChunks)/32)];
    public static short[] surfaceHeightmap = new short[size*size];
    public static short[] heightmap = new short[size*size];
    public static ArrayList<Integer> chunkBlockQueue = new ArrayList<>();
    public static ArrayList<Integer> chunkCornerQueue = new ArrayList<>();
    public static ArrayList<Integer> chunkLightQueue = new ArrayList<>();
    public static ArrayList<Vector3i> lightQueue = new ArrayList<>();
    public static long stageTime = 0;

    public static void tick() {
        iterateLightQueue();
    }

    public static void iterateLightQueue() {
        while (!lightQueue.isEmpty()) {
            Vector3i pos = lightQueue.getFirst();
            lightQueue.removeFirst();
            updateLight(pos);
        }
    }

    public static void generate() throws IOException {
        if (Files.exists(worldPath)) {
            String path = (World.worldPath + "/chunks.data");
            loadWorld(path);
            createdChunks = true;
            heightmapGenerated = true;
            surfaceGenerated = true;
            featuresGenerated = true;
            areChunksCompressed = true;
            worldGenerated = true;
            Renderer.worldChanged = true;
        } else {
            if (!createdChunks) {
                createdChunks = true;
                for (int x = 0; x < sizeChunks; x++) {
                    for (int z = 0; z < sizeChunks; z++) {
                        for (int y = 0; y < heightChunks; y++) {
                            int condensedChunkPos = condenseChunkPos(x, y, z);
                            chunks[condensedChunkPos] = new Chunk(new Vector3i(x, y, z), condensedChunkPos);
                        }
                    }
                }
            }
            currentChunk++;
            if (heightmapGenerated && surfaceGenerated && featuresGenerated) {
                if (doLight) {
                    if (stageTime == 0) {
                        stageTime = System.currentTimeMillis() / 1000;
                    }
                    if (currentChunk == sizeChunks) {
                        System.out.print("Light filling took " + ((System.currentTimeMillis() / 1000) - stageTime) + "s \n");
                        stageTime = 0;
                        surfaceHeightmap = null;
                        currentChunk = -1;
                        worldGenerated = true;
                        Renderer.worldChanged = true;
                    } else {
                        fillLight();
                    }
                } else {
                    worldGenerated = true;
                    Renderer.worldChanged = true;
                }
            } else if (!worldGenerated) {
                if (!heightmapGenerated) {
                    if (stageTime == 0) {
                        stageTime = System.currentTimeMillis() / 1000;
                    }
                    if (currentChunk == sizeChunks) {
                        System.out.print("Heightmap generation took " + ((System.currentTimeMillis() / 1000) - stageTime) + "s \n");
                        stageTime = 0;
                        int i = 0;
                        for (short y : heightmap) {
                            surfaceHeightmap[i++] = y;
                        }
                        currentChunk = -1;
                        heightmapGenerated = true;
                    } else {
                        generateHeightmap();
                    }
                } else if (!surfaceGenerated) {
                    if (stageTime == 0) {
                        stageTime = System.currentTimeMillis() / 1000;
                    }
                    if (currentChunk == sizeChunks) {
                        System.out.print("Surface generation took " + ((System.currentTimeMillis() / 1000) - stageTime) + "s \n");
                        stageTime = 0;
                        currentChunk = -1;
                        surfaceGenerated = true;
                    } else {
                        generateSurface();
                    }
                } else if (!featuresGenerated) {
                    if (stageTime == 0) {
                        stageTime = System.currentTimeMillis() / 1000;
                    }
                    if (currentChunk == sizeChunks) {
                        System.out.print("Feature generation took " + ((System.currentTimeMillis() / 1000) - stageTime) + "s \n");
                        stageTime = System.currentTimeMillis() / 1000;
                        for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                            for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
                                for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                                    int condensedChunkPos = Utils.condenseChunkPos(chunkX, chunkY, chunkZ);
                                    Chunk chunk = chunks[condensedChunkPos];
                                    if (chunk.uncompressedBlocks != null) {
                                        int i = 0;
                                        int key = 0;
                                        int prevBlock = 0;
                                        for (int block : chunk.uncompressedBlocks) {
                                            if (block != prevBlock) {
                                                key = chunk.blockPalette.indexOf(block);
                                                if (key == -1) {
                                                    key = chunk.blockPalette.size();
                                                    chunk.blockPalette.add(block);
                                                    chunk.updateBlockPaletteKeySize();
                                                }
                                                prevBlock = block;
                                            }

                                            chunk.blockData.setValue(i, key);
                                            i++;
                                        }
                                        chunk.uncompressedBlocks = null;
                                    }
                                }
                            }
                        }
                        areChunksCompressed = true;
                        System.out.print("Palette compression took " + ((System.currentTimeMillis() / 1000) - stageTime) + "s \n");
                        stageTime = 0;
                        currentChunk = -1;
                        featuresGenerated = true;
                    } else {
                        generateFeatures();
                    }
                }
            }
        }
    }

    public static void saveWorld(String path) throws IOException {
        new File(path).mkdirs();

        String playerDataPath = path+"player.data";
        FileOutputStream out = new FileOutputStream(playerDataPath);
        byte[] playerData = Utils.intArrayToByteArray(Main.player.getData());
        out.write(playerData);
        out.close();

        String globalDataPath = path+"world.data";
        out = new FileOutputStream(globalDataPath);
        byte[] globalData = Utils.intArrayToByteArray(new int[]{(int)(Renderer.time*1000), (int)(Main.timePassed*1000), Main.meridiem});
        out.write(globalData);
        out.close();

        String chunkEmptinessDataPath = path+"chunk_emptiness.data";
        out = new FileOutputStream(chunkEmptinessDataPath);
        byte[] chunkEpmtinessData = Utils.intArrayToByteArray(World.chunkEmptiness);
        out.write(chunkEpmtinessData);
        out.close();

        String heightmapDataPath = path+"heightmap.data";
        out = new FileOutputStream(heightmapDataPath);
        byte[] heightmapData = Utils.intArrayToByteArray(Utils.shortArrayToIntArray(World.heightmap));
        out.write(heightmapData);
        out.close();

        String chunksPath = path + "chunks.data";
        out = new FileOutputStream(chunksPath);
        for (int x = 0; x < World.sizeChunks; x++) {
            for (int z = 0; z < World.sizeChunks; z++) {
                for (int y = 0; y < World.heightChunks; y++) {
                    Chunk chunk = World.chunks[Utils.condenseChunkPos(x, y, z)];
                    byte[] subChunks = Utils.intArrayToByteArray(chunk.getSubChunks());

                    byte[] blockPalette = Utils.intArrayToByteArray(chunk.getBlockPalette());
                    int[] blockData = chunk.getBlockData();
                    byte[] blocks;
                    if (blockData != null) {
                        blocks = Utils.intArrayToByteArray(blockData);
                    } else {
                        blocks = new byte[]{};
                    }

                    byte[] cornerPalette = Utils.intArrayToByteArray(chunk.getCornerPalette());
                    int[] cornerData = chunk.getCornerData();
                    byte[] corners;
                    if (cornerData != null) {
                        corners = Utils.intArrayToByteArray(cornerData);
                    } else {
                        corners = new byte[]{};
                    }

                    byte[] lightPalette = Utils.intArrayToByteArray(chunk.getLightPalette());
                    int[] lightData = chunk.getLightData();
                    byte[] lights;
                    if (lightData != null) {
                        lights = Utils.intArrayToByteArray(lightData);
                    } else {
                        lights = new byte[]{};
                    }
                    ByteBuffer buffer = ByteBuffer.allocate(subChunks.length + 4 + blockPalette.length + 4 + blocks.length + 4 + cornerPalette.length + 4 + corners.length + 4 + lightPalette.length + 4 + lights.length + 4);
                    buffer.put(Utils.intArrayToByteArray(new int[]{subChunks.length / 4}));
                    buffer.put(subChunks);
                    buffer.put(Utils.intArrayToByteArray(new int[]{blockPalette.length / 4}));
                    buffer.put(blockPalette);
                    buffer.put(Utils.intArrayToByteArray(new int[]{blocks.length / 4}));
                    buffer.put(blocks);
                    buffer.put(Utils.intArrayToByteArray(new int[]{cornerPalette.length / 4}));
                    buffer.put(cornerPalette);
                    buffer.put(Utils.intArrayToByteArray(new int[]{corners.length / 4}));
                    buffer.put(corners);
                    buffer.put(Utils.intArrayToByteArray(new int[]{lightPalette.length / 4}));
                    buffer.put(lightPalette);
                    buffer.put(Utils.intArrayToByteArray(new int[]{lights.length / 4}));
                    buffer.put(lights);
                    out.write(buffer.array());
                }
            }
        }
        out.close();
    }
    public static void loadWorld(String path) throws IOException {
        FileInputStream in = new FileInputStream(path);

        int[] data = Utils.byteArrayToIntArray(in.readAllBytes());
        int dataIndex = 0;

        for (int x = 0; x < World.sizeChunks; x++) {
            for (int z = 0; z < World.sizeChunks; z++) {
                for (int y = 0; y < World.heightChunks; y++) {
                    int dataSize = data[dataIndex];
                    dataIndex++;
                    int[] subChunks = new int[dataSize];
                    for (int i = dataSize - 1; i >= 0; i--) {
                        subChunks[i] = data[dataIndex];
                        dataIndex++;
                    }

                    dataSize = data[dataIndex];
                    dataIndex++;
                    int[] blockPalette = new int[dataSize];
                    for (int i = dataSize - 1; i >= 0; i--) {
                        blockPalette[i] = data[dataIndex];
                        dataIndex++;
                    }

                    dataSize = data[dataIndex];
                    dataIndex++;
                    int[] blocks = new int[dataSize];
                    for (int i = dataSize - 1; i >= 0; i--) {
                        blocks[i] = data[dataIndex];
                        dataIndex++;
                    }

                    dataSize = data[dataIndex];
                    dataIndex++;
                    int[] cornerPalette = new int[dataSize];
                    for (int i = dataSize - 1; i >= 0; i--) {
                        cornerPalette[i] = data[dataIndex];
                        dataIndex++;
                    }

                    dataSize = data[dataIndex];
                    dataIndex++;
                    int[] corners = new int[dataSize];
                    for (int i = dataSize - 1; i >= 0; i--) {
                        corners[i] = data[dataIndex];
                        dataIndex++;
                    }

                    dataSize = data[dataIndex];
                    dataIndex++;
                    int[] lightPalette = new int[dataSize];
                    for (int i = dataSize - 1; i >= 0; i--) {
                        lightPalette[i] = data[dataIndex];
                        dataIndex++;
                    }

                    dataSize = data[dataIndex];
                    dataIndex++;
                    int[] lights = new int[dataSize];
                    for (int i = dataSize - 1; i >= 0; i--) {
                        lights[i] = data[dataIndex];
                        dataIndex++;
                    }

                    Chunk chunk = new Chunk(new Vector3i(x, y, z), Utils.condenseChunkPos(x, y, z));
                    chunk.setSubChunks(subChunks);
                    chunk.setBlockPalette(blockPalette);
                    chunk.setBlockData(blocks);
                    chunk.setCornerPalette(cornerPalette);
                    chunk.setCornerData(corners);
                    chunk.setLightPalette(lightPalette);
                    chunk.setLightData(lights);
                    World.chunks[condenseChunkPos(x, y, z)] = chunk;
                }
            }
        }
    }

    public static Vector2i getBlockUnsafe(Vector3i blockPos) {
        return chunks[condenseChunkPos(blockPos.x >> 4, blockPos.y >> 4, blockPos.z >> 4)].getBlock(condenseLocalPos(blockPos.x & 15, blockPos.y & 15, blockPos.z & 15));
    }
    public static Vector2i getBlock(Vector3i blockPos) {
        if (blockPos.x >= 0 && blockPos.x < size && blockPos.z >= 0 && blockPos.z < size && blockPos.y >= 0 && blockPos.y < height) {
            return chunks[condenseChunkPos(blockPos.x >> 4, blockPos.y >> 4, blockPos.z >> 4)].getBlock(condenseLocalPos(blockPos.x & 15, blockPos.y & 15, blockPos.z & 15));
        }
        return new Vector2i(0);
    }
    public static Vector2i getBlock(int x, int y, int z) {
        return getBlock(new Vector3i(x, y, z));
    }
    public static Vector2i getBlock(float x, float y, float z) {
        return getBlock(new Vector3i((int) x, (int) y, (int) z));
    }

    public static Vector4i getLight(Vector3i blockPos) {
        if (blockPos.x >= 0 && blockPos.x < size && blockPos.z >= 0 && blockPos.z < size && blockPos.y >= 0 && blockPos.y < height) {
            return chunks[condenseChunkPos(blockPos.x >> 4, blockPos.y >> 4, blockPos.z >> 4)].getLight(condenseLocalPos(blockPos.x & 15, blockPos.y & 15, blockPos.z & 15));
        }
        return null;
    }
    public static Vector4i getLight(int x, int y, int z) {
        return getLight(new Vector3i(x, y, z));
    }
    public static Vector4i getLight(float x, float y, float z) {
        return getLight(new Vector3i((int) x, (int) y, (int) z));
    }

    public static boolean canLightPassbetween(int blockType, int corners, Vector3i pos, Vector3i neighborPos) {
        Vector3i subtractedPos = new Vector3i(pos.x-neighborPos.x, pos.y-neighborPos.y, pos.z-neighborPos.z);
        if (!BlockTypes.blockTypeMap.get(blockType).blockProperties.blocksLight) {
            return true;
        } else {
            int blocked = 0;
            if (subtractedPos.x != 0) {
                for (int z = 0; z <= 2; z += 2) {
                    for (int y = 0; y <= 4; y += 4) {
                        blocked = 0;
                        for (int x = 0; x <= 1; x++) {
                            int cornerIndex = y + z + x;
                            int temp = corners;
                            temp &= (~(1 << (cornerIndex - 1)));
                            if (temp == corners) {
                                blocked++;
                            }
                        }
                        if (blocked <= 0) {
                            blocked = 0;
                            int newCorners = getCorner(neighborPos.x, neighborPos.y, neighborPos.z);
                            for (int x = 0; x <= 1; x ++) {
                                int cornerIndex = y + z + x;
                                int temp = newCorners;
                                temp &= (~(1 << (cornerIndex - 1)));
                                if (temp == corners) {
                                    blocked++;
                                }
                            }
                            if (blocked <= 0) {
                                return true;
                            }
                        }
                    }
                }
            } else if (subtractedPos.z != 0) {
                for (int x = 0; x <= 1; x ++) {
                    for (int y = 0; y <= 4; y += 4) {
                        blocked = 0;
                        for (int z = 0; z <= 2; z+=2) {
                            int cornerIndex = y + z + x;
                            int temp = corners;
                            temp &= (~(1 << (cornerIndex - 1)));
                            if (temp == corners) {
                                blocked++;
                            }
                        }
                        if (blocked <= 0) {
                            blocked = 0;
                            int newCorners = getCorner(neighborPos.x, neighborPos.y, neighborPos.z);
                            for (int z = 0; z <= 2; z += 2) {
                                int cornerIndex = y + z + x;
                                int temp = newCorners;
                                temp &= (~(1 << (cornerIndex - 1)));
                                if (temp == corners) {
                                    blocked++;
                                }
                            }
                            if (blocked <= 0) {
                                return true;
                            }
                        }
                    }
                }
            } else {
                for (int x = 0; x <= 1; x ++) {
                    for (int z = 0; z <= 2; z += 2) {
                        blocked = 0;
                        for (int y = 0; y <= 4; y += 4) {
                            int cornerIndex = y + z + x;
                            int temp = corners;
                            temp &= (~(1 << (cornerIndex - 1)));
                            if (temp == corners) {
                                blocked++;
                            }
                        }
                        if (blocked <= 0) {
                            blocked = 0;
                            int newCorners = getCorner(neighborPos.x, neighborPos.y, neighborPos.z);
                            for (int y = 0; y <= 4; y += 4) {
                                int cornerIndex = y + z + x;
                                int temp = newCorners;
                                temp &= (~(1 << (cornerIndex - 1)));
                                if (temp == corners) {
                                    blocked++;
                                }
                            }
                            if (blocked <= 0) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }
    public static boolean canLightPassbetween(int blockType, Vector3i pos, Vector3i neighborPos) {
        return canLightPassbetween(blockType, getCorner(pos.x, pos.y, pos.z), pos, neighborPos);
    }

    public static boolean blocksLight(int blockType, int corners) {
        if (!BlockTypes.blockTypeMap.get(blockType).blockProperties.blocksLight) {
            return false;
        } else {
            int blocked = 0;
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 2; z+=2) {
                    for (int y = 0; y <= 4; y+=4) {
                        int cornerIndex = y + z + x;
                        int temp = corners;
                        temp &= (~(1 << (cornerIndex - 1)));
                        if (temp == corners) {
                            blocked++;
                        }
                    }
                }
            }
            if (blocked >= 8) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean isBlockingHeightmap(Vector2i blockType, int corners) {
        BlockType type = BlockTypes.blockTypeMap.get(blockType.x);
        boolean obstructsHieghtmap = type.obstructingHeightmap(blockType);
        if (!type.blockProperties.blocksLight && !obstructsHieghtmap) {
            return false;
        } else if (corners != 0) {
            int blockedColumns = getBlockedColumns(corners);
            if (blockedColumns >= 4) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private static int getBlockedColumns(int corners) {
        int blockedColumns = 0;
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 2; z+=2) {
                for (int y = 0; y <= 4; y+=4) {
                    int cornerIndex = y + z + x;
                    int temp = corners;
                    temp &= (~(1 << (cornerIndex - 1)));
                    if (temp == corners) {
                        blockedColumns++;
                        break;
                    }
                }
            }
        }
        return blockedColumns;
    }

    public static void updateHeightmap(int x, int z, boolean update) {
        int pos = condensePos(x, z);
        boolean setHeightmap = false;
        for (int scanY = height-1; scanY >= 0; scanY--) {
            if (scanY == 0) {
                heightmap[pos] = (short) 0;
                break;
            } else {
                Vector2i block = getBlock(x, scanY, z);
                int corners = getCorner(x, scanY, z);
                boolean blocking = isBlockingHeightmap(block, corners);
                if (!setHeightmap && blocking) {
                    setHeightmap = true;
                    heightmap[pos] = (short) (scanY);
                }
                if (blocking) {
                    for (int scanExtraY = scanY - 1; scanExtraY >= 0; scanExtraY--) {
                        Vector2i blockExtra = getBlock(x, scanExtraY, z);
                        int cornersExtra = getCorner(x, scanExtraY, z);
                        boolean blockingExtra = update && isBlockingHeightmap(blockExtra, cornersExtra);
                        if (!blockingExtra) {
                            Vector4i lightExtra = getLight(x, scanExtraY, z);
                            chunks[condenseChunkPos(x >> 4, scanExtraY >> 4, z >> 4)].setLight(new Vector3i(x & 15, scanExtraY & 15, z & 15),
                                    new Vector4i(lightExtra.x(), lightExtra.y(), lightExtra.z(), (byte) 0), new Vector3i(x, scanExtraY, z));
                            if (update) {
                                recalculateLight(new Vector3i(x, scanExtraY, z), lightExtra.x(), lightExtra.y(), lightExtra.z(), lightExtra.w());
                            }
                        } else {
                            break;
                        }
                    }
                    break;
                } else {
                    Vector4i light = getLight(x, scanY, z);
                    chunks[condenseChunkPos(x >> 4, scanY >> 4, z >> 4)].setLight(new Vector3i(x & 15, scanY & 15, z & 15),
                            new Vector4i(light.x(), light.y(), light.z(), (byte) Math.min(20, 20+Math.max(-20, scanY-Math.max(scanY, heightmap[pos])))), new Vector3i(x, scanY, z));
                    if (update) {
                        updateLight(new Vector3i(x, scanY, z));
                    }
                }
            }
        }
    }

    public static void recalculateLight(Vector3i pos, int r, int g, int b, int s) {
        for (Vector3i neighborPos : new Vector3i[]{
                new Vector3i(pos.x, pos.y, pos.z + 1),
                new Vector3i(pos.x + 1, pos.y, pos.z),
                new Vector3i(pos.x, pos.y, pos.z - 1),
                new Vector3i(pos.x - 1, pos.y, pos.z),
                new Vector3i(pos.x, pos.y + 1, pos.z),
                new Vector3i(pos.x, pos.y - 1, pos.z)
        }) {
            Vector2i neighbor = World.getBlock(neighborPos);
            if (neighbor != null) {
                BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.x);
                if (!blocksLight(neighbor.x, getCorner(neighborPos.x, neighborPos.y, neighborPos.z)) || neighborBlockType instanceof LightBlockType) {
                    Vector4i neighborLight = World.getLight(neighborPos);
                    if (neighborLight != null) {
                        if ((neighborLight.x() > 0 && neighborLight.x() < r) || (neighborLight.y() > 0 && neighborLight.y() < g) || (neighborLight.z() > 0 && neighborLight.z() < b) || (neighborLight.w() > 0 && neighborLight.w() < s)) {
                            Vector3i chunkPos = new Vector3i(neighborPos.x>> 4, neighborPos.y>> 4, neighborPos.z>> 4);
                            byte nr = 0;
                            byte ng = 0;
                            byte nb = 0;
                            if (neighborBlockType instanceof LightBlockType lBlock) {
                                nr = lBlock.lightBlockProperties().r;
                                ng = lBlock.lightBlockProperties().g;
                                nb = lBlock.lightBlockProperties().b;
                            }
                            chunks[condenseChunkPos(chunkPos.x, chunkPos.y, chunkPos.z)].setLight(new Vector3i(neighborPos.x-(chunkPos.x*chunkSize), neighborPos.y-(chunkPos.y*chunkSize), neighborPos.z-(chunkPos.z*chunkSize)),
                                    new Vector4i(nr, ng, nb, (byte) (neighborLight.w() == 20 ? 20 : 0)), pos);
                            recalculateLight(neighborPos, neighborLight.x(), neighborLight.y(), neighborLight.z(), neighborLight.w());
                        }
                        queueLightUpdate(pos);
                    }
                }
            }
        }
    }


    public static void queueLightUpdate(Vector3i pos) {
        boolean exists = lightQueue.contains(pos);
        if (!exists) {
            lightQueue.add(pos);
        }
    }
    public static void updateLight(Vector3i pos) {
        Vector3i chunkPos = new Vector3i(pos.x >> 4, pos.y >> 4, pos.z >> 4);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 1; y++) {
                    int condensedChunkPos = Utils.condenseChunkPos(chunkPos.x+x, chunkPos.y+y, chunkPos.z+z);
                    if (condensedChunkPos > 0 && condensedChunkPos < chunks.length) {
                        if (!chunkLightQueue.contains(condensedChunkPos)) {
                            chunkLightQueue.addFirst(condensedChunkPos);
                        }
                    }
                }
            }
        }
        Vector2i block = getBlock(pos);
        if (block != null) {
            LightHelper.updateLight(pos, block, getLight(pos));
        }
    }

    public static boolean inBounds(int x, int y, int z) {
        return (x >= 0 && x < size && z >= 0 && z < size && y >= 0 && y < height);
    }
    public static boolean inBoundsChunk(int x, int y, int z) {
        return (x >= 0 && x < sizeChunks && z >= 0 && z < sizeChunks && y >= 0 && y < heightChunks);
    }

    public static void setBlock(int x, int y, int z, int blockTypeId, int blockSubtypeId, boolean replace, boolean priority, int tickDelay, boolean silent) {
        if (inBounds(x, y, z)) {
            Vector2i existing = getBlock(x, y, z);
            if (replace || (existing == null || existing.x() == 0)) {
                Vector3i pos = new Vector3i(x, y, z);
                Vector3i chunkPos = new Vector3i(x >> 4, y >> 4, z >> 4);
                int condensedChunkPos = condenseChunkPos(chunkPos);
                if (!chunkBlockQueue.contains(condensedChunkPos)) {
                    if (priority) {
                        chunkBlockQueue.addFirst(condensedChunkPos);
                    } else {
                        chunkBlockQueue.addLast(condensedChunkPos);
                    }
                }
                Vector4i oldLight = getLight(pos);
                byte r = 0;
                byte g = 0;
                byte b = 0;
                BlockType blockType = BlockTypes.blockTypeMap.get(blockTypeId);
                boolean lightChanged = false;
                if (blockType instanceof LightBlockType lType) {
                    lightChanged = true;
                    r = lType.lightBlockProperties().r;
                    g = lType.lightBlockProperties().g;
                    b = lType.lightBlockProperties().b;
                }
                Vector3i localPos = new Vector3i(x & 15, y & 15, z & 15);
                Chunk chunk = chunks[condensedChunkPos];
                Vector2i oldBlock = chunk.getBlock(Utils.condenseLocalPos(localPos));
                BlockType oldBlockType = BlockTypes.blockTypeMap.get(oldBlock.x);
                chunk.setBlock(localPos, blockTypeId, blockSubtypeId, pos);
                if (tickDelay > 0) {
                    ScheduledTicker.scheduleTick(Main.currentTick+tickDelay, pos, 0);
                }
                if (!lightChanged) {
                    lightChanged = blockType.blockProperties.blocksLight != oldBlockType.blockProperties.blocksLight;
                }
                if (lightChanged) {
                    chunk.setLight(new Vector3i(localPos), r, g, b, 0, pos);
                }

                if (blockType.obstructingHeightmap(new Vector2i(blockTypeId, blockSubtypeId)) != oldBlockType.obstructingHeightmap(oldBlock)) {
                    updateHeightmap(x, z, true);
                }

                if (lightChanged) {
                    recalculateLight(pos, org.joml.Math.max(oldLight.x, r), org.joml.Math.max(oldLight.y, g), org.joml.Math.max(oldLight.z, b), oldLight.w);
                }

                BlockTypes.blockTypeMap.get(blockTypeId).onPlace(pos, silent);
            }
        }
    }

    public static void setCorner(int x, int y, int z, int corner) {
        if (inBounds(x, y, z)) {
            Vector3i pos = new Vector3i(x, y, z);
            Vector4i oldLight = getLight(pos);
            byte r = 0;
            byte g = 0;
            byte b = 0;
            if (BlockTypes.blockTypeMap.get(getBlock(pos).x) instanceof LightBlockType lType) {
                r = lType.lightBlockProperties().r;
                g = lType.lightBlockProperties().g;
                b = lType.lightBlockProperties().b;
                updateLight(pos);
            }
            Vector3i chunkPos = new Vector3i(pos.x >> 4, pos.y >> 4, pos.z >> 4);
            int condensedChunkPos = condenseChunkPos(chunkPos);
            Vector3i localPos = new Vector3i(pos.x & 15, pos.y & 15, pos.z & 15);
            Chunk chunk = chunks[condensedChunkPos];
            chunk.setCorner(localPos, corner, pos);
            chunk.setLight(localPos, r, g, b, 0, pos);
            updateHeightmap(pos.x, pos.z, true);
            recalculateLight(pos, oldLight.x, oldLight.y, oldLight.z, oldLight.w);

            Vector2i existing = getBlock(x, y, z);
            if (existing.x() != 0) {
                if (chunkCornerQueue.contains(condensedChunkPos)) {
                    chunkCornerQueue.addLast(condensedChunkPos);
                }
            }
        }
    }

    public static int getCorner(int x, int y, int z) {
        if (inBounds(x, y, z)) {
            return chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].getCorner(condenseLocalPos(x & 15, y & 15, z & 15));
        }
        return 0;
    }
}