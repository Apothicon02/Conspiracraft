package org.conspiracraft.world;

import org.joml.Vector3i;

public class World {
    public static int size = 2048;
    public static int height = 320;
    public static byte chunkSize = 16;
    public static int sizeChunks = size / chunkSize;
    public static int heightChunks = height / chunkSize;
    public static byte lodSize = 4;
    public static int sizeLods = size / lodSize;
    public static int heightLods = height / lodSize;
    public static WorldType worldType = WorldTypes.EARTH;

    public static short[] heightmap = new short[size*size];
    public static int packPos(int x, int z) {return (x*size)+z;}
    public static Chunk[] chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static boolean chunkEmptinessChanged = false;
    public static int[] chunkEmptiness = new int[1+((sizeChunks*sizeChunks*heightChunks)/32)];
    public static long[] lods = new long[sizeLods*sizeLods*heightLods];
    public static int packLodPos(int x, int y, int z) {return x+y*sizeLods+z*sizeLods*heightLods;}
    public static int packChunkPos(Vector3i pos) {
        return (((pos.x*World.sizeChunks)+pos.z)*World.heightChunks)+pos.y;
    }
    public static int packChunkPos(int x, int y, int z) {
        return (((x*World.sizeChunks)+z)*World.heightChunks)+y;
    }
    public static int packChunkPos(int x, int z) {
        return (x*World.sizeChunks)+z;
    }
    public static Chunk recentlyEditedChunk = chunks[0];
    public static Vector3i recentlyEditedChunkPos = new Vector3i();
    public static Vector3i recentlyEditedLocalPos = new Vector3i();
    public static Vector3i recentlyEditedPos = new Vector3i();
    public static void setBlock(int x, int y, int z, int type, int subType) {
        int cX = x/chunkSize, cY = y/chunkSize, cZ = z/chunkSize;
        if (cX != recentlyEditedChunkPos.x() || cY != recentlyEditedChunkPos.y() || cZ != recentlyEditedChunkPos.z()) {
            recentlyEditedChunkPos.set(cX, cY, cZ);
            recentlyEditedChunk = chunks[packChunkPos(cX, cY, cZ)];
        }
        recentlyEditedLocalPos.set(x&15, y&15, z&15);
        recentlyEditedPos.set(x, y, z);
        recentlyEditedChunk.setBlock(recentlyEditedLocalPos, type, subType, recentlyEditedPos);

        int lodIdx = packLodPos(x / lodSize, y / lodSize, z / lodSize);
        int bitIdx = (x%lodSize) + (y%lodSize) * lodSize + (z%lodSize) * lodSize * lodSize;
        long mask = 1L << bitIdx;
        if (type > 0) {
            lods[lodIdx] |= mask;
        } else {
            lods[lodIdx] &= ~mask;
        }
    }
}
