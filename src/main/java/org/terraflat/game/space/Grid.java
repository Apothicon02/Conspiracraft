package org.terraflat.game.space;

import org.joml.Matrix4f;
import org.terraflat.game.blocks.Blocks;

import java.util.HashMap;
import java.util.Map;

public class Grid {
    private Matrix4f matrix;
    float scale;
    private final Map<Byte, Map<Byte, Map<Byte, Chunk>>> chunks = new HashMap<>(Map.of());

    public Grid() {
        matrix = new Matrix4f();
        scale = 1;
    }
    public Grid(Matrix4f newMatrix) {
        matrix = newMatrix;
        scale = 1;
    }
    public Grid(float newScale) {
        matrix = new Matrix4f();
        scale = newScale;
    }
    public Grid(Matrix4f newMatrix, float newScale) {
        matrix = newMatrix;
        scale = newScale;
    }

    public void setMatrix(Matrix4f newMatrix) {
        matrix = newMatrix;
    }
    public Matrix4f getMatrix() {
        return matrix;
    }
    public float getScale() {
        return scale;
    }
    public void setScale(float scale) {
        this.scale = scale;
    }
    public Map<Byte, Map<Byte, Map<Byte, Chunk>>> getChunks() {
        return chunks;
    }
    public Voxel getVoxel(short x, short y, short z) {
        byte[] chunkPos = Chunk.gridPosToChunkPos(x, y, z);
        Map<Byte, Map<Byte, Chunk>> xChunks = getChunks().get(chunkPos[0]);
        if (xChunks != null) {
            Map<Byte, Chunk> yChunks = xChunks.get(chunkPos[1]);
            if (yChunks != null) {
                Chunk chunk = yChunks.get(chunkPos[2]);
                if (chunk != null) {
                    byte[] voxelPos = Voxel.gridToVoxelPos(x, y, z, chunkPos);
                    return chunk.getVoxel(voxelPos);
                }
            }
        }
        return Blocks.allBlocks.get(Blocks.AIR).getDefaultVoxel();
    }
    public Voxel setVoxel(int x, int y, int z, Voxel voxel) {
        return setVoxel((short) x, (short) y, (short) z, voxel);
    }
    public Voxel setVoxel(short x, short y, short z, Voxel voxel) {
        byte[] chunkPos = Chunk.gridPosToChunkPos(x, y, z);
        Chunk chunk = getChunks().getOrDefault(chunkPos[0], new HashMap<>(Map.of())).getOrDefault(chunkPos[1], new HashMap<>(Map.of())).getOrDefault(chunkPos[2], new Chunk());
        chunk.setVoxel((byte) x, (byte) y, (byte) z, voxel);
        return voxel;
    }
}