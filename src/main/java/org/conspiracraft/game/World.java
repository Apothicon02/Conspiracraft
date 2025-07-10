package org.conspiracraft.game;

import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

public class World {
    public static boolean worldGenerated = true;
    public static int worldSizeXY = 512;
    public static int worldSizeZ = 32;
    public static long[] cells = new long[worldSizeXY*worldSizeXY*worldSizeZ];

    public static boolean inBounds(int x, int y, int z) {
        return (x >= 0 && x < worldSizeXY && y >= 0 && y < worldSizeXY && z >= 0 && z < worldSizeZ);
    }
    public static Vector4i getLight(Vector3i pos) {
        return new Vector4i(0, 0, 0, 15);
    }
    public static Vector4i getLight(int x, int y, int z) {
        return new Vector4i(0, 0, 0, 15);
    }
    public static Vector2i getBlock(float x, float y, float z) {
        return new Vector2i();
    }
    public static Vector2i getBlock(int x, int y, int z) {
        return new Vector2i();
    }

    public static void init() {
        for (int x = 0; x < worldSizeXY; x++) {
            for (int y = 0; y < worldSizeXY; y++) {
                for (int z = 0; z < worldSizeZ; z++) {
                    if (y < 2000) {
                        cells[(((x*worldSizeXY)+y)*worldSizeZ)+z] = ConspiraMath.compressCell(1, 0, 0, 0, 0, 0, 0, 16);
                    } else {
                        cells[(((x*worldSizeXY)+y)*worldSizeZ)+z] = ConspiraMath.compressCell(0, 0, 0, 0, 0, 0, 0, 16);
                    }
                }
            }
        }
    }

    public static void tick(double stage) {
        double stageEnd = Math.min(1, stage+0.025d);
        for (int x = (int) (worldSizeXY*stage); x < worldSizeXY*stageEnd; x++) {
            for (int y = 0; y < worldSizeXY; y++) {
                for (int z = 0; z < worldSizeZ; z++) {
                    if (((x & 1) == 0 && (y & 1) == 0) || ((x & 1) != 0 && (y & 1) != 0)) {
                        for (int i = 0; i < 7; i++) {
                            cells[(((x * worldSizeXY) + y) * worldSizeZ) + z] = ConspiraMath.compressCell((int) (Math.random() * 127), 0, 0, 0, 0, 0, 0, 16);
                        }
                    }
                }
            }
        }
    }
}