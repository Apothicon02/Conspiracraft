package org.conspiracraft.world;

public class World {
    public static int size = 1024;
    public static int height = 320;
    public static WorldType worldType = WorldTypes.EARTH;

    public static int packPos(int x, int y, int z) {
        return x+y*size+z*(size*height);
    }
}
