package org.conspiracraft.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.conspiracraft.Main;
import org.conspiracraft.blocks.entities.BlockEntity;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.entities.Entity;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.items.Item;
import org.conspiracraft.utils.Utils;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class World {
    public static final int seed = 67;
    public static final int seaLevel = 63;
    public static final int size = 4096;
    public static final int halfSize = size/2;
    public static final int quarterSize = size/4;
    public static final int height = 640;
    public static final byte chunkSize = 16;
    public static final int chunkBits = Integer.numberOfTrailingZeros(chunkSize);
    public static final int sizeChunks = size>>chunkBits;
    public static final int heightChunks = height>>chunkBits;
    public static final byte regionSizeChunks = 4;
    public static final int regionBits = Integer.numberOfTrailingZeros(regionSizeChunks);
    public static final int sizeRegions = sizeChunks>>regionBits;
    public static final int heightRegions = heightChunks>>regionBits;
    public static final byte lodSize = 4;
    public static final int lodBits = Integer.numberOfTrailingZeros(lodSize);
    public static final int sizeLods = size >>lodBits;
    public static final int heightLods = height >>lodBits;
    public static boolean generating = false;
    public static final WorldType worldType = WorldTypes.EARTH;
    public static final ObjectOpenHashSet<Item> items = new ObjectOpenHashSet<>();
    public static final Int2ObjectOpenHashMap<BlockEntity> blockEntities = new Int2ObjectOpenHashMap<>();

    public static void save(String path) throws IOException {
        long start = System.currentTimeMillis();
        boolean didExist = Files.exists(Path.of(path));
        new File(path).mkdirs();

        String globalDataPath = path+"global.data";
        FileOutputStream out = new FileOutputStream(globalDataPath);
        byte[] globalData = Utils.longArrayToByteArray(new long[]{Main.timeNs});
        out.write(globalData);
        out.close();

        String heightmapDataPath = path+"heightmap.data";
        out = new FileOutputStream(heightmapDataPath);
        byte[] heightmapData = Utils.shortArrayToByteArray(heightmap);
        out.write(heightmapData);
        out.close();

        String lodsDataPath = path+"lods.data";
        BufferedOutputStream outBuffered = new BufferedOutputStream(new FileOutputStream(lodsDataPath), Renderer.gigabyte);
        byte[] lodsData = Utils.longArrayToByteArray(lods);
        outBuffered.write(lodsData);
        outBuffered.close();

        String regionsDataPath = path+"regions.data";
        out = new FileOutputStream(regionsDataPath);
        byte[] regionsData = Utils.longArrayToByteArray(regions);
        out.write(regionsData);
        out.close();

        String chunksPath = path + "chunks.data";
        out = new FileOutputStream(chunksPath);
        for (Chunk chunk : World.chunks) {
            byte[] blockPalette = Utils.intArrayToByteArray(chunk.getBlockPalette());
            int[] blockData = chunk.getBlockData();
            byte[] blocks;
            if (blockData != null) {
                blocks = Utils.intArrayToByteArray(blockData);
            } else {
                blocks = new byte[]{};
            }

            ByteBuffer buffer = ByteBuffer.allocate(blockPalette.length + 4 + blocks.length + 4);
            buffer.put(Utils.intArrayToByteArray(new int[]{blockPalette.length / 4}));
            buffer.put(blockPalette);
            buffer.put(Utils.intArrayToByteArray(new int[]{blocks.length / 4}));
            buffer.put(blocks);
            out.write(buffer.array());
        }
        out.close();
        System.out.println("Took " + (System.currentTimeMillis()-start) + "ms to save world. ");
    }
    public static void load(String path) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        boolean didExist = Files.exists(Path.of(path));
        if (didExist) {
            long[] globalData = Utils.flipLongArray(Utils.byteArrayToLongArray(new FileInputStream(path + "global.data").readAllBytes()));
            Main.timeNs = globalData[0];
            FileInputStream in = new FileInputStream(path + "heightmap.data");
            Utils.byteArrayToShortArray(heightmap, in.readAllBytes());
            in.close();
            in = new FileInputStream(path + "lods.data");
            Utils.byteArrayToLongArray(lods, in.readAllBytes());
            in.close();
            in = new FileInputStream(path + "regions.data");
            Utils.byteArrayToLongArray(regions, in.readAllBytes());
            in.close();

            in = new FileInputStream(path+"chunks.data");
            int[] data = Utils.byteArrayToIntArray(in.readAllBytes());
            in.close();
            int dataIndex = 0;
            for (int chunkPos = 0; chunkPos < chunks.length; chunkPos++) {
                Chunk chunk = new Chunk(chunkPos);

                int dataSize = data[dataIndex];
                dataIndex++;
                int[] subData = new int[dataSize];
                for (int i = dataSize - 1; i >= 0; i--) {
                    subData[i] = data[dataIndex];
                    dataIndex++;
                }
                chunk.setBlockPalette(subData);

                dataSize = data[dataIndex];
                dataIndex++;
                subData = new int[dataSize];
                for (int i = dataSize - 1; i >= 0; i--) {
                    subData[i] = data[dataIndex];
                    dataIndex++;
                }
                chunk.setBlockData(subData);

                chunks[chunkPos] = chunk;
            }
        } else {
            worldType.generate();
        }
        System.out.println("Took "+(System.currentTimeMillis()-start)+"ms to load world.");
    }
    public static boolean inBounds(int x, int y, int z) {
        return !(x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size);
    }
    public static boolean inBounds(Vector3i pos) {
        return inBounds(pos.x(), pos.y(), pos.z());
    }
    public static boolean inBounds(int padding, int x, int y, int z) {
        return !(x < padding || x >= size-padding || y < padding || y >= height-padding || z < padding || z >= size-padding);
    }
    public static final short[] heightmap = new short[size*size];
    public static int packPos(int x, int z) {return (x*size)+z;}
    public static int packPosClamped(int x, int z) {return packPos(Math.clamp(x, 0, size-1), Math.clamp(z, 0, size-1));}
    public static final Chunk[] chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static boolean chunkEmptinessChanged = false;
    public static final int[] chunkEmptiness = new int[1+((sizeChunks*sizeChunks*heightChunks)/32)];
    public static final long[] regions = new long[sizeRegions*sizeRegions*heightRegions];
    public static int packRegionPos(int x, int y, int z) {return x+y*sizeRegions+z*sizeRegions*heightRegions;}
    public static int packRegionPos(Vector3i pos) {return pos.x()+pos.y()*sizeRegions+pos.z()*sizeRegions*heightRegions;}
    public static final long[] lods = new long[sizeLods*sizeLods*heightLods];
    public static int packLodPos(int x, int y, int z) {return x+y*sizeLods+z*sizeLods*heightLods;}
    public static int packLodPos(Vector3i pos) {return pos.x()+pos.y()*sizeLods+pos.z()*sizeLods*heightLods;}
    public static int packChunkPos(Vector3i pos) {
        return (((pos.x*World.sizeChunks)+pos.z)*World.heightChunks)+pos.y;
    }
    public static int packChunkPos(int x, int y, int z) {
        return (((x*World.sizeChunks)+z)*World.heightChunks)+y;
    }
    public static int packChunkPos(int x, int z) {
        return (x*World.sizeChunks)+z;
    }
    public static Vector2i getBlock(Vector3i pos) {return getBlock(pos.x(), pos.y(), pos.z());}
    public static Vector2i getBlock(float x, float y, float z) {
        return getBlock((int)x, (int)y, (int)z);
    }
    public static Vector2i getBlock(int x, int y, int z) {
        if (x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size) {
            //System.out.print("Tried getting block that's out of bounds: x"+x+", y"+y+", z"+z);
            return new Vector2i(0);
        }
        int cX = x>>chunkBits, cY = y>>chunkBits, cZ = z>>chunkBits;
        Chunk chunk = chunks[packChunkPos(cX, cY, cZ)];
        int pos = Chunk.condenseLocalPos(x&15, y&15, z&15);
        synchronized (chunk) {
            return chunk.getBlock(pos);
        }
    }
    public static final ArrayDeque<Vector3i> updateQueue = new ArrayDeque<>();
    public static final HashSet<Vector3i> updateSet = new HashSet<>();
    public static void setBlock(int x, int y, int z, int type, int subType, boolean idk, boolean idk2, int idk3, boolean idk4) {
        setBlock(x, y, z, type, subType);
    }
    public static void setBlock(int x, int y, int z, int type, int subType) {
        if (x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size) {
            //System.out.print("Tried setting block that's out of bounds: x"+x+", y"+y+", z"+z);
            return;
        }
        Vector3i chunkPos = new Vector3i(x>>chunkBits, y>>chunkBits, z>>chunkBits);
        Chunk chunk = chunks[packChunkPos(chunkPos.x(), chunkPos.y(), chunkPos.z())];
        int lX = x&15;
        int lY = y&15;
        int lZ = z&15;
        if (!generating && !updateSet.contains(chunkPos)) {
            updateSet.add(chunkPos);
            updateQueue.addLast(chunkPos);
        }
        synchronized (chunk) {
            chunk.setBlock(lX, lY, lZ, type, subType);
            updateLod(x, y, z, type == 0);
            updateRegion(chunkPos.x(), chunkPos.y(), chunkPos.z(), !(chunk.blockPalette.size() > 1 || chunk.blockPalette.getFirst() != 0));
        }
    }
    public static void replaceBlock(int x, int y, int z, int type, int subType) {
        if (x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size) {
            //System.out.print("Tried setting block that's out of bounds: x"+x+", y"+y+", z"+z);
            return;
        }
        Vector3i chunkPos = new Vector3i(x>>chunkBits, y>>chunkBits, z>>chunkBits);
        Chunk chunk = chunks[packChunkPos(chunkPos.x(), chunkPos.y(), chunkPos.z())];
        int lX = x&15;
        int lY = y&15;
        int lZ = z&15;
        if (!generating && !updateSet.contains(chunkPos)) {
            updateSet.add(chunkPos);
            updateQueue.addLast(chunkPos);
        }
        synchronized (chunk) {
            int pos = Chunk.condenseLocalPos(x&15, y&15, z&15);
            if (BlockTypes.blockTypeMap.get(chunk.getBlock(pos).x()).blockProperties.isFluidReplaceable) {
                chunk.setBlock(lX, lY, lZ, type, subType);
                updateLod(x, y, z, type == 0);
                updateRegion(chunkPos.x(), chunkPos.y(), chunkPos.z(), !(chunk.blockPalette.size() > 1 || chunk.blockPalette.getFirst() != 0));
            }
        }
    }
    public static void updateLod(int x, int y, int z, boolean empty) {
        int lodIdx = packLodPos(x >>lodBits, y >>lodBits, z >>lodBits);
        int bitIdx = (x % lodSize) + (y % lodSize) * lodSize + (z % lodSize) * lodSize * lodSize;
        long mask = 1L << bitIdx;
        if (empty) {lods[lodIdx] &= ~mask;} else {lods[lodIdx] |= mask;}
    }
    public static void updateRegion(int cX, int cY, int cZ, boolean empty) {
        int regionIdx = packRegionPos(cX >>regionBits, cY >>regionBits, cZ >>regionBits);
        int bitIdx = (cX % regionSizeChunks) + (cY % regionSizeChunks) * regionSizeChunks + (cZ % regionSizeChunks) * regionSizeChunks * regionSizeChunks;
        long mask = 1L << bitIdx;
        if (empty) {regions[regionIdx] &= ~mask;} else {regions[regionIdx] |= mask;}
    }

    public static final List<Entity> entities = new ArrayList<>();
    public static void tick() {
        for (Entity entity : entities) {
            entity.tick();
        }
    }
}
