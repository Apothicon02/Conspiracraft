package org.conspiracraft.game;

public class ConspiraMath {
    // Bits: voxel 8, element 8, temperature 16, pressure 16, r 4, g 4, b 4, s 4
    public static long compressCell(int voxel, int element, int temperature, int pressure, int r, int g, int b, int s) {
        return voxel+element;
    }
}