package org.conspiracraft.game.world;

import org.joml.Vector3i;

public class Directions {
    public static Vector3i DOWN = new Vector3i(0, -1, 0);
    public static Vector3i UP = new Vector3i(0, 1, 0);
    public static Vector3i EAST = new Vector3i(1, 0, 0);
    public static Vector3i WEST = new Vector3i(-1, 0, 0);
    public static Vector3i NORTH = new Vector3i(0, 0, 1);
    public static Vector3i SOUTH = new Vector3i(0, 0, -1);
}
