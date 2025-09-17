package org.conspiracraft.game;

import org.joml.Vector4f;

public class World {
    public static float[] voxels = new float[512*4];

    public static int condensePos(int x, int y, int z) {
        return ((((x*8)+y)*8)+z)*4;
    }
    public static boolean inBounds(int x, int y, int z) {
        return (x >= 0 && x < 8 && y >= 0 && y < 8 && z >= 0 && z < 8);
    }

    public static Vector4f getVoxel(int x, int y, int z) {
        if (inBounds(x, y, z)) {
            int pos = condensePos(x, y, z);
            return new Vector4f(voxels[pos], voxels[pos+1], voxels[pos+2], voxels[pos+3]);
        }
        return new Vector4f(0);
    }
    public static Vector4f getVoxel(float x, float y, float z) {
        return getVoxel((int) x, (int) y, (int) z);
    }

    public static void setVoxel(int x, int y, int z, Vector4f color) {
        if (inBounds(x, y, z)) {
            int pos = condensePos(x, y, z);
            voxels[pos] = color.x;
            voxels[pos + 1] = color.y;
            voxels[pos + 2] = color.z;
            voxels[pos + 3] = color.w;
        }
    }
    public static void setVoxel(float x, float y, float z, Vector4f color) {
        setVoxel((int) x, (int) y, (int) z, color);
    }
}
