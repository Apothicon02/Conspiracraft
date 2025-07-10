package org.conspiracraft.game;

public class World {
    public static int worldSizeXY = 2048;
    public static int worldSizeZ = 256;
    public static long[] cells = new long[worldSizeXY*worldSizeXY*worldSizeZ];

    public static void init() {
        for (int x = 0; x < worldSizeXY; x++) {
            for (int y = 0; y < worldSizeXY; y++) {
                for (int z = 0; z < worldSizeZ; z++) {
                    if (y < 2000) {
                        cells[(((x*worldSizeXY)+y)*worldSizeZ)+z] = ConspiraMath.compressCell(1, 0, 0, 0, 0, 0, 0, 16);
                    } else {
                        cells[(((x*worldSizeXY)+y)*worldSizeZ)+z] = ConspiraMath.compressCell(0, 0, 0, 0, 0, 0, 0, 16);;
                    }
                }
            }
        }
    }

    public static void tick(float stage) {
        float stageEnd = Math.min(1, stage+0.025f);
        for (int x = (int) (worldSizeXY*stage); x < worldSizeXY*stageEnd; x++) {
            for (int y = (int) (worldSizeXY*stage); y < worldSizeXY*stageEnd; y++) {
                for (int z = (int) (worldSizeZ*stage); z < worldSizeZ*stageEnd; z++) {
                    long prev = cells[(((x*worldSizeXY)+y)*worldSizeZ)+z];
                    for (int i = 0; i < 7; i++) {
                        cells[(((x*worldSizeXY)+y)*worldSizeZ)+z] = ConspiraMath.compressCell((int) (Math.random()*127), 0, 0, 0, 0, 0, 0, 16);
                        prev = cells[(((x*worldSizeXY)+y)*worldSizeZ)+z];
                    }
                    long f = prev;
                }
            }
        }
    }
}