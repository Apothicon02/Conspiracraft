package org.conspiracraft.physics;

import org.joml.Vector3i;

public class DDAResult {
    public Vector3i prevHit, hit;
    public boolean hitAnything;
    public DDAResult(Vector3i prevHit, Vector3i hit, boolean hitSolid) {
        this.prevHit = prevHit;
        this.hit = hit;
        this.hitAnything = hitSolid;
    }
}
