package org.conspiracraft.world;

import org.joml.Vector3i;

import static org.conspiracraft.world.World.regionSizeChunks;

public class Region {
    public final long condensedRegionPos;
    public final long rX, rY, rZ;
    public final int rXI, rYI, rZI;
    public static final int totalChunks = regionSizeChunks*regionSizeChunks*regionSizeChunks;
    public final Chunk[] chunks;

    public Region(long condensedRegionPos) {
        chunks = new Chunk[totalChunks];
        this.condensedRegionPos = condensedRegionPos;
        this.rX = (condensedRegionPos >> 42) & 0x3FFFFF;
        this.rZ = (condensedRegionPos >> 20) & 0x3FFFFF;
        this.rY = condensedRegionPos & 0xFFFFF;
        this.rXI = (int)this.rX;
        this.rYI = (int)this.rY;
        this.rZI = (int)this.rZ;
    }

    public static int packLocalPos(int x, int y, int z) {
        return (((x*regionSizeChunks)+z)*regionSizeChunks)+y;
    }
    public static int packLocalPos(Vector3i pos) {
        return (((pos.x*regionSizeChunks)+pos.z)*regionSizeChunks)+pos.y;
    }

    public Chunk getChunk() {
        return chunks[0];
    }
}
