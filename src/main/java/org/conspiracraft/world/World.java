package org.conspiracraft.world;

import org.joml.Vector3i;

public class World {
    public static int size = 4096;
    public static int height = 320;
    public static byte chunkSize = 16;
    public static int sizeChunks = size / chunkSize;
    public static int heightChunks = height / chunkSize;
    public static byte subChunkSize = (byte) (chunkSize/2);
    public static WorldType worldType = WorldTypes.EARTH;

    public static Chunk[] chunks = new Chunk[sizeChunks*sizeChunks*heightChunks];
    public static boolean chunkEmptinessChanged = false;
    public static int[] chunkEmptiness = new int[1+((sizeChunks*sizeChunks*heightChunks)/32)];

    public static int packPos(int x, int y, int z) {
        return x+y*size+z*(size*height);
    }
    public static int packChunkPos(Vector3i pos) {
        return (((pos.x*World.sizeChunks)+pos.z)*World.heightChunks)+pos.y;
    }
    public static int packChunkPos(int x, int y, int z) {
        return (((x*World.sizeChunks)+z)*World.heightChunks)+y;
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
    }
}
