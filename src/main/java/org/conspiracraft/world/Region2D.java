package org.conspiracraft.world;

import static org.conspiracraft.world.World.regionSize;

public class Region2D {
    public final long rPos;
    public final long rX, rZ;
    public final int rXI, rZI;
    public final short[] heights;
    public Region2D(long rPos) {
        this.rPos = rPos;
        this.rX = (rPos >> 42) & 0x3FFFFF;
        this.rZ = (rPos >> 20) & 0x3FFFFF;
        this.rXI = (int)this.rX;
        this.rZI = (int)this.rZ;
        heights = new short[regionSize * regionSize];
    }

    public static int packLocalPos(int x, int z) {
        return (x*regionSize)+z;
    }

    public void unload() {
        World.removeRegion2D(rPos);
    }
}
