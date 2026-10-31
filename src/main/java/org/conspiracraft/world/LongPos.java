package org.conspiracraft.world;

import org.joml.Vector3i;

public class LongPos {
    public static long pack(Vector3i pos, int x, int y, int z) {
        return (((pos.x()+x) & 0x3FFFFFL) << 42) | (((pos.y()+y) & 0xFFFFFL) << 22) | ((pos.z()+z) & 0x3FFFFFL);
    }
    public static long pack(Vector3i pos) {
        return ((pos.x() & 0x3FFFFFL) << 42) | ((pos.y() & 0xFFFFFL) << 22) | (pos.z() & 0x3FFFFFL);
    }
    public static long pack(int x, int y, int z) {
        return ((x & 0x3FFFFFL) << 42) | ((y & 0xFFFFFL) << 22) | (z & 0x3FFFFFL);
    }
    public static int x(long pos) {
        return (int) (pos >>> 42);
    }
    public static int y(long pos) {
        return (int) ((pos >> 22) & 0xFFFFFL);
    }
    public static int z(long pos) {
        return (int) (pos & 0x3FFFFFL);
    }
}
