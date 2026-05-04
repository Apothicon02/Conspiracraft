package org.conspiracraft.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.conspiracraft.blocks.entities.BlockEntity;
import org.conspiracraft.entities.Entity;
import org.conspiracraft.items.Item;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class World {
    public static int seed = 67;
    public static int size = 4096;
    public static int halfSize = size/2;
    public static int quarterSize = size/4;
    public static int height = 640;
    public static byte chunkSize = 16;
    public static int sizeChunks = size / chunkSize;
    public static int heightChunks = height / chunkSize;
    public static byte regionSizeChunks = 4;
    public static int sizeRegions = sizeChunks / regionSizeChunks;
    public static int heightRegions = heightChunks / regionSizeChunks;
    public static byte lodSize = 4;
    public static int sizeLods = size / lodSize;
    public static int heightLods = height / lodSize;
    public static boolean generating = false;
    public static WorldType worldType = WorldTypes.EARTH;
    public static ObjectOpenHashSet<Item> items = new ObjectOpenHashSet<>();
    public static Int2ObjectOpenHashMap<BlockEntity> blockEntities = new Int2ObjectOpenHashMap<>();
    public static boolean inBounds(int x, int y, int z) {
        return !(x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size);
    }
    public static boolean inBounds(Vector3i pos) {
        return inBounds(pos.x(), pos.y(), pos.z());
    }
    public static boolean inBounds(int padding, int x, int y, int z) {
        return !(x < padding || x >= size-padding || y < padding || y >= height-padding || z < padding || z >= size-padding);
    }
    public static short[] heightmap = new short[size*size];
    public static int packPos(int x, int z) {return (x*size)+z;}
    public static int packPosClamped(int x, int z) {return packPos(Math.clamp(x, 0, size-1), Math.clamp(z, 0, size-1));}
    public static Chunk[] chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static boolean chunkEmptinessChanged = false;
    public static int[] chunkEmptiness = new int[1+((sizeChunks*sizeChunks*heightChunks)/32)];
    public static long[] regions = new long[sizeRegions*sizeRegions*heightRegions];
    public static int packRegionPos(int x, int y, int z) {return x+y*sizeRegions+z*sizeRegions*heightRegions;}
    public static int packRegionPos(Vector3i pos) {return pos.x()+pos.y()*sizeRegions+pos.z()*sizeRegions*heightRegions;}
    public static long[] lods = new long[sizeLods*sizeLods*heightLods];
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
        int cX = x/chunkSize, cY = y/chunkSize, cZ = z/chunkSize;
        Chunk chunk = chunks[packChunkPos(cX, cY, cZ)];
        Vector3i recentlyEditedLocalPos = new Vector3i(x&15, y&15, z&15);
        synchronized (chunk) {
            return chunk.getBlock(Chunk.condenseLocalPos(recentlyEditedLocalPos));
        }
    }
    public static ArrayDeque<Vector3i> updateQueue = new ArrayDeque<>();
    public static HashSet<Vector3i> updateSet = new HashSet<>();
    public static void setBlock(int x, int y, int z, int type, int subType, boolean idk, boolean idk2, int idk3, boolean idk4) {
        setBlock(x, y, z, type, subType);
    }
    public static void setBlock(int x, int y, int z, int type, int subType) {
        if (x < 0 || x >= size || y < 0 || y >= height || z < 0 || z >= size) {
            System.out.print("Tried setting block that's out of bounds: x"+x+", y"+y+", z"+z);
        }
        Vector3i chunkPos = new Vector3i(x/chunkSize, y/chunkSize, z/chunkSize);
        Chunk chunk = chunks[packChunkPos(chunkPos.x(), chunkPos.y(), chunkPos.z())];
        Vector3i recentlyEditedLocalPos = new Vector3i(x&15, y&15, z&15);
        Vector3i recentlyEditedPos = new Vector3i(x, y, z);
        if (!generating && !updateSet.contains(chunkPos)) {
            updateSet.add(chunkPos);
            updateQueue.addLast(chunkPos);
        }
        synchronized (chunk) {
            chunk.setBlock(recentlyEditedLocalPos, type, subType, recentlyEditedPos);

            int lodIdx = packLodPos(x / lodSize, y / lodSize, z / lodSize);
            int bitIdx = (x % lodSize) + (y % lodSize) * lodSize + (z % lodSize) * lodSize * lodSize;
            long mask = 1L << bitIdx;
            if (type > 0) {
                lods[lodIdx] |= mask;
            } else {
                lods[lodIdx] &= ~mask;
            }

            int regionIdx = packRegionPos(chunkPos.x() / regionSizeChunks, chunkPos.y() / regionSizeChunks, chunkPos.z() / regionSizeChunks);
            bitIdx = (chunkPos.x() % regionSizeChunks) + (chunkPos.y() % regionSizeChunks) * regionSizeChunks + (chunkPos.z() % regionSizeChunks) * regionSizeChunks * regionSizeChunks;
            mask = 1L << bitIdx;
            if (chunk.blockPalette.size() > 1 || chunk.blockPalette.getFirst() != 0) {
                regions[regionIdx] |= mask;
            } else {
                regions[regionIdx] &= ~mask;
            }
        }
    }

    public static List<Entity> entities = new ArrayList<>();
    public static void tick() {
        for (Entity entity : entities) {
            entity.tick();
        }
    }
}
