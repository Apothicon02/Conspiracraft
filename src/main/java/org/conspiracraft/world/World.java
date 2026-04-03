package org.conspiracraft.world;

import org.joml.Vector3f;

public class World {
    public static int size = 1024;
    public static int height = 320;

    public static Vector3f sunPos = new Vector3f(0, World.height*2, 0);
    public static WorldType worldType = WorldTypes.EARTH;
}
