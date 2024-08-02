package org.apothicon.core.grids;

import org.joml.Vector3f;

public class Grid {
    Vector3f pos;
    float scale;

    public Grid() {
        this.pos = new Vector3f(0, 0, 0);
        this.scale = 1;
    }

    public Grid(Vector3f pos) {
        this.pos = pos;
        this.scale = 1;
    }

    public Grid(float scale) {
        this.pos = new Vector3f(0, 0, 0);
        this.scale = scale;
    }

    public Grid(Vector3f pos, float scale) {
        this.pos = pos;
        this.scale = scale;
    }

    public Vector3f getPos() {
        return pos;
    }

    public void setPos(Vector3f pos) {
        this.pos = pos;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
}