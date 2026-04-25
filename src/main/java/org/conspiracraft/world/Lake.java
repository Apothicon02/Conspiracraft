package org.conspiracraft.world;

import org.joml.Vector3i;

import java.util.BitSet;

public class Lake {
    public Vector3i pos;
    public BitSet visited;
    public Lake(Vector3i pos) {
        this.pos = pos;
    }
    public void visited(BitSet visited) {
        this.visited = visited;
    }
}
