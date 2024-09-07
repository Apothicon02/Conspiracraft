package org.terraflat.game.space;

import java.util.HashMap;
import java.util.Map;

public class Chunk {
    private Map<Byte, Map<Byte, Map<Byte, Voxel>>> voxels = new HashMap<>(Map.of());

    public Voxel setVoxel(byte x, byte y, byte z, Voxel voxel) {
        Map<Byte, Map<Byte, Voxel>> xMap = voxels.get(x);
        if (xMap == null) {
            voxels.put(x, new HashMap<>(Map.of(y, Map.of(z, voxel))));
            return null;
        } else {
            Map<Byte, Voxel> yMap = xMap.get(y);
            if (yMap == null) {
                yMap.put(z, voxel);
            }
        }
        return voxels.get(x).get(y).put(z, voxel);
    }
    public Voxel getVoxel(byte x, byte y, byte z) {
        return voxels.get(x).get(y).get(z);
    }
    public Voxel getVoxel(byte[] voxelPos) {
        return voxels.get(voxelPos[0]).get(voxelPos[1]).get(voxelPos[2]);
    }

    public static byte[] gridPosToChunkPos(short x, short y, short z) {
        return new byte[] {(byte) (x/32), (byte) (y/32), (byte) (z/32)};
    }
    public static short[] chunkPosToGridPos(byte x, byte y, byte z) {
        return new short[] {(short) (x*32), (short) (y*32), (short) (z*32)};
    }
    public static short[] gridPosToChunkGridPos(short x, short y, short z) {
        byte[] chunkPos = gridPosToChunkPos(x, y, z);
        return chunkPosToGridPos(chunkPos[0], chunkPos[1], chunkPos[2]);
    }
}
