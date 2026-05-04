package org.conspiracraft.world;

import org.joml.Vector3i;

public class Directions {
    public static Vector3i
            DOWN = new Vector3i(0, -1, 0),
            UP = new Vector3i(0, 1, 0),
            EAST = new Vector3i(1, 0, 0),
            WEST = new Vector3i(-1, 0, 0),
            NORTH = new Vector3i(0, 0, 1),
            SOUTH = new Vector3i(0, 0, -1);

    public static Vector3i[] dirs = new Vector3i[]{DOWN, UP, EAST, WEST, NORTH, SOUTH};
}
