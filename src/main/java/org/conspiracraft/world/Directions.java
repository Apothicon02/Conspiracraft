package org.conspiracraft.world;

import org.joml.Vector3f;
import org.joml.Vector3i;

public class Directions {
    public static final Vector3f
            DOWNF = new Vector3f(0, -1, 0),
            UPF = new Vector3f(0, 1, 0),
            EASTF = new Vector3f(1, 0, 0),
            WESTF = new Vector3f(-1, 0, 0),
            NORTHF = new Vector3f(0, 0, 1),
            SOUTHF = new Vector3f(0, 0, -1);
    public static final Vector3i
            DOWN = new Vector3i(0, -1, 0),
            UP = new Vector3i(0, 1, 0),
            EAST = new Vector3i(1, 0, 0),
            WEST = new Vector3i(-1, 0, 0),
            NORTH = new Vector3i(0, 0, 1),
            SOUTH = new Vector3i(0, 0, -1);

    public static final Vector3i[] dirs = new Vector3i[]{DOWN, UP, NORTH, EAST, SOUTH, WEST};
    public static final Vector3i[] horizontalDirs = new Vector3i[]{NORTH, EAST, SOUTH, WEST};
}
