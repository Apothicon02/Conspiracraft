package org.conspiracraft.game.world;

import org.conspiracraft.engine.ConspiracraftMath;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.Noise;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.rendering.Renderer;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.conspiracraft.engine.Utils.*;

public class World {
    public static int seaLevel = 63;
    public static int size = 6976; //6976
    public static int halfSize = size/2;
    public static byte chunkSize = 16;
    public static byte subChunkSize = (byte) (chunkSize/2);
    public static int sizeChunks = size / chunkSize;
    public static short height = 432;
    public static int heightChunks = height / chunkSize;
    public static Path worldPath = Path.of(System.getenv("APPDATA")+"/Conspiracraft/world0");
    public static boolean quarterWorld = true;
    public static boolean doLight = false;

    public static boolean cleanPalettes = false;
    public static boolean createdChunks = false;
    public static boolean worldGenerated = false;
    public static boolean terrainGenerated = false;
    public static boolean surfaceGenerated = false;
    public static int currentChunk = -1;

    public static Chunk[] chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static short[] surfaceHeightmap = new short[size*size];
    public static short[] heightmap = new short[size*size];
    public static ArrayList<Vector4i> liquidQueue = new ArrayList<>();
    public static ArrayList<Vector4i> blockQueue = new ArrayList<>();
    public static ArrayList<Vector4i> cornerQueue = new ArrayList<>();
    public static ArrayList<Vector3i> lightQueue = new ArrayList<>();
    public static boolean[] sliceUpdates = new boolean[sizeChunks];

