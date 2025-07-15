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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.conspiracraft.engine.Utils.*;
import static org.conspiracraft.game.world.WorldGen.*;

public class World {
    public static int seaLevel = 63;
    public static int size = 2048; //6976
    public static int halfSize = size/2;
    public static byte chunkSize = 16;
    public static byte subChunkSize = (byte) (chunkSize/2);
    public static int sizeChunks = size / chunkSize;
    public static short height = 432;
    public static int heightChunks = height / chunkSize;
    public static Path worldPath = Path.of(System.getenv("APPDATA")+"/Conspiracraft/world0");
    public static boolean quarterWorld = false;
    public static boolean doLight = true;

    public static boolean cleanPalettes = false;
    public static boolean createdChunks = false;
    public static boolean worldGenerated = false;
    public static boolean terrainGenerated = false;
    public static boolean surfaceGenerated = false;
    public static boolean featuresGenerated = false;
    public static int currentChunk = -1;

    public static Chunk[] chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static boolean chunkEmptinessChanged = false;
    public static int[] chunkEmptiness = new int[(sizeChunks*sizeChunks*heightChunks)/32];
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

    public static void run() throws IOException {
        if (Files.exists(worldPath)) {
            if (!worldGenerated) {
                String path = (World.worldPath + "/chunks.data");
                loadWorld(path);
                createdChunks = true;
                terrainGenerated = true;
                surfaceGenerated = true;
                featuresGenerated = true;
                worldGenerated = true;
                Renderer.worldChanged = true;
            }
        } else {
            if (!worldGenerated && !createdChunks) {
                createdChunks = true;
                for (int i = 0; i < chunks.length; i++) {
                    chunks[i] = new Chunk();
                }
            }
            currentChunk++;
            if (!cleanPalettes) {
                if (terrainGenerated && surfaceGenerated && featuresGenerated) {
                    if (doLight) {
                        if (stageTime == 0) {
                            stageTime = System.currentTimeMillis() / 1000;
                        }
                        if (currentChunk == sizeChunks) {
                            System.out.print("Light filling took " + ((System.currentTimeMillis()/1000)-stageTime) + "s \n");
                            stageTime = 0;
                            cleanPalettes = true;
                            currentChunk = -1;
                            Renderer.worldChanged = true;
                        } else {
                            fillLight();
                        }
                    }
                } else if (!worldGenerated) {
                    if (!terrainGenerated) {
                        if (stageTime == 0) {
                            stageTime = System.currentTimeMillis() / 1000;
                        }
                        if (currentChunk == sizeChunks) {
                            System.out.print("Terrain generation took " + ((System.currentTimeMillis() / 1000) - stageTime) + "s \n");
                            stageTime = 0;
                            int i = 0;
                            for (short y : heightmap) {
                                surfaceHeightmap[i++] = y;
                            }
                            currentChunk = -1;
                            terrainGenerated = true;
                        } else {
                            generateTerrain();
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
                            Renderer.worldChanged = true;
                        } else {
                            generateSurface();
                        }
                    } else if (!featuresGenerated) {
                        if (stageTime == 0) {
                            stageTime = System.currentTimeMillis() / 1000;
                        }
                        if (currentChunk == sizeChunks) {
                            System.out.print("Feature generation took " + ((System.currentTimeMillis() / 1000) - stageTime) + "s \n");
                            stageTime = 0;
                            currentChunk = -1;
                            surfaceHeightmap = null;
                            featuresGenerated = true;
                            worldGenerated = true;
                            Renderer.worldChanged = true;
                        } else {
                            generateFeatures();
                        }
                    }
                }
            } else {
//                    for (int z = 0; z < sizeChunks; z++) {
//                        for (int y = 0; y < heightChunks; y++) {
//                            chunks[condenseChunkPos(currentChunk, y, z)].cleanPalette();
//                        }
//                    }
            }
        }
//        if (cleaningQueue.size() > 3) {
//            Vector3i blockData = cleaningQueue.getFirst();
//            cleaningQueue.removeFirst();
//            region1Chunks[condenseChunkPos(blockData.x, blockData.y, blockData.z)].cleanPalette();
//        }
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

                    Chunk chunk = new Chunk();
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
        if (!BlockTypes.blockTypeMap.get(blockType).blocksLight) {
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
        if (!BlockTypes.blockTypeMap.get(blockType).blocksLight) {
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
        if (!type.blocksLight && !obstructsHieghtmap) {
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
                                nr = lBlock.r;
                                ng = lBlock.g;
                                nb = lBlock.b;
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
                    boolean exists = chunkLightQueue.contains(condensedChunkPos);
                    if (!exists) {
                        chunkLightQueue.addFirst(condensedChunkPos);
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

    public static void setBlock(int x, int y, int z, int blockTypeId, int blockSubtypeId, boolean replace, boolean priority, boolean tick) {
        if (inBounds(x, y, z)) {
            Vector2i existing = getBlock(x, y, z);
            if (replace || (existing == null || existing.x() == 0)) {
                Vector3i pos = new Vector3i(x, y, z);
                Vector3i chunkPos = new Vector3i(x >> 4, y >> 4, z >> 4);
                int condensedChunkPos = condenseChunkPos(chunkPos);
                if (priority) {
                    chunkBlockQueue.addFirst(condensedChunkPos);
                } else {
                    chunkBlockQueue.addLast(condensedChunkPos);
                }
                Vector4i oldLight = getLight(pos);
                byte r = 0;
                byte g = 0;
                byte b = 0;
                BlockType blockType = BlockTypes.blockTypeMap.get(blockTypeId);
                if (blockType instanceof LightBlockType lType) {
                    r = lType.r;
                    g = lType.g;
                    b = lType.b;
                }
                Vector3i localPos = new Vector3i(x & 15, y & 15, z & 15);
                Chunk chunk = chunks[condensedChunkPos];
                chunk.setBlock(localPos, blockTypeId, blockSubtypeId, pos);
                if (tick) {
                    ScheduledTicker.scheduleTick(Main.currentTick+1, pos);
                }
                chunk.setLight(new Vector3i(localPos), r, g, b, 0, pos);
                updateHeightmap(x, z, true);
                recalculateLight(pos, org.joml.Math.max(oldLight.x, r), org.joml.Math.max(oldLight.y, g), org.joml.Math.max(oldLight.z, b), oldLight.w);

                Source placeSource = new Source(new Vector3f(x, y, z), 1, 1, 0, 0);
                placeSource.play(AudioController.buffers.get((int) ((java.lang.Math.random()*2)+1)));

                Vector2i aboveBlock = getBlock(x, y+1, z);
                if (aboveBlock != null) {
                    int aboveBlockId = aboveBlock.x();
                    if (BlockTypes.blockTypeMap.get(aboveBlockId).needsSupport) {
                        setBlock(x, y + 1, z, 0, 0, true, priority, tick);
                    }
                }
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
                r = lType.r;
                g = lType.g;
                b = lType.b;
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
                chunkCornerQueue.addLast(condenseChunkPos(chunkPos));
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