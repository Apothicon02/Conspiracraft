package org.terraflat.game.space;

public class Voxel {
    private short blockID;

    public Voxel(short blockID) {
        this.blockID = blockID;
    }

    public void setBlockID(short blockID) {
        this.blockID = blockID;
    }
    public short getBlockID() {
        return blockID;
    }
    public static byte[] gridToVoxelPos(short x, short y, short z) {
        short[] chunkVoxelPos = Chunk.gridPosToChunkGridPos(x, y, z);
        return new byte[] {(byte) (x-chunkVoxelPos[0]), (byte) (y-chunkVoxelPos[1]), (byte) (z-chunkVoxelPos[2])};
    }
    public static byte[] gridToVoxelPos(short x, short y, short z, byte[] chunkPos) {
        short[] chunkVoxelPos = Chunk.chunkPosToGridPos(chunkPos[0], chunkPos[1], chunkPos[2]);
        return new byte[] {(byte) (x-chunkVoxelPos[0]), (byte) (y-chunkVoxelPos[1]), (byte) (z-chunkVoxelPos[2])};
    }
}