    public static void run() throws IOException {
        if (Files.exists(worldPath)) {
            if (!worldGenerated) {
                String path = (World.worldPath + "/chunks.data");
                loadWorld(path);
                createdChunks = true;
                terrainGenerated = true;
                surfaceGenerated = true;
                worldGenerated = true;
                Renderer.worldChanged = true;
            }
        } else {
            if (!worldGenerated) {
                if (!createdChunks) {
                    createdChunks = true;
                    for (int i = 0; i < chunks.length; i++) {
                        chunks[i] = new Chunk();
                    }
                }
                currentChunk++;
                if (!cleanPalettes) {
                    if (terrainGenerated && surfaceGenerated) {
//                        for (int x = 0; x < size; x++) {
//                            for (int z = 0; z < size; z++) {
//                                updateHeightmap(x, z, true);
//                            }
//                            String progress = String.valueOf(((float) x / size)*100).substring(0, 3);
//                            System.out.print("Heightmap is " + (progress + "% generated. \n"));
//                        }
                        if (doLight) {
                            for (int x = 1; x < size - 1; x++) {
        //                            long time = System.currentTimeMillis();
                                for (int z = 1; z < size - 1; z++) {
                                    if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                                        for (int y = height - 1; y > 1; y--) {
                                            Vector2i block = getBlockWorldgen(x, y, z);
                                            Vector4i light = getLightWorldgen(x, y, z);
                                            if (light.w() < 20 && !BlockTypes.blockTypeMap.get(block.x).blocksLight) {
                                                //check if any neighbors are a higher brightness
                                                if (getLightWorldgen(x, y, z + 1).w() > light.w() || getLightWorldgen(x + 1, y, z).w() > light.w() || getLightWorldgen(x, y, z - 1).w() > light.w() ||
                                                        getLightWorldgen(x - 1, y, z).w() > light.w() || getLightWorldgen(x, y + 1, z).w() > light.w() || getLightWorldgen(x, y - 1, z).w() > light.w()) {
                                                    LightHelper.updateLight(new Vector3i(x, y, z), block, light, true);
                                                }
                                            }
                                        }
                                    }
                                }
        //                            String progress = String.valueOf(((float) x / size)*100).substring(0, 3);
        //                            System.out.print("Sunlight is " + (progress + "% filled. \nSlice took " + (System.currentTimeMillis()-time) + "ms to fill. \n"));
                            }
                        }
                        cleanPalettes = true;
                        currentChunk = -1;
                    } else if (!terrainGenerated) {
                        if (currentChunk == sizeChunks) {
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
                        if (currentChunk == sizeChunks) {
                            surfaceHeightmap = null;
                            surfaceGenerated = true;
                        } else {
                            generateSurface();
                        }
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

    public static int maxWorldgenHeight = 256;

    public static void generateTerrain() {
//        long time = System.currentTimeMillis();
        int startX = currentChunk*chunkSize;
        for (int x = startX; x < startX+chunkSize; x++) {
            for (int z = 0; z < size; z++) {
                if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                    float baseCellularNoise = (Noise.blue(Noise.CELLULAR_NOISE.getRGB(x - (((int) (x / Noise.CELLULAR_NOISE.getWidth())) * Noise.CELLULAR_NOISE.getWidth()), z - (((int) (z / Noise.CELLULAR_NOISE.getHeight())) * Noise.CELLULAR_NOISE.getHeight()))) / 128) - 1;
                    float basePerlinNoise = (Noise.blue(Noise.COHERERENT_NOISE.getRGB(x - (((int) (x / Noise.COHERERENT_NOISE.getWidth())) * Noise.COHERERENT_NOISE.getWidth()), z - (((int) (z / Noise.COHERERENT_NOISE.getHeight())) * Noise.COHERERENT_NOISE.getHeight()))) / 128) - 1;
                    float noodleNoise = (Noise.blue(Noise.NOODLE_NOISE.getRGB((x - (((int) (x / Noise.NOODLE_NOISE.getWidth())) * Noise.NOODLE_NOISE.getWidth())), (z - (((int) (z / Noise.NOODLE_NOISE.getHeight())) * Noise.NOODLE_NOISE.getHeight())))) / 128) - 1;
                    int miniX = x*8;
                    int miniZ = z*8;
                    float miniCellularNoise = (Noise.blue(Noise.CELLULAR_NOISE.getRGB(miniX - (((int) (miniX / Noise.CELLULAR_NOISE.getWidth())) * Noise.CELLULAR_NOISE.getWidth()), miniZ - (((int) (miniZ / Noise.CELLULAR_NOISE.getHeight())) * Noise.CELLULAR_NOISE.getHeight()))) / 128) - 1;

                    double centDist = Math.min(1, (distance(x, z, halfSize, halfSize) / halfSize) * 1.15f);
                    double valley = (noodleNoise > 0.67 ? 184 : (184 * Math.min(1, Math.abs(noodleNoise) + 0.33f)));
                    int volcanoElevation = (int) ((((Math.min(0.25, centDist)-0.25)*-2200)+((Math.min(-0.95, centDist-1)+0.95)*10000))*(1 - Math.abs((miniCellularNoise*miniCellularNoise)/12)));
                    int surface = (int) (maxWorldgenHeight - Math.max(centDist * 184, (valley - (Math.max(0, noodleNoise * 320) * Math.abs(basePerlinNoise)))));
                    for (int y = Math.max(surface, volcanoElevation); y >= 0; y--) {
                        double baseGradient = ConspiracraftMath.gradient(y, surface, 48, 2, -1);
                        double baseDensity = baseCellularNoise + baseGradient;
                        if (baseDensity > 0 || volcanoElevation >= y) {
                            heightmap[condensePos(x, z)] = (short) y;
                            break;
                        }
                    }
                }
            }
        }
//        System.out.print("Took "+(System.currentTimeMillis()-time)+"ms to generate slice of terrain. \n");
//        String progress = String.valueOf(((float) currentChunk / sizeChunks)*100).substring(0, 3);
//        System.out.print("World is " + (progress + "% generated. \n"));
    }

    public static void generateSurface() {
//        long time = System.currentTimeMillis();
        int startX = currentChunk*chunkSize;
        for (int x = startX; x < startX+chunkSize; x++) {
            for (int z = 0; z < size; z++) {
                if ((x <= halfSize && z <= halfSize) || !quarterWorld) {
                    int lavaX = x*3;
                    int lavaZ = z*3;
                    float lavaNoise = (Noise.blue(Noise.NOODLE_NOISE.getRGB((lavaX - (((int) (lavaX / Noise.NOODLE_NOISE.getWidth())) * Noise.NOODLE_NOISE.getWidth())), (lavaZ - (((int) (lavaZ / Noise.NOODLE_NOISE.getHeight())) * Noise.NOODLE_NOISE.getHeight())))) / 128) - 1;
                    int y = surfaceHeightmap[condensePos(x, z)];
                    double centDist = Math.min(1, (distance(x, z, halfSize, halfSize) / halfSize) * 1.15f);
                    if (centDist < 0.05f && y < 375) {
                        heightmap[condensePos(x, z)] = (short) 375;
                        for (int newY = 375; newY > 0; newY--) {
                            setBlockWorldgen(x, newY, z, 19, 0);
                            setLightWorldgen(x, newY, z, new Vector4i(0, 0, 0, 0));
                        }
                    } else if (y < seaLevel) {
                        for (int newY = seaLevel; newY > y; newY--) {
                            setBlockWorldgen(x, newY, z, 1, 20);
                            setLightWorldgen(x, newY, z, new Vector4i(0, 0, 0, 0));
                            if (newY == seaLevel) {
                                heightmap[condensePos(x, z)] = (short) y;
                            }
                        }
                        for (int newY = y; newY > y-3; newY--) {
                            setBlockWorldgen(x, newY, z, 3, 0);
                        }
                        for (int newY = y-3; newY >= 0; newY--) {
                            setBlockWorldgen(x, newY, z, 10, 0);
                        }
                    } else {
                        int maxSteepness = 0;
                        int minNeighborY = height - 1;
                        for (int pos : new int[]{condensePos(Math.min(size - 1, x + 3), z), condensePos(Math.max(0, x - 3), z), condensePos(x, Math.min(size - 1, z + 3)), condensePos(x, Math.max(0, z - 3)),
                                condensePos(Math.max(0, x - 3), Math.max(0, z - 3)), condensePos(Math.min(size - 1, x + 3), Math.max(0, z - 3)), condensePos(Math.max(0, x - 3), Math.min(size - 1, z + 3)), condensePos(Math.min(size - 1, x + 3), Math.min(size - 1, z + 3))}) {
                            int nY = surfaceHeightmap[pos];
                            minNeighborY = Math.min(minNeighborY, nY);
                            int steepness = Math.abs(y - nY);
                            maxSteepness = Math.max(maxSteepness, steepness);
                        }
                        boolean flat = maxSteepness < 3;
//                    if (flat) {
//                        maxSteepness = 0;
//                        for (int pos : new int[]{condensePos(Math.min(size-1, x+5), z), condensePos(Math.max(0, x-5), z), condensePos(x, Math.min(size-1, z+5)), condensePos(x, Math.max(0, z-5))}) {
//                            int nY = surfaceHeightmap[pos];
//                            int steepness = Math.abs(y-nY);
//                            maxSteepness = Math.max(maxSteepness, steepness);
//                        }
//                        flat = maxSteepness < 5;
//                    }

                        if (flat) {
                            setBlockWorldgen(x, y, z, 2, 0);
                            setBlockWorldgen(x, y - 1, z, 3, 0);
                            setBlockWorldgen(x, y - 2, z, 3, 0);
                            setBlockWorldgen(x, y - 3, z, 3, 0);
                            for (int newY = y - 4; newY >= 0; newY--) {
                                setBlockWorldgen(x, newY, z, 10, 0);
                            }
                            float basePerlinNoise = (Noise.blue(Noise.COHERERENT_NOISE.getRGB(x - (((int) (x / Noise.COHERERENT_NOISE.getWidth())) * Noise.COHERERENT_NOISE.getWidth()), z - (((int) (z / Noise.COHERERENT_NOISE.getHeight())) * Noise.COHERERENT_NOISE.getHeight()))) / 128) - 1;
                            float foliageNoise = (basePerlinNoise + 0.5f);
                            float exponentialFoliageNoise = foliageNoise * foliageNoise;
                            double torchChance = Math.random();
                            if (torchChance > 0.99995d) {
                                if (torchChance > 0.99997d) {
                                    setBlockWorldgen(x, y, z, 7, 0);
                                    setBlockWorldgen(x, y + 1, z, 7, 0);
                                    setBlockWorldgen(x, y + 2, z, 7, 0);
                                } else {
                                    setBlockWorldgen(x, y + 1, z, 14, 0);
                                }
                            } else if (torchChance < exponentialFoliageNoise * 0.015f) { //tree 0.015
                                int maxHeight = (int) (Math.random() * 4) + 8;
                                int radius = (int) (maxHeight + (torchChance * 100));
                                for (int lX = x - radius; lX <= x + radius; lX++) {
                                    for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                                        for (int lY = y + maxHeight - radius; lY <= y + maxHeight + radius; lY++) {
                                            int xDist = lX - x;
                                            int yDist = lY - (y + maxHeight);
                                            int zDist = lZ - z;
                                            int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                                            if (dist <= radius * 3 && inBounds(lX, lY, lZ)) {
                                                setBlockWorldgen(lX, lY, lZ, 17, 0);
                                                int condensedPos = condensePos(lX, lZ);
                                                heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], lY);
                                                for (int extraY = lY; extraY >= seaLevel; extraY--) {
                                                    setLightWorldgen(lX, extraY, lZ, new Vector4i(0, 0, 0, 0));
                                                }
                                            }
                                        }
                                    }
                                }
                                for (int i = 0; i < maxHeight; i++) {
                                    setBlockWorldgen(x, y + i, z, 16, 0);
                                }
                            } else {
                                double flowerChance = Math.random();
                                setBlockWorldgen(x, y + 1, z, 4 + (flowerChance > 0.98f ? (flowerChance > 0.99f ? 14 : 1) : 0), (int) (Math.random() * 3));
                            }
                        } else {
                            int lavaAir = (int)(Math.abs(lavaNoise)*320);
                            for (int newY = y; newY >= 0; newY--) {
                                int volcanoElevation = (int) ((Math.min(0.25, centDist)-0.25)*-2200);
                                if (newY <= volcanoElevation) {
                                    setBlockWorldgen(x, newY, z, lavaNoise > -0.1 && lavaNoise < 0.1 ? (newY >= y-lavaAir ? 0 : (lavaAir > 3 ? 19 : 9)) : 9, 0);
                                } else {
                                    setBlockWorldgen(x, newY, z, newY >= minNeighborY ? 8 : 10, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
//        System.out.print("Took "+(System.currentTimeMillis()-time)+"ms to generate slice of surface. \n");
//        String progress = String.valueOf(((float) currentChunk / sizeChunks)*100).substring(0, 3);
//        System.out.print("World is " + (progress + "% generated. \n"));
    }

    public static void queueColumnUpdate(Vector3i pos) {
        sliceUpdates[pos.x/16] = true;
    }

    public static Vector2i getBlockWorldgen(int x, int y, int z) {
        return chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].getBlock(condenseLocalPos(x & 15, y & 15, z & 15));
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
    public static Vector4i getLightWorldgen(int x, int y, int z) {
        return chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].getLight(condenseLocalPos(x & 15, y & 15, z & 15));
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

    public static boolean isVerticallyBlockingLight(int blockType, int corners) {
        if (!BlockTypes.blockTypeMap.get(blockType).blocksLight) {
            return false;
        } else {
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
            if (blockedColumns >= 4) {
                return true;
            } else {
                return false;
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
                Vector2i block = getBlock(x, scanY, z);
                int corners = getCorner(x, scanY, z);
                boolean blocking = isVerticallyBlockingLight(block.x, corners);
                if (!setHeightmap && (blocking || block.x == 1)) {
                    setHeightmap = true;
                    heightmap[pos] = (short) (scanY);
                }
                if (blocking) {
                    for (int scanExtraY = scanY - 1; scanExtraY >= 0; scanExtraY--) {
                        Vector2i blockExtra = getBlock(x, scanExtraY, z);
                        int cornersExtra = getCorner(x, scanExtraY, z);
                        boolean blockingExtra = update && isVerticallyBlockingLight(blockExtra.x, cornersExtra);
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
                    queueLightUpdatePriority(pos);
                }
            }
        }
    }

    public static void queueLightUpdateWorldgen(Vector3i pos) {
        for (Vector3i nPos : new Vector3i[]{
                new Vector3i(pos.x, pos.y, pos.z+1), new Vector3i(pos.x, pos.y, pos.z-1),
                new Vector3i(pos.x+1, pos.y, pos.z), new Vector3i(pos.x-1, pos.y, pos.z)
        }) {
            Vector4i nLight = getLightWorldgen(nPos.x, nPos.y, nPos.z);
            if (nLight.w > 0) {
                lightQueue.addLast(pos);
                break;
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
        Vector2i block = getBlock(pos);
        if (block != null) {
            return LightHelper.updateLight(pos, block, getLight(pos));
        } else {
            return 0;
        }
    }

    public static boolean inBounds(int x, int y, int z) {
        return (x >= 0 && x < size && z >= 0 && z < size && y >= 0 && y < height);
    }

    public static void setLightWorldgen(int x, int y, int z, Vector4i light) {
        chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setLight(new Vector3i(x & 15, y & 15, z & 15), light, new Vector3i(x, y, z));
    }
    public static void setBlockWorldgen(int x, int y, int z, int blockTypeId, int blockSubtypeId) {
        chunks[condenseChunkPos(x >> 4, y >> 4, z >> 4)].setBlock(new Vector3i(x & 15, y & 15, z & 15), blockTypeId, blockSubtypeId, new Vector3i(x, y, z));
    }
    public static void setBlock(int x, int y, int z, int blockTypeId, int blockSubtypeId, boolean replace, boolean priority) {
        if (inBounds(x, y, z)) {
            Vector2i existing = getBlock(x, y, z);
            if (replace || (existing == null || existing.x() == 0)) {
                if (priority) {
                    blockQueue.addFirst(new Vector4i(x, y, z, Utils.packInts(blockTypeId, blockSubtypeId)));
                } else {
                    blockQueue.addLast(new Vector4i(x, y, z, Utils.packInts(blockTypeId, blockSubtypeId)));
                }
                Vector2i aboveBlock = getBlock(x, y+1, z);
                if (aboveBlock != null) {
                    int aboveBlockId = aboveBlock.x();
                    if (aboveBlockId == 4 || aboveBlockId == 5) {
                        setBlock(x, y + 1, z, 0, 0, true, priority);
                    }
                }
            }
        }
    }

    public static void setLiquid(int x, int y, int z, int blockTypeId, int blockSubtypeId, boolean replace, boolean priority) {
        if (inBounds(x, y, z)) {
            Vector2i existing = getBlock(x, y, z);
            if (replace || (existing == null || existing.x() == 0)) {
                if (priority) {
                    liquidQueue.addFirst(new Vector4i(x, y, z, Utils.packInts(blockTypeId, blockSubtypeId)));
                } else {
                    liquidQueue.addLast(new Vector4i(x, y, z, Utils.packInts(blockTypeId, blockSubtypeId)));
                }
                Vector2i aboveBlock = getBlock(x, y+1, z);
                if (aboveBlock != null) {
                    int aboveBlockId = aboveBlock.x();
                    if (aboveBlockId == 4 || aboveBlockId == 5) {
                        setBlock(x, y + 1, z, 0, 0, true, priority);
                    }
                }
            }
        }
    }

    public static void setCorner(int x, int y, int z, int corner) {
        if (inBounds(x, y, z)) {
            Vector2i existing = getBlock(x, y, z);
            if (existing.x() != 0) {
                cornerQueue.addLast(new Vector4i(x, y, z, corner));
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